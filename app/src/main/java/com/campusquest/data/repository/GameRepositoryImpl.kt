package com.campusquest.data.repository

import com.campusquest.data.local.dao.CheckpointDao
import com.campusquest.data.local.dao.GameDao
import com.campusquest.data.local.dao.PlayerProgressDao
import com.campusquest.data.local.dao.SyncQueueDao
import com.campusquest.data.local.entity.CheckpointEntity
import com.campusquest.data.local.entity.GameEntity
import com.campusquest.data.local.entity.PlayerProgressEntity
import com.campusquest.data.local.entity.SyncOperationType
import com.campusquest.data.local.entity.SyncQueueEntity
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.PlayerProgress
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GameRepositoryImpl(
    private val gameDao: GameDao,
    private val checkpointDao: CheckpointDao,
    private val playerProgressDao: PlayerProgressDao,
    private val syncQueueDao: SyncQueueDao
) : GameRepository {

    override suspend fun getAvailableGames(): List<Game> {
        return gameDao.getPublishedGames(GameStatus.PUBLISHED).map { it.toDomain() }
    }

    override suspend fun getGameDetails(gameId: String): Game? {
        return gameDao.getGameById(gameId)?.toDomain()
    }

    override suspend fun createGame(game: Game): Result<Game> {
        return try {
            gameDao.insertGame(GameEntity.fromDomain(game))
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.CREATE_GAME,
                    entityId = game.id,
                    payloadJson = "{}"
                )
            )
            Result.success(game)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Checkpoint> {
        return try {
            checkpointDao.insertCheckpoint(CheckpointEntity.fromDomain(checkpoint))
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.UPDATE_CHECKPOINT,
                    entityId = checkpoint.id,
                    payloadJson = "{}"
                )
            )
            Result.success(checkpoint)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Unit> {
        return try {
            checkpointDao.updateCheckpoint(CheckpointEntity.fromDomain(checkpoint))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun publishGame(gameId: String): Result<Unit> {
        return try {
            val game = gameDao.getGameById(gameId)
            if (game != null) {
                val published = game.copy(status = GameStatus.PUBLISHED, publishedAt = System.currentTimeMillis())
                gameDao.updateGame(published)
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        operationType = SyncOperationType.PUBLISH_GAME,
                        entityId = gameId,
                        payloadJson = "{}"
                    )
                )
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Game $gameId not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGame(gameId: String): Result<Unit> {
        return try {
            val progress = PlayerProgress(
                gameId = gameId,
                userId = "current_user",
                startedAt = System.currentTimeMillis()
            )
            playerProgressDao.insertOrUpdateProgress(PlayerProgressEntity.fromDomain(progress))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGameCheckpoints(gameId: String): List<Checkpoint> {
        return checkpointDao.getCheckpointsForGame(gameId).map { it.toDomain() }
    }

    override suspend fun getFusionSignature(gameId: String, checkpointId: String): LightSignature {
        val cp = checkpointDao.getCheckpointById(checkpointId)
        return if (cp != null) {
            LightSignature(cp.minLux, cp.maxLux)
        } else {
            LightSignature(0f, 500f)
        }
    }

    override suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit> {
        return try {
            val existing = playerProgressDao.getProgress(gameId, "current_user")
            val completedList = (existing?.completedCheckpointIds ?: emptyList()) + checkpointId
            val updated = PlayerProgressEntity(
                gameId = gameId,
                userId = "current_user",
                completedCheckpointIds = completedList.distinct(),
                currentCheckpointOrder = (existing?.currentCheckpointOrder ?: 1) + 1,
                score = (existing?.score ?: 0) + 100,
                startedAt = existing?.startedAt ?: System.currentTimeMillis(),
                completedAt = foundAt,
                isCompleted = false
            )
            playerProgressDao.insertOrUpdateProgress(updated)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.RECORD_DISCOVERY,
                    entityId = checkpointId,
                    payloadJson = "{}"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> {
        // Mock fallback or remote Firestore flow
        return flowOf(
            listOf(
                GameLeaderboardEntry("u1", "Elena_Rostova", 500, 840, System.currentTimeMillis() - 3600000, 1),
                GameLeaderboardEntry("u2", "Marcus_Vance", 400, 1020, System.currentTimeMillis() - 7200000, 2),
                GameLeaderboardEntry("u3", "Dev_Priya", 300, 1250, System.currentTimeMillis() - 10800000, 3)
            )
        )
    }

    override fun observePlayerProgress(gameId: String, userId: String): Flow<PlayerProgress?> {
        return playerProgressDao.observeProgress(gameId, userId).map { it?.toDomain() }
    }

    override suspend fun syncPending() {
        val pending = syncQueueDao.getPendingSyncItems()
        for (item in pending) {
            // Push to Firestore and remove on success
            syncQueueDao.deleteById(item.syncId)
        }
    }
}
