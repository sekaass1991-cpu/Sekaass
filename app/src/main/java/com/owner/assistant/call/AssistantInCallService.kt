package com.owner.assistant.call

import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.owner.assistant.util.SpeechOutput

/**
 * Feature A (call control): registering this as an InCallService (the owner
 * enables it once via Settings > Apps > Default apps > "Assistant call
 * screening/companion" role prompt, or via RoleManager) lets the assistant
 * see and control calls without becoming the full default dialer.
 *
 * On a ringing call it announces the caller by voice; "answer" / "decline" /
 * "mute the call" commands are routed here through [CallControlBus].
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
