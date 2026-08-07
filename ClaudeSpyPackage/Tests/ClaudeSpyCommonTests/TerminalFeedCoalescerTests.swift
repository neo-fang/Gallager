import ClaudeSpyCommon
import Foundation
import Testing

@Suite("Terminal feed coalescer")
@MainActor
struct TerminalFeedCoalescerTests {
    @Test("Adjacent chunks are combined before feed")
    func combinesAdjacentChunks() {
        var deliveries: [Data] = []
        let coalescer = TerminalFeedCoalescer(id: "test") { deliveries.append($0) }

        coalescer.enqueue(Data("abc".utf8))
        coalescer.enqueue(Data("def".utf8))
        coalescer.flushPendingNow()

        #expect(deliveries == [Data("abcdef".utf8)])
        #expect(coalescer.queuedBytes == 0)
    }

    @Test("Large pending output yields bounded feed calls")
    func boundedFeedCalls() {
        var deliveries: [Data] = []
        let coalescer = TerminalFeedCoalescer(
            id: "test",
            maximumFeedBytes: 4
        ) { deliveries.append($0) }

        coalescer.enqueue(Data("abcdefghij".utf8))
        coalescer.flushPendingNow()

        #expect(deliveries.map(\.count) == [4, 4, 2])
        let combined = deliveries.reduce(into: Data()) { $0.append($1) }
        #expect(combined == Data("abcdefghij".utf8))
    }

    @Test("Snapshot replacement discards obsolete increments")
    func snapshotReplacement() {
        var events: [String] = []
        let coalescer = TerminalFeedCoalescer(id: "test") { data in
            events.append(String(decoding: data, as: UTF8.self))
        }

        coalescer.enqueue(Data("stale".utf8))
        coalescer.replace(with: Data("snapshot".utf8)) {
            events.append("reset")
        }

        #expect(events == ["reset", "snapshot"])
        #expect(coalescer.queuedBytes == 0)
    }
}
