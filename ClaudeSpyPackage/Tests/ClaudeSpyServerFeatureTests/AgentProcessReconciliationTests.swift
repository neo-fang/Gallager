#if os(macOS)
    import ClaudeSpyCommon
    import ClaudeSpyNetworking
    import Dependencies
    import Foundation
    import Testing
    @testable import ClaudeSpyServerFeature

    @MainActor
    @Suite
    struct AgentProcessReconciliationTests {
        private func makeWindowManager() -> MirrorWindowManager {
            withDependencies {
                $0[PreferencesService.self] = .inMemory()
                $0[ProcessRunner.self] = .previewValue
                $0[LoginItemService.self] = .previewValue
            } operation: {
                let tmux = TmuxService()
                let control = TmuxControlClientManager()
                let streams = PaneStreamManager(tmuxService: tmux, controlClientManager: control)
                let manager = MirrorWindowManager(
                    settings: AppSettings(),
                    tmuxService: tmux,
                    paneStreamManager: streams,
                    editorSessionManager: EditorSessionManager()
                )
                manager.updatePaneStates(from: [
                    PaneInfo(
                        paneId: "%5",
                        target: "work:0.0",
                        sessionName: "work",
                        windowIndex: 0,
                        paneIndex: 0,
                        command: "zsh",
                        currentPath: "/tmp",
                        width: 80,
                        height: 24,
                        isActive: true
                    ),
                ])
                return manager
            }
        }

        private func detected(
            pluginID: String = "codex",
            path: String = "/tmp/project"
        ) -> [String: TmuxService.DetectedAgentPane] {
            ["%5": .init(path: path, pluginID: pluginID)]
        }

        @Test("agent process reconciliation runs every ten seconds")
        func reconciliationInterval() {
            #expect(MirrorWindowManager.agentReconciliationInterval == .seconds(10))
        }

        @Test("process detection creates, updates, and removes its own session")
        func processOwnedSessionLifecycle() {
            let manager = makeWindowManager()

            #expect(manager.reconcileDetectedAgentSessions(detected()))
            #expect(manager.paneStates["%5"]?.agentSession?.pluginID == "codex")
            #expect(manager.paneStates["%5"]?.agentSession?.detectedProjectPath == "/tmp/project")
            #expect(manager.paneStates["%5"]?.agentSession?.state == .idle)

            #expect(!manager.reconcileDetectedAgentSessions(detected()))

            #expect(manager.reconcileDetectedAgentSessions(detected(path: "/tmp/renamed")))
            #expect(manager.paneStates["%5"]?.agentSession?.detectedProjectPath == "/tmp/renamed")

            #expect(manager.reconcileDetectedAgentSessions([:]))
            #expect(manager.paneStates["%5"]?.agentSession == nil)
            #expect(!manager.reconcileDetectedAgentSessions([:]))
        }

        @Test("plugin state takes ownership from process detection")
        func pluginStateTakesOwnership() {
            let manager = makeWindowManager()
            #expect(manager.reconcileDetectedAgentSessions(detected()))

            manager.applyState(
                pluginID: "claude-code",
                sessionID: "session-1",
                state: .working,
                tmuxPane: "%5",
                projectPath: "/tmp/hook-project"
            )

            #expect(!manager.reconcileDetectedAgentSessions([:]))
            #expect(manager.paneStates["%5"]?.agentSession?.pluginID == "claude-code")
            #expect(manager.paneStates["%5"]?.agentSession?.state == .working)
            #expect(manager.paneStates["%5"]?.agentSession?.detectedProjectPath == "/tmp/hook-project")
        }

        @Test("session end suppresses detection until the old process disappears")
        func endedSessionIsNotResurrected() {
            let manager = makeWindowManager()
            manager.applyState(
                pluginID: "codex",
                sessionID: "session-1",
                state: .idle,
                tmuxPane: "%5",
                projectPath: "/tmp/project"
            )

            #expect(manager.endAgentSession(forPane: "%5"))
            #expect(!manager.reconcileDetectedAgentSessions(detected()))
            #expect(manager.paneStates["%5"]?.agentSession == nil)

            // One reliable absent snapshot proves the old process exited and
            // releases the tombstone. A later detection is a new agent process.
            #expect(!manager.reconcileDetectedAgentSessions([:]))
            #expect(manager.reconcileDetectedAgentSessions(detected()))
            #expect(manager.paneStates["%5"]?.agentSession != nil)
        }

        @Test("detections for unknown panes are ignored")
        func unknownPaneIsIgnored() {
            let manager = makeWindowManager()
            let unknown = [
                "%99": TmuxService.DetectedAgentPane(path: "/tmp/project", pluginID: "codex"),
            ]

            #expect(!manager.reconcileDetectedAgentSessions(unknown))
            #expect(manager.paneStates["%99"] == nil)
        }
    }
#endif
