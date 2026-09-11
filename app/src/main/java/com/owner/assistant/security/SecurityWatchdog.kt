package com.owner.assistant.security

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.owner.assistant.AssistantApp
import com.owner.assistant.util.SpeechOutput

/**
 * Runs the permission audit across every installed app and reports what it
 * finds. See [PermissionHeuristics] for what "unsafe" means here.
 */
class SecurityWatchdog(private val context: Context) {

    fun scan(): List<FlaggedApp> {
        val pm = context.packageManager
        val packages = try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list installed packages", e)
            return emptyList()
        }

        return packages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val profile = AppPermissionProfile(
                packageName = pkg.packageName,
                label = pm.getApplicationLabel(appInfo).toString(),
                isSystemApp = isSystemApp,
                requestedPermissions = pkg.requestedPermissions?.toSet() ?: emptySet()
            )
            PermissionHeuristics.evaluate(profile)
        }
    }

    /** Runs the scan and both speaks and posts a notification with a summary. */
    fun scanAndAnnounce() {
        val flagged = scan()
        if (flagged.isEmpty()) {
            SpeechOutput.speak("I checked your apps. Nothing looks unusual.")
            return
        }

        val topReasonsBySpeech = flagged.take(3).joinToString(". ") { app ->
            "${app.label}: ${app.reasons.first()}"
        }
        SpeechOutput.speak(
            "I found ${flagged.size} app${if (flagged.size == 1) "" else "s"} worth reviewing. " +
                "For example: $topReasonsBySpeech"
        )

        postNotification(flagged)
    }

    /** For the periodic background scan: notifies without speaking unprompted. */
    fun scanAndNotify() {
        val flagged = scan()
        if (flagged.isNotEmpty()) postNotification(flagged)
    }

    @SuppressLint("MissingPermission") // POST_NOTIFICATIONS is requested during onboarding
    private fun postNotification(flagged: List<FlaggedApp>) {
        val detail = flagged.joinToString("\n") { "${it.label}: ${it.reasons.joinToString("; ")}" }
        val notification = NotificationCompat.Builder(context, AssistantApp.CHANNEL_SECURITY)
            .setContentTitle("Security check: ${flagged.size} app(s) to review")
            .setContentText(flagged.joinToString(", ") { it.label })
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "SecurityWatchdog"
        private const val NOTIFICATION_ID = 3
    }
}
