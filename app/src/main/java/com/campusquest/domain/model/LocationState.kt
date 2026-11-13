package com.campusquest.domain.model

/**
 * Immutable state model describing the device's current location, accuracy, and operational health.
 * Pure Kotlin: no android.location types, so domain/gameplay code stays JVM-testable.
 */
data class LocationState(
    val permissionState: LocationPermissionState = LocationPermissionState.Unknown,
    val servicesEnabled: Boolean = false,
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
        get() = smoothedLatitude != null && smoothedLongitude != null &&
                (accuracyMeters == null || accuracyMeters <= MAX_VALID_ACCURACY_M)

    fun formattedAccuracy(): String {
        return accuracyMeters?.let { "±${"%.1f".format(it)}m" } ?: "Unknown accuracy"
    }

    companion object {
        const val MAX_VALID_ACCURACY_M = 35f
    }
}
