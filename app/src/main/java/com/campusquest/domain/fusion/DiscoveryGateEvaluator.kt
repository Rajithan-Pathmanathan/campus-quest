package com.campusquest.domain.fusion

import com.campusquest.domain.model.FusionResult

/**
 * Encapsulates the inputs required for the final physical discovery gate.
 */
data class DiscoveryGateInput(
    val fusionResult: FusionResult,
    val proximityNear: Boolean
)

/**
 * Evaluates whether the player can claim physical discovery of the relic at the checkpoint.
 *
 * Rule:
 *   Discovery is permitted ONLY when:
 *   1. Fusion threshold is reached (totalScore >= 85%)
 *   AND
 *   2. Physical proximity gate confirms close-range presence (isNear == true)
 */
class DiscoveryGateEvaluator {

    fun evaluate(input: DiscoveryGateInput): Boolean {
        return input.fusionResult.thresholdReached && input.proximityNear
    }

    fun evaluate(fusionResult: FusionResult, proximityNear: Boolean): Boolean {
        return fusionResult.thresholdReached && proximityNear
    }
}
