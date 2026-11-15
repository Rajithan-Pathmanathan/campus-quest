package com.campusquest.device.location.geofence

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.quest.QuestZones
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps one real Android geofence around the player's **next unfound checkpoint** of the quest
 * they started. Progress is observed, so after each discovery the fence moves to the next place,
 * and it is removed when the quest is complete. Entering the fence fires [GeofenceBroadcastReceiver],
 * which shows a "you're close" notification even when the app is closed.
 */
class QuestGeofenceCoordinator(
    private val geofences: GeofenceManagerHelper,
    private val repository: GameRepository,
    private val userId: () -> String,
    private val scope: CoroutineScope
) {
    private var job: Job? = null
    private var activeGameId: String? = null

    /** Call when the player starts a quest. [force] re-registers, e.g. after a new permission grant. */
    fun activate(gameId: String, force: Boolean = false) {
        if (!force && gameId == activeGameId && job?.isActive == true) return
        activeGameId = gameId
        job?.cancel()
        job = scope.launch {
            try {
                if (force) geofences.unregisterAllGeofences()
                val checkpoints = repository.getGameCheckpoints(gameId)
                repository.observePlayerProgress(gameId, userId())
                    .map { it?.completedCheckpointIds.orEmpty().toSet() }
                    .distinctUntilChanged()
                    .collect { found ->
                        val fence = fenceFor(checkpoints, found)
                        if (fence == null) geofences.unregisterAllGeofences()
                        else geofences.reconcileGeofences(gameId, listOf(fence))
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // No checkpoints / no permission: the in-app zones still guide the player.
            }
        }
    }

    companion object {
        /**
         * The next unfound checkpoint (by order), widened to the story's "middle ring":
         * Android geofences are unreliable below ~100 m, and the relic itself is still
         * decided in-app by the zones (10 m).
         */
        fun fenceFor(checkpoints: List<Checkpoint>, found: Set<String>): Checkpoint? =
            checkpoints.sortedBy { it.order }
                .firstOrNull { it.id !in found }
                ?.let { it.copy(radiusM = maxOf(it.radiusM, QuestZones.MIDDLE_RING_M)) }
    }
}
