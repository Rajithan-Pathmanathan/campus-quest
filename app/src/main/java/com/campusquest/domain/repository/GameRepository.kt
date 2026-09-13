package com.campusquest.domain.repository

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.PlayerProgress
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun getAvailableGames(): List<Game>

    suspend fun getGameDetails(gameId: String): Game?

    suspend fun createGame(game: Game): Result<Game>

    suspend fun createCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Checkpoint>

    suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Unit>

    suspend fun publishGame(gameId: String): Result<Unit>

    suspend fun joinGame(gameId: String): Result<Unit>

    suspend fun getGameCheckpoints(gameId: String): List<Checkpoint>

    suspend fun getFusionSignature(gameId: String, checkpointId: String): LightSignature

    suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit>

    fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>>

    fun observePlayerProgress(gameId: String, userId: String): Flow<PlayerProgress?>

    suspend fun syncPending()
}
