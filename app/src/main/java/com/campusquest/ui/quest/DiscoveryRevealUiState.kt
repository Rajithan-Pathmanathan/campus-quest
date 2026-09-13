package com.campusquest.ui.quest

import com.campusquest.domain.model.Checkpoint

/**
 * UI State for the Checkpoint Lore & Discovery Reveal Modal Dialog.
 */
data class DiscoveryRevealUiState(
    val isLoading: Boolean = false,
    val gameId: String = "",
    val checkpointId: String = "",
    val checkpointName: String = "",
    val clue: String = "",
    val lore: String = "",
    val order: Int = 1,
    val totalCheckpoints: Int = 3,
    val pointsAwarded: Int = 100,
    val isFinalCheckpoint: Boolean = false
) {
    val sequenceBadge: String
        get() = "WAYPOINT $order OF $totalCheckpoints"
}
