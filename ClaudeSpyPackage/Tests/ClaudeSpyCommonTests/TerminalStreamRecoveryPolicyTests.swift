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

    @Test("An unexpected stream end retries at most once")
    func unexpectedEndRecoveryIsBounded() {
        var policy = TerminalStreamRecoveryPolicy()

        let disconnected = policy.shouldRetryUnexpectedEnd(isConnected: false)
        let firstConnected = policy.shouldRetryUnexpectedEnd(isConnected: true)
        let secondConnected = policy.shouldRetryUnexpectedEnd(isConnected: true)

        #expect(!disconnected)
        #expect(firstConnected)
        #expect(policy.hasRetriedUnexpectedEnd)
        #expect(!secondConnected)
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
