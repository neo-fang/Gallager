package app.gallager.android.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TerminalTranscriptTest {
    @Test
    fun removesAnsiAndHandlesCarriageReturnRedraw() {
        val transcript = TerminalTranscript()
        transcript.feed("\u001B[32mWorking\u001B[0m 10%\rDone\u001B[K\n".toByteArray())

        assertEquals("Done", transcript.value())
        assertFalse(transcript.value().contains('\u001B'))
    }

    @Test
    fun resetReplacesPreviousBuffer() {
        val transcript = TerminalTranscript()
        transcript.feed("old".toByteArray())
        transcript.reset("new".toByteArray())

        assertEquals("new", transcript.value())
    }

    @Test
    fun cursorPositioningKeepsFullScreenLayout() {
        val transcript = TerminalTranscript()
        transcript.reset(
            "\u001B[2J\u001B[2;3HHello\u001B[4;1HBottom".toByteArray(),
            columns = 12,
            rows = 4,
        )

        assertEquals("\n  Hello\n\nBottom", transcript.value())
    }

    @Test
    fun escapeSequenceCanSpanNetworkChunks() {
        val transcript = TerminalTranscript(initialColumns = 12, initialRows = 3)
        transcript.feed("Top\u001B[".toByteArray())
        transcript.feed("2;1HSecond".toByteArray())

        assertEquals("Top\nSecond", transcript.value())
    }

    @Test
    fun preservesAnsiForegroundBackgroundAndEmphasis() {
        val transcript = TerminalTranscript(initialColumns = 40, initialRows = 2)
        transcript.feed(
            (
                "\u001B[90mWrite tests\u001B[0m " +
                    "\u001B[1;38;2;34;211;238;48;5;235mCodex\u001B[0m"
                ).toByteArray(),
        )

        val rendered = transcript.render()
        assertEquals("Write tests Codex", rendered.text)
        assertEquals(2, rendered.spans.size)
        assertEquals(0xFF666666.toInt(), rendered.spans[0].style.foreground)
        assertEquals(0xFF22D3EE.toInt(), rendered.spans[1].style.foreground)
        assertEquals(0xFF262626.toInt(), rendered.spans[1].style.background)
        assertEquals(true, rendered.spans[1].style.bold)
    }

    @Test
    fun redrawUpdatesBothCharacterAndStyle() {
        val transcript = TerminalTranscript(initialColumns = 20, initialRows = 2)
        transcript.feed("\u001B[31mError\r\u001B[32mReady\u001B[K".toByteArray())

        val rendered = transcript.render()
        assertEquals("Ready", rendered.text)
        assertEquals(0xFF0DBC79.toInt(), rendered.spans.single().style.foreground)
    }
}
