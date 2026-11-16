package com.campusquest

import com.campusquest.domain.model.CheckpointActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckpointActivityTest {
    @Test
    fun newValuesRoundTripFromStorage() {
        CheckpointActivity.entries.forEach { activity ->
            assertEquals(activity, CheckpointActivity.fromStored(activity.name, order = 1))
        }
    }

    @Test
    fun oldMotionValuesMapToTheThreeStoryChaptersByOrder() {
        assertEquals(CheckpointActivity.TILT_MAZE, CheckpointActivity.fromStored("SWEEP", 1))
        assertEquals(CheckpointActivity.QR_SCAN, CheckpointActivity.fromStored("SHAKE", 2))
        assertEquals(CheckpointActivity.LAMP_OIL, CheckpointActivity.fromStored("TILT", 3))
    }

    @Test
    fun labelsArePlainCreatorLanguage() {
        assertEquals(listOf("Tilt Maze", "QR Scan", "Lamp-Oil Pouring"), CheckpointActivity.entries.map { it.label })
    }
}
