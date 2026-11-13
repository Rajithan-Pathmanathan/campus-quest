package com.campusquest

import com.campusquest.data.repository.SensorRepositoryImpl
import com.campusquest.device.location.HaversineDistanceCalculator
import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.device.sensor.SensorSimulationFixture
import com.campusquest.domain.fusion.GpsScoreCalculator
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.LocationPermissionState
import com.campusquest.domain.model.LocationState
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Game-logic checks for the Member 3 fixes (GPS evidence, light curve, missing-sensor policy, lifecycle). */
@OptIn(ExperimentalCoroutinesApi::class)
class M3GameLogicTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val haversine = HaversineDistanceCalculator()

    private val checkpoint = Checkpoint(
        id = "CP_BELL_TOWER",
        gameId = "GAME_HERITAGE_001",
        name = "Bell Tower",
        lat = 6.9271,
        lng = 79.8612,
        radiusM = 20f,
        lightSignature = LightSignature(200f, 800f),
        clue = "c",
        lore = "l",
        motionType = "sweep"
    )

    private fun at(lat: Double, lng: Double, accuracy: Float = 4f) = LocationState(
        permissionState = LocationPermissionState.Granted,
        servicesEnabled = true,
        smoothedLatitude = lat,
        smoothedLongitude = lng,
        accuracyMeters = accuracy
    )

    private fun engine(fixture: SensorSimulationFixture) =
        SensorFusionEngine(signalSource = fixture, scope = CoroutineScope(dispatcher))

    private fun SensorSimulationFixture.fullSensorMatch() {
        simulateLightMatch(lux = 450f, matched = true)
        simulateMotionDetected(true)
        simulateProximity(true)
    }

    // --- H1: GPS score ---

    @Test
    fun gpsScore_fallsWithDistance() {
        assertEquals(1.0f, GpsScoreCalculator.score(0f, 20f), 0.001f)
        assertEquals(0.5f, GpsScoreCalculator.score(20f, 20f), 0.001f)
        assertEquals(0.0f, GpsScoreCalculator.score(40f, 20f), 0.001f)
        assertEquals(0.0f, GpsScoreCalculator.score(500f, 20f), 0.001f)
        assertEquals(0.0f, GpsScoreCalculator.score(0f, 0f), 0.001f)
    }

    @Test
    fun gpsScore_noFixOrPoorAccuracy_isZero() {
        val dist = haversine::calculateDistanceMeters
        assertEquals(0f, GpsScoreCalculator.score(LocationState(), checkpoint.lat, checkpoint.lng, 20f, dist), 0.001f)
        val poor = at(checkpoint.lat, checkpoint.lng, accuracy = 80f)
        assertEquals(0f, GpsScoreCalculator.score(poor, checkpoint.lat, checkpoint.lng, 20f, dist), 0.001f)
        val good = at(checkpoint.lat, checkpoint.lng)
        assertEquals(1f, GpsScoreCalculator.score(good, checkpoint.lat, checkpoint.lng, 20f, dist), 0.001f)
    }

    @Test
    fun engine_withoutGpsUpdate_cannotClaimEvenWithAllSensors() {
        val fixture = SensorSimulationFixture()
        val engine = engine(fixture)
        engine.startListening("G", "C", checkpoint.lightSignature, MotionType.SWEEP)
        fixture.fullSensorMatch()

        val r = engine.fusionResult.value
        assertEquals(0.60f, r.totalScore, 0.001f) // light 0.3 + motion 0.3, GPS 0
        assertFalse(r.canClaimDiscovery)
    }

    @Test
    fun repository_playerAtCheckpoint_canClaim_playerFarAway_cannot() {
        val location = MutableStateFlow(LocationState())
        val fixture = SensorSimulationFixture()
        val repo = SensorRepositoryImpl(
            fusionEngine = engine(fixture),
            locationRepository = FakeLocationRepository(location),
            scope = CoroutineScope(dispatcher)
        )

        repo.startScanSession(checkpoint)
        fixture.fullSensorMatch()

        // ~220 m north of the checkpoint
        location.value = at(checkpoint.lat + 0.002, checkpoint.lng)
        assertFalse("Far away must never allow discovery", repo.fusionResult.value.canClaimDiscovery)

        // ~5 m from the checkpoint
        location.value = at(checkpoint.lat + 0.00005, checkpoint.lng)
        assertTrue(repo.fusionResult.value.thresholdReached)
        assertTrue(repo.fusionResult.value.canClaimDiscovery)

        // Stopping the session stops GPS feeding
        repo.stopScanSession()
        location.value = at(checkpoint.lat + 0.002, checkpoint.lng)
        assertTrue(repo.fusionResult.value.canClaimDiscovery)
    }

    @Test
    fun repository_newCheckpoint_resetsGpsEvidence() {
        val location = MutableStateFlow(at(checkpoint.lat, checkpoint.lng))
        val fixture = SensorSimulationFixture()
        val repo = SensorRepositoryImpl(engine(fixture), FakeLocationRepository(location), scope = CoroutineScope(dispatcher))

        repo.startScanSession(checkpoint)
        assertEquals(1f, repo.fusionResult.value.gpsScore, 0.001f)

        val other = checkpoint.copy(id = "CP_OTHER", lat = checkpoint.lat + 0.01)
        repo.startScanSession(other)
        assertEquals("CP_OTHER", repo.fusionResult.value.checkpointId)
        assertEquals(0f, repo.fusionResult.value.gpsScore, 0.001f)
    }

    // --- M4: continuous light score ---

    @Test
    fun lightScore_isContinuous() {
        val sig = LightSignature(200f, 800f) // tolerance = 25% of 600 = 150
        assertEquals(1.0f, sig.score(500f), 0.001f)
        assertEquals(1.0f, sig.score(200f), 0.001f)
        assertEquals(0.5f, sig.score(125f), 0.001f)
        assertEquals(0.5f, sig.score(875f), 0.001f)
        assertEquals(0.0f, sig.score(0f), 0.001f)
        assertEquals(0.0f, sig.score(5000f), 0.001f)

        val narrow = LightSignature(100f, 100f) // tolerance floors at 20
        assertEquals(0.5f, narrow.score(110f), 0.001f)
    }

    @Test
    fun engine_usesContinuousLightScore() {
        val fixture = SensorSimulationFixture()
        val engine = engine(fixture)
        engine.startListening("G", "C", LightSignature(200f, 800f), MotionType.SWEEP, gpsScore = 1f)
        fixture.simulateLightMatch(lux = 125f, matched = false)
        assertEquals(0.5f, engine.fusionResult.value.lightScore!!, 0.001f)
        assertEquals(0.55f, engine.fusionResult.value.totalScore, 0.001f) // 0.4 + 0.3*0.5
    }

    // --- Missing hardware policy ---

    @Test
    fun missingProximitySensor_bypassesFinalGate() {
        val fixture = SensorSimulationFixture()
        val engine = engine(fixture)
        engine.startListening("G", "C", checkpoint.lightSignature, MotionType.SWEEP, gpsScore = 1f)
        fixture.simulateLightMatch()
        fixture.simulateMotionDetected(true)
        assertFalse("Proximity present but FAR blocks the gate", engine.fusionResult.value.canClaimDiscovery)

        fixture.simulateMissingHardware(hasProximity = false)
        assertTrue(engine.fusionResult.value.canClaimDiscovery)
    }

    @Test
    fun missingLightSensor_reNormalizesGpsAndMotion() {
        val fixture = SensorSimulationFixture()
        val engine = engine(fixture)
        engine.startListening("G", "C", checkpoint.lightSignature, MotionType.SWEEP, gpsScore = 1f)
        fixture.simulateMissingHardware(hasLight = false)
        fixture.simulateMotionDetected(true)
        fixture.simulateProximity(true)

        val r = engine.fusionResult.value
        assertEquals(null, r.lightScore)
        assertEquals(1.0f, r.totalScore, 0.001f)
        assertTrue(r.canClaimDiscovery)
    }

    // --- H4: lifecycle ---

    @Test
    fun engine_close_stopsReactingToSensors() {
        val fixture = SensorSimulationFixture()
        val engine = engine(fixture)
        engine.startListening("G", "C", checkpoint.lightSignature, MotionType.SWEEP, gpsScore = 1f)
        val before = engine.fusionResult.value.totalScore

        engine.close()
        fixture.simulateLightMatch()
        assertEquals(before, engine.fusionResult.value.totalScore, 0.001f)
    }

    // --- M2: motion type mapping ---

    @Test
    fun motionType_fromString_isSafe() {
        assertEquals(MotionType.SWEEP, MotionType.fromString("sweep"))
        assertEquals(MotionType.SHAKE, MotionType.fromString(" SHAKE "))
        assertEquals(MotionType.TILT, MotionType.fromString("Tilt"))
        assertEquals(MotionType.SWEEP, MotionType.fromString(null))
        assertEquals(MotionType.SWEEP, MotionType.fromString("garbage"))
    }

    private class FakeLocationRepository(
        override val locationState: StateFlow<LocationState>
    ) : LocationRepository {
        override fun startTracking(intervalMs: Long, minDistanceM: Float) = Unit
        override fun stopTracking() = Unit
        override fun updatePermissionState(state: LocationPermissionState) = Unit
    }
}
