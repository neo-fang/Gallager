/// Tracks whether a terminal view has already requested its host stream.
///
/// The first request must not send `StopTerminalStream`: another viewer may be
/// the only current subscriber. Later requests replace this view's previous
/// subscription with stop/start so reconnects refresh the complete screen and
/// keep the host's subscriber count balanced.
package struct TerminalStreamRecoveryPolicy: Equatable {
    package enum StartMode: Equatable {
        case initial
        case replaceExisting
    }

    package enum SuccessfulStartResolution: Equatable {
        case ready
        case retryReplacement
        case failMissingInitialState
    }

    package private(set) var hasRequestedStream = false
    package private(set) var hasRetriedUnexpectedEnd = false

    package init() { }

    package mutating func nextStartMode() -> StartMode {
        defer { hasRequestedStream = true }
        return hasRequestedStream ? .replaceExisting : .initial
    }

    /// Allows one automatic replacement when an established stream ends while
    /// its viewer is still connected. Further ends require an explicit retry,
    /// preventing a broken host from creating an unbounded command loop.
    package mutating func shouldRetryUnexpectedEnd(isConnected: Bool) -> Bool {
        guard isConnected, !hasRetriedUnexpectedEnd else { return false }
        hasRetriedUnexpectedEnd = true
        return true
    }

    package static func resolveSuccessfulStart(
        hasInitialState: Bool,
        canRetry: Bool
    ) -> SuccessfulStartResolution {
        if hasInitialState {
            return .ready
        } else if canRetry {
            return .retryReplacement
        } else {
            return .failMissingInitialState
        }
    }
}
