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
import com.campusquest.data.remote.FirestoreService
import com.campusquest.data.remote.dto.FirestoreStoryConfigDto
import com.campusquest.story.StoryConfigEntity
import com.campusquest.story.StoryDao
import com.campusquest.story.LightTheWay
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

class GameRepositoryImpl(
    private val gameDao: GameDao,
    private val checkpointDao: CheckpointDao,
    private val playerProgressDao: PlayerProgressDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository? = null,
    private val remoteSyncHandler: (suspend (SyncQueueEntity) -> Boolean)? = null,
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
    /** Cloud backend; null in tests or when Firebase is unavailable (the app then works offline). */
    private val remote: FirestoreService? = null,
    private val storyDao: StoryDao? = null,
    /** Display name shown on leaderboards. */
    private val userName: () -> String = { DEFAULT_PLAYER_NAME },
    /** Called after anything is queued for upload (schedules the sync worker). */
    private val onQueued: () -> Unit = {},
    /** Called once a newly published quest reached the cloud (sends the "new quest" push). */
    private val onPublished: suspend (gameId: String) -> Unit = {}
) : GameRepository {

    private suspend fun enqueue(item: SyncQueueEntity) {
        syncQueueDao.enqueue(item)
        onQueued()
    }

    private fun currentUserId(): String {
        return authRepository?.getCurrentUserId()?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCAL_USER_ID
    }

    override suspend fun getAvailableGames(): List<Game> {
        refreshFromCloud()
        return gameDao.getPublishedGames(GameStatus.PUBLISHED).map { it.toDomain() }
    }

    /**
     * Downloads published quests (with checkpoints and Light the Way config) and the signed-in
     * player's progress into Room. Best effort: offline, the app keeps using what is stored.
     */
    private suspend fun refreshFromCloud() {
        val cloud = remote ?: return
        val uid = authRepository?.getCurrentUserId()
        try {
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                val pendingGameIds = syncQueueDao.getPendingSyncItems()
                    .map { payloadValue(it.payloadJson, "gameId") ?: it.entityId }.toSet()
                val remoteGames = cloud.fetchPublishedGames()
                val remoteIds = remoteGames.map { it.game.id }.toSet()
                // Quests deleted in the cloud by their creator disappear here too (own drafts stay).
                gameDao.getPublishedGames(GameStatus.PUBLISHED)
                    .filter { it.id !in remoteIds && it.id !in pendingGameIds && it.creatorId != uid }
                    .forEach { inTransaction { removeLocally(it.id) } }
                for (remoteGame in remoteGames) {
                    val game = remoteGame.game
                    if (game.id in pendingGameIds) continue // local edits not uploaded yet win
                    inTransaction {
                        gameDao.insertGame(GameEntity.fromDomain(game))
                        val keep = remoteGame.checkpoints.map { it.id }.toSet()
                        checkpointDao.getCheckpointsForGame(game.id).filter { it.id !in keep }
                            .forEach { checkpointDao.deleteCheckpoint(it.id) }
                        checkpointDao.insertCheckpoints(remoteGame.checkpoints.map { CheckpointEntity.fromDomain(it.copy(gameId = game.id)) })
                        remoteGame.story?.let { storyDao?.putConfig(StoryConfigEntity(game.id, it.cardId, it.backupCode, it.targetBearing)) }
                    }
                }
                if (uid != null) {
                    for (cloudProgress in cloud.fetchProgress(uid)) {
                        if (gameDao.getGameById(cloudProgress.gameId) == null) continue
                        val local = playerProgressDao.getProgress(cloudProgress.gameId, uid)
                        if (local == null || cloudProgress.completedCheckpointIds.size > local.completedCheckpointIds.size) {
                            playerProgressDao.insertOrUpdateProgress(PlayerProgressEntity.fromDomain(cloudProgress))
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloud refresh failed, using local data: ${e.message}")
        }
    }

    override suspend fun getCreatorGames(creatorId: String): List<Game> {
        return gameDao.getCreatorGames(creatorId).map { it.toDomain() }
    }

    override suspend fun getGameDetails(gameId: String): Game? {
        if (gameId.isBlank()) return null

        val localGame = gameDao.getGameById(gameId)
        if (localGame != null) {
            return localGame.toDomain()
        }

        // Download quests when this phone does not have the requested one yet.
        refreshFromCloud()

        return gameDao.getGameById(gameId)?.toDomain()
    }

    override suspend fun createGame(game: Game): Result<Game> {
        return try {
            gameDao.insertGame(GameEntity.fromDomain(game))
            enqueue(
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
            enqueue(
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
            enqueue(
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
            enqueue(
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
            enqueue(
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
                inTransaction {
                    if (storyDao?.config(gameId) == null) storyDao?.putConfig(LightTheWay.newConfig(gameId))
                    gameDao.updateGame(published)
                    enqueue(
                        SyncQueueEntity(
                            operationType = SyncOperationType.PUBLISH_GAME,
                            entityId = gameId,
                            payloadJson = """{"gameId":"$gameId","publishedAt":$publishedAt}"""
                        )
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Game $gameId not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGame(gameId: String): Result<Unit> {
        return try {
            val game = gameDao.getGameById(gameId) ?: return Result.failure(IllegalArgumentException("This quest no longer exists."))
            val uid = authRepository?.getCurrentUserId()
            if (uid == null || game.creatorId != uid) {
                return Result.failure(SecurityException("Only the person who created this quest can delete it."))
            }
            inTransaction {
                removeLocally(gameId)
                enqueue(SyncQueueEntity(
                    operationType = SyncOperationType.DELETE_GAME,
                    entityId = gameId,
                    payloadJson = """{"gameId":"$gameId","creatorId":"$uid"}"""
                ))
            }
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Quest, its checkpoints (cascade), every player's progress on it and its Light the Way config. */
    private suspend fun removeLocally(gameId: String) {
        gameDao.deleteGame(gameId)
        playerProgressDao.deleteGameProgress(gameId)
        storyDao?.deleteConfig(gameId)
        storyDao?.deleteSessions(gameId)
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
                enqueue(
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
            inTransaction {
                val valid = checkpointDao.getCheckpointsForGame(gameId).map { it.id }.toSet()
                require(checkpointId in valid) { "Checkpoint does not belong to this game" }
                val existing = playerProgressDao.getProgress(gameId, userId)
                val completed = existing?.completedCheckpointIds.orEmpty().filter { it in valid }
                if (checkpointId !in completed) {
                    val updatedIds = completed + checkpointId
                    val finished = valid.all { it in updatedIds }
                    val updated = PlayerProgressEntity(gameId, userId, updatedIds, updatedIds.size + 1,
                        (existing?.score ?: 0) + 100, existing?.startedAt ?: foundAt,
                        if (finished) foundAt else null, finished)
                    playerProgressDao.insertOrUpdateProgress(updated)
                    enqueue(SyncQueueEntity(operationType = SyncOperationType.RECORD_DISCOVERY,
                        entityId = checkpointId,
                        payloadJson = """{"gameId":"$gameId","checkpointId":"$checkpointId","userId":"$userId","foundAt":$foundAt}"""))
                }
            }
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Cloud leaderboard (all players); falls back to this phone's players if the cloud is unavailable. */
    override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> {
        val me = authRepository?.getCurrentUserId()
        val local = playerProgressDao.observeGameProgress(gameId).map { rows -> rows.mapIndexed { index, row ->
            GameLeaderboardEntry(row.userId, if (row.userId == me) userName() else DEFAULT_PLAYER_NAME, row.score,
                ((row.completedAt ?: System.currentTimeMillis()) - row.startedAt).coerceAtLeast(0) / 1000,
                row.completedAt ?: 0, index + 1)
        } }
        val cloud = remote ?: return local
        if (me == null) return local
        return cloud.observeLeaderboard(gameId).catch { e ->
            Log.w(TAG, "Cloud leaderboard unavailable: ${e.message}")
            emitAll(local)
        }
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
        val cloud = remote
        val handler: suspend (SyncQueueEntity) -> Boolean = remoteSyncHandler
            ?: if (cloud != null) { item -> upload(cloud, item) } else return
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

    /** Uploads one queued change. Returns true when it is done (or nothing needs uploading). */
    private suspend fun upload(cloud: FirestoreService, item: SyncQueueEntity): Boolean {
        val uid = authRepository?.getCurrentUserId() ?: return false // wait until signed in
        return when (item.operationType) {
            SyncOperationType.DELETE_GAME -> {
                // Only the creator's account can delete it in the cloud; otherwise wait for them to sign in.
                if (payloadValue(item.payloadJson, "creatorId") != uid) return false
                cloud.deleteGame(payloadValue(item.payloadJson, "gameId") ?: item.entityId)
                true
            }
            SyncOperationType.CREATE_GAME, SyncOperationType.CREATE_CHECKPOINT,
            SyncOperationType.UPDATE_CHECKPOINT, SyncOperationType.DELETE_CHECKPOINT,
            SyncOperationType.PUBLISH_GAME -> {
                val gameId = payloadValue(item.payloadJson, "gameId") ?: item.entityId
                val game = gameDao.getGameById(gameId)?.toDomain() ?: return true // deleted locally
                if (game.status != GameStatus.PUBLISHED || game.creatorId != uid) return true // drafts stay on the phone
                val story = storyDao?.config(gameId)?.let { FirestoreStoryConfigDto(it.cardId, it.backupCode, it.targetBearing) }
                cloud.publishGame(game, checkpointDao.getCheckpointsForGame(gameId).map { it.toDomain() }, story)
                if (item.operationType == SyncOperationType.PUBLISH_GAME) {
                    try { onPublished(gameId) } catch (e: Exception) { Log.w(TAG, "New-quest notification failed: ${e.message}") }
                }
                true
            }
            SyncOperationType.JOIN_GAME, SyncOperationType.RECORD_DISCOVERY, SyncOperationType.UPDATE_PROGRESS -> {
                val userId = payloadValue(item.payloadJson, "userId") ?: uid
                if (userId != uid) return true // recorded by an offline/other account on this phone
                val gameId = payloadValue(item.payloadJson, "gameId") ?: return true
                val progress = playerProgressDao.getProgress(gameId, uid)?.toDomain() ?: return true
                cloud.saveProgress(progress)
                if (progress.score > 0) {
                    val seconds = ((progress.completedAt ?: System.currentTimeMillis()) - progress.startedAt).coerceAtLeast(0) / 1000
                    cloud.submitLeaderboardEntry(gameId, GameLeaderboardEntry(uid, userName(), progress.score, seconds, progress.completedAt ?: 0))
                }
                true
            }
        }
    }

    companion object {
        const val DEFAULT_LOCAL_USER_ID = "local_offline_user"
        const val DEFAULT_PLAYER_NAME = "Explorer"
        private const val TAG = "GameRepository"
        private const val CLOUD_TIMEOUT_MS = 10_000L
        private val FIELD = Regex("\"([A-Za-z]+)\":\"?([^\",}]*)")

        /** Reads a field from the small JSON payloads this class writes, e.g. {"gameId":"G1"}. */
        internal fun payloadValue(json: String, key: String): String? =
            FIELD.findAll(json).firstOrNull { it.groupValues[1] == key }?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }
    }
}
