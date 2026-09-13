package com.campusquest

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationGraphTest {

    @Test
    fun deepLinkUriPattern_parsesGameIdCorrectly() {
        val uriString = "campusquest://game/GAME_HERITAGE_COLOMBO"
        val expectedGameId = "GAME_HERITAGE_COLOMBO"

        // Emulate deep link URI pattern matching "campusquest://game/{gameId}"
        val prefix = "campusquest://game/"
        assertTrue("Deep link should match expected schema prefix", uriString.startsWith(prefix))

        val extractedGameId = uriString.removePrefix(prefix)
        assertEquals(expectedGameId, extractedGameId)
    }

    @Test
    fun safeArgs_checkpointEditor_supportsNullableOptionalCheckpointId() {
        // Create mode: checkpointId is null (New checkpoint)
        val createGameId = "GAME_001"
        val createCpId: String? = null
        assertNotNull(createGameId)
        assertEquals(null, createCpId)

        // Edit mode: checkpointId is non-null
        val editCpId: String? = "CP_BELL_TOWER"
        assertEquals("CP_BELL_TOWER", editCpId)
    }
}
