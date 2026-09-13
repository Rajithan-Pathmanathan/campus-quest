package com.campusquest.device.location

/**
 * Represents the current permission and availability state for device location services.
 */
sealed interface LocationPermissionState {
    data object Unknown : LocationPermissionState
    data object Granted : LocationPermissionState
    data object Denied : LocationPermissionState
    data object PermanentlyDenied : LocationPermissionState
    data object ServicesDisabled : LocationPermissionState
}
