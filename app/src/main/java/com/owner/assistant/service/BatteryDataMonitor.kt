package com.owner.assistant.service

import android.app.usage.UsageStatsManager
import android.content.Context
import com.owner.assistant.util.PermissionUtils
import com.owner.assistant.util.SpeechOutput
import java.util.concurrent.TimeUnit

/**
 * Feature H: background battery/data drain per app.
 *
 * Honest limitation: Android does not expose true per-app battery-percentage
 * consumption to third-party apps via public API (the system Battery screen
 * uses internal APIs this app can't call reliably across OS versions). This
 * uses [UsageStatsManager] foreground-time as the closest public-API proxy
 * for "how much this app has been running" — it's a real, working signal,
 * just not literally a battery-percentage number. Requires the owner to
 * grant "Usage access" in Settings (same special-access screen used by
 * [com.owner.assistant.util.PermissionUtils.isUsageAccessGranted]).
 */
class BatteryDataMonitor(private val context: Context) {

    private data class AppUsage(val label: String, val minutes: Long)

    fun speakSummary() {
        if (!PermissionUtils.isUsageAccessGranted(context)) {
            SpeechOutput.speak("I need usage access to check that. Please enable it in the app settings.")
            return
        }

        val topApps = topActiveApps(3)
        if (topApps.isEmpty()) {
            SpeechOutput.speak("You haven't used any apps in the last day.")
            return
        }

        val phrase = topApps.joinToString(". ") { "${it.label} was active for about ${it.minutes} minutes" }
        SpeechOutput.speak("Here's today's usage. $phrase")
    }

    private fun topActiveApps(count: Int): List<AppUsage> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(1)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: return emptyList()
        val pm = context.packageManager

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(count)
            .map { stat ->
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(stat.packageName, 0)).toString()
                } catch (e: Exception) {
                    stat.packageName
                }
                AppUsage(label, TimeUnit.MILLISECONDS.toMinutes(stat.totalTimeInForeground))
            }
    }
}
