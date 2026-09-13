package com.campusquest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.campusquest.data.local.entity.PlayerProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProgressDao {
    @Query("SELECT * FROM player_progress WHERE gameId = :gameId AND userId = :userId LIMIT 1")
    fun observeProgress(gameId: String, userId: String): Flow<PlayerProgressEntity?>

    @Query("SELECT * FROM player_progress WHERE gameId = :gameId AND userId = :userId LIMIT 1")
    suspend fun getProgress(gameId: String, userId: String): PlayerProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: PlayerProgressEntity)

    @Update
    suspend fun updateProgress(progress: PlayerProgressEntity)
}
