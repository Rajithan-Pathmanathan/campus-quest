package com.campusquest

import com.campusquest.device.sensor.motion.GravityFilter
import com.campusquest.device.sensor.motion.MotionDetector
import com.campusquest.device.sensor.motion.RepeatedGestureDetector
import com.campusquest.device.sensor.motion.SteadyHoldDetector
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.TrialStatus
import com.campusquest.domain.quest.LightMode
import com.campusquest.domain.quest.QuestCues
import com.campusquest.domain.quest.QuestZone
import com.campusquest.domain.quest.nextQuestZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M3 story game logic: zones, player-facing cues (no raw numbers) and motion trials. */
class M3StoryLogicTest {

    private val still = GravityFilter.LinearAcceleration(0f, 0f, 0f, 0f)
    private val level = GravityFilter.GravityVector(0f, 0f, 9.8f)
    private val tipped = GravityFilter.GravityVector(0f, 6.0f, 7.7f) // ~38 degrees

    @Test
    fun zones_followTheRings_andRelicNeedsGoodAccuracy() {
        assertEquals(QuestZone.FAR, nextQuestZone(QuestZone.FAR, 350f, 5f))
        assertEquals(QuestZone.OUTER, nextQuestZone(QuestZone.FAR, 150f, 5f))
        assertEquals(QuestZone.MIDDLE, nextQuestZone(QuestZone.FAR, 60f, 5f))
        assertEquals(QuestZone.RELIC, nextQuestZone(QuestZone.MIDDLE, 8f, 5f))
        // Within 10 m but GPS too vague: the relic must not appear.
        assertEquals(QuestZone.MIDDLE, nextQuestZone(QuestZone.MIDDLE, 8f, 30f))
        assertEquals(QuestZone.FAR, nextQuestZone(QuestZone.MIDDLE, null, 5f))
    }

    @Test
    fun relic_staysUntilPlayerWalksAway_noFlicker() {
        assertEquals(QuestZone.RELIC, nextQuestZone(QuestZone.RELIC, 15f, 25f))
        assertEquals(QuestZone.MIDDLE, nextQuestZone(QuestZone.RELIC, 25f, 5f))
    }

    @Test
    fun playerCues_neverShowRawNumbers() {
        assertEquals("~250 m", QuestCues.distanceText(247f))
        assertEquals("~45 m", QuestCues.distanceText(44f))
        assertEquals("Very close…", QuestCues.distanceText(12f))
        assertEquals("~1.2 km", QuestCues.distanceText(1234f))
        assertEquals("Freezing", QuestCues.warmth(500f))
        assertEquals("Burning!", QuestCues.warmth(8f))
        for (p in 0..100) assertFalse(QuestCues.glow(p).label.contains("%"))
    }

    @Test
    fun heartbeat_speedsUpNearTheEmber_andIsSilentWhenFar() {
        assertNull(QuestCues.heartbeatIntervalMs(QuestZone.FAR, 400f))
        val outer = QuestCues.heartbeatIntervalMs(QuestZone.OUTER, 190f)!!
        val middle = QuestCues.heartbeatIntervalMs(QuestZone.MIDDLE, 30f)!!
        assertTrue(middle < outer)
    }

    @Test
    fun lightHints_matchEachChapter() {
        val light = LightSignature(10_000f, 120_000f)
        val shade = LightSignature(500f, 10_000f)
        val dark = LightSignature(0f, 60f)
        assertEquals(LightMode.BRIGHT, LightMode.of(light))
        assertEquals(LightMode.SHADE, LightMode.of(shade))
        assertEquals(LightMode.DARK, LightMode.of(dark))
        assertTrue(QuestCues.lightHint(shade, 60_000f).contains("Find shade"))
        assertTrue(QuestCues.lightHint(light, 300f).contains("sunlight"))
        assertTrue(QuestCues.lightHint(dark, 400f).contains("afraid of the light"))
        assertTrue(QuestCues.lightHint(dark, 5f).contains("just right"))
    }

    @Test
    fun repeatedGesture_needsThreeSeparateShakes() {
        val single = object : MotionDetector {
            var fired = false
            override fun process(linear: GravityFilter.LinearAcceleration, gravity: GravityFilter.GravityVector, timestamp: Long): Boolean {
                fired = linear.magnitude > 10f; return fired
            }
            override val isVerified get() = fired
            override fun reset() { fired = false }
        }
        val shake = GravityFilter.LinearAcceleration(15f, 0f, 0f, 15f)
        val trial = RepeatedGestureDetector(single, required = 3, cooldownMs = 400)

        trial.process(shake, level, 0)
        trial.process(shake, level, 100) // same shake, inside cooldown: not counted
        assertEquals(2, trial.status.remaining)
        trial.process(shake, level, 600)
        trial.process(shake, level, 1200)
        assertTrue(trial.isVerified)
        assertEquals("The ember is awake!", QuestCues.trialHint(MotionType.SHAKE, trial.status))
    }

    @Test
    fun waterJar_spillRestartsTheHold() {
        val jar = SteadyHoldDetector(holdMs = 10_000, maxAngleDeg = 15f)
        jar.process(still, level, 0)
        jar.process(still, level, 6_000)
        jar.process(still, tipped, 6_500) // spill
        assertTrue(jar.status.justSpilled)
        assertEquals("Spilled! Steady… start again.", QuestCues.trialHint(MotionType.TILT, jar.status))
        jar.process(still, tipped, 15_000) // only 8.5 s steady since the spill
        assertFalse(jar.isVerified)
        jar.process(still, tipped, 16_600)
        assertTrue(jar.isVerified)
    }

    @Test
    fun trialHints_countDown() {
        assertEquals("2 more shakes!", QuestCues.trialHint(MotionType.SHAKE, TrialStatus(remaining = 2)))
        assertEquals("One more sweep!", QuestCues.trialHint(MotionType.SWEEP, TrialStatus(remaining = 1)))
    }
}
