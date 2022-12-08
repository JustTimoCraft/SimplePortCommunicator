package ui

import com.googlecode.lanterna.TerminalPosition
import debug.Debug
import debug.VerbosityLevel

const val notInitErrorMessage: String = "Function called on uninitialised terminal"

/**
 * Moves the cursor one line down and returns it to the left of the terminal.
 */
fun Terminal.newLine() {
    this.newLine(0)
}

/**
 * Moves the cursor one line down and returns it to the left of the terminal, offset by the given parameter.
 * @param offset The number of characters that the cursor should be placed away from the left side of the terminal.
 */
fun Terminal.newLine(offset: Int) {
    if (offset < 0) throw IllegalArgumentException("The offset cannot be negative")
    if (offset >= terminal.terminalSize.columns) throw IllegalArgumentException("The offset cannot be greater than the width of the terminal")
    if (this.initialised) {
        terminal.cursorPosition = terminal.cursorPosition.withColumn(offset).withRelativeRow(1)
    } else {
        Debug.debugMessage(VerbosityLevel.WARNING, "Terminal.nL", notInitErrorMessage)
    }
}

/**
 * Print a vertical line of the given character at the cursor's current position. The cursor's original position is
 * restored after printing.
 * @param char The character the resulting line will consist of.
 * @param height The number of characters to print
 */
fun Terminal.putVerticalLine(char: Char, height: Int) {
    val originalPosition = terminal.cursorPosition
    for (i in 0 until height) {
        terminal.cursorPosition = originalPosition.withRelativeRow(i)
        terminal.putCharacter(char)
    }
    terminal.cursorPosition = originalPosition
}

/**
 * Print a string to the terminal, centred between the two bounds. Prints nothing if the string does not fit.
 * @param string The text to print
 * @param row The row to print the text on
 * @param leftBound The left-most column the string can appear on (inclusive)
 * @param rightBound The right-most column the string can appear on (inclusive)
 */
fun Terminal.centreString(string: String, row: Int, leftBound: Int, rightBound: Int) {
    val maxWidth = rightBound - leftBound
    if (string.length > maxWidth) return
    if (leftBound < 0 || row < 0) throw IllegalArgumentException("Coordinates out of bounds")

    terminal.cursorPosition = TerminalPosition((maxWidth - string.length) / 2 + leftBound, row)
    terminal.putString(string)
}