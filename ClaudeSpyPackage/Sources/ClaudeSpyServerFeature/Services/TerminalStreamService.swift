import ClaudeSpyNetworking
import Foundation
import Logging

private enum TerminalStreamInput: Sendable {
    case data(Data)
    case finishBootstrap(viewerId: String, barrierId: UUID)
}

@MainActor
protocol TerminalStreamSending: AnyObject {
    func sendTerminalStream(
        _ streamMessage: TerminalStreamMessage,
        to viewerIds: Set<String>
    ) async
}

extension ConnectedViewerManager: TerminalStreamSending { }

// MARK: - Terminal Stream Service

/// Manages live terminal streaming to connected viewers.
///
/// This service subscribes to PaneStreamManager for terminal data and forwards
/// it to viewers via the network layer. It handles data batching for efficient
/// transmission.
///
/// Usage:
/// 1. Call `startStreaming(paneId:target:...)` when a viewer requests a stream
/// 2. Data flows automatically from PaneStreamManager
/// 3. Call `stopStreaming(paneId:)` when the stream should end
@Observable
@MainActor
final public class TerminalStreamService {
    // MARK: - Properties

    private let logger = Logger(label: "com.claudespy.terminalstream")

    /// Encrypted terminal-message sender. Production uses ConnectedViewerManager;
    /// tests inject an in-memory sender without opening relay connections.
    private weak var streamSender: (any TerminalStreamSending)?

    /// Reference to the pane stream manager
    private weak var paneStreamManager: PaneStreamManager?

    /// Active streams keyed by pane ID
    private var activeStreams: [String: StreamContext] = [:]

    /// Ordered data stream for each pane — ensures strictly sequential processing
    /// of incoming terminal data, preventing interleaved flushes from reordering
    /// WebSocket messages.
    private var dataStreams: [String: AsyncStream<TerminalStreamInput>.Continuation] = [:]
    private var dataConsumerTasks: [String: Task<Void, Never>] = [:]

    // MARK: - Batching Configuration

    /// Maximum time the oldest pending bytes wait before transmission.
    private let batchInterval: Duration = .milliseconds(16)

    /// Maximum batch size before forced send
    private let maxBatchSize = 8_192 // 8KB

    // MARK: - Initialization

    public init() { }

    init(streamSender: any TerminalStreamSending) {
        self.streamSender = streamSender
    }

    /// Configure the service with required dependencies for multi-device support.
    ///
    /// Must be called before starting any streams.
    ///
    /// - Parameters:
    ///   - connectionManager: The ConnectedViewerManager to use for sending stream data to all viewers
    ///   - paneStreamManager: The PaneStreamManager to subscribe to for data
    public func configureWithConnectionManager(
        connectionManager: ConnectedViewerManager,
        paneStreamManager: PaneStreamManager
    ) {
        self.streamSender = connectionManager
        self.paneStreamManager = paneStreamManager
    }

    // MARK: - Public API

    /// Check if a pane is currently streaming.
    public func isStreaming(paneId: String) -> Bool {
        activeStreams[paneId] != nil
    }

    /// Get all pane IDs that are currently streaming.
    public var streamingPaneIds: [String] {
        Array(activeStreams.keys)
    }

