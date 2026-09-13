package com.campusquest.device.sensor

import com.campusquest.domain.model.LightSignature

/**
 * Handles ambient illumination matching and debouncing against target LightSignature.
 */
class LightSensorDetector(
    private val debounceWindowMs: Long = 500L
) {
    private var lastLux: Float = 0f
    private var isMatched: Boolean = false
    private var matchStartTime: Long = 0L

    fun evaluateLux(lux: Float, targetSignature: LightSignature, timestamp: Long = System.currentTimeMillis()): Boolean {
        lastLux = lux
        val currentlyInRange = targetSignature.matches(lux)

        if (currentlyInRange) {
            if (matchStartTime == 0L) {
                matchStartTime = timestamp
            }
            if (timestamp - matchStartTime >= debounceWindowMs || !isMatched) {
                isMatched = true
            }
        } else {
            matchStartTime = 0L
            isMatched = false
        }

        return isMatched
    }

    val currentLux: Float get() = lastLux
    val isLightMatched: Boolean get() = isMatched

    fun reset() {
        lastLux = 0f
        isMatched = false
        matchStartTime = 0L
    }
}
