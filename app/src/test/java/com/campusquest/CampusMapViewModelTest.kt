package com.campusquest

import com.campusquest.device.location.LocationState
import com.campusquest.domain.model.GeofenceState
import com.campusquest.ui.map.CampusMapViewModel
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
class CampusMapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CampusMapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CampusMapViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockLocationState(lat: Double, lng: Double, accuracy: Float = 5f): LocationState {
        return LocationState(
            servicesEnabled = true,
            smoothedLatitude = lat,
            smoothedLongitude = lng,
            accuracyMeters = accuracy
        )
    }

    @Test
    fun loadMapData_withValidGameId_populatesCheckpointsAndInitialState() {
        viewModel.loadMapData("GAME_HERITAGE_001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("GAME_HERITAGE_001", state.gameId)
        assertEquals("Colombo Heritage Trail", state.gameTitle)
        assertEquals(3, state.checkpoints.size)
        assertEquals(0, state.activeCheckpointIndex)
        assertEquals("CP_BELL_TOWER", state.activeCheckpoint?.id)
        assertEquals("Historic Clock Tower", state.activeCheckpoint?.name)
    }

    @Test
    fun onLocationChanged_outsideRadius_computesDistanceAndOutsideState() {
        viewModel.loadMapData("GAME_HERITAGE_001")
        testDispatcher.scheduler.advanceUntilIdle()

        // Player is ~222 meters north of Historic Clock Tower (6.9360, 79.8436)
        val playerLoc = createMockLocationState(lat = 6.9380, lng = 79.8436)
        viewModel.onLocationChanged(playerLoc)

        val state = viewModel.uiState.value
        assertNotNull(state.playerLocation)
        val cpVisual = state.activeGeofenceVisualState
        assertNotNull(cpVisual)
        assertEquals(GeofenceState.OUTSIDE, cpVisual?.state)
        assertFalse(state.isScanEligible)
        assertTrue(cpVisual!!.distanceMeters!! > 200f)
        assertTrue(state.formattedDistance().contains("m remaining"))
    }

    @Test
    fun onLocationChanged_insideRadius_setsInsideStateAndEnablesScan() {
        viewModel.loadMapData("GAME_HERITAGE_001")
        testDispatcher.scheduler.advanceUntilIdle()

        // Player is ~0.8 meters from Clock Tower (6.9360, 79.8436)
        val playerLoc = createMockLocationState(lat = 6.936005, lng = 79.843605)
        viewModel.onLocationChanged(playerLoc)

        val state = viewModel.uiState.value
        val cpVisual = state.activeGeofenceVisualState
        assertNotNull(cpVisual)
        assertEquals(GeofenceState.INSIDE, cpVisual?.state)
        assertTrue(state.isScanEligible)
    }

    @Test
    fun selectCheckpoint_switchesActiveTargetAndUpdatesTelemetry() {
        viewModel.loadMapData("GAME_HERITAGE_001")
        testDispatcher.scheduler.advanceUntilIdle()

        val playerLoc = createMockLocationState(lat = 6.932805, lng = 79.843605)
        viewModel.onLocationChanged(playerLoc)

        // Select second checkpoint: Old Parliament (6.9328, 79.8436)
        viewModel.selectCheckpoint(1)

        val state = viewModel.uiState.value
        assertEquals(1, state.activeCheckpointIndex)
        assertEquals("CP_OLD_PARLIAMENT", state.activeCheckpoint?.id)
        assertEquals(GeofenceState.INSIDE, state.activeGeofenceVisualState?.state)
        assertTrue(state.isScanEligible)
    }

    @Test
    fun selectCheckpointById_selectsCorrectIndex() {
        viewModel.loadMapData("GAME_HERITAGE_001")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectCheckpointById("CP_GALLE_FACE")

        val state = viewModel.uiState.value
        assertEquals(2, state.activeCheckpointIndex)
        assertEquals("Galle Face Promenade", state.activeCheckpoint?.name)
    }

    @Test
    fun nextCheckpoint_advancesToNextWaypointUntilEnd() {
        viewModel.loadMapData("GAME_HERITAGE_001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.activeCheckpointIndex)

        val advanced1 = viewModel.nextCheckpoint()
        assertTrue(advanced1)
        assertEquals(1, viewModel.uiState.value.activeCheckpointIndex)

        val advanced2 = viewModel.nextCheckpoint()
        assertTrue(advanced2)
        assertEquals(2, viewModel.uiState.value.activeCheckpointIndex)

        val advanced3 = viewModel.nextCheckpoint()
        assertFalse(advanced3)
        assertEquals(2, viewModel.uiState.value.activeCheckpointIndex)
    }

    @Test
    fun loadMapData_withEmptyCheckpoints_setsErrorMessage() {
        viewModel.loadMapData(
            gameId = "GAME_EMPTY",
            customTitle = "Empty Quest",
            customCheckpoints = emptyList()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.checkpoints.isEmpty())
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("No checkpoints found"))
    }
}
