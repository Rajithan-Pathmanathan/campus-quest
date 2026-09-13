package com.campusquest.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusquest.domain.fusion.FusionConfig
import com.campusquest.domain.fusion.FusionInputs
import com.campusquest.domain.fusion.GeneralizedFusionCalculator
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the Live Scan HUD, multi-sensor signal normalization,
 * weighted fusion progression, and proximity gate confirmation.
 */
class QuestScanViewModel(
    private val fusionCalculator: GeneralizedFusionCalculator = GeneralizedFusionCalculator(
        FusionConfig(
            gpsWeight = 0.40f,
            lightWeight = 0.30f,
            motionWeight = 0.30f,
            threshold = 0.85f
        )
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestScanUiState())
    val uiState: StateFlow<QuestScanUiState> = _uiState.asStateFlow()

    fun loadScanContext(
        gameId: String,
        checkpointId: String,
        customCheckpoint: Checkpoint? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanState = ScanState.ENTERING, gameId = gameId, checkpointId = checkpointId) }

            val cp = customCheckpoint ?: getSampleCheckpoint(gameId, checkpointId)

            _uiState.update {
                it.copy(
                    scanState = ScanState.READY,
                    checkpoint = cp,
                    gpsScore = 1.0f // Initial default within geofence
                )
            }

            // Move to scanning
            _uiState.update { it.copy(scanState = ScanState.SCANNING) }
            recompute()
        }
    }

    fun onGpsScoreChanged(score: Float) {
        _uiState.update { it.copy(gpsScore = score.coerceIn(0f, 1f)) }
        recompute()
    }

    fun onLightReadingChanged(lux: Float) {
        _uiState.update { current ->
            val cp = current.checkpoint
            val score = if (cp != null && cp.lightSignature.matches(lux)) {
                1.0f
            } else if (cp != null) {
                val min = cp.lightSignature.minLux
                val max = cp.lightSignature.maxLux
                if (lux < min) {
                    (lux / min).coerceIn(0f, 0.9f)
                } else {
                    (max / lux).coerceIn(0f, 0.9f)
                }
            } else {
                0.5f
            }
            current.copy(lightScore = score, currentLux = lux)
        }
        recompute()
    }

    fun onMotionDetected(detected: Boolean) {
        _uiState.update {
            it.copy(
                motionDetected = detected,
                motionScore = if (detected) 1.0f else 0.0f
            )
        }
        recompute()
    }

    fun onProximityChanged(isNear: Boolean) {
        _uiState.update { it.copy(proximityNear = isNear) }
        recompute()
    }

    fun claimDiscovery(): Boolean {
        val state = _uiState.value
        return if (state.canClaimDiscovery) {
            _uiState.update {
                it.copy(
                    scanState = ScanState.DISCOVERED,
                    isDiscoveryClaimed = true
                )
            }
            true
        } else {
            false
        }
    }

    private fun recompute() {
        _uiState.update { current ->
            val inputs = FusionInputs(
                gpsScore = current.gpsScore,
                lightScore = current.lightScore,
                motionScore = current.motionScore
            )

            val result = fusionCalculator.calculate(
                gameId = current.gameId,
                checkpointId = current.checkpointId,
                inputs = inputs,
                proximityNear = current.proximityNear
            )

            val nextState = when {
                current.isDiscoveryClaimed -> ScanState.DISCOVERED
                result.canClaimDiscovery -> ScanState.REVEAL_READY
                result.thresholdReached -> ScanState.FUSION_THRESHOLD_REACHED
                else -> ScanState.SCANNING
            }

            current.copy(
                fusionResult = result,
                scanState = nextState
            )
        }
    }

    private fun getSampleCheckpoint(gameId: String, checkpointId: String): Checkpoint {
        return Checkpoint(
            id = checkpointId,
            gameId = gameId,
            name = "Historic Clock Tower",
            lat = 6.9360,
            lng = 79.8436,
            radiusM = 25f,
            lightSignature = LightSignature(150f, 800f),
            clue = "Look skyward where the bell chimes above the quad.",
            lore = "Erected in 1857 as a lighthouse and clock tower.",
            order = 1,
            motionType = "SWEEP"
        )
    }
}
