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
        ) async {
            guard
                #available(iOS 26.0, *),
                AgentBackgroundMonitoringPolicy.shouldStart(for: response)
            else { return }

            await start(hostId: hostId, paneId: paneId, sessionName: sessionName)
        }

        public func start(hostId: String, paneId: String, sessionName: String) async {
            guard #available(iOS 26.0, *) else { return }

            let key = PaneKey(pairId: hostId, paneId: paneId)
            cancelActivityReporter(for: key)
            if let previous = runs.removeValue(forKey: key) {
                await continuedProcessing.finish(previous.identifier, true)
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
                try await continuedProcessing.start(ContinuedProcessingRequest(
                    identifier: identifier,
                    title: "CtrlX Agent",
                    subtitle: "Waiting for \(sessionName)",
                    expirationHandler: { [weak self] in
                        await self?.handleExpiration(identifier: identifier)
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

        public func handle(_ status: AgentSessionStatusMessage) async {
            let key = PaneKey(pairId: status.pairId, paneId: status.sessionId)
            guard var run = runs[key] else { return }

            switch AgentBackgroundMonitoringPolicy.decision(
                for: status.state,
                phase: run.phase
            ) {
            case let .keep(phase):
                guard phase != run.phase else { return }
                run.phase = phase
                run.completedActivityUnits += 1
                runs[key] = run
                await continuedProcessing.update(
                    run.identifier,
                    ContinuedProcessingUpdate(
                        title: "CtrlX Agent",
                        subtitle: "\(run.sessionName) is working",
                        completedUnitCount: run.completedActivityUnits,
                        totalUnitCount: -1
                    )
                )

            case let .finish(reason):
                runs.removeValue(forKey: key)
                cancelActivityReporter(for: key)
                let subtitle = switch reason {
                case .completed: "\(run.sessionName) finished"
                case .waitingForInput: "\(run.sessionName) needs input"
                }
                await continuedProcessing.update(
                    run.identifier,
                    ContinuedProcessingUpdate(
                        title: "CtrlX Agent",
                        subtitle: subtitle,
                        completedUnitCount: 1,
                        totalUnitCount: 1
                    )
                )
                await continuedProcessing.finish(run.identifier, true)
            }
        }

        public func stop(hostId: String) async {
            let keys = runs.keys.filter { $0.pairId == hostId }
            for key in keys {
                guard let run = runs.removeValue(forKey: key) else { continue }
                cancelActivityReporter(for: key)
                await continuedProcessing.finish(run.identifier, false)
            }
        }

        public func stopAll() async {
            let identifiers = runs.values.map(\.identifier)
            runs.removeAll()
            for reporter in activityReporters.values {
                reporter.cancel()
            }
            activityReporters.removeAll()
            for identifier in identifiers {
                await continuedProcessing.finish(identifier, false)
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
                    guard await self.reportActivity(for: key, identifier: identifier) else {
                        return
                    }
                }
            }
        }

        private func cancelActivityReporter(for key: PaneKey) {
            activityReporters.removeValue(forKey: key)?.cancel()
        }

        private func reportActivity(for key: PaneKey, identifier: String) async -> Bool {
            guard var run = runs[key], run.identifier == identifier else { return false }

            run.completedActivityUnits += 1
            runs[key] = run
            let subtitle = switch run.phase {
            case .waitingForAgent: "Waiting for \(run.sessionName)"
            case .working: "\(run.sessionName) is working"
            }
            await continuedProcessing.update(
                identifier,
                ContinuedProcessingUpdate(
                    title: "CtrlX Agent",
                    subtitle: subtitle,
                    completedUnitCount: run.completedActivityUnits,
                    totalUnitCount: -1
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
