package ui.input.textbox

import ui.Terminal
import ui.input.InputStreamClosedException
import ui.input.StringListener
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import debug.Debug
import debug.VerbosityLevel
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.max

/**
 * String input listener with UI. When the user types, and this object is listening for input, the text shows up on the
 * screen. If overflow is enabled, the user can continue typing, even when the box is full.
 *
 * The text box will continuously overwrite everything in the rectangle that it occupies, given with the constructor parameters.
 *
 * **Note:** Due to the large number of (optional) parameters, it is highly recommended you use [TextBoxBuilder] to
 * create `TextBox` instances. This generally looks more readable and will continue to work if this class' constructor
 * changes in a future version.
 *
 * @param size The rows and columns of the text box.
 * @param position The top-left character of the text box.
 * @param overflow If `true`, the user can type infinitely, and the text will scroll in one of two ways. If the height
 * (`size.rows`) is exactly 1, the text will shift left with each character typed, once the edge of the box is reached.
 * If the height is more than 1, the text will shift upwards, with every row. The default is `true`.
 * @param placeHolder The text the box contains if no text has been entered yet. The default is nothing.
 * @param showOverflowIndicator If `true`, an overflow indicator will be shown if there is text that does not fit inside
 * the text box. The default is `true`.
 * @param overflowIndicator The character to use for the overflow indicator. The default is `null`, which will select a
 * character based on the overflow type used (horizontal or vertical). The overflow type, in turn, depends on the size of
 * the text box.
 * @param wordBreak If `true`, [overflow] is `true` _and_ the box height is at least 2 rows, the whole word will be put
 * on the next line when the edge of the text box is reached, provided that the word fits on one line of the text box.
 * A word is defined as any non-zero number of consecutive characters that are not whitespace. The default is `true`.
 * @param characterLimit Set a character limit. If [overflow] is `false`, the character limit will automatically become
 * the number of characters that can fit inside the box. The default is `0`, which evaluates to infinite.
 * @param padding Number of characters to keep blank within the text box. Handy in combination with a border to keep
 * space between the typed text and the border. This padding is not counted from the border, so if a border is set, without
 * padding, the text will likely overwrite (parts of) the border.
 * @param border If set to anything but [TextBoxBorder.None], a border will be put around the text box, provided the text
 * box is large enough to fit a border. This means the minimum size of the text box is 3 rows by 3 columns for a
 * 1-character text box.
 * @param characterMask If set to anything other than `null`, instead of printing the character that was typed, the given
 * character is printed instead. The character that was actually typed is still stored and returned. In less technical
 * terms, this makes the text box act like a password field if a character like '*' is provided.
 * @param startDisabled When `true`, the textbox will not start listening for user input until [enable] is called.
 * `false` by default.
 * @param keepContents If `true`, the contents of the textbox will be kept after the user presses enter. The default is
 * `false`. If `false`, the string will be returned to any thread listening on the [getString] method when the user
 * presses enter, and the contents will be reset to an empty string.
 * @param clearBeforeListening If `true`, the contents of the textbox will be cleared when the [getString] method is
 * first called. This ensures there is nothing left in the textbox when the user starts typing.
 *
 * @author JTC
 */
