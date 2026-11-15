package com.campusquest

import com.campusquest.story.StoryArrival
import org.junit.Assert.*
import org.junit.Test

class StoryArrivalTest {
    @Test fun `arrival needs continuous fixes and exit has hysteresis`() {
        val arrival=StoryArrival()
        assertFalse(arrival.update(10f,5f,0,true,0))
        assertFalse(arrival.update(16f,5f,0,true,2000))
        assertFalse(arrival.update(10f,5f,0,true,3000))
        assertTrue(arrival.update(10f,5f,0,true,6000))
        assertTrue(arrival.update(24f,5f,0,true,7000))
        assertFalse(arrival.update(26f,5f,0,true,8000))
    }
    @Test fun `missing stale disabled and inaccurate readings never arrive`() {
        for(accuracy in listOf(null,21f,Float.NaN)) {
            val a=StoryArrival();a.update(1f,accuracy,0,true,0)
            assertFalse(a.update(1f,accuracy,0,true,10000))
        }
        val a=StoryArrival();a.update(1f,5f,0,true,0)
        assertFalse(a.update(1f,5f,16000,true,5000))
        assertFalse(a.update(1f,5f,0,false,10000))
    }
}
