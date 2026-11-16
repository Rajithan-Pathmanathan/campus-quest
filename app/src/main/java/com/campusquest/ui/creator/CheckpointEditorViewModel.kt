package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.CheckpointActivity
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.repository.GameRepository
import com.campusquest.ui.creator.validation.CheckpointValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CheckpointEditorUiState(
    val checkpointId: String? = null,
    val gameId: String,
    val name: String = "",
    val lat: String = "",
    val lng: String = "",
    val radiusM: String = "20",
    val minLux: String = "0",
    val maxLux: String = "1000",
    val clue: String = "",
    val lore: String = "",
    val order: String = "1",
    val motionType: String = CheckpointActivity.TILT_MAZE.name,
    val rarity: String = "COMMON",
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val saveError: String? = null,
    val saved: Boolean = false
)

class CheckpointEditorViewModel(
    private val repository: GameRepository,
    gameId: String,
    private val editingCheckpointId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(CheckpointEditorUiState(gameId = gameId, checkpointId = editingCheckpointId))
    val state: StateFlow<CheckpointEditorUiState> = _state.asStateFlow()

    init {
        if (editingCheckpointId != null) loadExisting(gameId, editingCheckpointId)
    }

    private fun loadExisting(gameId: String, checkpointId: String) {
        viewModelScope.launch {
            try {
                val cp = repository.getGameCheckpoints(gameId).firstOrNull { it.id == checkpointId }
                if (cp == null) {
                    _state.update { it.copy(saveError = "Checkpoint not found.") }
                } else {
                    _state.update {
                        it.copy(
                            name = cp.name, lat = cp.lat.toString(), lng = cp.lng.toString(),
                            radiusM = cp.radiusM.toString(),
                            minLux = cp.lightSignature.minLux.toString(),
                            maxLux = cp.lightSignature.maxLux.toString(),
                            clue = cp.clue, lore = cp.lore, order = cp.order.toString(),
                            motionType = CheckpointActivity.fromStored(cp.motionType, cp.order).name, rarity = cp.rarity
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saveError = "Couldn't load the checkpoint.") }
            }
        }
    }

    fun update(transform: (CheckpointEditorUiState) -> CheckpointEditorUiState) {
        _state.update { transform(it).copy(errors = emptyMap()) }
    }

    /** Returns field-name -> error message. Empty map means valid. */
    private fun validate(s: CheckpointEditorUiState): Map<String, String> {
        val e = mutableMapOf<String, String>()
        CheckpointValidator.validateName(s.name)?.let { e["name"] = it }
        CheckpointValidator.validateLatitude(s.lat)?.let { e["lat"] = it }
        CheckpointValidator.validateLongitude(s.lng)?.let { e["lng"] = it }
        CheckpointValidator.validateRadius(s.radiusM)?.let { e["radius"] = it }
        CheckpointValidator.validateLux(s.minLux, s.maxLux)?.let { e["lux"] = it }
        CheckpointValidator.validateClue(s.clue, forPublish = false)?.let { e["clue"] = it }
        CheckpointValidator.validateActivity(s.motionType)?.let { e["activity"] = it }
        CheckpointValidator.validateRarity(s.rarity)?.let { e["rarity"] = it }
        val order = s.order.trim().toIntOrNull()
        if (order == null || order < 1) e["order"] = "Order must be a whole number from 1"
        return e
    }

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                // Safe now: everything was validated, so conversions and LightSignature cannot fail.
                val checkpoint = Checkpoint(
                    id = s.checkpointId ?: UUID.randomUUID().toString(),
                    gameId = s.gameId,
                    name = s.name.trim(),
                    lat = s.lat.trim().toDouble(),
                    lng = s.lng.trim().toDouble(),
                    radiusM = s.radiusM.trim().toFloat(),
                    lightSignature = LightSignature(s.minLux.trim().toFloat(), s.maxLux.trim().toFloat()),
                    clue = s.clue.trim(),
                    lore = s.lore.trim(),
                    order = s.order.trim().toInt(),
                    motionType = s.motionType,
                    rarity = s.rarity
                )
                val result = if (s.checkpointId == null) {
                    repository.createCheckpoint(s.gameId, checkpoint).map { }
                } else {
                    repository.updateCheckpoint(s.gameId, checkpoint)
                }
                result
                    .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                    .onFailure {
                        _state.update { it.copy(isSaving = false, saveError = "Unable to save checkpoint. Check your connection and try again.") }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = "Unable to save checkpoint. Check your connection and try again.") }
            }
        }
    }
}
