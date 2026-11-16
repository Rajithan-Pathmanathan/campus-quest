package com.campusquest

import com.campusquest.story.TiltControl
import org.junit.Assert.*
import org.junit.Test

class TiltControlTest {
    private fun settled(c:TiltControl,x:Float=0f,y:Float=0f,r:Int=0,start:Long=0){repeat(30){c.sample(x,y,r,start+it*20)}}
    @Test fun `waits for actual samples and recent calibration`() {
        val c=TiltControl();c.calibrate();assertFalse(c.fresh(0))
        settled(c,0f,7f);assertTrue(c.fresh(600));assertEquals(0f,c.y,0f)
        assertFalse(c.fresh(2000));c.reset();assertFalse(c.fresh(2000))
    }
    @Test fun `accelerometer tilts move both axes without a gyroscope`() {
        val c=TiltControl();settled(c)
        settled(c,-2f,0f,start=600);assertTrue(c.x>.5f)
        settled(c,2f,0f,start=1200);assertTrue(c.x<-.5f)
        settled(c,0f,2f,start=1800);assertTrue(c.y>.5f)
        settled(c,0f,-2f,start=2400);assertTrue(c.y<-.5f)
    }
    @Test fun `neutral pose dead zone and screen rotation are handled`() {
        val c=TiltControl();settled(c,0f,7f)
        settled(c,.05f,7.05f,start=600);assertEquals(0f,c.x,0f);assertEquals(0f,c.y,0f)
        c.calibrate();settled(c,2f,3f,start=1200);settled(c,2f,3f,start=1800)
        c.calibrate();settled(c,2f,3f,start=2400);assertEquals(0f,c.x,.01f);assertEquals(0f,c.y,.01f)
        settled(c,0f,0f,r=1,start=3000);settled(c,0f,0f,r=1,start=3600)
        c.calibrate();settled(c,0f,0f,r=1,start=4200)
        settled(c,0f,-2f,r=1,start=4800);assertTrue(c.x>.5f)
    }
    @Test fun `forward tilt works when starting with phone upright`() {
        val c=TiltControl()
        repeat(30){c.sample(0f,9.81f,0,it*20L,0f)}
        assertEquals(0f,c.y,0f)
        repeat(30){c.sample(0f,9.66f,0,600+it*20L,1.7f)}
        assertTrue("Use gravity Z too; Y alone barely changes when upright",c.y<-.35f)
    }

    @Test fun `left-right tilt is reported in real degrees and ignores forward tilt`() {
        val c=TiltControl()
        // Phone held upright (portrait): gravity along y.
        settled(c,x=0f,y=9.81f)
        // Rotate 45 degrees sideways in the screen plane.
        val g=9.81f; val r=Math.toRadians(45.0)
        repeat(60){c.sample((g*kotlin.math.sin(r)).toFloat(),(g*kotlin.math.cos(r)).toFloat(),0,1000L+it*20,rawZ=0f)}
        assertEquals(45f,kotlin.math.abs(c.xDegrees),2f)
        // Tipping forward/back only (z changes, x stays 0) does not change the sideways angle.
        val d=TiltControl(); settled(d,x=0f,y=9.81f)
        repeat(60){d.sample(0f,(g*kotlin.math.cos(r)).toFloat(),0,1000L+it*20,rawZ=(g*kotlin.math.sin(r)).toFloat())}
        assertEquals(0f,d.xDegrees,2f)
    }
}