    /// Start streaming a pane to viewers.
    ///
    /// Subscribes to PaneStreamManager for data and sends it to iOS.
    /// The initial content is captured atomically with the subscription,
    /// ensuring no timing gap between initial state and live updates.
    ///
    /// - Parameters:
    ///   - paneId: The pane identifier (e.g., "%1")
    ///   - target: The pane target (e.g., "mysession:0.1")
    public func startStreaming(
        paneId: String,
        target: String,
        viewerId: String
    ) async throws {
        guard let streamSender else {
            logger.error("Connection manager not configured, cannot start streaming")
            throw StreamError.notConfigured
        }

        guard let paneStreamManager else {
            logger.error("Pane stream manager not configured, cannot start streaming")
            throw StreamError.notConfigured
        }

        // If a stream is already active for this pane, reuse its single tmux
        // subscription. Only the joining viewer is staged while the existing
        // viewers continue receiving live output.
        if let context = activeStreams[paneId] {
            context.beginBootstrap(for: viewerId)

            guard let current = await paneStreamManager.currentContent(for: paneId) else {
                logger.error("Failed to capture content for existing stream", metadata: [
                    "paneId": "\(paneId)",
                ])
                await stopStreaming(paneId: paneId, force: true)
                throw StreamError.paneNotAvailable
            }

            guard let activeContext = activeStreams[paneId], activeContext === context else {
                throw StreamError.paneNotAvailable
            }

            logger.info("Viewer subscribing to existing stream", metadata: [
                "paneId": "\(paneId)",
                "subscriberCount": "\(context.ownership.count)",
            ])

            let initialMessage = await makeInitialStateMessage(
                paneId: paneId,
                width: current.width,
                height: current.height,
                content: current.content,
                paneStreamManager: paneStreamManager
            )
            await streamSender.sendTerminalStream(initialMessage, to: [viewerId])

            try await finishBootstrap(for: viewerId, context: context, paneId: paneId)

            // Live title callbacks exclude bootstrapping viewers. Send the
            // latest cached title after the viewer joins the ready set.
            if let title = paneStreamManager.terminalTitle(for: paneId) {
                let titleMessage = TerminalStreamMessage.titleChange(paneId: paneId, title: title)
                await streamSender.sendTerminalStream(titleMessage, to: [viewerId])
            }
            return
        }

        logger.info("Starting terminal stream", metadata: [
            "paneId": "\(paneId)",
            "target": "\(target)",
        ])

        // Create context for batching
        let context = StreamContext(paneId: paneId, viewerId: viewerId)

        // Store context BEFORE subscribing so callbacks work immediately
        activeStreams[paneId] = context

        // Start the ordered consumer before capture. While the first viewer is
        // bootstrapping, data is retained in its private buffer and cannot enter
        // the WebSocket send chain ahead of initialState.
        let (dataStream, dataContinuation) = AsyncStream.makeStream(of: TerminalStreamInput.self)
        dataStreams[paneId] = dataContinuation
        dataConsumerTasks[paneId] = makeDataConsumer(
            stream: dataStream,
            context: context,
            paneId: paneId
        )

        // Subscribe to PaneStreamManager for data
        // This returns initial content captured atomically with the subscription
        let result: PaneStreamManager.SubscriptionResult
        do {
            result = try await paneStreamManager.subscribe(
                paneId: paneId,
                target: target,
                onData: { [weak self] (data: Data) in
                    guard let self else { return }
                    guard self.activeStreams[paneId] != nil else { return }
                    self.dataStreams[paneId]?.yield(.data(data))
                },
                onDimensionChange: { [weak self] (newWidth: Int, newHeight: Int) in
                    guard let self else { return }
                    Task {
                        await self.handleDimensionChange(paneId: paneId, width: newWidth, height: newHeight)
                    }
                },
                onTitleChange: { [weak self] (title: String) in
                    guard let self else { return }
                    Task {
                        await self.handleTitleChange(paneId: paneId, title: title)
                    }
                },
                onNotification: { [weak self] notification in
                    guard let self else { return }
                    Task {
                        await self.handleNotification(paneId: paneId, notification: notification)
                    }
                },
                onClipboard: { [weak self] (content: String) in
                    guard let self else { return }
                    Task {
                        await self.handleClipboard(paneId: paneId, content: content)
                    }
                }
            )
        } catch {
            // Clean up on failure
            dataConsumerTasks[paneId]?.cancel()
            dataConsumerTasks.removeValue(forKey: paneId)
            dataContinuation.finish()
            dataStreams.removeValue(forKey: paneId)
            activeStreams.removeValue(forKey: paneId)
            throw error
        }

        context.subscriptionId = result.subscriptionId

        logger.info("Stream subscribed", metadata: [
            "paneId": "\(paneId)",
            "dimensions": "\(result.width)x\(result.height)",
            "bufferSize": "\(result.initialContent.count)",
        ])

        // Send the initial snapshot only to the requesting viewer. The ordered
        // consumer retains capture-time data in that viewer's bootstrap buffer.
        let initialMessage = await makeInitialStateMessage(
            paneId: paneId,
            width: result.width,
            height: result.height,
            content: result.initialContent,
            paneStreamManager: paneStreamManager
        )
        await streamSender.sendTerminalStream(initialMessage, to: [viewerId])

        guard let activeContext = activeStreams[paneId], activeContext === context else {
            dataContinuation.finish()
            dataStreams.removeValue(forKey: paneId)
            throw StreamError.paneNotAvailable
        }

        // This returns only after every pre-barrier byte has been sent. The
        // command response emitted by AppCoordinator is therefore a real ready
        // acknowledgement rather than merely "initialState was queued".
        try await finishBootstrap(for: viewerId, context: context, paneId: paneId)

        if let title = paneStreamManager.terminalTitle(for: paneId) {
            let titleMessage = TerminalStreamMessage.titleChange(paneId: paneId, title: title)
            await streamSender.sendTerminalStream(titleMessage, to: [viewerId])
        }
    }

