package com.campusquest.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.repository.GameRepository
import com.campusquest.ui.common.AppProviders
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val gameTitle: String = "",
    val entries: List<GameLeaderboardEntry> = emptyList(),
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && error == null && entries.isEmpty()
}

class LeaderboardViewModel(
    private val repository: GameRepository = AppProviders.gameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var loadedGameId: String? = null
    private var job: Job? = null

    fun load(gameId: String?, force: Boolean = false) {
        if (!force && !gameId.isNullOrBlank() && gameId == loadedGameId) return
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = LeaderboardUiState(isLoading = true)
            val game = try {
                if (gameId.isNullOrBlank()) {
                    repository.getAvailableGames().firstOrNull { it.status == GameStatus.PUBLISHED }
                } else {
                    repository.getGameDetails(gameId)
                }
            } catch (e: Exception) {
                _uiState.value = LeaderboardUiState(
                    isLoading = false,
                    error = e.message ?: "Could not load leaderboard"
                )
                return@launch
            }
            val selectedId = gameId?.takeIf { it.isNotBlank() } ?: game?.id
            if (selectedId == null) {
                _uiState.value = LeaderboardUiState(isLoading = false, gameTitle = "No published quests")
                return@launch
            }
            loadedGameId = selectedId
            val title = game?.title.orEmpty()
            repository.observeGameLeaderboard(selectedId)
                .catch { e ->
                    _uiState.value = LeaderboardUiState(
                        isLoading = false,
                        gameTitle = title,
                        error = e.message ?: "Could not load leaderboard"
                    )
                }
                .collect { entries ->
                    _uiState.value = LeaderboardUiState(
                        isLoading = false,
                        gameTitle = title,
                        entries = entries.sortedWith(
                            compareByDescending<GameLeaderboardEntry> { it.score }
                                .thenBy { it.completionTimeSeconds }
                        )
                    )
                }
        }
    }
}
