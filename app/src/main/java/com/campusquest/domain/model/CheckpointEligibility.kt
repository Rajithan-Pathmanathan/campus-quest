package com.campusquest.domain.model

/**
 * Immutable contract from M3 to M4 / M1 indicating whether a checkpoint
 * is physically eligible for active scanning / discovery mode.
 */
data class CheckpointEligibility(
    val gameId: String,
    val checkpointId: String,
    val distanceMeters: Float?,
    val gpsAccuracyMeters: Float?,
    val geofenceEntered: Boolean,
    val eligibleForScan: Boolean,
    val bearingDegrees: Float? = null
)
