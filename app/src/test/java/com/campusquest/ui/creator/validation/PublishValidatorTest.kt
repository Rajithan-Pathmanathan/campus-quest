package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.CheckpointActivity
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
        lightSignature = LightSignature(0f, 100f), clue = clue, lore = lore, order = order,
        motionType = CheckpointActivity.forOrder(order).name
    )

    @Test fun validGame_hasNoProblems() =
        assertEquals(emptyList<String>(), PublishValidator.validate(game(), listOf(cp(1), cp(2), cp(3))))

    @Test fun noCheckpoints_isBlocked() =
        assertTrue(PublishValidator.validate(game(), emptyList()).isNotEmpty())

    @Test fun missingClue_namesTheCheckpoint() {
        val problems = PublishValidator.validate(game(), listOf(cp(1), cp(2, clue = ""), cp(3)))
        assertTrue(problems.any { it.contains("Checkpoint 2") && it.contains("Clue") })
    }

    @Test fun emptyDescription_isBlockedForPublish() =
        assertTrue(PublishValidator.validate(game(desc = ""), listOf(cp(1))).isNotEmpty())

    @Test fun duplicateOrder_isBlocked() =
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), cp(1).copy(id = "other"))).isNotEmpty())

    @Test fun storyMustHaveExactlyThreeCheckpoints() {
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), cp(2))).any { it.contains("exactly 3") })
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), cp(2), cp(3), cp(4))).any { it.contains("exactly 3") })
    }

    @Test fun storyActivitiesMustFollowTheChapterOrder() {
        val wrong = cp(2).copy(motionType = CheckpointActivity.LAMP_OIL.name)
        assertTrue(PublishValidator.validate(game(), listOf(cp(1), wrong, cp(3))).any { it.contains("QR Scan") })
    }
}
