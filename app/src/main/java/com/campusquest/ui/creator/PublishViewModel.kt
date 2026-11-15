package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.repository.GameRepository
import com.campusquest.ui.creator.validation.PublishValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PublishUiState {
    data object Idle : PublishUiState
    data object Checking : PublishUiState
    data class Invalid(val problems: List<String>) : PublishUiState
    data class ReadyToConfirm(val gameId: String) : PublishUiState
    data object Publishing : PublishUiState
    data object Success : PublishUiState
    data class Error(val message: String) : PublishUiState
}

class PublishViewModel(private val repository: GameRepository) : ViewModel() {

    private val _state = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val state: StateFlow<PublishUiState> = _state.asStateFlow()

    /** Step 1: validate the whole game. */
    fun requestPublish(gameId: String) {
        val current = _state.value
        if (current is PublishUiState.Checking || current is PublishUiState.Publishing) return
        _state.value = PublishUiState.Checking
        viewModelScope.launch {
            try {
                val game = repository.getGameDetails(gameId)
                val checkpoints = repository.getGameCheckpoints(gameId)
                val problems = PublishValidator.validate(game, checkpoints)
                _state.value = if (problems.isEmpty()) PublishUiState.ReadyToConfirm(gameId)
                else PublishUiState.Invalid(problems)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PublishUiState.Error("Couldn't check your game. Check your connection and try again.")
            }
        }
    }

    /** Step 2: only works after validation passed, so repeated taps cannot publish twice. */
    fun confirmPublish() {
        val s = _state.value
        if (s !is PublishUiState.ReadyToConfirm) return
        _state.value = PublishUiState.Publishing
        viewModelScope.launch {
            try {
                repository.publishGame(s.gameId)
                    .onSuccess { _state.value = PublishUiState.Success }
                    .onFailure { _state.value = PublishUiState.Error(friendly(it)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = PublishUiState.Error(friendly(e))
            }
        }
    }

    fun dismiss() {
        val s = _state.value
        if (s !is PublishUiState.Publishing && s !is PublishUiState.Checking) {
            _state.value = PublishUiState.Idle
        }
    }

    private fun friendly(t: Throwable): String {
        val m = t.message.orEmpty().lowercase()
        return when {
            "permission" in m || "denied" in m -> "You don't have permission to publish this game."
            "network" in m || "unavailable" in m || "offline" in m ->
                "No connection. Check your internet and try again."
            else -> "Couldn't publish. Please try again."
        }
    }
}