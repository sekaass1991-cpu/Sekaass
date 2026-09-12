package com.owner.assistant.call

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.Call
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.owner.assistant.R

/**
 * The real calling screen — this only exists because Android will not bind
 * [AssistantInCallService] (and therefore will not let "answer"/"decline"/
 * "mute the call" work at all) for any app that isn't the phone's default
 * Dialer. Being the default dialer means being responsible for this screen;
 * without it, the owner would have no way to see or touch-answer a call.
 *
 * Deliberately minimal: caller name/number, call state, and the same
 * actions the voice commands trigger — this is not a replacement for a full
 * phone app's features (no contacts picker, no call log, no dial pad here
 * beyond [DialerActivity]).
 */
class InCallActivity : AppCompatActivity() {

    private lateinit var callerText: TextView
    private lateinit var stateText: TextView
    private lateinit var actionsRow: LinearLayout
    private lateinit var muteButton: Button

    private var call: Call? = null
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setBackgroundColor(ContextCompat.getColor(this@InCallActivity, R.color.primary_dark))
        }

        callerText = TextView(this).apply {
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        stateText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#D8D4FF"))
            setPadding(0, 16, 0, 64)
        }
        actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        muteButton = Button(this).apply {
            text = "Mute"
            setTextColor(Color.WHITE)
            background = ContextCompat.getDrawable(this@InCallActivity, R.drawable.bg_button_outline)
            isAllCaps = false
            setOnClickListener {
                val nowMuted = !CallControlBus.isMuted()
                CallControlBus.setMuted(nowMuted)
                text = if (nowMuted) "Unmute" else "Mute"
            }
        }

        root.addView(callerText)
        root.addView(stateText)
        root.addView(actionsRow)
        setContentView(root)

        CallControlBus.setListener { updatedCall ->
            runOnUiThread {
                if (updatedCall == null) {
                    finish()
                } else {
                    bindTo(updatedCall)
                }
            }
        }
        CallControlBus.getCurrentCall()?.let { bindTo(it) } ?: finish()
    }

    private fun bindTo(newCall: Call) {
        call?.unregisterCallback(callCallback)
        call = newCall
        newCall.registerCallback(callCallback)
        render()
    }

    private fun render() {
        val activeCall = call ?: return
        val number = activeCall.details?.handle?.schemeSpecificPart
        callerText.text = number?.let { lookupContactName(it) } ?: number ?: "Unknown caller"

        actionsRow.removeAllViews()
        when (activeCall.state) {
            Call.STATE_RINGING -> {
                stateText.text = "Incoming call"
                actionsRow.addView(Button(this).apply {
                    text = "Decline"
                    setTextColor(Color.WHITE)
                    background = ContextCompat.getDrawable(this@InCallActivity, R.drawable.bg_button_danger)
                    isAllCaps = false
                    setOnClickListener { CallControlBus.decline() }
                })
                actionsRow.addView(spacer())
                actionsRow.addView(Button(this).apply {
                    text = "Answer"
                    setTextColor(Color.WHITE)
                    background = ContextCompat.getDrawable(this@InCallActivity, R.drawable.bg_button_success)
                    isAllCaps = false
                    setOnClickListener { CallControlBus.answer() }
                })
            }
            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                stateText.text = "Calling..."
                actionsRow.addView(endCallButton())
            }
            Call.STATE_ACTIVE -> {
                stateText.text = "In call"
                actionsRow.addView(muteButton)
                actionsRow.addView(spacer())
                actionsRow.addView(endCallButton())
            }
            Call.STATE_DISCONNECTED -> {
                stateText.text = "Call ended"
                finish()
            }
            else -> stateText.text = "Call state: ${activeCall.state}"
        }
    }

    private fun endCallButton() = Button(this).apply {
        text = "End call"
        setTextColor(Color.WHITE)
        background = ContextCompat.getDrawable(this@InCallActivity, R.drawable.bg_button_danger)
        isAllCaps = false
        setOnClickListener { CallControlBus.decline() }
    }

    private fun spacer() = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(24, 1)
    }

    private fun lookupContactName(phoneNumber: String): String? {
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber)
            )
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    override fun onDestroy() {
        call?.unregisterCallback(callCallback)
        CallControlBus.setListener(null)
        super.onDestroy()
    }
}
