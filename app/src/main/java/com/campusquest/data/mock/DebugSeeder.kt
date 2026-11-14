package com.campusquest.data.mock

import com.campusquest.domain.repository.GameRepository

/** Debug-only helper: loads the sample games into the local DB when it is empty. */
object DebugSeeder {
    suspend fun seedIfEmpty(repository: GameRepository) {
        if (repository.getAvailableGames().isNotEmpty()) return
        MockDataCatalog.allSampleGames.forEach { repository.createGame(it) }
        MockDataCatalog.sampleCheckpointsGame1.forEach {
            repository.createCheckpoint(it.gameId, it)
        }
    }
}