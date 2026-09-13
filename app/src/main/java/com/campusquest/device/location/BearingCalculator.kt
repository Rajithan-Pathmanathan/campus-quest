package com.campusquest.device.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure mathematical utility for calculating forward azimuth / bearing degrees between two GPS coordinates.
 * Returns normalized heading in degrees [0.0f .. 360.0f).
 */
object BearingCalculator {

    /**
     * Calculates the initial bearing from point (fromLat, fromLng) to (toLat, toLng).
     * @return Bearing in degrees (0.0f <= bearing < 360.0f).
     */
    fun calculateBearing(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Float {
        val phi1 = Math.toRadians(fromLat)
        val phi2 = Math.toRadians(toLat)
        val deltaLambda = Math.toRadians(toLng - fromLng)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val theta = atan2(y, x)
        val degrees = Math.toDegrees(theta)

        val normalized = (degrees + 360.0) % 360.0
        return normalized.toFloat()
    }
}
