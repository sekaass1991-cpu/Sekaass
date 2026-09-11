package com.owner.assistant.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.owner.assistant.AssistantApp
import com.owner.assistant.util.SpeechOutput

class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // POST_NOTIFICATIONS is requested during onboarding
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "Reminder"

        val notification = NotificationCompat.Builder(context, AssistantApp.CHANNEL_REMINDERS)
            .setContentTitle("Reminder")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(label.hashCode(), notification)

        SpeechOutput.init(context)
        SpeechOutput.speak("Reminder: $label")
    }
}
