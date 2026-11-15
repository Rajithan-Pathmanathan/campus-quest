package com.campusquest

import com.campusquest.data.local.dao.CheckpointDao
import com.campusquest.data.local.dao.GameDao
import com.campusquest.data.local.dao.PlayerProgressDao
import com.campusquest.data.local.dao.SyncQueueDao
import com.campusquest.data.local.entity.CheckpointEntity
import com.campusquest.data.local.entity.GameEntity
import com.campusquest.data.local.entity.PlayerProgressEntity
import com.campusquest.data.local.entity.SyncQueueEntity
import com.campusquest.data.mock.MockDataCatalog
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M6RepositoryAndSyncTest {

    private class FakeGameDao : GameDao {
        private val games = LinkedHashMap<String, GameEntity>()

        override fun observePublishedGames(status: GameStatus): Flow<List<GameEntity>> =
            flowOf(games.values.filter { it.status == status })

        override suspend fun getPublishedGames(status: GameStatus): List<GameEntity> =
            games.values.filter { it.status == status }.sortedByDescending { it.createdAt }

        override fun observeCreatorGames(creatorId: String): Flow<List<GameEntity>> =
            flowOf(games.values.filter { it.creatorId == creatorId })

        override suspend fun getCreatorGames(creatorId: String): List<GameEntity> =
            games.values.filter { it.creatorId == creatorId }.sortedByDescending { it.createdAt }

        override suspend fun getGameById(gameId: String): GameEntity? = games[gameId]

        override fun observeGameById(gameId: String): Flow<GameEntity?> =
            flowOf(games[gameId])

        override suspend fun insertGame(game: GameEntity) {
            games[game.id] = game
        }

        override suspend fun insertGames(games: List<GameEntity>) {
            games.forEach { this.games[it.id] = it }
        }

        override suspend fun updateGame(game: GameEntity) {
            games[game.id] = game
        }

        override suspend fun deleteGame(gameId: String) {
            games.remove(gameId)
        }
    }

    private class FakeCheckpointDao : CheckpointDao {
        private val checkpoints = LinkedHashMap<String, CheckpointEntity>()

        override fun observeCheckpointsForGame(gameId: String): Flow<List<CheckpointEntity>> =
            flowOf(checkpoints.values.filter { it.gameId == gameId }.sortedBy { it.orderIndex })

        override suspend fun getCheckpointsForGame(gameId: String): List<CheckpointEntity> =
            checkpoints.values.filter { it.gameId == gameId }.sortedBy { it.orderIndex }

        override suspend fun getCheckpointById(checkpointId: String): CheckpointEntity? =
            checkpoints[checkpointId]

        override suspend fun getCheckpointByOrder(gameId: String, order: Int): CheckpointEntity? =
            checkpoints.values.firstOrNull { it.gameId == gameId && it.orderIndex == order }

        override suspend fun insertCheckpoint(checkpoint: CheckpointEntity) {
            checkpoints[checkpoint.id] = checkpoint
        }

        override suspend fun insertCheckpoints(checkpoints: List<CheckpointEntity>) {
            checkpoints.forEach { this.checkpoints[it.id] = it }
        }

        override suspend fun updateCheckpoint(checkpoint: CheckpointEntity) {
            checkpoints[checkpoint.id] = checkpoint
        }

        override suspend fun deleteCheckpoint(checkpointId: String) {
            checkpoints.remove(checkpointId)
        }
    }

    private class FakePlayerProgressDao : PlayerProgressDao {
        override fun observeUserProgress(userId: String): Flow<List<PlayerProgressEntity>> = flowOf(progressMap.values.filter { it.userId == userId })
        override fun observeGameProgress(gameId: String): Flow<List<PlayerProgressEntity>> = flowOf(progressMap.values.filter { it.gameId == gameId })
        private val progressMap = LinkedHashMap<String, PlayerProgressEntity>()

        private fun key(gameId: String, userId: String) = "$gameId#$userId"

        override fun observeProgress(gameId: String, userId: String): Flow<PlayerProgressEntity?> =
            flowOf(progressMap[key(gameId, userId)])

        override suspend fun getProgress(gameId: String, userId: String): PlayerProgressEntity? =
            progressMap[key(gameId, userId)]

        override suspend fun getAllPlayerProgress(userId: String): List<PlayerProgressEntity> =
            progressMap.values.filter { it.userId == userId }

        override suspend fun insertOrUpdateProgress(progress: PlayerProgressEntity) {
            progressMap[key(progress.gameId, progress.userId)] = progress
        }

        override suspend fun updateProgress(progress: PlayerProgressEntity) {
            progressMap[key(progress.gameId, progress.userId)] = progress
        }
    }

    private class FakeSyncQueueDao : SyncQueueDao {
        private var nextId = 1L
        private val items = LinkedHashMap<Long, SyncQueueEntity>()

        override suspend fun getPendingSyncItems(): List<SyncQueueEntity> =
            items.values.sortedBy { it.createdAt }

        override suspend fun enqueue(item: SyncQueueEntity): Long {
            val id = if (item.syncId == 0L) nextId++ else item.syncId
            items[id] = item.copy(syncId = id)
            return id
        }

        override suspend fun deleteById(syncId: Long) {
            items.remove(syncId)
        }

        override suspend fun incrementRetryCount(syncId: Long) {
            val existing = items[syncId] ?: return
            items[syncId] = existing.copy(retryCount = existing.retryCount + 1)
        }

        override suspend fun update(item: SyncQueueEntity) {
            items[item.syncId] = item
        }

        override suspend fun clearAll() {
            items.clear()
        }
    }

    private class FakeAuthRepository(var activeUid: String? = "user_alpha") : AuthRepository {
        private val state = MutableStateFlow<AuthUser?>(null)
        override val currentUserState: StateFlow<AuthUser?> = state
        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
            Result.failure(UnsupportedOperationException())
        override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<AuthUser> =
            Result.failure(UnsupportedOperationException())
        override suspend fun signInAnonymously(): Result<AuthUser> =
            Result.failure(UnsupportedOperationException())
        override suspend fun signOut() { activeUid = null }
        override fun getCurrentUserId(): String? = activeUid
    }

    @Test
    fun a1_and_c1_resavingGamePreservesCheckpoints_and_getCreatorGamesReturnsDrafts() = runBlocking {
        val gameDao = FakeGameDao()
        val cpDao = FakeCheckpointDao()
        val repo = GameRepositoryImpl(gameDao, cpDao, FakePlayerProgressDao(), FakeSyncQueueDao())

        val draft = Game(
            id = "g1",
            title = "Draft Quest",
            description = "Initial",
            creatorId = "creator_1",
            creatorName = "Creator",
            status = GameStatus.DRAFT
        )
        repo.createGame(draft)
        repo.createCheckpoint(
            "g1",
            Checkpoint("cp1", "g1", "CP 1", 6.97, 79.91, 15f, LightSignature(0f, 100f), "clue", "lore", 1)
        )

        repo.updateGame(draft.copy(title = "Updated Quest Title"))

        val checkpoints = repo.getGameCheckpoints("g1")
        assertEquals(1, checkpoints.size)

        val creatorGames = repo.getCreatorGames("creator_1")
        assertEquals(1, creatorGames.size)
        assertEquals("Updated Quest Title", creatorGames.first().title)
    }

    @Test
    fun a2_syncPendingKeepsItemsWhenRemoteFails_and_deletesOnSuccess() = runBlocking {
        val syncDao = FakeSyncQueueDao()
        var shouldSucceed = false
        val repo = GameRepositoryImpl(
            FakeGameDao(),
            FakeCheckpointDao(),
            FakePlayerProgressDao(),
            syncDao,
            remoteSyncHandler = { shouldSucceed }
        )

        repo.createGame(
            Game("g1", "Quest", "Desc", "c1", "Creator", GameStatus.DRAFT)
        )
        assertEquals(1, syncDao.getPendingSyncItems().size)

        repo.syncPending()
        val afterFail = syncDao.getPendingSyncItems()
        assertEquals(1, afterFail.size)
        assertEquals(1, afterFail.first().retryCount)

        shouldSucceed = true
        repo.syncPending()
        assertTrue(syncDao.getPendingSyncItems().isEmpty())
    }

    @Test
    fun b1_b2_b3_b4_userIsolation_repeatClaim_completion_and_rejoin() = runBlocking {
        val gameDao = FakeGameDao()
        val cpDao = FakeCheckpointDao()
        val progressDao = FakePlayerProgressDao()
        val authRepo = FakeAuthRepository("player_1")
        val repo = GameRepositoryImpl(gameDao, cpDao, progressDao, FakeSyncQueueDao(), authRepo)

        val game = Game("g1", "Story", "Desc", "c1", "Creator", GameStatus.PUBLISHED, checkpointCount = 2)
        repo.createGame(game)
        repo.createCheckpoint("g1", Checkpoint("cp1", "g1", "One", 6.97, 79.91, 15f, LightSignature(0f, 100f), "c", "l", 1))
        repo.createCheckpoint("g1", Checkpoint("cp2", "g1", "Two", 6.98, 79.92, 15f, LightSignature(0f, 100f), "c", "l", 2))

        repo.joinGame("g1")
        repo.recordDiscovery("g1", "cp1", 1000L)

        // Repeat claim does not add +100 score
        repo.recordDiscovery("g1", "cp1", 2000L)
        val afterRepeat = progressDao.getProgress("g1", "player_1")!!
        assertEquals(100, afterRepeat.score)
        assertEquals(listOf("cp1"), afterRepeat.completedCheckpointIds)
        assertFalse(afterRepeat.isCompleted)

        // Re-joining keeps found checkpoints
        repo.joinGame("g1")
        val afterRejoin = progressDao.getProgress("g1", "player_1")!!
        assertEquals(100, afterRejoin.score)
        assertEquals(listOf("cp1"), afterRejoin.completedCheckpointIds)

        // Discovering final checkpoint sets isCompleted = true
        repo.recordDiscovery("g1", "cp2", 5000L)
        val afterFinal = progressDao.getProgress("g1", "player_1")!!
        assertEquals(200, afterFinal.score)
        assertTrue(afterFinal.isCompleted)
        assertEquals(5000L, afterFinal.completedAt)

        // Switching to player_2 has separate progress
        authRepo.activeUid = "player_2"
        repo.joinGame("g1")
        val player2Progress = progressDao.getProgress("g1", "player_2")
        assertNotNull(player2Progress)
        assertEquals(0, player2Progress!!.score)
        assertTrue(player2Progress.completedCheckpointIds.isEmpty())
    }

    @Test
    fun c2_and_d1_deleteCheckpoint_and_storyQuestSeed() = runBlocking {
        val cpDao = FakeCheckpointDao()
        val repo = GameRepositoryImpl(FakeGameDao(), cpDao, FakePlayerProgressDao(), FakeSyncQueueDao())

        repo.createCheckpoint("g1", Checkpoint("cp1", "g1", "One", 6.97, 79.91, 15f, LightSignature(0f, 100f), "c", "l", 1))
        assertEquals(1, repo.getGameCheckpoints("g1").size)

        repo.deleteCheckpoint("g1", "cp1")
        assertTrue(repo.getGameCheckpoints("g1").isEmpty())

        assertEquals("The Founder's Lantern", MockDataCatalog.foundersLanternGame.title)
        assertEquals(3, MockDataCatalog.foundersLanternCheckpoints.size)
        assertEquals(15f, MockDataCatalog.foundersLanternCheckpoints.first().radiusM)
    }
}
