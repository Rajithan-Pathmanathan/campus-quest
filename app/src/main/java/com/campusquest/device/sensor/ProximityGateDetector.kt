package com.campusquest.device.sensor

/**
 * Interprets Android device-specific proximity sensor events into a binary isNear gate.
 */
class ProximityGateDetector {

    private var isNearState: Boolean = false
    private var lastReportedDistance: Float = 5.0f

    /**
     * Evaluates proximity event against the sensor's maximum range.
     * Standard Android HAL convention:
     * - Binary sensors report 0.0 for NEAR and maximumRange for FAR.
     * - Continuous sensors report distance; distance < maximumRange indicates NEAR.
     */
    fun processProximity(distance: Float, maximumRange: Float): Boolean {
        lastReportedDistance = distance
        isNearState = distance < maximumRange || distance < 1.0f
        return isNearState
    }

    val isNear: Boolean get() = isNearState
    val reportedDistanceCm: Float get() = lastReportedDistance

    fun reset() {
        isNearState = false
        lastReportedDistance = 5.0f
    }
}
