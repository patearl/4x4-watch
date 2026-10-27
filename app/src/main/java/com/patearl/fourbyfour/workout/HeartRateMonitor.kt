package com.patearl.fourbyfour.workout

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Streams the watch's optical heart rate sensor. Reports null when the reading is unreliable. */
class HeartRateMonitor(
    private val context: Context,
    private val onHeartRate: (Int?) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private var registered = false

    fun start() {
        if (registered) return
        if (context.checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) return
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) ?: return
        registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        registered = false
        onHeartRate(null)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val bpm = event.values.firstOrNull()?.toInt() ?: 0
        val reliable = event.accuracy != SensorManager.SENSOR_STATUS_NO_CONTACT &&
            event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
        onHeartRate(if (reliable && bpm > 0) bpm else null)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}
