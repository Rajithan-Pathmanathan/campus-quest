package com.campusquest.ui.quest

import com.campusquest.domain.quest.QuestZone

/**
 * Explicit scan state machine (M4 workplan §17-18).
 *
 * Active (signal-driven) states move freely among each other as the player walks and plays:
 *   WAITING_FOR_LOCATION <-> OUTSIDE_CHECKPOINT <-> READY_TO_SCAN -> SCANNING <-> WAITING_FOR_PROXIMITY
 * Discovery then flows one way:
 *   SCANNING / WAITING_FOR_PROXIMITY -> SUCCESS -> REVEALING -> COMPLETED
 * Anything that goes wrong (load failure, save failure) is ERROR, which can retry.
 *
 * Fusion threshold is NOT discovery: WAITING_FOR_PROXIMITY only becomes SUCCESS when M3 also reports
 * the proximity final gate as passed.
 */
enum class ScanState {
    LOADING,
    UNAVAILABLE,
    ALREADY_FOUND,
    WAITING_FOR_LOCATION,
    OUTSIDE_CHECKPOINT,
    READY_TO_SCAN,
    SCANNING,
    WAITING_FOR_PROXIMITY,
    SUCCESS,
    REVEALING,
    COMPLETED,
    ERROR
}

/** What the scan screen's primary button does in the current state. */
enum class ScanAction { NONE, BEGIN_SCAN, GRANT_LOCATION, RETRY, BACK }

/** Plain inputs the state machine needs. No raw sensor values, no fusion maths. */
data class ScanSignals(
    val hasLocationFix: Boolean,
    val zone: QuestZone,
    val scanStarted: Boolean,
    /** M3: weighted fusion (GPS + light + motion) >= threshold. */
    val fusionThresholdReached: Boolean,
    /** M3: proximity final gate. Not a weighted input. */
    val proximityNear: Boolean
)

object ScanStateMachine {

    private val ACTIVE = setOf(
        ScanState.WAITING_FOR_LOCATION,
        ScanState.OUTSIDE_CHECKPOINT,
        ScanState.READY_TO_SCAN,
        ScanState.SCANNING,
        ScanState.WAITING_FOR_PROXIMITY
    )

    /** Signals never move the machine out of these. */
    private val LATCHED = ScanState.entries.toSet() - ACTIVE

    private val SCAN_STARTED = setOf(ScanState.SCANNING, ScanState.WAITING_FOR_PROXIMITY)

    fun isActive(state: ScanState): Boolean = state in ACTIVE

    /** Where the signals say we should be. Latched states (success, reveal, error...) never change here. */
    fun resolve(current: ScanState, signals: ScanSignals): ScanState {
        if (current in LATCHED) return current
        return when {
            !signals.hasLocationFix -> ScanState.WAITING_FOR_LOCATION
            signals.zone != QuestZone.RELIC -> ScanState.OUTSIDE_CHECKPOINT
            !signals.scanStarted -> ScanState.READY_TO_SCAN
            signals.fusionThresholdReached && signals.proximityNear -> ScanState.SUCCESS
            signals.fusionThresholdReached -> ScanState.WAITING_FOR_PROXIMITY
            else -> ScanState.SCANNING
        }
    }

    fun canTransition(from: ScanState, to: ScanState): Boolean {
        if (from == to) return true
        return when (from) {
            ScanState.LOADING ->
                to in ACTIVE - SCAN_STARTED ||
                    to == ScanState.UNAVAILABLE || to == ScanState.ALREADY_FOUND || to == ScanState.ERROR
            // The scan must be begun explicitly before it can run.
            ScanState.WAITING_FOR_LOCATION, ScanState.OUTSIDE_CHECKPOINT ->
                to in ACTIVE - SCAN_STARTED || to == ScanState.ERROR
            ScanState.READY_TO_SCAN -> to in ACTIVE || to == ScanState.ERROR
            ScanState.SCANNING, ScanState.WAITING_FOR_PROXIMITY ->
                to in ACTIVE || to == ScanState.SUCCESS || to == ScanState.ERROR
            ScanState.SUCCESS -> to == ScanState.REVEALING || to == ScanState.ERROR
            ScanState.REVEALING -> to == ScanState.COMPLETED
            // Retry: re-save a confirmed discovery, or reload the checkpoint.
            ScanState.ERROR -> to == ScanState.SUCCESS || to == ScanState.LOADING
            ScanState.UNAVAILABLE, ScanState.ALREADY_FOUND, ScanState.COMPLETED -> false
        }
    }

    /** Moves to [to] when allowed, otherwise stays in [from]. */
    fun transition(from: ScanState, to: ScanState): ScanState =
        if (canTransition(from, to)) to else from
}
