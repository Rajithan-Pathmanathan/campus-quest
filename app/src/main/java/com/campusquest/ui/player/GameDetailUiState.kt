package com.campusquest.ui.player

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game

/**
 * UI State for the Game / Quest Details screen.
 */
data class GameDetailUiState(
    val isLoading: Boolean = false,
    val game: Game? = null,
    val checkpoints: List<Checkpoint> = emptyList(),
    val isJoined: Boolean = false,
    val errorMessage: String? = null
)
