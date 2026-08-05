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
}
