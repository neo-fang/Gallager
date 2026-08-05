package app.gallager.android.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TerminalTranscriptTest {
    @Test
    fun removesAnsiAndHandlesCarriageReturnRedraw() {
        val transcript = TerminalTranscript()
        transcript.feed("\u001B[32mWorking\u001B[0m 10%\rDone\u001B[K\n".toByteArray())

        assertEquals("Done\n", transcript.value())
        assertFalse(transcript.value().contains('\u001B'))
    }

    @Test
    fun resetReplacesPreviousBuffer() {
        val transcript = TerminalTranscript()
        transcript.feed("old".toByteArray())
        transcript.reset("new".toByteArray())

        assertEquals("new", transcript.value())
    }
}
