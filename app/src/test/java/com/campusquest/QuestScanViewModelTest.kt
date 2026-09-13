package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.ui.quest.QuestScanViewModel
import com.campusquest.ui.quest.ScanState
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
class QuestScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: QuestScanViewModel

    private val testCheckpoint = Checkpoint(
        id = "CP_BELL_TOWER",
        gameId = "GAME_HERITAGE_001",
        name = "Historic Clock Tower",
        lat = 6.9360,
        lng = 79.8436,
        radiusM = 25f,
        lightSignature = LightSignature(200f, 700f),
        clue = "Look skyward where the bell chimes",
        lore = "Erected in 1857",
        order = 1,
        motionType = "SWEEP"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = QuestScanViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadScanContext_initializesStateAndEntersScanning() {
        viewModel.loadScanContext("GAME_HERITAGE_001", "CP_BELL_TOWER", testCheckpoint)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ScanState.SCANNING, state.scanState)
        assertEquals("Historic Clock Tower", state.checkpoint?.name)
        assertEquals(1.0f, state.gpsScore, 0.01f) // GPS contribution = 0.40 -> 40% progress
        assertEquals(40, state.progressPercent)
        assertFalse(state.thresholdReached)
        assertFalse(state.canClaimDiscovery)
    }

    @Test
    fun onLightAndMotionUpdated_reachesThresholdButAwaitsProximity() {
        viewModel.loadScanContext("GAME_HERITAGE_001", "CP_BELL_TOWER", testCheckpoint)
        testDispatcher.scheduler.advanceUntilIdle()

        // Light in envelope (400 lux is between 200 and 700) -> 0.30 contribution
        viewModel.onLightReadingChanged(400f)

        // Motion gesture completed -> 0.30 contribution
        viewModel.onMotionDetected(true)

        val state = viewModel.uiState.value
        // Total score = 0.40 (GPS) + 0.30 (Light) + 0.30 (Motion) = 1.0 (100%)
        assertEquals(100, state.progressPercent)
        assertTrue(state.thresholdReached)
        assertFalse("Cannot claim discovery without physical proximity confirmation", state.canClaimDiscovery)
        assertEquals(ScanState.FUSION_THRESHOLD_REACHED, state.scanState)
    }

    @Test
    fun onProximityNear_unlocksDiscoveryClaim() {
        viewModel.loadScanContext("GAME_HERITAGE_001", "CP_BELL_TOWER", testCheckpoint)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onLightReadingChanged(400f)
        viewModel.onMotionDetected(true)
        viewModel.onProximityChanged(true)

        val state = viewModel.uiState.value
        assertTrue(state.thresholdReached)
        assertTrue(state.proximityNear)
        assertTrue("Discovery claim unlocked!", state.canClaimDiscovery)
        assertEquals(ScanState.REVEAL_READY, state.scanState)
    }

    @Test
    fun claimDiscovery_transitionsToDiscoveredState() {
        viewModel.loadScanContext("GAME_HERITAGE_001", "CP_BELL_TOWER", testCheckpoint)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onLightReadingChanged(400f)
        viewModel.onMotionDetected(true)
        viewModel.onProximityChanged(true)

        val success = viewModel.claimDiscovery()
        assertTrue(success)

        val state = viewModel.uiState.value
        assertTrue(state.isDiscoveryClaimed)
        assertEquals(ScanState.DISCOVERED, state.scanState)
    }

    @Test
    fun claimDiscovery_failsWhenNotReady() {
        viewModel.loadScanContext("GAME_HERITAGE_001", "CP_BELL_TOWER", testCheckpoint)
        testDispatcher.scheduler.advanceUntilIdle()

        // Only GPS active (40%), not ready to claim
        val success = viewModel.claimDiscovery()
        assertFalse(success)
        assertFalse(viewModel.uiState.value.isDiscoveryClaimed)
    }
}
