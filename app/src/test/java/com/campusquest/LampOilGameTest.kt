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
        assertEquals(OilPourStatus.RETURN_UPRIGHT, game.status)
    }

    @Test
    fun leavingCheckpointPausesPouring() {
        val game = LampOilGame()
        game.update(tilt = 45f, turnRate = .2f, seconds = 5f, active = false)

        assertEquals(0f, game.fill, .001f)
        assertEquals(OilPourStatus.PAUSED, game.status)
    }

    @Test
    fun anglesFollowTheJarRules_restPourSpill() {
        val rest = LampOilGame().apply { repeat(10) { update(tilt = 20f, turnRate = 0f, seconds = .1f, active = true) } }
        assertEquals(0f, rest.fill, .0001f)
        assertEquals(OilPourStatus.READY, rest.status)

        val pour = LampOilGame().apply { repeat(10) { update(tilt = -45f, turnRate = 0f, seconds = .1f, active = true) } }
        assertTrue(pour.fill > 0f)
        assertEquals(OilPourStatus.POURING, pour.status)

        val spill = LampOilGame().apply { repeat(10) { update(tilt = 70f, turnRate = 0f, seconds = .1f, active = true) } }
        assertEquals(OilPourStatus.TOO_FAR, spill.status)
    }
}
