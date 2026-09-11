package com.owner.assistant.chat

import android.content.Context
import android.util.Log
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.util.SpeechOutput

/**
 * Where [com.owner.assistant.service.CommandRouter] sends anything that
 * isn't one of the fixed phone-control phrases — this is what makes the
 * always-listening assistant hold an actual conversation instead of just
 * logging "no matching handler." Shares [ChatHistoryStore] with
 * [ChatActivity] so a follow-up by voice after typing (or vice versa) keeps
 * context.
 *
 * Called from a background thread already (CommandRouter runs off the main
 * thread inside AssistantForegroundService) — this makes a blocking network
 * call via [AnthropicClient], so never call it from the main thread.
 */
object AssistantChat {

    fun respondTo(context: Context, userText: String) {
        if (OwnerProfileStore.getAnthropicApiKey(context) == null) {
            SpeechOutput.speak("I don't have conversation mode set up yet. Open the chat screen in the app to add your API key.")
            return
        }

        ChatHistoryStore.add("user", userText)
        try {
            val reply = AnthropicClient.sendMessageBlocking(context, ChatHistoryStore.all())
            ChatHistoryStore.add("assistant", reply)
            SpeechOutput.speak(reply)
        } catch (e: Exception) {
            Log.e(TAG, "Claude request failed", e)
            SpeechOutput.speak("I couldn't reach the assistant service. Please check your internet connection or API key.")
        }
    }

    private const val TAG = "AssistantChat"
}
