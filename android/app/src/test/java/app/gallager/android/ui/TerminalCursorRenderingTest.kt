package app.gallager.android.ui

import app.gallager.android.terminal.TerminalCursor
import app.gallager.android.terminal.TerminalRender
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalCursorRenderingTest {
    @Test
    fun cursorHighlightsTheCharacterAtItsCurrentPosition() {
        val rendered = terminalAnnotatedString(
            TerminalRender(
                text = "hello",
                cursor = TerminalCursor(offset = 3, length = 1),
            ),
        )

        assertEquals("hello", rendered.text)
        assertEquals(3, rendered.spanStyles.last().start)
        assertEquals(4, rendered.spanStyles.last().end)
        assertEquals(TerminalDefaultForeground, rendered.spanStyles.last().item.background)
        assertEquals(TerminalBackground, rendered.spanStyles.last().item.color)
    }

    @Test
    fun cursorAtBlankCellPadsOnlyTheVisualTerminalText() {
        val rendered = terminalAnnotatedString(
            TerminalRender(
                text = "hello",
                cursor = TerminalCursor(offset = 5, length = 0, padding = 2),
            ),
        )

        assertEquals("hello   ", rendered.text)
        assertEquals(7, rendered.spanStyles.last().start)
        assertEquals(8, rendered.spanStyles.last().end)
    }
}
