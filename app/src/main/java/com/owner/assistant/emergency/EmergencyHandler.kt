package com.owner.assistant.emergency

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.owner.assistant.data.OwnerProfileStore
import com.owner.assistant.util.SpeechOutput
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Feature G: trigger phrase ("I'm in danger") sends the owner's current
 * location plus an alert message to a pre-set emergency contact, and starts
 * a silent audio recording as evidence of the incident. This only ever acts
 * on the phone's own owner triggering their own safety feature — it never
 * runs without the wake-word + voice-lock gate having already passed.
 */
class EmergencyHandler(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun trigger() {
        val contact = OwnerProfileStore.getEmergencyContact(context)
        if (contact.isNullOrBlank()) {
            SpeechOutput.speak("No emergency contact is set. Please add one in the app.")
            return
        }

        SpeechOutput.speak("Emergency mode activated. Sending your location now.")
        sendAlert(contact)
        startSilentRecording()
    }

    private fun sendAlert(contact: String) {
        val location = lastKnownLocation()
        val locationText = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "location unavailable"
        }
        val message = String.format(
            Locale.getDefault(), OwnerProfileStore.getEmergencyMessageTemplate(context), locationText
        )

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "SEND_SMS not granted; cannot send emergency alert")
            SpeechOutput.speak("I couldn't send the alert. SMS permission isn't granted.")
            return
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(contact, null, parts, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emergency SMS", e)
        }
    }

    private fun lastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return providers
            .filter { locationManager.isProviderEnabled(it) }
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun startSilentRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "EmergencyRecordings").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val file = File(dir, "emergency_$timestamp.m4a")

        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mainHandler.postDelayed({ stopRecording() }, RECORDING_DURATION_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start emergency recording", e)
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Recorder stop failed (may not have started)", e)
        }
        recorder?.release()
        recorder = null
    }

    companion object {
        private const val TAG = "EmergencyHandler"
        private const val RECORDING_DURATION_MS = 3 * 60 * 1000L
    }
}
