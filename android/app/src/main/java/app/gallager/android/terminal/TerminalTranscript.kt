package app.gallager.android.terminal

import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput
import com.termux.terminal.TextStyle
import com.termux.terminal.WcWidth

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
 * Stateful xterm/VT screen used by the Android companion.
 *
 * Terminal streams are screen mutations rather than append-only text. Claude
 * and Codex rely on alternate buffers, DEC origin/wrap modes, scroll regions,
 * character sets, and partial-line erases. Termux's maintained emulator owns
 * that state machine; this class only converts its visible cell buffer into the
 * lightweight styled model consumed by Compose.
 */
class TerminalTranscript(
    initialColumns: Int = DEFAULT_COLUMNS,
    initialRows: Int = DEFAULT_ROWS,
) {
    private var columns = initialColumns.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
    private var rows = initialRows.coerceIn(MIN_ROWS, MAX_ROWS)
    private var emulator = createEmulator()

    fun reset(bytes: ByteArray, columns: Int? = null, rows: Int? = null) {
        this.columns = (columns ?: this.columns).coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        this.rows = (rows ?: this.rows).coerceIn(MIN_ROWS, MAX_ROWS)
        emulator = createEmulator()
        feed(bytes)
    }

    fun feed(bytes: ByteArray) {
        if (bytes.isNotEmpty()) emulator.append(bytes, bytes.size)
    }

    fun resize(columns: Int, rows: Int) {
        val newColumns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val newRows = rows.coerceIn(MIN_ROWS, MAX_ROWS)
        if (newColumns == this.columns && newRows == this.rows) return
        this.columns = newColumns
        this.rows = newRows
        emulator.resize(newColumns, newRows, CELL_WIDTH_PIXELS, CELL_HEIGHT_PIXELS)
    }

    fun value(): String = render().text

    fun render(): TerminalRender {
        val screen = emulator.screen
        val renderedRows = (0 until rows).map { rowIndex ->
            val internalRow = screen.externalToInternalRow(rowIndex)
            val row = screen.allocateFullLineIfNecessary(internalRow)
            val glyphs = mutableListOf<StyledGlyph>()
            var charIndex = 0
            var column = 0

            while (charIndex < row.spaceUsed && column < columns) {
                val first = row.mText[charIndex++]
                val codePoint = if (
                    Character.isHighSurrogate(first) &&
                    charIndex < row.spaceUsed &&
                    Character.isLowSurrogate(row.mText[charIndex])
                ) {
                    Character.toCodePoint(first, row.mText[charIndex++])
                } else {
                    first.code
                }
                val width = WcWidth.width(codePoint)
                if (width <= 0) {
                    if (glyphs.isNotEmpty()) {
                        val last = glyphs.last()
                        glyphs[glyphs.lastIndex] = last.copy(
                            text = last.text + String(Character.toChars(codePoint)),
                        )
                    }
                    continue
                }

                val encodedStyle = row.getStyle(column)
                val style = decodeStyle(encodedStyle)
                val invisible = TextStyle.decodeEffect(encodedStyle) and
                    TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE != 0
                val glyphText = if (invisible) {
                    " ".repeat(width.coerceAtMost(2))
                } else {
                    String(Character.toChars(codePoint))
                }
                glyphs += StyledGlyph(
                    text = glyphText,
                    style = style,
                    visible = codePoint != SPACE || style.background != null || style.underline,
                )
                column += width
            }

            val lastVisible = glyphs.indexOfLast { it.visible }
            if (lastVisible < 0) emptyList() else glyphs.subList(0, lastVisible + 1).toList()
        }

        val lastVisibleRow = renderedRows.indexOfLast { it.isNotEmpty() }
        if (lastVisibleRow < 0) return TerminalRender()

        val text = StringBuilder()
        val spans = mutableListOf<TerminalStyleSpan>()
        for (rowIndex in 0..lastVisibleRow) {
            var runStyle: TerminalStyle? = null
            var runStart = text.length
            for (glyph in renderedRows[rowIndex]) {
                if (glyph.style != runStyle) {
                    appendStyleSpan(spans, runStart, text.length, runStyle)
                    runStyle = glyph.style
                    runStart = text.length
                }
                text.append(glyph.text)
            }
            appendStyleSpan(spans, runStart, text.length, runStyle)
            if (rowIndex < lastVisibleRow) text.append('\n')
        }
        return TerminalRender(text.toString(), spans)
    }

    private fun createEmulator(): TerminalEmulator = TerminalEmulator(
        NoOpTerminalOutput,
        columns,
        rows,
        CELL_WIDTH_PIXELS,
        CELL_HEIGHT_PIXELS,
        TRANSCRIPT_ROWS,
        null,
    ).also { terminal ->
        // Match Gallager's existing palette while retaining Termux's full SGR
        // and true-colour handling.
        ANSI_COLORS.forEachIndexed { index, color ->
            terminal.mColors.mCurrentColors[index] = color
        }
        ANSI_BRIGHT_COLORS.forEachIndexed { index, color ->
            terminal.mColors.mCurrentColors[index + ANSI_COLORS.size] = color
        }
        terminal.mColors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND] = DEFAULT_FOREGROUND
        terminal.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND] = DEFAULT_BACKGROUND
    }

    private fun decodeStyle(encoded: Long): TerminalStyle {
        val effects = TextStyle.decodeEffect(encoded)
        val foreground = resolveColor(TextStyle.decodeForeColor(encoded), TextStyle.COLOR_INDEX_FOREGROUND)
        val background = resolveColor(TextStyle.decodeBackColor(encoded), TextStyle.COLOR_INDEX_BACKGROUND)
        return TerminalStyle(
            foreground = foreground,
            background = background,
            bold = effects and TextStyle.CHARACTER_ATTRIBUTE_BOLD != 0,
            dim = effects and TextStyle.CHARACTER_ATTRIBUTE_DIM != 0,
            italic = effects and TextStyle.CHARACTER_ATTRIBUTE_ITALIC != 0,
            underline = effects and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE != 0,
            inverse = effects and TextStyle.CHARACTER_ATTRIBUTE_INVERSE != 0,
        )
    }

    private fun resolveColor(value: Int, defaultIndex: Int): Int? = when {
        value == defaultIndex -> null
        value in emulator.mColors.mCurrentColors.indices -> emulator.mColors.mCurrentColors[value]
        else -> value
    }

    private data class StyledGlyph(
        val text: String,
        val style: TerminalStyle,
        val visible: Boolean,
    )

    private object NoOpTerminalOutput : TerminalOutput() {
        override fun write(data: ByteArray, offset: Int, count: Int) = Unit
        override fun titleChanged(oldTitle: String?, newTitle: String?) = Unit
        override fun onCopyTextToClipboard(text: String?) = Unit
        override fun onPasteTextFromClipboard() = Unit
        override fun onBell() = Unit
        override fun onColorsChanged() = Unit
    }

    companion object {
        private const val DEFAULT_COLUMNS = 120
        private const val DEFAULT_ROWS = 40
        private const val MIN_COLUMNS = 2
        private const val MIN_ROWS = 2
        private const val MAX_COLUMNS = 500
        private const val MAX_ROWS = 200
        private const val TRANSCRIPT_ROWS = 2_000
        private const val CELL_WIDTH_PIXELS = 8
        private const val CELL_HEIGHT_PIXELS = 16
        private const val SPACE = 0x20
        private const val DEFAULT_FOREGROUND = 0xFFE2E8F0.toInt()
        private const val DEFAULT_BACKGROUND = 0xFF181818.toInt()

        private fun appendStyleSpan(
            spans: MutableList<TerminalStyleSpan>,
            start: Int,
            end: Int,
            style: TerminalStyle?,
        ) {
            if (style != null && style != TerminalStyle() && end > start) {
                spans += TerminalStyleSpan(start, end, style)
            }
        }

        private val ANSI_COLORS = intArrayOf(
            0xFF000000.toInt(),
            0xFFCD3131.toInt(),
            0xFF0DBC79.toInt(),
            0xFFE5E510.toInt(),
            0xFF2472C8.toInt(),
            0xFFBC3FBC.toInt(),
            0xFF11A8CD.toInt(),
            0xFFE5E5E5.toInt(),
        )
        private val ANSI_BRIGHT_COLORS = intArrayOf(
            0xFF666666.toInt(),
            0xFFF14C4C.toInt(),
            0xFF23D18B.toInt(),
            0xFFF5F543.toInt(),
            0xFF3B8EEA.toInt(),
            0xFFD670D6.toInt(),
            0xFF29B8DB.toInt(),
            0xFFFFFFFF.toInt(),
        )
    }
}
