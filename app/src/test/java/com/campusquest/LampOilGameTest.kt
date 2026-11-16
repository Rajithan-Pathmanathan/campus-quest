package com.campusquest

import com.campusquest.story.LampOilGame
import com.campusquest.story.OilPourStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LampOilGameTest {
    @Test
    fun steadyTiltFillsThenUprightHoldCompletesLamp() {
        val game = LampOilGame()

        repeat(250) { game.update(tilt = 45f, turnRate = .2f, seconds = .1f, active = true) }
        assertEquals(1f, game.fill, .001f)
        assertEquals(0f, game.jarFill, .001f)
        assertEquals(OilPourStatus.RETURN_UPRIGHT, game.status)

        repeat(20) { game.update(tilt = 3f, turnRate = .1f, seconds = .1f, active = true) }
        assertEquals(OilPourStatus.COMPLETE, game.status)
    }

    @Test
    fun suddenMovementSpillsOilAndRemovesSomeProgress() {
        val game = LampOilGame()
        repeat(50) { game.update(tilt = 45f, turnRate = .2f, seconds = .1f, active = true) }
        val beforeSpill = game.fill

        game.update(tilt = 45f, turnRate = 3f, seconds = .5f, active = true)

        assertEquals(OilPourStatus.TOO_FAST, game.status)
        assertTrue(game.fill < beforeSpill)
        assertTrue(game.spilled > 0f)
    }

    @Test
    fun missingGyroscopeStillAllowsAccelerometerPouring() {
        val game = LampOilGame()

        repeat(250) { game.update(tilt = 45f, turnRate = null, seconds = .1f, active = true) }

        assertEquals(1f, game.fill, .001f)
        assertEquals(0f, game.jarFill, .001f)
        assertEquals(OilPourStatus.RETURN_UPRIGHT, game.status)
    }

    @Test
    fun leavingCheckpointPausesPouring() {
        val game = LampOilGame()
        game.update(tilt = 45f, turnRate = .2f, seconds = 5f, active = false)

        assertEquals(0f, game.fill, .001f)
        assertEquals(1f, game.jarFill, .001f)
        assertEquals(OilPourStatus.PAUSED, game.status)
    }

    @Test
    fun anglesFollowTheJarRules_restPourSpill() {
        val rest = LampOilGame().apply { repeat(10) { update(tilt = 20f, turnRate = 0f, seconds = .1f, active = true) } }
        assertEquals(0f, rest.fill, .0001f)
        assertEquals(1f, rest.jarFill, .0001f)
        assertEquals(OilPourStatus.READY, rest.status)

        val pourClockwise = LampOilGame().apply { repeat(10) { update(tilt = 45f, turnRate = 0f, seconds = .1f, active = true) } }
        assertTrue(pourClockwise.fill > 0f)
        assertEquals(1f - pourClockwise.fill, pourClockwise.jarFill, .0001f)
        assertEquals(OilPourStatus.POURING, pourClockwise.status)

        val counterClockwise = LampOilGame().apply { repeat(10) { update(tilt = -45f, turnRate = 0f, seconds = .1f, active = true) } }
        assertEquals(0f, counterClockwise.fill, .0001f)
        assertEquals(1f, counterClockwise.jarFill, .0001f)
        assertEquals(OilPourStatus.READY, counterClockwise.status)

        val spill = LampOilGame().apply { repeat(10) { update(tilt = 70f, turnRate = 0f, seconds = .1f, active = true) } }
        assertEquals(OilPourStatus.TOO_FAR, spill.status)
    }

    @Test
    fun jarCapacityDecreasesBySameAmountAsLampFillIncreases() {
        val game = LampOilGame()
        assertEquals(1f, game.jarFill, .001f)
        assertEquals(0f, game.fill, .001f)

        game.update(tilt = 40f, turnRate = 0f, seconds = 5f, active = true)

        val expectedFill = 5f / 25f // 0.2
        assertEquals(expectedFill, game.fill, .001f)
        assertEquals(1f - expectedFill, game.jarFill, .001f)
    }

    @Test
    fun lampCompletesAtEightyPercentFill() {
        val game = LampOilGame()
        // Fill 85% (21.25 seconds)
        repeat(213) { game.update(tilt = 45f, turnRate = 0f, seconds = .1f, active = true) }
        assertTrue(game.fill >= 0.80f)

        // Hold upright to seal
        repeat(20) { game.update(tilt = 0f, turnRate = 0f, seconds = .1f, active = true) }
        assertEquals(OilPourStatus.COMPLETE, game.status)
    }

    @Test
    fun excessiveSpillingCausesGameOver() {
        val game = LampOilGame()
        // Excessive spill > 20%
        repeat(20) { game.update(tilt = 75f, turnRate = 0f, seconds = .1f, active = true) }
        assertEquals(OilPourStatus.FAILED, game.status)
    }
}
