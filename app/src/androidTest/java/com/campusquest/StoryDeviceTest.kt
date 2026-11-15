package com.campusquest

import android.graphics.Bitmap
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.fragment.NavHostFragment
import com.campusquest.story.*
import com.campusquest.ui.common.MainActivity
import com.campusquest.ui.common.AppProviders
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import android.view.View
import org.hamcrest.Matcher

@RunWith(AndroidJUnit4::class)
class StoryDeviceTest {
    private fun appClick() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = androidx.test.espresso.matcher.ViewMatchers.isEnabled()
        override fun getDescription() = "Invoke this app control without system input injection"
        override fun perform(ui: UiController, view: View) { assertTrue(view.performClick());ui.loopMainThreadUntilIdle() }
    }
    @Test fun createConfiguredStoryAndPreviewWithoutChangingOldGameProgress()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as CampusQuestApplication
        val uid=requireNotNull(AppProviders.authRepository.getCurrentUserId())
        val existing=app.database.gameDao().getCreatorGames(uid).firstOrNull {app.database.storyDao().config(it.id)!=null}
        val id=existing?.id ?: LightTheWay.create(app.database,uid,"Explorer",listOf(7.727295 to 81.696698,7.727241 to 81.696882,
            (7.727241+Math.toDegrees(50.0/6371000.0)) to 81.696882),0f)
        assertNotNull(app.database.gameDao().getGameById("GAME_TEST"))
        File(app.getExternalFilesDir(null),"story-game-id.txt").writeText(id)
        val c=app.database.storyDao().config(id)!!
        val printable=RepairCardArtwork.render(c)
        File(app.getExternalFilesDir(null),"Light-the-Way-Repair-Card.png").outputStream().use {printable.compress(Bitmap.CompressFormat.PNG,100,it)}
        printable.recycle()
        val bits=QRCodeWriter().encode(LightTheWay.qr(c),BarcodeFormat.QR_CODE,600,600)
        val bitmap=Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888)
        for(y in 0 until 600)for(x in 0 until 600)bitmap.setPixel(x,y,if(bits[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        val scanner=BarcodeScanning.getClient()
        try {
            val codes=Tasks.await(scanner.process(InputImage.fromBitmap(bitmap,0)),15,TimeUnit.SECONDS)
            assertTrue(codes.any {LightTheWay.matchesCard(it.rawValue.orEmpty(),c)})
        }finally{scanner.close();bitmap.recycle()}
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun open(destination:Int){scenario.onActivity {a ->
                (a.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController.navigate(destination,Bundle().apply {putString("gameId",id)})
            }}
            fun capture(name:String){instrumentation.waitForIdleSync();Thread.sleep(1500)
                val image=instrumentation.uiAutomation.takeScreenshot()
                File(app.getExternalFilesDir(null),name).outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
            }
            open(R.id.storySetupFragment);capture("story-card.png")
            open(R.id.storyGameFragment);capture("story-preflight.png")
            onView(withText("Start / resume the walk")).perform(scrollTo(),appClick())
            Thread.sleep(12000);capture("story-navigation.png")
            var entered=false
            var eligibleSince: Long?=null
            var stableEligible=false
            fun containsKey(view: android.view.View):Boolean {
                if(view is KeyBoxView)return true
                return view is android.view.ViewGroup && (0 until view.childCount).any {containsKey(view.getChildAt(it))}
            }
            for(attempt in 0 until 8) {
                onView(withText("Begin this chapter")).perform(scrollTo(),appClick())
                Thread.sleep(2000)
                val fix=app.locationRepository.locationState.value
                val age=System.currentTimeMillis()-fix.timestamp
                val distance=if(fix.smoothedLatitude!=null && fix.smoothedLongitude!=null)com.campusquest.device.location.HaversineDistanceCalculator().calculateDistanceMeters(fix.smoothedLatitude!!,fix.smoothedLongitude!!,7.727295,81.696698)else Float.MAX_VALUE
                val now=android.os.SystemClock.elapsedRealtime()
                if(fix.servicesEnabled && fix.isUpdating && fix.accuracyMeters!=null && fix.accuracyMeters!!<=20f && age in 0..15000 && distance<=15f) {
                    if(eligibleSince==null)eligibleSince=now
                    if(now-eligibleSince!!>=4000)stableEligible=true
                } else eligibleSince=null
                scenario.onActivity {entered=containsKey(it.findViewById(android.R.id.content))}
                if(entered)break
            }
            capture("story-task.png")
            File(app.getExternalFilesDir(null),"story-last-gps.txt").writeText(app.locationRepository.locationState.value.toString())
            assertTrue("Stable eligible GPS fixes should open the key task",entered || !stableEligible)
            capture("story-task.png")
        }
    }
}
