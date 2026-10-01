package com.campusquest.domain.model

/**
 * Immutable contract from M3 to M1 (Map) and M4 (Scan HUD) indicating whether a checkpoint
 * is physically eligible for active scanning / discovery mode.
 */
data class CheckpointEligibility(
    val gameId: String,
    val checkpointId: String,
    val distanceMeters: Float?,
    val radiusMeters: Float,
    val geofenceState: GeofenceState,
    val gpsAccuracyMeters: Float?,
    val eligibleForScan: Boolean,
    val bearingDegrees: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)
