package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.LocationPermissionState
import com.campusquest.domain.model.LocationState
import com.campusquest.domain.model.PlayerProgress
import com.campusquest.domain.model.SensorState
import com.campusquest.domain.repository.GameRepository
import com.campusquest.domain.repository.LocationRepository
import com.campusquest.domain.repository.SensorRepository
import com.campusquest.ui.quest.QuestScanViewModel
import com.campusquest.ui.quest.ScanAction
import com.campusquest.ui.quest.ScanState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun cp(gameId: String, id: String, order: Int = 1) = Checkpoint(
    id = id, gameId = gameId, name = "Ember $id",
    lat = 6.9722948, lng = 79.9163835, radiusM = 15f,
    clue = "clue-$id", lore = "lore-$id", order = order, rarity = "RARE"
)

private class ScanFakeGameRepository : GameRepository {
    val checkpoints = mutableMapOf<String, List<Checkpoint>>()
    var completed: List<String> = emptyList()
    var loadError: Exception? = null
    var recordResults = ArrayDeque<Result<Unit>>()
    val recorded = mutableListOf<Triple<String, String, Long>>()

    override suspend fun getGameCheckpoints(gameId: String): List<Checkpoint> {
        loadError?.let { throw it }
        return checkpoints[gameId].orEmpty()
    }

    override suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit> {
        recorded += Triple(gameId, checkpointId, foundAt)
        return recordResults.removeFirstOrNull() ?: Result.success(Unit)
    }

    override fun observePlayerProgress(gameId: String, userId: String): Flow<PlayerProgress?> =
        flowOf(PlayerProgress(gameId, userId, completedCheckpointIds = completed))

    override suspend fun getAvailableGames(): List<Game> = emptyList()
    var status: GameStatus? = GameStatus.PUBLISHED
    override suspend fun getGameDetails(gameId: String): Game? = status?.let {
        Game(id = gameId, title = "Quest", description = "", creatorId = "c", creatorName = "C", status = it)
    }
    override suspend fun createGame(game: Game): Result<Game> = Result.success(game)
    override suspend fun createCheckpoint(gameId: String, checkpoint: Checkpoint) = Result.success(checkpoint)
    override suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint) = Result.success(Unit)
    override suspend fun publishGame(gameId: String) = Result.success(Unit)
    override suspend fun joinGame(gameId: String) = Result.success(Unit)
    override suspend fun getFusionSignature(gameId: String, checkpointId: String) = LightSignature(0f, 1000f)
    override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> = flowOf(emptyList())
    override suspend fun syncPending() {}
}

private class ScanFakeSensorRepository : SensorRepository {
    private val _sensor = MutableStateFlow(SensorState())
    private val _fusion = MutableStateFlow(fusion("", ""))
    override val sensorState: StateFlow<SensorState> = _sensor
    override val fusionResult: StateFlow<FusionResult> = _fusion
    val started = mutableListOf<Checkpoint>()
    var stopCount = 0

    override fun startScanSession(checkpoint: Checkpoint) { started += checkpoint }
    override fun stopScanSession() { stopCount++ }
    override fun resetScanSession() {}

    fun emit(result: FusionResult) { _fusion.value = result }

    companion object {
        fun fusion(
            gameId: String,
            checkpointId: String,
            percent: Int = 0,
            threshold: Boolean = false,
            near: Boolean = false
        ) = FusionResult(
            gameId = gameId, checkpointId = checkpointId, gpsScore = 1f, lightScore = 1f, motionScore = 1f,
            totalScore = percent / 100f, progressPercent = percent, thresholdReached = threshold,
            proximityNear = near, activeSignals = emptySet()
        )
    }
}

private class ScanFakeLocationRepository : LocationRepository {
    private val _location = MutableStateFlow(LocationState())
    override val locationState: StateFlow<LocationState> = _location
    var tracking = false
    var permission: LocationPermissionState? = null

    override fun startTracking(intervalMs: Long, minDistanceM: Float) { tracking = true }
    override fun stopTracking() { tracking = false }
    override fun updatePermissionState(state: LocationPermissionState) { permission = state }

