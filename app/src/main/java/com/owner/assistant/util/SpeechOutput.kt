package com.owner.assistant.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_ADD
import android.speech.tts.Voice
import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.owner.assistant.data.AssistantPrefs
import java.util.Locale
import java.util.UUID

/**
 * App-wide text-to-speech sink. Every feature that "speaks" a result back to
 * the owner (reminders, message readout, emergency confirmation, translator
 * output, chat replies) goes through this single instance instead of each
 * creating and leaking its own [TextToSpeech] engine.
 *
 * Two things worth knowing about the "girl voice, speak every language"
 * request this was built for:
 *
 * - Android's public TTS API has no documented "gender" field on [Voice] —
 *   only whatever engine (usually Google Text-to-Speech) is installed knows
 *   that, and it isn't exposed consistently. A pinned voice (set via the
 *   chat screen's voice picker) always wins; failing that, a best-effort
 *   match against voice names that hint at gender is tried (works for many
 *   of Google TTS's classic per-language voices, e.g. names containing
 *   "female"); failing that, the engine's own default for the language is
 *   used as-is. There is no guaranteed way to force a specific gender across
 *   every device/engine — the chat screen's "choose a voice and preview it"
 *   picker is the reliable way to actually get what you want.
 * - "Every language in the world" isn't something any TTS engine does —
 *   Google's engine covers on the order of 40-50 languages with installed
 *   voice packs. [speakAuto] detects which language a piece of text is in
 *   (via ML Kit) and picks the closest matching installed voice/locale
 *   automatically, which covers "reply in whatever language you asked in"
 *   for everything the device's engine actually supports.
 */
object SpeechOutput {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var appContext: Context? = null
    private val pendingQueue = mutableListOf<Pair<String, Locale>>()
    private val pendingAutoQueue = mutableListOf<String>()

    private val FEMALE_NAME_HINTS = listOf("female", "#female", "-f-", "_f_", "woman")

    fun init(context: Context) {
        appContext = context.applicationContext
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                pendingQueue.forEach { (text, locale) -> speakInternal(text, locale) }
                pendingQueue.clear()
                pendingAutoQueue.forEach { speakAuto(it) }
                pendingAutoQueue.clear()
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

    /**
     * Detects what language [text] is actually in before speaking it — for
     * chat replies, which (unlike a fixed command's confirmation) can come
     * back in whatever language the owner asked their question in. Falls
     * back to the device's default locale if detection fails.
     */
    fun speakAuto(text: String) {
        if (!ready) {
            pendingAutoQueue.add(text)
            return
        }
        LanguageIdentification.getClient().identifyLanguage(text)
            .addOnSuccessListener { code ->
                val locale = if (code.isNullOrBlank() || code == "und") Locale.getDefault() else Locale.forLanguageTag(code)
                speakInternal(text, locale)
            }
            .addOnFailureListener {
                speakInternal(text, Locale.getDefault())
            }
    }

    /** All voices the active TTS engine currently offers — for a settings screen to list/preview. */
    fun availableVoices(): List<Voice> = tts?.voices?.toList() ?: emptyList()

    /** Speaks a short sample with a specific voice, bypassing locale/gender matching — for previewing in a picker. */
    fun previewVoice(voice: Voice, sampleText: String = "This is a preview of this voice.") {
        val engine = tts ?: return
        engine.voice = voice
        engine.speak(sampleText, QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    /** Pins a specific voice by name so [speak]/[speakAuto] use it whenever its language matches. */
    fun setPreferredVoice(context: Context, voice: Voice) {
        AssistantPrefs.setPreferredVoiceName(context, voice.name)
    }

    private fun speakInternal(text: String, locale: Locale) {
        val engine = tts ?: return
        val result = engine.setLanguage(locale)
        val effectiveLocale = if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
            Locale.US
        } else {
            locale
        }
        selectPreferredVoice(engine, effectiveLocale)
        engine.speak(text, QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    private fun selectPreferredVoice(engine: TextToSpeech, locale: Locale) {
        try {
            val voices = engine.voices ?: return
            val pinnedName = appContext?.let { AssistantPrefs.getPreferredVoiceName(it) }
            val pinned = pinnedName?.let { name -> voices.firstOrNull { it.name == name && it.locale.language == locale.language } }
            if (pinned != null) {
                engine.voice = pinned
                return
            }

            val femaleForLocale = voices.firstOrNull { voice ->
                voice.locale.language == locale.language &&
                    !voice.isNetworkConnectionRequired &&
                    FEMALE_NAME_HINTS.any { hint -> voice.name.contains(hint, ignoreCase = true) }
            }
            if (femaleForLocale != null) engine.voice = femaleForLocale
            // Otherwise leave the engine's own default voice for this language —
            // better than guessing wrong when no gender hint is available.
        } catch (e: Exception) {
            Log.w("SpeechOutput", "Voice selection failed", e)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
