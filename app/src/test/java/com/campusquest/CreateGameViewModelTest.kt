package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.ui.creator.CreateGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CreateGameViewModel

    private val sampleCp1 = Checkpoint(
        id = "CP_1",
        gameId = "DRAFT_1",
        name = "Lab Node",
        lat = 6.9345,
        lng = 79.8440,
        radiusM = 20f,
        lightSignature = LightSignature(300f, 800f),
        clue = "Optics clue",
        lore = "Lab lore",
        order = 1,
        motionType = "SWEEP"
    )

    private val sampleCp2 = Checkpoint(
        id = "CP_2",
        gameId = "DRAFT_1",
        name = "Particle Chamber",
        lat = 6.9350,
        lng = 79.8450,
        radiusM = 25f,
        lightSignature = LightSignature(100f, 500f),
        clue = "Particle clue",
        lore = "Chamber lore",
        order = 2,
        motionType = "SHAKE"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreateGameViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadQuest_withExistingId_populatesMetadataAndCheckpoints() {
        viewModel.loadQuest(
            gameId = "DRAFT_1",
            customTitle = "Custom Quest",
            customDesc = "Custom Description",
            customCheckpoints = listOf(sampleCp1, sampleCp2)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("DRAFT_1", state.gameId)
        assertEquals("Custom Quest", state.title)
        assertEquals("Custom Description", state.description)
        assertEquals(2, state.checkpointCount)
        assertTrue(state.canPublish)
    }

    @Test
    fun updateTitleAndDescription_updatesStateAndClearsErrors() {
        viewModel.loadQuest(null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTitle("New Adventure")
        viewModel.updateDescription("Exploring the university quad.")

        val state = viewModel.uiState.value
        assertEquals("New Adventure", state.title)
        assertEquals("Exploring the university quad.", state.description)
        assertNull(state.errorMessage)
    }

    @Test
    fun addOrUpdateCheckpoint_insertsNewAndMaintainsOrderedSequence() {
        viewModel.loadQuest(gameId = "DRAFT_1", customCheckpoints = emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.checkpointCount)

        viewModel.addOrUpdateCheckpoint(sampleCp1)
        viewModel.addOrUpdateCheckpoint(sampleCp2)

        val state = viewModel.uiState.value
        assertEquals(2, state.checkpointCount)
        assertEquals(1, state.checkpoints[0].order)
        assertEquals("Lab Node", state.checkpoints[0].name)
        assertEquals(2, state.checkpoints[1].order)
        assertEquals("Particle Chamber", state.checkpoints[1].name)
    }

    @Test
    fun moveCheckpointUpAndDown_swapsOrderCorrectly() {
        viewModel.loadQuest(
            gameId = "DRAFT_1",
            customCheckpoints = listOf(sampleCp1, sampleCp2)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Move CP_2 from index 1 to 0 (Move Up)
        viewModel.moveCheckpointUp(1)

        val state = viewModel.uiState.value
        assertEquals("Particle Chamber", state.checkpoints[0].name)
        assertEquals(1, state.checkpoints[0].order)
        assertEquals("Lab Node", state.checkpoints[1].name)
        assertEquals(2, state.checkpoints[1].order)

        // Move CP_2 back down from index 0 to 1 (Move Down)
        viewModel.moveCheckpointDown(0)
        val stateAfterDown = viewModel.uiState.value
        assertEquals("Lab Node", stateAfterDown.checkpoints[0].name)
        assertEquals(1, stateAfterDown.checkpoints[0].order)
        assertEquals("Particle Chamber", stateAfterDown.checkpoints[1].name)
        assertEquals(2, stateAfterDown.checkpoints[1].order)
    }

    @Test
    fun removeCheckpoint_reindexesRemainingSequence() {
        val sampleCp3 = sampleCp1.copy(id = "CP_3", name = "Node 3", order = 3)
        viewModel.loadQuest(
            gameId = "DRAFT_1",
            customCheckpoints = listOf(sampleCp1, sampleCp2, sampleCp3)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.checkpointCount)

        // Remove middle checkpoint (CP_2)
        viewModel.removeCheckpoint("CP_2")

        val state = viewModel.uiState.value
        assertEquals(2, state.checkpointCount)
        assertEquals("CP_1", state.checkpoints[0].id)
        assertEquals(1, state.checkpoints[0].order)
        assertEquals("CP_3", state.checkpoints[1].id)
        assertEquals(2, state.checkpoints[1].order)
    }

    @Test
    fun publishQuest_failsWhenUnderTwoCheckpoints() {
        viewModel.loadQuest(
            gameId = "DRAFT_1",
            customTitle = "Quest with 1 CP",
            customDesc = "Description here",
            customCheckpoints = listOf(sampleCp1)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canPublish)
        val success = viewModel.publishQuest()

        assertFalse(success)
        assertFalse(viewModel.uiState.value.isPublished)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("minimum 2 required"))
    }

    @Test
    fun publishQuest_succeedsWhenAllRequirementsMet() {
        viewModel.loadQuest(
            gameId = "DRAFT_1",
            customTitle = "Complete Quest",
            customDesc = "Complete Description",
            customCheckpoints = listOf(sampleCp1, sampleCp2)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canPublish)
        val success = viewModel.publishQuest()

        assertTrue(success)
        assertTrue(viewModel.uiState.value.isPublished)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
