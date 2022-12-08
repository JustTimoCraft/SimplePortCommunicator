package ui.input.textbox

import ui.Terminal
import ui.input.InputChecker
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize

/**
 * Builder class for a text box. Allows you to set a text box' properties to your liking before creating it with the
 * [build] method.
 */
class TextBoxBuilder {
    var position = TerminalPosition(-1, -1)
        private set
    var size = TerminalSize(0, 0)
        private set
    var overflow = true
        private set
    var placeHolder = ""
        private set
    var showOverflowIndicator: Boolean = true
        private set
    var overflowIndicator: Char = '\u0000'
        private set
    var wordBreak: Boolean = true
        private set
    var characterLimit: Int = 0
        private set
    var padding: TextBoxPadding = TextBoxPadding(0,0,0,0)
        private set
    var border: TextBoxBorder = TextBoxBorder.None
        private set
    var characterMask: Char = '\u0000'
        private set
    var startDisabled: Boolean = false
        private set
    var keepContents: Boolean = false
        private set
    var clearBeforeListening: Boolean = true
        private set

    /**
     * Build a new [TextBox] object with the configured properties, with the given terminal.
     *
     * @param terminal The terminal this textbox should print in.
     * @throws ImproperlyConfiguredException if either or both [position] and [size] are not set.
     */
    fun build(terminal: Terminal): TextBox {
        if (position.row < 0 || position.column < 0) throw ImproperlyConfiguredException("Position is not set")
        if (size.rows < 1 || size.columns < 1) throw ImproperlyConfiguredException("Size is not set")

        val actualPadding = TextBoxPadding(
            border.impl.top + padding.top,
            border.impl.right + padding.right,
            border.impl.bottom + padding.bottom,
            border.impl.left + padding.left
        )

        return TextBox(terminal, size, position,
            overflow,
            placeHolder,
            showOverflowIndicator,
            overflowIndicator,
            wordBreak,
            characterLimit,
            actualPadding,
            border,
            characterMask,
            startDisabled,
            keepContents,
            clearBeforeListening,
        )
    }

    /**
     * Set the size of the textbox.
     *
     * @param rows The number of characters of the height of the textbox, including the border and padding.
     * @param columns The number of characters of the width of the textbox, including the border and padding.
     * @throws IllegalArgumentException If either parameter is less than 1.
     */
    fun withSize(rows: Int, columns: Int): TextBoxBuilder {
        if (rows < 1 || columns < 1) throw IllegalArgumentException("Both parameters need to be 1 or more")
        size = TerminalSize(columns, rows)
        return this
    }

    /**
     * Set the height of the textbox.
     *
     * @param rows The number of characters of the height of the textbox, including the border and padding.
     * @throws IllegalArgumentException If the given height is less than 1.
     */
    fun withHeight(rows: Int): TextBoxBuilder {
        if (rows < 1) throw IllegalArgumentException("Text box height cannot be less than 1")
        size = TerminalSize(size.columns, rows)
        return this
    }

    /**
     * Set the width of the textbox.
     *
     * @param columns The number of characters of the width of the textbox, including the border and padding.
     * @throws IllegalArgumentException If the given width is less than 1.
     */
    fun withWidth(columns: Int): TextBoxBuilder {
        if (columns < 1) throw IllegalArgumentException("Text box width cannot be less than 1")
        size = TerminalSize(columns, size.rows)
        return this
    }

    /**
     * Set the position of the text box.
     *
     * @param row The row of the character in the top left corner of the text box.
     * @param column The column of the character in the top left corner of the text box.
     * @throws IllegalArgumentException If either coordinate is less than 0.
     */
    fun withPosition(row: Int, column: Int): TextBoxBuilder {
        if (row < 0 || column < 0) throw IllegalArgumentException("Coordinates out of bounds")
        position = TerminalPosition(column, row)
        return this
    }

    /**
     * Set the position and size of the text box by providing two coordinates.
     *
     * The coordinates are as shown below
     * ```none
     * A-------+
     * |       |
     * +-------B
     * ```
     * where A = `topLeft` and B = `bottomRight`
     *
     * @param topLeft The coordinates of the character in the top left of the text box.
     * @param bottomRight The coordinates of the character in the bottom right of the text box.
     * @throws IllegalArgumentException If either coordinate is out of bounds, or the resulting text box' size is not
     * at least 1x1.
     */
    fun withBounds(topLeft: TerminalPosition, bottomRight: TerminalPosition): TextBoxBuilder {
        if (topLeft.row < 0 || topLeft.column < 0 || bottomRight.row < 0 || bottomRight.column < 0)
            throw IllegalArgumentException("At least one of the given coordinates is out of bounds")
        val height = bottomRight.row - topLeft.row
        val width = bottomRight.column - topLeft.column
        if (height < 1) throw IllegalArgumentException("The resulting height is less than 1")
        if (width < 1) throw IllegalArgumentException("The resulting width is less than 1")
        position = topLeft
        size = TerminalSize(width, height)
        return this
    }

