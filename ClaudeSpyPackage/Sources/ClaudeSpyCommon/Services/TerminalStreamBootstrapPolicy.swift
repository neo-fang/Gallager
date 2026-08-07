/// Coordinates the two independent events required before a remote terminal
/// can be presented: a valid initial snapshot and the host's start-command
/// acknowledgement.
package struct TerminalStreamBootstrapPolicy: Equatable {
    package private(set) var acceptsInitialState = false
    package private(set) var hasInitialState = false
    package private(set) var hasStartAcknowledgement = false

    package var isReady: Bool {
        hasInitialState && hasStartAcknowledgement
    }

    package init() { }

    /// Starts a new attempt while rejecting snapshots left in flight by an old
    /// stream. Call `willSendStartRequest()` only after any replacement stop
    /// command has completed.
    package mutating func beginAttempt() {
        acceptsInitialState = false
        hasInitialState = false
        hasStartAcknowledgement = false
    }

    package mutating func willSendStartRequest() {
        acceptsInitialState = true
        hasInitialState = false
        hasStartAcknowledgement = false
    }

    /// Returns true when this is the first valid snapshot for the active start
    /// request. Broadcast refreshes and duplicate snapshots are ignored.
    package mutating func receiveInitialState() -> Bool {
        guard acceptsInitialState, !hasInitialState else { return false }
        acceptsInitialState = false
        hasInitialState = true
        return true
    }

    package mutating func receiveStartAcknowledgement() {
        hasStartAcknowledgement = true
    }
}
