package com.campusquest.device.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.SensorReading
import com.campusquest.domain.model.SensorSignalType
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Enterprise Multi-Sensor Fusion Engine for Campus Quest.
 * Manages physical sensor listeners, executes generalized weight normalization,
 * and exposes pure immutable FusionResult and SensorState streams to M4 Scan UI.
 */
class SensorFusionEngine(
    context: Context,
    private val fusionCalculator: GeneralizedFusionCalculator = GeneralizedFusionCalculator()
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val lightDetector = LightSensorDetector()
    private val motionDetector = StandardMotionDetector()
    private val proximityDetector = ProximityGateDetector()

    private val availableSignals = mutableSetOf<SensorSignalType>().apply {
        add(SensorSignalType.GPS_LOCATION)
        if (lightSensor != null) add(SensorSignalType.AMBIENT_LIGHT)
        if (accelerometerSensor != null) add(SensorSignalType.ACCELEROMETER_MOTION)
        if (proximitySensor != null) add(SensorSignalType.PROXIMITY_GATE)
    }

    private var currentGameId: String = ""
    private var currentCheckpointId: String = ""
    private var targetSignature: LightSignature = LightSignature(0f, 1000f)
    private var isListening = false
    private var currentGpsScore: Float = 1.0f // 1.0 when inside geofence

    private val _sensorState = MutableStateFlow(
        SensorState(availableSensors = availableSignals)
    )
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    private val _fusionResult = MutableStateFlow(
        FusionResult(
            gameId = "",
            checkpointId = "",
            gpsScore = 1.0f,
            lightScore = null,
            motionScore = null,
            totalScore = 0.0f,
            progressPercent = 0,
            thresholdReached = false,
            proximityNear = false,
            activeSignals = availableSignals
        )
    )
    val fusionResult: StateFlow<FusionResult> = _fusionResult.asStateFlow()

    /**
     * Starts physical sensor listeners when entering the Scan HUD.
     */
    fun startListening(
        gameId: String,
        checkpointId: String,
        targetSignature: LightSignature,
        motionType: String = "SWEEP",
        gpsScore: Float = 1.0f
    ) {
        this.currentGameId = gameId
        this.currentCheckpointId = checkpointId
        this.targetSignature = targetSignature
        this.currentGpsScore = gpsScore
        this.motionDetector.setMotionType(motionType)

        resetVerification()

        if (isListening) return

        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        isListening = true
        recalculateFusion()
    }

    /**
     * Lifecycle-safe unregistration when exiting the Scan HUD.
     */
    fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    fun updateGpsScore(score: Float) {
        this.currentGpsScore = score.coerceIn(0.0f, 1.0f)
        recalculateFusion()
    }

    fun resetVerification() {
        lightDetector.reset()
        motionDetector.reset()
        proximityDetector.reset()
        recalculateFusion()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                motionDetector.processAccelerometer(x, y, z)
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                lightDetector.evaluateLux(lux, targetSignature)
            }
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                proximityDetector.processProximity(distance, event.sensor.maximumRange)
            }
        }

        recalculateFusion()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handled if necessary
    }

    private fun recalculateFusion() {
        val lightMatched = if (lightSensor != null) lightDetector.isLightMatched else null
        val motionDetected = if (accelerometerSensor != null) motionDetector.isMotionVerified else null
        val isNear = proximityDetector.isNear

        _sensorState.update {
            it.copy(
                currentLux = lightDetector.currentLux,
                lightMatched = lightMatched,
                currentAcceleration = motionDetector.currentAccelerationMagnitude,
                motionDetected = motionDetected,
                isNear = isNear,
                availableSensors = availableSignals,
                timestamp = System.currentTimeMillis()
            )
        }

        val inputs = GeneralizedFusionCalculator.SignalInputs(
            gpsScore = currentGpsScore,
            lightMatched = lightMatched,
            motionDetected = motionDetected,
            proximityNear = isNear
        )

        val result = fusionCalculator.calculate(
            gameId = currentGameId,
            checkpointId = currentCheckpointId,
            inputs = inputs
        )

        _fusionResult.value = result
    }
}
