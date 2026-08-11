#if os(macOS)
    import ClaudeSpyCommon
    import ClaudeSpyNetworking
    import ConcurrencyExtras
    import Dependencies
    import Foundation
    import Observation
    import Testing
    @testable import ClaudeSpyServerFeature

    @MainActor
    @Suite("Local terminal state publishing")
    struct LocalTerminalStatePublishingTests {
        private func pane(path: String = "/tmp") -> PaneInfo {
            PaneInfo(
                paneId: "%5",
                target: "work:0.0",
                sessionName: "work",
                windowIndex: 0,
                paneIndex: 0,
                command: "zsh",
                currentPath: path,
                width: 80,
                height: 24,
                isActive: true
            )
        }

        private func makeWindowManager() -> MirrorWindowManager {
            withDependencies {
                $0[PreferencesService.self] = .inMemory()
                $0[ProcessRunner.self] = .previewValue
                $0[LoginItemService.self] = .previewValue
            } operation: {
                let tmux = TmuxService()
                let streams = PaneStreamManager(
                    tmuxService: tmux,
                    controlClientManager: TmuxControlClientManager()
                )
                return MirrorWindowManager(
                    settings: AppSettings(),
                    tmuxService: tmux,
                    paneStreamManager: streams,
                    editorSessionManager: EditorSessionManager()
                )
            }
        }

        @Test("Unchanged pane metadata performs no observable write")
        func unchangedPaneMetadataIsNoop() async {
            let manager = makeWindowManager()
            #expect(manager.updatePaneStates(from: [pane()]))

            let invalidations = LockIsolated(0)
            withObservationTracking {
                _ = manager.paneStates
            } onChange: {
                invalidations.withValue { $0 += 1 }
            }

            #expect(!manager.updatePaneStates(from: [pane()]))
            await Task.megaYield()
            #expect(invalidations.value == 0)
        }

        @Test("Metadata changes publish once and preserve runtime state")
        func metadataChangePreservesRuntimeState() {
            let manager = makeWindowManager()
            manager.updatePaneStates(from: [pane()])
            manager.applyState(
                pluginID: "codex",
                sessionID: "session-1",
                state: .working,
                tmuxPane: "%5",
                projectPath: "/tmp"
            )

            #expect(manager.updatePaneStates(from: [pane(path: "/tmp/renamed")]))
            #expect(manager.paneStates["%5"]?.currentPath == "/tmp/renamed")
            #expect(manager.paneStates["%5"]?.agentSession?.state == .working)
        }

        @Test("Viewer snapshot does not invalidate host pane state")
        func viewerSnapshotIsPureRead() async {
            let manager = makeWindowManager()
            manager.updatePaneStates(from: [pane()])
            let invalidations = LockIsolated(0)
            withObservationTracking {
                _ = manager.paneStates
            } onChange: {
                invalidations.withValue { $0 += 1 }
            }

            let snapshot = HostSessionStateSnapshot.make(
                windowManager: manager,
                editorManager: manager.editorSessionManager
            )
            await Task.megaYield()

            #expect(Set(snapshot.keys) == Set(manager.paneStates.keys))
            #expect(invalidations.value == 0)
        }

        @Test("Unchanged live tmux refresh does not publish observable state")
        func unchangedLiveRefreshIsNoop() async throws {
            let tmuxPath = try #require(TmuxBinaryLocator.liveValue.find())
            let suffix = UUID().uuidString.lowercased()
            let socketPath = "/tmp/gallager-publish-\(suffix.prefix(8)).sock"
            let sessionName = "gallager-publish-\(suffix)"

            try await withDependencies {
                $0[ProcessRunner.self] = .liveValue
            } operation: {
                let service = TmuxService(tmuxPath: tmuxPath, socketPath: socketPath)
                let created = try await service.createSession(
                    baseName: sessionName,
                    width: 80,
                    height: 24
                )
                do {
                    _ = await service.refreshPanes()
                    let invalidations = LockIsolated(0)
                    withObservationTracking {
                        _ = service.panes
                        _ = service.attachedSessionNames
                        _ = service.isRefreshing
                        _ = service.lastError
                    } onChange: {
                        invalidations.withValue { $0 += 1 }
                    }

                    _ = await service.refreshPanes()
                    await Task.megaYield()
                    #expect(invalidations.value == 0)
                    try await service.killSession(created.sessionName)
                } catch {
                    try? await service.killSession(created.sessionName)
                    throw error
                }
            }
        }

        @Test("Empty background refresh does not replay initial loading state")
        func emptyBackgroundRefreshIsNoop() async {
            let service = TmuxService(tmuxPath: "/nonexistent/gallager-tmux")
            _ = await service.refreshPanes()

            let invalidations = LockIsolated(0)
            withObservationTracking {
                _ = service.panes
                _ = service.isRefreshing
                _ = service.lastError
            } onChange: {
                invalidations.withValue { $0 += 1 }
            }

            _ = await service.refreshPanes()
            await Task.megaYield()
            #expect(invalidations.value == 0)
        }
    }
#endif
