import ClaudeSpyNetworking
import Testing
@testable import ClaudeSpyFeature

@Suite("Agent background monitoring policy")
struct AgentBackgroundMonitoringPolicyTests {
    @Test("A stale idle state does not finish a newly submitted turn")
    func staleIdleDoesNotFinish() {
        #expect(
            AgentBackgroundMonitoringPolicy.decision(
                for: .idle,
                phase: .waitingForAgent
            ) == .keep(.waitingForAgent)
        )
    }

    @Test("Working then idle completes the monitored turn")
    func workingThenIdleCompletes() {
        let started = AgentBackgroundMonitoringPolicy.decision(
            for: .working,
            phase: .waitingForAgent
        )
        #expect(started == .keep(.working))
        #expect(
            AgentBackgroundMonitoringPolicy.decision(
                for: .idle,
                phase: .working
            ) == .finish(.completed)
        )
    }

    @Test("Terminal states finish even when working update was missed", arguments: [
        AgentState.doneWorking(summary: "done"),
        AgentState.awaitingPermission(
            PermissionRequest(title: "Shell", description: "Run pwd"),
            requestID: "permission"
        ),
    ])
    func terminalStateFinishes(_ state: AgentState) {
        guard case .finish = AgentBackgroundMonitoringPolicy.decision(
            for: state,
            phase: .waitingForAgent
        ) else {
            Issue.record("Expected terminal Agent state to finish monitoring")
            return
        }
    }

    @Test("Only non-empty free-text turns start monitoring")
    func eligibleResponses() {
        #expect(AgentBackgroundMonitoringPolicy.shouldStart(for: .prompt(text: "run tests")))
        #expect(AgentBackgroundMonitoringPolicy.shouldStart(for: .replyAfterStop(text: "continue")))
        #expect(!AgentBackgroundMonitoringPolicy.shouldStart(for: .prompt(text: " \n ")))
        #expect(!AgentBackgroundMonitoringPolicy.shouldStart(for: .replyAfterStop(text: "")))
        #expect(!AgentBackgroundMonitoringPolicy.shouldStart(for: .approvePlan(
            decision: .approve,
            editedPlan: nil
        )))
    }

    @Test("Terminal input starts only after a meaningful line is submitted")
    func terminalInputAccumulation() {
        var input = AgentPromptInputAccumulator()

        let empty = input.consume([.space, .enter])
        let draft = input.consume([.text("run tests")])
        let submitted = input.consume([.enter])
        let repeatedEnter = input.consume([.enter])
        let cleared = input.consume([.text("discard me"), .ctrl("u")])
        let clearedEnter = input.consume([.enter])
        let embeddedNewline = input.consume([.text("continue\n")])

        #expect(!empty)
        #expect(!draft)
        #expect(submitted)
        #expect(!repeatedEnter)
        #expect(!cleared)
        #expect(!clearedEnter)
        #expect(embeddedNewline)
    }

    @Test("Only idle Agent turns accept terminal prompts")
    func terminalPromptStates() {
        #expect(AgentBackgroundMonitoringPolicy.canSubmitPrompt(from: .idle))
        #expect(AgentBackgroundMonitoringPolicy.canSubmitPrompt(from: .doneWorking(summary: nil)))
        #expect(!AgentBackgroundMonitoringPolicy.canSubmitPrompt(from: .working))
    }
}
