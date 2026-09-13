package com.campusquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campusquest.domain.model.PlayerProgress

@Entity(
    tableName = "player_progress",
    primaryKeys = ["gameId", "userId"]
)
data class PlayerProgressEntity(
    val gameId: String,
    val userId: String,
    val completedCheckpointIds: List<String>,
    val currentCheckpointOrder: Int,
    val score: Int,
    val startedAt: Long,
    val completedAt: Long?,
    val isCompleted: Boolean
) {
    fun toDomain(): PlayerProgress = PlayerProgress(
        gameId = gameId,
        userId = userId,
        completedCheckpointIds = completedCheckpointIds,
        currentCheckpointOrder = currentCheckpointOrder,
        score = score,
        startedAt = startedAt,
        completedAt = completedAt,
        isCompleted = isCompleted
    )

    companion object {
        fun fromDomain(progress: PlayerProgress): PlayerProgressEntity = PlayerProgressEntity(
            gameId = progress.gameId,
            userId = progress.userId,
            completedCheckpointIds = progress.completedCheckpointIds,
            currentCheckpointOrder = progress.currentCheckpointOrder,
            score = progress.score,
            startedAt = progress.startedAt,
            completedAt = progress.completedAt,
            isCompleted = progress.isCompleted
        )
    }
}
