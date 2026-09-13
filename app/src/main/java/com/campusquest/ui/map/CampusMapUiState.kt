package com.campusquest.ui.map

import com.campusquest.device.location.LocationState
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.GeofenceState
import com.campusquest.domain.model.GeofenceVisualState

/**
 * UI State representing the interactive campus quest map, active target checkpoint,
 * real-time distance/bearing indicators, and geofence eligibility status.
 */
data class CampusMapUiState(
    val isLoading: Boolean = false,
    val gameId: String? = null,
    val gameTitle: String = "",
    val checkpoints: List<Checkpoint> = emptyList(),
    val activeCheckpointIndex: Int = 0,
    val playerLocation: LocationState? = null,
    val geofenceVisualStates: Map<String, GeofenceVisualState> = emptyMap(),
    val errorMessage: String? = null
) {
    val activeCheckpoint: Checkpoint?
        get() = checkpoints.getOrNull(activeCheckpointIndex)

    val activeGeofenceVisualState: GeofenceVisualState?
        get() = activeCheckpoint?.let { geofenceVisualStates[it.id] }

    val targetDistanceMeters: Float?
        get() = activeGeofenceVisualState?.distanceMeters

    val targetBearingDegrees: Float?
        get() = activeGeofenceVisualState?.bearingDegrees

    val isScanEligible: Boolean
        get() {
            val visualState = activeGeofenceVisualState ?: return false
            val checkpoint = activeCheckpoint ?: return false
            return visualState.state == GeofenceState.INSIDE ||
                    ((visualState.distanceMeters ?: Float.MAX_VALUE) <= checkpoint.radiusM)
        }

    fun formattedDistance(): String {
        val dist = targetDistanceMeters ?: return "Calculating distance..."
        return if (dist < 1000f) {
            "${dist.toInt()}m remaining"
        } else {
            "${"%.1f".format(dist / 1000f)}km remaining"
        }
    }

    fun formattedBearing(): String {
        val bearing = targetBearingDegrees ?: return "--°"
        val normalized = ((bearing % 360) + 360) % 360
        val cardinal = when {
            normalized >= 337.5 || normalized < 22.5 -> "N"
            normalized < 67.5 -> "NE"
            normalized < 112.5 -> "E"
            normalized < 157.5 -> "SE"
            normalized < 202.5 -> "S"
            normalized < 247.5 -> "SW"
            normalized < 292.5 -> "W"
            else -> "NW"
        }
        return "${normalized.toInt()}° $cardinal"
    }
}
