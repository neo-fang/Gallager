import Foundation
import Logging
import os.lock

/// Low-overhead aggregate metrics for the terminal transport hot path.
///
/// Every mutation is an in-memory lock update. A compact debug record is emitted
/// at most once per interval, so observing a busy terminal does not add a log
/// write for every pipe read, encrypted frame, or SwiftTerm feed.
public final class TerminalTransportMetrics: Sendable {
    public enum QueueKind: String, CaseIterable, Hashable, Sendable {
        case pipeIngress
        case streamIngress
        case webSocketSend
        case terminalFeed
    }

    public enum TimingKind: String, CaseIterable, Hashable, Sendable {
        case encryption
        case webSocketSend
        case terminalFeed
    }

    public struct QueueSnapshot: Equatable, Sendable {
        public let currentDepth: Int
        public let currentBytes: Int
        public let maximumDepth: Int
        public let maximumBytes: Int
    }

    public struct TimingSnapshot: Equatable, Sendable {
        public let count: Int
        public let totalMicroseconds: Int64
        public let maximumMicroseconds: Int64
    }

    public struct Snapshot: Equatable, Sendable {
        public let queues: [QueueKind: QueueSnapshot]
        public let timings: [TimingKind: TimingSnapshot]
        public let batchCount: Int
        public let totalBatchBytes: Int
        public let maximumBatchBytes: Int
        public let resyncCount: Int
    }

    public static let shared = TerminalTransportMetrics(label: "app")

    private struct QueueKey: Hashable, Sendable {
        let kind: QueueKind
        let id: String
    }

    private struct QueueState: Sendable {
        var currentDepth = 0
        var currentBytes = 0
        var maximumDepth = 0
        var maximumBytes = 0
    }

    private struct TimingState: Sendable {
        var count = 0
        var totalMicroseconds: Int64 = 0
        var maximumMicroseconds: Int64 = 0
    }

    private struct State: Sendable {
        var queues: [QueueKey: QueueState] = [:]
        var timings: [TimingKind: TimingState] = [:]
        var batchCount = 0
        var totalBatchBytes = 0
        var maximumBatchBytes = 0
        var resyncCount = 0
        var nextEmission: ContinuousClock.Instant?
    }

    private let label: String
    private let emissionInterval: Duration
    private let logger = Logger(label: "com.claudespy.terminaltransport")
    private let state = OSAllocatedUnfairLock(initialState: State())

    public init(label: String, emissionInterval: Duration = .seconds(10)) {
        self.label = label
        self.emissionInterval = emissionInterval
    }

    public func recordQueue(
        _ kind: QueueKind,
        id: String,
        depth: Int,
        bytes: Int
    ) {
        mutate { state in
            let key = QueueKey(kind: kind, id: id)
            var queue = state.queues[key] ?? QueueState()
            queue.currentDepth = max(0, depth)
            queue.currentBytes = max(0, bytes)
            queue.maximumDepth = max(queue.maximumDepth, queue.currentDepth)
            queue.maximumBytes = max(queue.maximumBytes, queue.currentBytes)
            state.queues[key] = queue
        }
    }

    public func clearQueue(_ kind: QueueKind, id: String) {
        mutate { state in
            state.queues.removeValue(forKey: QueueKey(kind: kind, id: id))
        }
    }

    public func recordBatch(bytes: Int) {
        guard bytes > 0 else { return }
        mutate { state in
            state.batchCount += 1
            state.totalBatchBytes += bytes
            state.maximumBatchBytes = max(state.maximumBatchBytes, bytes)
        }
    }

    public func recordDuration(_ kind: TimingKind, since start: ContinuousClock.Instant) {
        let duration = ContinuousClock.now - start
        let components = duration.components
        let microseconds = max(
            0,
            components.seconds * 1_000_000 + components.attoseconds / 1_000_000_000_000
        )
        mutate { state in
            var timing = state.timings[kind] ?? TimingState()
            timing.count += 1
            timing.totalMicroseconds += microseconds
            timing.maximumMicroseconds = max(timing.maximumMicroseconds, microseconds)
            state.timings[kind] = timing
        }
    }

    public func recordResync() {
        mutate { $0.resyncCount += 1 }
    }

