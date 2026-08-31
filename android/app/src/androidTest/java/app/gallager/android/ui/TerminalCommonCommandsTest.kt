package app.gallager.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.gallager.android.model.PaneSummary
import app.gallager.android.terminal.TerminalRender
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TerminalCommonCommandsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun menuShowsRequestedCommandsAndRunsTheSelectionWithEnter() {
        val sent = mutableListOf<String>()
        showTerminal(sent)

        composeRule.onNodeWithContentDescription("Common commands").performClick()
        composeRule.onNodeWithText("Vaka directory").assertIsDisplayed()
        composeRule.onNodeWithText("Claude · skip permissions").assertIsDisplayed()
        composeRule.onNodeWithText("Codex · ModHub").assertIsDisplayed()
        composeRule.onNodeWithText("Codex", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithText("Codex · ModHub").performClick()
        assertEquals(
            listOf("codex --profile modhub --dangerously-bypass-approvals-and-sandbox\r"),
            sent,
        )
    }

    @Test
    fun eachShortcutUsesTheExpectedShellCommand() {
        val sent = mutableListOf<String>()
        showTerminal(sent)
        val expected = linkedMapOf(
            "Vaka directory" to "cd ~/llm-deveplop/vaka\r",
            "Claude · skip permissions" to "claude --dangerously-skip-permissions\r",
            "Codex · ModHub" to
                "codex --profile modhub --dangerously-bypass-approvals-and-sandbox\r",
            "Codex" to "codex --dangerously-bypass-approvals-and-sandbox\r",
        )

        expected.forEach { (label, command) ->
            composeRule.onNodeWithContentDescription("Common commands").performClick()
            composeRule.onNodeWithText(label, useUnmergedTree = true).performClick()
            assertEquals("wrong payload for $label", command, sent.removeAt(sent.lastIndex))
        }
    }

    private fun showTerminal(sent: MutableList<String>) {
        composeRule.setContent {
            GallagerTheme {
                TerminalScreen(
                    pane = PaneSummary(
                        paneId = "%commands",
                        sessionName = "commands-test",
                        windowIndex = 0,
                        paneIndex = 0,
                        windowName = "zsh",
                        terminalTitle = "zsh",
                        currentPath = "/tmp",
                        gitBranch = null,
                        pluginId = null,
                        state = "active",
                        customDescription = null,
                        customEmoji = null,
                    ),
                    terminalContent = TerminalRender(text = "$ ", columns = 80, rows = 24),
                    connected = true,
                    commandInProgress = false,
                    commandFeedback = null,
                    onBack = {},
                    onSend = { sent += it.toString(Charsets.UTF_8) },
                    onRequestHistory = { false },
                    onRefreshTerminal = {},
                    onCreateWindow = {},
                    onSplit = {},
                    onCloseWindow = {},
                    onCloseSession = {},
                    onFeedbackShown = {},
                )
            }
        }
    }
}
