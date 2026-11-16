package com.campusquest.device.sensor

import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorSignalType
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Deterministic test and UI demonstration fixture for Member 4.
 * Enables full end-to-end Scan HUD testing without requiring physical movement or hardware triggers.
 */
class SensorSimulationFixture : SensorSignalSource {

    private val _sensorState = MutableStateFlow(
        SensorState(
            availableSensors = setOf(
                SensorSignalType.GPS_LOCATION,
                SensorSignalType.AMBIENT_LIGHT,
                SensorSignalType.ACCELEROMETER_MOTION,
                SensorSignalType.PROXIMITY_GATE
            )
        )
    )
    override val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    override fun startListening(
        targetSignature: LightSignature,
        motionType: MotionType
    ) {
        _sensorState.update {
            it.copy(
                currentLux = 0f,
                lightMatched = false,
                motionDetected = false,
                isNear = false,
                activeMotionType = motionType
            )
        }
    }

    override fun stopListening() {
        // No-op for simulation
    }

    override fun reset() {
        _sensorState.update {
            it.copy(
                currentLux = 0f,
                lightMatched = false,
                motionDetected = false,
                isNear = false
            )
        }
    }

    // --- Deterministic Simulation Helpers for M4 ---

    fun simulateLightMatch(lux: Float = 450f, matched: Boolean = true) {
        _sensorState.update {
            it.copy(
                currentLux = lux,
                lightMatched = matched,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun simulateMotionDetected(detected: Boolean = true, magnitude: Float = 16.0f) {
        _sensorState.update {
            it.copy(
                motionDetected = detected,
                linearAccelerationMagnitude = magnitude,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun simulateProximity(isNear: Boolean) {
        _sensorState.update {
            it.copy(
                isNear = isNear,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun simulateMissingHardware(
        hasLight: Boolean = true,
        hasAccelerometer: Boolean = true,
        hasProximity: Boolean = true
    ) {
        val available = mutableSetOf(SensorSignalType.GPS_LOCATION)
        if (hasLight) available.add(SensorSignalType.AMBIENT_LIGHT)
        if (hasAccelerometer) available.add(SensorSignalType.ACCELEROMETER_MOTION)
        if (hasProximity) available.add(SensorSignalType.PROXIMITY_GATE)

        _sensorState.update {
            it.copy(
                currentLux = if (hasLight) it.currentLux else null,
                lightMatched = if (hasLight) it.lightMatched else null,
                motionDetected = if (hasAccelerometer) it.motionDetected else null,
                isNear = if (hasProximity) it.isNear else null,
                availableSensors = available,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
