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
            context.finishBootstrap(for: "viewer-a")

            context.appendIncomingData(Data("a".utf8))
            service.scheduleBatchSend(for: context, paneId: "%1")
            let firstTask = context.batchTask

            context.appendIncomingData(Data("b".utf8))
            service.scheduleBatchSend(for: context, paneId: "%1")

            #expect(firstTask?.isCancelled == false)
            let batch = context.flushPendingData()
            #expect(String(data: batch.data, encoding: .utf8) == "ab")
            #expect(batch.recipients == ["viewer-a"])

            context.batchTask?.cancel()
            context.batchTask = nil
        }

        @Test("Draining clears all pending bytes")
        func drainingClearsPendingBytes() {
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.finishBootstrap(for: "viewer-a")
            context.appendIncomingData(Data("abc".utf8))

            #expect(context.pendingDataSize == 3)
            #expect(context.flushPendingData().data == Data("abc".utf8))
            #expect(context.pendingDataSize == 0)
            #expect(context.flushPendingData().data.isEmpty)
        }

        @Test("Bootstrapping viewer is isolated from established live batch")
        func bootstrapDataUsesPrivateBuffer() {
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.finishBootstrap(for: "viewer-a")
            context.beginBootstrap(for: "viewer-b")

            context.appendIncomingData(Data("abc".utf8))

            let live = context.flushPendingData()
            #expect(live.data == Data("abc".utf8))
            #expect(live.recipients == ["viewer-a"])
            #expect(context.takeBootstrapData(for: "viewer-b") == Data("abc".utf8))
            #expect(context.takeBootstrapData(for: "viewer-b").isEmpty)

            context.finishBootstrap(for: "viewer-b")
            #expect(context.readyViewers == ["viewer-a", "viewer-b"])
        }

        @Test("Removing a viewer excludes it from future batches")
        func removedViewerIsNotARouteRecipient() {
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.finishBootstrap(for: "viewer-a")
            context.beginBootstrap(for: "viewer-b")
            context.finishBootstrap(for: "viewer-b")
            context.removeViewer("viewer-a")

            context.appendIncomingData(Data("x".utf8))

            #expect(context.flushPendingData().recipients == ["viewer-b"])
        }

        @Test("Bootstrap barrier handles completion before waiter registration")
        func earlyBootstrapBarrierCompletion() async {
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            let barrierId = UUID()

            context.completeBootstrapBarrier(barrierId)
            await context.waitForBootstrapBarrier(barrierId)
        }

    }
#endif
