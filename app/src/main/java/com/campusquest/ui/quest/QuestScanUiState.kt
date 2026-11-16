package com.campusquest.ui.quest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorState
import com.campusquest.domain.quest.EmberGlow
import com.campusquest.domain.quest.QuestCues
import com.campusquest.domain.quest.QuestZone

/** How far along one physical signal is. Shown as a word and a colour, never a number. */
enum class SignalLevel { WAITING, CLOSE, MATCHED }

/** One of the three signal chips (GPS, light, motion) on the scan screen. */
data class SignalChip(val status: String, val level: SignalLevel)

data class ScanErrorUiState(
    val title: String,
    val message: String,
    val retryAvailable: Boolean
)

/** What the reveal shows once the discovery is saved ("Memory restored"). */
data class RevealContent(
    val checkpointName: String,
    val clue: String,
    val lore: String,
    val rarity: String,
    val isFinalCheckpoint: Boolean,
    /** Points the repository awards for a first discovery. */
    val points: Int = POINTS_PER_CHECKPOINT
) {
    companion object {
        const val POINTS_PER_CHECKPOINT = 100
    }
}

/**
 * Everything the scan screen draws. The player never sees raw numbers (no fusion %, GPS score or
 * lux): only M3's story words. The screen derives nothing itself.
 */
data class QuestScanUiState(
    val navigationBearing: Float? = null,
    val navigationDistance: Float? = null,
    val gameId: String = "",
    val checkpointId: String = "",
    val checkpointName: String = "",
    val scanState: ScanState = ScanState.LOADING,
    val zone: QuestZone = QuestZone.FAR,
    val headline: String = "Loading checkpoint…",
    val distanceText: String? = null,
    val warmth: String? = null,
    /** Shown only while scanning; coarse stage of the ember, not a percentage. */
    val glow: EmberGlow? = null,
    val lightHint: String? = null,
    val trialHint: String? = null,
    val shieldHint: String? = null,
    val heartbeatIntervalMs: Long? = null,
    val showSignals: Boolean = false,
    val gps: SignalChip = SignalChip("Searching", SignalLevel.WAITING),
    val light: SignalChip = SignalChip("Waiting", SignalLevel.WAITING),
    val motion: SignalChip = SignalChip("Ready", SignalLevel.WAITING),
    val primaryAction: ScanAction = ScanAction.NONE,
    val permissionDenied: Boolean = false,
    val isRecordingDiscovery: Boolean = false,
    val isFinalCheckpoint: Boolean = false,
    val reveal: RevealContent? = null,
    val error: ScanErrorUiState? = null
)

/** Turns state + M3 signals into story copy. Pure, so it is unit-testable. */
object ScanNarrator {

    const val SEARCHING_FOR_FIX = "Finding your position… check that location is on."
    const val NEEDS_PERMISSION = "The Resonator needs your location to find the ember."

    fun unavailableError() = ScanErrorUiState(
        title = "Checkpoint unavailable",
        message = "Return to the quest and try again.",
        retryAvailable = false
    )

    fun closedError() = ScanErrorUiState(
        title = "Scan unavailable",
        message = "This game is no longer active.",
        retryAvailable = false
    )

    fun loadError() =ScanErrorUiState(
        title = "Couldn't load this chapter",
        message = "Check your connection and try again.",
        retryAvailable = true
    )

    fun saveError() = ScanErrorUiState(
        title = "The memory slipped away",
        message = "Your discovery is confirmed but could not be saved yet. Try again.",
        retryAvailable = true
    )

