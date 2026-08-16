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
        }

        @ObservationIgnored
        @Dependency(ContinuedProcessingClient.self) private var continuedProcessing

        private let logger = Logger(
            subsystem: "com.jicezeng.ctrlx",
            category: "AgentBackgroundMonitoring"
        )
        private var runs: [PaneKey: MonitoredRun] = [:]
        private var identifiers: [PaneKey: String] = [:]

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
            if let previous = runs.removeValue(forKey: key) {
                await continuedProcessing.finish(previous.identifier, true)
            }

            let identifier = identifiers[key] ?? Self.makeIdentifier()
            identifiers[key] = identifier
            let run = MonitoredRun(
                identifier: identifier,
                sessionName: sessionName,
                phase: .waitingForAgent
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
            } catch {
                if runs[key]?.identifier == identifier {
                    runs.removeValue(forKey: key)
                }
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
                runs[key] = run
                await continuedProcessing.update(
                    run.identifier,
                    ContinuedProcessingUpdate(
                        title: "CtrlX Agent",
                        subtitle: "\(run.sessionName) is working",
                        completedUnitCount: 1,
                        totalUnitCount: 2
                    )
                )

            case let .finish(reason):
                runs.removeValue(forKey: key)
                let subtitle = switch reason {
                case .completed: "\(run.sessionName) finished"
                case .waitingForInput: "\(run.sessionName) needs input"
                }
                await continuedProcessing.update(
                    run.identifier,
                    ContinuedProcessingUpdate(
                        title: "CtrlX Agent",
                        subtitle: subtitle,
                        completedUnitCount: 2,
                        totalUnitCount: 2
                    )
                )
                await continuedProcessing.finish(run.identifier, true)
            }
        }

        public func stop(hostId: String) async {
            let keys = runs.keys.filter { $0.pairId == hostId }
            for key in keys {
                guard let run = runs.removeValue(forKey: key) else { continue }
                await continuedProcessing.finish(run.identifier, false)
            }
        }

        public func stopAll() async {
            let identifiers = runs.values.map(\.identifier)
            runs.removeAll()
            for identifier in identifiers {
                await continuedProcessing.finish(identifier, false)
            }
        }

        private func handleExpiration(identifier: String) {
            guard let key = runs.first(where: { $0.value.identifier == identifier })?.key else {
                return
            }
            runs.removeValue(forKey: key)
        }

        private static func makeIdentifier() -> String {
            let bundleIdentifier = Bundle.main.bundleIdentifier ?? "com.jicezeng.ctrlx"
            return "\(bundleIdentifier).agent-monitor.\(UUID().uuidString)"
        }
    }
#endif
