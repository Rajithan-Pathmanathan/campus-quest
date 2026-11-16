package com.campusquest

import com.campusquest.story.GoldenWater
import org.junit.Assert.*
import org.junit.Test

class GoldenWaterTest {
    @Test fun balancedJugFinishesAfterOneMinute() {
        val jug = GoldenWater()
        fun balance() {
            val next = jug.elapsed + .1f
            val sway = .35f + .35f * next / 60f
            jug.step(-kotlin.math.sin(next*1.7f)*sway, -kotlin.math.sin(next*1.1f)*sway, .1f)
        }
        repeat(599) { balance() }
        assertFalse(jug.complete)
        repeat(2) { balance() }
        assertTrue(jug.complete)
        assertEquals(1f, jug.remaining, .001f)
    }
    @Test fun sustainedTiltSpillsTheJugAndCannotWin() {
        val jug = GoldenWater()
        repeat(1000) { jug.step(1f, 1f, .1f) }
        assertTrue(jug.empty)
        assertFalse(jug.complete)
    }
    @Test fun invalidReadingsAndBackgroundTimeDoNotSkipChallenge() {
        val jug = GoldenWater()
        jug.step(Float.NaN, 0f, 60f)
        assertEquals(0f, jug.elapsed, 0f)
        jug.step(0f, 0f, 60f)
        assertEquals(.1f, jug.elapsed, .001f)
    }
}
