#if os(macOS)
    import Testing
    @testable import ClaudeSpyServerFeature

    @Suite("Terminal stream ownership")
    struct TerminalStreamOwnershipTests {
        @Test("Repeated starts from one viewer are idempotent")
        func duplicateSubscribe() {
            var ownership = TerminalStreamOwnership(viewerId: "viewer-a")

            #expect(ownership.subscribe("viewer-a") == false)
            #expect(ownership.count == 1)
        }

        @Test("The stream survives until its last viewer leaves")
        func multipleViewers() {
            var ownership = TerminalStreamOwnership(viewerId: "viewer-a")
            #expect(ownership.subscribe("viewer-b") == true)

            #expect(ownership.unsubscribe("viewer-a") == .retained(1))
            #expect(ownership.unsubscribe("viewer-b") == .empty)
        }

        @Test("An unknown viewer cannot release another viewer's stream")
        func unknownViewer() {
            var ownership = TerminalStreamOwnership(viewerId: "viewer-a")

            #expect(ownership.unsubscribe("viewer-b") == .notSubscribed)
            #expect(ownership.count == 1)
        }
    }
#endif
