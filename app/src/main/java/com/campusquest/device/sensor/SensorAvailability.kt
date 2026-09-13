package com.campusquest.device.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.campusquest.domain.model.SensorSignalType

/**
 * Diagnostic helper to detect hardware sensor capabilities on the host device.
 */
class SensorAvailability(
    context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val hasLightSensor: Boolean = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
    val hasAccelerometer: Boolean = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    val hasProximitySensor: Boolean = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null

    fun getAvailableSignalTypes(): Set<SensorSignalType> {
        val signals = mutableSetOf<SensorSignalType>()
        signals.add(SensorSignalType.GPS_LOCATION) // Core baseline
        if (hasLightSensor) signals.add(SensorSignalType.AMBIENT_LIGHT)
        if (hasAccelerometer) signals.add(SensorSignalType.ACCELEROMETER_MOTION)
        if (hasProximitySensor) signals.add(SensorSignalType.PROXIMITY_GATE)
        return signals
    }
}
