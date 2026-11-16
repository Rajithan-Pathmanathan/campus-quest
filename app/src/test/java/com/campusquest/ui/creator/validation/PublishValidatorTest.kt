package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.LightSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishValidatorTest {

    private fun game(desc: String = "A walk") = Game(
        id = "G1", title = "Trail", description = desc, creatorId = "me", creatorName = "Me"
    )

    private fun cp(order: Int, clue: String = "clue", lore: String = "lore") = Checkpoint(
        id = "c$order", gameId = "G1", name = "CP$order", lat = 6.9, lng = 79.8, radiusM = 20f,
        lightSignature = LightSignature(0f, 100f), clue = clue, lore = lore, order = order
    )

    @Test fun validGame_hasNoProblems() =
        assertEquals(emptyList<String>(), PublishValidator.validate(game(), listOf(cp(1), cp(2), cp(3))))

    @Test fun goldenTree_needsExactlyThreeStops() {
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), cp(2))).any { it.contains("exactly 3 stops") })
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), cp(2), cp(3), cp(4))).any { it.contains("exactly 3 stops") })
    }

    @Test fun stopsMustBeNumberedOneToThree() =
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), cp(2), cp(5))).any { it.contains("1, 2 and 3") })

    @Test fun memoryTextIsNotRequired() =
        assertEquals(emptyList<String>(), PublishValidator.validate(game(), listOf(cp(1, lore = ""), cp(2, lore = ""), cp(3, lore = ""))))

    @Test fun noCheckpoints_isBlocked() =
        assertTrue(PublishValidator.validate(game(), emptyList()).isNotEmpty())

    @Test fun missingClue_namesTheCheckpoint() {
        val problems = PublishValidator.validate(game(), listOf(cp(1), cp(2, clue = "")))
        assertTrue(problems.any { it.contains("Checkpoint 2") && it.contains("Clue") })
    }

    @Test fun emptyDescription_isBlockedForPublish() =
        assertTrue(PublishValidator.validate(game(desc = ""), listOf(cp(1))).isNotEmpty())

    @Test fun duplicateOrder_isBlocked() =
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), cp(1).copy(id = "other"))).isNotEmpty())
}