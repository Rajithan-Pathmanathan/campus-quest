package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckpointListUiState(
    val checkpoints: List<Checkpoint> = emptyList(),
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val error: String? = null
)

class CheckpointListViewModel(
    private val repository: GameRepository,
    private val gameId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CheckpointListUiState())
    val state: StateFlow<CheckpointListUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val list = repository.getGameCheckpoints(gameId).sortedBy { it.order }
                _state.update { it.copy(checkpoints = list, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Couldn't load checkpoints. Try again.") }
            }
        }
    }

    fun moveUp(checkpointId: String) = move(checkpointId, -1)
    fun moveDown(checkpointId: String) = move(checkpointId, +1)

    private fun move(checkpointId: String, delta: Int) {
        val s = _state.value
        if (s.isBusy) return
        val list = s.checkpoints.sortedBy { it.order }.toMutableList()
        val from = list.indexOfFirst { it.id == checkpointId }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        val moved = list.removeAt(from)
        list.add(to, moved)
        // Renumber 1..n so orders are always unique and consistent
        val changed = list.mapIndexedNotNull { index, cp ->
            if (cp.order != index + 1) cp.copy(order = index + 1) else null
        }
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            try {
                var failed = false
                for (cp in changed) {
                    if (repository.updateCheckpoint(gameId, cp).isFailure) failed = true
                }
                if (failed) _state.update { it.copy(error = "Couldn't reorder. Try again.") }
                val fresh = repository.getGameCheckpoints(gameId).sortedBy { it.order }
                _state.update { it.copy(checkpoints = fresh, isBusy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isBusy = false, error = "Couldn't reorder. Try again.") }
            }
        }
    }

    fun delete(checkpointId: String) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.deleteCheckpoint(gameId, checkpointId)
                if (result.isFailure) {
                    _state.update { it.copy(isBusy = false, error = "Couldn't delete checkpoint. Try again.") }
                    return@launch
                }
                val remaining = repository.getGameCheckpoints(gameId).sortedBy { it.order }
                // Re-number orders 1..n after deletion
                remaining.forEachIndexed { index, cp ->
                    if (cp.order != index + 1) {
                        repository.updateCheckpoint(gameId, cp.copy(order = index + 1))
                    }
                }
                val fresh = repository.getGameCheckpoints(gameId).sortedBy { it.order }
                _state.update { it.copy(checkpoints = fresh, isBusy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isBusy = false, error = "Couldn't delete checkpoint. Try again.") }
            }
        }
    }

    fun onErrorShown() {
        _state.update { it.copy(error = null) }
    }
}
