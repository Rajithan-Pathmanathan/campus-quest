package com.campusquest.data.remote

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.PlayerProgress
import kotlinx.coroutines.flow.Flow

interface FirestoreService {
    suspend fun fetchPublishedGames(): Result<List<Game>>
    suspend fun fetchGameById(gameId: String): Result<Game?>
    suspend fun fetchGameCheckpoints(gameId: String): Result<List<Checkpoint>>
    suspend fun publishGame(game: Game, checkpoints: List<Checkpoint>): Result<Unit>
    suspend fun savePlayerProgress(progress: PlayerProgress): Result<Unit>
    suspend fun getPlayerProgress(gameId: String, userId: String): Result<PlayerProgress?>
    fun observeLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>>
    suspend fun submitLeaderboardEntry(gameId: String, entry: GameLeaderboardEntry): Result<Unit>
    suspend fun saveUserProfile(authUser: AuthUser): Result<Unit>
}
