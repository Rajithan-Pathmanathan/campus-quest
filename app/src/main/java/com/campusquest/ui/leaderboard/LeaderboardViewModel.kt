package com.campusquest.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.GameLeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Quest Leaderboard rankings, sorting by score and completion time,
 * extracting podium champions (1st, 2nd, 3rd), and identifying player position.
 */
class LeaderboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    fun loadLeaderboard(
        gameId: String,
        customTitle: String? = null,
        customEntries: List<GameLeaderboardEntry>? = null,
        currentUserId: String = "u_current_player"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val title = customTitle ?: getSampleTitle(gameId)
            val rawEntries = customEntries ?: getSampleLeaderboardEntries(gameId)

            if (rawEntries.isNotEmpty()) {
                // Primary: Score descending, Secondary: Completion time ascending
                val sorted = rawEntries
                    .sortedWith(
                        compareByDescending<GameLeaderboardEntry> { it.score }
                            .thenBy { it.completionTimeSeconds }
                    )
                    .mapIndexed { index, entry ->
                        entry.copy(rank = index + 1)
                    }

                val podium = sorted.take(3)
                val remaining = if (sorted.size > 3) sorted.drop(3) else emptyList()
                val currentPlayer = sorted.find { it.userId == currentUserId }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameId = gameId,
                        gameTitle = title,
                        podiumEntries = podium,
                        rankedEntries = remaining,
                        currentPlayerEntry = currentPlayer,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameId = gameId,
                        gameTitle = title,
                        podiumEntries = emptyList(),
                        rankedEntries = emptyList(),
                        currentPlayerEntry = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun getSampleTitle(gameId: String): String {
        return when (gameId) {
            "GAME_HERITAGE_001" -> "Colombo Heritage Trail"
            "GAME_CAMPUS_INNOVATION_002" -> "Engineering Quad Cipher"
            else -> "Quest Leaderboard: $gameId"
        }
    }

    private fun getSampleLeaderboardEntries(gameId: String): List<GameLeaderboardEntry> {
        val now = System.currentTimeMillis()
        return listOf(
            GameLeaderboardEntry(
                userId = "u1",
                userName = "CyberSeeker_99",
                score = 650,
                completionTimeSeconds = 740, // 12m 20s
                completedAt = now - 3600000
            ),
            GameLeaderboardEntry(
                userId = "u2",
                userName = "Elena_Rostova",
                score = 500,
                completionTimeSeconds = 840, // 14m 00s
                completedAt = now - 7200000
            ),
            GameLeaderboardEntry(
                userId = "u3",
                userName = "Marcus_Vance",
                score = 500,
                completionTimeSeconds = 960, // 16m 00s (tied score, slower time)
                completedAt = now - 10800000
            ),
            GameLeaderboardEntry(
                userId = "u_current_player",
                userName = "Player_You",
                score = 420,
                completionTimeSeconds = 1120, // 18m 40s
                completedAt = now - 1800000
            ),
            GameLeaderboardEntry(
                userId = "u5",
                userName = "Dev_Priya",
                score = 380,
                completionTimeSeconds = 1250,
                completedAt = now - 14400000
            ),
            GameLeaderboardEntry(
                userId = "u6",
                userName = "Kai_Chen",
                score = 250,
                completionTimeSeconds = 1480,
                completedAt = now - 18000000
            )
        )
    }
}