    /**
     * Disable overflow. It is enabled by default. If overflow is enabled, the user can type indefinitely, or until the
     * character limit is reached. With overflow enabled, the text can flow in two ways, depending on the size of the
     * text box.
     *
     * If the text box has a height of exactly 1, the text will scroll horizontally, where, as soon as the edge of the
     * box is reached, each new character will cause all preceding characters to shift one position to the left, and the
     * left-most character will disappear.
     *
     * If the text box has a height of more than 1, the text will scroll vertically, line by line. If the edge of the
     * text box is reached, all lines will shift upwards one row, and the top-most row will disappear.
     *
     * With overflow disabled, the user will be unable to type when the text box is full. A character limit is set
     * according to the number of characters that can fit inside the text box, which is calculated as
     * `(size.rows - padding.rows) * (size.columns - padding.columns)`.
     */
    fun disableOverflow(): TextBoxBuilder {
        overflow = false
        return this
    }

    /**
     * Enable overflow. It is enabled by default. If overflow is enabled, the user can type indefinitely, or until the
     * character limit is reached. With overflow enabled, the text can flow in two ways, depending on the size of the
     * text box.
     *
     * If the text box has a height of exactly 1, the text will scroll horizontally, where, as soon as the edge of the
     * box is reached, each new character will cause all preceding characters to shift one position to the left, and the
     * left-most character will disappear.
     *
     * If the text box has a height of more than 1, the text will scroll vertically, line by line. If the edge of the
     * text box is reached, all lines will shift upwards one row, and the top-most row will disappear.
     *
     * With overflow disabled, the user will be unable to type when the text box is full. A character limit is set
     * according to the number of characters that can fit inside the text box, which is calculated as
     * `(size.rows - padding.rows) * (size.columns - padding.columns)`.
     */
    fun enableOverflow(): TextBoxBuilder {
        overflow = true
        return this
    }

    /**
     * Set placeholder text for the text box. This text will be displayed as long as the internal string containing the
     * user's input is empty.
     * @param text The placeHolder text to display. If set to an empty string, no text will be displayed. This is the
     * default.
     */
    fun withPlaceholder(text: String): TextBoxBuilder {
        placeHolder = text
        return this
    }

    /**
     * Enable the overflow indicator. It is enabled by default. If the overflow indicator is enabled, a character will
     * be displayed if there is text that does not fit inside the input box. The character used for the overflow
     * indicator is set in [overflowIndicator].
     *
     * If the text box has a height (`size.rows`) of exactly 1, the overflow indicator will be displayed as the
     * first character of the otherwise typeable text. If the height of the textbox exceeds 1, it will be displayed as
     * the last possible character in the textbox. This has to do with the overflow type, as documented in [enableOverflow].
     *
     * The overflow indicator will be separated from the rest of the text by an additional whitespace character.
     */
    fun enableOverflowIndicator(): TextBoxBuilder {
        showOverflowIndicator = true
        return this
    }

    /**
     * Disable the overflow indicator. It is enabled by default. If the overflow indicator is enabled, a character will
     * be displayed if there is text that does not fit inside the input box. The character used for the overflow
     * indicator is set in [overflowIndicator].
     *
     * If the text box has a height (`size.rows`) of exactly 1, the overflow indicator will be displayed as the
     * first character of the otherwise typeable text. If the height of the textbox exceeds 1, it will be displayed as
     * the last possible character in the textbox. This has to do with the overflow type, as documented in [enableOverflow].
     *
     * The overflow indicator will be separated from the rest of the text by an additional whitespace character.
     */
    fun disableOverflowIndicator(): TextBoxBuilder {
        showOverflowIndicator = false
        return this
    }

    /**
     * Set an overflow indicator to display if [showOverflowIndicator] is set to `true`. The default is `\0`, which
     * will actually be either [TextBox.defaultHorizontalOverflowIndicator] if the height (`this.size.rows`) is exactly
     * 1, or [TextBox.defaultVerticalOverflowIndicator] if the height is more than 1.
     *
     * @param indicator The character to display as the overflow indicator, if it is enabled.
     */
    fun withOverflowIndicator(indicator: Char): TextBoxBuilder {
        overflowIndicator = indicator
        return this
    }

