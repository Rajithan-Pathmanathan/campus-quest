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
            lore = "Memory Restored: The campus Tennis Courts have hosted generations of spirited matches under the tropical sun, where the first spark of the Founder's Lantern took refuge in the open light.",
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
            lore = "Memory Restored: Beneath the peaceful shade of the sacred tree at the heart of campus, students pause in quiet reflection—preserving the second ember in stillness and balance.",
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
            lore = "Memory Restored: In the cool, dim shelter of the Medical Centre, care and resilience have guarded the final ember—completing the triad and relighting the Founder's Lantern!",
            order = 3,
            motionType = "SWEEP",
            rarity = "LEGENDARY"
        )
    )

    // Backwards-compatible aliases so existing callers/tests compile cleanly
    val sampleGame1: Game = foundersLanternGame
    val sampleGame2: Game = foundersLanternGame
    val sampleCheckpointsGame1: List<Checkpoint> = foundersLanternCheckpoints
    val sampleCheckpointsGame2: List<Checkpoint> = emptyList()

    val allSampleGames = listOf(foundersLanternGame)
}