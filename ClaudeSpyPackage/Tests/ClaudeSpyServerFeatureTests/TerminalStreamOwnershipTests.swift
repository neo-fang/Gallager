#if os(macOS)
    import ClaudeSpyNetworking
    import Foundation
    import Testing
    @testable import ClaudeSpyServerFeature

    @MainActor
    final private class CapturingTerminalStreamSender: TerminalStreamSending {
        struct Delivery {
            let message: TerminalStreamMessage
            let recipients: Set<String>
        }

        private(set) var deliveries: [Delivery] = []

        func sendTerminalStream(
            _ streamMessage: TerminalStreamMessage,
            to viewerIds: Set<String>
        ) async {
            deliveries.append(Delivery(message: streamMessage, recipients: viewerIds))
        }

        var dataDeliveries: [(data: Data, recipients: Set<String>)] {
            deliveries.compactMap { delivery in
                guard case let .dataChunk(chunk) = delivery.message.updateType else { return nil }
                return (Data(base64Encoded: chunk.dataBase64) ?? Data(), delivery.recipients)
            }
        }

        var resetDeliveries: [(state: TerminalStreamMessage.InitialState, recipients: Set<String>)] {
            deliveries.compactMap { delivery in
                guard case let .resetState(state) = delivery.message.updateType else { return nil }
                return (state, delivery.recipients)
            }
        }
    }

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

        @Test("Bootstrap sends established and joining viewers exactly once")
        func bootstrapRoutesWithoutCrossViewerRefresh() async {
            let sender = CapturingTerminalStreamSender()
            let service = TerminalStreamService(streamSender: sender)
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.finishBootstrap(for: "viewer-a")
            context.beginBootstrap(for: "viewer-b")
            context.appendIncomingData(Data("abc".utf8))

            await service.completeBootstrap(
                for: "viewer-b",
                barrierId: UUID(),
                context: context,
                paneId: "%1"
            )

            #expect(sender.dataDeliveries.count == 2)
            #expect(sender.dataDeliveries[0].data == Data("abc".utf8))
            #expect(sender.dataDeliveries[0].recipients == ["viewer-a"])
            #expect(sender.dataDeliveries[1].data == Data("abc".utf8))
            #expect(sender.dataDeliveries[1].recipients == ["viewer-b"])
            #expect(context.readyViewers == ["viewer-a", "viewer-b"])
        }

        @Test("Bootstrap output is split into bounded relay messages")
        func bootstrapUsesBoundedChunks() async {
            let sender = CapturingTerminalStreamSender()
            let service = TerminalStreamService(streamSender: sender)
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.appendIncomingData(Data(repeating: 0x61, count: 20_000))

            await service.completeBootstrap(
                for: "viewer-a",
                barrierId: UUID(),
                context: context,
                paneId: "%1"
            )

            #expect(sender.dataDeliveries.map(\.data.count) == [8_192, 8_192, 3_616])
            #expect(sender.dataDeliveries.allSatisfy { $0.recipients == ["viewer-a"] })
            #expect(context.isReady("viewer-a"))
        }

        @Test("Oversized live output is split into bounded relay messages")
        func liveOutputUsesBoundedChunks() async {
            let sender = CapturingTerminalStreamSender()
            let service = TerminalStreamService(streamSender: sender)
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.finishBootstrap(for: "viewer-a")

            await service.handleIncomingData(
                context: context,
                paneId: "%1",
                data: Data(repeating: 0x61, count: 65_536)
            )

            #expect(sender.dataDeliveries.map(\.data.count) == Array(repeating: 8_192, count: 8))
            #expect(sender.dataDeliveries.allSatisfy { $0.recipients == ["viewer-a"] })
            #expect(context.pendingDataSize == 0)
        }

        @Test("High water preserves controls and requests one snapshot")
        func highWaterCollapsesDataIntoSnapshot() {
            let buffer = TerminalStreamInputBuffer(paneId: "%1", highWaterBytes: 5)
            let barrierId = UUID()

            #expect(buffer.enqueueData(Data("abc".utf8)) == .enqueued)
            buffer.enqueueControl(.finishBootstrap(viewerId: "viewer-a", barrierId: barrierId))
            #expect(buffer.enqueueData(Data("def".utf8)) == .resyncRequired)
            #expect(buffer.enqueueData(Data("ignored".utf8)) == .awaitingSnapshot)
            #expect(buffer.queuedBytes == 0)

            let snapshot = PaneStreamManager.SubscriptionResult(
                subscriptionId: UUID(),
                initialContent: Data("snapshot".utf8),
                width: 80,
                height: 24
            )
            buffer.enqueueReset(snapshot)

            guard case let .reset(received)? = buffer.dequeue() else {
                Issue.record("Expected reset before preserved control barrier")
                return
            }
            #expect(received.initialContent == Data("snapshot".utf8))
            guard case let .finishBootstrap(viewerId, receivedBarrier)? = buffer.dequeue() else {
                Issue.record("Expected preserved bootstrap barrier")
                return
            }
            #expect(viewerId == "viewer-a")
            #expect(receivedBarrier == barrierId)
            #expect(buffer.dequeue() == nil)
        }

        @Test("Snapshot reset precedes fresh data and clears stale viewer buffers")
        func snapshotResetOrdering() async {
            let sender = CapturingTerminalStreamSender()
            let service = TerminalStreamService(streamSender: sender)
            let context = StreamContext(paneId: "%1", viewerId: "viewer-a")
            context.finishBootstrap(for: "viewer-a")
            context.beginBootstrap(for: "viewer-b")
            context.appendIncomingData(Data("stale".utf8))

            let snapshot = PaneStreamManager.SubscriptionResult(
                subscriptionId: UUID(),
                initialContent: Data("snapshot".utf8),
                width: 80,
                height: 24
            )
            await service.applyReset(snapshot, context: context, paneId: "%1")

            let fresh = Data(repeating: 0x61, count: 8_192)
            await service.handleIncomingData(context: context, paneId: "%1", data: fresh)

            #expect(sender.deliveries.count == 2)
            #expect(sender.resetDeliveries.count == 1)
            #expect(sender.resetDeliveries[0].state.content == Data("snapshot".utf8))
            #expect(sender.resetDeliveries[0].recipients == ["viewer-a", "viewer-b"])
            #expect(sender.dataDeliveries.count == 1)
            #expect(sender.dataDeliveries[0].data == fresh)
            #expect(sender.dataDeliveries[0].recipients == ["viewer-a"])
            #expect(context.takeBootstrapData(for: "viewer-b") == fresh)
        }

    }
#endif
