package com.campusquest.device.location.geofence

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.GeofenceState
import com.campusquest.domain.model.LocationState
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Keeps one real Android geofence around the player's **next unfound checkpoint** of the quest
 * they started. Progress is observed, so after each discovery the fence moves to the next place,
 * and it is removed when the quest is complete. Entering the fence fires [GeofenceBroadcastReceiver],
 * which shows a "you're close" notification even when the app is closed.
 *
 * Foreground fallback: while the app is tracking location (map / scan open), [ScopedGeofenceFallback]
 * checks the same fence from live GPS, so players who did not allow background location still get
 * the alert. Both paths use the same notification id, so it never appears twice.
 */
class QuestGeofenceCoordinator(
    private val geofences: GeofenceManagerHelper,
    private val repository: GameRepository,
    private val userId: () -> String,
    private val scope: CoroutineScope,
    private val locations: Flow<LocationState>,
    private val onFallbackEnter: (gameId: String, checkpointId: String) -> Unit,
    private val fallback: ScopedGeofenceFallback = ScopedGeofenceFallback()
) {
    private var job: Job? = null
    private var activeGameId: String? = null

    /** Call when the player starts a quest. [force] re-registers, e.g. after a new permission grant. */
    fun activate(gameId: String, force: Boolean = false) {
        if (!force && gameId == activeGameId && job?.isActive == true) return
        activeGameId = gameId
        job?.cancel()
        fallback.reset()
        job = scope.launch {
            try {
                if (force) geofences.unregisterAllGeofences()
                val checkpoints = repository.getGameCheckpoints(gameId)
                val fences = repository.observePlayerProgress(gameId, userId())
                    .map { fenceFor(checkpoints, it?.completedCheckpointIds.orEmpty().toSet()) }
                    .distinctUntilChanged()
                    .onEach { fence ->
                        if (fence == null) geofences.unregisterAllGeofences()
                        else geofences.reconcileGeofences(gameId, listOf(fence))
                    }
                var wasInside = false
                combine(fences, locations) { fence, location -> fence to location }.collect { (fence, location) ->
                    if (fence == null) return@collect
                    val inside = fallback.evaluate(fence, location).geofenceState == GeofenceState.INSIDE
                    if (inside && !wasInside) onFallbackEnter(gameId, fence.id)
                    wasInside = inside
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // No checkpoints / no permission: the in-app zones still guide the player.
            }
        }
    }

    companion object {
        /** Android geofences are unreliable below ~100 m; arrival itself is decided in-app. */
        const val MIN_FENCE_RADIUS_M = 100f

        /**
         * The next unfound checkpoint (by order), widened to [MIN_FENCE_RADIUS_M].
         */
        fun fenceFor(checkpoints: List<Checkpoint>, found: Set<String>): Checkpoint? =
            checkpoints.sortedBy { it.order }
                .firstOrNull { it.id !in found }
                ?.let { it.copy(radiusM = maxOf(it.radiusM, MIN_FENCE_RADIUS_M)) }
    }
}
