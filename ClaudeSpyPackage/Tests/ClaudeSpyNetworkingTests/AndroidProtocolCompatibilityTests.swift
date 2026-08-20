import ClaudeSpyEncryption
@testable import ClaudeSpyNetworking
import Foundation
import Testing

@Suite("Android wire protocol compatibility")
struct AndroidProtocolCompatibilityTests {
    @Test("Android pairing completion decodes on the relay")
    func pairingCompletion() throws {
        let data = Data(#"{"pairingCode":"ABCDEF","deviceId":"android-1","deviceName":"Pixel","publicKey":"AQID","publicKeyId":"android-key"}"#.utf8)

        let completion = try JSONDecoder().decode(PairingCompletion.self, from: data)

        #expect(completion.pairingCode == "ABCDEF")
        #expect(completion.deviceId == "android-1")
        #expect(completion.deviceName == "Pixel")
        #expect(completion.publicKey == "AQID")
        #expect(completion.publicKeyId == "android-key")
    }

    @Test("Android viewer registration decodes on the relay")
    func viewerRegistration() throws {
        let data = Data(#"{"type":"registerViewer","payload":{"pairId":"pair-1","deviceId":"android-1","deviceName":"Pixel","publicKey":"AQID","publicKeyId":"android-key"}}"#.utf8)

        let message = try JSONDecoder().decode(WebSocketMessage.self, from: data)

        guard case let .registerViewer(payload) = message else {
            Issue.record("Expected registerViewer")
            return
        }
        #expect(payload.pairId == "pair-1")
        #expect(payload.deviceId == "android-1")
        #expect(payload.deviceName == "Pixel")
        #expect(payload.publicKeyId == "android-key")
    }

    @Test("Android encrypted wrapper decodes on both peers")
    func encryptedWrapper() throws {
        let data = Data(#"{"type":"encrypted","payload":{"payload":{"ciphertext":"AQIDBA==","senderKeyId":"android-key","version":1}}}"#.utf8)

        let message = try JSONDecoder().decode(WebSocketMessage.self, from: data)

        guard case let .encrypted(wrapper) = message else {
            Issue.record("Expected encrypted message")
            return
        }
        #expect(wrapper.payload.ciphertext == Data([1, 2, 3, 4]))
        #expect(wrapper.payload.senderKeyId == "android-key")
        #expect(wrapper.payload.version == 1)
    }

    @Test("Android raw-input command uses Swift associated-value shape")
    func rawInputCommand() throws {
        let data = Data(#"{"type":"command","payload":{"id":"00000000-0000-0000-0000-000000000001","paneId":"%7","command":{"sendRawInput":{"_0":{"dataBase64":"DQ=="}}},"timestamp":"2026-08-05T00:00:00Z"}}"#.utf8)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        let message = try decoder.decode(WebSocketMessage.self, from: data)

        guard case let .command(command) = message else {
            Issue.record("Expected command")
            return
        }
        guard case let .sendRawInput(spec) = command.command else {
            Issue.record("Expected sendRawInput")
            return
        }
        #expect(command.paneId == "%7")
        #expect(spec.data == Data([0x0D]))
    }

    @Test("Android stream-start command uses Swift associated-value shape")
    func streamStartCommand() throws {
        let data = Data(#"{"type":"command","payload":{"id":"00000000-0000-0000-0000-000000000002","paneId":"%1","command":{"startTerminalStream":{"_0":{"scrollbackLines":2500}}},"timestamp":"2026-08-05T00:00:00Z"}}"#.utf8)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        let message = try decoder.decode(WebSocketMessage.self, from: data)

        guard case let .command(command) = message else {
            Issue.record("Expected command")
            return
        }
        guard case let .startTerminalStream(spec) = command.command else {
            Issue.record("Expected startTerminalStream")
            return
        }
        #expect(command.paneId == "%1")
        #expect(spec.scrollbackLines == 2_500)
    }
}
