package ui.screens

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.input.KeyType
import ui.centreString
import ui.input.SpecialKeyListener
import ui.putVerticalLine

fun showConnectingPopup() {
    val terminal = SPC.terminal
    val ip = SPC.ip
    val port = SPC.port

    val dimensions = PopupBox.dimensions
    val topLeft = PopupBox.topLeft

    val ipString = if (ip.length + port.length + 1 <= (dimensions.columns - 4) * 2) {
        "${ip}:${port}"
    } else {
        "${ip}:${port}".substring(0 until ((dimensions.columns - 4) * 2) - 3) + "..."
    }

    synchronized (terminal) {
        PopupBox.printPopup()

        // Text
        terminal.centreString("Connecting to", topLeft.row + 1, topLeft.column + 1, topLeft.column + dimensions.columns)
        terminal.centreString("Please wait", topLeft.row + 6, topLeft.column + 1, topLeft.column + dimensions.columns)

        if (ipString.length <= dimensions.columns - 4) {
            terminal.centreString(ipString, topLeft.row + 3, topLeft.column + 1, topLeft.column + dimensions.columns)
        } else {
            terminal.terminal.cursorPosition = topLeft.withRelativeRow(3).withRelativeColumn(2)
            terminal.terminal.putString(ipString.substring(0 until (dimensions.columns - 4)))
            terminal.terminal.cursorPosition = topLeft.withRelativeRow(4).withRelativeColumn(2)
            terminal.terminal.putString(ipString.substring(dimensions.columns - 4 until ipString.length))
        }

        terminal.terminal.flush()
    }
}

fun askRetryToConnect(): Boolean {
    val terminal = SPC.terminal
    val dimensions = PopupBox.dimensions
    val topLeft = PopupBox.topLeft

    // Static characters
    synchronized (terminal) {
        PopupBox.printPopup()

        // Text
        terminal.centreString("Connection failed", topLeft.row + 1, topLeft.column + 1, topLeft.column + dimensions.columns)
        terminal.centreString("Retry connection?", topLeft.row + 3, topLeft.column + 1, topLeft.column + dimensions.columns)

        terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(4)
        terminal.terminal.putString("Retry")
        terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(dimensions.columns - 10)
        terminal.terminal.putString("Cancel")

        terminal.terminal.flush()
    }

    var retry = true

    val input = SpecialKeyListener()
    terminal.addKeyListener(input)

    while (true) {
        synchronized (terminal) {
            if (retry) {
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(2)
                terminal.terminal.putCharacter('>')
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(10)
                terminal.terminal.putCharacter('<')
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(dimensions.columns - 3)
                terminal.terminal.putCharacter(' ')
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(dimensions.columns - 12)
                terminal.terminal.putCharacter(' ')
            } else {
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(2)
                terminal.terminal.putCharacter(' ')
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(10)
                terminal.terminal.putCharacter(' ')
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(dimensions.columns - 3)
                terminal.terminal.putCharacter('<')
                terminal.terminal.cursorPosition = topLeft.withRelativeRow(6).withRelativeColumn(dimensions.columns - 12)
                terminal.terminal.putCharacter('>')
            }
            terminal.terminal.flush()
        }

        val key = input.getKey()

        when (key.keyType) {
            KeyType.ArrowRight, KeyType.ArrowLeft -> retry = !retry
            KeyType.Enter -> break
            KeyType.Escape, KeyType.EOF -> {
                SPC.shutdown = true
                break
            }
            else -> Unit
        }
    }

    terminal.removeKeyListener(input)
    return retry
}