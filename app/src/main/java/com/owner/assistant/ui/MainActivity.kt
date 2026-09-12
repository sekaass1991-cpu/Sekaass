package com.owner.assistant.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.owner.assistant.R
import com.owner.assistant.chat.ChatActivity
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.onboarding.OnboardingActivity
import com.owner.assistant.service.AssistantForegroundService
import com.owner.assistant.util.PermissionUtils

/**
 * Entry point. Setup itself lives in [OnboardingActivity] — this screen just
 * shows whether it's done and lets the owner (re-)start the assistant.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun render() {
        val enrolled = OwnerProfileStore.isVoiceEnrolled(this)
        val backgroundActive = enrolled && PermissionUtils.isIgnoringBatteryOptimizations(this)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.background))
        }

        page.addView(buildHeader())

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        content.addView(statusPill(enrolled, backgroundActive))
        content.addView(spacer(32))

        content.addView(TextView(this).apply {
            text = if (enrolled) {
                "Setup complete. The assistant only responds to your voice."
            } else {
                "Setup isn't finished yet — the assistant won't start until your voice profile is enrolled."
            }
            setTextColor(color(R.color.text_secondary))
            textSize = 15f
        })
        content.addView(spacer(40))

        content.addView(primaryButton("Open Chat") {
            startActivity(Intent(this, ChatActivity::class.java))
        })
        content.addView(spacer(16))

        content.addView(outlineButton(if (enrolled) "Re-run setup" else "Start setup") {
            startActivity(Intent(this, OnboardingActivity::class.java))
        })

        if (enrolled) {
            content.addView(spacer(16))
            content.addView(successButton("Start assistant") {
                ContextCompat.startForegroundService(this, Intent(this, AssistantForegroundService::class.java))
                render()
            })
            content.addView(spacer(16))
            content.addView(dangerButton("Stop assistant") {
                stopService(Intent(this, AssistantForegroundService::class.java))
                render()
            })
        }

        page.addView(content)

        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun buildHeader(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(color(R.color.primary))
        setPadding(48, 96, 48, 64)

        addView(TextView(this@MainActivity).apply {
            text = "Personal Assistant"
            textSize = 26f
            setTextColor(Color.WHITE)
        })
        addView(TextView(this@MainActivity).apply {
            text = "Your private, voice-controlled companion"
            textSize = 14f
            setTextColor(Color.parseColor("#D8D4FF"))
            setPadding(0, 8, 0, 0)
        })
    }

    private fun statusPill(enrolled: Boolean, backgroundActive: Boolean) = TextView(this).apply {
        text = when {
            !enrolled -> "●  Setup incomplete"
            backgroundActive -> "●  Listening in the background"
            else -> "●  Ready — background listening not guaranteed"
        }
        setTextColor(color(if (enrolled) R.color.status_on_text else R.color.status_off_text))
        background = ContextCompat.getDrawable(this@MainActivity, if (enrolled) R.drawable.bg_status_on else R.drawable.bg_status_off)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.CENTER_VERTICAL
        textSize = 13f
    }

    private fun spacer(heightPx: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
    }

    private fun primaryButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_button_primary)
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun outlineButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(color(R.color.primary))
        background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_button_outline)
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun successButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_button_success)
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun dangerButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_button_danger)
        isAllCaps = false
        setOnClickListener { action() }
    }
}
