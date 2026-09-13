package com.campusquest

import com.campusquest.device.location.geofence.BoundaryHysteresis
import com.campusquest.device.location.geofence.GeofenceKey
import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.device.sensor.SensorSimulationFixture
import com.campusquest.domain.fusion.DiscoveryGateEvaluator
import com.campusquest.domain.fusion.FusionConfig
import com.campusquest.domain.fusion.GeneralizedFusionCalculator
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.CheckpointEligibility
import com.campusquest.domain.model.GeofenceState
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * M3 Golden Physical Discovery Path Integration Test.
 *
 * Validates the complete cross-module pipeline:
 * Creator Checkpoint Data (M2 model)
 *   ↓
 * Dynamic Geofencing & Hysteresis (Module B)
 *   ↓
 * CheckpointEligibility (M3 -> M4 contract)
 *   ↓
 * Physical / Simulated Sensors (Module C)
 *   ↓
 * Generalized Multi-Sensor Fusion & Normalization (Module D)
 *   ↓
 * 85% Resonance Threshold Reached
 *   ↓
 * Physical Proximity Confirmation Gate
 *   ↓
 * Discovery Claim Ready for M4 Reveal Dialog
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoldenPathPhysicalDiscoveryTest {

    private lateinit var hysteresis: BoundaryHysteresis
    private lateinit var simulationFixture: SensorSimulationFixture
    private lateinit var fusionEngine: SensorFusionEngine
    private lateinit var discoveryEvaluator: DiscoveryGateEvaluator

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        hysteresis = BoundaryHysteresis(hysteresisMarginMeters = 5.0f)
        simulationFixture = SensorSimulationFixture()
        fusionEngine = SensorFusionEngine(
            signalSource = simulationFixture,
            fusionCalculator = GeneralizedFusionCalculator(FusionConfig(threshold = 0.85f)),
            discoveryEvaluator = DiscoveryGateEvaluator(),
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        discoveryEvaluator = DiscoveryGateEvaluator()
    }

    @Test
    fun completeGoldenPath_crossesModuleB_C_D_to_M4Reveal() = runTest(testDispatcher) {
        val gameId = "GAME_HERITAGE_COLOMBO"
        val checkpoint = Checkpoint(
            id = "CP_OLD_PARLIAMENT",
            gameId = gameId,
            name = "Old Parliament Building",
            lat = 6.9328,
            lng = 79.8436,
            radiusM = 30.0f,
            order = 1,
            clue = "Where statesmen met by the Indian Ocean.",
            lore = "Built in 1930 in neo-baroque style.",
            lightSignature = LightSignature(minLux = 200f, maxLux = 800f),
            motionType = "SWEEP"
        )

        // -------------------------------------------------------------
        // Step 1 (Module B): Geofence Key Parsing & Dynamic Key Contract
        // -------------------------------------------------------------
        val requestId = GeofenceKey.build(gameId, checkpoint.id)
        val parsedKey = GeofenceKey.parse(requestId)
        assertEquals(gameId, parsedKey?.first)
        assertEquals(checkpoint.id, parsedKey?.second)

        // -------------------------------------------------------------
        // Step 2 (Module B): Player moves toward checkpoint (Outer Approach)
        // -------------------------------------------------------------
        // Distance 50m (Radius 30m) -> OUTSIDE
        val farState = hysteresis.evaluate(
            distanceMeters = 50.0f,
            radiusMeters = checkpoint.radiusM,
            previousState = GeofenceState.OUTSIDE
        )
        assertEquals(GeofenceState.OUTSIDE, farState)

        // Distance 20m (Inside 30m radius) -> Transition to INSIDE
        val insideState = hysteresis.evaluate(
            distanceMeters = 20.0f,
            radiusMeters = checkpoint.radiusM,
            previousState = farState
        )
        assertEquals(GeofenceState.INSIDE, insideState)

        // Generate CheckpointEligibility for M4
        val eligibility = CheckpointEligibility(
            gameId = gameId,
            checkpointId = checkpoint.id,
            distanceMeters = 20.0f,
            radiusMeters = checkpoint.radiusM,
            geofenceState = insideState,
            gpsAccuracyMeters = 3.5f,
            eligibleForScan = insideState == GeofenceState.INSIDE
        )
        assertTrue("Player is inside geofence and eligible to open Scan HUD", eligibility.eligibleForScan)

        // -------------------------------------------------------------
        // Step 3 (Module C & D): M4 Opens Scan HUD & Starts Sensor Session
        // -------------------------------------------------------------
        fusionEngine.startListening(
            gameId = gameId,
            checkpointId = checkpoint.id,
            targetSignature = checkpoint.lightSignature,
            motionType = MotionType.SWEEP,
            gpsScore = 1.0f // 100% GPS contribution from being inside geofence
        )

        // Initial State inside Scan HUD: GPS matched, but Light & Motion pending
        var result = fusionEngine.fusionResult.value
        assertEquals(0.40f, result.totalScore, 0.001f) // GPS 40% only
        assertFalse("40% is below 85% threshold", result.thresholdReached)
        assertFalse("Cannot claim discovery", result.canClaimDiscovery)

        // -------------------------------------------------------------
        // Step 4 (Module C): Environmental Light Sensor Matches
        // -------------------------------------------------------------
        simulationFixture.simulateLightMatch(lux = 450f, matched = true)
        result = fusionEngine.fusionResult.value
        // GPS (40%) + Light (30%) = 70%
        assertEquals(0.70f, result.totalScore, 0.001f)
        assertFalse("70% is below 85% threshold", result.thresholdReached)

        // -------------------------------------------------------------
        // Step 5 (Module C): Player Executes Required SWEEP Motion Gesture
        // -------------------------------------------------------------
        simulationFixture.simulateMotionDetected(detected = true)
        result = fusionEngine.fusionResult.value
        // GPS (40%) + Light (30%) + Motion (30%) = 100%
        assertEquals(1.00f, result.totalScore, 0.001f)
        assertTrue("Resonance threshold reached at 100%!", result.thresholdReached)
        assertFalse("Cannot claim discovery yet: Proximity gate is still FAR", result.canClaimDiscovery)

        // -------------------------------------------------------------
        // Step 6 (Proximity Gate): Phone Tapped / Brought Near Relic
        // -------------------------------------------------------------
        simulationFixture.simulateProximity(isNear = true)
        result = fusionEngine.fusionResult.value
        assertTrue("Proximity verified NEAR", result.proximityNear)
        assertTrue("All physical signals and gates confirmed: DISCOVERY READY!", result.canClaimDiscovery)

        // -------------------------------------------------------------
        // Step 7 (M4 Presentation): Clue & Lore Discovery Unlocked
        // -------------------------------------------------------------
        assertTrue(discoveryEvaluator.evaluate(result, proximityNear = true))
        assertEquals("Where statesmen met by the Indian Ocean.", checkpoint.clue)
    }
}
