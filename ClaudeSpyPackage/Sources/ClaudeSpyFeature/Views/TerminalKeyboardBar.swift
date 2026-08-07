#if os(iOS)
    import SwiftUI

    /// A dedicated terminal-input control that stays outside terminal content.
    struct TerminalKeyboardBar: View {
        let keyboardVisible: Bool
        let isEnabled: Bool
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                Label(
                    keyboardVisible ? "Hide Keyboard" : "Input",
                    symbol: keyboardVisible ? .keyboardChevronCompactDown : .keyboard
                )
                .font(.callout.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
            .disabled(!isEnabled)
            .accessibilityIdentifier("terminal-keyboard-control")
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(.bar)
            .overlay(alignment: .top) {
                Divider()
            }
        }
    }
#endif
