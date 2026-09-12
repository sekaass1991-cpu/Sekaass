package com.owner.assistant.data

import android.content.Context

/**
 * Plain (non-encrypted) app settings that aren't personally identifying —
 * sleep schedule, ad-block toggle, preferred translation languages. Contrast
 * with [SecureStore], which holds the owner's voice profile and emergency
 * contact.
 */
object AssistantPrefs {

    private const val FILE_NAME = "assistant_prefs"
    private const val KEY_SLEEP_START_HOUR = "sleep_start_hour"
    private const val KEY_SLEEP_START_MINUTE = "sleep_start_minute"
    private const val KEY_SLEEP_END_HOUR = "sleep_end_hour"
    private const val KEY_SLEEP_END_MINUTE = "sleep_end_minute"
    private const val KEY_SLEEP_DND_ENABLED = "sleep_dnd_enabled"
    private const val KEY_CALENDAR_DND_ENABLED = "calendar_dnd_enabled"
    private const val KEY_PREFERRED_VOICE_NAME = "preferred_tts_voice_name"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun setSleepWindow(context: Context, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        prefs(context).edit()
            .putInt(KEY_SLEEP_START_HOUR, startHour)
            .putInt(KEY_SLEEP_START_MINUTE, startMinute)
            .putInt(KEY_SLEEP_END_HOUR, endHour)
            .putInt(KEY_SLEEP_END_MINUTE, endMinute)
            .putBoolean(KEY_SLEEP_DND_ENABLED, true)
            .apply()
    }

    data class SleepWindow(val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int)

    fun getSleepWindow(context: Context): SleepWindow? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_SLEEP_DND_ENABLED, false)) return null
        return SleepWindow(
            p.getInt(KEY_SLEEP_START_HOUR, 23),
            p.getInt(KEY_SLEEP_START_MINUTE, 0),
            p.getInt(KEY_SLEEP_END_HOUR, 7),
            p.getInt(KEY_SLEEP_END_MINUTE, 0)
        )
    }

    fun setSleepDndEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SLEEP_DND_ENABLED, enabled).apply()
    }

    fun setCalendarDndEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALENDAR_DND_ENABLED, enabled).apply()
    }

    fun isCalendarDndEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALENDAR_DND_ENABLED, false)

    /** The exact TTS engine voice name the owner picked in the chat screen's voice picker, if any. */
    fun setPreferredVoiceName(context: Context, voiceName: String) {
        prefs(context).edit().putString(KEY_PREFERRED_VOICE_NAME, voiceName).apply()
    }

    fun getPreferredVoiceName(context: Context): String? =
        prefs(context).getString(KEY_PREFERRED_VOICE_NAME, null)
}
