package com.campusquest

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.SensorState
import com.campusquest.domain.model.TrialStatus
import com.campusquest.domain.quest.QuestZone
import com.campusquest.ui.quest.NextCheckpointResolver
import com.campusquest.ui.quest.QuestScanUiState
import com.campusquest.ui.quest.ScanAction
import com.campusquest.ui.quest.ScanNarrator
import com.campusquest.ui.quest.ScanState
import com.campusquest.ui.quest.SignalLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestScanProgressionTest {

    private fun cp(id: String, order: Int, motion: String = "SHAKE") = Checkpoint(
        id = id, gameId = "g", name = id, lat = 0.0, lng = 0.0,
        lightSignature = LightSignature(0f, 1000f), clue = "c", lore = "l", order = order, motionType = motion
    )

    @Test
    fun `next checkpoint follows the game order not the list order`() {
        val cps = listOf(cp("b", 2), cp("c", 3), cp("a", 1))
        assertEquals("a", NextCheckpointResolver.next(cps, emptyList())?.id)
        assertEquals("b", NextCheckpointResolver.next(cps, listOf("a"))?.id)
        assertEquals("c", NextCheckpointResolver.next(cps, listOf("a", "b"))?.id)
        assertNull(NextCheckpointResolver.next(cps, listOf("a", "b", "c")))
    }

    @Test
    fun `final checkpoint is the last one still to find`() {
        val cps = listOf(cp("a", 1), cp("b", 2))
        assertFalse(NextCheckpointResolver.isFinal(cps, emptyList(), cps[0]))
        assertTrue(NextCheckpointResolver.isFinal(cps, listOf("a"), cps[1]))
        // Works with arbitrary ids, nothing hard-coded.
        assertTrue(NextCheckpointResolver.isFinal(listOf(cp("only", 1)), emptyList(), cp("only", 1)))
    }

    private fun fusion(percent: Int) = FusionResult(
        gameId = "g", checkpointId = "a", gpsScore = 1f, lightScore = 1f, motionScore = 1f,
        totalScore = percent / 100f, progressPercent = percent, thresholdReached = false,
        proximityNear = false, activeSignals = emptySet()
    )

    private fun hud(state: ScanState, zone: QuestZone = QuestZone.FAR, percent: Int = 0, distance: Float? = 250f) =
        ScanNarrator.hud(
            QuestScanUiState(scanState = state, zone = zone),
            cp("a", 1), distance, fusion(percent), null
        )

    @Test
    fun `far from the checkpoint shows rounded distance and warmth, not a score`() {
        val s = hud(ScanState.OUTSIDE_CHECKPOINT, QuestZone.OUTER, distance = 183f)
        assertEquals("~180 m", s.distanceText)
        assertNotNull(s.warmth)
        assertNotNull(s.heartbeatIntervalMs)
        assertNull(s.glow)
    }

    @Test
    fun `scanning shows the ember glow and trial hint only`() {
        val s = hud(ScanState.SCANNING, QuestZone.RELIC, percent = 45, distance = 2f)
        assertEquals("The ember flickers…", s.headline)
        assertNotNull(s.trialHint)
        assertNotNull(s.lightHint)
        assertNull(s.distanceText)
        assertNull(s.shieldHint)
    }

    @Test
    fun `waiting for proximity asks the player to cup the ember`() {
        val s = hud(ScanState.WAITING_FOR_PROXIMITY, QuestZone.RELIC, percent = 90, distance = 1f)
        assertEquals("Cup the ember — hold the Resonator close (or press and hold here).", s.shieldHint)
    }

    @Test
    fun `primary actions match the state`() {
        assertEquals(ScanAction.BEGIN_SCAN, hud(ScanState.READY_TO_SCAN).primaryAction)
        assertEquals(ScanAction.NONE, hud(ScanState.SCANNING).primaryAction)
        assertEquals(ScanAction.BACK, hud(ScanState.ALREADY_FOUND).primaryAction)
    }

    @Test
    fun `no screen text ever contains a percent sign`() {
        for (state in ScanState.entries) {
            val s = hud(state, QuestZone.RELIC, percent = 88, distance = 5f)
            val text = listOfNotNull(s.headline, s.distanceText, s.warmth, s.glow?.label, s.lightHint, s.trialHint, s.shieldHint)
                .joinToString(" ")
            assertFalse("$state: $text", text.contains('%'))
        }
    }

    private fun signals(zone: QuestZone, distance: Float?, sensor: SensorState?, motion: String = "SHAKE") =
        ScanNarrator.signals(
            QuestScanUiState(scanState = ScanState.SCANNING, zone = zone),
            cp("a", 1, motion), distance, sensor
        )

    @Test
    fun `gps chip goes from searching to nearby to locked`() {
        assertEquals("Searching", signals(QuestZone.FAR, null, null).first.status)
        assertEquals("Far", signals(QuestZone.FAR, 500f, null).first.status)
        assertEquals("Nearby", signals(QuestZone.MIDDLE, 60f, null).first.status)
        val locked = signals(QuestZone.RELIC, 3f, null).first
        assertEquals("Locked", locked.status)
        assertEquals(SignalLevel.MATCHED, locked.level)
    }

    @Test
    fun `light chip uses words for the checkpoint's light range`() {
        // checkpoint light range is 0..1000 lux
        assertEquals("Waiting", signals(QuestZone.RELIC, 3f, SensorState(currentLux = null)).second.status)
        assertEquals("Match", signals(QuestZone.RELIC, 3f, SensorState(currentLux = 400f)).second.status)
        assertEquals("Off", signals(QuestZone.RELIC, 3f, SensorState(currentLux = 90_000f)).second.status)
    }

    @Test
    fun `motion chip follows the trial`() {
        assertEquals("Ready", signals(QuestZone.RELIC, 3f, SensorState(trialStatus = TrialStatus(remaining = 3))).third.status)
        assertEquals("In progress", signals(QuestZone.RELIC, 3f, SensorState(trialStatus = TrialStatus(remaining = 2))).third.status)
        assertEquals("Done", signals(QuestZone.RELIC, 3f, SensorState(trialStatus = TrialStatus(completed = true))).third.status)
        // a tilt trial counts as in progress once the player has started holding steady
        assertEquals(
            "In progress",
            signals(QuestZone.RELIC, 3f, SensorState(trialStatus = TrialStatus(started = true)), motion = "TILT").third.status
        )
    }

    @Test
    fun `signal chips are hidden while loading or on error`() {
        assertFalse(hud(ScanState.LOADING).showSignals)
        assertFalse(hud(ScanState.ERROR).showSignals)
        assertTrue(hud(ScanState.SCANNING, QuestZone.RELIC).showSignals)
    }
}
