package communication

class BasicAutoResponder(private var msg: String, private var response: String): AutoResponder {
    override var enabled: Boolean = true

    override fun messageMatches(message: String): Boolean {
        return message == msg && enabled
    }

    override fun responseMessage(): String {
        return response
    }

    override fun setMessage(message: String) {
        msg = message
    }

    override fun setResponse(response: String) {
        this.response = response
    }

    override fun getMessage(): String {
        return msg
    }

    override fun getResponse(): String {
        return response
    }
}