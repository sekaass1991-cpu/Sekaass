package com.owner.assistant.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import com.owner.assistant.data.AssistantPrefs
import com.owner.assistant.util.PermissionUtils
import java.util.Calendar

/**
 * Feature I: auto-enables Do Not Disturb based on a sleep schedule and/or
 * busy calendar events. Requires the owner to grant Notification Policy
 * access once (Settings > special access > Do Not Disturb access) — that
 * grant can't be requested via a normal runtime permission dialog.
 */
class DndScheduler(private val context: Context) {

    fun enableNow() = setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

    fun disableNow() = setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

    private fun setInterruptionFilter(filter: Int) {
        if (!PermissionUtils.isNotificationPolicyAccessGranted(context)) {
            Log.w(TAG, "Notification policy access not granted; can't change DND")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.setInterruptionFilter(filter)
    }

    /** Schedules the two daily alarms that flip DND on/off for the sleep window. */
    fun scheduleSleepWindow(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        AssistantPrefs.setSleepWindow(context, startHour, startMinute, endHour, endMinute)
        scheduleDaily(startHour, startMinute, DndAlarmReceiver.ACTION_ENABLE, REQUEST_CODE_START)
        scheduleDaily(endHour, endMinute, DndAlarmReceiver.ACTION_DISABLE, REQUEST_CODE_END)
    }

    fun cancelSleepWindow() {
        AssistantPrefs.setSleepDndEnabled(context, false)
        cancelAlarm(REQUEST_CODE_START)
        cancelAlarm(REQUEST_CODE_END)
    }

    private fun scheduleDaily(hour: Int, minute: Int, action: String, requestCode: Int) {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }
        val intent = Intent(context, DndAlarmReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, target.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent
        )
    }

    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, DndAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }

    /**
     * Optional calendar integration: if the owner has a calendar event marked
     * busy happening right now, enable DND. Meant to be polled periodically
     * (e.g. from a WorkManager job) rather than relying on a calendar
     * provider push, since there's no reliable "event starting" broadcast.
     */
    fun isBusyByCalendarNow(): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return false

        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath(now.toString())
            .build()
        val projection = arrayOf(CalendarContract.Instances.AVAILABILITY)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            while (cursor != null && cursor.moveToNext()) {
                val availability = cursor.getInt(0)
                if (availability == CalendarContract.Instances.AVAILABILITY_BUSY) return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Calendar query failed", e)
        } finally {
            cursor?.close()
        }
        return false
    }

    companion object {
        private const val TAG = "DndScheduler"
        private const val REQUEST_CODE_START = 9001
        private const val REQUEST_CODE_END = 9002
    }
}
