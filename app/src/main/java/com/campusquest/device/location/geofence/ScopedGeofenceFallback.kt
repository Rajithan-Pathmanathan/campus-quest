package com.campusquest.device.location.geofence

import com.campusquest.device.location.DistanceCalculator
import com.campusquest.device.location.HaversineDistanceCalculator
import com.campusquest.device.location.LocationState
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.CheckpointEligibility
import com.campusquest.domain.model.GeofenceState

/**
 * Battery-optimized fallback evaluator when Google Play Geofencing API is unavailable.
 * Restricts continuous location calculations strictly to the active target checkpoint.
 */
class ScopedGeofenceFallback(
    private val distanceCalculator: DistanceCalculator = HaversineDistanceCalculator(),
    private val hysteresis: BoundaryHysteresis = BoundaryHysteresis(hysteresisMarginMeters = 3.0f)
) {
    private var lastKnownState: GeofenceState = GeofenceState.OUTSIDE
    private var trackedCheckpointId: String? = null

    /**
     * Evaluates eligibility for the single active target checkpoint.
     */
    fun evaluate(
        activeCheckpoint: Checkpoint,
        locationState: LocationState,
        geofenceEnteredOverride: Boolean = false
    ): CheckpointEligibility {
        if (trackedCheckpointId != activeCheckpoint.id) {
            trackedCheckpointId = activeCheckpoint.id
            lastKnownState = GeofenceState.OUTSIDE
        }

        val lat = locationState.smoothedLatitude ?: locationState.location?.latitude
        val lng = locationState.smoothedLongitude ?: locationState.location?.longitude

        if (lat == null || lng == null) {
            return CheckpointEligibility(
                gameId = activeCheckpoint.gameId,
                checkpointId = activeCheckpoint.id,
                distanceMeters = null,
                radiusMeters = activeCheckpoint.radiusM,
                geofenceState = if (geofenceEnteredOverride) GeofenceState.INSIDE else GeofenceState.UNKNOWN,
                gpsAccuracyMeters = locationState.accuracyMeters,
                eligibleForScan = geofenceEnteredOverride,
                bearingDegrees = null
            )
        }

        val distance = distanceCalculator.calculateDistanceMeters(lat, lng, activeCheckpoint.lat, activeCheckpoint.lng)
        val bearing = distanceCalculator.calculateBearingDegrees(lat, lng, activeCheckpoint.lat, activeCheckpoint.lng)

        val nextState = if (geofenceEnteredOverride) {
            GeofenceState.INSIDE
        } else {
            hysteresis.evaluate(distance, activeCheckpoint.radiusM, lastKnownState)
        }

        lastKnownState = nextState
        val isEligible = nextState == GeofenceState.INSIDE

        return CheckpointEligibility(
            gameId = activeCheckpoint.gameId,
            checkpointId = activeCheckpoint.id,
            distanceMeters = distance,
            radiusMeters = activeCheckpoint.radiusM,
            geofenceState = nextState,
            gpsAccuracyMeters = locationState.accuracyMeters,
            eligibleForScan = isEligible,
            bearingDegrees = bearing
        )
    }

    fun reset() {
        lastKnownState = GeofenceState.OUTSIDE
        trackedCheckpointId = null
    }
}
