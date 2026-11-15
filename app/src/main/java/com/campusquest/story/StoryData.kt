package com.campusquest.story

import androidx.room.*
import com.campusquest.data.local.AppDatabase
import com.campusquest.data.local.entity.CheckpointEntity
import com.campusquest.data.local.entity.GameEntity
import com.campusquest.domain.model.*
import java.util.UUID

@Entity(tableName = "story_config")
data class StoryConfigEntity(@PrimaryKey val gameId: String, val cardId: String, val backupCode: String, val targetBearing: Float)

@Entity(tableName = "story_session", primaryKeys = ["gameId", "userId"])
data class StorySessionEntity(val gameId: String, val userId: String, val aligned: Boolean = false,
    val motionFallback: Boolean = false, val coverFallback: Boolean = false)

@Dao
interface StoryDao {
    @Query("SELECT * FROM story_config WHERE gameId = :gameId") suspend fun config(gameId: String): StoryConfigEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putConfig(config: StoryConfigEntity)
    @Query("SELECT * FROM story_session WHERE gameId = :gameId AND userId = :userId") suspend fun session(gameId: String, userId: String): StorySessionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSession(session: StorySessionEntity)
}

object LightTheWay {
    val names = listOf("The Key in the Box", "The Hidden Repair Card", "Bring Back the Light")
    val clues = listOf("Someone left the lamp key near the garden. Follow the arrow to find it.",
        "The key's note points to the covered board. Find the repair card in its shaded pocket.",
        "Bring the key and cover to the meeting place. The lamp is waiting there.")
    val reveals = listOf(
        "You found the key! A note is tied to it: The lamp needs one more part. Look for my repair card at the covered board.",
        "You found the missing part! Take the glass cover to the meeting place. Turn the lamp toward the path, then close its cover.",
        "The light is back! You found the key, followed the repair note and fixed the meeting lamp. The path is ready for the welcome walk. Well done!")
    const val DESCRIPTION = "The campus welcome walk starts soon, but the meeting lamp is not working. Find its key, follow the repair note and bring its light back. Three places, one small adventure."
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
            db.gameDao().insertGame(GameEntity.fromDomain(Game(id, "Light the Way", DESCRIPTION, uid, name, GameStatus.PUBLISHED, 3, publishedAt = System.currentTimeMillis())))
            db.checkpointDao().insertCheckpoints(points.mapIndexed { i, p ->
                CheckpointEntity.fromDomain(Checkpoint("${id}_CP${i + 1}", id, "Chapter ${i + 1} - ${names[i]}", p.first, p.second,
                    radiusM = 15f, lightSignature = LightSignature(0f, 100000f), clue = clues[i], lore = reveals[i], order = i + 1))
            })
            db.storyDao().putConfig(StoryConfigEntity(id, token, token, bearing))
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

/** Shared maze collisions for tilt and touch controls. Normalized screen coordinates. */
class KeyBox {
    var x = 0.12f; private set
    var y = 0.88f; private set
    private fun free(x: Float, y: Float): Boolean {
        val r = 0.035f
        if (x !in r..(1-r) || y !in r..(1-r)) return false
        return !(x+r > 0.32f && x-r < 0.38f && y-r < 0.72f) && !(x+r > 0.62f && x-r < 0.68f && y+r > 0.28f)
    }
    fun move(dx: Float, dy: Float) {
        val steps = (kotlin.math.max(kotlin.math.abs(dx), kotlin.math.abs(dy)) / 0.015f).toInt().coerceIn(1, 200)
        repeat(steps) {
            if (free(x+dx/steps,y)) x += dx/steps
            if (free(x,y+dy/steps)) y += dy/steps
        }
    }
    val complete get() = x > 0.84f && y < 0.16f
}
