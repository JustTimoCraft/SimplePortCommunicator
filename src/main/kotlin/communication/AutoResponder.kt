package communication

interface AutoResponder {
    /**
     * If this field is set to `false`, [messageMatches] will always return `false`.
     */
    var enabled: Boolean

    /**
     * Check if a message sent by the server matches this autoresponder.
     * @param message The message sent by the server.
     * @return `true` if this message should be responded to by this [AutoResponder].
     */
    fun messageMatches(message: String): Boolean

    /**
     * Get the response from this [AutoResponder].
     * @return The response to be sent back to the server.
     * @throws IllegalStateException If this [AutoResponder] requires the original message to generate the response,
     * and [messageMatches] was not called prior to calling this method.
     */
    fun responseMessage(): String

    /**
     * Alter the message to match to a new one.
     * @param message The new message this [AutoResponder] should check for.
     * @throws UnsupportedOperationException If this [AutoResponder] implementation does not support updating the
     * incoming message. More advanced implementations may throw this.
     */
    fun setMessage(message: String)

    /**
     * Get the message this [AutoResponder] checks for.
     * @return The message.
     */
    fun getMessage(): String

    /**
     * Alter the response message to a new one.
     * @param response The new response this [AutoResponder] should return after [messageMatches] returned `true`.
     * @throws UnsupportedOperationException If this [AutoResponder] implementation does not support updating the
     * response message. More advanced implementations may throw this.
     */
    fun setResponse(response: String)

    /**
     * Get a human-readable version of the response. In basic implementations, this method will be identical to [responseMessage],
     * but in more advanced implementations, [responseMessage] can throw exceptions, and may generate the response based on
     * the incoming message. This function, instead, prints a human-readable template for the response. It must not throw an exception.
     *
     * ## Example
     * Suppose an implementation where incoming messages in the form `DOUBLE <x>` should meet a response of `2*x` as a numeric value.
     *
     * Given no prior input, [responseMessage] should throw a [IllegalStateException], and [getResponse] should return `2 * x`
     * Given input `DOUBLE 5`, [responseMessage] should return `10`, and [getResponse] should return `2 * x`
     *
     * @return The response message or response template.
     */
    fun getResponse(): String
}