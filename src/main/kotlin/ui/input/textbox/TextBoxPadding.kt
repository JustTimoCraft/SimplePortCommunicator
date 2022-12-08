package ui.input.textbox

/**
 * Padding object for a [TextBox].
 *
 * @param top The thickness of the textbox padding above the text area, in rows.
 * @param right The thickness of the textbox padding to the right of the text area, in columns.
 * @param bottom The thickness of the textbox padding below the text area, in rows.
 * @param left The thickness of the textbox padding to the left of the text area, in columns.
 */
data class TextBoxPadding(val top: Int, val right: Int, val bottom: Int, val left: Int)