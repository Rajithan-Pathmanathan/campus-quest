package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.ui.creator.CheckpointEditorViewModel
import com.campusquest.ui.creator.LightPreset
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
class CheckpointEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CheckpointEditorViewModel

    private val sampleCp = Checkpoint(
        id = "CP_SAVED_1",
        gameId = "GAME_001",
        name = "Saved Checkpoint",
        lat = 6.9328,
        lng = 79.8436,
        radiusM = 30f,
        lightSignature = LightSignature(150f, 600f),
        clue = "Where leaders meet",
        lore = "Built in 1930",
        order = 2,
        motionType = "SHAKE"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CheckpointEditorViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadCheckpoint_withNullId_setsSensibleDefaults() {
        viewModel.loadCheckpoint(gameId = "GAME_001", checkpointId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEditMode)
        assertEquals("GAME_001", state.gameId)
        assertEquals(25f, state.radiusM)
        assertEquals(LightPreset.INDOOR_LAB, state.selectedPreset)
        assertEquals("SWEEP", state.motionType)
    }

    @Test
    fun loadCheckpoint_withExistingId_loadsCheckpointData() {
        viewModel.loadCheckpoint(
            gameId = "GAME_001",
            checkpointId = "CP_SAVED_1",
            customCheckpoint = sampleCp
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEditMode)
        assertEquals("CP_SAVED_1", state.checkpointId)
        assertEquals("Saved Checkpoint", state.name)
        assertEquals("Where leaders meet", state.clue)
        assertEquals("Built in 1930", state.lore)
        assertEquals(6.9328, state.lat, 0.0001)
        assertEquals(79.8436, state.lng, 0.0001)
        assertEquals(30f, state.radiusM)
        assertEquals("SHAKE", state.motionType)
    }

    @Test
    fun applyLightPreset_updatesLuxRangeAndPreset() {
        viewModel.loadCheckpoint(gameId = "GAME_001", checkpointId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.applyLightPreset(LightPreset.DAYLIGHT)

        val state = viewModel.uiState.value
        assertEquals(LightPreset.DAYLIGHT, state.selectedPreset)
        assertEquals(500f, state.minLux)
        assertEquals(2000f, state.maxLux)
    }

    @Test
    fun updateMotionType_updatesGesture() {
        viewModel.loadCheckpoint(gameId = "GAME_001", checkpointId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateMotionType("TILT")
        assertEquals("TILT", viewModel.uiState.value.motionType)
    }

    @Test
    fun saveCheckpoint_failsWhenMissingNameOrClue() {
        viewModel.loadCheckpoint(gameId = "GAME_001", checkpointId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Name is blank
        val result1 = viewModel.saveCheckpoint()
        assertNull(result1)
        assertFalse(viewModel.uiState.value.isSaved)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("name is required", ignoreCase = true))

        // Fill name, leave clue blank
        viewModel.updateIdentity("Valid Name", "", "Some lore")
        val result2 = viewModel.saveCheckpoint()
        assertNull(result2)
        assertFalse(viewModel.uiState.value.isSaved)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("clue riddle is required", ignoreCase = true))
    }

    @Test
    fun saveCheckpoint_succeedsAndReturnsCheckpointModel() {
        viewModel.loadCheckpoint(gameId = "GAME_001", checkpointId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateIdentity("Clock Tower", "Look up at the bells", "Erected in 1857")
        viewModel.updateCoordinates(6.9360, 79.8436, 25f)
        viewModel.applyLightPreset(LightPreset.HIGH_NOON)
        viewModel.updateMotionType("SWEEP")

        val savedCheckpoint = viewModel.saveCheckpoint()
        assertNotNull(savedCheckpoint)
        assertTrue(viewModel.uiState.value.isSaved)
        assertNull(viewModel.uiState.value.errorMessage)

        assertEquals("Clock Tower", savedCheckpoint?.name)
        assertEquals("Look up at the bells", savedCheckpoint?.clue)
        assertEquals("Erected in 1857", savedCheckpoint?.lore)
        assertEquals(6.9360, savedCheckpoint!!.lat, 0.0001)
        assertEquals(79.8436, savedCheckpoint.lng, 0.0001)
        assertEquals(25f, savedCheckpoint.radiusM)
        assertEquals(2000f, savedCheckpoint.lightSignature.minLux)
        assertEquals(10000f, savedCheckpoint.lightSignature.maxLux)
        assertEquals("SWEEP", savedCheckpoint.motionType)
    }
}
