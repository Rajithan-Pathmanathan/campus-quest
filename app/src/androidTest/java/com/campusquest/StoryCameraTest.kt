package com.campusquest

import android.Manifest
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.campusquest.story.CardScanner
import com.campusquest.ui.common.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

class CameraProbeFragment:Fragment() {
    var scanner:CardScanner?=null
    var message=""
    override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,saved:Bundle?)=PreviewView(requireContext())
    @androidx.camera.core.ExperimentalGetImage
    override fun onViewCreated(view:View,saved:Bundle?){scanner=CardScanner(this,{}, {message=it}).also {it.start(view as PreviewView)}}
    override fun onDestroyView(){scanner?.close();super.onDestroyView()}
}
@RunWith(AndroidJUnit4::class)
class StoryCameraTest {
    @Test fun scannerControlsActualTorchAndCleansUp() {
        val i=InstrumentationRegistry.getInstrumentation()
        val grant=i.uiAutomation.executeShellCommand("pm grant ${i.targetContext.packageName} ${Manifest.permission.CAMERA}")
        grant.close()
        var probe:CameraProbeFragment?=null
        ActivityScenario.launch(MainActivity::class.java).use {scenario ->
            scenario.onActivity {a ->
                val frame=FrameLayout(a).apply {id=View.generateViewId()}
                (a.findViewById<View>(android.R.id.content) as android.view.ViewGroup).addView(frame,android.view.ViewGroup.LayoutParams(-1,-1))
                probe=CameraProbeFragment()
                a.supportFragmentManager.beginTransaction().add(frame.id,probe!!).commitNow()
            }
            Thread.sleep(5000);i.waitForIdleSync()
            assertTrue("Expected rear flash on this demonstration phone: ${probe?.message}",probe?.scanner?.hasFlash==true)
            scenario.onActivity {probe!!.scanner!!.toggleTorch()}
            Thread.sleep(1000);i.waitForIdleSync();assertTrue(probe!!.scanner!!.torchOn)
            scenario.onActivity {probe!!.scanner!!.toggleTorch()}
            Thread.sleep(1000);i.waitForIdleSync();assertFalse(probe!!.scanner!!.torchOn)
            scenario.onActivity {probe!!.scanner!!.stop()}
            assertFalse(probe!!.scanner!!.torchOn)
        }
    }
}
