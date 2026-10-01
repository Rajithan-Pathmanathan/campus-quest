package com.campusquest.device.sensor

import android.content.Context
import com.campusquest.domain.fusion.DiscoveryGateEvaluator
import com.campusquest.domain.fusion.FusionConfig
import com.campusquest.domain.fusion.FusionInputs
import com.campusquest.domain.fusion.GeneralizedFusionCalculator
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Enterprise Multi-Sensor Fusion Orchestrator for Campus Quest.
 * Coordinates hardware/simulated sensor streams with the pure GeneralizedFusionCalculator.
 */
class SensorFusionEngine(
    private val signalSource: SensorSignalSource,
    private val fusionCalculator: GeneralizedFusionCalculator = GeneralizedFusionCalculator(FusionConfig()),
    private val discoveryEvaluator: DiscoveryGateEvaluator = DiscoveryGateEvaluator(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) {

    /**
     * Convenience constructor creating default real hardware source from Android Context.
     */
    constructor(
        context: Context,
        config: FusionConfig = FusionConfig()
    ) : this(
        signalSource = RealSensorSignalSource(context),
        fusionCalculator = GeneralizedFusionCalculator(config),
        discoveryEvaluator = DiscoveryGateEvaluator()
    )

    private var currentGameId: String = ""
    private var currentCheckpointId: String = ""
    private var currentGpsScore: Float = 1.0f

    val sensorState: StateFlow<SensorState> = signalSource.sensorState

    private val _fusionResult = MutableStateFlow(
        FusionResult(
            gameId = "",
            checkpointId = "",
            gpsScore = 1.0f,
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

    private var observationJob: Job? = null

    init {
        startObservation()
    }

    private fun startObservation() {
        observationJob?.cancel()
        observationJob = scope.launch {
            signalSource.sensorState.collectLatest { state ->
                recalculate(state)
            }
        }
    }

    /**
     * Starts listening when entering the Scan HUD.
     */
    fun startListening(
        gameId: String,
        checkpointId: String,
        targetSignature: LightSignature,
        motionType: MotionType = MotionType.SWEEP,
        gpsScore: Float = 1.0f
    ) {
        this.currentGameId = gameId
        this.currentCheckpointId = checkpointId
        this.currentGpsScore = gpsScore

        signalSource.startListening(targetSignature, motionType)
        recalculate(signalSource.sensorState.value)
    }

    /**
     * Lifecycle-safe stop when exiting Scan HUD.
     */
    fun stopListening() {
        signalSource.stopListening()
    }

    fun updateGpsScore(score: Float) {
        this.currentGpsScore = score.coerceIn(0.0f, 1.0f)
        recalculate(signalSource.sensorState.value)
    }

    fun reset() {
        signalSource.reset()
        recalculate(signalSource.sensorState.value)
    }

    private fun recalculate(state: SensorState) {
        val inputs = FusionInputs.fromBooleans(
            gpsScore = currentGpsScore,
            lightMatched = state.lightMatched,
            motionDetected = state.motionDetected
        )

        val result = fusionCalculator.calculate(
            gameId = currentGameId,
            checkpointId = currentCheckpointId,
            inputs = inputs,
            proximityNear = state.isNear ?: false
        )

        _fusionResult.value = result
    }
}
