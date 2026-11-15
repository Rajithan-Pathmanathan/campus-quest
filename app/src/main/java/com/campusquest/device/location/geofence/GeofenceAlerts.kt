package com.campusquest.device.location.geofence

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.ui.common.AppProviders
import com.campusquest.ui.common.MainActivity
import kotlinx.coroutines.flow.first

/** The "you're close" notification, used by Play Services geofences and by the foreground fallback. */
object GeofenceAlerts {

    /** Shows the alert unless notifications are off or that place is already found. Tapping opens the quest. */
    suspend fun notifyIfUnfound(context: Context, gameId: String, checkpointId: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val repo = CampusQuestApplication.instance.gameRepository
        val found = repo.observePlayerProgress(gameId, AppProviders.currentUserId()).first()
            ?.completedCheckpointIds.orEmpty()
        val checkpoint = repo.getGameCheckpoints(gameId).firstOrNull { it.id == checkpointId } ?: return
        if (checkpointId in found) return

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
        // Same id per checkpoint: the Play Services alert and the fallback never show it twice.
        manager.notify(checkpointId.hashCode(), notification)
    }
}
