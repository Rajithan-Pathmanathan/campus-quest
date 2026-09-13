package com.campusquest.ui.player

import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus

/**
 * UI State for the Games Explorer / Discovery screen.
 */
data class GamesListUiState(
    val isLoading: Boolean = false,
    val allGames: List<Game> = emptyList(),
    val filteredGames: List<Game> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: GameStatus? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && filteredGames.isEmpty()
}
