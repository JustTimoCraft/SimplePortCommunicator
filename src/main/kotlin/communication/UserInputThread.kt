package communication

import config.Constants
import debug.Debug
import debug.VerbosityLevel
import ui.input.InputStreamClosedException
import ui.input.textbox.TextBox

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
}