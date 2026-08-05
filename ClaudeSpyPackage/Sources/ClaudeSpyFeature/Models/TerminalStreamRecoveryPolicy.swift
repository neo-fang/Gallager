/// Tracks whether a terminal view has already requested its host stream.
///
/// The first request must not send `StopTerminalStream`: another viewer may be
/// the only current subscriber. Later requests replace this view's previous
/// subscription with stop/start so reconnects refresh the complete screen and
/// keep the host's subscriber count balanced.
struct TerminalStreamRecoveryPolicy: Equatable {
    enum StartMode: Equatable {
        case initial
        case replaceExisting
    }

    private(set) var hasRequestedStream = false

    mutating func nextStartMode() -> StartMode {
        defer { hasRequestedStream = true }
        return hasRequestedStream ? .replaceExisting : .initial
    }
}
