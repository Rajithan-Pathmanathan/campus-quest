package com.campusquest.data.remote

import com.campusquest.data.remote.dto.FirestoreStoryConfigDto
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.PlayerProgress
import kotlinx.coroutines.flow.Flow

/** A published game as stored in the cloud, with everything another phone needs to play it. */
data class RemoteGame(
    val game: Game,
    val checkpoints: List<Checkpoint>,
    val story: FirestoreStoryConfigDto?
)

/**
 * Cloud backend (Firestore). Paths:
 * /users/{uid}, /games/{gameId}, /games/{gameId}/checkpoints/{cpId},
 * /progress/{uid}_{gameId}, /leaderboards/{gameId}/entries/{uid}.
 * Rules: firestore.rules at the repository root.
 */
interface FirestoreService {
    suspend fun fetchPublishedGames(): List<RemoteGame>
    /** Writes the game, all its checkpoints and (optionally) the story config in one batch; removes deleted checkpoints. */
    suspend fun publishGame(game: Game, checkpoints: List<Checkpoint>, story: FirestoreStoryConfigDto?)
    /** Removes a quest: its checkpoints and leaderboard first, then the quest itself. Creator only (rules). */
    suspend fun deleteGame(gameId: String)
    suspend fun saveProgress(progress: PlayerProgress)
    suspend fun fetchProgress(userId: String): List<PlayerProgress>
    suspend fun submitLeaderboardEntry(gameId: String, entry: GameLeaderboardEntry)
    /** Live leaderboard, best score first, then fastest. Closes with an error if the query fails. */
    fun observeLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>>
    suspend fun saveUserProfile(user: AuthUser)
    suspend fun saveFcmToken(userId: String, token: String)
    /** Removes the user's profile, progress and leaderboard entries (account deletion). */
    suspend fun deleteUserData(userId: String)
}
