package com.campusquest.device.sensor

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pluggable motion detector supporting SWEEP, TILT, and SHAKE gestures.
 */
interface MotionDetector {
    fun processAccelerometer(x: Float, y: Float, z: Float, timestamp: Long = System.currentTimeMillis()): Boolean
    val currentAccelerationMagnitude: Float
    val isMotionVerified: Boolean
    fun reset()
}

class StandardMotionDetector(
    private var motionType: String = "SWEEP"
) : MotionDetector {

    companion object {
        const val SHAKE_THRESHOLD = 14.5f
        const val SWEEP_THRESHOLD = 12.0f
        const val TILT_GRAVITY_DELTA_THRESHOLD = 5.0f
    }

    private var lastMagnitude = 9.8f
    private var isVerified = false
    private var initialZ: Float? = null

    fun setMotionType(type: String) {
        motionType = type.uppercase()
        reset()
    }

    override fun processAccelerometer(x: Float, y: Float, z: Float, timestamp: Long): Boolean {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        lastMagnitude = magnitude

        if (initialZ == null) {
            initialZ = z
        }

        when (motionType) {
            "SHAKE" -> {
                if (magnitude > SHAKE_THRESHOLD) {
                    isVerified = true
                }
            }
            "TILT" -> {
                val zDelta = abs(z - (initialZ ?: 9.8f))
                if (zDelta > TILT_GRAVITY_DELTA_THRESHOLD) {
                    isVerified = true
                }
            }
            "SWEEP" -> {
                // Sweep motion: significant horizontal oscillation along x or y axis
                if (abs(x) > SWEEP_THRESHOLD || abs(y) > SWEEP_THRESHOLD || magnitude > 13.0f) {
                    isVerified = true
                }
            }
            else -> {
                // Default fallback: any active movement exceeding resting gravity
                if (magnitude > 12.5f) {
                    isVerified = true
                }
            }
        }

        return isVerified
    }

    override val currentAccelerationMagnitude: Float get() = lastMagnitude
    override val isMotionVerified: Boolean get() = isVerified

    override fun reset() {
        lastMagnitude = 9.8f
        isVerified = false
        initialZ = null
    }
}
