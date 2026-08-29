package app.gallager.android.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import app.gallager.android.model.PaneSummary
import app.gallager.android.terminal.TerminalRender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TerminalScrollGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val transcript = (1..140).joinToString("\n") { line ->
        "HISTORY %03d — Android terminal scroll regression test".format(line)
    }

    private val pane = PaneSummary(
        paneId = "%test",
        sessionName = "scroll-test",
        windowIndex = 0,
        paneIndex = 0,
        windowName = "Claude Code",
        terminalTitle = "Claude Code",
        currentPath = "/tmp",
        gitBranch = null,
        pluginId = "claude-code",
        state = "active",
        customDescription = null,
        customEmoji = null,
    )

    @Test
    fun remoteTuiDragMovesContinuouslyBeforeRequestingMoreHistory() {
        val sent = mutableListOf<String>()
        showTerminal(sent)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val (value, max) = scrollPosition()
            max > 1_000f && value >= max - 2f
        }

        val (tail, max) = scrollPosition()
        swipeFinger(down = true, distance = 220f)
        val (afterFirstPull, _) = scrollPosition()

        assertTrue("a partial pull must move away from the tail", afterFirstPull < tail - 100f)
        assertTrue("a partial pull must not jump to the oldest row", afterFirstPull > 100f)
        assertEquals("the local snapshot must be consumed before remote wheel events", 0, sent.size)

        swipeFinger(down = false, distance = 110f)
        val (afterReverse, _) = scrollPosition()
        assertTrue("reversing must move toward newer output", afterReverse > afterFirstPull + 50f)
        assertTrue("reversing must not snap straight back to the tail", afterReverse < max - 20f)
        assertEquals(0, sent.size)
    }

    @Test
    fun remoteHistoryIsRequestedOnlyAfterLocalSnapshotReachesItsTopEdge() {
        val sent = mutableListOf<String>()
        showTerminal(sent)
        composeRule.waitUntil(timeoutMillis = 5_000) { scrollPosition().second > 1_000f }

        repeat(20) { swipeFinger(down = true, distance = 500f) }
        val (top, _) = scrollPosition()
        assertTrue("repeated pulls must reach the oldest locally cached row", top <= 2f)

        sent.clear()
        swipeFinger(down = true, distance = 180f)
        assertTrue("pulling beyond the top must emit terminal wheel-up", sent.isNotEmpty())
        assertTrue(sent.all { it.contains("\u001B[<64;") })

        sent.clear()
        swipeFinger(down = false, distance = 180f)
        assertTrue("the newly available local range is consumed before wheel-down", scrollPosition().first > 50f)
        assertEquals(0, sent.size)
    }

    private fun showTerminal(sent: MutableList<String>) {
        composeRule.setContent {
            GallagerTheme {
                TerminalScreen(
                    pane = pane,
                    terminalContent = TerminalRender(
                        text = transcript,
                        renderRevision = 1,
                        mouseTrackingActive = true,
                        columns = 120,
                        rows = 80,
                    ),
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

    private fun scrollPosition(): Pair<Float, Float> {
        val semantics = composeRule.onNodeWithTag("terminal-transcript")
            .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        return semantics.value() to semantics.maxValue()
    }

    private fun swipeFinger(down: Boolean, distance: Float) {
        composeRule.onNodeWithTag("terminal-transcript").performTouchInput {
            val start = center
            val endY = if (down) start.y + distance else start.y - distance
            swipe(
                start = start,
                end = Offset(start.x, endY.coerceIn(4f, height - 4f)),
                durationMillis = 450,
            )
        }
        composeRule.waitForIdle()
    }
}
