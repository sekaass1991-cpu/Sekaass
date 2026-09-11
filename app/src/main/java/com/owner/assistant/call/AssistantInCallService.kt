package com.owner.assistant.call

import android.content.Intent
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.owner.assistant.util.SpeechOutput

/**
 * Feature A (call control): Android only binds an InCallService for real
 * phone calls to the phone's default Dialer app (or a car-mode companion) —
 * there is no lighter-weight role for this. So this app registers as the
 * default dialer ([android.app.role.RoleManager.ROLE_DIALER], requested
 * during onboarding), and [InCallActivity] is the calling screen that comes
 * with that responsibility.
 *
 * On a ringing call it announces the caller by voice AND launches
 * [InCallActivity] for touch control; "answer" / "decline" / "mute the
 * call" commands are routed here through [CallControlBus] either way.
 */
class AssistantInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state == Call.STATE_RINGING) announceCaller(call)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallControlBus.setCurrentCall(call)
        CallControlBus.bindService(this)
        call.registerCallback(callback)
        if (call.state == Call.STATE_RINGING) announceCaller(call)
        launchInCallScreen()
    }

    private fun launchInCallScreen() {
        startActivity(
            Intent(this, InCallActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        CallControlBus.clearIfCurrent(call)
    }

    override fun onDestroy() {
        CallControlBus.unbindService(this)
        super.onDestroy()
    }

    private fun announceCaller(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart
        val name = number?.let { lookupContactName(it) } ?: number ?: "an unknown number"
        SpeechOutput.speak("Incoming call from $name. Say answer, decline, or mute the call.")
    }

    private fun lookupContactName(phoneNumber: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "No contacts permission; announcing raw number")
            null
        }
    }

    companion object {
        private const val TAG = "AssistantInCallService"
    }
}
