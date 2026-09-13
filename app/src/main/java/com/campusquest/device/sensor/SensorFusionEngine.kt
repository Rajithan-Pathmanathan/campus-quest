package com.campusquest.device.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.campusquest.domain.model.AccelerometerReading
import com.campusquest.domain.model.FusionState
import com.campusquest.domain.model.LightReading
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.ProximityReading
import com.campusquest.domain.repository.SensorRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class SensorFusionEngine(
    context: Context
) : SensorRepository, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val _sensorReadings = MutableSharedFlow<com.campusquest.domain.model.SensorReading>(replay = 1)
    override val sensorReadings: SharedFlow<com.campusquest.domain.model.SensorReading> = _sensorReadings.asSharedFlow()

    private val _fusionState = MutableStateFlow(FusionState())
    override val fusionState: StateFlow<FusionState> = _fusionState.asStateFlow()

    private var targetSignature: LightSignature = LightSignature(0f, 1000f)
    private var targetMotionType: String = "SWEEP"

    // Moving window sensor state
    private var lastAccelMagnitude = 9.8f
    private var lastLux = 0f
    private var lastDistanceCm = 5f
    private var motionDetected = false
    private var lightSatisfied = false
    private var proximitySatisfied = false

    // Mathematical thresholds
    companion object {
        const val ACCEL_SHAKE_THRESHOLD = 14.5f
        const val PROXIMITY_NEAR_THRESHOLD_CM = 1.0f
    }

    override fun startListening(targetSignature: LightSignature, motionType: String) {
        this.targetSignature = targetSignature
        this.targetMotionType = motionType
        resetVerification()

        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun resetVerification() {
        motionDetected = false
        lightSatisfied = false
        proximitySatisfied = false
        updateFusionState()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                lastAccelMagnitude = mag

                if (mag > ACCEL_SHAKE_THRESHOLD) {
                    motionDetected = true
                }
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                lastLux = lux
                lightSatisfied = targetSignature.matches(lux)
            }
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                lastDistanceCm = distance
                proximitySatisfied = distance <= PROXIMITY_NEAR_THRESHOLD_CM
            }
        }

        updateFusionState()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun updateFusionState() {
        var score = 0
        if (motionDetected) score += 34
        if (lightSatisfied) score += 33
        if (proximitySatisfied) score += 33

        val isComplete = score >= 100

        _fusionState.value = FusionState(
            motionVerified = motionDetected,
            lightVerified = lightSatisfied,
            proximityVerified = proximitySatisfied,
            fusionProgressPercent = score.coerceAtMost(100),
            isFullyVerified = isComplete,
            currentLux = lastLux,
            currentAcceleration = lastAccelMagnitude,
            currentDistanceCm = lastDistanceCm,
            targetSignature = targetSignature
        )
    }
}
