package ui.input

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

/**
 * Terminal key listener that returns when any key is pressed that is not a normal character key. The keys that are
 * returned are keys like the arrow keys, enter, backspace, the function keys, etc. Formally: Any key that registers as
 * *not* `KeyType.Character`.
 */
class SpecialKeyListener: KeyListener() {
    override fun sendKey(key: KeyStroke) {
        if (key.keyType != KeyType.Character) {
            super.lock.lock()
            super.buffer = key
            super.keyReceived.signal()
            super.lock.unlock()
        }
    }
}