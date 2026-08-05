#if os(macOS)
    import AppKit
    import ClaudeSpyNetworking
    import Testing
    @testable import ClaudeSpyServerFeature

    @MainActor
    struct MacTerminalInputTests {
        @Test("Terminal input bridge receives focus")
        func terminalInputBridgeReceivesFocus() {
            let (window, view) = makeTerminalWindow()

            #expect(view.focusTerminal())
            #expect(window.firstResponder === view)
        }

        @Test("IME composition commits terminal text")
        func imeCompositionCommitsTerminalText() {
            let (_, view) = makeTerminalWindow()
            var input: [TmuxKey] = []
            view.onInput = { input.append(contentsOf: $0) }

            view.setMarkedText(
                "zhong",
                selectedRange: NSRange(location: 5, length: 0),
                replacementRange: NSRange(location: NSNotFound, length: 0)
            )
            #expect(view.hasMarkedText())
            #expect(input.isEmpty)

            view.insertText(
                "中",
                replacementRange: NSRange(location: NSNotFound, length: 0)
            )
            #expect(!view.hasMarkedText())
            #expect(input == [.text("中")])
        }

        @Test("Arrow commands are forwarded to SwiftTerm")
        func arrowCommandsAreForwarded() {
            let (_, view) = makeTerminalWindow()
            var input: [TmuxKey] = []
            view.onInput = { input.append(contentsOf: $0) }

            view.doCommand(by: #selector(NSResponder.moveLeft(_:)))
            view.doCommand(by: #selector(NSResponder.moveRight(_:)))
            view.doCommand(by: #selector(NSResponder.moveUp(_:)))
            view.doCommand(by: #selector(NSResponder.moveDown(_:)))

            #expect(input == [.left, .right, .up, .down])
        }

        @Test("Function-key events stay on SwiftTerm's native path")
        func functionKeyEventsUseNativePath() throws {
            let (_, view) = makeTerminalWindow()
            var input: [TmuxKey] = []
            view.onInput = { input.append(contentsOf: $0) }

            view.keyDown(with: try functionKeyEvent(NSLeftArrowFunctionKey, keyCode: 123))
            view.keyDown(with: try functionKeyEvent(NSHomeFunctionKey, keyCode: 115))
            view.keyDown(with: try functionKeyEvent(NSEndFunctionKey, keyCode: 119))

            #expect(input == [.left, .home, .end])
        }

        @Test("Fn arrow commands map to line boundaries")
        func fnArrowCommandsMapToLineBoundaries() {
            let (_, view) = makeTerminalWindow()
            var input: [TmuxKey] = []
            view.onInput = { input.append(contentsOf: $0) }

            view.doCommand(by: #selector(NSResponder.moveToBeginningOfLine(_:)))
            view.doCommand(by: #selector(NSResponder.moveToEndOfLine(_:)))

            #expect(input == [.home, .end])
        }

        @Test("Persistent tmux input encodes text and named keys")
        func persistentTmuxInputEncoding() {
            let commands = PaneStreamManager.sendKeysCommands(
                paneId: "%7",
                keys: [.text("A中"), .left, .right]
            )

            #expect(commands == [
                "send-keys -t '%7' -H 41 e4 b8 ad",
                "send-keys -t '%7' Left Right",
            ])
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

        private func functionKeyEvent(_ functionKey: Int, keyCode: UInt16) throws -> NSEvent {
            let characters = String(try #require(UnicodeScalar(functionKey)))
            return try #require(NSEvent.keyEvent(
                with: .keyDown,
                location: .zero,
                modifierFlags: [.function],
                timestamp: 0,
                windowNumber: 0,
                context: nil,
                characters: characters,
                charactersIgnoringModifiers: characters,
                isARepeat: false,
                keyCode: keyCode
            ))
        }
    }
#endif