    private func makeDataConsumer(
        stream: AsyncStream<TerminalStreamInput>,
        context: StreamContext,
        paneId: String
    ) -> Task<Void, Never> {
        Task { [weak self] in
            for await input in stream {
                guard
                    let self,
                    let activeContext = self.activeStreams[paneId],
                    activeContext === context
                else {
                    context.cancelBootstrapBarriers()
                    break
                }

                switch input {
                case let .data(data):
                    await self.handleIncomingData(context: context, paneId: paneId, data: data)

                case let .finishBootstrap(viewerId, barrierId):
                    await self.completeBootstrap(
                        for: viewerId,
                        barrierId: barrierId,
                        context: context,
                        paneId: paneId
                    )
                }
            }
        }
    }

    private func finishBootstrap(
        for viewerId: String,
        context: StreamContext,
        paneId: String
    ) async throws {
        guard let continuation = dataStreams[paneId] else {
            throw StreamError.paneNotAvailable
        }

        let barrierId = UUID()
        continuation.yield(.finishBootstrap(viewerId: viewerId, barrierId: barrierId))
        await context.waitForBootstrapBarrier(barrierId)

        guard
            let activeContext = activeStreams[paneId],
            activeContext === context,
            context.isReady(viewerId)
        else {
            throw StreamError.paneNotAvailable
        }
    }

    func completeBootstrap(
        for viewerId: String,
        barrierId: UUID,
        context: StreamContext,
        paneId: String
    ) async {
        defer { context.completeBootstrapBarrier(barrierId) }
        guard context.isBootstrapping(viewerId) else { return }

        // Data already batched for established viewers must be sent before the
        // ready set changes; otherwise the joining viewer would receive those
        // bytes both here and from the shared live batch.
        await flushPendingData(for: context, paneId: paneId)

        guard let streamSender else { return }
        let bootstrapData = context.takeBootstrapData(for: viewerId)
        var offset = bootstrapData.startIndex
        while offset < bootstrapData.endIndex {
            let remaining = bootstrapData.distance(from: offset, to: bootstrapData.endIndex)
            let end = bootstrapData.index(offset, offsetBy: min(maxBatchSize, remaining))
            let message = TerminalStreamMessage.dataChunk(
                paneId: paneId,
                data: Data(bootstrapData[offset..<end])
            )
            await streamSender.sendTerminalStream(message, to: [viewerId])
            offset = end
        }

        context.finishBootstrap(for: viewerId)
    }

