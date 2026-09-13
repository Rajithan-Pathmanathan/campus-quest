package com.campusquest.ui.creator

import com.campusquest.domain.model.Checkpoint

/**
 * UI State for the Create/Edit Quest Wizard and Checkpoint Sequence Builder.
 */
data class CreateGameUiState(
    val isLoading: Boolean = false,
    val gameId: String = "",
    val title: String = "",
    val description: String = "",
    val creatorId: String = "CREATOR_PROF_ALAN",
    val creatorName: String = "Prof. Alan Turing",
    val checkpoints: List<Checkpoint> = emptyList(),
    val isDraftSaved: Boolean = false,
    val isPublished: Boolean = false,
    val errorMessage: String? = null
) {
    val checkpointCount: Int
        get() = checkpoints.size

    val canPublish: Boolean
        get() = title.isNotBlank() && description.isNotBlank() && checkpoints.size >= 2

    val validationMessage: String?
        get() = when {
            title.isBlank() -> "Quest title is required."
            description.isBlank() -> "Quest description is required."
            checkpoints.isEmpty() -> "Add at least 2 checkpoints to publish."
            checkpoints.size == 1 -> "Add 1 more checkpoint (minimum 2 required to publish)."
            else -> null
        }
}
