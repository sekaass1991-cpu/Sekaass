package com.owner.assistant.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.owner.assistant.AssistantApp
import com.owner.assistant.util.PermissionUtils
import java.util.concurrent.TimeUnit

/**
 * Feature H's periodic side: once a day, warns about whichever app has been
 * running the most. See [BatteryDataMonitor] for the API limitation this
 * works around.
 */
class BatteryDataMonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        if (!PermissionUtils.isUsageAccessGranted(applicationContext)) return Result.success()

        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(1)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: return Result.success()
        val top = stats.maxByOrNull { it.totalTimeInForeground } ?: return Result.success()
        if (top.totalTimeInForeground <= 0) return Result.success()

        val pm = applicationContext.packageManager
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(top.packageName, 0)).toString()
        } catch (e: Exception) {
            top.packageName
        }
        val minutes = TimeUnit.MILLISECONDS.toMinutes(top.totalTimeInForeground)

        val notification = NotificationCompat.Builder(applicationContext, AssistantApp.CHANNEL_SERVICE)
            .setContentTitle("Today's heaviest app")
            .setContentText("$label was active for about $minutes minutes today")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "battery_data_monitor_daily"
        private const val NOTIFICATION_ID = 5

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BatteryDataMonitorWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
