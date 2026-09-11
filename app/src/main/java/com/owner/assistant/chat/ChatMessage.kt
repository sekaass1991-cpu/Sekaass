package com.owner.assistant.chat

/** @param role either "user" or "assistant", matching the Anthropic Messages API's role field. */
data class ChatMessage(
    val role: String,
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
