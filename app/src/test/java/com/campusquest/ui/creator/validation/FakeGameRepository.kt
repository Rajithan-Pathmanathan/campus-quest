package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.PlayerProgress
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeGameRepository : GameRepository {
    val games = mutableListOf<Game>()
    val checkpoints = mutableListOf<Checkpoint>()
    var failLoading = false
    var publishCalls = 0

    override suspend fun getAvailableGames(): List<Game> {
        if (failLoading) throw RuntimeException("boom")
        return games.toList()
    }

    override suspend fun getCreatorGames(creatorId: String): List<Game> {
        if (failLoading) throw RuntimeException("boom")
        return games.filter { it.creatorId == creatorId }
    }

    override suspend fun getGameDetails(gameId: String) = games.firstOrNull { it.id == gameId }

    override suspend fun createGame(game: Game): Result<Game> {
        games.add(game)
        return Result.success(game)
    }

    override suspend fun updateGame(game: Game): Result<Unit> {
        val i = games.indexOfFirst { it.id == game.id }
        if (i >= 0) games[i] = game
        return Result.success(Unit)
    }

    override suspend fun createCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Checkpoint> {
        val saved = checkpoint.copy(gameId = gameId)
        checkpoints.add(saved)
        return Result.success(saved)
    }

    override suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Unit> {
        val i = checkpoints.indexOfFirst { it.id == checkpoint.id && it.gameId == gameId }
        if (i >= 0) checkpoints[i] = checkpoint
        return Result.success(Unit)
    }

    override suspend fun deleteCheckpoint(gameId: String, checkpointId: String): Result<Unit> {
        checkpoints.removeAll { it.gameId == gameId && it.id == checkpointId }
        return Result.success(Unit)
    }

    override suspend fun publishGame(gameId: String): Result<Unit> {
        publishCalls++
        val i = games.indexOfFirst { it.id == gameId }
        if (i >= 0) games[i] = games[i].copy(status = GameStatus.PUBLISHED, publishedAt = System.currentTimeMillis())
        return Result.success(Unit)
    }

    override suspend fun joinGame(gameId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getGameCheckpoints(gameId: String) =
        checkpoints.filter { it.gameId == gameId }.sortedBy { it.order }

    override suspend fun getFusionSignature(gameId: String, checkpointId: String): LightSignature =
        checkpoints.firstOrNull { it.gameId == gameId && it.id == checkpointId }?.lightSignature
            ?: LightSignature(0f, 1000f)

    override suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit> =
        Result.success(Unit)

    override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> = flowOf(emptyList())

    override fun observePlayerProgress(gameId: String, userId: String): Flow<PlayerProgress?> = flowOf(null)

    override suspend fun getAllPlayerProgress(userId: String): List<PlayerProgress> = emptyList()

    override suspend fun syncPending() {}
}
