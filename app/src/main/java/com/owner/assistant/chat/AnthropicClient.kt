package com.owner.assistant.chat

import android.content.Context
import com.owner.assistant.data.OwnerProfileStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Thin, dependency-free client for Anthropic's Messages API
 * (https://docs.anthropic.com/en/api/messages) — this is what makes the
 * assistant actually converse, versus the fixed-phrase commands in
 * [com.owner.assistant.service.CommandRouter]. Uses the owner's own API key
 * (set via [ChatActivity]'s settings dialog); nothing is bundled or shared —
 * every phone running this app calls Anthropic with its owner's own key and
 * pays for its own usage.
 *
 * Blocking by design: every caller (a background thread in
 * [AssistantForegroundService][com.owner.assistant.service.AssistantForegroundService],
 * or an explicit background thread in [ChatActivity]) is already off the
 * main thread before calling this.
 */
object AnthropicClient {

    class ApiKeyMissingException : Exception("No Anthropic API key set")

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val ANTHROPIC_VERSION = "2023-06-01"
    private const val MAX_TOKENS = 1024
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    private const val SYSTEM_PROMPT =
        "You are the conversational layer of a personal Android assistant app " +
            "running on the owner's own phone. Keep replies short and natural — " +
            "they're usually read aloud by text-to-speech. The app also has separate " +
            "fixed voice commands (reminders, camera, phone lock, call control, " +
            "emergency alerts, ad blocking, a security scan, and translation) that " +
            "you do not control directly — if asked to do one of those, tell the " +
            "owner the phrase to say (e.g. \"say 'remind me at 5 for the meeting'\") " +
            "rather than claiming you did it yourself."

    /** @throws ApiKeyMissingException if no key is configured. @throws IOException on any network/API failure. */
    fun sendMessageBlocking(context: Context, history: List<ChatMessage>): String {
        val apiKey = OwnerProfileStore.getAnthropicApiKey(context) ?: throw ApiKeyMissingException()
        val model = OwnerProfileStore.getAnthropicModel(context)

        val messagesJson = JSONArray()
        history.forEach { message ->
            messagesJson.put(JSONObject().put("role", message.role).put("content", message.content))
        }

        val requestBody = JSONObject()
            .put("model", model)
            .put("max_tokens", MAX_TOKENS)
            .put("system", SYSTEM_PROMPT)
            .put("messages", messagesJson)

        val connection = (URL(ENDPOINT).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
            setRequestProperty("content-type", "application/json")
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }

        try {
            connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

            val successful = connection.responseCode in 200..299
            val stream = if (successful) connection.inputStream else connection.errorStream
            val responseText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            if (!successful) {
                val apiMessage = runCatching {
                    JSONObject(responseText).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IOException("Claude API error (${connection.responseCode}): ${apiMessage ?: responseText.take(200)}")
            }

            val content = JSONObject(responseText).getJSONArray("content")
            val text = StringBuilder()
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "text") text.append(block.optString("text"))
            }
            return text.toString().ifBlank { "..." }
        } finally {
            connection.disconnect()
        }
    }
}
