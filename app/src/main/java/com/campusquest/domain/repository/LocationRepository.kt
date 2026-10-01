package com.campusquest.domain.repository

import com.campusquest.device.location.LocationPermissionState
import com.campusquest.device.location.LocationState
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture repository contract for device GPS tracking and location permissions.
 * Exposes pure Kotlin StateFlows to M1 (Map) and M4 (Scan HUD) without exposing Android location APIs.
 */
interface LocationRepository {
    val locationState: StateFlow<LocationState>

    fun startTracking(intervalMs: Long = 5000L, minDistanceM: Float = 2.0f)
    fun stopTracking()
    fun updatePermissionState(state: LocationPermissionState)
}
