package com.campusquest.data.mock

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature

object MockDataCatalog {

    val sampleGame1 = Game(
        id = "GAME_HERITAGE_001",
        title = "University Heritage Trail",
        description = "Discover historic landmarks, the original bell tower, and the founder's archives across the North Quad.",
        creatorId = "PROF_OAK_01",
        creatorName = "Prof. Oak",
        status = GameStatus.PUBLISHED,
        checkpointCount = 3,
        createdAt = 1700000000000L,
        publishedAt = 1700001000000L
    )

    val sampleGame2 = Game(
        id = "GAME_INNOVATION_002",
        title = "Silicon Quad Tech Hunt",
        description = "Uncover robotics labs, clean energy testbeds, and the ancient mainframe server room.",
        creatorId = "IEEE_STUDENT_BRANCH",
        creatorName = "IEEE Student Branch",
        status = GameStatus.PUBLISHED,
        checkpointCount = 2,
        createdAt = 1700002000000L,
        publishedAt = 1700003000000L
    )

    val sampleCheckpointsGame1 = listOf(
        Checkpoint(
            id = "CP_BELL_TOWER",
            gameId = "GAME_HERITAGE_001",
            name = "Historic Old Clock Tower",
            lat = 37.4275,
            lng = -122.1697,
            radiusM = 25f,
            lightSignature = LightSignature(minLux = 150f, maxLux = 800f),
            clue = "Where shadows stretch longest at noon, look beneath the bronze chime.",
            lore = "Erected in 1891, this bell tower survived the great quake of 1906.",
            order = 1,
            motionType = "SWEEP",
            rarity = "COMMON"
        ),
        Checkpoint(
            id = "CP_FOUNDER_ARCHIVE",
            gameId = "GAME_HERITAGE_001",
            name = "Founder's Rare Archive Vault",
            lat = 37.4282,
            lng = -122.1685,
            radiusM = 15f,
            lightSignature = LightSignature(minLux = 5f, maxLux = 45f),
            clue = "Seek the dim subterranean corridor where first editions rest.",
            lore = "Preserves the original charter signed with an iron fountain pen.",
            order = 2,
            motionType = "TILT",
            rarity = "RARE"
        ),
        Checkpoint(
            id = "CP_QUAD_SUNDIAL",
            gameId = "GAME_HERITAGE_001",
            name = "Memorial Sun Dial",
            lat = 37.4290,
            lng = -122.1700,
            radiusM = 30f,
            lightSignature = LightSignature(minLux = 400f, maxLux = 2000f),
            clue = "Stand where solar rays cast the precise mathematical azimuth.",
            lore = "Calibrated in 1920 by the astronomy department for celestial navigation.",
            order = 3,
            motionType = "SHAKE",
            rarity = "EPIC"
        )
    )

    val sampleCheckpointsGame2 = listOf(
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

    val allSampleGames = listOf(sampleGame1, sampleGame2)
}

