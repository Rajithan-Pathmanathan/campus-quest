package com.campusquest

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
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSyncIntegrationTest {

    private val gameDao = mockk<GameDao>(relaxed = true)
    private val checkpointDao = mockk<CheckpointDao>(relaxed = true)
    private val playerProgressDao = mockk<PlayerProgressDao>(relaxed = true)
    private val syncQueueDao = mockk<SyncQueueDao>(relaxed = true)
    private val firestoreService = mockk<FirestoreService>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    private lateinit var repository: GameRepositoryImpl

    private val sampleGame = Game(
        id = "G_OFFLINE",
        title = "Underground Tunnels Quest",
        description = "Discover basement labs",
        creatorId = "USER_1",
        creatorName = "Architect",
        status = GameStatus.PUBLISHED,
        checkpointCount = 2
    )

    private val sampleCheckpoint = Checkpoint(
        id = "CP_TUNNEL_1",
        gameId = "G_OFFLINE",
        name = "Hydraulics Room",
        lat = 6.9020,
        lng = 79.8600,
        radiusM = 20f,
        lightSignature = LightSignature(10f, 200f),
        clue = "Follow the hum of the turbines",
        lore = "Built in 1965 to power early physics labs.",
        order = 1
    )

    @Before
    fun setUp() {
        coEvery { authRepository.getCurrentUserId() } returns "USER_1"
        repository = GameRepositoryImpl(
            gameDao = gameDao,
            checkpointDao = checkpointDao,
            playerProgressDao = playerProgressDao,
            syncQueueDao = syncQueueDao,
            firestoreService = firestoreService,
            authRepository = authRepository
        )
    }

    @Test
    fun getAvailableGames_remoteFails_returnsLocalCachedGames() = runTest {
        coEvery { firestoreService.fetchPublishedGames() } returns Result.failure(Exception("No connection"))
        coEvery { gameDao.getPublishedGames(GameStatus.PUBLISHED) } returns listOf(GameEntity.fromDomain(sampleGame))

        val games = repository.getAvailableGames()

        assertEquals(1, games.size)
        assertEquals("Underground Tunnels Quest", games[0].title)
    }

    @Test
    fun getAvailableGames_remoteSucceeds_writesThroughToLocalRoomCache() = runTest {
        val remoteGame = sampleGame.copy(title = "Updated Cloud Quest")
        coEvery { firestoreService.fetchPublishedGames() } returns Result.success(listOf(remoteGame))

        val games = repository.getAvailableGames()

        assertEquals(1, games.size)
        assertEquals("Updated Cloud Quest", games[0].title)
        coVerify(exactly = 1) { gameDao.insertGames(any()) }
    }

    @Test
    fun publishGame_enqueuesSyncAndSavesLocally() = runTest {
        coEvery { gameDao.getGameById("G_OFFLINE") } returns GameEntity.fromDomain(sampleGame.copy(status = GameStatus.DRAFT))
        coEvery { checkpointDao.getCheckpointsForGame("G_OFFLINE") } returns listOf(CheckpointEntity.fromDomain(sampleCheckpoint))
        coEvery { syncQueueDao.getPendingSyncItems() } returns emptyList()

        val result = repository.publishGame("G_OFFLINE")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { gameDao.updateGame(match { it.status == GameStatus.PUBLISHED }) }
        coVerify(exactly = 1) { syncQueueDao.enqueue(match { it.operationType == SyncOperationType.PUBLISH_GAME }) }
    }

    @Test
    fun syncPending_drainsPendingQueueWhenOnline() = runTest {
        val pendingItem = SyncQueueEntity(
            syncId = 42L,
            operationType = SyncOperationType.PUBLISH_GAME,
            entityId = "G_OFFLINE",
            payloadJson = "{}"
        )

        coEvery { syncQueueDao.getPendingSyncItems() } returns listOf(pendingItem)
        coEvery { gameDao.getGameById("G_OFFLINE") } returns GameEntity.fromDomain(sampleGame)
        coEvery { checkpointDao.getCheckpointsForGame("G_OFFLINE") } returns listOf(CheckpointEntity.fromDomain(sampleCheckpoint))
        coEvery { firestoreService.publishGame(any(), any()) } returns Result.success(Unit)

        repository.syncPending()

        coVerify(exactly = 1) { firestoreService.publishGame(any(), any()) }
        coVerify(exactly = 1) { syncQueueDao.deleteById(42L) }
    }
}
