package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.ui.player.GameDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: GameDetailViewModel

    private val sampleGame = Game(
        id = "GAME_001",
        title = "Heritage Exploration",
        description = "Uncover historical landmarks",
        creatorId = "C1",
        creatorName = "Alan Turing",
        status = GameStatus.PUBLISHED,
        checkpointCount = 3
    )

    private val sampleCheckpoints = listOf(
        Checkpoint(
            id = "CP3",
            gameId = "GAME_001",
            name = "Galle Face",
            lat = 6.9248,
            lng = 79.8427,
            radiusM = 35f,
            lightSignature = LightSignature(300f, 1000f),
            clue = "Sunset winds",
            lore = "Promenade lore",
            order = 3
        ),
        Checkpoint(
            id = "CP1",
            gameId = "GAME_001",
            name = "Clock Tower",
            lat = 6.9360,
            lng = 79.8436,
            radiusM = 25f,
            lightSignature = LightSignature(200f, 700f),
            clue = "Timekeeper",
            lore = "Tower lore",
            order = 1
        ),
        Checkpoint(
            id = "CP2",
            gameId = "GAME_001",
            name = "Parliament",
            lat = 6.9328,
            lng = 79.8436,
            radiusM = 30f,
            lightSignature = LightSignature(150f, 600f),
            clue = "Statesmen",
            lore = "Parliament lore",
            order = 2
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GameDetailViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadGameDetails_sortsCheckpointsByOrder() {
        viewModel.loadGameDetails("GAME_001", sampleGame, sampleCheckpoints)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.game)
        assertEquals("Heritage Exploration", state.game?.title)
        assertEquals(3, state.checkpoints.size)

        // Check sorting: order 1 -> 2 -> 3
        assertEquals(1, state.checkpoints[0].order)
        assertEquals("Clock Tower", state.checkpoints[0].name)
        assertEquals(2, state.checkpoints[1].order)
        assertEquals("Parliament", state.checkpoints[1].name)
        assertEquals(3, state.checkpoints[2].order)
        assertEquals("Galle Face", state.checkpoints[2].name)
    }

    @Test
    fun joinQuest_togglesJoinedState() {
        viewModel.loadGameDetails("GAME_001", sampleGame, sampleCheckpoints)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isJoined)

        viewModel.joinQuest()
        assertTrue(viewModel.uiState.value.isJoined)
    }
}
