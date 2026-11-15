package com.campusquest

import com.campusquest.story.*
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationAccessTest {
    @Test fun `first request asks permission even with device location off`() {
        assertEquals(LocationAccessAction.REQUEST_PERMISSION,locationAccessAction(false,false,false,false))
    }
    @Test fun `granted permission with services off asks to enable device services`() {
        assertEquals(LocationAccessAction.ENABLE_SERVICES,locationAccessAction(true,false,true,false))
    }
    @Test fun `blocked permission leads to app settings and ordinary denial can retry`() {
        assertEquals(LocationAccessAction.APP_SETTINGS,locationAccessAction(false,true,true,false))
        assertEquals(LocationAccessAction.REQUEST_PERMISSION,locationAccessAction(false,true,true,true))
    }
    @Test fun `ready location never requests permission again`() {
        assertEquals(LocationAccessAction.READY,locationAccessAction(true,true,true,false))
    }
}
