package com.campusquest.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.BuildConfig
import com.campusquest.data.mock.DebugSeeder
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.repository.GameRepository
import com.campusquest.ui.common.AppProviders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GamesUiState(
    val isLoading: Boolean = true,
    val games: List<Game> = emptyList(),
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && error == null && games.isEmpty()
}

class GamesViewModel(
    private val repository: GameRepository = AppProviders.gameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            _uiState.value = GamesUiState(isLoading = true)
            _uiState.value = try {
                if (BuildConfig.DEBUG) DebugSeeder.seedIfEmpty(repository)
                val games = repository.getAvailableGames().filter { it.status == GameStatus.PUBLISHED }
                GamesUiState(isLoading = false, games = games)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                GamesUiState(isLoading = false, error = e.message ?: "Could not load quests")
            }
        }
    }
}