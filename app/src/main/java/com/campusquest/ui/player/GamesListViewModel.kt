package com.campusquest.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing player quest discovery, search query filtering, and status chip selections.
 */
class GamesListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GamesListUiState(isLoading = true))
    val uiState: StateFlow<GamesListUiState> = _uiState.asStateFlow()

    init {
        loadGames()
    }

    fun loadGames(initialGames: List<Game>? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val games = initialGames ?: getDemoSeedGames()

            _uiState.update { current ->
                val filtered = applyFilters(games, current.searchQuery, current.statusFilter)
                current.copy(
                    isLoading = false,
                    allGames = games,
                    filteredGames = filtered,
                    errorMessage = null
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val trimmed = query.trim()
            val filtered = applyFilters(current.allGames, trimmed, current.statusFilter)
            current.copy(
                searchQuery = trimmed,
                filteredGames = filtered
            )
        }
    }

    fun onStatusFilterChanged(status: GameStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(current.allGames, current.searchQuery, status)
            current.copy(
                statusFilter = status,
                filteredGames = filtered
            )
        }
    }

    fun refresh() {
        loadGames()
    }

    private fun applyFilters(
        games: List<Game>,
        query: String,
        status: GameStatus?
    ): List<Game> {
        return games.filter { game ->
            val matchesQuery = query.isBlank() ||
                    game.title.contains(query, ignoreCase = true) ||
                    game.description.contains(query, ignoreCase = true) ||
                    game.creatorName.contains(query, ignoreCase = true)

            val matchesStatus = status == null || game.status == status

            matchesQuery && matchesStatus
        }
    }

    private fun getDemoSeedGames(): List<Game> {
        return listOf(
            Game(
                id = "GAME_HERITAGE_001",
                title = "Colombo Heritage Trail",
                description = "Explore iconic colonial architecture, historical landmarks, and relics across the heart of Colombo.",
                creatorId = "CREATOR_PROF_ALAN",
                creatorName = "Prof. Alan Turing",
                status = GameStatus.PUBLISHED,
                checkpointCount = 5,
                publishedAt = System.currentTimeMillis() - 86400000L
            ),
            Game(
                id = "GAME_CAMPUS_INNOVATION_002",
                title = "Engineering Quad Cipher",
                description = "Decrypt sensor secrets hidden in labs, robotics arenas, and innovation hubs.",
                creatorId = "CREATOR_DR_GRACE",
                creatorName = "Dr. Grace Hopper",
                status = GameStatus.PUBLISHED,
                checkpointCount = 4,
                publishedAt = System.currentTimeMillis() - 43200000L
            ),
            Game(
                id = "GAME_BOTANICAL_003",
                title = "Peradeniya Flora Quest",
                description = "A botanical expedition identifying rare flora signatures and ambient canopy light resonance.",
                creatorId = "CREATOR_BOTANIST_SARAH",
                creatorName = "Sarah Connor",
                status = GameStatus.DRAFT,
                checkpointCount = 6,
                publishedAt = null
            )
        )
    }
}
