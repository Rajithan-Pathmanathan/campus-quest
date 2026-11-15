package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatorGamesUiState(
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val drafts: List<Game> get() = games.filter { it.status == GameStatus.DRAFT }
    val published: List<Game> get() = games.filter { it.status == GameStatus.PUBLISHED }
}

class CreatorGamesViewModel(
    private val repository: GameRepository,
    private val currentUserId: () -> String?
) : ViewModel() {

    private val _state = MutableStateFlow(CreatorGamesUiState())
    val state: StateFlow<CreatorGamesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.isLoading) return            // ignore repeated taps
        val creatorId = currentUserId()
        if (creatorId == null) {
            _state.update { it.copy(error = "Please sign in to see your games.") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val mine = repository.getCreatorGames(creatorId)
                _state.update { it.copy(games = mine, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Couldn't load your games. Check your connection and try again.")
                }
            }
        }
    }
}