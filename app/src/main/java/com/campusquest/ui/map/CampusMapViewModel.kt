package com.campusquest.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.device.location.LocationState
import com.campusquest.device.location.MapGeofenceDataProvider
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.GeofenceState
import com.campusquest.domain.model.LightSignature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.campusquest.domain.repository.GameRepository

/**
 * ViewModel orchestrating the interactive Campus Map UI, tracking player location,
 * geofence boundary visual states, active target waypoint, and scan trigger eligibility.
 */
class CampusMapViewModel(
    private val dataProvider: MapGeofenceDataProvider = MapGeofenceDataProvider(),
    private val gameRepository: GameRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampusMapUiState(isLoading = true))
    val uiState: StateFlow<CampusMapUiState> = _uiState.asStateFlow()

    fun loadMapData(
        gameId: String,
        customTitle: String? = null,
        customCheckpoints: List<Checkpoint>? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val repoGame = try { gameRepository?.getGameDetails(gameId) } catch (e: Exception) { null }
            val title = customTitle ?: repoGame?.title ?: getSampleTitle(gameId)

            val repoCheckpoints = try { gameRepository?.getGameCheckpoints(gameId) } catch (e: Exception) { null }
            val checkpoints = (customCheckpoints ?: repoCheckpoints?.ifEmpty { null } ?: getSampleCheckpoints(gameId)).sortedBy { it.order }

            if (checkpoints.isNotEmpty()) {
                _uiState.update {
                    val updatedVisuals = computeVisualStates(checkpoints, it.playerLocation, it.geofenceVisualStates)
                    it.copy(
                        isLoading = false,
                        gameId = gameId,
                        gameTitle = title,
                        checkpoints = checkpoints,
                        activeCheckpointIndex = 0,
                        geofenceVisualStates = updatedVisuals,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameId = gameId,
                        gameTitle = title,
                        checkpoints = emptyList(),
                        errorMessage = "No checkpoints found for Quest ID '$gameId'."
                    )
                }
            }
        }
    }

    fun onLocationChanged(locationState: LocationState) {
        _uiState.update { current ->
            val updatedVisuals = computeVisualStates(
                current.checkpoints,
                locationState,
                current.geofenceVisualStates
            )
            current.copy(
                playerLocation = locationState,
                geofenceVisualStates = updatedVisuals
            )
        }
    }

    fun selectCheckpoint(index: Int) {
        _uiState.update { current ->
            if (index in current.checkpoints.indices) {
                current.copy(activeCheckpointIndex = index)
            } else {
                current
            }
        }
    }

    fun selectCheckpointById(checkpointId: String) {
        _uiState.update { current ->
            val index = current.checkpoints.indexOfFirst { it.id == checkpointId }
            if (index >= 0) {
                current.copy(activeCheckpointIndex = index)
            } else {
                current
            }
        }
    }

    fun nextCheckpoint(): Boolean {
        var changed = false
        _uiState.update { current ->
            val nextIndex = current.activeCheckpointIndex + 1
            if (nextIndex < current.checkpoints.size) {
                changed = true
                current.copy(activeCheckpointIndex = nextIndex)
            } else {
                current
            }
        }
        return changed
    }

    private fun computeVisualStates(
        checkpoints: List<Checkpoint>,
        locationState: LocationState?,
        existingStates: Map<String, com.campusquest.domain.model.GeofenceVisualState>
    ): Map<String, com.campusquest.domain.model.GeofenceVisualState> {
        if (locationState == null) return emptyMap()

        return checkpoints.associate { cp ->
            val prevState = existingStates[cp.id]?.state ?: GeofenceState.UNKNOWN
            val visualState = dataProvider.createVisualState(cp, locationState, prevState)
            cp.id to visualState
        }
    }

    private fun getSampleTitle(gameId: String): String {
        return when (gameId) {
            "GAME_HERITAGE_001" -> "Colombo Heritage Trail"
            "GAME_CAMPUS_INNOVATION_002" -> "Engineering Quad Cipher"
            else -> "Campus Quest: $gameId"
        }
    }

    private fun getSampleCheckpoints(gameId: String): List<Checkpoint> {
        return listOf(
            Checkpoint(
                id = "CP_BELL_TOWER",
                gameId = gameId,
                name = "Historic Clock Tower",
                lat = 6.9360,
                lng = 79.8436,
                radiusM = 25f,
                lightSignature = LightSignature(200f, 700f),
                clue = "Look skyward where the timekeeper chimes above the quad.",
                lore = "Erected in 1857 as a lighthouse and clock tower.",
                order = 1,
                motionType = "SWEEP"
            ),
            Checkpoint(
                id = "CP_OLD_PARLIAMENT",
                gameId = gameId,
                name = "Old Parliament Building",
                lat = 6.9328,
                lng = 79.8436,
                radiusM = 30f,
                lightSignature = LightSignature(150f, 600f),
                clue = "Where statesmen gathered beside the Indian Ocean breeze.",
                lore = "Neo-baroque architecture constructed in 1930.",
                order = 2,
                motionType = "SHAKE"
            ),
            Checkpoint(
                id = "CP_GALLE_FACE",
                gameId = gameId,
                name = "Galle Face Promenade",
                lat = 6.9248,
                lng = 79.8427,
                radiusM = 35f,
                lightSignature = LightSignature(300f, 1000f),
                clue = "Feel the coastal winds and sunset light resonance.",
                lore = "Originally laid out in 1859 by Governor Henry Ward.",
                order = 3,
                motionType = "TILT"
            )
        )
    }
}
