package com.campusquest.device.sensor.motion

import com.campusquest.domain.model.TrialStatus
import kotlin.math.acos
import kotlin.math.sqrt

/** A [MotionDetector] that can also report trial progress for the story UI. */
interface TrialDetector : MotionDetector {
    val status: TrialStatus
}

/**
 * Needs the same gesture [required] times ("shake 3 times", "sweep 3 times").
 * [cooldownMs] stops one long, strong movement from counting several times.
 */
class RepeatedGestureDetector(
    private val single: MotionDetector,
    private val required: Int = 3,
    private val cooldownMs: Long = 450L
) : TrialDetector {

    private var count = 0
    private var lastCountedAt = Long.MIN_VALUE / 2

    override fun process(
        linear: GravityFilter.LinearAcceleration,
        gravity: GravityFilter.GravityVector,
        timestamp: Long
    ): Boolean {
        if (isVerified) return true
        single.process(linear, gravity, timestamp)
        if (single.isVerified) {
            if (timestamp - lastCountedAt >= cooldownMs) {
                count++
                lastCountedAt = timestamp
            }
            single.reset()
        }
        return isVerified
    }

    override val isVerified: Boolean get() = count >= required

    override val status: TrialStatus
        get() = TrialStatus(completed = isVerified, remaining = (required - count).coerceAtLeast(0))

    override fun reset() {
        count = 0
        lastCountedAt = Long.MIN_VALUE / 2
        single.reset()
    }
}

/**
 * The water-jar trial: keep the phone within [maxAngleDeg] of the pose it started in for
 * [holdMs] without a break. Tipping further "spills" the water and restarts the hold.
 */
class SteadyHoldDetector(
    private val holdMs: Long = 10_000L,
    private val maxAngleDeg: Float = 15f
) : TrialDetector {

    private var reference: GravityFilter.GravityVector? = null
    private var steadySince: Long? = null
    private var heldMs = 0L
    private var spilled = false
    private var done = false

    override fun process(
        linear: GravityFilter.LinearAcceleration,
        gravity: GravityFilter.GravityVector,
        timestamp: Long
    ): Boolean {
        if (done) return true
        val ref = reference
        if (ref == null) {
            reference = gravity
            steadySince = timestamp
            return false
        }
        if (angleBetween(ref, gravity) > maxAngleDeg) {
            // Spill: restart from the new pose so the player can re-level and try again.
            spilled = true
            reference = gravity
            steadySince = timestamp
            heldMs = 0L
            return false
        }
        val since = steadySince ?: timestamp.also { steadySince = it }
        heldMs = timestamp - since
        if (heldMs >= holdMs / 4) spilled = false // a good stretch clears the "spilled" message
        if (heldMs >= holdMs) done = true
        return done
    }

    override val isVerified: Boolean get() = done

    override val status: TrialStatus
        get() = TrialStatus(
            completed = done,
            started = reference != null,
            justSpilled = spilled && !done,
            holdFraction = if (done) 1f else (heldMs.toFloat() / holdMs).coerceIn(0f, 1f)
        )

    override fun reset() {
        reference = null
        steadySince = null
        heldMs = 0L
        spilled = false
        done = false
    }

    private fun angleBetween(a: GravityFilter.GravityVector, b: GravityFilter.GravityVector): Float {
        val dot = a.gx * b.gx + a.gy * b.gy + a.gz * b.gz
        val mags = sqrt(a.gx * a.gx + a.gy * a.gy + a.gz * a.gz) * sqrt(b.gx * b.gx + b.gy * b.gy + b.gz * b.gz)
        if (mags <= 0f) return 0f
        return Math.toDegrees(acos((dot / mags).coerceIn(-1f, 1f).toDouble())).toFloat()
    }
}
