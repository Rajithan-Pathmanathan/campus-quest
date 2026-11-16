package com.campusquest.domain.model

/**
 * Clean result produced by the M3 Fusion Engine and consumed by the M4 Scan UI.
 *
 * Notice: Proximity is NOT part of the weighted totalScore.
 * Proximity is a separate close-range physical confirmation gate.
 */
data class FusionResult(
    val gameId: String,
    val checkpointId: String,
    val gpsScore: Float,              // Normalized GPS contribution (0.0f .. 1.0f)
    val lightScore: Float?,           // Normalized Light contribution (0.0f .. 1.0f, null if sensor unavailable)
    val motionScore: Float?,          // Normalized Motion contribution (0.0f .. 1.0f, null if sensor unavailable)
    val totalScore: Float,            // Weighted normalized sum (0.0f .. 1.0f)
    val progressPercent: Int,         // Progress percentage (0 .. 100)
    val thresholdReached: Boolean,    // True when totalScore >= 0.85f (85%)
    val proximityNear: Boolean,       // Final close-range confirmation gate
    val activeSignals: Set<SensorSignalType>
) {
    /**
     * True only when both the multi-sensor fusion threshold is reached (>= 85%)
     * AND the physical proximity gate is verified (near).
     */
    val canClaimDiscovery: Boolean
        get() = thresholdReached && proximityNear
}
