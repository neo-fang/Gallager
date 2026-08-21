package app.gallager.android.terminal

/** Encodes terminal mouse-wheel input using xterm's SGR mouse protocol. */
object TerminalMouseScroll {
    private const val WHEEL_UP = 64
    private const val WHEEL_DOWN = 65
    private const val MAX_EVENTS_PER_BATCH = 64

    /** Android gesture mapping: an upward finger drag reveals older output. */
    fun encodeVerticalDrag(
        deltaY: Float,
        column: Int,
        row: Int,
        columns: Int,
        rows: Int,
        events: Int,
    ): ByteArray = encode(
        revealOlder = deltaY < 0f,
        column = column,
        row = row,
        columns = columns,
        rows = rows,
        events = events,
    )

    /**
     * Creates one or more wheel events at a zero-based terminal cell.
     * A wheel-up event asks full-screen TUIs such as Claude Code and Codex to
     * reveal older content; wheel-down returns toward newer content.
     */
    fun encode(
        revealOlder: Boolean,
        column: Int,
        row: Int,
        columns: Int,
        rows: Int,
        events: Int,
    ): ByteArray {
        if (columns <= 0 || rows <= 0 || events <= 0) return byteArrayOf()
        val button = if (revealOlder) WHEEL_UP else WHEEL_DOWN
        val x = column.coerceIn(0, columns - 1) + 1
        val y = row.coerceIn(0, rows - 1) + 1
        val event = "\u001B[<$button;$x;${y}M"
        return buildString {
            repeat(events.coerceAtMost(MAX_EVENTS_PER_BATCH)) { append(event) }
        }.toByteArray(Charsets.UTF_8)
    }
}
