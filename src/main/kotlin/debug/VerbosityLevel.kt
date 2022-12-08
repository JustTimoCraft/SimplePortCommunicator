package debug

/**
 * All possible verbosity levels of the debug class.
 */
enum class VerbosityLevel {
    /** No messages will be printed in the console  */
    SILENT,

    /** Only fatal error messages, which cause the program to exit, will be shown  */
    FATAL,

    /** Fatal and severe error messages will be shown  */
    SEVERE,

    /** All error messages, fatal and non-fatal, will be shown  */
    ERROR,

    /** Error messages and warnings will be shown  */
    WARNING,

    /** Error messages, warnings and generic info will be shown  */
    INFO,

    /** Everything will be shown, generates a tonne of output  */
    EXTRA
}