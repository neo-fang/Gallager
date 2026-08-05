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

    package private(set) var hasRequestedStream = false

    package init() { }

    package mutating func nextStartMode() -> StartMode {
        defer { hasRequestedStream = true }
        return hasRequestedStream ? .replaceExisting : .initial
    }
}
