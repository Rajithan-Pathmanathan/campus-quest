package com.campusquest.ui.creator

import com.campusquest.domain.model.Game

/**
 * UI State for the Creator Dashboard screen, tracking drafts in progress,
 * live published quests, creator identity, and aggregate authoring metrics.
 */
data class CreatorDashboardUiState(
    val isLoading: Boolean = false,
    val creatorId: String = "CREATOR_DEFAULT",
    val creatorName: String = "Campus Quest Architect",
    val draftGames: List<Game> = emptyList(),
    val publishedGames: List<Game> = emptyList(),
    val totalCheckpointsCreated: Int = 0,
    val errorMessage: String? = null
) {
    val totalQuests: Int
        get() = draftGames.size + publishedGames.size

    val hasDrafts: Boolean
        get() = draftGames.isNotEmpty()

    val hasPublished: Boolean
        get() = publishedGames.isNotEmpty()
}
