package com.owner.assistant.security

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Feature D's "periodic scan of installed apps' permissions." Runs once a
 * day in the background and posts a notification if anything is flagged —
 * see [SecurityWatchdog] for the actual heuristics.
 */
class SecurityWatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        SecurityWatchdog(applicationContext).scanAndNotify()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "security_watchdog_periodic"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SecurityWatchdogWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
