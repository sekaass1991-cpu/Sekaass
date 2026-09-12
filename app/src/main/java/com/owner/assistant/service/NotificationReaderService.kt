package com.owner.assistant.service

import android.app.Notification
import android.content.pm.PackageManager
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.owner.assistant.data.CapturedMessage
import com.owner.assistant.data.MessageStore

/**
 * Captures incoming notifications from messaging apps so the assistant can
 * read them aloud on command ("what messages did I get?"). Requires the
 * owner to manually enable this app under
 * Settings > Apps > Special access > Notification access.
 *
 * Privacy: message content is kept only in the in-memory [MessageStore] ring
 * buffer for this session — never written to disk, logged, or sent
 * off-device (see blueprint section 5, local-first).
 */
class NotificationReaderService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (!isMessagingApp(sbn.packageName)) return
        // Group/summary notifications carry no useful text of their own.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val appLabel = labelFor(sbn.packageName)
        MessageStore.add(
            CapturedMessage(
                appLabel = appLabel,
                sender = title ?: appLabel,
                text = text ?: "",
                timestampMillis = sbn.postTime
            )
        )
        Log.d("NotificationReader", "Captured message from $appLabel")
    }

    private fun isMessagingApp(packageName: String): Boolean {
        if (packageName in KNOWN_MESSAGING_PACKAGES) return true
        return packageName == Telephony.Sms.getDefaultSmsPackage(this)
    }

    private fun labelFor(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    companion object {
        private val KNOWN_MESSAGING_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms", // Signal
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.facebook.orca", // Messenger
            "com.instagram.android"
        )
    }
}
