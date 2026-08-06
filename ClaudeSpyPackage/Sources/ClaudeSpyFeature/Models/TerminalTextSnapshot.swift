#if canImport(SwiftTerm)
    import Foundation
    import SwiftTerm

    /// An immutable copy of the text currently retained by a terminal buffer.
    ///
    /// The snapshot deliberately does not observe subsequent terminal output. A stable
    /// value lets iOS keep a user's selection in place while the live pane continues to run.
    struct TerminalTextSnapshot: Identifiable, Equatable {
        let id = UUID()
        let text: String

        @MainActor
        init?(terminal: Terminal) {
            var lines: [String] = []
            var row = terminal.buffer.totalLinesTrimmed

            while let line = terminal.getScrollInvariantLine(row: row) {
                lines.append(line.translateToString(
                    trimRight: true,
                    skipNullCellsFollowingWide: true
                ))
                row += 1
            }

            var text = lines.joined(separator: "\n")

            while text.last?.isNewline == true {
                text.removeLast()
            }

            guard text.contains(where: { !$0.isWhitespace }) else { return nil }
            self.text = text
        }
    }
#endif
