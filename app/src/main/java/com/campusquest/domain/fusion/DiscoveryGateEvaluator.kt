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

    companion object {
        /**
         * The single discovery rule, used by the scan screen ([com.campusquest.ui.quest.ScanStateMachine]).
         * [shieldHeld] is the fallback for phones whose proximity sensor never reports "near"
         * (e.g. virtual proximity sensors): the player presses and holds the shield instead.
         * It only counts once the fusion threshold is reached.
         */
        fun canDiscover(fusionThresholdReached: Boolean, proximityNear: Boolean, shieldHeld: Boolean = false): Boolean =
            fusionThresholdReached && (proximityNear || shieldHeld)
    }

    fun evaluate(input: DiscoveryGateInput): Boolean {
        return canDiscover(input.fusionResult.thresholdReached, input.proximityNear)
    }

    fun evaluate(fusionResult: FusionResult, proximityNear: Boolean): Boolean {
        return canDiscover(fusionResult.thresholdReached, proximityNear)
    }
}
