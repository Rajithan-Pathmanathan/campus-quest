package com.campusquest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.campusquest.data.local.dao.CheckpointDao
import com.campusquest.data.local.dao.GameDao
import com.campusquest.data.local.dao.PlayerProgressDao
import com.campusquest.data.local.dao.SyncQueueDao
import com.campusquest.data.local.entity.CheckpointEntity
import com.campusquest.data.local.entity.GameEntity
import com.campusquest.data.local.entity.PlayerProgressEntity
import com.campusquest.data.local.entity.SyncQueueEntity
import com.campusquest.story.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GameEntity::class,
        CheckpointEntity::class,
        PlayerProgressEntity::class,
        StoryConfigEntity::class,
        StorySessionEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storyDao(): StoryDao

    abstract fun gameDao(): GameDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun playerProgressDao(): PlayerProgressDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        val STORY_MIGRATION = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS story_config (gameId TEXT NOT NULL PRIMARY KEY, cardId TEXT NOT NULL, backupCode TEXT NOT NULL, targetBearing REAL NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS story_session (gameId TEXT NOT NULL, userId TEXT NOT NULL, aligned INTEGER NOT NULL, motionFallback INTEGER NOT NULL, coverFallback INTEGER NOT NULL, PRIMARY KEY(gameId, userId))")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_quest_db"
                )
                    .addMigrations(STORY_MIGRATION)
                    .build()
                INSTANCE = instance

                instance
            }
        }
    }
}
