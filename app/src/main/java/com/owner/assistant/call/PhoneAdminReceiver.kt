package com.owner.assistant.call

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Required receiver for Device Admin. The owner enables this once during
 * onboarding via Settings' "activate device admin app" prompt — it can't be
 * granted programmatically.
 */
class PhoneAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Personal Assistant can now lock your screen on command", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Phone-lock voice command disabled", Toast.LENGTH_SHORT).show()
    }
}
