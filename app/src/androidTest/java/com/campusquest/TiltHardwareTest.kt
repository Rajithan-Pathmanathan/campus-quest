package com.campusquest

import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.campusquest.story.*
import com.campusquest.ui.common.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TiltHardwareTest {
    @Test fun phoneAccelerometerDeliversCalibratedReadingsAndTouchCannotMoveKey() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        var sensors:StorySensors?=null
        ActivityScenario.launch(MainActivity::class.java).use {scenario ->
            scenario.onActivity {activity ->
                sensors=StorySensors(activity).also {it.start();it.calibrate()}
                val maze=KeyBoxView(activity).apply {active=true;layout(0,0,500,500)}
                val bitmap=android.graphics.Bitmap.createBitmap(1000,1000,android.graphics.Bitmap.Config.ARGB_8888)
                maze.layout(0,0,1000,1000);maze.draw(android.graphics.Canvas(bitmap))
                java.io.File(activity.getExternalFilesDir(null),"tilt-maze-preview.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
                val jug=GoldenWaterView(activity).apply {layout(0,0,1000,1000)}
                val jugBitmap=android.graphics.Bitmap.createBitmap(1000,1000,android.graphics.Bitmap.Config.ARGB_8888)
                jug.draw(android.graphics.Canvas(jugBitmap))
                java.io.File(activity.getExternalFilesDir(null),"golden-water-preview.png").outputStream().use {jugBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                jugBitmap.recycle()
                val x=maze.box.x;val y=maze.box.y
                for((action,px,py) in listOf(Triple(MotionEvent.ACTION_DOWN,50f,450f),Triple(MotionEvent.ACTION_MOVE,300f,100f),Triple(MotionEvent.ACTION_UP,300f,100f))){
                    val event=MotionEvent.obtain(0,0,action,px,py,0)
                    maze.dispatchTouchEvent(event);event.recycle()
                }
                assertEquals(x,maze.box.x,0f);assertEquals(y,maze.box.y,0f)
            }
            try {
                Thread.sleep(2500);instrumentation.waitForIdleSync()
                scenario.onActivity {
                    assertTrue("This test phone must have an accelerometer",sensors!!.hasTilt)
                    assertTrue("Real accelerometer readings must arrive and calibrate",sensors!!.tilt.fresh(SystemClock.elapsedRealtime()))
                }
            } finally {scenario.onActivity {sensors?.stop()}}
        }
    }
}
