package ui.input

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import debug.Debug
import debug.VerbosityLevel
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

open class StringListener: KeyListener() {
    var stringBuffer = ""
    protected val stringLock = ReentrantLock()
    protected val stringReady: Condition = stringLock.newCondition()
    protected var error = false

    /**
     * Wait for input until the user has typed something and presses enter.
     *
     * @throws InputStreamClosedException If the key sent by the input checker is `KeyType.EOF`, which is sent if the
     * terminal is closed. This allows any threads waiting for a string to wake when the terminal closes. Upon receiving
     * this error, no further calls to getString() should be made.
     */
    open fun getString(): String {
        if (error) throw InputStreamClosedException("The terminal has closed")
        stringLock.lock()
        stringBuffer = ""
        try {
            stringReady.await()
            stringLock.unlock()
            if (error) throw InputStreamClosedException("The terminal has closed")
            val resultString = stringBuffer
            stringBuffer = ""
            return resultString
        } catch (e: InterruptedException) {
            stringLock.unlock()
            Debug.debugMessage(
                VerbosityLevel.WARNING, "StringListener.gS",
                "Thread interrupted while waiting for string")
            Thread.currentThread().interrupt()
            return ""
        }
    }

    /**
     * Get the current contents of the string buffer. Can be used by the screen to display the string as it is being
     * typed. Will always return immediately.
     */
    fun getUnfinishedString(): String {
        stringLock.lock()
        val contents = stringBuffer
        stringLock.unlock()
        return contents
    }

    override fun sendKey(key: KeyStroke) {
        lock.lock()
        // Default single-key behaviour
        buffer = key
        keyReceived.signal()
        lock.unlock()

        // String behaviour
        stringLock.lock()
        when (key.keyType) {
            KeyType.Character -> stringBuffer += key.character
            KeyType.Backspace -> stringBuffer = stringBuffer.dropLast(1)
            KeyType.Enter -> stringReady.signal()
            KeyType.EOF -> {
                error = true
                stringReady.signal()
            }
            else -> 0
        }
        stringLock.unlock()
    }

    override fun close() {
        super.close()
        error = true
        stringLock.lock()
        stringReady.signal()
        stringLock.unlock()
    }
}