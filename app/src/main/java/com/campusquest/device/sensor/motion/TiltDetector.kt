package com.campusquest.device.sensor.motion

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Tilt detector monitoring changes in device orientation relative to gravity vector.
 * Supports both angular tilt calculation (degrees) and Delta-Gz heuristics.
 */
class TiltDetector(
    private val deltaZThreshold: Float = 5.0f,
    private val angleDegreesThreshold: Float = 30.0f
) : MotionDetector {

    private var initialGz: Float? = null
    private var initialGx: Float? = null
    private var initialGy: Float? = null
    private var isTilted: Boolean = false

    override fun process(
        linear: GravityFilter.LinearAcceleration,
        gravity: GravityFilter.GravityVector,
        timestamp: Long
    ): Boolean {
        if (initialGz == null) {
            initialGx = gravity.gx
            initialGy = gravity.gy
            initialGz = gravity.gz
            return false
        }

        val baseZ = initialGz ?: 9.8f
        val deltaZ = abs(gravity.gz - baseZ)

        // Angular tilt calculation
        val initX = initialGx ?: 0f
        val initY = initialGy ?: 0f
        val dotProduct = initX * gravity.gx + initY * gravity.gy + baseZ * gravity.gz
        val magInit = sqrt(initX * initX + initY * initY + baseZ * baseZ)
        val magCurrent = sqrt(gravity.gx * gravity.gx + gravity.gy * gravity.gy + gravity.gz * gravity.gz)

        val cosTheta = if (magInit > 0f && magCurrent > 0f) {
            (dotProduct / (magInit * magCurrent)).coerceIn(-1.0f, 1.0f)
        } else 1.0f

        val angleDegrees = Math.toDegrees(acos(cosTheta.toDouble())).toFloat()

        if (deltaZ > deltaZThreshold || angleDegrees > angleDegreesThreshold) {
            isTilted = true
        }

        return isTilted
    }

    override val isVerified: Boolean get() = isTilted

    override fun reset() {
        initialGx = null
        initialGy = null
        initialGz = null
        isTilted = false
    }
}
