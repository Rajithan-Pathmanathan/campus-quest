package com.campusquest.story

import kotlin.math.abs

enum class OilPourStatus { READY, POURING, TOO_FAST, TOO_FAR, RETURN_UPRIGHT, SEALING, PAUSED, COMPLETE }

/**
 * Pouring oil from a jar by tilting the phone LEFT or RIGHT (forward/back tilt is ignored).
 * [update]'s tilt is the sideways angle in degrees from the upright start pose. Measured as the
 * jar's angle with the horizontal x axis (upright = 90°):
 *  - more than 60° from horizontal (tipped < 30°): the jar rests, nothing pours;
 *  - 30°–60° from horizontal (tipped 30°–60°): oil pours into the lamp;
 *  - less than 30° from horizontal (tipped > 60°): tipped too far, oil spills.
 */
class LampOilGame {
    var fill = 0f
        private set
    var spilled = 0f
        private set
    var seal = 0f
        private set
    var status = OilPourStatus.READY
        private set

    fun update(tilt: Float, turnRate: Float?, seconds: Float, active: Boolean) {
        if (!seconds.isFinite() || seconds <= 0f) return
        if (!active) {
            seal = 0f
            status = OilPourStatus.PAUSED
            return
        }
        val angle = abs(tilt).takeIf { it.isFinite() } ?: return
        val movingTooFast = turnRate?.let { it.isFinite() && abs(it) > MAX_TURN_RATE } == true

        if (fill < 1f) {
            when {
                movingTooFast -> spill(seconds, OilPourStatus.TOO_FAST)
                angle > MAX_POUR_TILT -> spill(seconds, OilPourStatus.TOO_FAR)
                angle >= MIN_POUR_TILT -> {
                    fill = (fill + seconds / POUR_SECONDS).coerceAtMost(1f)
                    if (fill >= .999f) fill = 1f
                    status = if (fill == 1f) OilPourStatus.RETURN_UPRIGHT else OilPourStatus.POURING
                }
                else -> status = OilPourStatus.READY
            }
            return
        }

        if (!movingTooFast && angle <= UPRIGHT_TILT) {
            seal = (seal + seconds / SEAL_SECONDS).coerceAtMost(1f)
            if (seal >= .999f) seal = 1f
            status = if (seal == 1f) OilPourStatus.COMPLETE else OilPourStatus.SEALING
        } else {
            seal = 0f
            status = OilPourStatus.RETURN_UPRIGHT
        }
    }

    private fun spill(seconds: Float, reason: OilPourStatus) {
        val amount = seconds / SPILL_SECONDS
        fill = (fill - amount).coerceAtLeast(0f)
        spilled = (spilled + amount).coerceAtMost(1f)
        status = reason
    }

    companion object {
        private const val POUR_SECONDS = 25f
        private const val SEAL_SECONDS = 2f
        private const val SPILL_SECONDS = 8f
        const val MIN_POUR_TILT = 30f   // degrees tipped from upright
        const val MAX_POUR_TILT = 60f
        const val UPRIGHT_TILT = 10f
        private const val MAX_TURN_RATE = 2.4f
    }
}
