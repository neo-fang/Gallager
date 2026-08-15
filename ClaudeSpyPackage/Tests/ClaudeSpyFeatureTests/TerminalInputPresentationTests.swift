import Testing
@testable import ClaudeSpyFeature

@Suite("Terminal input presentation")
struct TerminalInputPresentationTests {
    @Test("The copy sheet suppresses toolbar-controlled terminal input")
    func copySheetSuppressesToolbarInput() {
        #expect(TerminalInputPresentation.isInteractive(
            showKeyboardButton: true,
            keyboardRequested: true,
            isActive: true,
            isCopyPresented: false
        ))
        #expect(!TerminalInputPresentation.isInteractive(
            showKeyboardButton: true,
            keyboardRequested: true,
            isActive: true,
            isCopyPresented: true
        ))
    }

    @Test("The copy sheet also suppresses parent-controlled multi-pane input")
    func copySheetSuppressesParentInput() {
        #expect(TerminalInputPresentation.isInteractive(
            showKeyboardButton: false,
            keyboardRequested: false,
            isActive: true,
            isCopyPresented: false
        ))
        #expect(!TerminalInputPresentation.isInteractive(
            showKeyboardButton: false,
            keyboardRequested: false,
            isActive: true,
            isCopyPresented: true
        ))
    }

    @Test("Inactive terminals never accept input")
    func inactiveTerminal() {
        #expect(!TerminalInputPresentation.isInteractive(
            showKeyboardButton: true,
            keyboardRequested: true,
            isActive: false,
            isCopyPresented: false
        ))
        #expect(!TerminalInputPresentation.isInteractive(
            showKeyboardButton: false,
            keyboardRequested: false,
            isActive: false,
            isCopyPresented: false
        ))
    }
}
