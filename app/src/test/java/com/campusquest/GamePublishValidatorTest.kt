package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.validation.GamePublishValidator
import com.campusquest.domain.validation.GameValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GamePublishValidatorTest {

    private lateinit var validator: GamePublishValidator

    private val validGame = Game(
        id = "GAME_VALID",
        title = "Colombo Heritage Trail",
        description = "Explore iconic colonial architecture and historical landmarks.",
        creatorId = "CREATOR_1",
        creatorName = "Alan Turing",
        status = GameStatus.DRAFT,
        checkpointCount = 2
    )

    private val validCp1 = Checkpoint(
        id = "CP_1",
        gameId = "GAME_VALID",
        name = "Historic Clock Tower",
        lat = 6.9360,
        lng = 79.8436,
        radiusM = 25f,
        lightSignature = LightSignature(200f, 700f),
        clue = "Look skyward where the bell chimes",
        lore = "Erected in 1857",
        order = 1,
        motionType = "SWEEP"
    )

    private val validCp2 = Checkpoint(
        id = "CP_2",
        gameId = "GAME_VALID",
        name = "Old Parliament",
        lat = 6.9328,
        lng = 79.8436,
        radiusM = 30f,
        lightSignature = LightSignature(100f, 500f),
        clue = "Near the ocean breeze and grand columns",
        lore = "Constructed in 1930",
        order = 2,
        motionType = "SHAKE"
    )

    @Before
    fun setUp() {
        validator = GamePublishValidator()
    }

    @Test
    fun validate_fullyValidQuest_returnsValidResult() {
        val result = validator.validate(validGame, listOf(validCp1, validCp2))
        assertTrue("Expected validation to pass", result is GameValidationResult.Valid)
    }

    @Test
    fun validate_blankOrShortTitle_returnsInvalid() {
        val shortTitleGame = validGame.copy(title = "Hi")
        val result = validator.validate(shortTitleGame, listOf(validCp1, validCp2))

        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field == "title" })
    }

    @Test
    fun validate_blankOrShortDescription_returnsInvalid() {
        val shortDescGame = validGame.copy(description = "Short desc")
        val result = validator.validate(shortDescGame, listOf(validCp1, validCp2))

        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field == "description" })
    }

    @Test
    fun validate_lessThanTwoCheckpoints_returnsInvalid() {
        val singleCpResult = validator.validate(validGame, listOf(validCp1))
        assertTrue(singleCpResult is GameValidationResult.Invalid)
        val invalid = singleCpResult as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field == "checkpoints" })

        val emptyCpResult = validator.validate(validGame, emptyList())
        assertTrue(emptyCpResult is GameValidationResult.Invalid)
    }

    @Test
    fun validate_nonSequentialOrders_returnsInvalid() {
        val cpWithGap = validCp2.copy(order = 4) // orders are [1, 4] instead of [1, 2]
        val result = validator.validate(validGame, listOf(validCp1, cpWithGap))

        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field == "checkpoints.order" })
    }

    @Test
    fun validate_outOfBoundsCoordinates_returnsInvalid() {
        val badCoordsCp = validCp1.copy(lat = 95.0, lng = -195.0)
        val result = validator.validate(validGame, listOf(badCoordsCp, validCp2))

        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field.endsWith(".lat") })
        assertTrue(invalid.errors.any { it.field.endsWith(".lng") })
    }

    @Test
    fun validate_radiusTooSmall_returnsInvalid() {
        val smallRadiusCp = validCp1.copy(radiusM = 2.0f) // below minimum 5m
        val result = validator.validate(validGame, listOf(smallRadiusCp, validCp2))

        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field.endsWith(".radiusM") })
    }

    @Test
    fun validate_invalidLightSignature_returnsInvalid() {
        val zeroWidthLightCp = validCp1.copy(lightSignature = LightSignature(200f, 200f)) // min == max (0 lux width envelope)
        val result = validator.validate(validGame, listOf(zeroWidthLightCp, validCp2))

        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field.endsWith(".lightSignature") })
    }

    @Test
    fun validate_invalidMotionGesture_returnsInvalid() {
        val badGestureCp = validCp1.copy(motionType = "FLY_DRONE")
        val result = validator.validate(validGame, listOf(badGestureCp, validCp2))

        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid
        assertTrue(invalid.errors.any { it.field.endsWith(".motionType") })
    }

    @Test
    fun validate_multipleErrors_aggregatesAllFailures() {
        val badGame = validGame.copy(title = "", description = "")
        val badCp = validCp1.copy(name = "", clue = "", radiusM = 1f)

        val result = validator.validate(badGame, listOf(badCp))
        assertTrue(result is GameValidationResult.Invalid)
        val invalid = result as GameValidationResult.Invalid

        // Should have errors on title, description, checkpoint count, cp name, cp clue, and cp radius
        assertTrue(invalid.errors.size >= 5)
        assertTrue(invalid.formattedSummary.isNotEmpty())
    }
}
