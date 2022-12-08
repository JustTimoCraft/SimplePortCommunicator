package ui.input

import com.googlecode.lanterna.input.KeyType
import debug.Debug
import debug.VerbosityLevel
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class InputChecker(terminal: com.googlecode.lanterna.terminal.Terminal) : Runnable {
    private val terminal: com.googlecode.lanterna.terminal.Terminal
    private val keyListenerLock: Any = Any()
    private val keyListeners: MutableList<KeyListener> = LinkedList()
    private val anyKeyLock = ReentrantLock()
    private val anyKeySignal = anyKeyLock.newCondition()

    init {
        this.terminal = terminal
    }

    override fun run() {
        while (!SPC.shutdown) {
            val key = terminal.readInput()
            anyKeyLock.lock()
            anyKeySignal.signalAll()
            anyKeyLock.unlock()

            // Keytype EOF is returned when the input stream has closed
            if (key.keyType == KeyType.EOF) {
                SPC.shutdown = true
                break;
            }

            synchronized(keyListenerLock) {
                for (listener in keyListeners) {
                    listener.sendKey(key)
                }
            }
        }
        // Wake the waiting threads when the terminal is closing
        synchronized(keyListenerLock) {
            for (listener in keyListeners) {
                listener.close()
            }
        }
        Debug.debugMessage(VerbosityLevel.EXTRA, "UI.InputChecker.<run>", "InputChecker closed")
    }

    fun addKeyListener(listener: KeyListener) {
        synchronized(keyListenerLock) {
            keyListeners.add(listener)
            Debug.debugMessage(VerbosityLevel.EXTRA, "UI.InputChecker.aKL", "KeyListener added")
        }
    }

    fun removeKeyListener(listener: KeyListener) {
        val origin = "UI.InputChecker.rKL"
        synchronized(keyListenerLock) {
            try {
                listener.close()
            } catch (e: Exception) {
                Debug.debugMessage(VerbosityLevel.WARNING, origin, "Could not close key listener of type [${listener.javaClass}]")
            }
            if (keyListeners.remove(listener)) {
                Debug.debugMessage(VerbosityLevel.EXTRA, origin, "KeyListener removed")
            } else {
                Debug.debugMessage(VerbosityLevel.EXTRA, origin,
                    "KeyListener not found - Cannot remove")
            }
        }
    }

    /**
     * Debug function. Wait until any key is pressed on the terminal.
     */
    fun waitForKey() {
        anyKeyLock.lock()
        anyKeySignal.await()
        anyKeyLock.unlock()
    }
}