    fun at(lat: Double, lng: Double, accuracy: Float = 5f) {
        _location.value = LocationState(
            permissionState = LocationPermissionState.Granted, servicesEnabled = true,
            smoothedLatitude = lat, smoothedLongitude = lng, accuracyMeters = accuracy
        )
    }

    fun atRelic() = at(6.9722948, 79.9163835)
    fun farAway() = at(6.9822948, 79.9163835) // ~1.1 km north
}

class QuestScanViewModelTest {

    @get:Rule
    val mainRule = M1MainDispatcherRule()

    private val games = ScanFakeGameRepository()
    private val sensors = ScanFakeSensorRepository()
    private val location = ScanFakeLocationRepository()

    private fun vm(clockMs: Long = 1_000L) = QuestScanViewModel(
        gameRepository = games,
        sensorRepository = sensors,
        locationRepository = location,
        userId = { "u1" },
        clock = { clockMs }
    )

    private fun QuestScanViewModel.openAlpha(): QuestScanViewModel {
        games.checkpoints["game-alpha"] = listOf(cp("game-alpha", "cp-alpha-001", 1), cp("game-alpha", "cp-alpha-002", 2))
        load("game-alpha", "cp-alpha-001")
        onLocationPermissionResult(true)
        onScreenStarted()
        return this
    }

    private fun signalsAligned(game: String = "game-alpha", id: String = "cp-alpha-001", near: Boolean) =
        sensors.emit(ScanFakeSensorRepository.fusion(game, id, percent = 90, threshold = true, near = near))

    @Test
    fun `loads the checkpoint and waits for location`() {
        val vm = vm().openAlpha()
        val s = vm.uiState.value
        assertEquals("Ember cp-alpha-001", s.checkpointName)
        assertEquals(ScanState.WAITING_FOR_LOCATION, s.scanState)
        assertEquals("cp-alpha-001", sensors.started.single().id)
        assertTrue(location.tracking)
    }

    @Test
    fun `unknown or blank checkpoint is unavailable without crashing`() {
        val vm = vm()
        vm.load("game-alpha", "missing")
        assertEquals(ScanState.UNAVAILABLE, vm.uiState.value.scanState)
        assertEquals("Checkpoint unavailable", vm.uiState.value.error?.title)
        assertEquals(ScanAction.BACK, vm.uiState.value.primaryAction)

        vm.load("", "")
        assertEquals(ScanState.UNAVAILABLE, vm.uiState.value.scanState)
    }

    @Test
    fun `load failure is a retryable error`() {
        games.loadError = RuntimeException("offline")
        val vm = vm()
        vm.load("game-alpha", "cp-alpha-001")
        assertEquals(ScanState.ERROR, vm.uiState.value.scanState)
        assertEquals(ScanAction.RETRY, vm.uiState.value.primaryAction)

        games.loadError = null
        games.checkpoints["game-alpha"] = listOf(cp("game-alpha", "cp-alpha-001"))
        vm.retry()
        assertEquals(ScanState.WAITING_FOR_LOCATION, vm.uiState.value.scanState)
    }

    @Test
    fun `already found checkpoint cannot be scanned again`() {
        games.completed = listOf("cp-alpha-001")
        val vm = vm().openAlpha()
        assertEquals(ScanState.ALREADY_FOUND, vm.uiState.value.scanState)
        assertTrue(sensors.started.isEmpty())
        vm.beginScan()
        assertEquals(ScanState.ALREADY_FOUND, vm.uiState.value.scanState)
        assertTrue(games.recorded.isEmpty())
    }

    @Test
    fun `walking toward the checkpoint moves from far to ready to scan`() {
        val vm = vm().openAlpha()
        location.farAway()
        assertEquals(ScanState.OUTSIDE_CHECKPOINT, vm.uiState.value.scanState)
        assertNotNull(vm.uiState.value.distanceText)
        assertNotNull(vm.uiState.value.warmth)

        location.atRelic()
        assertEquals(ScanState.READY_TO_SCAN, vm.uiState.value.scanState)
        assertEquals(ScanAction.BEGIN_SCAN, vm.uiState.value.primaryAction)
    }

