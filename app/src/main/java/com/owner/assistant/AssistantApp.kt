package com.owner.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * App-wide init: notification channels are created once here instead of being
 * re-declared (and racing) inside every service/receiver that posts to them.
 */
class AssistantApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_SERVICE, getString(R.string.notification_channel_service),
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    CHANNEL_REMINDERS, getString(R.string.notification_channel_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_SECURITY, getString(R.string.notification_channel_security),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_EMERGENCY, getString(R.string.notification_channel_emergency),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        )
    }

    companion object {
        const val CHANNEL_SERVICE = "assistant_service"
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_SECURITY = "security_watchdog"
        const val CHANNEL_EMERGENCY = "emergency"
    }
}
