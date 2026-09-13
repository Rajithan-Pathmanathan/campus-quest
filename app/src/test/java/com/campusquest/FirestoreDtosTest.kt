package com.campusquest

import com.campusquest.data.remote.dto.FirestoreCheckpointDto
import com.campusquest.data.remote.dto.FirestoreGameDto
import com.campusquest.data.remote.dto.FirestoreLeaderboardEntryDto
import com.campusquest.data.remote.dto.FirestoreProgressDto
import com.campusquest.data.remote.dto.FirestoreUserDto
import com.campusquest.domain.model.AuthUser
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.PlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreDtosTest {

    @Test
    fun gameDto_bidirectionalMapping_preservesAllFields() {
        val domainGame = Game(
            id = "G100",
            title = "Quantum Physics Trail",
            description = "Explore particle physics landmarks",
            creatorId = "U_PHYSICS",
            creatorName = "Prof Feynman",
            status = GameStatus.PUBLISHED,
            checkpointCount = 4,
            createdAt = 1700000000000L,
            publishedAt = 1700001000000L
        )

        val dto = FirestoreGameDto.fromDomain(domainGame)
        assertEquals("G100", dto.id)
        assertEquals("Quantum Physics Trail", dto.title)
        assertEquals("PUBLISHED", dto.status)

        val mappedBack = dto.toDomain()
        assertEquals(domainGame, mappedBack)
    }

    @Test
    fun checkpointDto_bidirectionalMapping_preservesSensorSignatures() {
        val checkpoint = Checkpoint(
            id = "CP_LASER",
            gameId = "G100",
            name = "Laser Optics Lab",
            lat = 6.9025,
            lng = 79.8610,
            radiusM = 25f,
            lightSignature = LightSignature(minLux = 150f, maxLux = 800f),
            clue = "Where coherent beams converge",
            lore = "Home to the first helium-neon laser on campus in 1978.",
            order = 2,
            motionType = "TILT_UP",
            rarity = "RARE"
        )

        val dto = FirestoreCheckpointDto.fromDomain(checkpoint)
        assertEquals(150f, dto.minLux)
        assertEquals(800f, dto.maxLux)
        assertEquals("TILT_UP", dto.motionType)

        val mappedBack = dto.toDomain()
        assertEquals(checkpoint, mappedBack)
    }

    @Test
    fun progressDto_bidirectionalMapping_preservesCheckpointsList() {
        val progress = PlayerProgress(
            gameId = "G100",
            userId = "USER_42",
            completedCheckpointIds = listOf("CP1", "CP2"),
            currentCheckpointOrder = 3,
            score = 200,
            startedAt = 1700000000000L,
            completedAt = null,
            isCompleted = false
        )

        val dto = FirestoreProgressDto.fromDomain(progress)
        assertEquals(listOf("CP1", "CP2"), dto.completedCheckpointIds)
        assertEquals(200, dto.score)

        val mappedBack = dto.toDomain()
        assertEquals(progress, mappedBack)
    }

    @Test
    fun leaderboardDto_bidirectionalMapping_preservesRankAndTiming() {
        val entry = GameLeaderboardEntry(
            userId = "U_TOP_1",
            userName = "SpeedRunner",
            score = 500,
            completionTimeSeconds = 420,
            completedAt = 1700005000000L,
            rank = 1
        )

        val dto = FirestoreLeaderboardEntryDto.fromDomain(entry)
        assertEquals("SpeedRunner", dto.userName)
        assertEquals(420L, dto.completionTimeSeconds)

        val mappedBack = dto.toDomain()
        assertEquals(entry, mappedBack)
    }

    @Test
    fun userDto_bidirectionalMapping_preservesAuthProperties() {
        val authUser = AuthUser(
            uid = "USER_ABC",
            displayName = "Explorer Prime",
            email = "prime@quest.edu",
            isAnonymous = false
        )

        val dto = FirestoreUserDto.fromDomain(authUser)
        assertEquals("USER_ABC", dto.uid)
        assertEquals("prime@quest.edu", dto.email)

        val mappedBack = dto.toDomain()
        assertEquals(authUser, mappedBack)
    }
}
