package app.gallager.android.terminal

/**
 * Small VT transcript used by the Android MVP. It preserves readable terminal
 * text while handling the control sequences most coding agents use for redraws.
 * A full cell-grid renderer can replace this without changing the relay layer.
 */
class TerminalTranscript(private val maxChars: Int = 200_000) {
    private val text = StringBuilder()
    private var cursor = 0

    fun reset(bytes: ByteArray) {
        text.clear()
        cursor = 0
        feed(bytes)
    }

    fun feed(bytes: ByteArray) {
        val input = bytes.toString(Charsets.UTF_8)
        var index = 0
        while (index < input.length) {
            when (val char = input[index]) {
                '\u001b' -> index = consumeEscape(input, index)
                '\r' -> {
                    cursor = lineStart(cursor)
                    index++
                }
                '\b' -> {
                    cursor = maxOf(lineStart(cursor), cursor - 1)
                    index++
                }
                '\u0000' -> index++
                else -> {
                    if (char == '\n') {
                        insertOrOverwrite('\n')
                    } else if (!char.isISOControl()) {
                        insertOrOverwrite(char)
                    }
                    index++
                }
            }
        }
        trimIfNeeded()
    }

    fun value(): String = text.toString()

    private fun consumeEscape(input: String, start: Int): Int {
        if (start + 1 >= input.length) return input.length
        return when (input[start + 1]) {
            '[' -> consumeCsi(input, start + 2)
            ']' -> consumeOsc(input, start + 2)
            else -> minOf(input.length, start + 2)
        }
    }

    private fun consumeCsi(input: String, start: Int): Int {
        var index = start
        while (index < input.length && input[index].code !in 0x40..0x7e) index++
        if (index >= input.length) return input.length
        val command = input[index]
        val params = input.substring(start, index)
        when (command) {
            'J' -> if (params == "2" || params == "3") {
                text.clear()
                cursor = 0
            }
            'K' -> clearLineFromCursor()
            'G' -> cursor = lineStart(cursor) + ((params.toIntOrNull() ?: 1) - 1).coerceAtLeast(0)
            'H', 'f' -> {
                // Full-screen cursor positioning is intentionally approximated as
                // end-of-transcript; the next printable update remains readable.
                cursor = text.length
            }
        }
        return index + 1
    }

    private fun consumeOsc(input: String, start: Int): Int {
        var index = start
        while (index < input.length) {
            if (input[index] == '\u0007') return index + 1
            if (input[index] == '\u001b' && index + 1 < input.length && input[index + 1] == '\\') {
                return index + 2
            }
            index++
        }
        return input.length
    }

    private fun insertOrOverwrite(char: Char) {
        if (cursor < text.length && text[cursor] != '\n') {
            text.setCharAt(cursor, char)
        } else if (cursor <= text.length) {
            text.insert(cursor, char)
        } else {
            while (text.length < cursor) text.append(' ')
            text.append(char)
        }
        cursor++
    }

    private fun clearLineFromCursor() {
        val end = text.indexOf("\n", cursor).let { if (it == -1) text.length else it }
        if (end > cursor) text.delete(cursor, end)
    }

    private fun lineStart(position: Int): Int {
        val safe = position.coerceIn(0, text.length)
        return text.lastIndexOf("\n", maxOf(0, safe - 1)).let { if (it == -1) 0 else it + 1 }
    }

    private fun trimIfNeeded() {
        if (text.length <= maxChars) return
        val remove = text.length - maxChars
        text.delete(0, remove)
        cursor = maxOf(0, cursor - remove)
    }
}
