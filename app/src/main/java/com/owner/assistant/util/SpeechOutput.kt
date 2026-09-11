package com.owner.assistant.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_ADD
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * App-wide text-to-speech sink. Every feature that "speaks" a result back to
 * the owner (reminders, message readout, emergency confirmation, translator
 * output) goes through this single instance instead of each creating and
 * leaking its own [TextToSpeech] engine.
 */
object SpeechOutput {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pendingQueue = mutableListOf<Pair<String, Locale>>()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                pendingQueue.forEach { (text, locale) -> speakInternal(text, locale) }
                pendingQueue.clear()
            } else {
                Log.w("SpeechOutput", "TextToSpeech init failed with status $status")
            }
        }
    }

    fun speak(text: String, locale: Locale = Locale.getDefault()) {
        if (!ready) {
            pendingQueue.add(text to locale)
            return
        }
        speakInternal(text, locale)
    }

    private fun speakInternal(text: String, locale: Locale) {
        val engine = tts ?: return
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.speak(text, QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
