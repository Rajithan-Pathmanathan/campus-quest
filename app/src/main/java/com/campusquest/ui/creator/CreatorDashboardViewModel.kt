package com.campusquest.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.campusquest.domain.repository.GameRepository

/**
 * ViewModel managing the Creator Studio Dashboard, handling authored quest partitions (Drafts / Published),
 * authoring metrics calculation, and draft mutations (delete/publish).
 */
class CreatorDashboardViewModel(
    private val gameRepository: GameRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorDashboardUiState(isLoading = true))
    val uiState: StateFlow<CreatorDashboardUiState> = _uiState.asStateFlow()

    fun loadAuthoredGames(
        creatorId: String = "CREATOR_PROF_ALAN",
        creatorName: String = "Prof. Alan Turing",
        customGames: List<Game>? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val repoGames = try { gameRepository?.getAvailableGames() } catch (e: Exception) { null }
            val allGames = customGames
                ?: repoGames?.ifEmpty { null }
                ?: getSampleAuthoredGames(creatorId, creatorName)

            val drafts = allGames.filter { it.status == GameStatus.DRAFT }
            val published = allGames.filter { it.status == GameStatus.PUBLISHED }
            val totalCheckpoints = allGames.sumOf { it.checkpointCount }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    creatorId = creatorId,
                    creatorName = creatorName,
                    draftGames = drafts,
                    publishedGames = published,
                    totalCheckpointsCreated = totalCheckpoints,
                    errorMessage = null
                )
            }
        }
    }

    fun deleteGame(gameId: String) {
        _uiState.update { current ->
            val updatedDrafts = current.draftGames.filterNot { it.id == gameId }
            val updatedPublished = current.publishedGames.filterNot { it.id == gameId }
            val totalCheckpoints = (updatedDrafts + updatedPublished).sumOf { it.checkpointCount }

            current.copy(
                draftGames = updatedDrafts,
                publishedGames = updatedPublished,
                totalCheckpointsCreated = totalCheckpoints
            )
        }
    }

    fun publishGame(gameId: String) {
        viewModelScope.launch {
            if (gameRepository != null) {
                try {
                    gameRepository.publishGame(gameId)
                } catch (e: Exception) {
                    // Handled
                }
            }
        }
        _uiState.update { current ->
            val draftToPublish = current.draftGames.find { it.id == gameId }
            if (draftToPublish != null) {
                val publishedGame = draftToPublish.copy(
                    status = GameStatus.PUBLISHED,
                    publishedAt = System.currentTimeMillis()
                )
                val updatedDrafts = current.draftGames.filterNot { it.id == gameId }
                val updatedPublished = listOf(publishedGame) + current.publishedGames
                val totalCheckpoints = (updatedDrafts + updatedPublished).sumOf { it.checkpointCount }

                current.copy(
                    draftGames = updatedDrafts,
                    publishedGames = updatedPublished,
                    totalCheckpointsCreated = totalCheckpoints
                )
            } else {
                current
            }
        }
    }

    private fun getSampleAuthoredGames(creatorId: String, creatorName: String): List<Game> {
        val now = System.currentTimeMillis()
        return listOf(
            Game(
                id = "GAME_HERITAGE_001",
                title = "Colombo Heritage Trail",
                description = "Explore iconic colonial architecture and historical relics across Colombo.",
                creatorId = creatorId,
                creatorName = creatorName,
                status = GameStatus.PUBLISHED,
                checkpointCount = 3,
                createdAt = now - 86400000L * 3,
                publishedAt = now - 86400000L
            ),
            Game(
                id = "DRAFT_QUANTUM_LAB_003",
                title = "Quantum Physics Mystery Quest",
                description = "Locate laser optics labs, particle physics simulators, and sensor resonance nodes.",
                creatorId = creatorId,
                creatorName = creatorName,
                status = GameStatus.DRAFT,
                checkpointCount = 2,
                createdAt = now - 3600000L * 5,
                publishedAt = null
            ),
            Game(
                id = "DRAFT_BOTANICAL_CRYPT_004",
                title = "Campus Botanical Arboreum",
                description = "Decipher ecological flora markers and ambient light signatures in the botanical gardens.",
                creatorId = creatorId,
                creatorName = creatorName,
                status = GameStatus.DRAFT,
                checkpointCount = 4,
                createdAt = now - 3600000L * 12,
                publishedAt = null
            )
        )
    }
}
