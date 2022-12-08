package connection

import config.Constants
import debug.Debug
import debug.VerbosityLevel
import message.Message
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ConnectionListener: Runnable {
    private var stop = false
    var connected = false
        private set

    fun init() {
        stop = false
        connected = SPC.socket?.isConnected ?: false
    }

    override fun run() {
        val origin = "ConnectionListener.<run>"
        if (SPC.socket == null || !SPC.socket!!.isConnected) {
            Debug.debugMessage(VerbosityLevel.ERROR, origin, "Cannot start - Connection not established")
            return
        }
        Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Connection listener started")
        try {
            BufferedReader(InputStreamReader(SPC.socket!!.getInputStream())).use {
                while (!stop) {
                    val line: String? = it.readLine()
                    if (line == null) {
                        stop = true
                        connected = false
                        Debug.debugMessage(VerbosityLevel.WARNING, origin, "Disconnected from server")
                        continue
                    }
                    Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Received new message: $line")
                    SPC.addMessage(Message(Constants.remoteName, line, System.currentTimeMillis()))
                }
            }
        } catch (e: IOException) {
            Debug.debugMessage(VerbosityLevel.WARNING, origin, "Exception caught:\n$e")
            // Notify the user of the lost connection if the chat window is up
            if (SPC.chatWindow.active) {
                SPC.addMultiMessage(arrayListOf(
                    Message(Constants.systemName, "Connection lost to server"),
                    Message(Constants.systemName, "To reconnect, please type /disconnect and reconnect through the connection menu"))
                )
            }
        }
    }

    fun stop() {
        stop = true
    }
}