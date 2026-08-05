#if os(macOS)
    import AppKit
    import ClaudeSpyNetworking
    import Testing
    @testable import ClaudeSpyServerFeature

    @MainActor
    struct MacTerminalInputTests {
        @Test("SwiftTerm receives terminal focus")
        func swiftTermReceivesTerminalFocus() {
            let (window, view) = makeTerminalWindow()

            #expect(view.focusTerminal())
            #expect(window.firstResponder === view.terminalView)
        }

        @Test("IME composition commits terminal text")
        func imeCompositionCommitsTerminalText() {
            let (_, view) = makeTerminalWindow()
            var input: [TmuxKey] = []
            view.onInput = { input.append(contentsOf: $0) }

            view.terminalView.setMarkedText(
                "zhong",
                selectedRange: NSRange(location: 5, length: 0),
                replacementRange: NSRange(location: NSNotFound, length: 0)
            )
            #expect(view.terminalView.hasMarkedText())
            #expect(input.isEmpty)

            view.terminalView.insertText(
                "中",
                replacementRange: NSRange(location: NSNotFound, length: 0)
            )
            #expect(!view.terminalView.hasMarkedText())
            #expect(input == [.text("中")])
        }

        private func makeTerminalWindow() -> (NSWindow, InteractiveTerminalView) {
            let frame = NSRect(x: 0, y: 0, width: 800, height: 600)
            let window = NSWindow(
                contentRect: frame,
                styleMask: [.borderless],
                backing: .buffered,
                defer: false
            )
            let view = InteractiveTerminalView(frame: frame)
            window.contentView = view
            return (window, view)
        }
    }
#endif
