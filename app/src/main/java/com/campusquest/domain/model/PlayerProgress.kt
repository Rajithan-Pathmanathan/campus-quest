package com.campusquest.domain.model

data class PlayerProgress(
    val gameId: String,
    val userId: String,
    val completedCheckpointIds: List<String> = emptyList(),
    val currentCheckpointOrder: Int = 1,
    val score: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isCompleted: Boolean = false
)
