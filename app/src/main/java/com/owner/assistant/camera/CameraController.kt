package com.owner.assistant.camera

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.owner.assistant.AssistantApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Feature B: "take a picture" / "take a video" on voice command. Runs
 * headlessly as a lifecycle-aware foreground service (no visible camera
 * preview) and saves to the app's private media folder — nothing is shared
 * or uploaded.
 */
class CameraController : LifecycleService() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private lateinit var executor: ExecutorService

    override fun onCreate() {
        super.onCreate()
        executor = Executors.newSingleThreadExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, buildNotification())

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted")
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PHOTO -> capture()
            ACTION_VIDEO_START -> recordVideo()
            ACTION_VIDEO_STOP -> stopRecording()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun capture() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            val imageCapture = ImageCapture.Builder().build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                stopSelf()
                return@addListener
            }

            val file = mediaFile("jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(
                outputOptions, executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d(TAG, "Photo saved: ${file.absolutePath}")
                        provider.unbindAll()
                        stopSelf()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        provider.unbindAll()
                        stopSelf()
                    }
                }
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun recordVideo() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            val recorder = Recorder.Builder().build()
            val capture = VideoCapture.withOutput(recorder)
            videoCapture = capture
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, capture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                stopSelf()
                return@addListener
            }

            val file = mediaFile("mp4")
            val outputOptions = FileOutputOptions.Builder(file).build()
            activeRecording = capture.output.prepareRecording(this, outputOptions)
                .start(executor) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        Log.d(TAG, "Video saved: ${file.absolutePath}")
                        provider.unbindAll()
                        stopSelf()
                    }
                }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun mediaFile(extension: String): File {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_DCIM), "PersonalAssistant").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return File(dir, "assistant_$timestamp.$extension")
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, AssistantApp.CHANNEL_SERVICE)
            .setContentTitle("Personal Assistant")
            .setContentText("Capturing media")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        executor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CameraController"
        private const val NOTIFICATION_ID = 2
        private const val ACTION_PHOTO = "com.owner.assistant.action.PHOTO"
        private const val ACTION_VIDEO_START = "com.owner.assistant.action.VIDEO_START"
        private const val ACTION_VIDEO_STOP = "com.owner.assistant.action.VIDEO_STOP"

        fun capturePhoto(context: Context) {
            val intent = Intent(context, CameraController::class.java).setAction(ACTION_PHOTO)
            ContextCompat.startForegroundService(context, intent)
        }

        fun startVideo(context: Context) {
            val intent = Intent(context, CameraController::class.java).setAction(ACTION_VIDEO_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopVideo(context: Context) {
            val intent = Intent(context, CameraController::class.java).setAction(ACTION_VIDEO_STOP)
            context.startService(intent)
        }
    }
}
