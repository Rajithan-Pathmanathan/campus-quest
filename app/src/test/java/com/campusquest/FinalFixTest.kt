package com.campusquest

import com.campusquest.data.repository.GameRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinalFixTest {

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
