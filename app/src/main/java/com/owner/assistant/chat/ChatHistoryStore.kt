package com.owner.assistant.chat

import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory conversation history shared between the on-screen [ChatActivity]
 * and voice commands that fall through to [AssistantChat] — so asking a
 * follow-up question by voice after typing (or vice versa) keeps context.
 * Deliberately not persisted to disk: this is conversation content, not a
 * durable record, and keeping it in memory only means it clears on app
 * restart like the rest of this app's local-first, no-cloud-sync design.
 */
object ChatHistoryStore {

    private const val MAX_MESSAGES = 20
    private val messages = CopyOnWriteArrayList<ChatMessage>()

    fun add(role: String, content: String) {
        messages.add(ChatMessage(role, content))
        while (messages.size > MAX_MESSAGES) messages.removeAt(0)
    }

    fun all(): List<ChatMessage> = messages.toList()

    fun clear() = messages.clear()
}
