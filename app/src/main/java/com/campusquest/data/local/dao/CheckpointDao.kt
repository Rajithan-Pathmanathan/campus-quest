package com.campusquest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.campusquest.data.local.entity.CheckpointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckpointDao {
    @Query("SELECT * FROM checkpoints WHERE gameId = :gameId ORDER BY orderIndex ASC")
    fun observeCheckpointsForGame(gameId: String): Flow<List<CheckpointEntity>>

    @Query("SELECT * FROM checkpoints WHERE gameId = :gameId ORDER BY orderIndex ASC")
    suspend fun getCheckpointsForGame(gameId: String): List<CheckpointEntity>

    @Query("SELECT * FROM checkpoints WHERE id = :checkpointId LIMIT 1")
    suspend fun getCheckpointById(checkpointId: String): CheckpointEntity?

    @Query("SELECT * FROM checkpoints WHERE gameId = :gameId AND orderIndex = :order LIMIT 1")
    suspend fun getCheckpointByOrder(gameId: String, order: Int): CheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoint(checkpoint: CheckpointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoints(checkpoints: List<CheckpointEntity>)

    @Update
    suspend fun updateCheckpoint(checkpoint: CheckpointEntity)

    @Query("DELETE FROM checkpoints WHERE id = :checkpointId")
    suspend fun deleteCheckpoint(checkpointId: String)
}
