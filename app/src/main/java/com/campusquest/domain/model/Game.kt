package com.campusquest.domain.model

data class Game(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val status: GameStatus = GameStatus.DRAFT,
    val checkpointCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null
)
