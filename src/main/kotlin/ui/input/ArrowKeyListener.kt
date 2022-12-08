package ui.input

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

/**
 * Nearly the same as `KeyListener`, except that this class only wakes when the arrow keys are used.
 * These keys are
 */
class ArrowKeyListener: KeyListener() {
    override fun sendKey(key: KeyStroke) {
        if (key.keyType == KeyType.ArrowUp || key.keyType == KeyType.ArrowRight ||
            key.keyType == KeyType.ArrowDown || key.keyType == KeyType.ArrowLeft) {
            super.lock.lock()
            super.buffer = key
            super.keyReceived.signal()
            super.lock.unlock()
        }
    }
}