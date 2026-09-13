package com.campusquest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.campusquest.data.local.entity.GameEntity
import com.campusquest.domain.model.GameStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE status = :status ORDER BY createdAt DESC")
    fun observePublishedGames(status: GameStatus = GameStatus.PUBLISHED): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getPublishedGames(status: GameStatus = GameStatus.PUBLISHED): List<GameEntity>

    @Query("SELECT * FROM games WHERE creatorId = :creatorId ORDER BY createdAt DESC")
    fun observeCreatorGames(creatorId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameById(gameId: String): GameEntity?

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    fun observeGameById(gameId: String): Flow<GameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGame(gameId: String)
}
