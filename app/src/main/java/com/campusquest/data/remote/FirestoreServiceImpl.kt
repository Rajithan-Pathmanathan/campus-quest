package com.campusquest.data.remote

import com.campusquest.data.remote.dto.FirestoreCheckpointDto
import com.campusquest.data.remote.dto.FirestoreGameDto
import com.campusquest.data.remote.dto.FirestoreLeaderboardEntryDto
import com.campusquest.data.remote.dto.FirestoreProgressDto
import com.campusquest.data.remote.dto.FirestoreStoryConfigDto
import com.campusquest.data.remote.dto.FirestoreUserDto
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.PlayerProgress
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreServiceImpl(private val db: FirebaseFirestore) : FirestoreService {

    private val games get() = db.collection("games")
    private fun checkpoints(gameId: String) = games.document(gameId).collection("checkpoints")
    private fun entries(gameId: String) = db.collection("leaderboards").document(gameId).collection("entries")

    override suspend fun fetchPublishedGames(): List<RemoteGame> {
        val docs = games.whereEqualTo("status", GameStatus.PUBLISHED.name).get().await().documents
        return docs.mapNotNull { doc ->
            val dto = doc.toObject(FirestoreGameDto::class.java) ?: return@mapNotNull null
            val cps = checkpoints(dto.id).orderBy("order").get().await().documents
                .mapNotNull { it.toObject(FirestoreCheckpointDto::class.java)?.toDomain() }
            RemoteGame(dto.toDomain(), cps, dto.story)
        }
    }

    override suspend fun publishGame(game: Game, checkpoints: List<Checkpoint>, story: FirestoreStoryConfigDto?) {
        val remoteIds = runCatching { checkpoints(game.id).get().await().documents.map { it.id } }.getOrDefault(emptyList())
        val batch = db.batch()
        batch.set(games.document(game.id), FirestoreGameDto.fromDomain(game, story))
        checkpoints.forEach { batch.set(checkpoints(game.id).document(it.id), FirestoreCheckpointDto.fromDomain(it)) }
        (remoteIds - checkpoints.map { it.id }.toSet()).forEach { batch.delete(checkpoints(game.id).document(it)) }
        batch.commit().await()
    }

    override suspend fun saveProgress(progress: PlayerProgress) {
        db.collection("progress").document("${progress.userId}_${progress.gameId}")
            .set(FirestoreProgressDto.fromDomain(progress)).await()
    }

    override suspend fun fetchProgress(userId: String): List<PlayerProgress> =
        db.collection("progress").whereEqualTo("userId", userId)
            .get().await().documents
            .mapNotNull { it.toObject(FirestoreProgressDto::class.java)?.toDomain() }

    override suspend fun submitLeaderboardEntry(gameId: String, entry: GameLeaderboardEntry) {
        entries(gameId).document(entry.userId).set(FirestoreLeaderboardEntryDto.fromDomain(entry)).await()
    }

    override fun observeLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> = callbackFlow {
        val registration = entries(gameId)
            .orderBy("score", Query.Direction.DESCENDING)
            .orderBy("completionTimeSeconds", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents.orEmpty().mapIndexedNotNull { i, doc ->
                    doc.toObject(FirestoreLeaderboardEntryDto::class.java)?.toDomain(rank = i + 1)
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun saveUserProfile(user: AuthUser) {
        db.collection("users").document(user.uid).set(FirestoreUserDto.fromDomain(user), SetOptions.merge()).await()
    }

    override suspend fun saveFcmToken(userId: String, token: String) {
        db.collection("users").document(userId).set(mapOf("fcmToken" to token), SetOptions.merge()).await()
    }

    override suspend fun deleteUserData(userId: String) {
        val progress = fetchProgress(userId)
        val batch = db.batch()
        progress.forEach {
            batch.delete(db.collection("progress").document("${userId}_${it.gameId}"))
            batch.delete(entries(it.gameId).document(userId))
        }
        batch.delete(db.collection("users").document(userId))
        batch.commit().await()
    }
}
