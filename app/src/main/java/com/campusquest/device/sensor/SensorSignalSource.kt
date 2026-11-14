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
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorSignalType
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Common interface for physical or simulated sensor signal sources.
 * Decouples sensor state emission from Android SensorManager hardware.
 */
interface SensorSignalSource {
    val sensorState: StateFlow<SensorState>

    fun startListening(
        targetSignature: LightSignature,
        motionType: MotionType
    )

    fun stopListening()
    fun reset()
}

/**
 * Real hardware implementation backed by Android SensorManager.
 */
class RealSensorSignalSource(
    context: Context
) : SensorSignalSource, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val availability = SensorAvailability(context)

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val light: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximity: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val lightDetector = LightSensorDetector()
    private val gravityFilter = GravityFilter()
    private val shakeDetector = ShakeDetector()
    private val sweepDetector = SweepDetector()
    private val tiltDetector = TiltDetector()
    private val proximityDetector = ProximityGateDetector()

    private val availableSignals = availability.getAvailableSignalTypes()

    private var targetSignature: LightSignature = LightSignature(0f, 1000f)
    private var activeMotionType: MotionType = MotionType.SWEEP
    private var isListening = false

    private val _sensorState = MutableStateFlow(
        SensorState(availableSensors = availableSignals)
    )
    override val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    private fun getActiveMotionDetector(): MotionDetector {
        return when (activeMotionType) {
            MotionType.SHAKE -> shakeDetector
            MotionType.SWEEP -> sweepDetector
            MotionType.TILT -> tiltDetector
        }
    }

    override fun startListening(
        targetSignature: LightSignature,
        motionType: MotionType
    ) {
        this.targetSignature = targetSignature
        this.activeMotionType = motionType
        reset()

        if (isListening) return

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        light?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        isListening = true
        publishState()
    }

    override fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun reset() {
        lightDetector.reset()
        gravityFilter.reset()
        shakeDetector.reset()
        sweepDetector.reset()
        tiltDetector.reset()
        proximityDetector.reset()
        publishState()
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

        publishState()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handled if necessary
    }

    private fun publishState() {
        val lightMatched = if (availability.hasLightSensor) lightDetector.isLightMatched else null
        val motionDetected = if (availability.hasAccelerometer) getActiveMotionDetector().isVerified else null
        val isNear = if (availability.hasProximitySensor) proximityDetector.isNear else null

        _sensorState.update {
            it.copy(
                currentLux = lightDetector.currentLux,
                lightMatched = lightMatched,
                linearAccelerationMagnitude = null,
                motionDetected = motionDetected,
                isNear = isNear,
                activeMotionType = activeMotionType,
                availableSensors = availableSignals,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
