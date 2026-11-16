package com.campusquest.story

import kotlin.math.abs
import kotlin.math.sin

/** A stationary player's tilt balances the jug during a sixty-second virtual walk. */
class GoldenWater {
    var elapsed = 0f; private set
    var remaining = 1f; private set
    var leanX = 0f; private set
    var leanY = 0f; private set
    val complete get() = elapsed >= 60f && remaining > 0f
    val empty get() = remaining <= 0f

    fun step(x: Float, y: Float, seconds: Float) {
        if (complete || empty || !x.isFinite() || !y.isFinite() || !seconds.isFinite() || seconds <= 0f) return
        // Cap interrupted frames so returning from the background cannot advance the challenge.
        val dt = seconds.coerceAtMost(.1f)
        elapsed = (elapsed + dt).coerceAtMost(60f)
        val sway = .35f + .35f * elapsed / 60f
        val targetX = x.coerceIn(-1f, 1f) + sin(elapsed * 1.7f) * sway
        val targetY = y.coerceIn(-1f, 1f) + sin(elapsed * 1.1f) * sway
        leanX += (targetX - leanX) * (dt * 5f).coerceAtMost(1f)
        leanY += (targetY - leanY) * (dt * 5f).coerceAtMost(1f)
        val overflow = (maxOf(abs(leanX), abs(leanY)) - .48f).coerceAtLeast(0f)
        remaining = (remaining - overflow * dt * .35f).coerceAtLeast(0f)
    }
}
