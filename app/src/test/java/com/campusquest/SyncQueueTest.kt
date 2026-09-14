package com.campusquest

import com.campusquest.data.local.entity.SyncOperationType
import com.campusquest.data.local.entity.SyncQueueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueueTest {

    @Test
    fun syncQueueEntity_defaultValues_initializedCorrectly() {
        val entity = SyncQueueEntity(
            operationType = SyncOperationType.RECORD_DISCOVERY,
            entityId = "CP_999",
            payloadJson = "{\"score\":100}"
        )

        assertEquals(0L, entity.syncId)
        assertEquals(SyncOperationType.RECORD_DISCOVERY, entity.operationType)
        assertEquals("CP_999", entity.entityId)
        assertEquals(0, entity.retryCount)
        assertTrue(entity.createdAt > 0L)
    }

    @Test
    fun syncQueueEntity_incrementRetry_updatesState() {
        val original = SyncQueueEntity(
            syncId = 10L,
            operationType = SyncOperationType.CREATE_GAME,
            entityId = "G_NEW",
            payloadJson = "{}",
            retryCount = 0
        )

        val updated = original.copy(retryCount = original.retryCount + 1)
        assertEquals(1, updated.retryCount)
        assertEquals(10L, updated.syncId)
    }
}
