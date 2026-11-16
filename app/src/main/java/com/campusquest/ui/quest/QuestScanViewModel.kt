package com.campusquest.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.campusquest.device.location.DistanceCalculator
import com.campusquest.device.location.HaversineDistanceCalculator
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LocationPermissionState
import com.campusquest.domain.model.LocationState
import com.campusquest.domain.model.SensorState
import com.campusquest.domain.quest.QuestZone
import com.campusquest.domain.quest.nextQuestZone
import com.campusquest.domain.repository.GameRepository
import com.campusquest.domain.repository.LocationRepository
import com.campusquest.domain.repository.SensorRepository
import com.campusquest.ui.common.AppProviders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the scan screen state (M4). It only consumes M3's repositories (location, fusion, sensors) and
 * the shared GameRepository; it never touches SensorManager, Room or Firebase, and it never computes
 * fusion itself.
 *
 * Discovery needs fusion threshold AND the proximity final gate. A discovery is recorded exactly once
 * per checkpoint per screen session (and the repository is expected to be idempotent too).
 */
class QuestScanViewModel(
    private val gameRepository: GameRepository,
    private val sensorRepository: SensorRepository,
    private val locationRepository: LocationRepository,
    private val userId: () -> String,
    private val distanceCalculator: DistanceCalculator = HaversineDistanceCalculator(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestScanUiState())
    val uiState: StateFlow<QuestScanUiState> = _uiState.asStateFlow()

    private var loadedKey: String? = null
    private var checkpoint: Checkpoint? = null
    private var loadJob: Job? = null
    private var signalsJob: Job? = null

    private var scanStarted = false
    private var discoveryRequested = false
    private var pendingFoundAt: Long? = null
    private var zone = QuestZone.FAR
    private var sessionRunning = false
    private var screenVisible = false
    private var permissionGranted = false
    /** Proximity fallback: the player held the shield (only counts once fusion is reached). */
    private var shieldHeld = false

    private var lastDistanceM: Float? = null
    private var lastFusion: FusionResult? = null
    private var lastSensor: SensorState? = null

    /** Opens a checkpoint. Calling it again for the same game/checkpoint is a no-op (rotation-safe). */
    fun load(gameId: String, checkpointId: String, force: Boolean = false) {
        val key = "$gameId#$checkpointId"
        if (!force && key == loadedKey) return

        // New game/checkpoint (or forced reload): drop everything from the previous one.
        loadJob?.cancel()
        signalsJob?.cancel()
        stopSession()
        loadedKey = key
        checkpoint = null
        scanStarted = false
        shieldHeld = false
        discoveryRequested = false
        pendingFoundAt = null
        zone = QuestZone.FAR
        lastDistanceM = null
        lastFusion = null
        lastSensor = null
        val permissionDenied = _uiState.value.permissionDenied
        _uiState.value = QuestScanUiState(
            gameId = gameId,
            checkpointId = checkpointId,
            permissionDenied = permissionDenied
        )

        loadJob = viewModelScope.launch {
            try {
                if (gameId.isBlank() || checkpointId.isBlank()) {
                    fail(ScanState.UNAVAILABLE, ScanNarrator.unavailableError())
                    return@launch
                }
                // Only active games can be played (not drafts, not closed games).
                val game = gameRepository.getGameDetails(gameId)
                if (game == null || game.status == GameStatus.DRAFT) {
                    fail(ScanState.UNAVAILABLE, ScanNarrator.unavailableError())
                    return@launch
                }
                if (game.status == GameStatus.CLOSED) {
                    fail(ScanState.UNAVAILABLE, ScanNarrator.closedError())
                    return@launch
                }
                val cps = gameRepository.getGameCheckpoints(gameId)
                val cp = cps.firstOrNull { it.id == checkpointId && it.gameId == gameId }
                if (cp == null) {
                    fail(ScanState.UNAVAILABLE, ScanNarrator.unavailableError())
                    return@launch
                }
                val completed = gameRepository.observePlayerProgress(gameId, userId()).first()
                    ?.completedCheckpointIds.orEmpty()
                checkpoint = cp
                val isFinal = NextCheckpointResolver.isFinal(cps, completed, cp)
                _uiState.value = _uiState.value.copy(
                    checkpointName = cp.name,
                    isFinalCheckpoint = isFinal
                )
                if (cp.id in completed) {
                    setState(ScanState.ALREADY_FOUND)
                    return@launch
                }
                setState(ScanState.WAITING_FOR_LOCATION)
                startSignals(cp)
                if (screenVisible) startSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(ScanState.ERROR, ScanNarrator.loadError())
            }
        }
    }

    /** Screen is in the foreground: start/resume M3 collection. */
    fun onScreenStarted() {
        screenVisible = true
        startSession()
    }

    /** Screen left the foreground: stop M3 collection to save battery. The scan must be begun again. */
    fun onScreenStopped() {
        screenVisible = false
        stopSession()
        if (ScanStateMachine.isActive(_uiState.value.scanState)) {
            scanStarted = false
            shieldHeld = false
            recompute()
        }
    }

    /** The result of the runtime location-permission request (the Android dialog lives in the fragment). */
    fun onLocationPermissionResult(granted: Boolean) {
        permissionGranted = granted
        locationRepository.updatePermissionState(
            if (granted) LocationPermissionState.Granted else LocationPermissionState.Denied
        )
        _uiState.value = _uiState.value.copy(permissionDenied = !granted)
        if (granted && sessionRunning) locationRepository.startTracking()
        recompute()
    }

    /** Player taps "wake the Resonator" once the relic appeared. */
    fun beginScan() {
        if (_uiState.value.scanState != ScanState.READY_TO_SCAN) return
        scanStarted = true
        recompute()
    }

    /** Retry from an error: re-save a confirmed discovery, or reload the checkpoint. Never duplicates a discovery. */
    fun retry() {
        if (_uiState.value.scanState != ScanState.ERROR) return
        val foundAt = pendingFoundAt
        if (foundAt != null && checkpoint != null) {
            saveDiscovery(foundAt)
        } else {
            val s = _uiState.value
            load(s.gameId, s.checkpointId, force = true)
            if (screenVisible) onScreenStarted()
        }
    }

    /** Proximity fallback: the player pressed and held the shield (only matters once fusion is reached). */
    fun onShieldHeld() {
        if (_uiState.value.scanState != ScanState.WAITING_FOR_PROXIMITY) return
        shieldHeld = true
        recompute()
    }

    /** The reveal was dismissed. */
    fun onRevealDismissed() {
        if (_uiState.value.scanState == ScanState.REVEALING) setState(ScanState.COMPLETED)
    }

    override fun onCleared() {
        stopSession()
        super.onCleared()
    }

    // --- internals ---

    private fun startSignals(cp: Checkpoint) {
        signalsJob?.cancel()
        signalsJob = viewModelScope.launch {
            combine(
                locationRepository.locationState,
                sensorRepository.fusionResult,
                sensorRepository.sensorState
            ) { loc, fusion, sensor -> Triple(loc, fusion, sensor) }
                .collect { (loc, fusion, sensor) ->
                    // Ignore evidence that belongs to another game/checkpoint.
                    lastFusion = fusion.takeIf { it.gameId == cp.gameId && it.checkpointId == cp.id }
                    lastSensor = sensor
                    updateDistanceAndZone(cp, loc)
                    recompute()
                }
        }
    }

    private fun updateDistanceAndZone(cp: Checkpoint, loc: LocationState) {
        val lat = loc.smoothedLatitude
        val lng = loc.smoothedLongitude
        if (!loc.hasValidFix || !loc.servicesEnabled || lat == null || lng == null) {
            _uiState.value = _uiState.value.copy(navigationBearing = null, navigationDistance = null)
            lastDistanceM = null
            zone = QuestZone.FAR
            return
        }
        val d = distanceCalculator.calculateDistanceMeters(lat, lng, cp.lat, cp.lng)
        _uiState.value = _uiState.value.copy(navigationBearing = distanceCalculator.calculateBearingDegrees(lat, lng, cp.lat, cp.lng), navigationDistance = d)
        lastDistanceM = d
        zone = nextQuestZone(zone, d, loc.accuracyMeters)
    }

    private fun recompute() {
        val cp = checkpoint ?: return
        val current = _uiState.value.scanState
        if (!ScanStateMachine.isActive(current)) return

        // Leaving the relic ring closes the scan; the player must wake the Resonator again.
        if (zone != QuestZone.RELIC) { scanStarted = false; shieldHeld = false }

        val fusion = lastFusion
        val signals = ScanSignals(
            hasLocationFix = lastDistanceM != null,
            zone = zone,
            scanStarted = scanStarted,
            fusionThresholdReached = fusion?.thresholdReached == true,
            proximityNear = fusion?.proximityNear == true,
            shieldHeld = shieldHeld
        )
        val next = ScanStateMachine.transition(current, ScanStateMachine.resolve(current, signals))
        _uiState.value = ScanNarrator.hud(
            _uiState.value.copy(scanState = next, zone = zone),
            cp, lastDistanceM, fusion, lastSensor
        )

        // Final gate passed: close the active scan and save, once.
        if (next == ScanState.SUCCESS && !discoveryRequested) {
            discoveryRequested = true
            stopSession()
            val foundAt = clock()
            pendingFoundAt = foundAt
            saveDiscovery(foundAt)
        }
    }

    private fun saveDiscovery(foundAt: Long) {
        val cp = checkpoint ?: return
        val from = _uiState.value.scanState
        if (!ScanStateMachine.canTransition(from, ScanState.SUCCESS)) return
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.SUCCESS,
            isRecordingDiscovery = true,
            error = null
        ).let { ScanNarrator.hud(it, cp, lastDistanceM, lastFusion, lastSensor) }

        viewModelScope.launch {
            val result = try {
                gameRepository.recordDiscovery(cp.gameId, cp.id, foundAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (result.isSuccess) {
                pendingFoundAt = null
                val s = _uiState.value
                _uiState.value = ScanNarrator.hud(
                    s.copy(
                        scanState = ScanStateMachine.transition(s.scanState, ScanState.REVEALING),
                        isRecordingDiscovery = false,
                        reveal = RevealContent(
                            checkpointName = cp.name,
                            clue = cp.clue,
                            lore = cp.lore,
                            rarity = cp.rarity,
                            isFinalCheckpoint = s.isFinalCheckpoint
                        )
                    ),
                    cp, lastDistanceM, lastFusion, lastSensor
                )
            } else {
                _uiState.value = _uiState.value.let {
                    ScanNarrator.hud(
                        it.copy(
                            scanState = ScanStateMachine.transition(it.scanState, ScanState.ERROR),
                            isRecordingDiscovery = false,
                            error = ScanNarrator.saveError()
                        ),
                        cp, lastDistanceM, lastFusion, lastSensor
                    )
                }
            }
        }
    }

    private fun startSession() {
        val cp = checkpoint ?: return
        if (sessionRunning) return
        if (!ScanStateMachine.isActive(_uiState.value.scanState)) return
        sessionRunning = true
        sensorRepository.startScanSession(cp)
        if (permissionGranted) locationRepository.startTracking()
    }

    private fun stopSession() {
        if (!sessionRunning) return
        sessionRunning = false
        sensorRepository.stopScanSession()
        locationRepository.stopTracking()
    }

    private fun setState(state: ScanState) {
        val cp = checkpoint
        val s = _uiState.value
        val next = _uiState.value.copy(scanState = ScanStateMachine.transition(s.scanState, state))
        _uiState.value = if (cp != null) ScanNarrator.hud(next, cp, lastDistanceM, lastFusion, lastSensor)
        else next.copy(headline = headlineFor(next))
    }

    private fun fail(state: ScanState, error: ScanErrorUiState) {
        val s = _uiState.value
        _uiState.value = s.copy(
            scanState = ScanStateMachine.transition(s.scanState, state),
            error = error,
            headline = error.title,
            primaryAction = if (error.retryAvailable) ScanAction.RETRY else ScanAction.BACK
        )
    }

    private fun headlineFor(s: QuestScanUiState) = when (s.scanState) {
        ScanState.UNAVAILABLE -> "Checkpoint unavailable"
        else -> s.headline
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = com.campusquest.CampusQuestApplication.instance
            return QuestScanViewModel(
                gameRepository = AppProviders.gameRepository,
                sensorRepository = app.sensorRepository,
                locationRepository = app.locationRepository,
                // GameRepositoryImpl currently stores progress under this fixed id (see recordDiscovery).
                userId = { com.campusquest.ui.common.AppProviders.authRepository.getCurrentUserId().orEmpty() }
            ) as T
        }
    }
}
