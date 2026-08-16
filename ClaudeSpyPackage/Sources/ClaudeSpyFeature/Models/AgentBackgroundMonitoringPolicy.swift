import ClaudeSpyNetworking
import Foundation

/// Platform-neutral lifecycle for a user-initiated Agent run monitored by iOS.
///
/// The first `.idle` may describe the turn that existed before the prompt was
/// submitted, so it cannot finish a newly-created monitor. Once `.working` has
/// arrived, a later idle state is a valid completion fallback.
public enum AgentBackgroundMonitoringPolicy {
    public enum Phase: Sendable, Equatable {
        case waitingForAgent
        case working
    }

    public enum FinishReason: Sendable, Equatable {
        case completed
        case waitingForInput
    }

    public enum Decision: Sendable, Equatable {
        case keep(Phase)
        case finish(FinishReason)
    }

    public static func decision(for state: AgentState, phase: Phase) -> Decision {
        switch state {
        case .working:
            return .keep(.working)

        case .awaitingPlanApproval,
             .awaitingPermission,
             .awaitingReplies:
            return .finish(.waitingForInput)

        case .doneWorking:
            return .finish(.completed)

        case .idle:
            return phase == .working ? .finish(.completed) : .keep(.waitingForAgent)
        }
    }

    /// Only a non-empty structured free-text turn represents new Agent work.
    /// Terminal-key submissions are classified separately by
    /// `AgentPromptInputAccumulator` because they do not produce a response.
    public static func shouldStart(for response: AgentResponse) -> Bool {
        let text: String
        switch response {
        case let .prompt(value), let .replyAfterStop(value):
            text = value
        case .permission,
             .askUserQuestion,
             .approvePlan:
            return false
        }
        return !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    public static func canSubmitPrompt(from state: AgentState) -> Bool {
        switch state {
        case .idle,
             .doneWorking:
            return true
        case .working,
             .awaitingPlanApproval,
             .awaitingPermission,
             .awaitingReplies:
            return false
        }
    }
}

/// Tracks enough of one terminal input line to distinguish a real prompt from
/// an empty Enter. It deliberately clears on editing operations it cannot model
/// safely; missing one monitor is preferable to starting an orphaned task.
public struct AgentPromptInputAccumulator: Sendable, Equatable {
    private var text = ""

    public init() { }

    public mutating func consume(_ keys: [TmuxKey]) -> Bool {
        var submittedPrompt = false

        for key in keys {
            switch key {
            case let .text(value):
                for character in value {
                    if character.isNewline {
                        submittedPrompt = submitCurrentLine() || submittedPrompt
                    } else {
                        text.append(character)
                    }
                }
            case .space:
                text.append(" ")
            case .shiftEnter:
                text.append("\n")
            case .backspace:
                if !text.isEmpty { text.removeLast() }
            case .enter:
                submittedPrompt = submitCurrentLine() || submittedPrompt
            case let .ctrl(character) where character == "c" || character == "u" || character == "w":
                text = ""
            case .escape,
                 .tab,
                 .backtab,
                 .delete,
                 .up,
                 .down,
                 .left,
                 .right,
                 .home,
                 .end,
                 .pageUp,
                 .pageDown,
                 .ctrl,
                 .alt,
                 .ctrlAlt,
                 .delay:
                break
            }
        }

        return submittedPrompt
    }

    private mutating func submitCurrentLine() -> Bool {
        defer { text = "" }
        return !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
