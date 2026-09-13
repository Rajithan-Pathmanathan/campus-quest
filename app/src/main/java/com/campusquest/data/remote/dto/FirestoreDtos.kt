package com.campusquest.data.remote.dto

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.PlayerProgress

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

    companion object {
        fun fromDomain(game: Game): FirestoreGameDto = FirestoreGameDto(
            id = game.id,
            title = game.title,
            description = game.description,
            creatorId = game.creatorId,
            creatorName = game.creatorName,
            status = game.status.name,
            checkpointCount = game.checkpointCount,
            createdAt = game.createdAt,
            publishedAt = game.publishedAt
        )
    }
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

    companion object {
        fun fromDomain(checkpoint: Checkpoint): FirestoreCheckpointDto = FirestoreCheckpointDto(
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
            order = checkpoint.order,
            motionType = checkpoint.motionType,
            rarity = checkpoint.rarity
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
    val isCompleted: Boolean = false
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
        fun fromDomain(progress: PlayerProgress): FirestoreProgressDto = FirestoreProgressDto(
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

data class FirestoreLeaderboardEntryDto(
    val userId: String = "",
    val userName: String = "",
    val score: Int = 0,
    val completionTimeSeconds: Long = 0L,
    val completedAt: Long = 0L,
    val rank: Int = 0
) {
    fun toDomain(): GameLeaderboardEntry = GameLeaderboardEntry(
        userId = userId,
        userName = userName,
        score = score,
        completionTimeSeconds = completionTimeSeconds,
        completedAt = completedAt,
        rank = rank
    )

    companion object {
        fun fromDomain(entry: GameLeaderboardEntry): FirestoreLeaderboardEntryDto = FirestoreLeaderboardEntryDto(
            userId = entry.userId,
            userName = entry.userName,
            score = entry.score,
            completionTimeSeconds = entry.completionTimeSeconds,
            completedAt = entry.completedAt,
            rank = entry.rank
        )
    }
}

data class FirestoreUserDto(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val isAnonymous: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): AuthUser = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        isAnonymous = isAnonymous
    )

    companion object {
        fun fromDomain(user: AuthUser): FirestoreUserDto = FirestoreUserDto(
            uid = user.uid,
            displayName = user.displayName,
            email = user.email,
            isAnonymous = user.isAnonymous
        )
    }
}
