package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.ui.quest.DiscoveryRevealViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryRevealViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DiscoveryRevealViewModel

    private val sampleCp = Checkpoint(
        id = "CP_BELL_TOWER",
        gameId = "GAME_HERITAGE_001",
        name = "Historic Clock Tower",
        lat = 6.9360,
        lng = 79.8436,
        radiusM = 25f,
        lightSignature = LightSignature(200f, 700f),
        clue = "Look skyward where the bell chimes",
        lore = "Erected in 1857 as a navigational lighthouse.",
        order = 1,
        motionType = "SWEEP"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DiscoveryRevealViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDiscovery_formatsCheckpointLoreAndDetails() {
        viewModel.loadDiscovery(
            gameId = "GAME_HERITAGE_001",
            checkpointId = "CP_BELL_TOWER",
            totalCheckpoints = 3,
            customCheckpoint = sampleCp
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Historic Clock Tower", state.checkpointName)
        assertEquals("Erected in 1857 as a navigational lighthouse.", state.lore)
        assertEquals("WAYPOINT 1 OF 3", state.sequenceBadge)
        assertEquals(100, state.pointsAwarded)
        assertFalse(state.isFinalCheckpoint)
    }

    @Test
    fun loadDiscovery_finalCheckpoint_setsBonusAndFinalFlag() {
        val finalCp = sampleCp.copy(order = 3)
        viewModel.loadDiscovery(
            gameId = "GAME_HERITAGE_001",
            checkpointId = "CP_BELL_TOWER",
            totalCheckpoints = 3,
            customCheckpoint = finalCp
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("WAYPOINT 3 OF 3", state.sequenceBadge)
        assertEquals(150, state.pointsAwarded) // 150 pts bonus for final quest completion
        assertTrue(state.isFinalCheckpoint)
    }
}
