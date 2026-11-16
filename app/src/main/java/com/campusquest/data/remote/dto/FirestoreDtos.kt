package com.campusquest.data.remote.dto

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.PlayerProgress
import com.google.firebase.firestore.PropertyName

/** Firestore documents. Default values are required by Firestore's deserializer. */

data class FirestoreGameDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val status: String = GameStatus.DRAFT.name,
    val checkpointCount: Int = 0,
    val createdAt: Long = 0L,
    val publishedAt: Long? = null,
    /** Light the Way only: the repair-card config other phones need to play it. */
    val story: FirestoreStoryConfigDto? = null
) {
    fun toDomain(): Game = Game(
        id = id,
        title = title,
        description = description,
        creatorId = creatorId,
        creatorName = creatorName,
        status = runCatching { GameStatus.valueOf(status) }.getOrDefault(GameStatus.DRAFT),
        checkpointCount = checkpointCount,
        createdAt = createdAt,
        publishedAt = publishedAt
    )

    companion object {
        fun fromDomain(game: Game, story: FirestoreStoryConfigDto? = null) = FirestoreGameDto(
            id = game.id,
            title = game.title,
            description = game.description,
            creatorId = game.creatorId,
            creatorName = game.creatorName,
            status = game.status.name,
            checkpointCount = game.checkpointCount,
            createdAt = game.createdAt,
            publishedAt = game.publishedAt,
            story = story
        )
    }
}

data class FirestoreStoryConfigDto(
    val cardId: String = "",
    val backupCode: String = "",
    val targetBearing: Float = 0f
)

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

    companion object {
        fun fromDomain(cp: Checkpoint) = FirestoreCheckpointDto(
            id = cp.id,
            gameId = cp.gameId,
            name = cp.name,
            lat = cp.lat,
            lng = cp.lng,
            radiusM = cp.radiusM,
            minLux = cp.lightSignature.minLux,
            maxLux = cp.lightSignature.maxLux,
            clue = cp.clue,
            lore = cp.lore,
            order = cp.order,
            motionType = cp.motionType,
            rarity = cp.rarity
        )
    }
}

data class FirestoreProgressDto(
    val gameId: String = "",
    val userId: String = "",
    val completedCheckpointIds: List<String> = emptyList(),
    val currentCheckpointOrder: Int = 1,
    val score: Int = 0,
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    // Without these annotations Firestore stores a Kotlin "isCompleted" Boolean as "completed".
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false
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
        fun fromDomain(p: PlayerProgress) = FirestoreProgressDto(
            gameId = p.gameId,
            userId = p.userId,
            completedCheckpointIds = p.completedCheckpointIds,
            currentCheckpointOrder = p.currentCheckpointOrder,
            score = p.score,
            startedAt = p.startedAt,
            completedAt = p.completedAt,
            isCompleted = p.isCompleted
        )
    }
}

data class FirestoreLeaderboardEntryDto(
    val userId: String = "",
    val userName: String = "",
    val score: Int = 0,
    val completionTimeSeconds: Long = 0L,
    val completedAt: Long = 0L
) {
    fun toDomain(rank: Int): GameLeaderboardEntry =
        GameLeaderboardEntry(userId, userName, score, completionTimeSeconds, completedAt, rank)

    companion object {
        fun fromDomain(e: GameLeaderboardEntry) =
            FirestoreLeaderboardEntryDto(e.userId, e.userName, e.score, e.completionTimeSeconds, e.completedAt)
    }
}

data class FirestoreUserDto(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    @get:PropertyName("isAnonymous") @set:PropertyName("isAnonymous")
    var isAnonymous: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDomain(user: AuthUser) = FirestoreUserDto(user.uid, user.displayName, user.email, user.isAnonymous)
    }
}
