package com.campusquest.device.location.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.GeofenceRegistrationState
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles Google Play Services Geofencing lifecycle, registration reconciliation,
 * and active-game isolation for Campus Quest.
 */
class GeofenceManagerHelper(
    private val context: Context
) {
    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private var currentActiveGameId: String? = null
    private val registeredKeys = mutableSetOf<String>()

    private val _registrationState = MutableStateFlow(GeofenceRegistrationState())
    val registrationState: StateFlow<GeofenceRegistrationState> = _registrationState.asStateFlow()

    val activeGameId: String? get() = currentActiveGameId

    /**
     * Reconciles dynamic geofences for a game.
     * Computes differential updates (adds new, removes removed, preserves unchanged)
     * and guarantees active-game isolation.
     */
    @SuppressLint("MissingPermission")
    fun reconcileGeofences(
        gameId: String,
        checkpoints: List<Checkpoint>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        // If switching games, unregister everything from the previous game first
        if (currentActiveGameId != null && currentActiveGameId != gameId) {
            unregisterAllGeofences {
                currentActiveGameId = gameId
                registerGeofencesInternal(gameId, checkpoints, onSuccess, onFailure)
            }
            return
        }

        currentActiveGameId = gameId

        val desiredKeyMap = checkpoints.associateBy { GeofenceKey.build(gameId, it.id) }
        val desiredKeys = desiredKeyMap.keys

        val keysToRemove = registeredKeys.subtract(desiredKeys).toList()
        val keysToAdd = desiredKeys.subtract(registeredKeys)

        if (keysToRemove.isNotEmpty()) {
            geofencingClient.removeGeofences(keysToRemove)
                .addOnSuccessListener {
                    registeredKeys.removeAll(keysToRemove.toSet())
                    Log.d(TAG, "Removed ${keysToRemove.size} stale geofences for game $gameId")
                }
        }

        if (keysToAdd.isEmpty()) {
            _registrationState.value = GeofenceRegistrationState(
                activeGameId = gameId,
                registeredCheckpointCount = registeredKeys.size,
                isPlayServicesGeofencingActive = true,
                isUsingFallback = false
            )
            onSuccess()
            return
        }

        val checkpointsToAdd = keysToAdd.mapNotNull { desiredKeyMap[it] }
        registerGeofencesInternal(gameId, checkpointsToAdd, onSuccess, onFailure)
    }

    @SuppressLint("MissingPermission")
    private fun registerGeofencesInternal(
        gameId: String,
        checkpoints: List<Checkpoint>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (checkpoints.isEmpty()) {
            onSuccess()
            return
        }

        val geofenceList = checkpoints.map { cp ->
            val requestId = GeofenceKey.build(gameId, cp.id)
            Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(cp.lat, cp.lng, cp.radiusM)
                .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    checkpoints.forEach { registeredKeys.add(GeofenceKey.build(gameId, it.id)) }
                    _registrationState.value = GeofenceRegistrationState(
                        activeGameId = gameId,
                        registeredCheckpointCount = registeredKeys.size,
                        isPlayServicesGeofencingActive = true,
                        isUsingFallback = false
                    )
                    Log.d(TAG, "Successfully registered ${checkpoints.size} dynamic geofences for game $gameId")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Play Services geofencing failed, falling back to Scoped Fallback: ${e.message}")
                    _registrationState.value = GeofenceRegistrationState(
                        activeGameId = gameId,
                        registeredCheckpointCount = 0,
                        isPlayServicesGeofencingActive = false,
                        isUsingFallback = true,
                        error = e.message
                    )
                    onFailure(e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing for geofences: ${e.message}")
            _registrationState.value = GeofenceRegistrationState(
                activeGameId = gameId,
                registeredCheckpointCount = 0,
                isPlayServicesGeofencingActive = false,
                isUsingFallback = true,
                error = "Permission denied"
            )
            onFailure(e)
        }
    }

    /**
     * Unregisters all active geofences when closing quest session or switching accounts.
     */
    fun unregisterAllGeofences(onComplete: () -> Unit = {}) {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnCompleteListener {
                    registeredKeys.clear()
                    currentActiveGameId = null
                    _registrationState.value = GeofenceRegistrationState()
                    Log.d(TAG, "Unregistered all geofences")
                    onComplete()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofences: ${e.message}")
            registeredKeys.clear()
            currentActiveGameId = null
            _registrationState.value = GeofenceRegistrationState()
            onComplete()
        }
    }
}
