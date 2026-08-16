#if os(iOS)
    import ClaudeSpyCommon
    import ClaudeSpyNetworking
    import Dependencies
    import Foundation
    import os

    /// Owns the finite iOS 26 continued-processing task associated with each
    /// user-submitted Agent turn. It does not create work or keep idle sockets
    /// alive; it only gives an already-started turn a system-visible lifetime.
    @Observable
    @MainActor
    final public class AgentBackgroundMonitoringService {
        public static let shared = AgentBackgroundMonitoringService()

        private struct MonitoredRun {
            let identifier: String
            let sessionName: String
            var phase: AgentBackgroundMonitoringPolicy.Phase
            var completedActivityUnits: Int64
        }

        @ObservationIgnored
        @Dependency(ContinuedProcessingClient.self) private var continuedProcessing

        private let logger = Logger(
            subsystem: "com.jicezeng.ctrlx",
            category: "AgentBackgroundMonitoring"
        )
        private var runs: [PaneKey: MonitoredRun] = [:]

        @ObservationIgnored
        private var activityReporters: [PaneKey: Task<Void, Never>] = [:]

        public init() { }

        public func startIfNeeded(
            for response: AgentResponse,
            hostId: String,
            paneId: String,
            sessionName: String
        ) {
            guard
                #available(iOS 26.0, *),
                AgentBackgroundMonitoringPolicy.shouldStart(for: response)
            else { return }

            start(hostId: hostId, paneId: paneId, sessionName: sessionName)
        }

        /// Submit while handling the foreground input event. Deferring this call
        /// races the user's Home gesture; iOS only accepts continued-processing
        /// work submitted from the foreground.
        public func start(hostId: String, paneId: String, sessionName: String) {
            guard #available(iOS 26.0, *) else { return }

            let key = PaneKey(pairId: hostId, paneId: paneId)
            cancelActivityReporter(for: key)
            if let previous = runs.removeValue(forKey: key) {
                continuedProcessing.finish(previous.identifier, true)
            }

            let identifier = Self.makeIdentifier()
            let run = MonitoredRun(
                identifier: identifier,
                sessionName: sessionName,
                phase: .waitingForAgent,
                completedActivityUnits: 0
            )
            runs[key] = run

            do {
                try continuedProcessing.start(ContinuedProcessingRequest(
                    identifier: identifier,
                    title: "CtrlX Agent",
                    subtitle: "Waiting for \(sessionName)",
                    expirationHandler: { [weak self] in
                        self?.handleExpiration(identifier: identifier)
                    }
                ))
                guard runs[key]?.identifier == identifier else { return }
                startActivityReporter(for: key, identifier: identifier)
            } catch {
                if runs[key]?.identifier == identifier {
                    runs.removeValue(forKey: key)
                }
                cancelActivityReporter(for: key)
                logger.info(
                    "Continued processing unavailable; using normal lifecycle: \(error.localizedDescription, privacy: .public)"
                )
            }
        }

        public func handle(_ status: AgentSessionStatusMessage) {
            let key = PaneKey(pairId: status.pairId, paneId: status.sessionId)
            guard var run = runs[key] else { return }

            switch AgentBackgroundMonitoringPolicy.decision(
                for: status.state,
                phase: run.phase
            ) {
            case let .keep(phase):
                guard phase != run.phase else { return }
                run.phase = phase
                run.completedActivityUnits = AgentBackgroundMonitoringPolicy.nextActivityUnit(
                    after: run.completedActivityUnits
                ) ?? run.completedActivityUnits
                runs[key] = run
                continuedProcessing.update(
                    run.identifier,
                    ContinuedProcessingUpdate(
                        title: "CtrlX Agent",
                        subtitle: "\(run.sessionName) is working",
                        completedUnitCount: run.completedActivityUnits,
                        totalUnitCount: AgentBackgroundMonitoringPolicy.maximumActivityUnits
                    )
                )

            case let .finish(reason):
                runs.removeValue(forKey: key)
                cancelActivityReporter(for: key)
                let subtitle = switch reason {
                case .completed: "\(run.sessionName) finished"
                case .waitingForInput: "\(run.sessionName) needs input"
                }
                continuedProcessing.update(
                    run.identifier,
                    ContinuedProcessingUpdate(
                        title: "CtrlX Agent",
                        subtitle: subtitle,
                        completedUnitCount: 1,
                        totalUnitCount: 1
                    )
                )
                continuedProcessing.finish(run.identifier, true)
            }
        }

        public func stop(hostId: String) {
            let keys = runs.keys.filter { $0.pairId == hostId }
            for key in keys {
                guard let run = runs.removeValue(forKey: key) else { continue }
                cancelActivityReporter(for: key)
                continuedProcessing.finish(run.identifier, false)
            }
        }

        public func stop(hostId: String, paneId: String) {
            let key = PaneKey(pairId: hostId, paneId: paneId)
            guard let run = runs.removeValue(forKey: key) else { return }
            cancelActivityReporter(for: key)
            continuedProcessing.finish(run.identifier, false)
        }

        public func stopAll() {
            let identifiers = runs.values.map(\.identifier)
            runs.removeAll()
            for reporter in activityReporters.values {
                reporter.cancel()
            }
            activityReporters.removeAll()
            for identifier in identifiers {
                continuedProcessing.finish(identifier, false)
            }
        }

        private func handleExpiration(identifier: String) {
            guard let key = runs.first(where: { $0.value.identifier == identifier })?.key else {
                return
            }
            runs.removeValue(forKey: key)
            cancelActivityReporter(for: key)
        }

        private func startActivityReporter(for key: PaneKey, identifier: String) {
            activityReporters[key] = Task { [weak self] in
                while !Task.isCancelled {
                    do {
                        try await Task.sleep(for: .seconds(10))
                    } catch {
                        return
                    }

                    guard let self else { return }
                    guard self.reportActivity(for: key, identifier: identifier) else {
                        return
                    }
                }
            }
        }

        private func cancelActivityReporter(for key: PaneKey) {
            activityReporters.removeValue(forKey: key)?.cancel()
        }

        private func reportActivity(for key: PaneKey, identifier: String) -> Bool {
            guard var run = runs[key], run.identifier == identifier else { return false }

            guard let nextUnit = AgentBackgroundMonitoringPolicy.nextActivityUnit(
                after: run.completedActivityUnits
            ) else {
                runs.removeValue(forKey: key)
                activityReporters.removeValue(forKey: key)
                continuedProcessing.finish(identifier, false)
                return false
            }

            run.completedActivityUnits = nextUnit
            runs[key] = run
            let subtitle = switch run.phase {
            case .waitingForAgent: "Waiting for \(run.sessionName)"
            case .working: "\(run.sessionName) is working"
            }
            continuedProcessing.update(
                identifier,
                ContinuedProcessingUpdate(
                    title: "CtrlX Agent",
                    subtitle: subtitle,
                    completedUnitCount: run.completedActivityUnits,
                    totalUnitCount: AgentBackgroundMonitoringPolicy.maximumActivityUnits
                )
            )
            return true
        }

        private static func makeIdentifier() -> String {
            let bundleIdentifier = Bundle.main.bundleIdentifier ?? "com.jicezeng.ctrlx"
            return "\(bundleIdentifier).agent-monitor.\(UUID().uuidString)"
        }
    }
#endif
