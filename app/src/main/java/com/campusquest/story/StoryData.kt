package com.campusquest.story

import androidx.room.*
import com.campusquest.data.local.AppDatabase
import com.campusquest.data.local.entity.CheckpointEntity
import com.campusquest.data.local.entity.GameEntity
import com.campusquest.domain.model.*
import java.util.UUID

@Entity(tableName = "story_config")
data class StoryConfigEntity(@PrimaryKey val gameId: String, val cardId: String, val backupCode: String, val targetBearing: Float)

/** motionFallback/coverFallback belong to the retired lamp tasks; kept so the Room schema (v2) needs no migration. */
@Entity(tableName = "story_session", primaryKeys = ["gameId", "userId"])
data class StorySessionEntity(val gameId: String, val userId: String, val aligned: Boolean = false,
    val motionFallback: Boolean = false, val coverFallback: Boolean = false)

@Dao
interface StoryDao {
    @Query("SELECT * FROM story_config WHERE gameId = :gameId") suspend fun config(gameId: String): StoryConfigEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putConfig(config: StoryConfigEntity)
    @Query("SELECT * FROM story_session WHERE gameId = :gameId AND userId = :userId") suspend fun session(gameId: String, userId: String): StorySessionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSession(session: StorySessionEntity)
    @Query("DELETE FROM story_config WHERE gameId = :gameId") suspend fun deleteConfig(gameId: String)
    @Query("DELETE FROM story_session WHERE gameId = :gameId") suspend fun deleteSessions(gameId: String)
}

/** The supported story is "The Golden Tree" (object name kept for compatibility with stored records). */
object LightTheWay {
    const val STOPS = 3
    /** Records created before the Golden Tree rewrite; shown with the current copy, never rewritten. */
    const val LEGACY_TITLE = "Light the Way"

    /** A new printable-card configuration (QR token + backup code) for a quest. */
    fun newConfig(gameId: String): StoryConfigEntity {
        val token = UUID.randomUUID().toString().replace("-", "").take(8).uppercase(java.util.Locale.ROOT)
        return StoryConfigEntity(gameId, token, token, 0f)
    }

    /** Playable = has its card configuration and exactly three stops. */
    suspend fun isPlayable(db: AppDatabase, gameId: String): Boolean =
        db.storyDao().config(gameId) != null && db.checkpointDao().getCheckpointsForGame(gameId).size == STOPS

    val names = listOf("Find the Key", "Find the Instruction Card", "Bring the Golden Water")
    val clues = listOf("Follow the arrow to the first stop. Open the key box on your screen when you arrive.",
        "Follow the arrow to the second stop. Look for the keeper's printed QR card there.",
        "Follow the arrow to the final stop. Stop somewhere safe before balancing the golden water.")
    val reveals = listOf(
        "You found the key! Inside the box is a note: Find my instruction card at the next stop. Next: follow the arrow to stop 2.",
        "The key unlocks the keeper's supplies. You collected a jug of golden water! The card says: Bring this water to the golden tree at the final stop. Next: follow the arrow to stop 3.",
        "You brought the golden tree back to life! You found the key, followed the keeper's note and delivered the golden water. Adventure complete!")
    const val DESCRIPTION = "The golden tree has lost its light. Find the key, read the keeper's card and bring golden water to the tree. Visit three real places and complete the puzzles on your screen."
    fun qr(c: StoryConfigEntity) = "campusquest:v1:${c.gameId}:REPAIR_CARD:${c.cardId}"
    fun matchesCard(input: String, c: StoryConfigEntity) = input.length <= 256 && input.trim() == qr(c)
    fun matchesCode(input: String, c: StoryConfigEntity) = input.length <= 32 && input.trim().uppercase(java.util.Locale.ROOT) == c.backupCode

    suspend fun create(db: AppDatabase, uid: String, name: String, points: List<Pair<Double, Double>>, bearing: Float): String {
        require(uid.isNotBlank() && points.size == 3)
        require(points.all { it.first.isFinite() && it.second.isFinite() && it.first in -90.0..90.0 && it.second in -180.0..180.0 })
        require(bearing.isFinite() && bearing in 0f..359.99f)
        val id = "LIGHT_" + UUID.randomUUID().toString()
        val token = UUID.randomUUID().toString().replace("-", "").take(8).uppercase(java.util.Locale.ROOT)
        db.withTransaction {
            db.gameDao().insertGame(GameEntity.fromDomain(Game(id, "The Golden Tree", DESCRIPTION, uid, name, GameStatus.PUBLISHED, 3, publishedAt = System.currentTimeMillis())))
            db.checkpointDao().insertCheckpoints(points.mapIndexed { i, p ->
                CheckpointEntity.fromDomain(Checkpoint("${id}_CP${i + 1}", id, "Chapter ${i + 1} - ${names[i]}", p.first, p.second,
                    radiusM = 15f, lightSignature = LightSignature(0f, 100000f), clue = clues[i], lore = reveals[i], order = i + 1))
            })
            db.storyDao().putConfig(StoryConfigEntity(id, token, token, bearing))
            // Upload to the cloud (game + checkpoints + card config) so other phones can play it.
            db.syncQueueDao().enqueue(com.campusquest.data.local.entity.SyncQueueEntity(
                operationType = com.campusquest.data.local.entity.SyncOperationType.PUBLISH_GAME,
                entityId = id, payloadJson = "{\"gameId\":\"$id\"}"))
        }
        return id
    }
}

class ContinuousHold(private val durationMs: Long) {
    private var started: Long? = null
    fun update(valid: Boolean, now: Long): Float {
        if (!valid) { started = null; return 0f }
        if (started == null) started = now
        return ((now - started!!).toFloat() / durationMs).coerceIn(0f, 1f)
    }
    fun reset() { started = null }
}
