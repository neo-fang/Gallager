import Testing
@testable import ClaudeSpyCommon

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

    @Test("A successful start without initial state retries once")
    func missingInitialStateRecoveryIsBounded() {
        let first = TerminalStreamRecoveryPolicy.resolveSuccessfulStart(
            hasInitialState: false,
            canRetry: true
        )
        let second = TerminalStreamRecoveryPolicy.resolveSuccessfulStart(
            hasInitialState: false,
            canRetry: false
        )

        #expect(first == .retryReplacement)
        #expect(second == .failMissingInitialState)
        #expect(
            TerminalStreamRecoveryPolicy.resolveSuccessfulStart(
                hasInitialState: true,
                canRetry: true
            ) == .ready
        )
    }
}
