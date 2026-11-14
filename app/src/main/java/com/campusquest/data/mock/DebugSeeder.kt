package com.campusquest.data.mock

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.repository.GameRepository

/** Debug-only helper: makes sure the sample games and their checkpoints exist in the local DB. */
object DebugSeeder {

    suspend fun seedIfEmpty(repository: GameRepository) {
        MockDataCatalog.allSampleGames.forEach { game ->
            if (repository.getGameDetails(game.id) == null) repository.createGame(game)
        }

        val checkpointsByGame: Map<String, List<Checkpoint>> = mapOf(
            MockDataCatalog.sampleGame1.id to MockDataCatalog.sampleCheckpointsGame1,
            MockDataCatalog.sampleGame2.id to MockDataCatalog.sampleCheckpointsGame2
        )
        checkpointsByGame.forEach { (gameId, checkpoints) ->
            if (repository.getGameCheckpoints(gameId).isEmpty()) {
                checkpoints.forEach { repository.createCheckpoint(gameId, it) }
            }
        }
    }
}

