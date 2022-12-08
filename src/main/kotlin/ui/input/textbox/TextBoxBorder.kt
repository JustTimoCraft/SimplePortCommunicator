package ui.input.textbox

import ui.Terminal
import ui.putVerticalLine
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize

enum class TextBoxBorder(val impl: TextBoxBorderImpl) {
    None(NoBorder()),
    Thin(ThinBoxDrawingBorder(false)),
    ThinCornersOnly(ThinBoxDrawingBorder(true)),
    Double(DoubleLineBoxDrawing(false)),
    DoubleCornersOnly(DoubleLineBoxDrawing(true)),
    ThinASCII(ASCIIThin(false)),
    ThinASCIICornersOnly(ASCIIThin(true)),
}

/**
 * Interface for a border implementation.
 */
interface TextBoxBorderImpl {
    /**
     * The thickness of the top of the border in rows.
     */
    val top: Int

    /**
     * The thickness of the right of the border in columns.
     */
    val right: Int

    /**
     * The thickness of the bottom of the border in rows.
     */
    val bottom: Int

    /**
     * The thickness of the left of the border in columns.
     */
    val left: Int

    /**
     * Print the border to the terminal.
     *
     * @param terminal The terminal to print the border to
     */
    fun printBorder(terminal: Terminal, tbSize: TerminalSize, tbPosition: TerminalPosition)
}

/* IMPLEMENTATIONS */

private class NoBorder: TextBoxBorderImpl {
    override val top: Int = 0
    override val right: Int = 0
    override val bottom: Int = 0
    override val left: Int = 0

    override fun printBorder(terminal: Terminal, tbSize: TerminalSize, tbPosition: TerminalPosition) {
        return
    }
}

private open class ThinBoxDrawingBorder(val cornersOnly: Boolean): TextBoxBorderImpl {
    override val top: Int = 1
    override val right: Int = 1
    override val bottom: Int = 1
    override val left: Int = 1

    protected open val topLeft = '┌'
    protected open val topRight = '┐'
    protected open val bottomLeft = '└'
    protected open val bottomRight = '┘'
    protected open val verticalEdge = '│'
    protected open val horizontalEdge = '─'

    override fun printBorder(terminal: Terminal, tbSize: TerminalSize, tbPosition: TerminalPosition) {
        if (tbSize.columns < 3 || tbSize.rows < 3) throw IllegalArgumentException("Textbox too small to print border")

        // Corners
        terminal.terminal.cursorPosition = tbPosition
        terminal.terminal.putCharacter(topLeft)
        terminal.terminal.cursorPosition = tbPosition.withRelativeRow(tbSize.rows - 1)
        terminal.terminal.putCharacter(bottomLeft)
        terminal.terminal.cursorPosition = tbPosition.withRelativeColumn(tbSize.columns - 1)
        terminal.terminal.putCharacter(topRight)
        terminal.terminal.cursorPosition = tbPosition.withRelativeColumn(tbSize.columns - 1).withRelativeRow(tbSize.rows - 1)
        terminal.terminal.putCharacter(bottomRight)

        if (!cornersOnly) {
            // Top horizontal
            terminal.terminal.cursorPosition = tbPosition.withRelativeColumn(1)
            terminal.terminal.putString(horizontalEdge.toString().repeat(tbSize.columns - 2))
            // Bottom horizontal
            terminal.terminal.cursorPosition = tbPosition.withRelativeColumn(1).withRelativeRow(tbSize.rows - 1)
            terminal.terminal.putString(horizontalEdge.toString().repeat(tbSize.columns - 2))

            // Left vertical
            terminal.terminal.cursorPosition = tbPosition.withRelativeRow(1)
            terminal.putVerticalLine(verticalEdge, tbSize.rows - 2)
            // Right vertical
            terminal.terminal.cursorPosition = tbPosition.withRelativeRow(1).withRelativeColumn(tbSize.columns - 1)
            terminal.putVerticalLine(verticalEdge, tbSize.rows - 2)
        }
    }
}

private class ASCIIThin(cornersOnly: Boolean): ThinBoxDrawingBorder(cornersOnly) {
    override val topLeft = '+'
    override val topRight = '+'
    override val bottomLeft = '+'
    override val bottomRight = '+'
    override val verticalEdge = '|'
    override val horizontalEdge = '-'
}

private class DoubleLineBoxDrawing(cornersOnly: Boolean): ThinBoxDrawingBorder(cornersOnly) {
    override val topLeft = '╔'
    override val topRight = '╗'
    override val bottomLeft = '╚'
    override val bottomRight = '╝'
    override val verticalEdge = '║'
    override val horizontalEdge = '═'
}
