package com.campusquest.data.remote

import com.campusquest.data.remote.dto.FirestoreCheckpointDto
import com.campusquest.data.remote.dto.FirestoreGameDto
import com.campusquest.data.remote.dto.FirestoreLeaderboardEntryDto
import com.campusquest.data.remote.dto.FirestoreProgressDto
import com.campusquest.data.remote.dto.FirestoreUserDto
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.PlayerProgress
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreServiceImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FirestoreService {

    companion object {
        private const val COLLECTION_GAMES = "games"
        private const val COLLECTION_CHECKPOINTS = "checkpoints"
        private const val COLLECTION_PROGRESS = "progress"
        private const val COLLECTION_LEADERBOARDS = "leaderboards"
        private const val COLLECTION_ENTRIES = "entries"
        private const val COLLECTION_USERS = "users"
    }

    override suspend fun fetchPublishedGames(): Result<List<Game>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_GAMES)
                .whereEqualTo("status", GameStatus.PUBLISHED.name)
                .get()
                .await()

            val games = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreGameDto::class.java)?.toDomain()
            }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchGameById(gameId: String): Result<Game?> {
        return try {
            val doc = firestore.collection(COLLECTION_GAMES)
                .document(gameId)
                .get()
                .await()

            val game = doc.toObject(FirestoreGameDto::class.java)?.toDomain()
            Result.success(game)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchGameCheckpoints(gameId: String): Result<List<Checkpoint>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_GAMES)
                .document(gameId)
                .collection(COLLECTION_CHECKPOINTS)
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()

            val checkpoints = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreCheckpointDto::class.java)?.toDomain()
            }
            Result.success(checkpoints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun publishGame(game: Game, checkpoints: List<Checkpoint>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val gameRef = firestore.collection(COLLECTION_GAMES).document(game.id)
            batch.set(gameRef, FirestoreGameDto.fromDomain(game))

            val checkpointsCollection = gameRef.collection(COLLECTION_CHECKPOINTS)
            for (cp in checkpoints) {
                val cpRef = checkpointsCollection.document(cp.id)
                batch.set(cpRef, FirestoreCheckpointDto.fromDomain(cp))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePlayerProgress(progress: PlayerProgress): Result<Unit> {
        return try {
            val docId = "${progress.userId}_${progress.gameId}"
            firestore.collection(COLLECTION_PROGRESS)
                .document(docId)
                .set(FirestoreProgressDto.fromDomain(progress))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPlayerProgress(gameId: String, userId: String): Result<PlayerProgress?> {
        return try {
            val docId = "${userId}_${gameId}"
            val doc = firestore.collection(COLLECTION_PROGRESS)
                .document(docId)
                .get()
                .await()

            val progress = doc.toObject(FirestoreProgressDto::class.java)?.toDomain()
            Result.success(progress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_LEADERBOARDS)
            .document(gameId)
            .collection(COLLECTION_ENTRIES)
            .orderBy("score", Query.Direction.DESCENDING)
            .orderBy("completionTimeSeconds", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
                        doc.toObject(FirestoreLeaderboardEntryDto::class.java)?.toDomain()?.copy(rank = index + 1)
                    }
                    trySend(entries)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun submitLeaderboardEntry(gameId: String, entry: GameLeaderboardEntry): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_LEADERBOARDS)
                .document(gameId)
                .collection(COLLECTION_ENTRIES)
                .document(entry.userId)
                .set(FirestoreLeaderboardEntryDto.fromDomain(entry))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(authUser: AuthUser): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(authUser.uid)
                .set(FirestoreUserDto.fromDomain(authUser))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
