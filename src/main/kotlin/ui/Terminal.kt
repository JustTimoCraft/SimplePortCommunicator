package ui

import ui.input.InputChecker
import ui.input.KeyListener
import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal
import com.googlecode.lanterna.terminal.TerminalResizeListener
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger
import debug.Debug
import debug.VerbosityLevel
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Simplified version of the `Terminal` wrapper from my Ultimate TicTacToe project.
 *
 * @param title The window title, if the Swing based Lanterna terminal is created.
 */
class Terminal(title: String): TerminalResizeListener {
    lateinit var terminal: Terminal
    private lateinit var inputChecker: InputChecker
    private lateinit var inputCheckerThread: Thread
    private lateinit var moveInput: KeyListener
    var initialised: Boolean = false
        protected set
    private val minimumTerminalColumns = 24
    private val minimumTerminalRows = 8
    var tooSmall = false

    init {
        try {
            terminal = DefaultTerminalFactory(System.out, System.`in`, StandardCharsets.UTF_8)
                .setTerminalEmulatorTitle(title)
                .setTerminalEmulatorFrameAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode)
                .createTerminal()
            this.initialised = true

            // Start the user input thread
            inputChecker = InputChecker(terminal)
            inputCheckerThread = Thread(inputChecker, "InputChecker")
            inputCheckerThread.start()
            terminal.setCursorVisible(false)

            // Make sure the screen is large enough to display the contents at all times
            terminal.addResizeListener(this)
        } catch (e: IOException) {
            Debug.debugMessage(VerbosityLevel.FATAL, "Terminal.<init>", "Could not initialise the terminal")
        }
    }

    fun addKeyListener(listener: KeyListener) {
        if (initialised && inputCheckerThread.isAlive && !SPC.shutdown) {
            inputChecker.addKeyListener(listener)
        } else {
            Debug.debugMessage(VerbosityLevel.ERROR, "Terminal.aKL", "Could not add key listener")
        }
    }

    fun removeKeyListener(listener: KeyListener) {
        inputChecker.removeKeyListener(listener)
    }

    /**
     * Wait for any key to be pressed on the terminal. Debug function.
     * Basically a 'press any key to continue' without the text.
     */
    fun waitForKey() {
        if (!initialised || !inputCheckerThread.isAlive || SPC.shutdown) return
        inputChecker.waitForKey()
    }

    override fun onResized(p0: Terminal?, p1: TerminalSize?) {
        if (p0 == null || p1 == null) return

        if (p1.rows < minimumTerminalRows || p1.columns < minimumTerminalColumns) {
            tooSmall = true
            showTooSmallMessage()
        } else {
            tooSmall = false
        }
    }

    fun close() {
        val origin = "Terminal.close"
        try {
            initialised = false
            Debug.debugMessage(VerbosityLevel.EXTRA, origin, "Terminal is closing")
            terminal.close()
            Debug.debugMessage(VerbosityLevel.EXTRA, "Terminal", "Terminal closed\nWaiting for input checker to terminate")
            // Input checker should automatically get the hint when the closed terminal sends an EOF key
            inputCheckerThread.join()
        } catch (e: IOException) {
            Debug.debugMessage(VerbosityLevel.WARNING, "Terminal.qG", "Could not properly deinitialise the terminal")
        }
    }

    fun showTooSmallMessage() {
        if (!initialised) return
        terminal.clearScreen()
        terminal.cursorPosition = TerminalPosition( 0, 0)
        // Strings should automatically wrap, I think
        terminal.putString("A terminal of at least ${minimumTerminalColumns}x${minimumTerminalRows} characters is required")
        newLine()
        terminal.putString("Resize and press any key to continue")
        terminal.flush()
    }
}