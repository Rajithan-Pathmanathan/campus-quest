package com.campusquest

import com.campusquest.domain.quest.QuestZone
import com.campusquest.ui.quest.ScanSignals
import com.campusquest.ui.quest.ScanState
import com.campusquest.ui.quest.ScanStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestScanStateMachineTest {

    private fun signals(
        fix: Boolean = true,
        zone: QuestZone = QuestZone.RELIC,
        started: Boolean = true,
        threshold: Boolean = false,
        near: Boolean = false
    ) = ScanSignals(fix, zone, started, threshold, near)

    private fun resolve(current: ScanState, s: ScanSignals) = ScanStateMachine.resolve(current, s)

    @Test
    fun `no location fix waits for location`() {
        assertEquals(ScanState.WAITING_FOR_LOCATION, resolve(ScanState.OUTSIDE_CHECKPOINT, signals(fix = false)))
    }

    @Test
    fun `outside the relic ring is outside checkpoint`() {
        for (zone in listOf(QuestZone.FAR, QuestZone.OUTER, QuestZone.MIDDLE)) {
            assertEquals(ScanState.OUTSIDE_CHECKPOINT, resolve(ScanState.WAITING_FOR_LOCATION, signals(zone = zone)))
        }
    }

    @Test
    fun `relic zone before the scan is begun is ready to scan`() {
        assertEquals(ScanState.READY_TO_SCAN, resolve(ScanState.OUTSIDE_CHECKPOINT, signals(started = false)))
    }

    @Test
    fun `begun scan below threshold is scanning`() {
        assertEquals(ScanState.SCANNING, resolve(ScanState.READY_TO_SCAN, signals()))
    }

    @Test
    fun `threshold reached with proximity far waits for proximity and never reveals`() {
        assertEquals(
            ScanState.WAITING_FOR_PROXIMITY,
            resolve(ScanState.SCANNING, signals(threshold = true, near = false))
        )
    }

    @Test
    fun `threshold reached with proximity near succeeds`() {
        assertEquals(ScanState.SUCCESS, resolve(ScanState.SCANNING, signals(threshold = true, near = true)))
        assertEquals(ScanState.SUCCESS, resolve(ScanState.WAITING_FOR_PROXIMITY, signals(threshold = true, near = true)))
    }

    @Test
    fun `proximity alone without the fusion threshold is not enough`() {
        assertEquals(ScanState.SCANNING, resolve(ScanState.SCANNING, signals(threshold = false, near = true)))
    }

    @Test
    fun `fusion dropping below the threshold goes back to scanning`() {
        assertEquals(ScanState.SCANNING, resolve(ScanState.WAITING_FOR_PROXIMITY, signals(threshold = false)))
    }

    @Test
    fun `leaving the relic ring while scanning is outside checkpoint`() {
        assertEquals(
            ScanState.OUTSIDE_CHECKPOINT,
            resolve(ScanState.SCANNING, signals(zone = QuestZone.MIDDLE, started = false))
        )
    }

    @Test
    fun `latched states never change from signals`() {
        val latched = listOf(
            ScanState.LOADING, ScanState.UNAVAILABLE, ScanState.ALREADY_FOUND, ScanState.SUCCESS,
            ScanState.REVEALING, ScanState.COMPLETED, ScanState.ERROR
        )
        for (state in latched) {
            assertEquals(state, resolve(state, signals(fix = false)))
            assertEquals(state, resolve(state, signals(threshold = true, near = true)))
        }
    }

    @Test
    fun `discovery flows one way success reveal completed`() {
        assertTrue(ScanStateMachine.canTransition(ScanState.SCANNING, ScanState.SUCCESS))
        assertTrue(ScanStateMachine.canTransition(ScanState.WAITING_FOR_PROXIMITY, ScanState.SUCCESS))
        assertTrue(ScanStateMachine.canTransition(ScanState.SUCCESS, ScanState.REVEALING))
        assertTrue(ScanStateMachine.canTransition(ScanState.REVEALING, ScanState.COMPLETED))
    }

    @Test
    fun `invalid transitions are rejected`() {
        // Reveal is only reachable through SUCCESS.
        assertFalse(ScanStateMachine.canTransition(ScanState.SCANNING, ScanState.REVEALING))
        assertFalse(ScanStateMachine.canTransition(ScanState.WAITING_FOR_PROXIMITY, ScanState.REVEALING))
        // Success only after scanning has started.
        assertFalse(ScanStateMachine.canTransition(ScanState.READY_TO_SCAN, ScanState.SUCCESS))
        assertFalse(ScanStateMachine.canTransition(ScanState.OUTSIDE_CHECKPOINT, ScanState.SCANNING))
        // Nothing leaves the terminal states.
        assertFalse(ScanStateMachine.canTransition(ScanState.COMPLETED, ScanState.SCANNING))
        assertFalse(ScanStateMachine.canTransition(ScanState.REVEALING, ScanState.SCANNING))
        assertFalse(ScanStateMachine.canTransition(ScanState.ALREADY_FOUND, ScanState.READY_TO_SCAN))
        assertFalse(ScanStateMachine.canTransition(ScanState.UNAVAILABLE, ScanState.WAITING_FOR_LOCATION))
        assertEquals(ScanState.SCANNING, ScanStateMachine.transition(ScanState.SCANNING, ScanState.REVEALING))
    }

    @Test
    fun `error can retry a save or reload`() {
        assertTrue(ScanStateMachine.canTransition(ScanState.ERROR, ScanState.SUCCESS))
        assertTrue(ScanStateMachine.canTransition(ScanState.ERROR, ScanState.LOADING))
        assertFalse(ScanStateMachine.canTransition(ScanState.ERROR, ScanState.REVEALING))
    }
}
