package com.campusquest.domain.model

data class GameLeaderboardEntry(
    val userId: String,
    val userName: String,
    val score: Int,
    val completionTimeSeconds: Long,
    val completedAt: Long,
    val rank: Int = 0
)
