package com.campusquest.device.location

import android.location.Location

/**
 * Low-pass exponential moving average (EMA) filter to suppress GPS jitter
 * and reject extreme location spikes near checkpoint boundaries.
 */
class GpsSmoothingFilter(
    private val alpha: Float = 0.65f, // Smoothing weight (0 < alpha <= 1)
    private val maxAcceptedAccuracyMeters: Float = 40.0f
) {
    private var smoothedLat: Double? = null
    private var smoothedLng: Double? = null
    private var lastTimestamp: Long = 0L

    data class SmoothedCoordinate(
        val latitude: Double,
        val longitude: Double,
        val isFiltered: Boolean
    )

    fun filter(location: Location): SmoothedCoordinate {
        val lat = location.latitude
        val lng = location.longitude

        // Reject fixes with poor accuracy if we already have an established estimate
        if (location.hasAccuracy() && location.accuracy > maxAcceptedAccuracyMeters && smoothedLat != null) {
            return SmoothedCoordinate(smoothedLat!!, smoothedLng!!, isFiltered = true)
        }

        val currentSmoothLat = smoothedLat
        val currentSmoothLng = smoothedLng

        if (currentSmoothLat == null || currentSmoothLng == null) {
            smoothedLat = lat
            smoothedLng = lng
            lastTimestamp = location.time
            return SmoothedCoordinate(lat, lng, isFiltered = false)
        }

        // Apply Exponential Moving Average: S_t = alpha * Y_t + (1 - alpha) * S_{t-1}
        val newLat = (alpha * lat) + ((1f - alpha) * currentSmoothLat)
        val newLng = (alpha * lng) + ((1f - alpha) * currentSmoothLng)

        smoothedLat = newLat
        smoothedLng = newLng
        lastTimestamp = location.time

        return SmoothedCoordinate(newLat, newLng, isFiltered = true)
    }

    fun reset() {
        smoothedLat = null
        smoothedLng = null
        lastTimestamp = 0L
    }
}
