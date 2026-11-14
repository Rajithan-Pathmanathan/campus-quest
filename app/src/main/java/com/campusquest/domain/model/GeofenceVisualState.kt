package com.campusquest.domain.model

/**
 * Clean data contract provided by M3 to M1 (Campus Map) for rendering geofence overlays.
 * Contains purely mathematical and state values; UI styling (colors, stroke, fill) is owned by M1.
 */
data class GeofenceVisualState(
    val checkpointId: String,
    val state: GeofenceState,
    val distanceMeters: Float?,
    val radiusMeters: Float,
    val bearingDegrees: Float?
)
