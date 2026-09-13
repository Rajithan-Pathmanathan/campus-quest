package com.campusquest.device.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.campusquest.device.sensor.light.LightSensorDetector
import com.campusquest.device.sensor.motion.GravityFilter
import com.campusquest.device.sensor.motion.MotionDetector
import com.campusquest.device.sensor.motion.ShakeDetector
import com.campusquest.device.sensor.motion.SweepDetector
import com.campusquest.device.sensor.motion.TiltDetector
import com.campusquest.device.sensor.proximity.ProximityGateDetector
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorSignalType
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Enterprise Multi-Sensor Fusion Orchestrator for Campus Quest.
 * Coordinates hardware sensor listeners, delegates signal processing to specialized sub-detectors,
 * and publishes pure immutable SensorState and FusionResult streams to M4 Scan UI.
 */
class SensorFusionEngine(
    context: Context,
    private val fusionCalculator: GeneralizedFusionCalculator = GeneralizedFusionCalculator()
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val availability = SensorAvailability(context)

    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    // Sub-detectors
    private val lightDetector = LightSensorDetector()
    private val gravityFilter = GravityFilter()
    private val shakeDetector = ShakeDetector()
    private val sweepDetector = SweepDetector()
    private val tiltDetector = TiltDetector()
    private val proximityDetector = ProximityGateDetector()

    private val availableSignals = availability.getAvailableSignalTypes()

    private var currentGameId: String = ""
    private var currentCheckpointId: String = ""
    private var targetSignature: LightSignature = LightSignature(0f, 1000f)
    private var activeMotionType: MotionType = MotionType.SWEEP
    private var isListening = false
    private var currentGpsScore: Float = 1.0f

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

    private fun getActiveMotionDetector(): MotionDetector {
        return when (activeMotionType) {
            MotionType.SHAKE -> shakeDetector
            MotionType.SWEEP -> sweepDetector
            MotionType.TILT -> tiltDetector
        }
    }

    /**
     * Starts physical sensor listeners when entering the Scan HUD.
     */
    fun startListening(
        gameId: String,
        checkpointId: String,
        targetSignature: LightSignature,
        motionType: MotionType = MotionType.SWEEP,
        gpsScore: Float = 1.0f
    ) {
        this.currentGameId = gameId
        this.currentCheckpointId = checkpointId
        this.targetSignature = targetSignature
        this.activeMotionType = motionType
        this.currentGpsScore = gpsScore

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
        gravityFilter.reset()
        shakeDetector.reset()
        sweepDetector.reset()
        tiltDetector.reset()
        proximityDetector.reset()
        recalculateFusion()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val rawX = event.values[0]
                val rawY = event.values[1]
                val rawZ = event.values[2]
                val (linearAcc, gravityVec) = gravityFilter.filter(rawX, rawY, rawZ)
                getActiveMotionDetector().process(linearAcc, gravityVec)
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
        val lightMatched = if (availability.hasLightSensor) lightDetector.isLightMatched else null
        val motionDetected = if (availability.hasAccelerometer) getActiveMotionDetector().isVerified else null
        val isNear = if (availability.hasProximitySensor) proximityDetector.isNear else null

        _sensorState.update {
            it.copy(
                currentLux = lightDetector.currentLux,
                lightMatched = lightMatched,
                linearAccelerationMagnitude = null, // Can expose linear.magnitude if needed
                motionDetected = motionDetected,
                isNear = isNear,
                activeMotionType = activeMotionType,
                availableSensors = availableSignals,
                timestamp = System.currentTimeMillis()
            )
        }

        val inputs = GeneralizedFusionCalculator.FusionInputs(
            gpsScore = currentGpsScore,
            lightMatched = lightMatched,
            motionDetected = motionDetected
        )

        val gateState = GeneralizedFusionCalculator.PhysicalGateState(
            proximityNear = isNear
        )

        val result = fusionCalculator.calculate(
            gameId = currentGameId,
            checkpointId = currentCheckpointId,
            inputs = inputs,
            gateState = gateState
        )

        _fusionResult.value = result
    }
}
