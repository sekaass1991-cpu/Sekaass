package com.owner.assistant.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.owner.assistant.call.CallControlBus
import com.owner.assistant.call.PhoneLocker
import com.owner.assistant.camera.CameraController
import com.owner.assistant.data.MessageStore
import com.owner.assistant.emergency.EmergencyHandler
import com.owner.assistant.security.SecurityWatchdog
import com.owner.assistant.translate.Translator
import com.owner.assistant.util.PermissionUtils
import com.owner.assistant.util.SpeechOutput
import com.owner.assistant.vpn.AdBlockVpnService

/**
 * Central dispatcher. Every recognized command string (already passed through
 * VoiceLock) lands here and is routed to the right feature module.
 */
class CommandRouter(private val context: Context) {

    fun handle(command: String) {
        val text = command.lowercase().trim()
        Log.d("CommandRouter", "Handling: $text")

        when {
            text.contains("remind me") ->
                ReminderScheduler(context).scheduleFromSpeech(text)

            text.contains("take a picture") || text.contains("take a photo") ->
                CameraController.capturePhoto(context)

            text.contains("start a video") || text.contains("take a video") ->
                CameraController.startVideo(context)

            text.contains("stop the video") || text.contains("stop recording") ->
                CameraController.stopVideo(context)

            text.contains("lock my phone") || text.contains("lock the phone") ->
                PhoneLocker(context).lock()

            (text.contains("what messages") || text.contains("read my messages") ||
                text.contains("notifications")) ->
                SpeechOutput.speak(MessageStore.summarizeForSpeech())

            text.contains("answer") ->
                CallControlBus.answer()

            text.contains("decline") || text.contains("hang up") || text.contains("reject the call") ->
                CallControlBus.decline()

            text.contains("mute the call") || text == "mute" ->
                CallControlBus.setMuted(true)

            text.contains("unmute") ->
                CallControlBus.setMuted(false)

            text.contains("i'm in danger") || text.contains("emergency") ->
                EmergencyHandler(context).trigger()

            text.contains("check my apps") || text.contains("security check") ||
                text.contains("scan for unsafe apps") ->
                SecurityWatchdog(context).scanAndAnnounce()

            text.contains("turn on ad block") || text.contains("start ad block") ->
                startAdBlocking()

            text.contains("turn off ad block") || text.contains("stop ad block") ->
                context.stopService(Intent(context, AdBlockVpnService::class.java))

            text.contains("battery") || text.contains("data usage") ->
                BatteryDataMonitor(context).speakSummary()

            text.contains("do not disturb") && (text.contains("on") || text.contains("enable")) ->
                DndScheduler(context).enableNow()

            text.contains("do not disturb") && (text.contains("off") || text.contains("disable")) ->
                DndScheduler(context).disableNow()

            text.startsWith("translate to ") || text.startsWith("translate into ") ->
                startTranslation(text)

            text.contains("stop translating") || text.contains("stop translation") ->
                Translator(context).stopSession()

            else -> Log.d("CommandRouter", "No matching handler for: $text")
        }
    }

    private fun startAdBlocking() {
        if (!PermissionUtils.isVpnPrepared(context)) {
            SpeechOutput.speak("Ad blocking needs one-time approval. Please open the app to turn it on.")
            return
        }
        context.startService(Intent(context, AdBlockVpnService::class.java))
    }

    private fun startTranslation(text: String) {
        val languageName = text.substringAfter("translate to ", "")
            .ifBlank { text.substringAfter("translate into ", "") }
            .trim()
        val languageCode = Translator.languageCodeFor(languageName)
        if (languageCode == null) {
            SpeechOutput.speak("I don't have a translator set up for $languageName yet.")
            return
        }
        Translator(context).startSession(languageCode)
    }
}
