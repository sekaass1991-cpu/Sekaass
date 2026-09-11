package com.owner.assistant.chat

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.util.SpeechOutput
import kotlin.concurrent.thread

/**
 * The "controllable, talks like ChatGPT/Claude" screen: type or hold the mic
 * button to talk, see the conversation on screen, hear replies spoken back.
 * This is a manual front-end onto the same [AnthropicClient] and
 * [ChatHistoryStore] that voice commands fall through to from
 * [com.owner.assistant.service.CommandRouter] when they don't match a fixed
 * phone-control phrase — so the two stay in the same conversation.
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
        }

        root.addView(buildHeader())

        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(messagesContainer)
        }
        root.addView(scrollView)

        root.addView(buildInputRow())
        setContentView(root)

        renderHistory()
        if (OwnerProfileStore.getAnthropicApiKey(this) == null) {
            showApiKeyDialog(initialSetup = true)
        }
    }

    private fun buildHeader(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 48, 32, 16)
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = "Chat"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(Button(this).apply {
            text = "API key"
            setOnClickListener { showApiKeyDialog(initialSetup = false) }
        })
        return row
    }

    private fun buildInputRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "Type a message..."
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        micButton = Button(this).apply {
            text = "🎤" // microphone emoji
            setOnClickListener { toggleVoiceInput() }
        }
        val sendButton = Button(this).apply {
            text = "Send"
            setOnClickListener { sendTypedMessage() }
        }
        row.addView(input)
        row.addView(micButton)
        row.addView(sendButton)
        return row
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
                val reply = AnthropicClient.sendMessageBlocking(this, ChatHistoryStore.all())
                ChatHistoryStore.add("assistant", reply)
                runOnUiThread {
                    thinkingBubble.text = reply
                    SpeechOutput.speak(reply)
                }
            } catch (e: AnthropicClient.ApiKeyMissingException) {
                runOnUiThread {
                    thinkingBubble.text = "No API key set — tap \"API key\" above to add one."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    thinkingBubble.text = "Couldn't reach Claude: ${e.message ?: "unknown error"}"
                }
            }
        }
    }

    private fun addBubble(role: String, text: String): TextView {
        val bubble = TextView(this).apply {
            this.text = text
            setPadding(24, 16, 24, 16)
            setBackgroundColor(if (role == "user") Color.parseColor("#DDE6FF") else Color.parseColor("#EEEEEE"))
            setTextColor(Color.BLACK)
        }
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (role == "user") Gravity.END else Gravity.START
            setPadding(0, 8, 0, 8)
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

    private fun showApiKeyDialog(initialSetup: Boolean) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }
        val keyInput = EditText(this).apply {
            hint = "Anthropic API key (sk-ant-...)"
            setText(OwnerProfileStore.getAnthropicApiKey(this@ChatActivity) ?: "")
        }
        val modelInput = EditText(this).apply {
            hint = "Model ID"
            setText(OwnerProfileStore.getAnthropicModel(this@ChatActivity))
        }
        layout.addView(TextView(this).apply {
            text = if (initialSetup) {
                "Add your Anthropic API key to enable real conversation. Get one at console.anthropic.com — usage is billed to your own account, not free."
            } else {
                "Update your Anthropic API key or model."
            }
        })
        layout.addView(keyInput)
        layout.addView(modelInput)

        AlertDialog.Builder(this)
            .setTitle("Claude API settings")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val key = keyInput.text.toString().trim()
                val model = modelInput.text.toString().trim()
                if (key.isNotBlank()) OwnerProfileStore.setAnthropicApiKey(this, key)
                if (model.isNotBlank()) OwnerProfileStore.setAnthropicModel(this, model)
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
    }
}
