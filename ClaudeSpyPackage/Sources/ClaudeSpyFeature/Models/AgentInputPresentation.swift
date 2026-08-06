enum AgentInputPresentation {
    static func showsResponseForm(
        isBlocking: Bool,
        quickInputEnabled: Bool,
        keyboardActive: Bool
    ) -> Bool {
        isBlocking || (quickInputEnabled && !keyboardActive)
    }

    static func startsWithKeyboard(isAgentPane: Bool, quickInputEnabled: Bool) -> Bool {
        isAgentPane && !quickInputEnabled
    }
}
