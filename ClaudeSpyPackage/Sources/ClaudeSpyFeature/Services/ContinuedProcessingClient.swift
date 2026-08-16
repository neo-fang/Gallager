#if os(iOS)
    import BackgroundTasks
    import Dependencies
    import DependenciesMacros
    import Foundation
    import os

    public struct ContinuedProcessingRequest: Sendable {
        public let identifier: String
        public let title: String
        public let subtitle: String
        public let expirationHandler: @MainActor @Sendable () -> Void

        public init(
            identifier: String,
            title: String,
            subtitle: String,
            expirationHandler: @escaping @MainActor @Sendable () -> Void
        ) {
            self.identifier = identifier
            self.title = title
            self.subtitle = subtitle
            self.expirationHandler = expirationHandler
        }
    }

    public struct ContinuedProcessingUpdate: Sendable, Equatable {
        public let title: String
        public let subtitle: String
        public let completedUnitCount: Int64
        public let totalUnitCount: Int64

        public init(
            title: String,
            subtitle: String,
            completedUnitCount: Int64,
            totalUnitCount: Int64
        ) {
            self.title = title
            self.subtitle = subtitle
            self.completedUnitCount = completedUnitCount
            self.totalUnitCount = totalUnitCount
        }
    }

    /// Testable boundary around iOS 26 continued-processing APIs.
    @DependencyClient
    public struct ContinuedProcessingClient: Sendable {
        public var start: @MainActor @Sendable (_ request: ContinuedProcessingRequest) throws -> Void
        public var update: @MainActor @Sendable (
            _ identifier: String,
            _ update: ContinuedProcessingUpdate
        ) -> Void
        public var finish: @MainActor @Sendable (_ identifier: String, _ succeeded: Bool) -> Void
    }

    extension ContinuedProcessingClient: DependencyKey {
        public static var previewValue: ContinuedProcessingClient {
            ContinuedProcessingClient(
                start: { _ in },
                update: { _, _ in },
                finish: { _, _ in }
            )
        }

        public static var liveValue: ContinuedProcessingClient {
            ContinuedProcessingClient(
                start: { request in
                    guard #available(iOS 26.0, *) else {
                        throw ContinuedProcessingError.unsupportedSystem
                    }
                    try ContinuedProcessingRuntime.shared.start(request)
                },
                update: { identifier, update in
                    guard #available(iOS 26.0, *) else { return }
                    ContinuedProcessingRuntime.shared.update(identifier: identifier, update: update)
                },
                finish: { identifier, succeeded in
                    guard #available(iOS 26.0, *) else { return }
                    ContinuedProcessingRuntime.shared.finish(identifier: identifier, succeeded: succeeded)
                }
            )
        }
    }

    public enum ContinuedProcessingError: Error {
        case unsupportedSystem
        case registrationFailed
        case duplicateIdentifier
    }

    @available(iOS 26.0, *)
    @MainActor
    private final class ContinuedProcessingRuntime {
        static let shared = ContinuedProcessingRuntime()

        private struct Entry {
            let request: ContinuedProcessingRequest
            var update: ContinuedProcessingUpdate
            var task: BGContinuedProcessingTask?
        }

        private let logger = Logger(
            subsystem: "com.jicezeng.ctrlx",
            category: "ContinuedProcessing"
        )
        private var entries: [String: Entry] = [:]

        func start(_ request: ContinuedProcessingRequest) throws {
            guard entries[request.identifier] == nil else {
                throw ContinuedProcessingError.duplicateIdentifier
            }

            let initialUpdate = ContinuedProcessingUpdate(
                title: request.title,
                subtitle: request.subtitle,
                completedUnitCount: 0,
                totalUnitCount: AgentBackgroundMonitoringPolicy.maximumActivityUnits
            )
            entries[request.identifier] = Entry(
                request: request,
                update: initialUpdate,
                task: nil
            )

            let identifier = request.identifier
            // This closure inherits MainActor isolation from the runtime. The
            // scheduler must therefore invoke it on the main queue; `nil`
            // selects a private BGTaskScheduler queue and trips Swift 6's
            // executor precondition before the closure body can hop actors.
            let registered = BGTaskScheduler.shared.register(
                forTaskWithIdentifier: identifier,
                using: .main
            ) { task in
                ContinuedProcessingRuntime.shared.didLaunch(
                    task,
                    identifier: identifier
                )
            }
            guard registered else {
                entries.removeValue(forKey: identifier)
                throw ContinuedProcessingError.registrationFailed
            }

            let taskRequest = BGContinuedProcessingTaskRequest(
                identifier: identifier,
                title: request.title,
                subtitle: request.subtitle
            )
            taskRequest.strategy = .queue

            do {
                try BGTaskScheduler.shared.submit(taskRequest)
            } catch {
                entries.removeValue(forKey: identifier)
                throw error
            }
        }

        func update(identifier: String, update: ContinuedProcessingUpdate) {
            guard var entry = entries[identifier] else { return }
            entry.update = update
            apply(update, to: entry.task)
            entries[identifier] = entry
        }

        func finish(identifier: String, succeeded: Bool) {
            guard let entry = entries.removeValue(forKey: identifier) else { return }
            BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: identifier)
            if succeeded, let task = entry.task {
                task.progress.completedUnitCount = task.progress.totalUnitCount
            }
            entry.task?.setTaskCompleted(success: succeeded)
        }

        private func didLaunch(_ task: BGTask, identifier: String) {
            guard
                var entry = entries[identifier],
                let continuedTask = task as? BGContinuedProcessingTask
            else {
                task.setTaskCompleted(success: false)
                return
            }

            continuedTask.expirationHandler = {
                ContinuedProcessingRuntime.shared.expire(identifier: identifier)
            }
            entry.task = continuedTask
            apply(entry.update, to: continuedTask)
            entries[identifier] = entry
        }

        private func apply(
            _ update: ContinuedProcessingUpdate,
            to task: BGContinuedProcessingTask?
        ) {
            guard let task else { return }
            task.updateTitle(update.title, subtitle: update.subtitle)
            task.progress.totalUnitCount = update.totalUnitCount
            task.progress.completedUnitCount = min(
                update.completedUnitCount,
                update.totalUnitCount
            )
        }

        private func expire(identifier: String) {
            guard let entry = entries.removeValue(forKey: identifier) else { return }
            logger.info("Continued-processing task expired: \(identifier, privacy: .public)")
            entry.task?.setTaskCompleted(success: false)
            entry.request.expirationHandler()
        }
    }
#endif
