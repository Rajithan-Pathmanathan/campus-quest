package com.campusquest.domain.fusion

import com.campusquest.domain.model.LocationState

/**
 * Converts distance-to-checkpoint into the normalized GPS fusion score (workplan §36).
 *
 *   score = 1 - distance / (2 × radius), clamped to [0,1]
 *
 *   distance 0          → 1.0   (standing on the checkpoint)
 *   distance = radius   → 0.5   (geofence edge)
 *   distance ≥ 2×radius → 0.0
 *
 * Being inside the radius is therefore NOT automatically full GPS credit (§36).
 * With the default weights (GPS 0.40 / light 0.30 / motion 0.30, threshold 0.85) a player
 * with full light + motion needs gpsScore ≥ 0.625, i.e. within 0.75 × radius of the checkpoint.
 *
 * No valid fix (no coordinates, or accuracy worse than 35 m) → 0.0, never null:
 * GPS is mandatory evidence, so a missing fix must block discovery rather than
 * be re-normalized away (which would let a player discover from anywhere).
 */
object GpsScoreCalculator {

    fun score(distanceMeters: Float, radiusMeters: Float): Float {
        if (radiusMeters <= 0f) return 0.0f
        return (1.0f - distanceMeters / (2.0f * radiusMeters)).coerceIn(0.0f, 1.0f)
    }

    fun score(
        location: LocationState,
        targetLat: Double,
        targetLng: Double,
        radiusMeters: Float,
        distance: (lat1: Double, lng1: Double, lat2: Double, lng2: Double) -> Float
    ): Float {
        if (!location.hasValidFix) return 0.0f
        val d = distance(location.smoothedLatitude!!, location.smoothedLongitude!!, targetLat, targetLng)
        return score(d, radiusMeters)
    }
}
