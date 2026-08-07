enum TerminalInputPresentation {
    static func isInteractive(
        showKeyboardButton: Bool,
        keyboardRequested: Bool,
        isActive: Bool,
        isCopyPresented: Bool
    ) -> Bool {
        guard isActive, !isCopyPresented else { return false }
        return showKeyboardButton ? keyboardRequested : true
    }
}
