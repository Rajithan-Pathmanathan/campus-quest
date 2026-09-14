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
import com.campusquest.data.remote.FirestoreService
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
    private val firestoreService: FirestoreService? = null,
    private val authRepository: AuthRepository? = null
) : GameRepository {

    override suspend fun getAvailableGames(): List<Game> {
        // Read local cache first
        val localGames = gameDao.getPublishedGames(GameStatus.PUBLISHED).map { it.toDomain() }

        // Fetch remote Firestore games if service is available
        if (firestoreService != null) {
            val remoteResult = firestoreService.fetchPublishedGames()
            if (remoteResult.isSuccess) {
                val remoteGames = remoteResult.getOrNull().orEmpty()
                if (remoteGames.isNotEmpty()) {
                    gameDao.insertGames(remoteGames.map { GameEntity.fromDomain(it) })
                    return remoteGames
                }
            }
        }
        return localGames
    }

    override suspend fun getGameDetails(gameId: String): Game? {
        val localGame = gameDao.getGameById(gameId)?.toDomain()
        if (localGame != null) {
            return localGame
        }

        if (firestoreService != null) {
            val remoteResult = firestoreService.fetchGameById(gameId)
            if (remoteResult.isSuccess) {
                val remoteGame = remoteResult.getOrNull()
                if (remoteGame != null) {
                    gameDao.insertGame(GameEntity.fromDomain(remoteGame))
                    return remoteGame
                }
            }
        }
        return null
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
            // Attempt optimistic sync if service is available
            syncPending()
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
            syncPending()
            Result.success(checkpoint)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Unit> {
        return try {
            checkpointDao.updateCheckpoint(CheckpointEntity.fromDomain(checkpoint))
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    operationType = SyncOperationType.UPDATE_CHECKPOINT,
                    entityId = checkpoint.id,
                    payloadJson = "{}"
                )
            )
            syncPending()
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
                syncPending()
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Game $gameId not found in local database"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGame(gameId: String): Result<Unit> {
        return try {
            val currentUid = authRepository?.getCurrentUserId() ?: "current_user"
            val progress = PlayerProgress(
                gameId = gameId,
                userId = currentUid,
                startedAt = System.currentTimeMillis()
            )
            playerProgressDao.insertOrUpdateProgress(PlayerProgressEntity.fromDomain(progress))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGameCheckpoints(gameId: String): List<Checkpoint> {
        val localCheckpoints = checkpointDao.getCheckpointsForGame(gameId).map { it.toDomain() }
        if (localCheckpoints.isNotEmpty()) {
            return localCheckpoints
        }

        if (firestoreService != null) {
            val remoteResult = firestoreService.fetchGameCheckpoints(gameId)
            if (remoteResult.isSuccess) {
                val remoteCheckpoints = remoteResult.getOrNull().orEmpty()
                if (remoteCheckpoints.isNotEmpty()) {
                    checkpointDao.insertCheckpoints(remoteCheckpoints.map { CheckpointEntity.fromDomain(it) })
                    return remoteCheckpoints
                }
            }
        }
        return localCheckpoints
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
            val currentUid = authRepository?.getCurrentUserId() ?: "current_user"
            val existing = playerProgressDao.getProgress(gameId, currentUid)
            val completedList = (existing?.completedCheckpointIds ?: emptyList()) + checkpointId
            val updated = PlayerProgressEntity(
                gameId = gameId,
                userId = currentUid,
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
            syncPending()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> {
        if (firestoreService != null) {
            return firestoreService.observeLeaderboard(gameId)
        }
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
        if (firestoreService == null) return
        val pending = syncQueueDao.getPendingSyncItems()
        for (item in pending) {
            try {
                when (item.operationType) {
                    SyncOperationType.CREATE_GAME,
                    SyncOperationType.PUBLISH_GAME -> {
                        val game = gameDao.getGameById(item.entityId)?.toDomain()
                        if (game != null) {
                            val checkpoints = checkpointDao.getCheckpointsForGame(item.entityId).map { it.toDomain() }
                            val res = firestoreService.publishGame(game, checkpoints)
                            if (res.isSuccess) {
                                syncQueueDao.deleteById(item.syncId)
                            }
                        } else {
                            syncQueueDao.deleteById(item.syncId)
                        }
                    }
                    SyncOperationType.RECORD_DISCOVERY,
                    SyncOperationType.UPDATE_PROGRESS -> {
                        val currentUid = authRepository?.getCurrentUserId() ?: "current_user"
                        // Find game that contains this checkpoint
                        val checkpoint = checkpointDao.getCheckpointById(item.entityId)
                        if (checkpoint != null) {
                            val progress = playerProgressDao.getProgress(checkpoint.gameId, currentUid)?.toDomain()
                            if (progress != null) {
                                val res = firestoreService.savePlayerProgress(progress)
                                if (res.isSuccess) {
                                    syncQueueDao.deleteById(item.syncId)
                                }
                            }
                        } else {
                            syncQueueDao.deleteById(item.syncId)
                        }
                    }
                    SyncOperationType.UPDATE_CHECKPOINT -> {
                        val checkpoint = checkpointDao.getCheckpointById(item.entityId)?.toDomain()
                        if (checkpoint != null) {
                            val game = gameDao.getGameById(checkpoint.gameId)?.toDomain()
                            if (game != null) {
                                val checkpoints = checkpointDao.getCheckpointsForGame(checkpoint.gameId).map { it.toDomain() }
                                firestoreService.publishGame(game, checkpoints)
                            }
                        }
                        syncQueueDao.deleteById(item.syncId)
                    }
                }
            } catch (e: Exception) {
                // If remote sync fails due to network, leave item in sync_queue for WorkManager to retry
            }
        }
    }
}
