package com.campusquest.device.location.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.campusquest.domain.model.GeofenceTransitionEvent
import com.campusquest.domain.model.GeofenceTransitionType
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * BroadcastReceiver triggered by Google Play Services Geofencing API.
 * Parses "${gameId}#${checkpointId}" composite keys and emits immutable transition events.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"

        private val _geofenceEvents = MutableSharedFlow<GeofenceTransitionEvent>(extraBufferCapacity = 64)
        val geofenceEvents: SharedFlow<GeofenceTransitionEvent> = _geofenceEvents.asSharedFlow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error code: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        val transitionType = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransitionType.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransitionType.EXIT
            else -> return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
        for (geofence in triggeringGeofences) {
            val parsed = GeofenceKey.parse(geofence.requestId)
            if (parsed != null) {
                val (gameId, checkpointId) = parsed
                val event = GeofenceTransitionEvent(
                    gameId = gameId,
                    checkpointId = checkpointId,
                    transitionType = transitionType,
                    timestamp = System.currentTimeMillis()
                )
                Log.d(TAG, "Geofence event: $transitionType for game=$gameId, cp=$checkpointId")
                _geofenceEvents.tryEmit(event)
            } else {
                Log.w(TAG, "Unrecognized geofence requestId format: ${geofence.requestId}")
            }
        }
    }
}
