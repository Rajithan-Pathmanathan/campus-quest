package com.campusquest.device.sensor.motion

/**
 * Interface for specialized physical gesture detectors.
 */
interface MotionDetector {
    /**
     * Evaluates linear acceleration and/or gravity vector.
     * @return true if the gesture is verified/detected.
     */
    fun process(
        linear: GravityFilter.LinearAcceleration,
        gravity: GravityFilter.GravityVector,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean

    val isVerified: Boolean
    fun reset()
}
