import ClaudeSpyNetworking
import Foundation
import Testing

@Suite("Terminal stream resynchronization message")
struct TerminalStreamResyncTests {
    @Test("Reset state survives JSON round trip")
    func resetStateRoundTrip() throws {
        let original = TerminalStreamMessage.resetState(
            paneId: "%7",
            width: 120,
            height: 40,
            content: Data([0x1B, 0x5B, 0x32, 0x4A])
        )

        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(TerminalStreamMessage.self, from: data)

        #expect(decoded.paneId == "%7")
        guard case let .resetState(snapshot) = decoded.updateType else {
            Issue.record("Expected resetState")
            return
        }
        #expect(snapshot.width == 120)
        #expect(snapshot.height == 40)
        #expect(snapshot.content == Data([0x1B, 0x5B, 0x32, 0x4A]))
    }
}