    /// Builds an `initialState` message with the pane's current mouse-mode escape sequences
    /// appended to `content`. Without these sequences, the viewer's SwiftTerm won't pick up
    /// the host pane's current mouse tracking mode until the terminal app redraws.
    private func makeInitialStateMessage(
        paneId: String,
        width: Int,
        height: Int,
        content: Data,
        paneStreamManager: PaneStreamManager
    ) async -> TerminalStreamMessage {
        var payload = content
        payload.append(await paneStreamManager.mouseModeSequences(for: paneId))
        return .initialState(paneId: paneId, width: width, height: height, content: payload)
    }

    /// Errors that can occur during streaming
    public enum StreamError: Error {
        case notConfigured
        case paneNotAvailable
    }

    /// Stop streaming a pane.
    ///
    /// Removes the requesting viewer's ownership. Only truly stops (unsubscribes,
    /// sends streamEnd) when the last viewer leaves or when `force` is true.
    ///
    /// - Parameters:
    ///   - paneId: The pane identifier
    ///   - force: If true, stop immediately regardless of subscriber count (used for system cleanup)
    public func stopStreaming(
        paneId: String,
        viewerId: String? = nil,
        force: Bool = false
    ) async {
        guard let context = activeStreams[paneId] else {
            logger.debug("No active stream for pane \(paneId)")
            return
        }

        let endRecipients = context.ownership.subscribers

        if !force {
            guard let viewerId else {
                logger.error("Missing viewer ID when stopping terminal stream", metadata: [
                    "paneId": "\(paneId)",
                ])
                return
            }

            switch context.ownership.unsubscribe(viewerId) {
            case .notSubscribed:
                logger.debug("Viewer was not subscribed to terminal stream", metadata: [
                    "paneId": "\(paneId)",
                    "viewerId": "\(viewerId)",
                ])
                return

            case let .retained(remainingSubscribers):
                context.removeViewer(viewerId)
                logger.info("Viewer unsubscribed from stream, others still watching", metadata: [
                    "paneId": "\(paneId)",
                    "remainingSubscribers": "\(remainingSubscribers)",
                ])
                return

            case .empty:
                context.removeViewer(viewerId)
                break
            }
        }

        activeStreams.removeValue(forKey: paneId)

        logger.info("Stopping terminal stream", metadata: ["paneId": "\(paneId)"])

        // Stop the ordered data consumer and stream
        dataConsumerTasks[paneId]?.cancel()
        dataConsumerTasks.removeValue(forKey: paneId)
        dataStreams[paneId]?.finish()
        dataStreams.removeValue(forKey: paneId)
        context.cancelBootstrapBarriers()

        // Cancel any pending batch send. The drain below sends its bytes once.
        context.batchTask?.cancel()
        context.batchTask = nil

        // Unsubscribe from PaneStreamManager
        if let subscriptionId = context.subscriptionId {
            await paneStreamManager?.unsubscribe(subscriptionId)
        }

        // Flush any pending data
        await flushPendingData(for: context, paneId: paneId)

        // Send stream end only to viewers that owned this pane.
        guard let streamSender else { return }
        let endMessage = TerminalStreamMessage.streamEnd(paneId: paneId)
        await streamSender.sendTerminalStream(endMessage, to: endRecipients)
    }

    /// Stop all active streams.
    ///
    /// Called when viewers disconnect or the app is shutting down.
    /// Uses force to bypass subscriber count since this is a system-level cleanup.
    public func stopAllStreams() async {
        let paneIds = Array(activeStreams.keys)
        for paneId in paneIds {
            await stopStreaming(paneId: paneId, force: true)
        }
    }

