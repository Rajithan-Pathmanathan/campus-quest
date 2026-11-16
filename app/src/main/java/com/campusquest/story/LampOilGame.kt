package com.campusquest.story

import kotlin.math.abs

enum class OilPourStatus { READY, POURING, TOO_FAST, TOO_FAR, RETURN_UPRIGHT, SEALING, PAUSED, FAILED, COMPLETE }

/**
 * Pouring oil from a jar by tilting the phone CLOCKWISE (tilt right, tilt > 0).
 * [update]'s tilt is the sideways angle in degrees from the upright start pose:
 *  - Clockwise tilt between 30° and 60° (30° <= tilt <= 60°): oil pours into the lamp;
 *  - Clockwise tilt < 30° or counter-clockwise tilt (tilt < 30°): jar rests, oil does not pour;
 *  - Tipped past 60° (|tilt| > 60°): tipped too far, oil spills.
 *
 * Rules & Capacity:
 *  - The player must fill the lamp to AT LEAST 80% (0.80f) to complete the stage.
 *  - Up to 20% (0.20f) of oil can be spilled.
 *  - If the jar runs out of oil (jarFill == 0) and the lamp fill is < 80%, status becomes [OilPourStatus.FAILED] (Game Over).
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

    /** Remaining capacity/volume in the pouring jar (1.0 = full, 0.0 = empty). */
    val jarFill: Float
        get() = (1f - fill - spilled).coerceIn(0f, 1f)

    fun update(tilt: Float, turnRate: Float?, seconds: Float, active: Boolean) {
        if (!seconds.isFinite() || seconds <= 0f) return
        if (status == OilPourStatus.FAILED || status == OilPourStatus.COMPLETE) return
        if (!active) {
            seal = 0f
            status = OilPourStatus.PAUSED
            return
        }

        // Fail immediately if spilled over 20% or jar emptied without reaching 80% fill
        if (spilled > MAX_SPILL_ALLOWED || (jarFill <= 0.001f && fill < MIN_REQUIRED_FILL)) {
            status = OilPourStatus.FAILED
            return
        }

        val angle = tilt.takeIf { it.isFinite() } ?: return
        val absAngle = abs(angle)
        val movingTooFast = turnRate?.let { it.isFinite() && abs(it) > MAX_TURN_RATE } == true

        if (fill < 1f) {
            when {
                movingTooFast -> spill(seconds, OilPourStatus.TOO_FAST)
                absAngle > MAX_POUR_TILT -> spill(seconds, OilPourStatus.TOO_FAR)
                angle >= MIN_POUR_TILT && angle <= MAX_POUR_TILT -> {
                    fill = (fill + seconds / POUR_SECONDS).coerceAtMost(1f)
                    status = if (fill >= 1f) OilPourStatus.RETURN_UPRIGHT else OilPourStatus.POURING
                }
                else -> {
                    status = if (fill >= MIN_REQUIRED_FILL) OilPourStatus.RETURN_UPRIGHT else OilPourStatus.READY
                }
            }
        } else {
            status = OilPourStatus.RETURN_UPRIGHT
        }

        if (fill >= MIN_REQUIRED_FILL && !movingTooFast && absAngle <= UPRIGHT_TILT) {
            seal = (seal + seconds / SEAL_SECONDS).coerceAtMost(1f)
            if (seal >= .999f) seal = 1f
            status = if (seal == 1f) OilPourStatus.COMPLETE else OilPourStatus.SEALING
        } else if (fill >= MIN_REQUIRED_FILL && status != OilPourStatus.TOO_FAST && status != OilPourStatus.TOO_FAR && status != OilPourStatus.POURING) {
            seal = 0f
            status = OilPourStatus.RETURN_UPRIGHT
        }
    }

    private fun spill(seconds: Float, reason: OilPourStatus) {
        val amount = seconds / SPILL_SECONDS
        fill = (fill - amount).coerceAtLeast(0f)
        spilled = (spilled + amount).coerceAtMost(1f)
        if (spilled > MAX_SPILL_ALLOWED || (jarFill <= 0.001f && fill < MIN_REQUIRED_FILL)) {
            status = OilPourStatus.FAILED
        } else {
            status = reason
        }
    }

    companion object {
        private const val POUR_SECONDS = 25f
        private const val SEAL_SECONDS = 2f
        private const val SPILL_SECONDS = 8f
        const val MIN_POUR_TILT = 30f   // degrees tipped clockwise from upright
        const val MAX_POUR_TILT = 60f
        const val UPRIGHT_TILT = 10f
        private const val MAX_TURN_RATE = 2.4f
        const val MIN_REQUIRED_FILL = 0.80f // At least 80% lamp fill required
        const val MAX_SPILL_ALLOWED = 0.20f // At most 20% spill allowed
    }
}
