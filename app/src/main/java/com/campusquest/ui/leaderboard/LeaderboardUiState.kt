package com.campusquest.ui.leaderboard

import com.campusquest.domain.model.GameLeaderboardEntry

/**
 * UI State for the Game Leaderboard screen, separating top 3 podium entries from ranked table rows.
 */
data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val gameId: String? = null,
    val gameTitle: String = "",
    val podiumEntries: List<GameLeaderboardEntry> = emptyList(),
    val rankedEntries: List<GameLeaderboardEntry> = emptyList(),
    val currentPlayerEntry: GameLeaderboardEntry? = null,
    val errorMessage: String? = null
) {
    val totalEntries: Int
        get() = podiumEntries.size + rankedEntries.size

    val firstPlace: GameLeaderboardEntry?
        get() = podiumEntries.getOrNull(0)

    val secondPlace: GameLeaderboardEntry?
        get() = podiumEntries.getOrNull(1)

    val thirdPlace: GameLeaderboardEntry?
        get() = podiumEntries.getOrNull(2)
}
