package com.campusquest.data.repository

import com.campusquest.device.location.LocationManagerHelper
import com.campusquest.device.location.LocationPermissionState
import com.campusquest.device.location.LocationState
import com.campusquest.domain.repository.LocationRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation of LocationRepository wrapping the hardware LocationManagerHelper.
 */
class LocationRepositoryImpl(
    private val locationManager: LocationManagerHelper
) : LocationRepository {

    override val locationState: StateFlow<LocationState> = locationManager.locationState

    override fun startTracking(intervalMs: Long, minDistanceM: Float) {
        locationManager.startLocationUpdates(intervalMs, minDistanceM)
    }

    override fun stopTracking() {
        locationManager.stopLocationUpdates()
    }

    override fun updatePermissionState(state: LocationPermissionState) {
        locationManager.updatePermissionState(state)
    }
}