class TextBox(private val terminal: Terminal,
              val size: TerminalSize,
              val position: TerminalPosition,
              val overflow: Boolean = true,
              val placeHolder: String = "",
              val showOverflowIndicator: Boolean = true,
              val overflowIndicator: Char = '\u0000',
              val wordBreak: Boolean = true,
              val characterLimit: Int = 0,
              val padding: TextBoxPadding = TextBoxPadding(0,0,0,0),
              val border: TextBoxBorder = TextBoxBorder.None,
              val characterMask: Char = '\u0000',
              val startDisabled: Boolean = false,
              val keepContents: Boolean = false,
              val clearBeforeListening: Boolean = true) : StringListener() {
    companion object {
        const val defaultHorizontalOverflowIndicator = '<'
        const val defaultVerticalOverflowIndicator = '^'
    }

    /**
     * If `true`, this textbox will listen for new input. If `false`, it the contents of this textbox will not change
     * when the user types.
     */
    var enabled = true
        private set
    private var overflowType = OverflowType.Disabled
    private var textAreaSize = 0
    private var textAreaWidth = 0
    private var textAreaHeight = 0
    private var textAreaStartCoord = TerminalPosition(0,0)
    private var maximumCharAmount = 0
    private var overflowIndChar = overflowIndicator

    init {
        if (size.rows < 1 || size.columns < 1) throw IllegalArgumentException("Size cannot be less than 1x1")
        if (position.row < 0 || position.column < 0)
            throw IllegalArgumentException("Position out of bounds for coordinates (${position.column}, ${position.row})")

        if (padding.top < 0 || padding.bottom < 0 || padding.right < 0 || padding.left < 0)
            throw IllegalArgumentException("Padding cannot be negative")

        // Calculate text area size
        textAreaWidth = (size.columns - (padding.left + padding.right))
        textAreaHeight = (size.rows - (padding.top + padding.bottom))
        textAreaSize = textAreaHeight * textAreaWidth
        textAreaStartCoord = TerminalPosition(position.column + padding.left, position.row + padding.top)

        if (textAreaSize < 1) {
            throw IllegalArgumentException("Too much padding for this textbox size (usable text area would be $textAreaSize characters)")
        } else if (textAreaSize < 3 && overflow && showOverflowIndicator) {
            throw IllegalArgumentException("Not enough space for the overflow indicator - Consider disabling it")
        }

        // Check if the border actually fits
        if (size.rows <= (border.impl.top + border.impl.bottom) ||
                size.columns <= (border.impl.right + border.impl.left))
            throw IllegalArgumentException("Textbox too small to fit the border")

        // Set overflow type
        if (!overflow) {
            overflowType = OverflowType.Disabled
            maximumCharAmount = if (characterLimit == 0) {
                // If the character limit is infinite, but overflow is disabled, we still have a limit
                textAreaSize
            } else {
                // If the character limit is set, we should check if the limit isn't more than the maximum
                min(characterLimit, textAreaSize)
            }
        } else {
            maximumCharAmount = characterLimit
            overflowType = if (textAreaHeight == 1) {
                OverflowType.HorizontalScrolling
            } else {
                OverflowType.VerticalScrolling
            }
        }

        if (overflowIndicator == '\u0000') {
            if (overflowType == OverflowType.HorizontalScrolling) {
                overflowIndChar = defaultHorizontalOverflowIndicator
            } else if (overflowType == OverflowType.VerticalScrolling) {
                overflowIndChar = defaultVerticalOverflowIndicator
            }
        } else {
            overflowIndChar = overflowIndicator
        }

        enabled = !startDisabled

        printTextbox()
    }

    /**
     * Enables the textbox. If the textbox was already enabled, this function has no effect.
     *
     * When the textbox is enabled, it will listen for user input. If it is disabled, no changes will be made to the
     * internal buffer that stores the user input.
     */
    fun enable() {
        enabled = true
    }

    /**
     * Disables the textbox. If the textbox was already disabled, this function has no effect.
     *
     * When the textbox is enabled, it will listen for user input. If it is disabled, no changes will be made to the
     * internal buffer that stores the user input.
     */
    fun disable() {
        enabled = false
    }

    override fun sendKey(key: KeyStroke) {
        lock.lock()
        // Default single-key behaviour
        buffer = key
        keyReceived.signal()
        lock.unlock()

        keyReceived(key)
    }

    private fun keyReceived(key: KeyStroke) {
        if (key.keyType == KeyType.EOF) {
            stringLock.lock()
            error = true
            stringReady.signal()
            stringLock.unlock()
            return
        }
        if (!enabled) return

        stringLock.lock()
        when (key.keyType) {
            KeyType.Character -> {
                if (stringBuffer.length < maximumCharAmount || maximumCharAmount == 0) {
                    stringBuffer += key.character
                }
            }
            KeyType.Backspace -> stringBuffer = stringBuffer.dropLast(1)
            KeyType.Enter -> stringReady.signal()
            else -> {
                stringLock.unlock()
                return
            }
        }
        stringLock.unlock()
        printTextbox()
    }

    override fun getString(): String {
        if (error) throw InputStreamClosedException("The terminal has closed")
        stringLock.lock()
        if (clearBeforeListening) stringBuffer = ""
        try {
            stringReady.await()
            if (error) {
                stringLock.unlock()
                throw InputStreamClosedException("The terminal has closed")
            }
            val resultString = stringBuffer
            if (!keepContents) stringBuffer = ""
            stringLock.unlock()
            return resultString
        } catch (e: InterruptedException) {
            stringLock.unlock()
            Debug.debugMessage(
                VerbosityLevel.WARNING, "TextBox.gS",
                "Thread interrupted while waiting for string")
            Thread.currentThread().interrupt()
            return ""
        }
    }

    /**
     * Set the contents of the textbox to the given string. Also prints the new contents to the screen.
     * @param string The string to set as the new contents.
     * @throws IllegalArgumentException If the given string exceeds the maximum string length of the textbox.
     */
    fun setContents(string: String) {
        if (maximumCharAmount != 0 && string.length > maximumCharAmount)
            throw IllegalArgumentException("Given string exceeds the maximum contents' length")
        stringLock.lock()
        stringBuffer = string
        stringLock.unlock()
        printTextbox()
    }

    private fun printTextbox() {
        synchronized(terminal) {
            clearTextBoxArea()

            // Print the border
            border.impl.printBorder(terminal, size, position)

            stringLock.lock()
            val stringBufferCopy = String(stringBuffer.toByteArray())
            stringLock.unlock()
            val textToPrint = if (characterMask != '\u0000') {
                characterMask.toString().repeat(stringBufferCopy.length)
            } else if (stringBufferCopy.isEmpty()) {
                placeHolder
            } else {
                stringBufferCopy
            }
            // Printing depends heavily on the overflow type
            when (overflowType) {
                OverflowType.Disabled -> printTextNoOverflow(textToPrint)
                OverflowType.HorizontalScrolling -> printTextHorizontalOverflow(textToPrint)
                OverflowType.VerticalScrolling -> printTextVerticalOverflow(textToPrint)
            }
            terminal.terminal.flush()
        }
    }

    private fun printTextNoOverflow(text: String) {
        if (text.length <= maximumCharAmount) {
            if (wordBreak) {
                val textWithWordBreak = wordBreakText(text)
                // If the word-break text fits inside the textbox, print it
                if (textWithWordBreak.size <= textAreaHeight) {
                    for (row in textWithWordBreak.indices) {
                        terminal.terminal.cursorPosition = textAreaStartCoord.withRelativeRow(row)
                        terminal.terminal.putString(textWithWordBreak[row])
                    }
                    // Exit
                    return
                }
            }

            // If the word-break text takes up more rows than allowed, just use character breaking
            printTextCharacterBreak(text)
        } else {
            Debug.debugMessage(VerbosityLevel.ERROR, "TextBox.pTB", "Too much text to print!")
        }
    }

    private fun printTextHorizontalOverflow(text: String) {
        terminal.terminal.cursorPosition = textAreaStartCoord
        if (text.length > textAreaWidth) {
            val lowerBound = text.length - textAreaWidth
            terminal.terminal.putString(text.slice(lowerBound until text.length))
            if (showOverflowIndicator) {
                terminal.terminal.cursorPosition = textAreaStartCoord
                terminal.terminal.putString("$overflowIndChar ")
            }
        } else {
            terminal.terminal.putString(text)
        }
    }

    private fun printTextVerticalOverflow(text: String) {
        if (wordBreak) {
            val rows = wordBreakText(text)
            val startingRow = max(rows.size - textAreaHeight, 0)
            for (row in 0 until textAreaHeight) {
                if (row >= rows.size) continue // Don't print if we have no more text to print
                terminal.terminal.cursorPosition = textAreaStartCoord.withRelativeRow(row)
                terminal.terminal.putString(rows[row + startingRow])
            }
            if (rows.size > textAreaHeight && showOverflowIndicator) {
                terminal.terminal.cursorPosition = textAreaStartCoord
                terminal.terminal.putString("$overflowIndChar ")
            }
        } else {
            printTextCharacterBreak(text)
            if (text.length > textAreaSize && showOverflowIndicator) {
                terminal.terminal.cursorPosition = textAreaStartCoord
                terminal.terminal.putString("$overflowIndChar ")
            }
        }
    }

    private fun wordBreakText(text: String): List<String> {
        val words = text.split(' ')
        if (words.isEmpty()) return emptyList()

        val textLines = ArrayList<String>()
        // We need to handle the first word separately, as it is the only one that is not preceded by a space.
        // If the first word fits on the first line, add it
        if (words[0].length <= textAreaWidth) {
            textLines.add(words[0])
        } else {
            for (offset in 0 until words[0].length step textAreaWidth) {
                if (words[0].length - offset <= textAreaWidth) {
                    // The last part of the word fits on one line
                    textLines.add(words[0].slice(offset until words[0].length))
                } else {
                    textLines.add(words[0].slice(offset until offset + textAreaWidth))
                }
            }
        }

        for (index in words.indices) {
            // Skip the first word
            if (index == 0) continue
            val word = words[index]

            val last = textLines.size - 1 // Last line in the lines array
            if (textLines[last].length + 1 + word.length <= textAreaWidth) {
                // The current word fits on the previous line, so add it there
                textLines[last] += " $word"
            } else if (word.length > textAreaWidth) {
                // The word won't fit on a new line either, so we might as well put as much as possible on the previous
                // line
                val inWordIndex = textAreaWidth - (textLines[last].length + 1)
                textLines[textLines.size - 1] += " ${word.slice(0 until inWordIndex)}"
                // Then do this until we've printed the whole word
                for (offset in inWordIndex until word.length step textAreaWidth) {
                    if (word.length - offset <= textAreaWidth) {
                        // The last part of the word fits on one line
                        textLines.add(word.slice(offset until word.length))
                    } else {
                        textLines.add(word.slice(offset until offset + textAreaWidth))
                    }
                }
            } else {
                // The word goes on a new line, and fits there. So we print it there, alone
                textLines.add(word)
            }
        }
        return textLines
    }

    private fun printTextCharacterBreak(text: String) {
        // Character break
        val totalRows = ceil(text.length / textAreaWidth.toDouble()).toInt()
        val startingRow = max(totalRows - textAreaHeight, 0)

        for (row in 0 until textAreaHeight) {
            terminal.terminal.cursorPosition = textAreaStartCoord.withRelativeRow(row)
            val lowerBound = row * textAreaWidth + (startingRow * textAreaWidth)
            val upperBound = lowerBound + textAreaWidth
            if (text.length < lowerBound) {
                /* Nothing more to print */
                break
            } else if (text.length < upperBound) {
                /* Not a full line to print anymore, so print everything */
                terminal.terminal.putString(text.slice(lowerBound until text.length))
            } else {
                terminal.terminal.putString(text.slice(lowerBound until upperBound))
            }
        }
    }

    private fun clearTextBoxArea() {
        // Clear this part of the terminal
        for (row in 0 until size.rows) {
            terminal.terminal.cursorPosition = position.withRelativeRow(row)
            terminal.terminal.putString(" ".repeat(size.columns))
        }
    }
}

private enum class OverflowType {
    Disabled,
    HorizontalScrolling,
    VerticalScrolling
}
