package com.campusquest.data.mock

import com.campusquest.data.local.AppDatabase
import com.campusquest.data.local.entity.CheckpointEntity
import com.campusquest.data.local.entity.GameEntity
import com.campusquest.domain.repository.GameRepository

/** Ensures the official story quest ("The Founder's Lantern") exists in the local DB in both debug and release builds. */
object DebugSeeder {

    private val legacySampleGameIds = listOf("GAME_HERITAGE_001", "GAME_INNOVATION_002")

    /** Direct DAO seeding (preferred): does not pollute sync_queue with seed data the player doesn't own. */
    suspend fun seedDatabase(database: AppDatabase) {
        val gameDao = database.gameDao()
        val checkpointDao = database.checkpointDao()

        // Clean up legacy California sample quests if present from an older build
        legacySampleGameIds.forEach { legacyId ->
            gameDao.deleteGame(legacyId)
        }

        val storyGame = MockDataCatalog.foundersLanternGame
        gameDao.insertGame(GameEntity.fromDomain(storyGame))

        val existingCheckpoints = checkpointDao.getCheckpointsForGame(storyGame.id)
        if (existingCheckpoints.isEmpty()) {
            checkpointDao.insertCheckpoints(
                MockDataCatalog.foundersLanternCheckpoints.map { CheckpointEntity.fromDomain(it) }
            )
        }
    }

    /** Backwards-compatible helper called by GamesViewModel. */
    suspend fun seedIfEmpty(repository: GameRepository) {
        val storyGame = MockDataCatalog.foundersLanternGame
        if (repository.getGameDetails(storyGame.id) == null) {
            repository.createGame(storyGame)
        }
        if (repository.getGameCheckpoints(storyGame.id).isEmpty()) {
            MockDataCatalog.foundersLanternCheckpoints.forEach { checkpoint ->
                repository.createCheckpoint(storyGame.id, checkpoint)
            }
        }
    }
}