package com.campusquest.device.location

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common abstraction for Android Location Services.
 */
interface LocationProvider {
    val locationState: StateFlow<LocationState>
    val smoothedCoordinates: Flow<GpsSmoothingFilter.SmoothedCoordinate>

    fun startLocationUpdates(intervalMs: Long = 3000L, minDistanceM: Float = 2.0f)
    fun stopLocationUpdates()
    fun updatePermissionState(state: LocationPermissionState)
    fun calculateDistanceTo(targetLat: Double, targetLng: Double): Float
    fun calculateBearingTo(targetLat: Double, targetLng: Double): Float
    fun isWithinGeofence(targetLat: Double, targetLng: Double, radiusMeters: Float): Boolean
}
