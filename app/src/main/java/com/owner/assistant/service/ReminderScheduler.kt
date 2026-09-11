package com.owner.assistant.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import java.util.regex.Pattern

/**
 * Parses simple "remind me at <time> for <thing>" phrases and schedules an
 * exact alarm delivered via [ReminderReceiver].
 *
 * Example inputs handled:
 *   "remind me at 5 for the meeting"
 *   "remind me at 5:30 to call mom"
 */
class ReminderScheduler(private val context: Context) {

    private val timePattern = Pattern.compile("at (\\d{1,2})(?::(\\d{2}))?")

    fun scheduleFromSpeech(text: String) {
        val matcher = timePattern.matcher(text)
        if (!matcher.find()) {
            Log.d("ReminderScheduler", "Couldn't find a time in: $text")
            return
        }

        val hour = matcher.group(1)?.toIntOrNull() ?: return
        val minute = matcher.group(2)?.toIntOrNull() ?: 0

        // naive extraction of the reminder label after "for" or "to"
        val label = text.substringAfter(" for ", "")
            .ifBlank { text.substringAfter(" to ", "Reminder") }

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1) // roll to tomorrow if time passed
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("label", label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, target.timeInMillis.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
        )

        Log.d("ReminderScheduler", "Scheduled '$label' at ${target.time}")
    }
}
