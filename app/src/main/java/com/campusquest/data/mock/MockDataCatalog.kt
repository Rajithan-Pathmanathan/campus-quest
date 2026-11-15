package com.campusquest.data.mock

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature

object MockDataCatalog {

    val foundersLanternGame = Game(
        id = "GAME_FOUNDERS_LANTERN",
        title = "The Founder's Lantern",
        description = "Long ago, the founders' lantern fell and its flame split into three embers hiding across campus—one in the bright sun, one in the quiet shade, and one in the dark. Carry the Resonator to each place, pass its trial, and relight the lantern.",
        creatorId = "CAMPUS_QUEST_OFFICIAL",
        creatorName = "Campus Quest Team",
        status = GameStatus.PUBLISHED,
        checkpointCount = 3,
        createdAt = 1700000000000L,
        publishedAt = 1700001000000L
    )

    val foundersLanternCheckpoints = listOf(
        Checkpoint(
            id = "CP_EMBER_LIGHT",
            gameId = "GAME_FOUNDERS_LANTERN",
            name = "The Ember in the Light — Tennis Court",
            lat = 6.9722948878782995,
            lng = 79.91638354596967,
            radiusM = 15f,
            lightSignature = LightSignature(minLux = 10000f, maxLux = 120000f),
            clue = "Seek the open courts where the sun blazes brightest. Step into the full sunlight and shake the Resonator three times to wake the sleeping ember.",
            lore = "[Placeholder — replace with verified Tennis Court facts] The first ember of the Founder's Lantern rests here in the open sunlight.",
            order = 1,
            motionType = "SHAKE",
            rarity = "COMMON"
        ),
        Checkpoint(
            id = "CP_EMBER_SHADE",
            gameId = "GAME_FOUNDERS_LANTERN",
            name = "The Ember in the Shade — Buddha Statue",
            lat = 6.974319507376335,
            lng = 79.91600245459793,
            radiusM = 15f,
            lightSignature = LightSignature(minLux = 500f, maxLux = 10000f),
            clue = "Find the tranquil canopy near the Buddha Statue where harsh glare softens into calm shade. Hold the Resonator steady for ten seconds as if carrying a sacred water jar.",
            lore = "[Placeholder — replace with verified Buddha Statue facts] The second ember of the Founder's Lantern is kept in calm shade and stillness.",
            order = 2,
            motionType = "TILT",
            rarity = "RARE"
        ),
        Checkpoint(
            id = "CP_EMBER_DARK",
            gameId = "GAME_FOUNDERS_LANTERN",
            name = "The Ember in the Dark — Medical Centre",
            lat = 6.976196957961348,
            lng = 79.91471980379754,
            radiusM = 15f,
            lightSignature = LightSignature(minLux = 0f, maxLux = 60f),
            clue = "Follow the path to the sheltered shadows near the Medical Centre. Shield the Resonator from bright light and sweep three times to clear the cobwebs.",
            lore = "[Placeholder — replace with verified Medical Centre facts] The final ember of the Founder's Lantern hides here in the sheltered dark, ready to relight the lantern.",
            order = 3,
            motionType = "SWEEP",
            rarity = "LEGENDARY"
        )
    )

    val allSampleGames = listOf(foundersLanternGame)
}
