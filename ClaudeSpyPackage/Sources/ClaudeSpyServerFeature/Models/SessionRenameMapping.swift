import Foundation

/// A tmux session rename inferred from two pane snapshots.
///
/// Pane IDs survive `rename-session`, while session/window textual IDs change.
/// Requiring the complete pane-ID set to match prevents a normal `move-window`
/// from being mistaken for a session rename.
struct SessionRenameMapping: Equatable, Sendable {
    let oldName: String
    let newName: String
    let windowIDs: [String: String]

    static func detect(from oldPanes: [PaneInfo], to newPanes: [PaneInfo]) -> [SessionRenameMapping] {
        let oldPaneIDs = Dictionary(grouping: oldPanes, by: \.sessionName)
            .mapValues { Set($0.map(\.paneId)) }
        let newPaneIDs = Dictionary(grouping: newPanes, by: \.sessionName)
            .mapValues { Set($0.map(\.paneId)) }

        return detectNames(from: oldPaneIDs, to: newPaneIDs).map { oldName, newName in
            let oldByPaneID = Dictionary(uniqueKeysWithValues: oldPanes
                .filter { $0.sessionName == oldName }
                .map { ($0.paneId, $0.windowId) })
            let windowIDs = Dictionary(
                newPanes
                    .filter { $0.sessionName == newName }
                    .compactMap { pane in
                        oldByPaneID[pane.paneId].map { ($0, pane.windowId) }
                    },
                uniquingKeysWith: { _, latest in latest }
            )

            return SessionRenameMapping(oldName: oldName, newName: newName, windowIDs: windowIDs)
        }
    }

    static func detectNames(
        from oldPaneIDs: [String: Set<String>],
        to newPaneIDs: [String: Set<String>]
    ) -> [(oldName: String, newName: String)] {
        let removedNames = oldPaneIDs.keys.filter { newPaneIDs[$0] == nil }
        let addedNames = newPaneIDs.keys.filter { oldPaneIDs[$0] == nil }

        return removedNames.compactMap { oldName in
            guard let paneIDs = oldPaneIDs[oldName], !paneIDs.isEmpty else { return nil }
            let matchingOldNames = removedNames.filter { oldPaneIDs[$0] == paneIDs }
            let matchingNewNames = addedNames.filter { newPaneIDs[$0] == paneIDs }
            guard matchingOldNames.count == 1, matchingNewNames.count == 1 else { return nil }
            return (oldName, matchingNewNames[0])
        }
        .sorted { $0.oldName < $1.oldName }
    }
}
