package com.campusquest.device.sensor.motion

import kotlin.math.sqrt

/**
 * Isolated gravity low-pass filter for Android accelerometer readings.
 * Separates the constant Earth gravity vector from dynamic user motion.
 * Formula:
 *   g_t = alpha * g_{t-1} + (1 - alpha) * raw
 *   d_t = raw - g_t (linear acceleration)
 */
class GravityFilter(
    private val alpha: Float = 0.8f
) {
    data class LinearAcceleration(
        val dx: Float,
        val dy: Float,
        val dz: Float,
        val magnitude: Float
    )

    data class GravityVector(
        val gx: Float,
        val gy: Float,
        val gz: Float
    )

    private var gx: Float = 0.0f
    private var gy: Float = 0.0f
    private var gz: Float = 9.8f
    private var isInitialized = false

    fun filter(rawX: Float, rawY: Float, rawZ: Float): Pair<LinearAcceleration, GravityVector> {
        if (!isInitialized) {
            gx = rawX
            gy = rawY
            gz = rawZ
            isInitialized = true
        } else {
            gx = alpha * gx + (1.0f - alpha) * rawX
            gy = alpha * gy + (1.0f - alpha) * rawY
            gz = alpha * gz + (1.0f - alpha) * rawZ
        }

        val dx = rawX - gx
        val dy = rawY - gy
        val dz = rawZ - gz
        val magnitude = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

        return Pair(
            LinearAcceleration(dx, dy, dz, magnitude),
            GravityVector(gx, gy, gz)
        )
    }

    fun reset() {
        gx = 0.0f
        gy = 0.0f
        gz = 9.8f
        isInitialized = false
    }
}
