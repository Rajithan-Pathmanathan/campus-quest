package com.campusquest.domain.fusion

/**
 * Pure mathematical input representation for multi-sensor weighted scoring.
 *
 * Explicit Signal Semantics:
 *   - null: Hardware sensor is unavailable/missing on this device -> excluded from weight sum (triggers re-normalization).
 *   - 0.0f: Sensor is available, but currently outside target range / gesture not detected.
 *   - 1.0f: Sensor is available and target condition is fully matched/verified.
 *   - gpsScore: Continuous accuracy/distance score (0.0f .. 1.0f) or null if location service unavailable.
 *
 * Notice: Proximity is intentionally absent here because it is not a weighted signal.
 */
data class FusionInputs(
    val gpsScore: Float?,
    val lightScore: Float?,
    val motionScore: Float?
) {
    companion object {
        fun fromBooleans(
            gpsScore: Float?,
            lightMatched: Boolean?,
            motionDetected: Boolean?
        ): FusionInputs {
            return FusionInputs(
                gpsScore = gpsScore,
                lightScore = lightMatched?.let { if (it) 1.0f else 0.0f },
                motionScore = motionDetected?.let { if (it) 1.0f else 0.0f }
            )
        }
    }
}
