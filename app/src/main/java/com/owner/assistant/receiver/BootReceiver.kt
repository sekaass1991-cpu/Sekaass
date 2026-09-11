package com.owner.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.owner.assistant.data.AssistantPrefs
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.service.AssistantForegroundService
import com.owner.assistant.service.BatteryDataMonitorWorker
import com.owner.assistant.service.DndScheduler
import com.owner.assistant.security.SecurityWatchdogWorker

/**
 * Restarts the always-on assistant (and re-arms the sleep-DND alarms +
 * periodic workers, which don't survive a reboot on their own) once setup
 * has actually been completed. Nothing starts automatically before the
 * owner has enrolled their voice — see [OwnerProfileStore.isVoiceEnrolled].
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!OwnerProfileStore.isVoiceEnrolled(context)) return

        ContextCompat.startForegroundService(context, Intent(context, AssistantForegroundService::class.java))

        AssistantPrefs.getSleepWindow(context)?.let { window ->
            DndScheduler(context).scheduleSleepWindow(
                window.startHour, window.startMinute, window.endHour, window.endMinute
            )
        }

        SecurityWatchdogWorker.schedule(context)
        BatteryDataMonitorWorker.schedule(context)
    }
}
