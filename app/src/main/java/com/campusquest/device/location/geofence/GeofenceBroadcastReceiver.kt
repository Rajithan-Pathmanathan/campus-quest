package com.campusquest.device.location.geofence

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.ui.common.AppProviders
import com.campusquest.ui.common.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        var alertFor: Pair<String, String>? = null // goAsync() may be used once per broadcast
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
                if (transitionType == GeofenceTransitionType.ENTER && alertFor == null) alertFor = gameId to checkpointId
            } else {
                Log.w(TAG, "Unrecognized geofence requestId format: ${geofence.requestId}")
            }
        }
        alertFor?.let { (gameId, checkpointId) -> showNearbyAlert(context, gameId, checkpointId) }
    }

    /** "You're close" notification; tapping it opens the quest. Skipped if that place is already found. */
    private fun showNearbyAlert(context: Context, gameId: String, checkpointId: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = CampusQuestApplication.instance.gameRepository
                val found = repo.observePlayerProgress(gameId, AppProviders.currentUserId()).first()
                    ?.completedCheckpointIds.orEmpty()
                val checkpoint = repo.getGameCheckpoints(gameId).firstOrNull { it.id == checkpointId }
                if (checkpoint == null || checkpointId in found) return@launch

                val open = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_GAME_ID, gameId)
                }
                val tap = PendingIntent.getActivity(
                    context, checkpointId.hashCode(), open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(context, CampusQuestApplication.CHANNEL_GEOFENCE_EVENTS)
                    .setSmallIcon(android.R.drawable.ic_dialog_map)
                    .setContentTitle(context.getString(R.string.geofence_alert_title))
                    .setContentText(context.getString(R.string.geofence_alert_body, checkpoint.name))
                    .setAutoCancel(true)
                    .setContentIntent(tap)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(checkpointId.hashCode(), notification)
            } catch (e: Exception) {
                Log.w(TAG, "Could not show geofence alert: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }
}
