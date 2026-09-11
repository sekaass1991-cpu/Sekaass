package com.owner.assistant.chat

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.Voice
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.owner.assistant.R
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.util.SpeechOutput
import kotlin.concurrent.thread

/**
 * The "controllable, talks like ChatGPT/Claude" screen: type or hold the mic
 * button to talk, see the conversation on screen, hear replies spoken back.
 * This is a manual front-end onto the same [GeminiClient] and
 * [ChatHistoryStore] that voice commands fall through to from
 * [com.owner.assistant.service.CommandRouter] when they don't match a fixed
 * phone-control phrase — so the two stay in the same conversation. Uses
 * Google's Gemini API rather than a paid-only provider specifically because
 * Google AI Studio issues free-tier keys (see README).
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var micButton: Button
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpeechOutput.init(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(ContextCompat.getColor(this@ChatActivity, R.color.background))
        }

        root.addView(buildHeader())

        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
        }
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(messagesContainer)
        }
        root.addView(scrollView)

        root.addView(buildInputRow())
        setContentView(root)

        renderHistory()
        if (OwnerProfileStore.getGeminiApiKey(this) == null) {
            showApiKeyDialog(initialSetup = true)
        }
    }

    private fun buildHeader(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 72, 24, 24)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@ChatActivity, R.color.primary))
        }
        row.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@ChatActivity).apply {
                text = "Personal Assistant"
                textSize = 18f
                setTextColor(Color.WHITE)
            })
            addView(TextView(this@ChatActivity).apply {
                text = "Chat"
                textSize = 13f
                setTextColor(Color.parseColor("#D8D4FF"))
            })
        })
        row.addView(headerIconButton("Voice") { showVoicePickerDialog() })
        row.addView(headerIconButton("API key") { showApiKeyDialog(initialSetup = false) })
        return row
    }

    private fun headerIconButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 13f
        setTextColor(Color.WHITE)
        isClickable = true
        isFocusable = true
        setPadding(20, 12, 20, 12)
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
        setOnClickListener { action() }
    }

    private fun buildInputRow(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 24)
            gravity = Gravity.CENTER_VERTICAL
        }

        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(this@ChatActivity, R.drawable.bg_input_pill)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 16
            }
        }
        input = EditText(this).apply {
            hint = "Message..."
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        pill.addView(input)

        micButton = Button(this).apply {
            text = "🎤"
            background = ContextCompat.getDrawable(this@ChatActivity, R.drawable.bg_circle_accent)
            setTextColor(Color.WHITE)
            minWidth = 0
            minimumWidth = 0
            setPadding(20, 20, 20, 20)
            setOnClickListener { toggleVoiceInput() }
        }
        pill.addView(micButton)

        val sendButton = Button(this).apply {
            text = "➤"
            background = ContextCompat.getDrawable(this@ChatActivity, R.drawable.bg_circle_primary)
            setTextColor(Color.WHITE)
            minWidth = 0
            minimumWidth = 0
            setPadding(20, 20, 20, 20)
            setOnClickListener { sendTypedMessage() }
        }

        outer.addView(pill)
        outer.addView(sendButton)
        return outer
    }

    private fun sendTypedMessage() {
        val text = input.text.toString().trim()
        if (text.isBlank()) return
        input.setText("")
        sendMessage(text)
    }

    private fun sendMessage(text: String) {
        addBubble(role = "user", text = text)
        ChatHistoryStore.add("user", text)
        val thinkingBubble = addBubble(role = "assistant", text = "...")

        thread {
            try {
                val reply = GeminiClient.sendMessageBlocking(this, ChatHistoryStore.all())
                ChatHistoryStore.add("assistant", reply)
                runOnUiThread {
                    thinkingBubble.text = reply
                    SpeechOutput.speakAuto(reply)
                }
            } catch (e: GeminiClient.ApiKeyMissingException) {
                runOnUiThread {
                    thinkingBubble.text = "No API key set — tap \"API key\" above to add one."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    thinkingBubble.text = "Couldn't reach Gemini: ${e.message ?: "unknown error"}"
                }
            }
        }
    }

    private fun addBubble(role: String, text: String): TextView {
        val isUser = role == "user"
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 15f
            background = ContextCompat.getDrawable(
                this@ChatActivity, if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_assistant
            )
            setTextColor(ContextCompat.getColor(this@ChatActivity, if (isUser) R.color.bubble_user_text else R.color.bubble_assistant_text))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            maxWidth = (resources.displayMetrics.widthPixels * 0.78).toInt()
        }
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(0, 8, 0, 8)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(bubble)
        }
        messagesContainer.addView(wrapper)
        scrollView.post { scrollView.fullScroll(ViewGroup.FOCUS_DOWN) }
        return bubble
    }

    private fun renderHistory() {
        ChatHistoryStore.all().forEach { addBubble(it.role, it.content) }
    }

    private fun toggleVoiceInput() {
        if (isListening) {
            recognizer?.stopListening()
            return
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition isn't available on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        isListening = true
        micButton.text = "■" // stop square, indicates listening
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    isListening = false
                    micButton.text = "🎤"
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    micButton.text = "🎤"
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) sendMessage(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            })
        }
    }

    /**
     * Android's TTS API has no reliable, documented "gender" field, so
     * [SpeechOutput] only auto-picks a female-sounding voice when the
     * engine's own voice names hint at it. This picker is the guaranteed
     * way to actually get the voice you want: browse, preview, and pin one.
     */
    private fun showVoicePickerDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        layout.addView(TextView(this).apply {
            text = "Filter by language code (e.g. en, ta, hi, es, fr) — blank shows your device's language."
        })
        val filterInput = EditText(this).apply { hint = "Language code" }
        layout.addView(filterInput)

        val resultsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun renderVoices(languageFilter: String) {
            resultsContainer.removeAllViews()
            val effectiveFilter = languageFilter.ifBlank { java.util.Locale.getDefault().language }
            val matches = SpeechOutput.availableVoices()
                .filter { it.locale.language.equals(effectiveFilter, ignoreCase = true) && !it.isNetworkConnectionRequired }
                .sortedBy { it.name }
                .take(MAX_VOICE_RESULTS)

            if (matches.isEmpty()) {
                resultsContainer.addView(TextView(this).apply {
                    text = "No installed voices found for \"$effectiveFilter\". Try a different code, or leave it blank."
                })
                return
            }
            matches.forEach { voice -> resultsContainer.addView(buildVoiceRow(voice)) }
        }

        filterInput.setOnEditorActionListener { _, _, _ ->
            renderVoices(filterInput.text.toString().trim())
            true
        }
        layout.addView(Button(this).apply {
            text = "Search"
            setOnClickListener { renderVoices(filterInput.text.toString().trim()) }
        })
        layout.addView(ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800)
            addView(resultsContainer)
        })

        renderVoices("")

        AlertDialog.Builder(this)
            .setTitle("Choose a voice")
            .setView(layout)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun buildVoiceRow(voice: Voice): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }
        row.addView(TextView(this).apply {
            text = "${voice.locale.displayName}\n${voice.name}"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(Button(this).apply {
            text = "▶"
            setOnClickListener { SpeechOutput.previewVoice(voice) }
        })
        row.addView(Button(this).apply {
            text = "Use"
            setOnClickListener {
                SpeechOutput.setPreferredVoice(this@ChatActivity, voice)
                Toast.makeText(this@ChatActivity, "Using this voice from now on", Toast.LENGTH_SHORT).show()
            }
        })
        return row
    }

    private fun showApiKeyDialog(initialSetup: Boolean) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }
        val keyInput = EditText(this).apply {
            hint = "Gemini API key (AIza...)"
            setText(OwnerProfileStore.getGeminiApiKey(this@ChatActivity) ?: "")
        }
        val modelInput = EditText(this).apply {
            hint = "Model ID"
            setText(OwnerProfileStore.getGeminiModel(this@ChatActivity))
        }
        layout.addView(TextView(this).apply {
            text = if (initialSetup) {
                "Add a free Gemini API key to enable real conversation. Get one at aistudio.google.com/apikey — free tier, no credit card needed (rate-limited)."
            } else {
                "Update your Gemini API key or model."
            }
        })
        layout.addView(keyInput)
        layout.addView(modelInput)

        AlertDialog.Builder(this)
            .setTitle("Gemini API settings")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val key = keyInput.text.toString().trim()
                val model = modelInput.text.toString().trim()
                if (key.isNotBlank()) OwnerProfileStore.setGeminiApiKey(this, key)
                if (model.isNotBlank()) OwnerProfileStore.setGeminiModel(this, model)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(!initialSetup)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            toggleVoiceInput()
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }

    companion object {
        private const val RECORD_AUDIO_REQUEST_CODE = 2001
        private const val MAX_VOICE_RESULTS = 30
    }
}
