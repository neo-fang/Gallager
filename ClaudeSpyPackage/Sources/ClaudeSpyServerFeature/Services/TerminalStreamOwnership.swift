/// Tracks which paired viewers own a terminal stream subscription.
///
/// A viewer may retry `StartTerminalStream`; ownership must remain idempotent
/// instead of behaving like a reference count.
struct TerminalStreamOwnership: Equatable, Sendable {
    enum Removal: Equatable, Sendable {
        case notSubscribed
        case retained(Int)
        case empty
    }

    private var viewerIds: Set<String>

    init(viewerId: String) {
        viewerIds = [viewerId]
    }

    var count: Int {
        viewerIds.count
    }

    var subscribers: Set<String> {
        viewerIds
    }

    func contains(_ viewerId: String) -> Bool {
        viewerIds.contains(viewerId)
    }

    @discardableResult
    mutating func subscribe(_ viewerId: String) -> Bool {
        viewerIds.insert(viewerId).inserted
    }

    mutating func unsubscribe(_ viewerId: String) -> Removal {
        guard viewerIds.remove(viewerId) != nil else {
            return .notSubscribed
        }
        return viewerIds.isEmpty ? .empty : .retained(viewerIds.count)
    }
}
