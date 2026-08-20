import Foundation
import Testing
@testable import ClaudeSpyNetworking

@Suite("Terminal stream lease commands")
struct TerminalStreamLeaseCommandTests {
    @Test("Modern stream commands preserve their lease")
    func modernRoundTrip() throws {
        let leaseId = UUID()
        let commands: [CommandType] = [
            .startTerminalStream(StartTerminalStream(leaseId: leaseId, scrollbackLines: 2_500)),
            .stopTerminalStream(StopTerminalStream(leaseId: leaseId)),
        ]

        for command in commands {
            let decoded = try JSONDecoder().decode(
                CommandType.self,
                from: JSONEncoder().encode(command)
            )
            #expect(decoded == command)
        }
    }

    @Test("Legacy empty commands decode without a lease")
    func legacyEmptyPayload() throws {
        let encodedStart = try #require(
            JSONSerialization.jsonObject(with: JSONEncoder().encode(StartTerminalStream()))
                as? [String: Any]
        )
        let encodedStop = try #require(
            JSONSerialization.jsonObject(with: JSONEncoder().encode(StopTerminalStream()))
                as? [String: Any]
        )
        let start = try JSONDecoder().decode(
            StartTerminalStream.self,
            from: Data("{}".utf8)
        )
        let stop = try JSONDecoder().decode(
            StopTerminalStream.self,
            from: Data("{}".utf8)
        )

        #expect(encodedStart.isEmpty)
        #expect(encodedStop.isEmpty)
        #expect(start.leaseId == nil)
        #expect(start.scrollbackLines == nil)
        #expect(stop.leaseId == nil)
    }

    @Test("History depth survives the Android command shape")
    func historyDepthRoundTrip() throws {
        let command = StartTerminalStream(scrollbackLines: 10_000)
        let decoded = try JSONDecoder().decode(
            StartTerminalStream.self,
            from: JSONEncoder().encode(command)
        )

        #expect(decoded.scrollbackLines == 10_000)
        #expect(decoded.leaseId == nil)
    }
}
