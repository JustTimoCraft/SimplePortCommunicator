package debug

/**
 * Global class for debugging purposes. All methods that are used for debugging should
 * be contained within this class.
 */
object Debug {
    private var vb = VerbosityLevel.INFO

    /**
     * Sets the verbosity level of the debug messages in the entire program.
     * Verbosity level meanings are explained in the `VerbosityLevel` class
     * documentation.
     * @param verbosity The verbosity level to be the new application-wide value.
     */
    fun setVerbosityLevel(verbosity: VerbosityLevel) {
        vb = verbosity
    }

    /**
     * Print a debug message to System err, if the verbosity setting is high enough.
     * **This method should be used as little as possible. Please use the equivalent
     * with three parameters instead!**
     *
     * @param verbosity The verbosity level of this message.
     * @param message   The debug message text.
     */
    fun debugMessage(verbosity: VerbosityLevel, message: String) {
        if (vb != VerbosityLevel.SILENT && vb.ordinal >= verbosity.ordinal) {
            System.err.printf("Debug %s >> %s", verbosity, message)
        }
    }

    /**
     * Print a debug message to System out or System err, if the verbosity setting is high enough.
     * If the message to show consists of multiple lines, it will be handled the same as if the
     * lines were each sent separately.
     *
     * @param verbosity The verbosity level of this message.
     * @param origin    Usually the class and/or method whence this method is called.
     * @param message   The debug message text.
     */
    fun debugMessage(verbosity: VerbosityLevel, origin: String, message: String) {
        // Don't do any unnecessary calculation if we're not going to show the message anyway
        if (vb.ordinal < verbosity.ordinal) {
            return
        }

        // Generate the start of the message only once
        val debugMessageStart = String.format("Debug <%s> %s >> ", origin, verbosity)
        // Print for each line, if there are multiple
        val messages = message.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in messages) {
            val debugMessage = debugMessageStart + line
            if (vb.ordinal >= VerbosityLevel.WARNING.ordinal) {
                // System out for WARNING, INFO and EXTRA
                println(debugMessage)
            } else {
                // System err for FATAL, SEVERE and ERROR
                System.err.println(debugMessage)
            }
        }
    }
}