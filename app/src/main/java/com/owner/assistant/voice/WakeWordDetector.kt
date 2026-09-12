package com.owner.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.owner.assistant.data.OwnerProfileStore
import java.io.ByteArrayOutputStream

/**
 * Always-listening wake-word + command capture loop.
 *
 * Honest limitation vs. the blueprint spec: a *true* offline wake-word engine
 * (Porcupine, Vosk) runs a tiny keyword-spotting model continuously at very
 * low power and never sends audio to a cloud recognizer. Wiring one of those
 * in requires a proprietary access key (Porcupine) or bundling a multi-MB
 * language model as an asset (Vosk) — neither is something this codebase can
 * ship for you sight-unseen. Until you plug one of those in, this class uses
 * Android's built-in [SpeechRecognizer] in a continuous restart loop as a
 * pragmatic stand-in: it listens for full utterances, checks the recognized
 * text against the configured wake phrase, and treats anything said right
 * after the wake phrase as the command. It uses more battery than a real
 * wake-word engine and (depending on the OEM) may use an online recognizer
 * rather than a fully offline one — see README "Known limitations".
 *
 * To swap in Porcupine/Vosk later: implement the same [Listener] callback
 * from their detection callback instead of [onResults], and feed the audio
 * frames they capture into [AudioFeatureExtractor] exactly as done here.
 */
class WakeWordDetector(
    private val context: Context,
    private val listener: Listener
) : RecognitionListener {

    interface Listener {
        fun onCommandCaptured(text: String, rawAudio: ShortArray)
        fun onListeningStateChanged(isListening: Boolean) {}
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var audioBytes = ByteArrayOutputStream()
    private var awaitingCommandAfterWake = false
    private var running = false

    fun start() {
        if (running) return
        running = true
        mainHandler.post { beginCycle() }
    }

    fun stop() {
        running = false
        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
            listener.onListeningStateChanged(false)
        }
    }

    private fun beginCycle() {
        if (!running) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "No speech recognizer available on this device")
            return
        }

        audioBytes = ByteArrayOutputStream()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@WakeWordDetector)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
        }
        recognizer?.startListening(intent)
        listener.onListeningStateChanged(true)
    }

    private fun scheduleRestart(delayMs: Long = 300) {
        if (!running) return
        mainHandler.postDelayed({ beginCycle() }, delayMs)
    }

    private fun bytesToShorts(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        for (i in shorts.indices) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            shorts[i] = ((hi shl 8) or lo).toShort()
        }
        return shorts
    }

    private fun handleRecognizedText(text: String) {
        val normalized = text.lowercase().trim()
        val wakePhrase = OwnerProfileStore.getWakePhrase(context)
        val rawAudio = bytesToShorts(audioBytes.toByteArray())

        if (!awaitingCommandAfterWake) {
            if (normalized.contains(wakePhrase)) {
                val remainder = normalized.substringAfter(wakePhrase).trim()
                if (remainder.isNotBlank()) {
                    listener.onCommandCaptured(remainder, rawAudio)
                    awaitingCommandAfterWake = false
                } else {
                    // Wake phrase alone: listen once more for the actual command.
                    awaitingCommandAfterWake = true
                }
            }
        } else {
            listener.onCommandCaptured(normalized, rawAudio)
            awaitingCommandAfterWake = false
        }
    }

    // ---- RecognitionListener ----

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {
        buffer?.let { audioBytes.write(it) }
    }

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        listener.onListeningStateChanged(false)
        scheduleRestart()
    }

    override fun onResults(results: Bundle?) {
        listener.onListeningStateChanged(false)
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
        if (text != null) handleRecognizedText(text)
        scheduleRestart()
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    companion object {
        private const val TAG = "WakeWordDetector"
    }
}
