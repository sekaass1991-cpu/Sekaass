package com.owner.assistant.chat

import android.content.Context
import android.net.Uri
import com.owner.assistant.data.OwnerProfileStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Thin, dependency-free client for Google's Gemini API
 * (https://ai.google.dev/api/generate-content) — this is what makes the
 * assistant actually converse, versus the fixed-phrase commands in
 * [com.owner.assistant.service.CommandRouter]. Chosen over Anthropic's Claude
 * API specifically because Google AI Studio issues API keys with a genuine
 * free tier (rate-limited, no credit card required to start) — see README
 * for the sign-up steps. Uses the owner's own key (set via [ChatActivity]'s
 * settings dialog); nothing is bundled or shared.
 *
 * Blocking by design: every caller (a background thread in
 * [AssistantForegroundService][com.owner.assistant.service.AssistantForegroundService],
 * or an explicit background thread in [ChatActivity]) is already off the
 * main thread before calling this.
 */
object GeminiClient {

    class ApiKeyMissingException : Exception("No Gemini API key set")

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val MAX_OUTPUT_TOKENS = 1024
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
        val apiKey = OwnerProfileStore.getGeminiApiKey(context) ?: throw ApiKeyMissingException()
        val model = OwnerProfileStore.getGeminiModel(context)

        val contents = JSONArray()
        history.forEach { message ->
            // Gemini uses "model" for the assistant's turns, not "assistant".
            val role = if (message.role == "assistant") "model" else "user"
            contents.put(
                JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", message.content)))
            )
        }

        val requestBody = JSONObject()
            .put("contents", contents)
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
            )
            .put("generationConfig", JSONObject().put("maxOutputTokens", MAX_OUTPUT_TOKENS))

        val url = Uri.parse("$BASE_URL/$model:generateContent")
            .buildUpon()
            .appendQueryParameter("key", apiKey)
            .build()

        val connection = (URL(url.toString()).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
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
                throw IOException("Gemini API error (${connection.responseCode}): ${apiMessage ?: responseText.take(200)}")
            }

            val candidates = JSONObject(responseText).optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
                ?: throw IOException("Gemini returned no candidates (it may have blocked the response for safety)")
            val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")

            val text = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    text.append(parts.optJSONObject(i)?.optString("text") ?: "")
                }
            }
            return text.toString().ifBlank { "..." }
        } finally {
            connection.disconnect()
        }
    }
}