    /// Stops streams for panes that are no longer in the provided list.
    ///
    /// Called when panes change to clean up streams for closed panes.
    /// This sends the streamEnd message to viewer so it can close the terminal view.
    ///
    /// - Parameter currentPanes: The list of currently existing panes
    public func stopStreamsForClosedPanes(currentPanes: [PaneInfo]) async {
        let existingPaneIds = Set(currentPanes.map(\.paneId))
        let streamsToStop = activeStreams.keys.filter { !existingPaneIds.contains($0) }

        for paneId in streamsToStop {
            logger.info("Stopping stream for closed pane", metadata: ["paneId": "\(paneId)"])
            await stopStreaming(paneId: paneId, force: true)
        }
    }

    // MARK: - Private Methods

    func scheduleBatchSend(for context: StreamContext, paneId: String) {
        // A fixed cadence starts with the first byte. Do not move the deadline
        // when later chunks arrive, otherwise a busy TUI can postpone delivery
        // until it stops drawing.
        guard context.batchTask == nil else { return }

        context.batchTask = Task { [weak self] in
            guard let self else { return }
            try? await Task.sleep(for: self.batchInterval)
            guard !Task.isCancelled else { return }
            context.batchTask = nil
            await self.flushPendingData(for: context, paneId: paneId)
        }
    }

    private func flushPendingData(for context: StreamContext, paneId: String) async {
        context.batchTask?.cancel()
        context.batchTask = nil
        let pendingBatch = context.flushPendingData()
        guard !pendingBatch.data.isEmpty, !pendingBatch.recipients.isEmpty else { return }

        guard let streamSender else { return }
        let message = TerminalStreamMessage.dataChunk(paneId: paneId, data: pendingBatch.data)
        await streamSender.sendTerminalStream(message, to: pendingBatch.recipients)
    }

    /// Handle incoming data from PaneStreamManager
    private func handleIncomingData(context: StreamContext, paneId: String, data: Data) async {
        context.appendIncomingData(data)

        // Check if we should send immediately (batch full) or schedule
        if context.pendingDataSize >= maxBatchSize {
            await flushPendingData(for: context, paneId: paneId)
        } else if context.pendingDataSize > 0 {
            scheduleBatchSend(for: context, paneId: paneId)
        }
    }

    /// Handle dimension change from PaneStreamManager
    private func handleDimensionChange(paneId: String, width: Int, height: Int) async {
        guard let context = activeStreams[paneId] else { return }
        guard let streamSender else { return }

        logger.info("Sending dimension change", metadata: [
            "paneId": "\(paneId)",
            "dimensions": "\(width)x\(height)",
        ])

        let message = TerminalStreamMessage.dimensionChange(paneId: paneId, width: width, height: height)
        await streamSender.sendTerminalStream(message, to: context.readyViewers)
    }

    /// Handle title change reported by a subscriber's SwiftTerm instance
    private func handleTitleChange(paneId: String, title: String) async {
        guard let context = activeStreams[paneId] else { return }
        guard let streamSender else { return }

        logger.info("Sending title change", metadata: [
            "paneId": "\(paneId)",
            "title": "\(title)",
        ])

        let message = TerminalStreamMessage.titleChange(paneId: paneId, title: title)
        await streamSender.sendTerminalStream(message, to: context.readyViewers)
    }

    /// Handle terminal notification (OSC 9/777) — forward to connected iOS viewers
    private func handleNotification(
        paneId: String,
        notification: TerminalStreamMessage.TerminalNotification
    ) async {
        guard let context = activeStreams[paneId] else { return }
        guard let streamSender else { return }

        let message = TerminalStreamMessage.notification(
            paneId: paneId,
            title: notification.title,
            body: notification.body
        )
        await streamSender.sendTerminalStream(message, to: context.readyViewers)
    }

    /// Maximum clipboard content size (1 MB) to forward to viewers.
    /// Prevents adversarial or buggy terminals from broadcasting huge payloads.
    private static let maxClipboardSize = 1_048_576

