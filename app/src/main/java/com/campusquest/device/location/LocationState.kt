package com.campusquest.device.location

import android.location.Location

/**
 * Immutable state model describing the device's current location, accuracy, and operational health.
 */
data class LocationState(
    val permissionState: LocationPermissionState = LocationPermissionState.Unknown,
    val servicesEnabled: Boolean = false,
    val location: Location? = null,
    val smoothedLatitude: Double? = null,
    val smoothedLongitude: Double? = null,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val hasValidFix: Boolean
        get() = location != null && (accuracyMeters == null || accuracyMeters <= 35f)

    fun formattedAccuracy(): String {
        return accuracyMeters?.let { "±${"%.1f".format(it)}m" } ?: "Unknown accuracy"
    }
}