    @Test
    fun `weak gps never shows the relic`() {
        val vm = vm().openAlpha()
        location.at(6.9722948, 79.9163835, accuracy = 30f)
        assertEquals(ScanState.OUTSIDE_CHECKPOINT, vm.uiState.value.scanState)
    }

    @Test
    fun `scan only starts after the player begins it`() {
        val vm = vm().openAlpha()
        location.atRelic()
        signalsAligned(near = true)
        assertEquals(ScanState.READY_TO_SCAN, vm.uiState.value.scanState)
        assertTrue(games.recorded.isEmpty())
    }

    @Test
    fun `threshold with proximity far blocks the reveal`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        signalsAligned(near = false)
        val s = vm.uiState.value
        assertEquals(ScanState.WAITING_FOR_PROXIMITY, s.scanState)
        assertNotNull(s.shieldHint)
        assertNull(s.reveal)
        assertTrue(games.recorded.isEmpty())
    }

    @Test
    fun `proximity near after threshold records the discovery and reveals`() {
        val vm = vm(clockMs = 4242L).openAlpha()
        location.atRelic()
        vm.beginScan()
        signalsAligned(near = true)

        val s = vm.uiState.value
        assertEquals(ScanState.REVEALING, s.scanState)
        assertEquals(Triple("game-alpha", "cp-alpha-001", 4242L), games.recorded.single())
        assertEquals("lore-cp-alpha-001", s.reveal?.lore)
        assertEquals("clue-cp-alpha-001", s.reveal?.clue)
        assertEquals("RARE", s.reveal?.rarity)
        assertFalse(s.reveal!!.isFinalCheckpoint)
        // The active scan is closed.
        assertEquals(1, sensors.stopCount)
        assertFalse(location.tracking)
    }

    @Test
    fun `discovery is recorded once even when signals keep firing`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        repeat(5) { signalsAligned(near = true); location.atRelic() }
        sensors.emit(ScanFakeSensorRepository.fusion("game-alpha", "cp-alpha-001", 95, true, true))
        assertEquals(1, games.recorded.size)
        assertEquals(ScanState.REVEALING, vm.uiState.value.scanState)
    }

    @Test
    fun `dismissing the reveal completes the scan`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        signalsAligned(near = true)
        vm.onRevealDismissed()
        assertEquals(ScanState.COMPLETED, vm.uiState.value.scanState)
        vm.onRevealDismissed()
        assertEquals(ScanState.COMPLETED, vm.uiState.value.scanState)
    }

    @Test
    fun `last checkpoint shows quest complete`() {
        games.completed = listOf("cp-alpha-001")
        games.checkpoints["game-alpha"] = listOf(cp("game-alpha", "cp-alpha-001", 1), cp("game-alpha", "cp-alpha-002", 2))
        val vm = vm()
        vm.load("game-alpha", "cp-alpha-002")
        vm.onLocationPermissionResult(true)
        vm.onScreenStarted()
        location.atRelic()
        vm.beginScan()
        signalsAligned(id = "cp-alpha-002", near = true)
        assertTrue(vm.uiState.value.reveal!!.isFinalCheckpoint)
    }

    @Test
    fun `save failure shows an error and retry does not duplicate the discovery`() {
        games.recordResults = ArrayDeque(listOf(Result.failure(RuntimeException("disk"))))
        val vm = vm(clockMs = 77L).openAlpha()
        location.atRelic()
        vm.beginScan()
        signalsAligned(near = true)

        assertEquals(ScanState.ERROR, vm.uiState.value.scanState)
        assertEquals(ScanAction.RETRY, vm.uiState.value.primaryAction)
        assertEquals(1, games.recorded.size)

        vm.retry()
        assertEquals(ScanState.REVEALING, vm.uiState.value.scanState)
        // The retry re-saves the same confirmed discovery (same timestamp); it never starts a second scan.
        assertEquals(2, games.recorded.size)
        assertEquals(games.recorded[0], games.recorded[1])
        assertEquals(1, sensors.started.size)
    }

    @Test
    fun `evidence from another checkpoint is ignored`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        signalsAligned(game = "game-beta", id = "cp-beta-001", near = true)
        assertEquals(ScanState.SCANNING, vm.uiState.value.scanState)
        assertTrue(games.recorded.isEmpty())
    }

    @Test
    fun `switching game or checkpoint reinitialises the scan`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        assertEquals(ScanState.SCANNING, vm.uiState.value.scanState)

        games.checkpoints["game-beta"] = listOf(cp("game-beta", "cp-beta-001"))
        vm.load("game-beta", "cp-beta-001")
        vm.onScreenStarted()

        val s = vm.uiState.value
        assertEquals("game-beta", s.gameId)
        assertEquals("cp-beta-001", s.checkpointId)
        assertEquals("Ember cp-beta-001", s.checkpointName)
        // Scan must be begun again for the new checkpoint.
        assertEquals(ScanState.READY_TO_SCAN, s.scanState)
        assertEquals("cp-beta-001", sensors.started.last().id)
    }

    @Test
    fun `reloading the same checkpoint keeps the state`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        vm.load("game-alpha", "cp-alpha-001")
        assertEquals(ScanState.SCANNING, vm.uiState.value.scanState)
    }

    @Test
    fun `leaving the screen stops the sensors and requires beginning the scan again`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        vm.onScreenStopped()
        assertEquals(1, sensors.stopCount)
        assertFalse(location.tracking)
        assertEquals(ScanState.READY_TO_SCAN, vm.uiState.value.scanState)

        vm.onScreenStarted()
        assertEquals(2, sensors.started.size)
    }

    @Test
    fun `denied location permission asks the player to allow it`() {
        val vm = vm()
        games.checkpoints["game-alpha"] = listOf(cp("game-alpha", "cp-alpha-001"))
        vm.load("game-alpha", "cp-alpha-001")
        vm.onLocationPermissionResult(false)
        val s = vm.uiState.value
        assertEquals(ScanState.WAITING_FOR_LOCATION, s.scanState)
        assertEquals(ScanAction.GRANT_LOCATION, s.primaryAction)
        assertEquals(LocationPermissionState.Denied, location.permission)
    }

    @Test
    fun `player never sees a percentage or raw score`() {
        val vm = vm().openAlpha()
        location.atRelic()
        vm.beginScan()
        sensors.emit(ScanFakeSensorRepository.fusion("game-alpha", "cp-alpha-001", percent = 72))
        val s = vm.uiState.value
        val text = listOf(s.headline, s.distanceText, s.warmth, s.glow?.label, s.lightHint, s.trialHint, s.shieldHint)
            .filterNotNull().joinToString(" ")
        assertFalse(text, text.contains('%'))
        assertFalse(text, text.contains("72"))
        assertEquals("The ember burns steadily", s.glow?.label)
    }
}

