package com.campusquest.device.location

import com.campusquest.device.location.geofence.BoundaryHysteresis
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.GeofenceState
import com.campusquest.domain.model.GeofenceVisualState

/**
 * Data provider helper for Member 1's Campus Map.
 * Computes pure mathematical distances, geofence states, and bearings for all checkpoints.
 * UI styling (colors, stroke, fill, animations) is completely owned and rendered by Member 1.
 */
class MapGeofenceDataProvider(
    private val distanceCalculator: DistanceCalculator = HaversineDistanceCalculator(),
    private val hysteresis: BoundaryHysteresis = BoundaryHysteresis()
) {

    /**
     * Generates a GeofenceVisualState for a specific checkpoint relative to current player location.
     */
    fun createVisualState(
        checkpoint: Checkpoint,
        currentLocation: LocationState,
        previousState: GeofenceState = GeofenceState.UNKNOWN
    ): GeofenceVisualState {
        val lat = currentLocation.smoothedLatitude ?: currentLocation.location?.latitude
        val lng = currentLocation.smoothedLongitude ?: currentLocation.location?.longitude

        val distance: Float? = if (lat != null && lng != null) {
            distanceCalculator.calculateDistanceMeters(
                fromLat = lat,
                fromLng = lng,
                toLat = checkpoint.lat,
                toLng = checkpoint.lng
            )
        } else {
            null
        }

        val state = if (distance != null) {
            hysteresis.evaluate(distance, checkpoint.radiusM, previousState)
        } else {
            GeofenceState.UNKNOWN
        }

        val bearing = if (lat != null && lng != null) {
            BearingCalculator.calculateBearing(
                fromLat = lat,
                fromLng = lng,
                toLat = checkpoint.lat,
                toLng = checkpoint.lng
            )
        } else {
            null
        }

        return GeofenceVisualState(
            checkpointId = checkpoint.id,
            state = state,
            distanceMeters = distance,
            radiusMeters = checkpoint.radiusM,
            bearingDegrees = bearing
        )
    }
}
