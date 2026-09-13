package com.campusquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val status: GameStatus,
    val checkpointCount: Int,
    val createdAt: Long,
    val publishedAt: Long?
) {
    fun toDomain(): Game = Game(
        id = id,
        title = title,
        description = description,
        creatorId = creatorId,
        creatorName = creatorName,
        status = status,
        checkpointCount = checkpointCount,
        createdAt = createdAt,
        publishedAt = publishedAt
    )

    companion object {
        fun fromDomain(game: Game): GameEntity = GameEntity(
            id = game.id,
            title = game.title,
            description = game.description,
            creatorId = game.creatorId,
            creatorName = game.creatorName,
            status = game.status,
            checkpointCount = game.checkpointCount,
            createdAt = game.createdAt,
            publishedAt = game.publishedAt
        )
    }
}
