import Foundation

/// Coalesces terminal bytes until the next MainActor turn and feeds SwiftTerm
/// with a bounded amount of work before yielding again.
@MainActor
public final class TerminalFeedCoalescer {
    private let id: String
    private let maximumFeedBytes: Int
    private let metrics: TerminalTransportMetrics
    private let feed: @MainActor (Data) -> Void

    private var chunks: [Data] = []
    private var headIndex = 0
    private var headOffset = 0
    private var pendingBytes = 0
    private var drainTask: Task<Void, Never>?
    private var drainToken: UUID?

    public init(
        id: String,
        maximumFeedBytes: Int = 32_768,
        metrics: TerminalTransportMetrics = .shared,
        feed: @escaping @MainActor (Data) -> Void
    ) {
        precondition(maximumFeedBytes > 0)
        self.id = id
        self.maximumFeedBytes = maximumFeedBytes
        self.metrics = metrics
        self.feed = feed
    }

    public var queuedBytes: Int {
        pendingBytes
    }

    public func enqueue(_ data: Data) {
        guard !data.isEmpty else { return }
        chunks.append(data)
        pendingBytes += data.count
        recordQueue()
        scheduleDrainIfNeeded()
    }

    /// Drops bytes belonging to an obsolete terminal state before a snapshot reset.
    public func discardPending() {
        drainTask?.cancel()
        drainTask = nil
        drainToken = nil
        chunks.removeAll(keepingCapacity: true)
        headIndex = 0
        headOffset = 0
        pendingBytes = 0
        recordQueue()
    }

    /// Replaces all queued incremental data with an authoritative snapshot.
    /// The reset and feed are synchronous on MainActor so later increments cannot
    /// overtake the snapshot.
    public func replace(with data: Data, reset: @MainActor () -> Void) {
        discardPending()
        reset()
        feedNow(data)
    }

    /// Deterministic drain used by lifecycle boundaries and focused tests.
    public func flushPendingNow() {
        drainTask?.cancel()
        drainTask = nil
        drainToken = nil
        while let data = takeNextBatch() {
            feedNow(data)
        }
    }

    private func scheduleDrainIfNeeded() {
        guard drainTask == nil else { return }
        let token = UUID()
        drainToken = token
        drainTask = Task { @MainActor [weak self] in
            await Task.yield()
            guard let self else { return }

            while !Task.isCancelled, let data = self.takeNextBatch() {
                self.feedNow(data)
                guard self.pendingBytes > 0 else { break }
                await Task.yield()
            }

            guard self.drainToken == token else { return }
            self.drainTask = nil
            self.drainToken = nil
            if self.pendingBytes > 0 {
                self.scheduleDrainIfNeeded()
            }
        }
    }

    private func takeNextBatch() -> Data? {
        guard pendingBytes > 0 else { return nil }
        let byteCount = min(maximumFeedBytes, pendingBytes)
        var result = Data()
        result.reserveCapacity(byteCount)

        while result.count < byteCount, headIndex < chunks.count {
            let chunk = chunks[headIndex]
            let available = chunk.count - headOffset
            let consumed = min(available, byteCount - result.count)
            let start = chunk.index(chunk.startIndex, offsetBy: headOffset)
            let end = chunk.index(start, offsetBy: consumed)
            result.append(contentsOf: chunk[start..<end])
            headOffset += consumed

            if headOffset == chunk.count {
                headIndex += 1
                headOffset = 0
            }
        }

        pendingBytes -= result.count
        compactConsumedChunks()
        recordQueue()
        return result
    }

    private func compactConsumedChunks() {
        if headIndex == chunks.count {
            chunks.removeAll(keepingCapacity: true)
            headIndex = 0
        } else if headIndex >= 32 {
            chunks.removeFirst(headIndex)
            headIndex = 0
        }
    }

    private func feedNow(_ data: Data) {
        guard !data.isEmpty else { return }
        let start = ContinuousClock.now
        feed(data)
        metrics.recordDuration(.terminalFeed, since: start)
    }

    private func recordQueue() {
        metrics.recordQueue(
            .terminalFeed,
            id: id,
            depth: chunks.count - headIndex,
            bytes: pendingBytes
        )
    }
}
