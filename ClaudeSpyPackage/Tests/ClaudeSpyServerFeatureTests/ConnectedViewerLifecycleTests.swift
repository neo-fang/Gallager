#if os(macOS)
    import Testing
    @testable import ClaudeSpyServerFeature

    @Suite("Connected viewer lifecycle")
    struct ConnectedViewerLifecycleTests {
        @Test("Invalidation rejects work captured by an older connection")
        func connectionGenerationRejectsOldWork() {
            var generation = ConnectionGeneration()
            let old = generation.current

            generation.invalidate()

            #expect(!generation.isCurrent(old))
            #expect(generation.isCurrent(generation.current))
        }

        @Test("Terminal delivery requires both relay and viewer readiness")
        func terminalDeliveryRequiresViewerPresence() {
            #expect(
                ConnectedViewer.canSendTerminalStream(
                    relayConnected: true,
                    viewerConnected: true
                )
            )
            #expect(
                !ConnectedViewer.canSendTerminalStream(
                    relayConnected: true,
                    viewerConnected: false
                )
            )
            #expect(
                !ConnectedViewer.canSendTerminalStream(
                    relayConnected: false,
                    viewerConnected: true
                )
            )
        }
    }
#endif
