package com.campusquest.device.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.campusquest.domain.model.LocationPermissionState
import com.campusquest.domain.model.LocationState

class LocationManagerHelper(
    private val context: Context,
    private val distanceCalculator: DistanceCalculator = HaversineDistanceCalculator(),
    private val smoothingFilter: GpsSmoothingFilter = GpsSmoothingFilter()
) : LocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _locationState = MutableStateFlow(LocationState())
    override val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _smoothedCoordinates = MutableSharedFlow<GpsSmoothingFilter.SmoothedCoordinate>(replay = 1)
    override val smoothedCoordinates: SharedFlow<GpsSmoothingFilter.SmoothedCoordinate> = _smoothedCoordinates.asSharedFlow()

    private var isCurrentlyListening = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val rawLocation = result.lastLocation ?: return
            val filtered = smoothingFilter.filter(rawLocation)
            _smoothedCoordinates.tryEmit(filtered)

            val isGpsEnabled = isLocationServiceEnabled()

            _locationState.value = LocationState(
                permissionState = _locationState.value.permissionState,
                servicesEnabled = isGpsEnabled,
                smoothedLatitude = filtered.latitude,
                smoothedLongitude = filtered.longitude,
                accuracyMeters = if (rawLocation.hasAccuracy()) rawLocation.accuracy else null,
                altitudeMeters = if (rawLocation.hasAltitude()) rawLocation.altitude else null,
                speedMps = if (rawLocation.hasSpeed()) rawLocation.speed else null,
                bearingDegrees = if (rawLocation.hasBearing()) rawLocation.bearing else null,
                isUpdating = true,
                error = null,
                timestamp = rawLocation.time
            )
        }
    }

    override fun updatePermissionState(state: LocationPermissionState) {
        val servicesEnabled = isLocationServiceEnabled()
        _locationState.value = _locationState.value.copy(
            permissionState = state,
            servicesEnabled = servicesEnabled
        )
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates(intervalMs: Long, minDistanceM: Float) {
        if (isCurrentlyListening) return

        if (!hasLocationPermission()) {
            _locationState.value = _locationState.value.copy(
                permissionState = LocationPermissionState.Denied,
                isUpdating = false,
                error = "Location permission not granted"
            )
            return
        }

        val servicesEnabled = isLocationServiceEnabled()
        if (!servicesEnabled) {
            _locationState.value = _locationState.value.copy(
                servicesEnabled = false,
                error = "Location services disabled on device"
            )
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                intervalMs
            )
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .setMinUpdateDistanceMeters(minDistanceM)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            isCurrentlyListening = true
            _locationState.value = _locationState.value.copy(
                permissionState = LocationPermissionState.Granted,
                isUpdating = true,
                servicesEnabled = true,
                error = null
            )
        } catch (e: SecurityException) {
            _locationState.value = _locationState.value.copy(
                permissionState = LocationPermissionState.Denied,
                isUpdating = false,
                error = "Missing location permission: ${e.message}"
            )
        } catch (e: Exception) {
            _locationState.value = _locationState.value.copy(
                isUpdating = false,
                error = "Failed to start location updates: ${e.message}"
            )
        }
    }

    override fun stopLocationUpdates() {
        if (!isCurrentlyListening) return
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore during teardown
        } finally {
            isCurrentlyListening = false
            smoothingFilter.reset()
            _locationState.value = _locationState.value.copy(isUpdating = false)
        }
    }

    override fun calculateDistanceTo(targetLat: Double, targetLng: Double): Float {
        val lat = _locationState.value.smoothedLatitude ?: return Float.MAX_VALUE
        val lng = _locationState.value.smoothedLongitude ?: return Float.MAX_VALUE
        return distanceCalculator.calculateDistanceMeters(lat, lng, targetLat, targetLng)
    }

    override fun calculateBearingTo(targetLat: Double, targetLng: Double): Float {
        val lat = _locationState.value.smoothedLatitude ?: return 0f
        val lng = _locationState.value.smoothedLongitude ?: return 0f
        return distanceCalculator.calculateBearingDegrees(lat, lng, targetLat, targetLng)
    }

    override fun isWithinGeofence(targetLat: Double, targetLng: Double, radiusMeters: Float): Boolean {
        return calculateDistanceTo(targetLat, targetLng) <= radiusMeters
    }

    /** True when FINE or COARSE location is granted at runtime. */
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isLocationServiceEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }
}
