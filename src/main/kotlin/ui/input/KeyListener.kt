package ui.input

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import debug.Debug
import debug.VerbosityLevel
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Terminal key listener. This object can be added to the terminal listeners, after which a thread can wait on the
 * `getKey()` method until a key is returned. If multiple threads need to receive keys at the same time, separate
 * key listener objects should be used.
 */
open class KeyListener {
    protected val lock: Lock = ReentrantLock()
    protected val keyReceived: Condition = lock.newCondition()
    protected var buffer: KeyStroke = KeyStroke(KeyType.Unknown)

    /**
     * Get the most recent key input after the thread has invoked this function. If the returned KeyType is `KeyType.EOF`
     * the terminal has closed.
     */
    open fun getKey(): KeyStroke {
        lock.lock()
        try {
            keyReceived.await()
            lock.unlock()
            return buffer
        } catch (e: InterruptedException) {
            lock.unlock()
            Debug.debugMessage(VerbosityLevel.WARNING, "KeyListener.gK",
                "Thread interrupted while waiting for key")
            Thread.currentThread().interrupt()
        }
        return KeyStroke(KeyType.Unknown) // Should be unreachable
    }

    /**
     * Send a key to the thread waiting on this object's `getKey()` method. This method is to be called by the terminal's
     * input system.
     */
    open fun sendKey(key: KeyStroke) {
        lock.lock() // Acquire the lock
        buffer = key // Set the key
        keyReceived.signal() // Signal a potentially waiting thread
        lock.unlock() // Release the lock
    }

    /**
     * Is called by the terminal when the terminal closes. It sends KeyType.EOF to the waiting threads.
     */
    open fun close() {
        lock.lock()
        buffer = KeyStroke(KeyType.EOF)
        keyReceived.signal()
        lock.unlock()
    }
}