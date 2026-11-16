package com.campusquest.device.sensor.light

import com.campusquest.domain.model.LightSignature

/**
 * Ambient light sensor processor implementing candidate stability state machine:
 * OUTSIDE -> reading enters range -> CANDIDATE -> 500ms stable -> MATCHED.
 * If reading exits range before 500ms: resets to OUTSIDE.
 */
class LightSensorDetector(
    private val candidateStabilityWindowMs: Long = 500L
) {
    enum class LightState {
        OUTSIDE,
        CANDIDATE,
        MATCHED
    }

    private var currentState: LightState = LightState.OUTSIDE
    private var candidateEntryTimestamp: Long = 0L
    private var lastObservedLux: Float = 0f

    fun evaluateLux(
        lux: Float,
        targetSignature: LightSignature,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        lastObservedLux = lux
        val inRange = targetSignature.matches(lux)

        when (currentState) {
            LightState.OUTSIDE -> {
                if (inRange) {
                    currentState = LightState.CANDIDATE
                    candidateEntryTimestamp = timestamp
                }
            }
            LightState.CANDIDATE -> {
                if (!inRange) {
                    currentState = LightState.OUTSIDE
                    candidateEntryTimestamp = 0L
                } else {
                    if (timestamp - candidateEntryTimestamp >= candidateStabilityWindowMs) {
                        currentState = LightState.MATCHED
                    }
                }
            }
            LightState.MATCHED -> {
                if (!inRange) {
                    currentState = LightState.OUTSIDE
                    candidateEntryTimestamp = 0L
                }
            }
        }

        return currentState == LightState.MATCHED
    }

    val currentLux: Float get() = lastObservedLux
    val isLightMatched: Boolean get() = currentState == LightState.MATCHED
    val state: LightState get() = currentState

    fun reset() {
        currentState = LightState.OUTSIDE
        candidateEntryTimestamp = 0L
        lastObservedLux = 0f
    }
}
