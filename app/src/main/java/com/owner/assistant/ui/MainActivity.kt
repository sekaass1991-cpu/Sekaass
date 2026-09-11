package com.owner.assistant.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.onboarding.OnboardingActivity
import com.owner.assistant.service.AssistantForegroundService

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

    private fun render() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val enrolled = OwnerProfileStore.isVoiceEnrolled(this)

        layout.addView(TextView(this).apply {
            text = "Personal Assistant"
            textSize = 22f
            setPadding(0, 0, 0, 24)
        })

        layout.addView(TextView(this).apply {
            text = if (enrolled) {
                "Setup complete. The assistant only responds to your voice."
            } else {
                "Setup isn't finished yet — the assistant won't start until your voice profile is enrolled."
            }
            setPadding(0, 0, 0, 32)
        })

        layout.addView(Button(this).apply {
            text = if (enrolled) "Re-run setup" else "Start setup"
            setOnClickListener { startActivity(Intent(this@MainActivity, OnboardingActivity::class.java)) }
        })

        if (enrolled) {
            layout.addView(Button(this).apply {
                text = "Start assistant"
                setOnClickListener {
                    ContextCompat.startForegroundService(
                        this@MainActivity, Intent(this@MainActivity, AssistantForegroundService::class.java)
                    )
                }
            })
            layout.addView(Button(this).apply {
                text = "Stop assistant"
                setOnClickListener {
                    stopService(Intent(this@MainActivity, AssistantForegroundService::class.java))
                }
            })
        }

        setContentView(layout)
    }
}
