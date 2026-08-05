#if os(macOS)
    import ClaudeSpyCommon
    import Dependencies
    import Testing
    @testable import ClaudeSpyServerFeature

    @MainActor
    @Suite("Sidebar appearance settings")
    struct SidebarAppearanceSettingsTests {
        @Test("Selected-session highlight defaults off and persists")
        func selectedSessionHighlightPersistence() {
            let preferences = PreferencesService.inMemory()

            let settings = withDependencies {
                $0[PreferencesService.self] = preferences
            } operation: {
                AppSettings()
            }

            #expect(settings.highlightSelectedSidebarSession == false)
            settings.highlightSelectedSidebarSession = true

            let reloaded = withDependencies {
                $0[PreferencesService.self] = preferences
            } operation: {
                AppSettings()
            }

            #expect(reloaded.highlightSelectedSidebarSession == true)
        }
    }
#endif
