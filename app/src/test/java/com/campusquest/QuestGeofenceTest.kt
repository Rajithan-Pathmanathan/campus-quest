package com.campusquest

import com.campusquest.device.location.geofence.QuestGeofenceCoordinator
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.quest.QuestZones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestGeofenceTest {

    private fun cp(id: String, order: Int, radius: Float = 15f) =
        Checkpoint(id, "G1", id, 6.97, 79.91, radius, LightSignature(0f, 100f), "c", "l", order)

    private val chapters = listOf(cp("C", 3), cp("A", 1), cp("B", 2))

    @Test
    fun fenceIsTheNextUnfoundChapter_widenedToTheMiddleRing() {
        val fence = QuestGeofenceCoordinator.fenceFor(chapters, setOf("A"))!!
        assertEquals("B", fence.id)
        assertEquals(QuestZones.MIDDLE_RING_M, fence.radiusM)
    }

    @Test
    fun largeCreatorRadiusIsKept() {
        assertEquals(250f, QuestGeofenceCoordinator.fenceFor(listOf(cp("A", 1, 250f)), emptySet())!!.radiusM)
    }

    @Test
    fun noFenceWhenTheQuestIsComplete() {
        assertNull(QuestGeofenceCoordinator.fenceFor(chapters, setOf("A", "B", "C")))
    }
}