    /// Handle clipboard content (OSC 52) — forward to connected viewers
    private func handleClipboard(paneId: String, content: String) async {
        guard let context = activeStreams[paneId] else { return }
        guard let streamSender else { return }

        guard content.utf8.count <= Self.maxClipboardSize else {
            logger.warning("Dropping oversized clipboard update", metadata: [
                "paneId": "\(paneId)",
                "contentLength": "\(content.count)",
            ])
            return
        }

        logger.info("Sending clipboard update", metadata: [
            "paneId": "\(paneId)",
            "contentLength": "\(content.count)",
        ])

        let message = TerminalStreamMessage.clipboardUpdate(paneId: paneId, content: content)
        await streamSender.sendTerminalStream(message, to: context.readyViewers)
    }
}

// MARK: - Stream Context

/// Context for an active terminal stream, handles data batching.
@MainActor
final class StreamContext {
    struct PendingBatch: Equatable, Sendable {
        let data: Data
        let recipients: Set<String>
    }

    let paneId: String
    var ownership: TerminalStreamOwnership
    var subscriptionId: UUID?
    private var pendingData = Data()
    private var readyViewerIds: Set<String> = []
    private var bootstrapData: [String: Data] = [:]
    private var bootstrapBarrierWaiters: [UUID: CheckedContinuation<Void, Never>] = [:]
    private var completedBootstrapBarriers: Set<UUID> = []
    var batchTask: Task<Void, Never>?

    var pendingDataSize: Int {
        pendingData.count
    }

    init(paneId: String, viewerId: String) {
        self.paneId = paneId
        self.ownership = TerminalStreamOwnership(viewerId: viewerId)
        self.bootstrapData[viewerId] = Data()
    }

    var readyViewers: Set<String> {
        readyViewerIds
    }

    func beginBootstrap(for viewerId: String) {
        ownership.subscribe(viewerId)
        readyViewerIds.remove(viewerId)
        bootstrapData[viewerId] = Data()
    }

    func isBootstrapping(_ viewerId: String) -> Bool {
        bootstrapData[viewerId] != nil
    }

    func isReady(_ viewerId: String) -> Bool {
        readyViewerIds.contains(viewerId)
    }

    func appendIncomingData(_ data: Data) {
        guard !data.isEmpty else { return }

        for viewerId in Array(bootstrapData.keys) {
            bootstrapData[viewerId]?.append(data)
        }
        if !readyViewerIds.isEmpty {
            pendingData.append(data)
        }
    }

    func flushPendingData() -> PendingBatch {
        let batch = PendingBatch(data: pendingData, recipients: readyViewerIds)
        pendingData = Data()
        return batch
    }

    func takeBootstrapData(for viewerId: String) -> Data {
        let data = bootstrapData[viewerId] ?? Data()
        bootstrapData[viewerId] = Data()
        return data
    }

    func finishBootstrap(for viewerId: String) {
        guard bootstrapData.removeValue(forKey: viewerId) != nil else { return }
        guard ownership.contains(viewerId) else { return }
        readyViewerIds.insert(viewerId)
    }

    func removeViewer(_ viewerId: String) {
        readyViewerIds.remove(viewerId)
        bootstrapData.removeValue(forKey: viewerId)
    }

    func waitForBootstrapBarrier(_ barrierId: UUID) async {
        if completedBootstrapBarriers.remove(barrierId) != nil { return }
        await withCheckedContinuation { continuation in
            bootstrapBarrierWaiters[barrierId] = continuation
        }
    }

    func completeBootstrapBarrier(_ barrierId: UUID) {
        if let continuation = bootstrapBarrierWaiters.removeValue(forKey: barrierId) {
            continuation.resume()
        } else {
            completedBootstrapBarriers.insert(barrierId)
        }
    }

    func cancelBootstrapBarriers() {
        let continuations = bootstrapBarrierWaiters.values
        bootstrapBarrierWaiters.removeAll()
        completedBootstrapBarriers.removeAll()
        for continuation in continuations {
            continuation.resume()
        }
    }
}