    /**
     * Enables word break. This is enabled by default. This setting only has effect if [overflow] is `true`, and
     * the height (`this.size.rows`) is greater than 1. If it is enabled, if a word does not fit on the current line,
     * the entire word will be moved over to the row below. If the size of one word is greater than the width of a line,
     * character break will be applied instead.
     *
     * A word is defined as any non-zero number of consecutive, non-whitespace, characters.
     *
     * The inverse of this method is [withCharacterBreak].
     */
    fun withWordBreak(): TextBoxBuilder {
        wordBreak = true
        return this
    }

    /**
     * Enables character break, and thus disables word break. Character break is disabled by default. If character break
     * is enabled, as soon as the user types a character that does not fit on the current line anymore, only that character
     * will be moved to the next line, as opposed to moving the entire word down.
     *
     * The inverse of this method is [withWordBreak].
     */
    fun withCharacterBreak(): TextBoxBuilder {
        wordBreak = false
        return this
    }

    /**
     * Sets a character limit. As soon as this limit is reached, any further input from the user (except backspace) will
     * be ignored. If [overflow] is `false`, this value will automatically be set to the maximum number of characters
     * that can fit inside the text box, if this value is not set.
     *
     * If the limit is set to `0`, there is no character limit, as long as overflow is enabled.
     *
     * @param limit The maximum number of characters a user should be allowed to type. This includes whitespace characters.
     * Setting this value to `0` removes the limitation.
     * @throws IllegalArgumentException If the given `limit` is negative.
     */
    fun withCharacterLimit(limit: Int): TextBoxBuilder {
        if (limit < 0) throw IllegalArgumentException("Character limit cannot be negative")
        characterLimit = limit
        return this
    }

    /**
     * Set padding inside the textbox. Padding keeps certain lines or character columns clear of text.
     *
     * For the parameters `top` and `bottom`, the given number is the number of rows that will not be used to display
     * the text inside the textbox.
     * For the parameters `right` and `left`, the given number is the number of character columns.
     *
     * Beware that padding is automatically added when setting a border, and that any padding set here is additional to
     * that padding.
     *
     * @param top The thickness of the textbox padding above the text area, in rows.
     * @param right The thickness of the textbox padding to the right of the text area, in columns.
     * @param bottom The thickness of the textbox padding below the text area, in rows.
     * @param left The thickness of the textbox padding to the left of the text area, in columns.
     */
    fun withPadding(top: Int, right: Int, bottom: Int, left: Int): TextBoxBuilder {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) throw IllegalArgumentException("Padding cannot be negative")
        padding = TextBoxPadding(top, right, bottom, left)
        return this
    }

    /**
     * Set padding inside the textbox. Padding keeps certain lines or character columns clear of text.
     *
     * Beware that padding is automatically added when setting a border, and that any padding set here is additional to
     * that padding.
     */
    fun withPadding(padding: TextBoxPadding): TextBoxBuilder {
        if (padding.top < 0 || padding.right < 0 || padding.bottom < 0 || padding.left < 0)
            throw IllegalArgumentException("Padding cannot be negative")
        this.padding = padding
        return this
    }

    /**
     * Set a border around the textbox. The different border types are defined in [TextBoxBorder].
     *
     * Padding is necessary to keep the user-input text from overwriting the border. This is automatically taken care of
     * when [build] is invoked. It is possible to set additional padding with [withPadding]. Setting a padding there with
     * `top = 1` would leave one row of space between the border and the text.
     */
    fun withBorder(border: TextBoxBorder): TextBoxBuilder {
        this.border = border
        return this
    }

    /**
     * Set a character mask. If the character mask is set, each character the user types will be shown inside the textbox
     * as the set character here. Essentially, if the character here is '*', the textbox will function as a password field.
     *
     * @param character The character to use as a mask. Setting it to `\u0000` disables the mask.
     */
    fun withCharacterMask(character: Char): TextBoxBuilder {
        characterMask = character
        return this
    }

    /**
     * Start the textbox disabled, which means it will not listen for user input, even if it is already added as a
     * listener to [InputChecker]. Calling `enable()` on the [TextBox] once it is built will allow it to start listening.
     */
    fun startDisabled(): TextBoxBuilder {
        startDisabled = true
        return this
    }

    /**
     * Start the textbox enabled, which means it will immediately start listening for user input when it is added as
     * a listener to [InputChecker]. This is the default. This method serves as the inverse of [startDisabled].
     */
    fun startEnabled(): TextBoxBuilder {
        startDisabled = false
        return this
    }

    /**
     * Keep the contents of the textbox after the user presses enter.
     */
    fun keepContents(): TextBoxBuilder {
        keepContents = true
        return this
    }

    /**
     * Do not clear the textbox contents when the `getString()` method is called on the textbox.
     */
    fun withoutAutoClear(): TextBoxBuilder {
        clearBeforeListening = false
        return this
    }
}