package com.campusquest.domain.fusion

import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.SensorSignalType

/**
 * Pure mathematical calculator for multi-sensor weighted scoring.
 * Implements generalized missing-sensor weight re-normalization without hardcoded branch ladders.
 *
 * This class is completely pure:
 * - No Android framework dependencies
 * - No SensorManager or Context references
 * - No mutable state
 * - Completely deterministic and unit testable in pure JVM
 */
class GeneralizedFusionCalculator(
    private val config: FusionConfig = FusionConfig()
) {

    /**
     * Computes the normalized weighted fusion score from active physical signals.
     */
    fun calculate(
        gameId: String,
        checkpointId: String,
        inputs: FusionInputs,
        proximityNear: Boolean = false
    ): FusionResult {
        val activeWeights = mutableMapOf<SensorSignalType, Float>()
        val activeScores = mutableMapOf<SensorSignalType, Float>()

        // 1. GPS Location channel
        if (inputs.gpsScore != null) {
            activeWeights[SensorSignalType.GPS_LOCATION] = config.gpsWeight
            activeScores[SensorSignalType.GPS_LOCATION] = inputs.gpsScore.coerceIn(0.0f, 1.0f)
        }

        // 2. Ambient Light channel (null = missing sensor, 0.0 = unmatched, 1.0 = matched)
        if (inputs.lightScore != null) {
            activeWeights[SensorSignalType.AMBIENT_LIGHT] = config.lightWeight
            activeScores[SensorSignalType.AMBIENT_LIGHT] = inputs.lightScore.coerceIn(0.0f, 1.0f)
        }

        // 3. Accelerometer Motion channel (null = missing sensor, 0.0 = unmatched, 1.0 = matched)
        if (inputs.motionScore != null) {
            activeWeights[SensorSignalType.ACCELEROMETER_MOTION] = config.motionWeight
            activeScores[SensorSignalType.ACCELEROMETER_MOTION] = inputs.motionScore.coerceIn(0.0f, 1.0f)
        }

        val totalActiveWeight = activeWeights.values.sum()

        // Safe fallback for zero active signals (avoids division by zero)
        if (totalActiveWeight <= 0f) {
            return FusionResult(
                gameId = gameId,
                checkpointId = checkpointId,
                gpsScore = 0.0f,
                lightScore = null,
                motionScore = null,
                totalScore = 0.0f,
                progressPercent = 0,
                thresholdReached = false,
                proximityNear = proximityNear,
                activeSignals = emptySet()
            )
        }

        // Generalized Weight Normalization: W'_i = W_i / sum(W_active)
        var weightedSum = 0.0f
        for ((signal, weight) in activeWeights) {
            val normalizedWeight = weight / totalActiveWeight
            val score = activeScores[signal] ?: 0.0f
            weightedSum += (normalizedWeight * score)
        }

        val clampedScore = weightedSum.coerceIn(0.0f, 1.0f)
        val progressPercent = (clampedScore * 100).toInt().coerceIn(0, 100)
        val thresholdReached = clampedScore >= config.threshold

        val reportedSignals = mutableSetOf<SensorSignalType>().apply {
            addAll(activeWeights.keys)
            add(SensorSignalType.PROXIMITY_GATE)
        }

        return FusionResult(
            gameId = gameId,
            checkpointId = checkpointId,
            gpsScore = inputs.gpsScore ?: 0.0f,
            lightScore = inputs.lightScore,
            motionScore = inputs.motionScore,
            totalScore = clampedScore,
            progressPercent = progressPercent,
            thresholdReached = thresholdReached,
            proximityNear = proximityNear,
            activeSignals = reportedSignals
        )
    }
}
