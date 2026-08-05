package app.gallager.android.terminal

data class TerminalStyle(
    val foreground: Int? = null,
    val background: Int? = null,
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val inverse: Boolean = false,
)

data class TerminalStyleSpan(
    val start: Int,
    val end: Int,
    val style: TerminalStyle,
)

data class TerminalRender(
    val text: String = "",
    val spans: List<TerminalStyleSpan> = emptyList(),
)

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
    private var styles = newStyleScreen()
    private var currentStyle = TerminalStyle()
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
        styles = newStyleScreen()
        currentStyle = TerminalStyle()
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

    fun value(): String = render().text

    fun render(): TerminalRender {
        val visibleLengths = screen.indices.map { row ->
            (columns - 1 downTo 0).firstOrNull { column ->
                screen[row][column] != ' ' || styles[row][column].background != null
            }?.plus(1) ?: 0
        }
        val lastRow = visibleLengths.indexOfLast { it > 0 }
        if (lastRow < 0) return TerminalRender()

        val text = StringBuilder()
        val spans = mutableListOf<TerminalStyleSpan>()
        for (row in 0..lastRow) {
            var runStart = text.length
            var runStyle: TerminalStyle? = null
            for (column in 0 until visibleLengths[row]) {
                val style = styles[row][column]
                if (style != runStyle) {
                    runStyle?.takeIf { it != TerminalStyle() }?.let {
                        spans += TerminalStyleSpan(runStart, text.length, it)
                    }
                    runStart = text.length
                    runStyle = style
                }
                text.append(screen[row][column])
            }
            runStyle?.takeIf { it != TerminalStyle() }?.let {
                spans += TerminalStyleSpan(runStart, text.length, it)
            }
            if (row < lastRow) text.append('\n')
        }
        return TerminalRender(text.toString(), spans.filter { it.end > it.start })
    }

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
            'm' -> applyGraphicRendition(params.map { it ?: 0 })
            'l', 'n', 'r', 't' -> Unit
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
        styles[cursorRow][cursorColumn] = currentStyle
        cursorColumn++
    }

    private fun moveDown() {
        if (cursorRow < rows - 1) {
            cursorRow++
        } else {
            screen.removeAt(0)
            styles.removeAt(0)
            screen.add(blankRow())
            styles.add(blankStyleRow())
        }
    }

    private fun eraseDisplay(mode: Int) {
        when (mode) {
            0 -> {
                eraseRange(cursorRow, cursorColumn, columns)
                for (row in cursorRow + 1 until rows) eraseRange(row, 0, columns)
            }
            1 -> {
                for (row in 0 until cursorRow) eraseRange(row, 0, columns)
                eraseRange(cursorRow, 0, cursorColumn + 1)
            }
            2, 3 -> screen.indices.forEach { eraseRange(it, 0, columns) }
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
        val styleLine = styles[cursorRow]
        for (column in cursorColumn until columns - amount) {
            line[column] = line[column + amount]
            styleLine[column] = styleLine[column + amount]
        }
        for (column in columns - amount until columns) {
            line[column] = ' '
            styleLine[column] = eraseStyle()
        }
    }

    private fun insertCharacters(count: Int) {
        val amount = count.coerceIn(1, columns - cursorColumn)
        val line = screen[cursorRow]
        val styleLine = styles[cursorRow]
        for (column in columns - 1 downTo cursorColumn + amount) {
            line[column] = line[column - amount]
            styleLine[column] = styleLine[column - amount]
        }
        for (column in cursorColumn until cursorColumn + amount) {
            line[column] = ' '
            styleLine[column] = eraseStyle()
        }
    }

    private fun eraseRange(row: Int, start: Int, end: Int) {
        for (column in start.coerceAtLeast(0) until end.coerceAtMost(columns)) {
            screen[row][column] = ' '
            styles[row][column] = eraseStyle()
        }
    }

    private fun applyGraphicRendition(parameters: List<Int>) {
        val values = parameters.ifEmpty { listOf(0) }
        var index = 0
        while (index < values.size) {
            when (val code = values[index]) {
                0 -> currentStyle = TerminalStyle()
                1 -> currentStyle = currentStyle.copy(bold = true)
                2 -> currentStyle = currentStyle.copy(dim = true)
                3 -> currentStyle = currentStyle.copy(italic = true)
                4, 21 -> currentStyle = currentStyle.copy(underline = true)
                7 -> currentStyle = currentStyle.copy(inverse = true)
                22 -> currentStyle = currentStyle.copy(bold = false, dim = false)
                23 -> currentStyle = currentStyle.copy(italic = false)
                24 -> currentStyle = currentStyle.copy(underline = false)
                27 -> currentStyle = currentStyle.copy(inverse = false)
                in 30..37 -> currentStyle = currentStyle.copy(foreground = ANSI_COLORS[code - 30])
                38 -> readExtendedColor(values, index)?.let { (color, consumed) ->
                    currentStyle = currentStyle.copy(foreground = color)
                    index += consumed
                }
                39 -> currentStyle = currentStyle.copy(foreground = null)
                in 40..47 -> currentStyle = currentStyle.copy(background = ANSI_COLORS[code - 40])
                48 -> readExtendedColor(values, index)?.let { (color, consumed) ->
                    currentStyle = currentStyle.copy(background = color)
                    index += consumed
                }
                49 -> currentStyle = currentStyle.copy(background = null)
                in 90..97 -> currentStyle = currentStyle.copy(foreground = ANSI_BRIGHT_COLORS[code - 90])
                in 100..107 -> currentStyle = currentStyle.copy(background = ANSI_BRIGHT_COLORS[code - 100])
            }
            index++
        }
    }

    private fun readExtendedColor(values: List<Int>, start: Int): Pair<Int, Int>? = when {
        values.getOrNull(start + 1) == 5 && values.getOrNull(start + 2) != null ->
            indexedColor(values[start + 2]) to 2
        values.getOrNull(start + 1) == 2 && values.getOrNull(start + 4) != null -> {
            val red = values[start + 2].coerceIn(0, 255)
            val green = values[start + 3].coerceIn(0, 255)
            val blue = values[start + 4].coerceIn(0, 255)
            argb(red, green, blue) to 4
        }
        else -> null
    }

    private fun indexedColor(index: Int): Int = when (val value = index.coerceIn(0, 255)) {
        in 0..7 -> ANSI_COLORS[value]
        in 8..15 -> ANSI_BRIGHT_COLORS[value - 8]
        in 16..231 -> {
            val offset = value - 16
            val levels = intArrayOf(0, 95, 135, 175, 215, 255)
            argb(levels[offset / 36], levels[(offset / 6) % 6], levels[offset % 6])
        }
        else -> {
            val gray = 8 + (value - 232) * 10
            argb(gray, gray, gray)
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
        screen.indices.forEach { eraseRange(it, 0, columns) }
        cursorRow = 0
        cursorColumn = 0
    }

    private fun newScreen(): MutableList<CharArray> = MutableList(rows) { blankRow() }

    private fun newStyleScreen(): MutableList<Array<TerminalStyle>> =
        MutableList(rows) { blankStyleRow() }

    private fun blankRow(): CharArray = CharArray(columns) { ' ' }

    private fun blankStyleRow(): Array<TerminalStyle> = Array(columns) { TerminalStyle() }

    private fun eraseStyle(): TerminalStyle = TerminalStyle(background = currentStyle.background)

    private enum class ParserState { TEXT, ESCAPE, CSI, OSC, OSC_ESCAPE }

    companion object {
        private const val DEFAULT_COLUMNS = 120
        private const val DEFAULT_ROWS = 40
        private const val MAX_COLUMNS = 500
        private const val MAX_ROWS = 200
        private const val MAX_SEQUENCE_LENGTH = 128

        private fun argb(red: Int, green: Int, blue: Int): Int =
            (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

        private val ANSI_COLORS = intArrayOf(
            argb(0, 0, 0),
            argb(205, 49, 49),
            argb(13, 188, 121),
            argb(229, 229, 16),
            argb(36, 114, 200),
            argb(188, 63, 188),
            argb(17, 168, 205),
            argb(229, 229, 229),
        )
        private val ANSI_BRIGHT_COLORS = intArrayOf(
            argb(102, 102, 102),
            argb(241, 76, 76),
            argb(35, 209, 139),
            argb(245, 245, 67),
            argb(59, 142, 234),
            argb(214, 112, 214),
            argb(41, 184, 219),
            argb(255, 255, 255),
        )
    }
}