class QuestScanAccessTest {

    @get:Rule
    val mainRule = M1MainDispatcherRule()

    private fun open(status: GameStatus?): QuestScanViewModel {
        val games = ScanFakeGameRepository()
        games.status = status
        games.checkpoints["g"] = listOf(cp("g", "c1"))
        val vm = QuestScanViewModel(games, ScanFakeSensorRepository(), ScanFakeLocationRepository(), { "u" })
        vm.load("g", "c1")
        return vm
    }

    @Test
    fun `closed game shows scan unavailable`() {
        val s = open(GameStatus.CLOSED).uiState.value
        assertEquals(ScanState.UNAVAILABLE, s.scanState)
        assertEquals("Scan unavailable", s.error?.title)
        assertEquals("This game is no longer active.", s.error?.message)
        assertEquals(ScanAction.BACK, s.primaryAction)
    }

    @Test
    fun `draft or missing game is unavailable`() {
        assertEquals(ScanState.UNAVAILABLE, open(GameStatus.DRAFT).uiState.value.scanState)
        assertEquals(ScanState.UNAVAILABLE, open(null).uiState.value.scanState)
    }

    @Test
    fun `published game is playable`() {
        assertEquals(ScanState.WAITING_FOR_LOCATION, open(GameStatus.PUBLISHED).uiState.value.scanState)
    }
}
