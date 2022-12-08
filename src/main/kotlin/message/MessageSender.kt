package message

import config.Constants
import debug.Debug
import debug.VerbosityLevel
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock

class MessageSender: Runnable {
    private val messageQueue = LinkedBlockingQueue<String>()
    private var stop = false
    private val messageLock = ReentrantLock()
    private val newMessage = messageLock.newCondition()

    fun addMessage(message: String) {
        messageQueue.put(message)
        messageLock.lock()
        newMessage.signal()
        messageLock.unlock()
    }

    override fun run() {
        val origin = "MessageSender.<run>"
        if (SPC.socket == null || !SPC.socket!!.isConnected) {
            Debug.debugMessage(VerbosityLevel.ERROR, origin, "Cannot start - Connection not established")
            return
        }
        try {
            Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Message sender started")
            BufferedWriter(OutputStreamWriter(SPC.socket!!.getOutputStream())).use {
                while (!stop) {
                    while(!messageQueue.isEmpty()) {
                        val message = messageQueue.take()
                        it.write(message + "\n")
                        it.flush()
                    }

                    messageLock.lock()
                    newMessage.await()
                    messageLock.unlock()
                }
            }
        } catch (e: SocketException) {
            Debug.debugMessage(VerbosityLevel.WARNING, origin, "Couldn't send message - Exception caught:\n$e")
            // Notify the user of the lost connection if the chat window is up
            if (SPC.chatWindow.active) {
                SPC.addMultiMessage(arrayListOf(
                    Message(Constants.systemName, "Couldn't send message - Disconnected from server"),
                    Message(Constants.systemName, "To reconnect, please type /disconnect and reconnect through the connection menu"))
                )
            }
        }
    }

    fun stop() {
        stop = true
        messageLock.lock()
        newMessage.signalAll()
        messageLock.unlock()
    }
}