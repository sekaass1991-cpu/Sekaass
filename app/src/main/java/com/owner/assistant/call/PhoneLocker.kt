package com.owner.assistant.call

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.owner.assistant.util.SpeechOutput

/**
 * Feature C: "lock my phone" on command, via the Device Admin API.
 */
class PhoneLocker(private val context: Context) {

    fun lock() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, PhoneAdminReceiver::class.java)
        if (!dpm.isAdminActive(admin)) {
            Log.w(TAG, "Device admin not active; can't lock screen")
            SpeechOutput.speak("I don't have permission to lock the phone yet. Please enable it in the app.")
            return
        }
        dpm.lockNow()
    }

    companion object {
        private const val TAG = "PhoneLocker"
    }
}
