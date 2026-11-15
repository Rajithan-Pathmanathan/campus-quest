package com.campusquest

import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.domain.fusion.DiscoveryGateEvaluator
import com.campusquest.domain.quest.QuestZone
import com.campusquest.ui.quest.ScanSignals
import com.campusquest.ui.quest.ScanState
import com.campusquest.ui.quest.ScanStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalFixTest {

    @Test
    fun shieldFallback_onlyCountsAfterFusionIsReached() {
        assertFalse(DiscoveryGateEvaluator.canDiscover(fusionThresholdReached = false, proximityNear = false, shieldHeld = true))
        assertTrue(DiscoveryGateEvaluator.canDiscover(fusionThresholdReached = true, proximityNear = false, shieldHeld = true))
        assertTrue(DiscoveryGateEvaluator.canDiscover(fusionThresholdReached = true, proximityNear = true))
        assertFalse(DiscoveryGateEvaluator.canDiscover(fusionThresholdReached = true, proximityNear = false))
    }

    @Test
    fun scanSucceedsWithHeldShield_onPhonesWithoutProximity() {
        val waiting = ScanSignals(hasLocationFix = true, zone = QuestZone.RELIC, scanStarted = true,
            fusionThresholdReached = true, proximityNear = false)
        assertEquals(ScanState.WAITING_FOR_PROXIMITY, ScanStateMachine.resolve(ScanState.SCANNING, waiting))
        assertEquals(ScanState.SUCCESS, ScanStateMachine.resolve(ScanState.WAITING_FOR_PROXIMITY, waiting.copy(shieldHeld = true)))
    }

    @Test
    fun syncPayloadFieldsAreRead() {
        val json = """{"gameId":"G_1","checkpointId":"CP-2","userId":"u9","foundAt":1700}"""
        assertEquals("G_1", GameRepositoryImpl.payloadValue(json, "gameId"))
        assertEquals("u9", GameRepositoryImpl.payloadValue(json, "userId"))
        assertEquals("1700", GameRepositoryImpl.payloadValue(json, "foundAt"))
        assertNull(GameRepositoryImpl.payloadValue(json, "missing"))
        assertNull(GameRepositoryImpl.payloadValue("{}", "gameId"))
    }
}
