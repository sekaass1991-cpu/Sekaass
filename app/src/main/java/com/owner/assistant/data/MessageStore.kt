package com.owner.assistant.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

data class CapturedMessage(
    val appLabel: String,
    val sender: String,
    val text: String,
    val timestampMillis: Long
)

/**
 * In-memory ring buffer of recent messaging notifications, for
 * "what messages did I get?" readout. Deliberately never touches disk or
 * network — see blueprint's privacy note on NotificationReaderService.
 */
object MessageStore {

    private const val MAX_MESSAGES = 25
    private val messages = ConcurrentLinkedDeque<CapturedMessage>()

    fun add(message: CapturedMessage) {
        messages.addFirst(message)
        while (messages.size > MAX_MESSAGES) messages.removeLast()
    }

    fun latest(count: Int = 5): List<CapturedMessage> =
        messages.take(count)

    fun summarizeForSpeech(count: Int = 5): String {
        val recent = latest(count)
        if (recent.isEmpty()) return "You have no new messages."
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return recent.joinToString(". ") { msg ->
            "From ${msg.sender} on ${msg.appLabel} at ${timeFormat.format(msg.timestampMillis)}: ${msg.text}"
        }
    }

    fun clear() = messages.clear()
}
