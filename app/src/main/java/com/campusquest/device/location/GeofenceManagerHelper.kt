package com.campusquest.device.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.CheckpointEligibility
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManagerHelper(
    private val context: Context,
    private val distanceCalculator: DistanceCalculator = HaversineDistanceCalculator()
) {
    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val GEOFENCE_DWELL_DELAY_MS = 3000 // 3 seconds

        fun buildGeofenceKey(gameId: String, checkpointId: String): String = "$gameId#$checkpointId"

        fun parseGeofenceKey(key: String): Pair<String, String>? {
            val parts = key.split("#")
            return if (parts.size == 2) Pair(parts[0], parts[1]) else null
        }
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

    private val registeredGeofenceKeys = mutableSetOf<String>()

    /**
     * Dynamically registers geofences for all checkpoints in the active game.
     * Checkpoints are keyed by "${gameId}#${checkpointId}" to prevent cross-game collision.
     */
    @SuppressLint("MissingPermission")
    fun registerGameGeofences(gameId: String, checkpoints: List<Checkpoint>, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (checkpoints.isEmpty()) return

        val geofenceList = checkpoints.map { cp ->
            val requestId = buildGeofenceKey(gameId, cp.id)
            Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(cp.lat, cp.lng, cp.radiusM)
                .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(GEOFENCE_DWELL_DELAY_MS)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    registeredGeofenceKeys.clear()
                    checkpoints.forEach { registeredGeofenceKeys.add(buildGeofenceKey(gameId, it.id)) }
                    Log.d(TAG, "Successfully registered ${checkpoints.size} dynamic geofences for game $gameId")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to register geofences with Play Services, falling back to GPS distance: ${e.message}")
                    onFailure(e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing for geofences: ${e.message}")
            onFailure(e)
        }
    }

    /**
     * Unregisters all active geofences when navigating away or switching games.
     */
    fun unregisterGeofences(onComplete: () -> Unit = {}) {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnCompleteListener {
                    registeredGeofenceKeys.clear()
                    onComplete()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofences: ${e.message}")
            registeredGeofenceKeys.clear()
            onComplete()
        }
    }

    /**
     * Scoped GPS Fallback Evaluator:
     * Evaluates whether the user is physically within the active checkpoint's boundary
     * using continuous GPS distance with hysteresis to avoid boundary flapping.
     */
    fun evaluateEligibility(
        activeCheckpoint: Checkpoint,
        locationState: LocationState,
        isCurrentlyInside: Boolean = false,
        geofenceEnteredOverride: Boolean = false
    ): CheckpointEligibility {
        val lat = locationState.smoothedLatitude ?: locationState.location?.latitude
        val lng = locationState.smoothedLongitude ?: locationState.location?.longitude

        if (lat == null || lng == null) {
            return CheckpointEligibility(
                gameId = activeCheckpoint.gameId,
                checkpointId = activeCheckpoint.id,
                distanceMeters = null,
                gpsAccuracyMeters = locationState.accuracyMeters,
                geofenceEntered = geofenceEnteredOverride,
                eligibleForScan = geofenceEnteredOverride,
                bearingDegrees = null
            )
        }

        val distance = distanceCalculator.calculateDistanceMeters(lat, lng, activeCheckpoint.lat, activeCheckpoint.lng)
        val bearing = distanceCalculator.calculateBearingDegrees(lat, lng, activeCheckpoint.lat, activeCheckpoint.lng)

        val isInsideRadius = distanceCalculator.isWithinRadiusWithHysteresis(
            currentLat = lat,
            currentLng = lng,
            targetLat = activeCheckpoint.lat,
            targetLng = activeCheckpoint.lng,
            radiusMeters = activeCheckpoint.radiusM,
            isCurrentlyInside = isCurrentlyInside,
            hysteresisMarginMeters = 3.0f
        )

        val eligible = geofenceEnteredOverride || isInsideRadius

        return CheckpointEligibility(
            gameId = activeCheckpoint.gameId,
            checkpointId = activeCheckpoint.id,
            distanceMeters = distance,
            gpsAccuracyMeters = locationState.accuracyMeters,
            geofenceEntered = eligible,
            eligibleForScan = eligible,
            bearingDegrees = bearing
        )
    }
}
