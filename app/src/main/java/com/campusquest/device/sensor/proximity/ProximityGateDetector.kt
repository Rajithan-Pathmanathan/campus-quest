package com.campusquest.device.sensor.proximity

/**
 * Physical confirmation gate detector interpreting device-specific proximity sensor events.
 * Standard Android HAL convention:
 * - Binary sensors report 0.0 for NEAR and maximumRange for FAR.
 * - Continuous sensors report distance; distance < maximumRange indicates NEAR.
 * Note: Proximity is NOT a weighted fusion signal; it is the final physical confirmation gate.
 */
class ProximityGateDetector {

    data class ProximityReading(
        val isNear: Boolean,
        val rawDistanceCm: Float,
        val maximumRangeCm: Float
    )

    private var currentReading = ProximityReading(
        isNear = false,
        rawDistanceCm = 5.0f,
        maximumRangeCm = 5.0f
    )

    /**
     * Evaluates whether device proximity sensor indicates NEAR.
     * Evaluated strictly as rawDistance < maximumRange without hardcoded physical distance assumptions.
     */
    fun processProximity(rawDistance: Float, maximumRange: Float): Boolean {
        val near = rawDistance < maximumRange
        currentReading = ProximityReading(
            isNear = near,
            rawDistanceCm = rawDistance,
            maximumRangeCm = maximumRange
        )
        return near
    }

    val isNear: Boolean get() = currentReading.isNear
    val reading: ProximityReading get() = currentReading

    fun reset() {
        currentReading = ProximityReading(
            isNear = false,
            rawDistanceCm = 5.0f,
            maximumRangeCm = 5.0f
        )
    }
}
