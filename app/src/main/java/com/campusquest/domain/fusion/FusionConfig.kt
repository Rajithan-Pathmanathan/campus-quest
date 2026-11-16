package com.campusquest.domain.fusion

/**
 * System configuration parameters for generalized multi-sensor fusion.
 * Defaults:
 *   - GPS Weight: 0.40 (40%)
 *   - Light Weight: 0.30 (30%)
 *   - Motion Weight: 0.30 (30%)
 *   - Cutoff Threshold: 0.85 (85% required for resonance threshold)
 */
data class FusionConfig(
    val gpsWeight: Float = 0.40f,
    val lightWeight: Float = 0.30f,
    val motionWeight: Float = 0.30f,
    val threshold: Float = 0.85f
) {
    init {
        require(gpsWeight > 0f) { "GPS weight must be positive" }
        require(lightWeight >= 0f) { "Light weight cannot be negative" }
        require(motionWeight >= 0f) { "Motion weight cannot be negative" }
        require(threshold in 0.0f..1.0f) { "Threshold must be between 0.0 and 1.0" }
    }
}
