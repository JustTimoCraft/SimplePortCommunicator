package ui.screens

import com.googlecode.lanterna.TerminalPosition
import config.Constants
import debug.Debug
import debug.VerbosityLevel
import ui.centreString
import ui.input.SpecialKeyListener
import ui.input.textbox.TextBoxBuilder
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.NumberFormatException

fun askForIP() {
    val terminal = SPC.terminal

    // Strings
    val title = "Simple Port Communicator"
    val accredition = "JTC, 2022"

    val terminalSize = terminal.terminal.terminalSize
    val tbWidth = terminalSize.columns - 9

    val ipTextBox = TextBoxBuilder().withSize(1, tbWidth).withPosition(3, 8)
        .keepContents()
        .build(terminal)
    val portTextBox = TextBoxBuilder().withSize(1, 5).withPosition(5, 8)
        .disableOverflow().keepContents()
        .build(terminal)

    val keyListener = SpecialKeyListener()
    terminal.addKeyListener(keyListener)

    // Print stuff to the terminal
    synchronized (terminal) {
        // Clear screen
        terminal.terminal.clearScreen()

        // Title
        terminal.centreString(title, 1, 0, terminalSize.columns - 1)

        // Name and version at the bottom
        terminal.terminal.cursorPosition = TerminalPosition(2, terminalSize.rows - 2)
        terminal.terminal.putString(accredition)
        terminal.terminal.cursorPosition =
            TerminalPosition(terminalSize.columns - (4 + Constants.version.length), terminalSize.rows - 2)
        terminal.terminal.putString("V${Constants.version}")

        // IP and Port labels
        terminal.terminal.cursorPosition = TerminalPosition(2, 3)
        terminal.terminal.putString("IP:")
        terminal.terminal.cursorPosition = TerminalPosition(2, 5)
        terminal.terminal.putString("Port:")

        terminal.terminal.flush()
    }

    terminal.addKeyListener(ipTextBox)
    SPC.ip = ipTextBox.getString()
    ipTextBox.disable()

    if (SPC.ip.equals("q", true) || SPC.ip.equals("quit", true) || SPC.ip.equals("exit", true)) {
        Debug.debugMessage(VerbosityLevel.EXTRA, "EIPS", "Program exit from IP input screen")
        SPC.shutdown = true
        return
    }

    terminal.addKeyListener(portTextBox)
    SPC.port = portTextBox.getString()

    terminal.removeKeyListener(ipTextBox)
    terminal.removeKeyListener(portTextBox)

    if (SPC.port.equals("q", true) || SPC.port.equals("quit", true) || SPC.port.equals("exit", true)) {
        Debug.debugMessage(VerbosityLevel.EXTRA, "EIPS", "Program exit from IP input screen")
        SPC.shutdown = true
        return
    }


    // Perform checks
    var errorMessageLine1 = ""
    var errorMessageLine2 = ""
    try {
        InetAddress.getByName(SPC.ip)
        val portNum = SPC.port.toInt()
        if (portNum !in 1 until 65536) {
            throw NumberFormatException()
        }
    } catch (e: UnknownHostException) {
        errorMessageLine1 = "IP or domain name"
        errorMessageLine2 = "is invalid"
    } catch (e: NumberFormatException) {
        errorMessageLine1 = "Port number must be"
        errorMessageLine2 = "between 1 and 65536"
    }

    if (errorMessageLine1.isEmpty()) return

    val dimensions = PopupBox.dimensions
    val topLeft = PopupBox.topLeft

    synchronized (terminal) {
        PopupBox.printPopup()
        terminal.centreString("Invalid input", topLeft.row + 2, topLeft.column + 1, topLeft.column + dimensions.columns)
        terminal.centreString(errorMessageLine1, topLeft.row + 4, topLeft.column + 1, topLeft.column + dimensions.columns)
        terminal.centreString(errorMessageLine2, topLeft.row + 5, topLeft.column + 1, topLeft.column + dimensions.columns)
        terminal.terminal.flush()
    }

    terminal.waitForKey()
    askForIP()
}