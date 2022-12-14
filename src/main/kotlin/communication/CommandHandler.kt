package communication

import config.Constants
import java.lang.Integer.min
import java.util.*

fun commandHandler(input: String): String {
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
    } else if (input.startsWith("/autoresponder", true) || input.startsWith("/ar", true)) {
        autoResponderCommandHandler(input)
    } else if (input.equals("/help", true)) {
        val helpMessages = LinkedList<Message>()
        helpMessages.add(Message(Constants.systemName, "--- Help menu:"))
        helpMessages.add(Message(
            Constants.systemName, "Any text typed in the textbox at the bottom will" +
                " be sent to the server, unless a '/' is put in front. To send a message starting with a '/', start" +
                " the message with two, like this: '//text' sends '/text' to the server"))
        helpMessages.add(Message(Constants.systemName, "--- Commands:"))
        helpMessages.add(Message(Constants.systemName, "/disconnect : Disconnect from the server and return to the connection screen"))
        helpMessages.add(Message(Constants.systemName, "/quit       : Disconnect from the server and exit the application"))
        helpMessages.add(Message(Constants.systemName, "/clear      : Clear the screen of all past messages"))
        helpMessages.add(Message(Constants.systemName, "/help       : Prints this help menu"))
        helpMessages.add(Message(Constants.systemName, "/ar         : Prints the AutoResponder help menu"))
        SPC.addMultiMessage(helpMessages)
    } else {
        SPC.addMessage(Message(Constants.systemName, "Error, unrecognised command: '$input'"))
    }

    return ""
}

// Handle all commands relating to autoresponder
private fun autoResponderCommandHandler(input: String) {
    // Cut of the first part of the command
    val parts = input.split(" ").filter { it.isNotEmpty() }
    if (parts.size == 1 || parts[1].isEmpty()) {
        // Show help menu
        val helpMenu = LinkedList<Message>()
        helpMenu.add(Message(Constants.systemName, "--- AutoResponder help menu:"))
        helpMenu.add(Message(Constants.systemName,
            "AutoResponders can be used to respond to incoming messages automatically. This can be helpful if " +
                    "the connected server sends periodic pings or similar messages."))
        helpMenu.add(Message(Constants.systemName, "/ar list          : Lists the current AutoResponders, and their IDs"))
        helpMenu.add(Message(Constants.systemName, "/ar new           : Create a new AR"))
        helpMenu.add(Message(Constants.systemName, "/ar <id> info     : Prints information about the selected AR"))
        helpMenu.add(Message(Constants.systemName, "/ar <id> enable   : Enables the selected AR"))
        helpMenu.add(Message(Constants.systemName, "/ar <id> disable  : Disables the selected AR - Disabled ARs will " +
                "remain in the list, but will not respond to incoming messages"))
        helpMenu.add(Message(Constants.systemName, "/ar <id> delete   : Delete the selected AR"))
        helpMenu.add(Message(Constants.systemName, "/ar <id> set-in  <msg>  : Set the incoming message for this AR"))
        helpMenu.add(Message(Constants.systemName, "/ar <id> set-out <msg>  : Set the response message for this AR"))
        SPC.addMultiMessage(helpMenu)
    } else if (parts[1].equals("list", true)) {
        val arList = LinkedList<Message>()
        arList.add(Message(Constants.systemName, "--- List of AutoResponders"))
        synchronized (SPC.autoResponders) {
            // Print a nice message if the list of responders is empty
            if (SPC.autoResponders.isEmpty()) {
                arList.add(Message(Constants.systemName, " - No AutoResponders configured - Create one with /ar new"))
                return@synchronized
            }

            // Otherwise, print a list of the autoresponders
            // The index will start at 1
            var index = 1
            for (responder in SPC.autoResponders) {
                var entry = "> $index ["
                entry += if (responder.enabled) {
                    "Active]   : "
                } else {
                    "Inactive] : "
                }
                entry += responder.getMessage().substring(0 until (min(responder.getMessage().length, 40)))

                arList.add(Message(Constants.systemName, entry))
                index++
            }
        }
        SPC.addMultiMessage(arList)
    } else if (parts[1].equals("new", true)) {
        synchronized (SPC.autoResponders) {
            SPC.autoResponders.add(BasicAutoResponder("If the server sends this", "This will be responded"))
            SPC.addMessage(Message(Constants.systemName, "New AutoResponder created with ID ${SPC.autoResponders.size}"))
        }
    } else {
        try {
            val index = parts[1].toInt()
            singleAutoResponderCommands(index, parts)
        } catch (e: NumberFormatException) {
            SPC.addMessage(Message(Constants.systemName, "Error, unrecognised command: $input"))
        }
    }

}

fun singleAutoResponderCommands(index: Int, parts: List<String>) {
    synchronized (SPC.autoResponders) {
        // Check if the ID is valid
        if (index < 1 || index > SPC.autoResponders.size) {
            SPC.addMessage(Message(Constants.systemName, "Invalid AutoResponder ID - Check the ID with /ar list"))
            return@singleAutoResponderCommands
        }

        // The ID is valid, so we can retrieve the auto responder object
        val ar = SPC.autoResponders[index - 1]
        if (parts.size < 3 || parts[2].equals("info", true)) {
            // Print info
            val info = LinkedList<Message>()
            info.add(Message(Constants.systemName, "--- AutoResponder information: [${ if (ar.enabled) { "Active" } else { "Inactive" }}]"))
            info.add(Message(Constants.systemName, "> Incoming message: ${ar.getMessage()}"))
            info.add(Message(Constants.systemName, "> Response message: ${ar.getResponse()}"))
            SPC.addMultiMessage(info)
        } else if (parts[2].equals("enable", true)) {
            // Set the AR to enabled, even if it already was
            ar.enabled = true
            SPC.addMessage(Message(Constants.systemName, "AutoResponder $index enabled"))
        } else if (parts[2].equals("disable", true)) {
            // Set the AR to disabled, even if it already was
            ar.enabled = false
            SPC.addMessage(Message(Constants.systemName, "AutoResponder $index disabled"))
        } else if (parts[2].equals("delete", true)) {
            // We already verified the ID, so this is a safe operation
            SPC.autoResponders.removeAt(index - 1)
        } else if (parts[2].equals("set-in", true) || parts[2].equals("set-out", true)) {
            var fullMessage = ""
            for (i in 3 until parts.size) {
                fullMessage += " ${parts[i]}"
            }
            fullMessage = fullMessage.trim()
            if (parts[2].equals("set-in",true)) {
                try {
                    ar.setMessage(fullMessage)
                    SPC.addMessage(Message(Constants.systemName, "New incoming message set for AR $index"))
                } catch (e: UnsupportedOperationException) {
                    SPC.addMessage(Message(Constants.systemName, "Error: This AutoResponder does not support setting the incoming message"))
                }
            } else {
                try {
                    ar.setResponse(fullMessage)
                    SPC.addMessage(Message(Constants.systemName, "New response message set for AR $index"))
                } catch (e: UnsupportedOperationException) {
                    SPC.addMessage(Message(Constants.systemName, "Error: This AutoResponder does not support setting the response message"))
                }
            }
        } else {
            SPC.addMessage(Message(Constants.systemName, "Error, unrecognised command: ${parts[2]}"))
        }
    }
}