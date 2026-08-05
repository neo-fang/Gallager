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

    @Test
    fun wideChineseCharactersKeepCursorColumnsAligned() {
        val transcript = TerminalTranscript(initialColumns = 12, initialRows = 2)
        transcript.feed("测试AB".toByteArray())
        transcript.feed("\u001B[1;5HOK\u001B[K".toByteArray())

        assertEquals("测试OK", transcript.value())
    }

    @Test
    fun supplementaryEmojiIsOneWideGlyphAndUtf8CanSpanChunks() {
        val transcript = TerminalTranscript(initialColumns = 12, initialRows = 2)
        val bytes = "A🟡B".toByteArray()
        transcript.feed(bytes.copyOfRange(0, 3))
        transcript.feed(bytes.copyOfRange(3, bytes.size))
        transcript.feed("\u001B[1;4HX\u001B[K".toByteArray())

        assertEquals("A🟡X", transcript.value())
        assertFalse(transcript.value().contains('\uFFFD'))
    }

    @Test
    fun ignoresCharsetDesignationAndDeviceControlStrings() {
        val transcript = TerminalTranscript(initialColumns = 20, initialRows = 2)
        transcript.feed("A\u001B(B\u001BPprivate payload\u001B\\B".toByteArray())

        assertEquals("AB", transcript.value())
    }

    @Test
    fun scrollRegionAndLineInsertionMatchFullScreenTuiUpdates() {
        val transcript = TerminalTranscript(initialColumns = 8, initialRows = 4)
        transcript.feed("one\r\ntwo\r\nthree\r\nfour".toByteArray())
        transcript.feed("\u001B[2;4r\u001B[3;1H\u001B[LNEW".toByteArray())

        assertEquals("one\ntwo\nNEW\nthree", transcript.value())
    }

    @Test
    fun resizeAppliesHostDimensionChangesWithoutDiscardingContent() {
        val transcript = TerminalTranscript(initialColumns = 8, initialRows = 2)
        transcript.feed("hello".toByteArray())
        transcript.resize(columns = 12, rows = 3)
        transcript.feed("\u001B[3;1Hbottom".toByteArray())

        assertEquals("hello\n\nbottom", transcript.value())
    }
}
