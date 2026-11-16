package com.campusquest

import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.PlayerProgress
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Replaces Dispatchers.Main so viewModelScope works in plain JVM tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class M1MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

fun m1Game(
    id: String = "g1",
    title: String = "Test Quest",
    status: GameStatus = GameStatus.PUBLISHED,
    checkpointCount: Int = 2
) = Game(
    id = id,
    title = title,
    description = "Description",
    creatorId = "creator-1",
    creatorName = "Creator",
    status = status,
    checkpointCount = checkpointCount
)

fun m1Entry(userId: String, score: Int, timeSeconds: Long) = GameLeaderboardEntry(
    userId = userId,
    userName = userId,
    score = score,
    completionTimeSeconds = timeSeconds,
    completedAt = 0L
)

class M1FakeGameRepository : GameRepository {

    var games: List<Game> = emptyList()
    var availableGamesError: Exception? = null
    var availableGamesGate: CompletableDeferred<Unit>? = null
    var detailsError: Exception? = null

    var joinResult: Result<Unit> = Result.success(Unit)
    var joinGate: CompletableDeferred<Unit>? = null
    val joinCalls = mutableListOf<String>()

    var leaderboardFlow: Flow<List<GameLeaderboardEntry>> = flowOf(emptyList())
    val leaderboardRequests = mutableListOf<String>()

    override suspend fun getAvailableGames(): List<Game> {
        availableGamesGate?.await()
        availableGamesError?.let { throw it }
        return games
    }

    override suspend fun getCreatorGames(creatorId: String): List<Game> {
        availableGamesGate?.await()
        availableGamesError?.let { throw it }
        return games.filter { it.creatorId == creatorId }
    }

    override suspend fun getGameDetails(gameId: String): Game? {
        detailsError?.let { throw it }
        return games.firstOrNull { it.id == gameId }
    }

    override suspend fun createGame(game: Game): Result<Game> = Result.success(game)

    override suspend fun updateGame(game: Game): Result<Unit> = Result.success(Unit)

    override suspend fun createCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Checkpoint> =
        Result.success(checkpoint)

    override suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint): Result<Unit> =
        Result.success(Unit)

    override suspend fun deleteGame(gameId: String): Result<Unit> = Result.success(Unit)

    override suspend fun deleteCheckpoint(gameId: String, checkpointId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun publishGame(gameId: String): Result<Unit> = Result.success(Unit)

    override suspend fun joinGame(gameId: String): Result<Unit> {
        joinCalls.add(gameId)
        joinGate?.await()
        return joinResult
    }

    override suspend fun getGameCheckpoints(gameId: String): List<Checkpoint> = emptyList()

    override suspend fun getFusionSignature(gameId: String, checkpointId: String): LightSignature =
        LightSignature(minLux = 0f, maxLux = 500f)

    override suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit> =
        Result.success(Unit)

    override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> {
        leaderboardRequests.add(gameId)
        return leaderboardFlow
    }

    override fun observePlayerProgress(gameId: String, userId: String): Flow<PlayerProgress?> = flowOf(null)

    override suspend fun getAllPlayerProgress(userId: String): List<PlayerProgress> = emptyList()

    override suspend fun syncPending() {}
}

class M1FakeAuthRepository : AuthRepository {

    var signInResult: Result<AuthUser> =
        Result.success(AuthUser(uid = "u1", displayName = "User", email = "user@test.com"))
    var guestResult: Result<AuthUser> =
        Result.success(AuthUser(uid = "guest", displayName = "Guest", email = null, isAnonymous = true))
    var signInGate: CompletableDeferred<Unit>? = null
    var signInCalls = 0
    var lastEmail: String? = null

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUserState: StateFlow<AuthUser?> = _currentUser

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        signInCalls++
        lastEmail = email
        signInGate?.await()
        return signInResult
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> = signInResult

    override suspend fun signInAnonymously(): Result<AuthUser> = guestResult

    override suspend fun signOut() {}

    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)

    override fun getCurrentUserId(): String? = _currentUser.value?.uid
}
