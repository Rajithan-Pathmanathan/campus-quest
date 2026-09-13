package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * ViewModel orchestrating the Quest Creation/Editing Wizard and the Checkpoint Sequence Builder.
 */
class CreateGameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGameUiState(isLoading = true))
    val uiState: StateFlow<CreateGameUiState> = _uiState.asStateFlow()

    fun loadQuest(
        gameId: String?,
        customTitle: String? = null,
        customDesc: String? = null,
        customCheckpoints: List<Checkpoint>? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val targetId = if (!gameId.isNullOrBlank()) gameId else "DRAFT_${System.currentTimeMillis()}"

            val initialTitle = customTitle ?: when (gameId) {
                "DRAFT_QUANTUM_LAB_003" -> "Quantum Physics Mystery Quest"
                "DRAFT_BOTANICAL_CRYPT_004" -> "Campus Botanical Arboreum"
                else -> if (!gameId.isNullOrBlank()) "Editing Quest: $gameId" else ""
            }

            val initialDesc = customDesc ?: when (gameId) {
                "DRAFT_QUANTUM_LAB_003" -> "Locate laser optics labs, particle physics simulators, and sensor resonance nodes."
                "DRAFT_BOTANICAL_CRYPT_004" -> "Decipher ecological flora markers and ambient light signatures in the botanical gardens."
                else -> if (!gameId.isNullOrBlank()) "Custom authored exploration quest." else ""
            }

            val initialCheckpoints = (customCheckpoints ?: getSampleDraftCheckpoints(targetId)).sortedBy { it.order }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    gameId = targetId,
                    title = initialTitle,
                    description = initialDesc,
                    checkpoints = initialCheckpoints,
                    isDraftSaved = false,
                    isPublished = false,
                    errorMessage = null
                )
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, errorMessage = null) }
    }

    fun updateDescription(newDesc: String) {
        _uiState.update { it.copy(description = newDesc, errorMessage = null) }
    }

    fun addOrUpdateCheckpoint(checkpoint: Checkpoint) {
        _uiState.update { current ->
            val list = current.checkpoints.toMutableList()
            val existingIndex = list.indexOfFirst { it.id == checkpoint.id }

            if (existingIndex >= 0) {
                list[existingIndex] = checkpoint
            } else {
                list.add(checkpoint)
            }

            // Re-index order cleanly 1..N
            val reIndexed = list.mapIndexed { idx, cp -> cp.copy(order = idx + 1) }
            current.copy(checkpoints = reIndexed, errorMessage = null)
        }
    }

    fun removeCheckpoint(checkpointId: String) {
        _uiState.update { current ->
            val filtered = current.checkpoints.filterNot { it.id == checkpointId }
            val reIndexed = filtered.mapIndexed { idx, cp -> cp.copy(order = idx + 1) }
            current.copy(checkpoints = reIndexed)
        }
    }

    fun moveCheckpointUp(index: Int) {
        _uiState.update { current ->
            if (index > 0 && index < current.checkpoints.size) {
                val list = current.checkpoints.toMutableList()
                Collections.swap(list, index, index - 1)
                val reIndexed = list.mapIndexed { idx, cp -> cp.copy(order = idx + 1) }
                current.copy(checkpoints = reIndexed)
            } else {
                current
            }
        }
    }

    fun moveCheckpointDown(index: Int) {
        _uiState.update { current ->
            if (index >= 0 && index < current.checkpoints.size - 1) {
                val list = current.checkpoints.toMutableList()
                Collections.swap(list, index, index + 1)
                val reIndexed = list.mapIndexed { idx, cp -> cp.copy(order = idx + 1) }
                current.copy(checkpoints = reIndexed)
            } else {
                current
            }
        }
    }

    fun saveDraft(): Boolean {
        _uiState.update { it.copy(isDraftSaved = true) }
        return true
    }

    fun publishQuest(): Boolean {
        val state = _uiState.value
        return if (state.canPublish) {
            _uiState.update { it.copy(isPublished = true, errorMessage = null) }
            true
        } else {
            _uiState.update { it.copy(errorMessage = state.validationMessage) }
            false
        }
    }

    private fun getSampleDraftCheckpoints(gameId: String): List<Checkpoint> {
        return if (gameId.startsWith("DRAFT_QUANTUM")) {
            listOf(
                Checkpoint(
                    id = "CP_OPTICS_LAB",
                    gameId = gameId,
                    name = "Laser Optics Lab",
                    lat = 6.9345,
                    lng = 79.8440,
                    radiusM = 20f,
                    lightSignature = LightSignature(400f, 900f),
                    clue = "Where coherent photons align in darkness.",
                    lore = "Established in 1998 for semiconductor optics research.",
                    order = 1,
                    motionType = "SWEEP"
                ),
                Checkpoint(
                    id = "CP_PARTICLE_ACCEL",
                    gameId = gameId,
                    name = "Subatomic Resonance Arena",
                    lat = 6.9350,
                    lng = 79.8450,
                    radiusM = 25f,
                    lightSignature = LightSignature(200f, 600f),
                    clue = "Listen for electromagnetic magnetic harmonic vibration.",
                    lore = "High voltage resonance simulation testbed.",
                    order = 2,
                    motionType = "SHAKE"
                )
            )
        } else {
            emptyList()
        }
    }
}
