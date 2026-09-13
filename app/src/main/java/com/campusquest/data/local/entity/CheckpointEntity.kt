package com.campusquest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature

@Entity(
    tableName = "checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class CheckpointEntity(
    @PrimaryKey
    val id: String,
    val gameId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val minLux: Float,
    val maxLux: Float,
    val clue: String,
    val lore: String,
    val orderIndex: Int,
    val motionType: String,
    val rarity: String
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
        order = orderIndex,
        motionType = motionType,
        rarity = rarity
    )

    companion object {
        fun fromDomain(checkpoint: Checkpoint): CheckpointEntity = CheckpointEntity(
            id = checkpoint.id,
            gameId = checkpoint.gameId,
            name = checkpoint.name,
            lat = checkpoint.lat,
            lng = checkpoint.lng,
            radiusM = checkpoint.radiusM,
            minLux = checkpoint.lightSignature.minLux,
            maxLux = checkpoint.lightSignature.maxLux,
            clue = checkpoint.clue,
            lore = checkpoint.lore,
            orderIndex = checkpoint.order,
            motionType = checkpoint.motionType,
            rarity = checkpoint.rarity
        )
    }
}
