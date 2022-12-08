package message

import config.Constants
import debug.Debug
import debug.VerbosityLevel
import ui.input.InputStreamClosedException
import ui.input.textbox.TextBox
import java.util.*

class UserInputThread(private val tb: TextBox): Runnable {

    override fun run() {
        try {
            while (true) {
                val input = tb.getString()
                val message = commandHandler(input)
                if (message.isNotEmpty()) {
                    SPC.addMessage(Message(Constants.localName, input, System.currentTimeMillis()))
                    SPC.messageSender.addMessage(input)
                }
            }
        } catch (e: InputStreamClosedException) {
            // Exit
            Debug.debugMessage(VerbosityLevel.EXTRA, "UIT.<run>", "User input checker closed - Textbox closed")
            return
        }
    }

    private fun commandHandler(input: String): String {
        if (input.isEmpty() || input[0] != '/') {
            return input
        } else if (input.startsWith("//")) {
            return input.drop(1)
        }

        if (input.equals("/disconnect", true)) {
            SPC.chatWindow.close()
        } else if (input.equals("/quit", true)) {
            SPC.shutdown = true
            SPC.chatWindow.close()
        } else if (input.equals("/clear", true)) {
            SPC.clearMessages()
        } else if (input.equals("/help", true)) {
            val helpMessages = LinkedList<Message>()
            helpMessages.add(Message(Constants.systemName, "--- Help menu:"))
            helpMessages.add(Message(Constants.systemName, "Any text typed in the textbox at the bottom will" +
                    " be sent to the server, unless a '/' is put in front. To send a message starting with a '/', start" +
                    " the message with two, like this: '//text' sends '/text' to the server"))
            helpMessages.add(Message(Constants.systemName, "--- Commands:"))
            helpMessages.add(Message(Constants.systemName, "/disconnect : Disconnect from the server and return to the connection screen"))
            helpMessages.add(Message(Constants.systemName, "/quit       : Disconnect from the server and exit the application"))
            helpMessages.add(Message(Constants.systemName, "/clear      : Clear the screen of all past messages"))
            helpMessages.add(Message(Constants.systemName, "/help       : Prints this help menu"))
            SPC.addMultiMessage(helpMessages)
        }

        return ""
    }
}