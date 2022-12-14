package communication

import java.text.SimpleDateFormat
import java.util.Date

class Message {
    val sender: String
    val message: String
    val time: Long

    val length: Int
    private val timeLength = 8
    private val additionalLength = 5

    constructor(sender: String, message: String, time: Long) {
        this.sender = sender
        this.message = message
        this.time = time
        this.length = additionalLength + sender.length + message.length + timeLength
    }

    constructor(sender: String, message: String) {
        this.sender = sender
        this.message = message
        this.time = System.currentTimeMillis()
        this.length = additionalLength + sender.length + message.length + timeLength
    }

    override fun toString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss")
        val formattedTime = sdf.format(Date(time))
        return "$sender [${formattedTime}]: $message"
    }
}