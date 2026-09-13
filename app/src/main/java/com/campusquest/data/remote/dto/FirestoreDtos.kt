package com.campusquest.data.remote.dto

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature

data class FirestoreGameDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val status: String = GameStatus.DRAFT.name,
    val checkpointCount: Int = 0,
    val createdAt: Long = 0L,
    val publishedAt: Long? = null
) {
    fun toDomain(): Game = Game(
        id = id,
        title = title,
        description = description,
        creatorId = creatorId,
        creatorName = creatorName,
        status = try { GameStatus.valueOf(status) } catch (e: Exception) { GameStatus.DRAFT },
        checkpointCount = checkpointCount,
        createdAt = createdAt,
        publishedAt = publishedAt
    )
}

data class FirestoreCheckpointDto(
    val id: String = "",
    val gameId: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radiusM: Float = 20f,
    val minLux: Float = 0f,
    val maxLux: Float = 1000f,
    val clue: String = "",
    val lore: String = "",
    val order: Int = 1,
    val motionType: String = "SWEEP",
    val rarity: String = "COMMON"
) {
    fun toDomain(): Checkpoint = Checkpoint(
        id = id,
        gameId = gameId,
        name = name,
        lat = lat,
        lng = lng,
        radiusM = radiusM,
        lightSignature = LightSignature(minLux, maxLux),
        clue = clue,
        lore = lore,
        order = order,
        motionType = motionType,
        rarity = rarity
    )
}

data class FirestoreProgressDto(
    val gameId: String = "",
    val userId: String = "",
    val completedCheckpointIds: List<String> = emptyList(),
    val currentCheckpointOrder: Int = 1,
    val score: Int = 0,
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    val isCompleted: Boolean = false
)
