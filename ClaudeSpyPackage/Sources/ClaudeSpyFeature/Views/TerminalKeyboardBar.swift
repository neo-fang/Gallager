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
                .font(.caption.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: 18)
                .contentShape(Rectangle())
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
            .controlSize(.mini)
            .disabled(!isEnabled)
            .accessibilityIdentifier("terminal-keyboard-control")
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(.bar)
            .overlay(alignment: .top) {
                Divider()
            }
        }
    }
#endif
