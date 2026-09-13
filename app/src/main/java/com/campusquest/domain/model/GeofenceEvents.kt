package com.campusquest.domain.model

enum class GeofenceTransitionType {
    ENTER,
    EXIT
}

data class GeofenceTransitionEvent(
    val gameId: String,
    val checkpointId: String,
    val transitionType: GeofenceTransitionType,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeofenceRegistrationState(
    val activeGameId: String? = null,
    val registeredCheckpointCount: Int = 0,
    val isPlayServicesGeofencingActive: Boolean = false,
    val isUsingFallback: Boolean = false,
    val error: String? = null
)
