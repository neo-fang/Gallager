/// Viewer-local ordering for paired remote hosts.
public enum RemoteHostOrder {
    /// Moves one element to the target element's original position.
    ///
    /// This matches drag-and-drop intent in both directions: dropping a lower
    /// element on an upper target places it before the target, while dropping
    /// an upper element on a lower target places it after the target.
    public static func moving<Element>(
        _ elements: [Element],
        sourceID: String,
        targetID: String,
        id: (Element) -> String
    ) -> [Element] {
        guard
            sourceID != targetID,
            let sourceIndex = elements.firstIndex(where: { id($0) == sourceID }),
            let targetIndex = elements.firstIndex(where: { id($0) == targetID })
        else {
            return elements
        }

        var result = elements
        let source = result.remove(at: sourceIndex)
        result.insert(source, at: min(targetIndex, result.endIndex))
        return result
    }
}
