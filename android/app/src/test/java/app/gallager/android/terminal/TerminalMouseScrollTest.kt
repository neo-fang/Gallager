package app.gallager.android.terminal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalMouseScrollTest {
    @Test
    fun upwardAndroidDragReturnsToNewerContent() {
        assertEquals(
            "\u001B[<65;40;12M",
            TerminalMouseScroll.encodeVerticalDrag(
                deltaY = -20f,
                column = 39,
                row = 11,
                columns = 80,
                rows = 24,
                events = 1,
            ).toString(Charsets.UTF_8),
        )
    }

    @Test
    fun downwardAndroidDragRequestsOlderContent() {
        assertEquals(
            "\u001B[<64;40;12M",
            TerminalMouseScroll.encodeVerticalDrag(
                deltaY = 20f,
                column = 39,
                row = 11,
                columns = 80,
                rows = 24,
                events = 1,
            ).toString(Charsets.UTF_8),
        )
    }

    @Test
    fun encodesWheelUpForOlderContentUsingOneBasedCoordinates() {
        assertEquals(
            "\u001B[<64;3;4M\u001B[<64;3;4M",
            TerminalMouseScroll.encode(
                revealOlder = true,
                column = 2,
                row = 3,
                columns = 80,
                rows = 24,
                events = 2,
            ).toString(Charsets.UTF_8),
        )
    }

    @Test
    fun encodesWheelDownAndClampsCoordinates() {
        assertEquals(
            "\u001B[<65;80;1M",
            TerminalMouseScroll.encode(
                revealOlder = false,
                column = 999,
                row = -4,
                columns = 80,
                rows = 24,
                events = 1,
            ).toString(Charsets.UTF_8),
        )
    }

    @Test
    fun ignoresEventsWithoutAValidTerminalGrid() {
        assertArrayEquals(
            byteArrayOf(),
            TerminalMouseScroll.encode(true, 0, 0, columns = 0, rows = 24, events = 1),
        )
    }
}
