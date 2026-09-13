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

/**
 * ViewModel orchestrating Checkpoint editing, geospatial parameters, ambient light presets,
 * and physical motion gesture configurations.
 */
class CheckpointEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CheckpointEditorUiState(isLoading = true))
    val uiState: StateFlow<CheckpointEditorUiState> = _uiState.asStateFlow()

    fun loadCheckpoint(
        gameId: String,
        checkpointId: String?,
        customCheckpoint: Checkpoint? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val existing = customCheckpoint ?: if (!checkpointId.isNullOrBlank()) {
                getSampleCheckpoint(gameId, checkpointId)
            } else {
                null
            }

            if (existing != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameId = gameId,
                        checkpointId = existing.id,
                        order = existing.order,
                        name = existing.name,
                        clue = existing.clue,
                        lore = existing.lore,
                        lat = existing.lat,
                        lng = existing.lng,
                        radiusM = existing.radiusM,
                        minLux = existing.lightSignature.minLux,
                        maxLux = existing.lightSignature.maxLux,
                        motionType = existing.motionType,
                        isSaved = false,
                        errorMessage = null
                    )
                }
            } else {
                // New Checkpoint Defaults
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameId = gameId,
                        checkpointId = null,
                        order = 1,
                        name = "",
                        clue = "",
                        lore = "",
                        lat = 6.9360,
                        lng = 79.8436,
                        radiusM = 25f,
                        minLux = 100f,
                        maxLux = 500f,
                        selectedPreset = LightPreset.INDOOR_LAB,
                        motionType = "SWEEP",
                        isSaved = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun updateIdentity(name: String, clue: String, lore: String) {
        _uiState.update {
            it.copy(name = name, clue = clue, lore = lore, errorMessage = null)
        }
    }

    fun updateCoordinates(lat: Double, lng: Double, radiusM: Float) {
        _uiState.update {
            it.copy(lat = lat, lng = lng, radiusM = radiusM, errorMessage = null)
        }
    }

    fun applyLightPreset(preset: LightPreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                minLux = preset.minLux,
                maxLux = preset.maxLux,
                errorMessage = null
            )
        }
    }

    fun updateCustomLight(minLux: Float, maxLux: Float) {
        _uiState.update {
            it.copy(
                selectedPreset = LightPreset.CUSTOM,
                minLux = minLux,
                maxLux = maxLux,
                errorMessage = null
            )
        }
    }

    fun updateMotionType(motionType: String) {
        _uiState.update {
            it.copy(motionType = motionType, errorMessage = null)
        }
    }

    fun saveCheckpoint(): Checkpoint? {
        val state = _uiState.value
        return if (state.isValid) {
            _uiState.update { it.copy(isSaved = true, errorMessage = null) }
            state.toCheckpoint()
        } else {
            _uiState.update { it.copy(errorMessage = state.validationMessage) }
            null
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
            clue = "Look skyward where the timekeeper chimes above the quad.",
            lore = "Erected in 1857 as a lighthouse and clock tower.",
            order = 1,
            motionType = "SWEEP"
        )
    }
}
