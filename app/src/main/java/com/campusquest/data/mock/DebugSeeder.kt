package com.campusquest.data.mock

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.repository.GameRepository

/** Debug-only helper: makes sure the sample games and their checkpoints exist in the local DB. */
object DebugSeeder {

    private val sampleCheckpointsGame2 = listOf(
        Checkpoint(
            id = "CP_ROBOTICS_LAB",
            gameId = "GAME_INNOVATION_002",
            name = "Robotics Lab Entrance",
            lat = 37.4310,
            lng = -122.1730,
            radiusM = 20f,
            lightSignature = LightSignature(minLux = 100f, maxLux = 600f),
            clue = "Where machines learn to move, look for the glowing sign.",
            lore = "The first student-built robot rolled out of this lab.",
            order = 1,
            motionType = "SWEEP",
            rarity = "COMMON"
        ),
        Checkpoint(
            id = "CP_SOLAR_TESTBED",
            gameId = "GAME_INNOVATION_002",
            name = "Solar Testbed",
            lat = 37.4318,
            lng = -122.1722,
            radiusM = 25f,
            lightSignature = LightSignature(minLux = 500f, maxLux = 2500f),
            clue = "Face the sun and find the panels that never rest.",
            lore = "Clean energy research began on this rooftop.",
            order = 2,
            motionType = "SWEEP",
            rarity = "RARE"
        )
    )

    suspend fun seedIfEmpty(repository: GameRepository) {
        MockDataCatalog.allSampleGames.forEach { game ->
            if (repository.getGameDetails(game.id) == null) repository.createGame(game)
        }

        val checkpointsByGame: Map<String, List<Checkpoint>> = mapOf(
            "GAME_HERITAGE_001" to MockDataCatalog.sampleCheckpointsGame1,
            "GAME_INNOVATION_002" to sampleCheckpointsGame2
        )
        checkpointsByGame.forEach { (gameId, checkpoints) ->
            if (repository.getGameCheckpoints(gameId).isEmpty()) {
                checkpoints.forEach { repository.createCheckpoint(gameId, it) }
            }
        }
    }
}