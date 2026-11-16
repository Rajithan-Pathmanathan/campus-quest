package com.campusquest.device.sensor

import android.content.Context
import com.campusquest.domain.fusion.FusionConfig
import com.campusquest.domain.fusion.FusionInputs
import com.campusquest.domain.fusion.GeneralizedFusionCalculator
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Multi-sensor fusion orchestrator for Campus Quest.
 * Coordinates hardware/simulated sensor streams with the pure GeneralizedFusionCalculator.
 *
 * MVP missing-hardware policy (workplan §26 / §56):
 *   - Light or accelerometer missing → channel excluded, remaining weights re-normalized.
 *   - Proximity missing (isNear == null) → final gate is bypassed (treated as satisfied).
 *   - GPS has no fix → gpsScore 0.0 (see GpsScoreCalculator); GPS is never re-normalized away.
 *
 * One instance per app (owned by SensorRepositoryImpl). Call [close] to release its coroutine.
 */
class SensorFusionEngine(
    private val signalSource: SensorSignalSource,
    private val fusionCalculator: GeneralizedFusionCalculator = GeneralizedFusionCalculator(FusionConfig()),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    constructor(
        context: Context,
        config: FusionConfig = FusionConfig()
    ) : this(
        signalSource = RealSensorSignalSource(context),
        fusionCalculator = GeneralizedFusionCalculator(config)
    )

    private var currentGameId: String = ""
    private var currentCheckpointId: String = ""
    private var currentSignature: LightSignature? = null

    // 0.0 until a real location update arrives: a missing update must never grant GPS credit.
    private var currentGpsScore: Float = 0.0f

    val sensorState: StateFlow<SensorState> = signalSource.sensorState

    private val _fusionResult = MutableStateFlow(
        FusionResult(
            gameId = "",
            checkpointId = "",
            gpsScore = 0.0f,
            lightScore = null,
            motionScore = null,
            totalScore = 0.0f,
            progressPercent = 0,
            thresholdReached = false,
            proximityNear = false,
            activeSignals = emptySet()
        )
    )
    val fusionResult: StateFlow<FusionResult> = _fusionResult.asStateFlow()

    private val observationJob = scope.launch {
        signalSource.sensorState.collectLatest { state -> recalculate(state) }
    }

    /**
     * Starts a scan session for one checkpoint. Re-scopes all state so nothing leaks
     * from the previous checkpoint or game (workplan §41–§43).
     */
    fun startListening(
        gameId: String,
        checkpointId: String,
        targetSignature: LightSignature,
        motionType: MotionType = MotionType.SWEEP,
        gpsScore: Float = 0.0f
    ) {
        currentGameId = gameId
        currentCheckpointId = checkpointId
        currentSignature = targetSignature
        currentGpsScore = gpsScore.coerceIn(0.0f, 1.0f)

        signalSource.startListening(targetSignature, motionType)
        recalculate(signalSource.sensorState.value)
    }

    fun stopListening() {
        signalSource.stopListening()
    }

    fun updateGpsScore(score: Float) {
        currentGpsScore = score.coerceIn(0.0f, 1.0f)
        recalculate(signalSource.sensorState.value)
    }

    fun reset() {
        signalSource.reset()
        recalculate(signalSource.sensorState.value)
    }

    /** Stops sensors and cancels the observation coroutine. The engine is unusable afterwards. */
    fun close() {
        signalSource.stopListening()
        observationJob.cancel()
    }

    private fun recalculate(state: SensorState) {
        val lightScore: Float? = when {
            state.lightMatched == null -> null // sensor unavailable → re-normalize
            currentSignature != null && state.currentLux != null -> currentSignature!!.score(state.currentLux)
            else -> if (state.lightMatched) 1.0f else 0.0f
        }

        val inputs = FusionInputs(
            gpsScore = currentGpsScore,
            lightScore = lightScore,
            motionScore = state.motionDetected?.let { if (it) 1.0f else 0.0f }
        )

        _fusionResult.value = fusionCalculator.calculate(
            gameId = currentGameId,
            checkpointId = currentCheckpointId,
            inputs = inputs,
            proximityNear = state.isNear ?: true // no proximity sensor → gate bypassed (MVP policy)
        )
    }
}