    /** GPS, light and motion as short words (workplan 9-12). Raw values never reach the screen. */
    fun signals(
        state: QuestScanUiState,
        checkpoint: Checkpoint,
        distanceM: Float?,
        sensor: SensorState?
    ): Triple<SignalChip, SignalChip, SignalChip> {
        val gps = when {
            distanceM == null -> SignalChip("Searching", SignalLevel.WAITING)
            state.zone == QuestZone.RELIC -> SignalChip("Locked", SignalLevel.MATCHED)
            state.zone == QuestZone.FAR -> SignalChip("Far", SignalLevel.WAITING)
            else -> SignalChip("Nearby", SignalLevel.CLOSE)
        }
        val lux = sensor?.currentLux
        val light = when {
            lux == null -> SignalChip("Waiting", SignalLevel.WAITING)
            checkpoint.lightSignature.matches(lux) -> SignalChip("Match", SignalLevel.MATCHED)
            checkpoint.lightSignature.score(lux) >= 0.4f -> SignalChip("Almost", SignalLevel.CLOSE)
            else -> SignalChip("Off", SignalLevel.WAITING)
        }
        val trial = sensor?.trialStatus
        val motionType = MotionType.fromString(checkpoint.motionType)
        val started = trial != null && (trial.started || (motionType != MotionType.TILT && trial.remaining in 1..2))
        val motion = when {
            sensor?.motionDetected == true || trial?.completed == true -> SignalChip("Done", SignalLevel.MATCHED)
            started -> SignalChip("In progress", SignalLevel.CLOSE)
            else -> SignalChip("Ready", SignalLevel.WAITING)
        }
        return Triple(gps, light, motion)
    }

    /** Fills in all story text for [state]. Distance is the player's distance to the checkpoint, if known. */
    fun hud(
        state: QuestScanUiState,
        checkpoint: Checkpoint,
        distanceM: Float?,
        fusion: FusionResult?,
        sensor: SensorState?
    ): QuestScanUiState {
        val s = state.scanState
        val scanning = s == ScanState.SCANNING || s == ScanState.WAITING_FOR_PROXIMITY
        val approaching = s == ScanState.OUTSIDE_CHECKPOINT
        val motionType = MotionType.fromString(checkpoint.motionType)

        val headline = when (s) {
            ScanState.LOADING -> "Loading checkpoint…"
            ScanState.UNAVAILABLE -> "Checkpoint unavailable"
            ScanState.ALREADY_FOUND -> "You already restored this memory."
            ScanState.WAITING_FOR_LOCATION ->
                if (state.permissionDenied) NEEDS_PERMISSION else SEARCHING_FOR_FIX
            ScanState.OUTSIDE_CHECKPOINT -> QuestCues.zoneLine(state.zone)
            ScanState.READY_TO_SCAN -> "The ember appears. Wake the Resonator when you're ready."
            ScanState.SCANNING ->
                fusion?.let { QuestCues.glow(it.progressPercent).label } ?: EmberGlow.COLD.label
            ScanState.WAITING_FOR_PROXIMITY -> "The signals are aligned! Almost there…"
            ScanState.SUCCESS -> "Memory restored… saving your discovery."
            ScanState.REVEALING, ScanState.COMPLETED -> "Memory restored!"
            ScanState.ERROR -> state.error?.title ?: "Something went wrong"
        }

        val (gps, light, motion) = signals(state, checkpoint, distanceM, sensor)
        val showSignals = s != ScanState.LOADING && s != ScanState.UNAVAILABLE && s != ScanState.ERROR

        return state.copy(
            showSignals = showSignals,
            gps = gps,
            light = light,
            motion = motion,
            headline = headline,
            distanceText = if (approaching) distanceM?.let(QuestCues::distanceText) else null,
            warmth = if (approaching) distanceM?.let(QuestCues::warmth) else null,
            glow = if (scanning && fusion != null) QuestCues.glow(fusion.progressPercent) else null,
            lightHint = if (scanning) QuestCues.lightHint(checkpoint.lightSignature, sensor?.currentLux) else null,
            trialHint = if (scanning) QuestCues.trialHint(motionType, sensor?.trialStatus) else null,
            shieldHint = if (s == ScanState.WAITING_FOR_PROXIMITY) QuestCues.shieldHint(near = false) else null,
            heartbeatIntervalMs = if (approaching && distanceM != null)
                QuestCues.heartbeatIntervalMs(state.zone, distanceM) else null,
            primaryAction = when (s) {
                ScanState.READY_TO_SCAN -> ScanAction.BEGIN_SCAN
                ScanState.WAITING_FOR_LOCATION ->
                    if (state.permissionDenied) ScanAction.GRANT_LOCATION else ScanAction.NONE
                ScanState.ERROR -> if (state.error?.retryAvailable == true) ScanAction.RETRY else ScanAction.BACK
                ScanState.UNAVAILABLE, ScanState.ALREADY_FOUND -> ScanAction.BACK
                else -> ScanAction.NONE
            }
        )
    }
}
