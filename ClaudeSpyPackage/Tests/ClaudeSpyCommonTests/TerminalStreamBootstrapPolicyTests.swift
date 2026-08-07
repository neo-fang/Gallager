import Testing
@testable import ClaudeSpyCommon

@Suite("Terminal stream bootstrap policy")
struct TerminalStreamBootstrapPolicyTests {
    @Test("Initial state alone does not reveal the terminal")
    func initialNeedsAcknowledgement() {
        var policy = TerminalStreamBootstrapPolicy()
        policy.beginAttempt()
        policy.willSendStartRequest()

        let accepted = policy.receiveInitialState()
        #expect(accepted)
        #expect(!policy.isReady)

        policy.receiveStartAcknowledgement()
        #expect(policy.isReady)
    }

    @Test("Acknowledgement may arrive before initial state")
    func acknowledgementNeedsInitialState() {
        var policy = TerminalStreamBootstrapPolicy()
        policy.beginAttempt()
        policy.willSendStartRequest()

        policy.receiveStartAcknowledgement()
        #expect(!policy.isReady)

        let accepted = policy.receiveInitialState()
        #expect(accepted)
        #expect(policy.isReady)
    }

    @Test("Replacement stop window rejects stale snapshots")
    func staleInitialStateIsRejected() {
        var policy = TerminalStreamBootstrapPolicy()
        policy.beginAttempt()

        let staleAccepted = policy.receiveInitialState()
        #expect(!staleAccepted)

        policy.willSendStartRequest()
        let accepted = policy.receiveInitialState()
        let duplicateAccepted = policy.receiveInitialState()
        #expect(accepted)
        #expect(!duplicateAccepted)
    }

    @Test("New attempt clears prior readiness")
    func newAttemptClearsReadyState() {
        var policy = TerminalStreamBootstrapPolicy()
        policy.willSendStartRequest()
        let accepted = policy.receiveInitialState()
        #expect(accepted)
        policy.receiveStartAcknowledgement()
        #expect(policy.isReady)

        policy.beginAttempt()
        #expect(!policy.isReady)
        #expect(!policy.acceptsInitialState)
    }
}
