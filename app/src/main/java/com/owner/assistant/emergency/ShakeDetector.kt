package com.owner.assistant.emergency

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Secondary emergency trigger from the blueprint ("hardware gesture
 * (shake/power-button pattern)"). The power-button pattern would require an
 * Accessibility Service intercepting key events, which is a much bigger
 * permission ask for a rarely-used secondary trigger — this implements the
 * shake gesture, which needs no extra permission on any Android version.
 */
class ShakeDetector(context: Context, private val onShakeDetected: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeTimestamps = mutableListOf<Long>()

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > SHAKE_THRESHOLD_G) {
            val now = System.currentTimeMillis()
            shakeTimestamps.add(now)
            shakeTimestamps = shakeTimestamps.filter { now - it < SHAKE_WINDOW_MS }.toMutableList()
            if (shakeTimestamps.size >= SHAKES_REQUIRED) {
                shakeTimestamps.clear()
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SHAKE_THRESHOLD_G = 2.7
        private const val SHAKE_WINDOW_MS = 1500L
        private const val SHAKES_REQUIRED = 3
    }
}
