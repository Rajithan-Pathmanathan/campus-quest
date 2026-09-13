package com.campusquest.device.sensor

import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.SensorSignalType

/**
 * Pure mathematical calculator for multi-sensor weighted scoring.
 * Implements generalized missing-sensor weight normalization without static branch ladders.
 * Proximity is strictly separated as a physical confirmation gate outside the weighted calculation.
 */
class GeneralizedFusionCalculator(
    private val baseGpsWeight: Float = 0.40f,
    private val baseLightWeight: Float = 0.30f,
    private val baseMotionWeight: Float = 0.30f,
    private val thresholdCutoff: Float = 0.85f // 85% to reach threshold
) {
    data class FusionInputs(
        val gpsScore: Float,              // 0.0f .. 1.0f (derived from distance / geofence)
        val lightMatched: Boolean?,       // null if sensor unavailable
        val motionDetected: Boolean?      // null if sensor unavailable
    )

    data class PhysicalGateState(
        val proximityNear: Boolean?       // Final close-range confirmation gate (null if unavailable)
    )

    /**
     * Computes the normalized weighted fusion score and produces an immutable FusionResult.
     */
    fun calculate(
        gameId: String,
        checkpointId: String,
        inputs: FusionInputs,
        gateState: PhysicalGateState = PhysicalGateState(proximityNear = false)
    ): FusionResult {
        val activeWeights = mutableMapOf<SensorSignalType, Float>()
        val activeScores = mutableMapOf<SensorSignalType, Float>()

        // GPS is always available as a signal channel
        activeWeights[SensorSignalType.GPS_LOCATION] = baseGpsWeight
        activeScores[SensorSignalType.GPS_LOCATION] = inputs.gpsScore.coerceIn(0.0f, 1.0f)

        // Light sensor
        if (inputs.lightMatched != null) {
            activeWeights[SensorSignalType.AMBIENT_LIGHT] = baseLightWeight
            activeScores[SensorSignalType.AMBIENT_LIGHT] = if (inputs.lightMatched) 1.0f else 0.0f
        }

        // Accelerometer / Motion sensor
        if (inputs.motionDetected != null) {
            activeWeights[SensorSignalType.ACCELEROMETER_MOTION] = baseMotionWeight
            activeScores[SensorSignalType.ACCELEROMETER_MOTION] = if (inputs.motionDetected) 1.0f else 0.0f
        }

        // Generalized Weight Normalization: W'_i = W_i / sum(W_active)
        val totalActiveWeight = activeWeights.values.sum()
        var weightedTotalScore = 0.0f

        for ((signal, weight) in activeWeights) {
            val normalizedWeight = if (totalActiveWeight > 0f) weight / totalActiveWeight else 0f
            val score = activeScores[signal] ?: 0f
            weightedTotalScore += (normalizedWeight * score)
        }

        val clampedTotalScore = weightedTotalScore.coerceIn(0.0f, 1.0f)
        val progressPercent = (clampedTotalScore * 100).toInt().coerceIn(0, 100)
        val thresholdReached = clampedTotalScore >= thresholdCutoff

        val allActiveSignals = mutableSetOf<SensorSignalType>().apply {
            addAll(activeWeights.keys)
            if (gateState.proximityNear != null) {
                add(SensorSignalType.PROXIMITY_GATE)
            }
        }

        return FusionResult(
            gameId = gameId,
            checkpointId = checkpointId,
            gpsScore = inputs.gpsScore,
            lightScore = if (inputs.lightMatched != null) (if (inputs.lightMatched) 1.0f else 0.0f) else null,
            motionScore = if (inputs.motionDetected != null) (if (inputs.motionDetected) 1.0f else 0.0f) else null,
            totalScore = clampedTotalScore,
            progressPercent = progressPercent,
            thresholdReached = thresholdReached,
            proximityNear = gateState.proximityNear ?: false,
            activeSignals = allActiveSignals
        )
    }
}
