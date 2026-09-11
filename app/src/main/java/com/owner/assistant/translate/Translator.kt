package com.owner.assistant.translate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.owner.assistant.util.SpeechOutput
import java.util.Locale

/**
 * Feature F: real-time speech-to-speech translation between English and
 * Tamil/Hindi/Telugu, using on-device ML Kit Translate + language
 * identification. The owner says "translate to tamil", then speaks
 * normally; each utterance is auto-detected, translated, and spoken back.
 *
 * Malayalam (also requested in the blueprint) isn't in ML Kit Translate's
 * on-device language set — see the comment on [LANGUAGE_NAME_TO_CODE].
 *
 * Known limitation: this reuses the phone's single speech recognizer, so a
 * translation session and the always-on wake-word loop
 * ([com.owner.assistant.voice.WakeWordDetector]) briefly compete for the
 * microphone — say "stop translating" to hand it back cleanly. ML Kit
 * downloads each language's model (a few MB) the first time it's used, so
 * the first translation into a new language needs a network connection.
 */
class Translator(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var targetLanguage: String? = null
    private var running = false

    fun startSession(targetLanguageCode: String) {
        targetLanguage = targetLanguageCode
        running = true
        SpeechOutput.speak("Translation mode on. Say something and I will translate it.")
        mainHandler.post { listenNext() }
    }

    fun stopSession() {
        running = false
        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
        }
        SpeechOutput.speak("Translation mode off.")
    }

    private fun listenNext() {
        if (!running) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    mainHandler.postDelayed({ listenNext() }, 400)
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) translateAndSpeak(text)
                    mainHandler.postDelayed({ listenNext() }, 400)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
        }
        recognizer?.startListening(intent)
    }

    private fun translateAndSpeak(sourceText: String) {
        val target = targetLanguage ?: return
        LanguageIdentification.getClient().identifyLanguage(sourceText)
            .addOnSuccessListener { detected ->
                val sourceLang = if (detected == "und") TranslateLanguage.ENGLISH else detected
                translateWith(sourceLang, target, sourceText)
            }
            .addOnFailureListener {
                translateWith(TranslateLanguage.ENGLISH, target, sourceText)
            }
    }

    private fun translateWith(sourceLang: String, targetLang: String, text: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val mlTranslator = Translation.getClient(options)

        mlTranslator.downloadModelIfNeeded()
            .addOnSuccessListener {
                mlTranslator.translate(text)
                    .addOnSuccessListener { translated ->
                        SpeechOutput.speak(translated, localeFor(targetLang))
                        mlTranslator.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Translation failed", e)
                        mlTranslator.close()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Model download failed", e)
                SpeechOutput.speak("I need Wi-Fi to download that language the first time.")
                mlTranslator.close()
            }
    }

    private fun localeFor(languageCode: String): Locale = when (languageCode) {
        TranslateLanguage.HINDI -> Locale("hi", "IN")
        TranslateLanguage.TAMIL -> Locale("ta", "IN")
        TranslateLanguage.TELUGU -> Locale("te", "IN")
        else -> Locale.US
    }

    companion object {
        private const val TAG = "Translator"

        // ML Kit Translate's on-device model set (~59 languages) does not include
        // Malayalam — there is no TranslateLanguage.MALAYALAM constant. The
        // blueprint asks for all four; Hindi/Tamil/Telugu work on-device here,
        // and Malayalam would need swapping in a cloud API (e.g. Google Cloud
        // Translation) for that one language, which trades away the "runs
        // on-device, no network required after first download" property the
        // other three have. Left as a follow-up rather than silently claiming
        // support that doesn't exist.
        private val LANGUAGE_NAME_TO_CODE = mapOf(
            "english" to TranslateLanguage.ENGLISH,
            "hindi" to TranslateLanguage.HINDI,
            "tamil" to TranslateLanguage.TAMIL,
            "telugu" to TranslateLanguage.TELUGU
        )

        fun languageCodeFor(spokenName: String): String? =
            LANGUAGE_NAME_TO_CODE[spokenName.lowercase().trim()]
    }
}
