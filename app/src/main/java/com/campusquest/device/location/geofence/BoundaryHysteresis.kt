package com.campusquest.device.location.geofence

import com.campusquest.domain.model.GeofenceState

/**
 * Stateful hysteresis calculator to eliminate perimeter state bouncing (INSIDE <-> OUTSIDE)
 * when the player is walking or standing near the geofence perimeter.
 */
class BoundaryHysteresis(
    private val hysteresisMarginMeters: Float = 3.0f
) {
    /**
     * Evaluates the next GeofenceState based on current distance, checkpoint radius, and previous state.
     *
     * State Machine:
     * - OUTSIDE / UNKNOWN -> transitions to INSIDE only when distance <= radiusMeters
     * - INSIDE -> transitions to OUTSIDE only when distance > (radiusMeters + hysteresisMarginMeters)
     */
    fun evaluate(
        distanceMeters: Float,
        radiusMeters: Float,
        previousState: GeofenceState
    ): GeofenceState {
        return when (previousState) {
            GeofenceState.OUTSIDE, GeofenceState.UNKNOWN -> {
                if (distanceMeters <= radiusMeters) {
                    GeofenceState.INSIDE
                } else {
                    GeofenceState.OUTSIDE
                }
            }
            GeofenceState.INSIDE -> {
                if (distanceMeters > (radiusMeters + hysteresisMarginMeters)) {
                    GeofenceState.OUTSIDE
                } else {
                    GeofenceState.INSIDE
                }
            }
        }
    }
}
