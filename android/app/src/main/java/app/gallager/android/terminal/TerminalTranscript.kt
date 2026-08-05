package app.gallager.android.terminal

/**
 * Small stateful VT screen used by the Android companion.
 *
 * Gallager's initial terminal snapshot and live chunks contain cursor movement
 * and erase commands, not a printable transcript. Keeping a cell screen lets
 * full-screen TUIs such as Codex redraw in place instead of collapsing all
 * positioned text into one line.
 */
class TerminalTranscript(
    initialColumns: Int = DEFAULT_COLUMNS,
    initialRows: Int = DEFAULT_ROWS,
) {
    private var columns = initialColumns.coerceIn(1, MAX_COLUMNS)
    private var rows = initialRows.coerceIn(1, MAX_ROWS)
    private var screen = newScreen()
    private var cursorRow = 0
    private var cursorColumn = 0
    private var savedRow = 0
    private var savedColumn = 0
    private var state = ParserState.TEXT
    private val sequence = StringBuilder()

    fun reset(bytes: ByteArray, columns: Int? = null, rows: Int? = null) {
        this.columns = (columns ?: this.columns).coerceIn(1, MAX_COLUMNS)
        this.rows = (rows ?: this.rows).coerceIn(1, MAX_ROWS)
        screen = newScreen()
        cursorRow = 0
        cursorColumn = 0
        savedRow = 0
        savedColumn = 0
        state = ParserState.TEXT
        sequence.clear()
        feed(bytes)
    }

    fun feed(bytes: ByteArray) {
        bytes.toString(Charsets.UTF_8).forEach(::consume)
    }

    fun value(): String = screen
        .joinToString("\n") { String(it).trimEnd() }
        .trimEnd('\n')

    private fun consume(char: Char) {
        when (state) {
            ParserState.TEXT -> consumeText(char)
            ParserState.ESCAPE -> consumeEscape(char)
            ParserState.CSI -> consumeCsi(char)
            ParserState.OSC -> consumeOsc(char)
            ParserState.OSC_ESCAPE -> {
                state = if (char == '\\') ParserState.TEXT else ParserState.OSC
            }
        }
    }

    private fun consumeText(char: Char) {
        when (char) {
            '\u001b' -> state = ParserState.ESCAPE
            '\r' -> cursorColumn = 0
            '\n', '\u000b', '\u000c' -> moveDown()
            '\b' -> cursorColumn = (cursorColumn - 1).coerceAtLeast(0)
            '\t' -> cursorColumn = (cursorColumn + (8 - cursorColumn % 8)).coerceAtMost(columns - 1)
            '\u0000', '\u0007' -> Unit
            else -> if (!char.isISOControl()) put(char)
        }
    }

    private fun consumeEscape(char: Char) {
        when (char) {
            '[' -> {
                sequence.clear()
                state = ParserState.CSI
            }
            ']' -> state = ParserState.OSC
            '7' -> {
                saveCursor()
                state = ParserState.TEXT
            }
            '8' -> {
                restoreCursor()
                state = ParserState.TEXT
            }
            'c' -> {
                clearScreen()
                state = ParserState.TEXT
            }
            else -> state = ParserState.TEXT
        }
    }

    private fun consumeCsi(char: Char) {
        if (char.code !in 0x40..0x7e) {
            if (sequence.length < MAX_SEQUENCE_LENGTH) sequence.append(char)
            return
        }

        val raw = sequence.toString()
        val params = raw.trimStart('?', '>', '!').split(';').map { it.toIntOrNull() }
        fun param(index: Int, fallback: Int = 1): Int = params.getOrNull(index) ?: fallback

        when (char) {
            'A' -> cursorRow = (cursorRow - param(0)).coerceAtLeast(0)
            'B', 'e' -> cursorRow = (cursorRow + param(0)).coerceAtMost(rows - 1)
            'C', 'a' -> cursorColumn = (cursorColumn + param(0)).coerceAtMost(columns - 1)
            'D' -> cursorColumn = (cursorColumn - param(0)).coerceAtLeast(0)
            'E' -> {
                cursorRow = (cursorRow + param(0)).coerceAtMost(rows - 1)
                cursorColumn = 0
            }
            'F' -> {
                cursorRow = (cursorRow - param(0)).coerceAtLeast(0)
                cursorColumn = 0
            }
            'G', '`' -> cursorColumn = (param(0) - 1).coerceIn(0, columns - 1)
            'd' -> cursorRow = (param(0) - 1).coerceIn(0, rows - 1)
            'H', 'f' -> {
                cursorRow = (param(0) - 1).coerceIn(0, rows - 1)
                cursorColumn = (param(1) - 1).coerceIn(0, columns - 1)
            }
            'J' -> eraseDisplay(param(0, 0))
            'K' -> eraseLine(param(0, 0))
            'X' -> eraseCharacters(param(0))
            'P' -> deleteCharacters(param(0))
            '@' -> insertCharacters(param(0))
            's' -> saveCursor()
            'u' -> restoreCursor()
            'h' -> if (raw.contains("1049") || raw.contains("1047") || raw == "?47") clearScreen()
            'l', 'm', 'n', 'r', 't' -> Unit
        }
        sequence.clear()
        state = ParserState.TEXT
    }

    private fun consumeOsc(char: Char) {
        when (char) {
            '\u0007' -> state = ParserState.TEXT
            '\u001b' -> state = ParserState.OSC_ESCAPE
        }
    }

    private fun put(char: Char) {
        if (cursorColumn >= columns) {
            cursorColumn = 0
            moveDown()
        }
        screen[cursorRow][cursorColumn] = char
        cursorColumn++
    }

    private fun moveDown() {
        if (cursorRow < rows - 1) {
            cursorRow++
        } else {
            screen.removeAt(0)
            screen.add(blankRow())
        }
    }

    private fun eraseDisplay(mode: Int) {
        when (mode) {
            0 -> {
                eraseRange(cursorRow, cursorColumn, columns)
                for (row in cursorRow + 1 until rows) screen[row].fill(' ')
            }
            1 -> {
                for (row in 0 until cursorRow) screen[row].fill(' ')
                eraseRange(cursorRow, 0, cursorColumn + 1)
            }
            2, 3 -> screen.forEach { it.fill(' ') }
        }
    }

    private fun eraseLine(mode: Int) {
        when (mode) {
            0 -> eraseRange(cursorRow, cursorColumn, columns)
            1 -> eraseRange(cursorRow, 0, cursorColumn + 1)
            2 -> screen[cursorRow].fill(' ')
        }
    }

    private fun eraseCharacters(count: Int) {
        eraseRange(cursorRow, cursorColumn, (cursorColumn + count).coerceAtMost(columns))
    }

    private fun deleteCharacters(count: Int) {
        val amount = count.coerceIn(1, columns - cursorColumn)
        val line = screen[cursorRow]
        for (column in cursorColumn until columns - amount) line[column] = line[column + amount]
        for (column in columns - amount until columns) line[column] = ' '
    }

    private fun insertCharacters(count: Int) {
        val amount = count.coerceIn(1, columns - cursorColumn)
        val line = screen[cursorRow]
        for (column in columns - 1 downTo cursorColumn + amount) line[column] = line[column - amount]
        for (column in cursorColumn until cursorColumn + amount) line[column] = ' '
    }

    private fun eraseRange(row: Int, start: Int, end: Int) {
        for (column in start.coerceAtLeast(0) until end.coerceAtMost(columns)) {
            screen[row][column] = ' '
        }
    }

    private fun saveCursor() {
        savedRow = cursorRow
        savedColumn = cursorColumn
    }

    private fun restoreCursor() {
        cursorRow = savedRow.coerceIn(0, rows - 1)
        cursorColumn = savedColumn.coerceIn(0, columns - 1)
    }

    private fun clearScreen() {
        screen.forEach { it.fill(' ') }
        cursorRow = 0
        cursorColumn = 0
    }

    private fun newScreen(): MutableList<CharArray> = MutableList(rows) { blankRow() }

    private fun blankRow(): CharArray = CharArray(columns) { ' ' }

    private enum class ParserState { TEXT, ESCAPE, CSI, OSC, OSC_ESCAPE }

    companion object {
        private const val DEFAULT_COLUMNS = 120
        private const val DEFAULT_ROWS = 40
        private const val MAX_COLUMNS = 500
        private const val MAX_ROWS = 200
        private const val MAX_SEQUENCE_LENGTH = 128
    }
}
