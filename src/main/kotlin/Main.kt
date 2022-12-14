import communication.*
import ui.Terminal
import debug.Debug
import debug.VerbosityLevel
import config.Constants
import ui.screens.ChatWindow
import ui.screens.askForIP
import ui.screens.askRetryToConnect
import ui.screens.showConnectingPopup
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.system.exitProcess

class SPC {
    companion object {
        lateinit var terminal: Terminal
        var shutdown = false
            @Synchronized set
            @Synchronized get
        var socket: Socket? = null
        var ip = ""
        var port = ""
        val connectionListener = ConnectionListener()
        val messageSender = MessageSender()
        var clt: Thread? = null
        var mst: Thread? = null
        lateinit var chatWindow: ChatWindow
        val messageHistory = LinkedList<Message>()
        val newMessageLock = ReentrantLock()
        val newMessageSignal = newMessageLock.newCondition()
        val autoResponders = ArrayList<AutoResponder>()

        fun addMessage(message: Message) {
            synchronized (messageHistory) {
                messageHistory.addLast(message)

                while (messageHistory.size > 50) {
                    messageHistory.removeFirst()
                }
            }

            newMessageLock.lock()
            newMessageSignal.signalAll()
            newMessageLock.unlock()
        }

        fun addMultiMessage(messages: List<Message>) {
            synchronized (messageHistory) {
                for (m in messages) {
                    messageHistory.addLast(m)
                }

                while (messageHistory.size > 50) {
                    messageHistory.removeFirst()
                }
            }

            newMessageLock.lock()
            newMessageSignal.signalAll()
            newMessageLock.unlock()
        }

        fun clearMessages() {
            synchronized (messageHistory) {
                messageHistory.clear()
            }

            newMessageLock.lock()
            newMessageSignal.signalAll()
            newMessageLock.unlock()
        }
    }
}

fun main(args: Array<String>) {
    val origin = "SPC.<main>"

    // Smooth-brain's commandline argument parser
    if (args.contains("--debug")) {
        Debug.setVerbosityLevel(VerbosityLevel.EXTRA)
        Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Debug mode enabled")
    } else if (args.contains("--error-only")) {
        Debug.setVerbosityLevel(VerbosityLevel.ERROR)
    } else if (args.contains("--silent")) {
        Debug.setVerbosityLevel(VerbosityLevel.SILENT)
    }

    Debug.debugMessage(VerbosityLevel.INFO, origin, "Simple port communicator V${Constants.version} - JTC, 2022")

    // Initialise the LanternaTerminal
    SPC.terminal = Terminal("Simple Port Communicator")
    if (!SPC.terminal.initialised) {
        Debug.debugMessage(VerbosityLevel.FATAL, origin, "The terminal could not be initialised")
        exitProcess(1)
    }

    // Program loop
    main@ while (!SPC.shutdown) {
        // Ask for IP and port
        askForIP()

        // Try to connect
        while (!SPC.shutdown) {
            showConnectingPopup()
            if (connect()) {
                break
            } else {
                if (!askRetryToConnect()) {
                    continue@main
                }
            }
        }

        // Connection established when code reaches here
        if (SPC.shutdown) break

        // Go to chat window
        SPC.addMessage(Message(Constants.systemName, "Simple port communicator V${Constants.version} - JTC, 2022"))
        SPC.addMessage(Message(Constants.systemName, "You can start typing messages now, or type /help for a list of commands"))
        SPC.chatWindow = ChatWindow()
        SPC.chatWindow.startChatWindow()
        Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Chat window closed")
        disconnect()
    }

    SPC.terminal.close()
}

fun connect(): Boolean {
    try {
        SPC.socket = Socket(InetAddress.getByName(SPC.ip), SPC.port.toInt())
        // Start connection listener thread
        SPC.connectionListener.init()
        SPC.clt = Thread(SPC.connectionListener, "ConnectionListener")
        SPC.clt!!.start()
        SPC.mst = Thread(SPC.messageSender, "MessageSender")
        SPC.mst!!.start()

    } catch (e: IOException) {
        return false
    }
    Debug.debugMessage(VerbosityLevel.EXTRA, "SPC.connect", "Connection established")
    return true
}

fun disconnect() {
    val origin = "SPC.disconnect"
    try {
        Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Stopping connection listener")
        if (SPC.socket != null) {
            Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Closing socket")
            SPC.socket!!.close()
        }
        SPC.connectionListener.stop()
        if (SPC.clt != null && SPC.clt!!.isAlive) {
            Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Waiting for connection listener thread to terminate")
        }
        SPC.clt?.join() ?: Unit
        SPC.messageSender.stop()
        if (SPC.mst != null && SPC.mst!!.isAlive) {
            Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Waiting for message sender thread to terminate")
        }
        SPC.mst?.join() ?: Unit
        //Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Connection listener terminated")

    } catch (e: IOException) {
        Debug.debugMessage(VerbosityLevel.ERROR, origin, "The following exception occurred when trying to disconnect:\n${e.printStackTrace()}")
    }
}