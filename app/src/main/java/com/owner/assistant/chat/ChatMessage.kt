package com.owner.assistant.chat

/**
 * @param role either "user" or "assistant" — provider-agnostic; [GeminiClient]
 * maps "assistant" to Gemini's own "model" role when building a request.
 */
data class ChatMessage(
    val role: String,
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
