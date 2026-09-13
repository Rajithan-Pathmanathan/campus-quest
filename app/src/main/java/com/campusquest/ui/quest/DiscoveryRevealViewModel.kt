package com.campusquest.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel preparing and managing the Relic Discovery & Lore Reveal modal presentation.
 */
class DiscoveryRevealViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryRevealUiState(isLoading = true))
    val uiState: StateFlow<DiscoveryRevealUiState> = _uiState.asStateFlow()

    fun loadDiscovery(
        gameId: String,
        checkpointId: String,
        totalCheckpoints: Int = 3,
        customCheckpoint: Checkpoint? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cp = customCheckpoint ?: getSampleCheckpoint(gameId, checkpointId)
            val isFinal = cp.order >= totalCheckpoints
            val points = if (isFinal) 150 else 100

            _uiState.update {
                it.copy(
                    isLoading = false,
                    gameId = gameId,
                    checkpointId = checkpointId,
                    checkpointName = cp.name,
                    clue = cp.clue,
                    lore = cp.lore,
                    order = cp.order,
                    totalCheckpoints = totalCheckpoints,
                    pointsAwarded = points,
                    isFinalCheckpoint = isFinal
                )
            }
        }
    }

    private fun getSampleCheckpoint(gameId: String, checkpointId: String): Checkpoint {
        return Checkpoint(
            id = checkpointId,
            gameId = gameId,
            name = "Historic Clock Tower",
            lat = 6.9360,
            lng = 79.8436,
            radiusM = 25f,
            lightSignature = LightSignature(200f, 700f),
            clue = "Look skyward where the bell chimes above the quad.",
            lore = "Erected in 1857 as a navigational lighthouse and clock tower, this monument stood as the guardian of Colombo's maritime trade.",
            order = 1,
            motionType = "SWEEP"
        )
    }
}
