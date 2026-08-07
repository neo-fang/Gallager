#if os(macOS)
    import Foundation
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

    @Suite("Terminal stream fixed-cadence batching")
    @MainActor
    struct TerminalStreamBatchingTests {
        @Test("Later chunks keep the first scheduled deadline")
        func continuousChunksDoNotReschedule() {
            let service = TerminalStreamService()
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")

            context.appendData(Data("a".utf8))
            service.scheduleBatchSend(for: context, paneId: "%1")
            let firstTask = context.batchTask

            context.appendData(Data("b".utf8))
            service.scheduleBatchSend(for: context, paneId: "%1")

            #expect(firstTask?.isCancelled == false)
            #expect(String(data: context.flushPendingData(), encoding: .utf8) == "ab")

            context.batchTask?.cancel()
            context.batchTask = nil
        }

        @Test("Draining clears all pending bytes")
        func drainingClearsPendingBytes() {
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.appendData(Data("abc".utf8))

            #expect(context.pendingDataSize == 3)
            #expect(context.flushPendingData() == Data("abc".utf8))
            #expect(context.pendingDataSize == 0)
            #expect(context.flushPendingData().isEmpty)
        }
    }
#endif
