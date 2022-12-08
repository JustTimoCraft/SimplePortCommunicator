package ui.screens

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import ui.putVerticalLine

class PopupBox private constructor() {

   companion object {
       val terminal = SPC.terminal
       val dimensions = if (terminal.terminal.terminalSize.columns > 36) {
           TerminalSize(36, 8)
       } else {
           TerminalSize(24, 8)
       }
       val topLeft = TerminalPosition((terminal.terminal.terminalSize.columns / 2) - (dimensions.columns / 2),
           (terminal.terminal.terminalSize.rows / 2) - 4)

       fun printPopup() {
           clearArea()
           drawBorders()
       }

       fun drawBorders() {
           // Draw borders
           terminal.terminal.cursorPosition = topLeft
           terminal.terminal.putString("╔${"═".repeat(dimensions.columns - 2)}╗")
           terminal.terminal.cursorPosition = topLeft.withRelativeRow(7)
           terminal.terminal.putString("╚${"═".repeat(dimensions.columns - 2)}╝")
           terminal.terminal.cursorPosition = topLeft.withRelativeRow(1)
           terminal.putVerticalLine('║', 6)
           terminal.terminal.cursorPosition = topLeft.withRelativeColumn(dimensions.columns - 1).withRelativeRow(1)
           terminal.putVerticalLine('║', 6)
       }

       fun clearArea() {
           // Clear area
           for (row in 0 until dimensions.rows) {
               terminal.terminal.cursorPosition = topLeft.withRelativeRow(row)
               terminal.terminal.putString(" ".repeat(dimensions.columns))
           }
       }
   }
}