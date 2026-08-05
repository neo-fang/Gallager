import Testing
@testable import ClaudeSpyFeature

@Suite("Terminal stream recovery policy")
struct TerminalStreamRecoveryPolicyTests {
    @Test("The first request does not stop another viewer's stream")
    func initialRequest() {
        var policy = TerminalStreamRecoveryPolicy()

        #expect(policy.nextStartMode() == .initial)
        #expect(policy.hasRequestedStream)
    }

    @Test("Reconnect and retry replace the previous subscription")
    func subsequentRequests() {
        var policy = TerminalStreamRecoveryPolicy()

        #expect(policy.nextStartMode() == .initial)
        #expect(policy.nextStartMode() == .replaceExisting)
        #expect(policy.nextStartMode() == .replaceExisting)
    }
}
