package com.campusquest

import com.campusquest.data.repository.AuthRepositoryImpl
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.repository.GameRepository
import com.campusquest.ui.creator.CreateGameViewModel
import com.campusquest.ui.map.CampusMapViewModel
import com.campusquest.ui.player.GameDetailViewModel
import com.campusquest.ui.player.GamesListViewModel
import com.campusquest.ui.quest.QuestScanViewModel
import com.campusquest.ui.quest.ScanState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndIntegrationAuditTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockGameRepository = mockk<GameRepository>(relaxed = true)

    private val sampleGame = Game(
        id = "GAME_HERITAGE_001",
        title = "Colombo Heritage Trail",
        description = "Discover historic colonial landmarks across the city.",
        creatorId = "PROF_ALAN",
        creatorName = "Prof. Alan Turing",
        status = GameStatus.PUBLISHED,
        checkpointCount = 2,
        publishedAt = System.currentTimeMillis()
    )

    private val sampleCheckpoints = listOf(
        Checkpoint(
            id = "CP_BELL_TOWER",
            gameId = "GAME_HERITAGE_001",
            name = "Historic Clock Tower",
            lat = 6.9360,
            lng = 79.8436,
            radiusM = 25f,
            lightSignature = LightSignature(150f, 800f),
            clue = "Look skyward where the timekeeper chimes above the quad.",
            lore = "Erected in 1857 as a lighthouse and clock tower.",
            order = 1,
            motionType = "SWEEP"
        ),
        Checkpoint(
            id = "CP_OLD_PARLIAMENT",
            gameId = "GAME_HERITAGE_001",
            name = "Old Parliament Building",
            lat = 6.9328,
            lng = 79.8436,
            radiusM = 30f,
            lightSignature = LightSignature(100f, 600f),
            clue = "Where statesmen met beside the sea.",
            lore = "Neo-baroque building completed in 1930.",
            order = 2,
            motionType = "SHAKE"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mockGameRepository.getAvailableGames() } returns listOf(sampleGame)
        coEvery { mockGameRepository.getGameDetails(sampleGame.id) } returns sampleGame
        coEvery { mockGameRepository.getGameCheckpoints(sampleGame.id) } returns sampleCheckpoints
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun audit_gamesListViewModel_queriesGameRepository() = runTest {
        val viewModel = GamesListViewModel(mockGameRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.allGames.size)
        assertEquals("Colombo Heritage Trail", state.allGames.first().title)
        coVerify(atLeast = 1) { mockGameRepository.getAvailableGames() }
    }

    @Test
    fun audit_gameDetailViewModel_wiresJoinQuestToRepository() = runTest {
        val viewModel = GameDetailViewModel(mockGameRepository)
        viewModel.loadGameDetails(sampleGame.id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sampleGame.title, viewModel.uiState.value.game?.title)
        assertEquals(2, viewModel.uiState.value.checkpoints.size)

        viewModel.joinQuest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isJoined)
        coVerify(exactly = 1) { mockGameRepository.joinGame(sampleGame.id) }
    }

    @Test
    fun audit_campusMapViewModel_loadsCheckpointsFromRepository() = runTest {
        val viewModel = CampusMapViewModel(gameRepository = mockGameRepository)
        viewModel.loadMapData(sampleGame.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.checkpoints.size)
        assertEquals("Historic Clock Tower", state.checkpoints.first().name)
        coVerify(atLeast = 1) { mockGameRepository.getGameCheckpoints(sampleGame.id) }
    }

    @Test
    fun audit_questScanViewModel_claimDiscovery_recordsDiscoveryInRepository() = runTest {
        val viewModel = QuestScanViewModel(gameRepository = mockGameRepository)
        viewModel.loadScanContext(sampleGame.id, "CP_BELL_TOWER")
        testDispatcher.scheduler.advanceUntilIdle()

        // Provide resonant multi-sensor signals
        viewModel.onGpsScoreChanged(1.0f)
        viewModel.onLightReadingChanged(400f) // Matches 150-800
        viewModel.onMotionDetected(true)
        viewModel.onProximityChanged(true) // Physical gate confirmed
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Should be eligible to claim discovery", viewModel.uiState.value.canClaimDiscovery)

        val claimed = viewModel.claimDiscovery()
        assertTrue("Claim discovery should return true", claimed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ScanState.DISCOVERED, viewModel.uiState.value.scanState)
        coVerify(exactly = 1) { mockGameRepository.recordDiscovery(sampleGame.id, "CP_BELL_TOWER", any()) }
    }

    @Test
    fun audit_createGameViewModel_publishQuest_enforcesGamePublishValidator() = runTest {
        val viewModel = CreateGameViewModel(gameRepository = mockGameRepository)
        viewModel.loadQuest("DRAFT_TEST", customTitle = "Short", customDesc = "Short", customCheckpoints = emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        // Attempting to publish invalid quest must fail validation
        val publishedInvalid = viewModel.publishQuest()
        assertFalse("Publishing quest with short title & 0 checkpoints must be blocked", publishedInvalid)
        assertNotNull(viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { mockGameRepository.publishGame(any()) }

        // Provide valid title, description, and 2 ordered checkpoints
        viewModel.updateTitle("Valid Quest Title Across Campus")
        viewModel.updateDescription("This is a valid long description for the authored quest.")
        for (cp in sampleCheckpoints) {
            viewModel.addOrUpdateCheckpoint(cp)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val publishedValid = viewModel.publishQuest()
        assertTrue("Publishing valid quest must succeed", publishedValid)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPublished)
        coVerify(exactly = 1) { mockGameRepository.publishGame("DRAFT_TEST") }
    }

    @Test
    fun audit_authRepository_safeAgainstMissingFirebase_allowsGuestSignAndUser() = runTest {
        val safeAuth = AuthRepositoryImpl(customFirebaseAuth = null)
        val guestResult = safeAuth.signInAnonymously()

        assertTrue("Guest login must succeed without crashing even when Firebase is absent", guestResult.isSuccess)
        val guestUser = guestResult.getOrNull()
        assertNotNull(guestUser)
        assertTrue(guestUser!!.isAnonymous)

        val emailResult = safeAuth.signInWithEmail("cadet@campusquest.org", "Pass1234!")
        assertTrue("Email sign-in fallback must succeed without crashing", emailResult.isSuccess)
        assertEquals("cadet@campusquest.org", emailResult.getOrNull()?.email)
    }
}
