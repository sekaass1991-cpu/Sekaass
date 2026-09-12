package com.owner.assistant.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.owner.assistant.AssistantApp
import com.owner.assistant.R
import com.owner.assistant.emergency.EmergencyHandler
import com.owner.assistant.emergency.ShakeDetector
import com.owner.assistant.util.SpeechOutput
import com.owner.assistant.voice.VoiceLock
import com.owner.assistant.voice.WakeWordDetector

/**
 * The heart of the assistant: a persistent foreground service that runs the
 * wake-word/command capture loop and, once a command clears [VoiceLock],
 * hands the recognized text to [CommandRouter].
 */
class AssistantForegroundService : Service(), WakeWordDetector.Listener {

    private lateinit var commandRouter: CommandRouter
    private lateinit var voiceLock: VoiceLock
    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var shakeDetector: ShakeDetector
    private val workerExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        commandRouter = CommandRouter(applicationContext)
        voiceLock = VoiceLock(applicationContext)
        wakeWordDetector = WakeWordDetector(applicationContext, this)
        shakeDetector = ShakeDetector(applicationContext) {
            EmergencyHandler(applicationContext).trigger()
        }
        SpeechOutput.init(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Listening for your wake word"))
        wakeWordDetector.start()
        shakeDetector.start()
        return START_STICKY
    }

    override fun onDestroy() {
        wakeWordDetector.stop()
        shakeDetector.stop()
        workerExecutor.shutdown()
        super.onDestroy()
    }

    // ---- WakeWordDetector.Listener ----

    override fun onCommandCaptured(text: String, rawAudio: ShortArray) {
        // Voice-lock comparison and command handling both do real work
        // (FFT-based feature extraction, PackageManager scans, network calls
        // for translation, etc.) — none of that belongs on the main thread
        // that SpeechRecognizer callbacks arrive on.
        workerExecutor.execute {
            if (voiceLock.isOwnerVoice(rawAudio)) {
                commandRouter.handle(text)
            }
            // Silently ignored otherwise: a stranger saying the wake word must
            // never trigger anything, and must never get an audible signal that
            // their voice was rejected (that would itself leak that this app exists).
        }
    }

    override fun onListeningStateChanged(isListening: Boolean) {
        updateNotification(if (isListening) "Listening..." else "Assistant active")
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, AssistantApp.CHANNEL_SERVICE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}
