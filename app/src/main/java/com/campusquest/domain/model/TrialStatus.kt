package com.campusquest.domain.model

/**
 * Progress of a chapter's motion trial (3 shakes, 3 sweeps, or holding the water jar level).
 * Internal values only — the UI turns this into story words, never numbers or percentages.
 */
data class TrialStatus(
    val completed: Boolean = false,
    /** Gestures still needed (repeat trials); 0 for hold trials. */
    val remaining: Int = 0,
    /** Hold trial: the player has started holding steady. */
    val started: Boolean = false,
    /** Hold trial: the jar tipped since the last steady stretch began. */
    val justSpilled: Boolean = false,
    /** Hold trial: 0..1 how long it has been held, for a visual (e.g. water settling). */
    val holdFraction: Float = 0f
)
