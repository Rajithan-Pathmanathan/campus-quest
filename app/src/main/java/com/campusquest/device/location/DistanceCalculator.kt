package com.campusquest.device.location

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interface and reference implementation for geospatial distance and bearing calculations.
 */
interface DistanceCalculator {
    fun calculateDistanceMeters(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Float

    fun calculateBearingDegrees(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Float

    fun isWithinRadius(
        currentLat: Double,
        currentLng: Double,
        targetLat: Double,
        targetLng: Double,
        radiusMeters: Float
    ): Boolean
}

class HaversineDistanceCalculator : DistanceCalculator {

    companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0
    }

    override fun calculateDistanceMeters(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Float {
        val dLat = Math.toRadians(toLat - fromLat)
        val dLng = Math.toRadians(toLng - fromLng)

        val lat1Rad = Math.toRadians(fromLat)
        val lat2Rad = Math.toRadians(toLat)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLng / 2) * sin(dLng / 2) * cos(lat1Rad) * cos(lat2Rad)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS_METERS * c).toFloat()
    }

    override fun calculateBearingDegrees(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Float {
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val dLng = Math.toRadians(toLng - fromLng)

        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val initialBearingRad = atan2(y, x)

        val initialBearingDeg = Math.toDegrees(initialBearingRad)
        return ((initialBearingDeg + 360) % 360).toFloat()
    }

    override fun isWithinRadius(
        currentLat: Double,
        currentLng: Double,
        targetLat: Double,
        targetLng: Double,
        radiusMeters: Float
    ): Boolean {
        return calculateDistanceMeters(currentLat, currentLng, targetLat, targetLng) <= radiusMeters
    }
}
