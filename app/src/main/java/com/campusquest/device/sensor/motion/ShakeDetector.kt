package com.campusquest.device.sensor.motion

/**
 * Shake detector operating on isolated linear acceleration.
 * Triggers when dynamic acceleration magnitude exceeds the threshold.
 */
class ShakeDetector(
    private val threshold: Float = 14.5f
) : MotionDetector {

    private var detected: Boolean = false

    override fun process(
        linear: GravityFilter.LinearAcceleration,
        gravity: GravityFilter.GravityVector,
        timestamp: Long
    ): Boolean {
        if (linear.magnitude > threshold) {
            detected = true
        }
        return detected
    }

    override val isVerified: Boolean get() = detected

    override fun reset() {
        detected = false
    }
}
