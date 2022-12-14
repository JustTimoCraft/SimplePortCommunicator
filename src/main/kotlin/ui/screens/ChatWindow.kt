package ui.screens

import com.googlecode.lanterna.TerminalPosition
import config.Constants
import debug.Debug
import debug.VerbosityLevel
import communication.UserInputThread
import ui.input.textbox.TextBoxBuilder
import java.lang.Integer.min
import kotlin.math.ceil

class ChatWindow {
    val terminal = SPC.terminal
    private var stop = false
    var active = false
        private set

    fun startChatWindow() {
        active = true
        val terminalSize = terminal.terminal.terminalSize

        // Input box in the bottom 2 rows of the screen
        val inputBox = TextBoxBuilder().withBounds(TerminalPosition(0, terminalSize.rows - 2),
            TerminalPosition(terminalSize.columns - 1, terminalSize.rows))
            .withPadding(0, 1, 0 ,1)
            .build(terminal)
        terminal.addKeyListener(inputBox)

        // Should auto-close when inputBox.close() is called
        Thread(UserInputThread(inputBox)).start()

        while (!stop && !SPC.shutdown) {
            // Print stuff
            terminal.terminal.clearScreen()
            printStatic()
            printMessages()

            // wait for new message
            SPC.newMessageLock.lock()
            SPC.newMessageSignal.await()
            SPC.newMessageLock.unlock()
        }

        active = false
        Debug.debugMessage(VerbosityLevel.EXTRA, "ChatWindow.sCW", "Chat window closing")
        inputBox.close()
        terminal.removeKeyListener(inputBox)
    }

    private fun printStatic() {
        val terminalSize = terminal.terminal.terminalSize

        synchronized (terminal) {
            terminal.terminal.cursorPosition = TerminalPosition(0, terminalSize.rows - 3)
            terminal.terminal.putString("─".repeat(terminalSize.columns))
        }
    }

    private fun printMessages() {
        synchronized (terminal) {
            synchronized (SPC.messageHistory) {
                val terminalSize = terminal.terminal.terminalSize
                val rowWidth = terminalSize.columns - (Constants.chatWindowPaddingLeft + Constants.chatWindowPaddingRight)
                var rowNum = terminalSize.rows - 4 // Start value
                for (message in SPC.messageHistory.reversed()) {
                    val messageHeight = ceil(message.length / (rowWidth*1.0)).toInt()
                    for (i in (0 until messageHeight).reversed()) {
                        terminal.terminal.cursorPosition = TerminalPosition(Constants.chatWindowPaddingLeft, rowNum)
                        terminal.terminal.putString(message.toString()
                            .substring(i*rowWidth until min(i*rowWidth+rowWidth, message.length)))
                        rowNum--
                        if (rowNum < 0) {
                            break
                        }
                    }
                }
            }

            terminal.terminal.flush()
        }
    }

    fun close() {
        Debug.debugMessage(VerbosityLevel.EXTRA, "ChatWindow.close", "Received close signal")
        stop = true
        SPC.newMessageLock.lock()
        SPC.newMessageSignal.signal()
        SPC.newMessageLock.unlock()
    }
}