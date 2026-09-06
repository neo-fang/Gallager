package app.gallager.android.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsProperties
import app.gallager.android.model.PaneSummary
import app.gallager.android.terminal.TerminalRender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TerminalMultilineInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun newlineExpandsInputAndIsInsertedWithoutTerminalEnter() {
        val sent = mutableListOf<String>()
        showTerminal(sent)
        val input = composeRule.onNodeWithTag("terminal-input")

        input.performTextInput("first line")
        val singleLineHeight = input.fetchSemanticsNode().boundsInRoot.height
        input.performKeyInput { pressKey(Key.Enter) }
        input.performTextInput("second line")
        input.assertTextEquals("first line\nsecond line")
        val expandedHeight = input.fetchSemanticsNode().boundsInRoot.height
        assertTrue("a newline must expand the terminal input", expandedHeight > singleLineHeight)

        composeRule.onNodeWithContentDescription("Insert text without Enter").performClick()
        assertEquals(listOf("first line\nsecond line"), sent)
        assertEquals(
            "",
            input.fetchSemanticsNode().config[SemanticsProperties.EditableText].text,
        )
    }

    @Test
    fun longInputStopsGrowingAfterFiveVisibleLines() {
        showTerminal(mutableListOf())
        val input = composeRule.onNodeWithTag("terminal-input")

        input.performTextInput((1..5).joinToString("\n") { "line $it" })
        val fiveLineHeight = input.fetchSemanticsNode().boundsInRoot.height
        input.performTextInput("\nline 6\nline 7")
        val sevenLineHeight = input.fetchSemanticsNode().boundsInRoot.height

        input.assertTextEquals((1..7).joinToString("\n") { "line $it" })
        assertEquals(fiveLineHeight, sevenLineHeight, 1f)
    }

    private fun showTerminal(sent: MutableList<String>) {
        composeRule.setContent {
            GallagerTheme {
                TerminalScreen(
                    pane = PaneSummary(
                        paneId = "%input",
                        sessionName = "input-test",
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
