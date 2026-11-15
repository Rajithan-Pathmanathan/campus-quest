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
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GameRepositoryImpl(
    private val gameDao: GameDao,
    private val checkpointDao: CheckpointDao,
    private val playerProgressDao: PlayerProgressDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository? = null,
    private val remoteSyncHandler: (suspend (SyncQueueEntity) -> Boolean)? = null
) : GameRepository {

    private fun currentUserId(): String {
        return authRepository?.getCurrentUserId()?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCAL_USER_ID
    }

    override suspend fun getAvailableGames(): List<Game> {
        return gameDao.getPublishedGames(GameStatus.PUBLISHED).map { it.toDomain() }
    }

    override suspend fun getCreatorGames(creatorId: String): List<Game> {
        return gameDao.getCreatorGames(creatorId).map { it.toDomain() }
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
                    payloadJson = """{"gameId":"${game.id}","creatorId":"${game.creatorId}"}"""
                )
            )
            Result.success(game)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGame(game: Game): Result<Unit> {
        return try {
            gameDao.updateGame(GameEntity.fromDomain(game))
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.CREATE_GAME,
                    entityId = game.id,
                    payloadJson = """{"gameId":"${game.id}","creatorId":"${game.creatorId}"}"""
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Checkpoint> {
        return try {
            val scopedCheckpoint = checkpoint.copy(gameId = gameId)
            checkpointDao.insertCheckpoint(CheckpointEntity.fromDomain(scopedCheckpoint))
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.CREATE_CHECKPOINT,
                    entityId = scopedCheckpoint.id,
                    payloadJson = """{"gameId":"$gameId","checkpointId":"${scopedCheckpoint.id}"}"""
                )
            )
            Result.success(scopedCheckpoint)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Unit> {
        return try {
            val scopedCheckpoint = checkpoint.copy(gameId = gameId)
            checkpointDao.updateCheckpoint(CheckpointEntity.fromDomain(scopedCheckpoint))
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.UPDATE_CHECKPOINT,
                    entityId = scopedCheckpoint.id,
                    payloadJson = """{"gameId":"$gameId","checkpointId":"${scopedCheckpoint.id}"}"""
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCheckpoint(gameId: String, checkpointId: String): Result<Unit> {
        return try {
            checkpointDao.deleteCheckpoint(checkpointId)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.DELETE_CHECKPOINT,
                    entityId = checkpointId,
                    payloadJson = """{"gameId":"$gameId","checkpointId":"$checkpointId"}"""
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun publishGame(gameId: String): Result<Unit> {
        return try {
            val game = gameDao.getGameById(gameId)
            if (game != null) {
                val checkpoints = checkpointDao.getCheckpointsForGame(gameId)
                val publishedAt = System.currentTimeMillis()
                val published = game.copy(
                    status = GameStatus.PUBLISHED,
                    checkpointCount = if (checkpoints.isNotEmpty()) checkpoints.size else game.checkpointCount,
                    publishedAt = publishedAt
                )
                gameDao.updateGame(published)
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        operationType = SyncOperationType.PUBLISH_GAME,
                        entityId = gameId,
                        payloadJson = """{"gameId":"$gameId","publishedAt":$publishedAt}"""
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
            val userId = currentUserId()
            val existing = playerProgressDao.getProgress(gameId, userId)
            if (existing == null) {
                val joinedAt = System.currentTimeMillis()
                val progress = PlayerProgress(
                    gameId = gameId,
                    userId = userId,
                    startedAt = joinedAt
                )
                playerProgressDao.insertOrUpdateProgress(PlayerProgressEntity.fromDomain(progress))
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        operationType = SyncOperationType.JOIN_GAME,
                        entityId = "${gameId}_$userId",
                        payloadJson = """{"gameId":"$gameId","userId":"$userId","joinedAt":$joinedAt}"""
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGameCheckpoints(gameId: String): List<Checkpoint> {
        return checkpointDao.getCheckpointsForGame(gameId).map { it.toDomain() }
    }

    override suspend fun getFusionSignature(gameId: String, checkpointId: String): LightSignature {
        val cp = checkpointDao.getCheckpointsForGame(gameId).firstOrNull { it.id == checkpointId }
            ?: checkpointDao.getCheckpointById(checkpointId)
        return if (cp != null) {
            LightSignature(cp.minLux, cp.maxLux)
        } else {
            LightSignature(0f, 500f)
        }
    }

    override suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit> {
        return try {
            val userId = currentUserId()
            val existing = playerProgressDao.getProgress(gameId, userId)
            val currentCompleted = existing?.completedCheckpointIds ?: emptyList()

            // Task B2: Idempotent discovery — do not add points or advance order if already found
            if (checkpointId in currentCompleted) {
                return Result.success(Unit)
            }

            val updatedCompletedList = currentCompleted + checkpointId
            val totalCheckpoints = checkpointDao.getCheckpointsForGame(gameId).size
                .takeIf { it > 0 }
                ?: gameDao.getGameById(gameId)?.checkpointCount
                ?: 0

            // Task B3: Set isCompleted = true and completedAt only when the final checkpoint is found
            val isNowCompleted = totalCheckpoints > 0 && updatedCompletedList.size >= totalCheckpoints
            val finalCompletedAt = when {
                existing?.isCompleted == true -> existing.completedAt
                isNowCompleted -> foundAt
                else -> existing?.completedAt
            }

            val updated = PlayerProgressEntity(
                gameId = gameId,
                userId = userId,
                completedCheckpointIds = updatedCompletedList,
                currentCheckpointOrder = (existing?.currentCheckpointOrder ?: 1) + 1,
                score = (existing?.score ?: 0) + 100,
                startedAt = existing?.startedAt ?: foundAt,
                completedAt = finalCompletedAt,
                isCompleted = existing?.isCompleted == true || isNowCompleted
            )
            playerProgressDao.insertOrUpdateProgress(updated)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.RECORD_DISCOVERY,
                    entityId = checkpointId,
                    payloadJson = """{"gameId":"$gameId","checkpointId":"$checkpointId","userId":"$userId","foundAt":$foundAt}"""
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> {
        // Mock fallback until M5 Firestore leaderboard listener is connected
        return flowOf(
            listOf(
                GameLeaderboardEntry("u1", "Elena_Rostova", 500, 840, System.currentTimeMillis() - 3600000, 1),
                GameLeaderboardEntry("u2", "Marcus_Vance", 400, 1020, System.currentTimeMillis() - 7200000, 2),
                GameLeaderboardEntry("u3", "Dev_Priya", 300, 1250, System.currentTimeMillis() - 10800000, 3)
            )
        )
    }

    override fun observePlayerProgress(gameId: String, userId: String): Flow<PlayerProgress?> {
        val resolvedUserId = if (userId == "current_user" || userId.isBlank()) currentUserId() else userId
        return playerProgressDao.observeProgress(gameId, resolvedUserId).map { it?.toDomain() }
    }

    override suspend fun getAllPlayerProgress(userId: String): List<PlayerProgress> {
        val resolvedUserId = if (userId == "current_user" || userId.isBlank()) currentUserId() else userId
        return playerProgressDao.getAllPlayerProgress(resolvedUserId).map { it.toDomain() }
    }

    override suspend fun syncPending() {
        val handler = remoteSyncHandler ?: return
        val pending = syncQueueDao.getPendingSyncItems()
        for (item in pending) {
            val uploaded = try {
                handler(item)
            } catch (_: Exception) {
                false
            }
            if (uploaded) {
                syncQueueDao.deleteById(item.syncId)
            } else {
                syncQueueDao.incrementRetryCount(item.syncId)
            }
        }
    }

    companion object {
        const val DEFAULT_LOCAL_USER_ID = "local_offline_user"
    }
}