    public func snapshot() -> Snapshot {
        state.withLock { makeSnapshot(from: $0) }
    }

    private func mutate(_ operation: @Sendable (inout State) -> Void) {
        let now = ContinuousClock.now
        let report = state.withLock { state -> Snapshot? in
            operation(&state)
            guard let next = state.nextEmission else {
                state.nextEmission = now.advanced(by: emissionInterval)
                return nil
            }
            guard now >= next else { return nil }
            state.nextEmission = now.advanced(by: emissionInterval)
            let snapshot = makeSnapshot(from: state)
            resetWindow(&state)
            return snapshot
        }

        guard let report else { return }
        logger.debug(
            "Terminal transport metrics",
            metadata: [
                "label": "\(label)",
                "queues": "\(formatQueues(report.queues))",
                "batches": "\(report.batchCount)",
                "batchBytesTotal": "\(report.totalBatchBytes)",
                "batchBytesMax": "\(report.maximumBatchBytes)",
                "timings": "\(formatTimings(report.timings))",
                "resyncs": "\(report.resyncCount)",
            ]
        )
    }

    private func makeSnapshot(from state: State) -> Snapshot {
        var queues: [QueueKind: QueueSnapshot] = [:]
        for kind in QueueKind.allCases {
            let matching = state.queues.filter { $0.key.kind == kind }.map(\.value)
            queues[kind] = QueueSnapshot(
                currentDepth: matching.reduce(0) { $0 + $1.currentDepth },
                currentBytes: matching.reduce(0) { $0 + $1.currentBytes },
                maximumDepth: matching.map(\.maximumDepth).max() ?? 0,
                maximumBytes: matching.map(\.maximumBytes).max() ?? 0
            )
        }

        var timings: [TimingKind: TimingSnapshot] = [:]
        for kind in TimingKind.allCases {
            let timing = state.timings[kind] ?? TimingState()
            timings[kind] = TimingSnapshot(
                count: timing.count,
                totalMicroseconds: timing.totalMicroseconds,
                maximumMicroseconds: timing.maximumMicroseconds
            )
        }

        return Snapshot(
            queues: queues,
            timings: timings,
            batchCount: state.batchCount,
            totalBatchBytes: state.totalBatchBytes,
            maximumBatchBytes: state.maximumBatchBytes,
            resyncCount: state.resyncCount
        )
    }

    /// Keep current gauges across reporting windows, but reset accumulated
    /// counters and maxima. Empty per-view queues are removed here so opening
    /// and closing terminals cannot grow the metrics registry forever.
    private func resetWindow(_ state: inout State) {
        state.queues = state.queues.reduce(into: [:]) { queues, entry in
            guard entry.value.currentDepth > 0 || entry.value.currentBytes > 0 else { return }
            queues[entry.key] = QueueState(
                currentDepth: entry.value.currentDepth,
                currentBytes: entry.value.currentBytes,
                maximumDepth: entry.value.currentDepth,
                maximumBytes: entry.value.currentBytes
            )
        }
        state.timings.removeAll(keepingCapacity: true)
        state.batchCount = 0
        state.totalBatchBytes = 0
        state.maximumBatchBytes = 0
        state.resyncCount = 0
    }

    private func formatQueues(_ queues: [QueueKind: QueueSnapshot]) -> String {
        QueueKind.allCases.map { kind in
            let value = queues[kind] ?? QueueSnapshot(
                currentDepth: 0,
                currentBytes: 0,
                maximumDepth: 0,
                maximumBytes: 0
            )
            return "\(kind.rawValue)=\(value.currentDepth)/\(value.currentBytes)B,max:\(value.maximumDepth)/\(value.maximumBytes)B"
        }.joined(separator: ",")
    }

    private func formatTimings(_ timings: [TimingKind: TimingSnapshot]) -> String {
        TimingKind.allCases.map { kind in
            let value = timings[kind] ?? TimingSnapshot(count: 0, totalMicroseconds: 0, maximumMicroseconds: 0)
            return "\(kind.rawValue)=\(value.count)/\(value.totalMicroseconds)us,max:\(value.maximumMicroseconds)us"
        }.joined(separator: ",")
    }
}
