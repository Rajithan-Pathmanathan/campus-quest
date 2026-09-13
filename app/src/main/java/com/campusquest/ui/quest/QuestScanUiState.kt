package com.campusquest.ui.quest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult

/**
 * Lifecycle states of the live physical scanning gameplay machine.
 */
enum class ScanState {
    ENTERING,                  // Initializing sensors and camera reticle
    READY,                     // Waypoint signature loaded
    SCANNING,                  // Telemetry active, evaluating fusion
    FUSION_THRESHOLD_REACHED,  // Composite fusion >= 85%, awaiting close physical proximity
    PROXIMITY_GATE_PASSED,     // Proximity sensor near confirmed
    REVEAL_READY,              // Ready to claim relic discovery
    DISCOVERED                 // Relic claimed and saved
}

/**
 * UI State for the live AR / Cyberpunk Quest Scan HUD.
 */
data class QuestScanUiState(
    val scanState: ScanState = ScanState.ENTERING,
    val gameId: String = "",
    val checkpointId: String = "",
    val checkpoint: Checkpoint? = null,
    val gpsScore: Float = 0.0f,
    val lightScore: Float = 0.0f,
    val motionScore: Float = 0.0f,
    val currentLux: Float? = null,
    val motionDetected: Boolean = false,
    val proximityNear: Boolean = false,
    val fusionResult: FusionResult? = null,
    val isDiscoveryClaimed: Boolean = false,
    val errorMessage: String? = null
) {
    val progressPercent: Int
        get() = fusionResult?.progressPercent ?: 0

    val thresholdReached: Boolean
        get() = fusionResult?.thresholdReached ?: false

    val canClaimDiscovery: Boolean
        get() = fusionResult?.canClaimDiscovery ?: false

    val statusBadgeText: String
        get() = when (scanState) {
            ScanState.ENTERING -> "INITIALIZING SCANNER"
            ScanState.READY -> "ALIGNING SENSORS"
            ScanState.SCANNING -> "SCANNING SIGNALS (${progressPercent}%)"
            ScanState.FUSION_THRESHOLD_REACHED -> "APPROACH CLOSER FOR CONFIRMATION"
            ScanState.PROXIMITY_GATE_PASSED -> "RELIC RESONANCE 100%"
            ScanState.REVEAL_READY -> "READY TO CLAIM DISCOVERY"
            ScanState.DISCOVERED -> "DISCOVERY CLAIMED"
        }
}
