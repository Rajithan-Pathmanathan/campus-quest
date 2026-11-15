package com.campusquest.story

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.os.*
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.device.location.HaversineDistanceCalculator
import com.campusquest.domain.model.*
import com.campusquest.ui.common.AppProviders
import com.campusquest.ui.quest.TrailCompassView
import com.campusquest.ui.quest.TrailGuidance
import kotlinx.coroutines.*
import kotlin.math.abs

/** Story screen owns hardware only while resumed. Awards go through the shared transactional repository. */
class StoryGameFragment : Fragment() {
    private val app get() = CampusQuestApplication.instance
    private val gameId get() = arguments?.getString("gameId").orEmpty()
    private var uid=""
    private var config: StoryConfigEntity?=null
    private var session: StorySessionEntity?=null
    private var checkpoints: List<Checkpoint> = emptyList()
    private var found: List<String> = emptyList()
    private var sensors: StorySensors?=null
    private var body: LinearLayout?=null
    private var status: TextView?=null
    private var live: TextView?=null
    private var lightHint: TextView?=null
    private var diagnostics: TextView?=null
    private var compass: TrailCompassView?=null
    private var maze: KeyBoxView?=null
    private var lamp: LampView?=null
    private var scanner: CardScanner?=null
    private var cameraPreview: PreviewView?=null
    private var torchButton: Button?=null
    private var stage="loading"
    private var useRelative=false
    private var arrived=false
    private var saving=false
    private var coverPressed=false
    private var lastTick=0L
    private var cardErrorAt=0L
    private val arrivalHold=StoryArrival()
    private val alignHold=ContinuousHold(2000)
    private val coverHold=ContinuousHold(1000)
    private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshLocation()
        if(stage=="card" && cameraGranted()) openCamera()
    }
    private fun closeScanner() {
        scanner?.close();scanner=null
        cameraPreview?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
        cameraPreview=null
    }
    private fun cameraGranted()=ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        body=StoryUi.column(requireContext())
        return ScrollView(requireContext()).apply {addView(body)}
    }
    override fun onViewCreated(view: View, saved: Bundle?) {
        sensors=StorySensors(requireContext())
        stage="loading";draw()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                uid=requireNotNull(AppProviders.authRepository.getCurrentUserId()) {"Please sign in to play"}
                config=app.database.storyDao().config(gameId)
                requireNotNull(config) {"This game has no Light the Way setup"}
                checkpoints=AppProviders.gameRepository.getGameCheckpoints(gameId).sortedBy {it.order}
                require(checkpoints.size==3) {"This story needs exactly three checkpoints"}
                require(AppProviders.gameRepository.getGameDetails(gameId)?.status==GameStatus.PUBLISHED) {"This game is not published"}
                AppProviders.gameRepository.joinGame(gameId).getOrThrow()
               found=app.database.playerProgressDao().getProgress(gameId,uid)?.completedCheckpointIds.orEmpty()
                session=app.database.storyDao().session(gameId,uid) ?: StorySessionEntity(gameId,uid,
                    motionFallback=sensors?.hasTilt!=true, coverFallback=sensors?.hasProximity!=true)
                stage="preflight";draw()
            } catch(e: Exception) {stage="error";draw();status?.text=e.message ?: "Could not load story"}
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while(isActive) {tick();delay(50)}
            }
        }
    }
    override fun onResume() {
        super.onResume();sensors?.start();compass?.start();refreshLocation()
        if(stage=="card" && cameraGranted()) openCamera()
        sensors?.calibrate();lastTick=0
    }
    override fun onPause() {
        closeScanner();sensors?.stop();compass?.stop();app.locationRepository.stopTracking()
        alignHold.reset();coverHold.reset();arrivalHold.reset();arrived=false;coverPressed=false
        super.onPause()
    }
    override fun onDestroyView() {
        closeScanner();body=null;compass=null;maze=null;status=null;live=null;diagnostics=null
        super.onDestroyView()
    }
    private fun refreshLocation() {
        if(!isAdded)return
        val granted=ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        app.locationRepository.updatePermissionState(if(granted)LocationPermissionState.Granted else LocationPermissionState.Denied)
        if(granted && isResumed)app.locationRepository.startTracking(1000,1f)
    }
    private fun index()=checkpoints.indexOfFirst {it.id !in found}
    private fun cp()=checkpoints.getOrNull(index())
    private fun saveSession() {
        val s=session ?: return
        viewLifecycleOwner.lifecycleScope.launch {runCatching {app.database.storyDao().putSession(s)}.onFailure {status?.text="Could not save controls. Try again."}}
    }
    private fun switch(next: String) { closeScanner();stage=next;draw() }
    private fun draw() {
        val b=body ?: return
        closeScanner();compass?.stop();compass=null;maze=null;torchButton=null
        b.removeAllViews();live=null;lightHint=null;diagnostics=null;lamp=null
        StoryUi.button(b,"Back to quest") {findNavController().navigateUp()}
        StoryUi.text(b,"LIGHT THE WAY",28f)
        status=StoryUi.text(b,"")
        if(stage=="loading") {status?.text="Loading your walk…";return}
        if(stage=="error")return
        if(stage=="preflight") {
            StoryUi.text(b,"Can you help light the way?",23f)
            StoryUi.text(b,"The welcome walk starts soon, but the old meeting lamp is not working. Find its key, follow the repair note and bring its light back.")
            StoryUi.text(b, "Tilt controls: ${if(sensors?.hasTilt==true)"available" else "touch alternative"}\nDirection guidance: ${if(sensors?.hasHeading==true)"available" else "map and dial alternative"}\nLamp cover: ${if(session?.coverFallback==true)"slide control" else "hand-cover check"}",14f)
            StoryUi.text(b, "Camera: ${requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)} · Flashlight: ${requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)}",14f)
            StoryUi.text(b,"Cover the top front of your phone, then move your hand away. If the cover sensor does not respond, choose the slide control. You can always use touch controls.")
            live=StoryUi.text(b,"")
            StoryUi.button(b,"Use touch controls for the key") {session=session?.copy(motionFallback=true);saveSession();status?.text="Key puzzle will use touch controls."}
            StoryUi.button(b,"Use slide control for the cover") {session=session?.copy(coverFallback=true);saveSession();status?.text="Lamp cover will use a slide control."}
            StoryUi.button(b,"Allow location") {permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))}
            StoryUi.button(b,"Start / resume the walk") {
                if(sensors?.sawFar!=true || sensors?.sawNear!=true)session=session?.copy(coverFallback=true)
                saveSession();switch(if(index()<0)"ending" else "walk")
            }
            return
        }
        if(stage=="ending") {
            StoryUi.text(b,"The light is back!",26f);StoryUi.text(b,LightTheWay.reveals[2])
            StoryUi.text(b,"Key found · Cover found · Lamp repaired\n3 chapters completed · 300 points")
            StoryUi.button(b,"View my results") {findNavController().navigate(R.id.leaderboardFragment,Bundle().apply {putString("gameId",gameId)})}
            return
        }
        val cp=cp() ?: run {stage="ending";draw();return}
        StoryUi.text(b,cp.name,23f)
        StoryUi.text(b,"Inventory: ${if(found.isEmpty())"No items yet" else if(found.size==1)"Key found" else "Key and glass cover found"}",14f)
        live=StoryUi.text(b,"")
        if(stage=="walk") {
            val c=TrailCompassView(requireContext());c.setBackgroundResource(R.drawable.bg_expedition);compass=c;b.addView(c,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),300)));if(isResumed)c.start()
            StoryUi.text(b,cp.clue)
            StoryUi.text(b,"Follow safe paths. The arrow points directly toward the place.",14f)
            StoryUi.button(b,"Begin this chapter") {
                if(!arrived){status?.text="Reach the marked area and wait for an accurate GPS fix.";return@button}
                when(index()){0 -> switch("key");1 -> switch("card");else -> switch("parts")}
            }
            StoryUi.button(b,"Open map") {findNavController().navigate(R.id.campusMapFragment,Bundle().apply {putString("gameId",gameId)})}
        } else if(stage=="key") {
            StoryUi.text(b,"The key has slipped behind the wooden bars. Stop in a safe place. Tilt left/right and forward/back to move it to the green opening.")
            sensors?.calibrate()
            maze=KeyBoxView(requireContext()).also {m -> m.touchControls=session?.motionFallback==true;m.onSolved={award()};b.addView(m,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),320)))}
            StoryUi.button(b,"Use drag controls") {session=session?.copy(motionFallback=true);maze?.touchControls=true;saveSession();status?.text="Drag anywhere in the box to guide the key."}
            StoryUi.button(b,"Recalibrate / restart puzzle") {maze?.box=KeyBox();sensors?.calibrate();maze?.invalidate()}
        } else if(stage=="card") {
            StoryUi.text(b,"The repair card is inside the shaded pocket. Point the camera at it. Use the light if it is dark, or enter the code below the card.")
            StoryUi.button(b,"Open camera") {if(cameraGranted())openCamera() else permissions.launch(arrayOf(Manifest.permission.CAMERA))}
            lightHint=StoryUi.text(b, "Use the light if the card is hard to see.",14f)
            torchButton=StoryUi.button(b,"Light on") {scanner?.toggleTorch() ?: run {status?.text="Open the camera first."}}
            val code=StoryUi.input(b,"Printed card code")
            StoryUi.button(b,"Check card code") {if(!arrived){status?.text="Return to this checkpoint first."}else if(LightTheWay.matchesCode(code.text.toString(),config!!))award() else status?.text="That code does not match this repair card."}
        } else if(stage=="parts") {
            lamp=LampView(requireContext()).also {b.addView(it,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),180)))}
            StoryUi.text(b,"You have the key and glass cover. Put them back so the meeting lamp can work again.")
            StoryUi.button(b,"Use key and fit the glass cover") {if(arrived)switch(if(session?.aligned==true)"cover" else "aim") else status?.text="Return to the marked area."}
        } else if(stage=="aim") {
            lamp=LampView(requireContext()).also {b.addView(it,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),180)))}
            StoryUi.text(b,"Hold your phone level. Turn slowly until the arrow lines up with the path, then hold still for two seconds.")
            compass=TrailCompassView(requireContext()).also {it.setBackgroundResource(R.drawable.bg_expedition);b.addView(it,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),280)));if(isResumed)it.start()}
            val dial=SeekBar(requireContext()).apply {max=360;progress=180;contentDescription="Turn the lamp until its two marks line up"}
            b.addView(dial)
            var touchAngle=180f
            dial.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {touchAngle=value.toFloat();dial.tag=touchAngle}
                override fun onStartTrackingTouch(s: SeekBar?){}
                override fun onStopTrackingTouch(s: SeekBar?){}
            });dial.tag=touchAngle
            dial.id=View.generateViewId();alignmentDial=dial
            StoryUi.button(b,"Use the lamp dial instead") {useDial=true;alignHold.reset();status?.text="Slide the dial to 0° / 360° so the two marks line up. Hold for two seconds."}
            if(sensors?.hasGyro==true)StoryUi.button(b,"Use relative turning") {
                sensors?.calibrate();useRelative=true;useDial=false;alignHold.reset()
                status?.text="Turn the phone a quarter turn until the marks line up."
            }
            if(sensors?.hasHeading!=true) {
                if(sensors?.hasGyro==true){useRelative=true;sensors?.calibrate()}else useDial=true
            }
        } else if(stage=="cover") {
            lamp=LampView(requireContext()).also {b.addView(it,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),180)))}
            StoryUi.text(b,if(session?.coverFallback==true)"Slide the glass cover down. Hold the latch closed for one second." else "Place your hand over the top front of your phone. Hold it there until the latch clicks.")
            val slide=SeekBar(requireContext()).apply {max=100;contentDescription="Slide lamp cover closed"};b.addView(slide)
            slide.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean){coverSlide=value>=95}
                override fun onStartTrackingTouch(s: SeekBar?){}
                override fun onStopTrackingTouch(s: SeekBar?){}
            })
            StoryUi.button(b,"Use slide and hold control") {session=session?.copy(coverFallback=true);saveSession();status?.text="Slide the cover fully down, then hold the latch button."}
            val latch=StoryUi.button(b,"Hold latch closed") {}
            latch.setOnTouchListener {v,event ->
                coverPressed=event.actionMasked==MotionEvent.ACTION_DOWN || event.actionMasked==MotionEvent.ACTION_MOVE
                if(event.actionMasked==MotionEvent.ACTION_UP)v.performClick()
                true
            }
            coverSlide=false;coverPressed=false
        }
        if(stage!="walk")StoryUi.button(b,"Back to navigation") {switch("walk")}
        val diag=StoryUi.text(b,"");diag.visibility=View.GONE;diagnostics=diag
        StoryUi.button(b,"Show / hide device readings") {diag.visibility=if(diag.visibility==View.VISIBLE)View.GONE else View.VISIBLE}
    }
    private var alignmentDial: SeekBar?=null
    private var useDial=false
    private var coverSlide=false
    private fun tick() {
        if(body==null || config==null)return
        val s=sensors ?: return
        s.rotation=view?.display?.rotation ?: Surface.ROTATION_0
        val now=SystemClock.elapsedRealtime()
        if(stage=="preflight"){live?.text="Cover check: ${if(s.sawFar&&s.sawNear)"ready" else "not verified — slide alternative available"}";return}
        val cp=cp() ?: return
        val loc=app.locationRepository.locationState.value
        val recent=loc.isUpdating && loc.servicesEnabled && loc.hasValidFix && loc.accuracyMeters!=null && loc.accuracyMeters<=20f && System.currentTimeMillis()-loc.timestamp in 0..15000
        val distance=if(recent)HaversineDistanceCalculator().calculateDistanceMeters(loc.smoothedLatitude!!,loc.smoothedLongitude!!,cp.lat,cp.lng)else null
        val wasArrived=arrived
        arrived=arrivalHold.update(distance,loc.accuracyMeters,System.currentTimeMillis()-loc.timestamp,recent,now)
        if(arrived && !wasArrived)status?.text=""
        live?.text=when {
            arrived -> "You reached the marked area. Stop in a safe place."
            distance!=null && distance<=15f -> "Inside the circle. Wait here for a stable GPS position."
            distance!=null -> "About ${distance.toInt()} m away. Follow the arrow to the circle."
            !loc.servicesEnabled -> "Switch on phone location services to continue."
            loc.accuracyMeters!=null && loc.accuracyMeters>20f -> "GPS accuracy is about ${loc.accuracyMeters.toInt()} m. Step into an open area and wait for a better fix."
            else -> "Waiting for a fresh GPS position. Check location permission."
        }
        val target=if(recent)HaversineDistanceCalculator().calculateBearingDegrees(loc.smoothedLatitude!!,loc.smoothedLongitude!!,cp.lat,cp.lng)else null
        compass?.update(if(stage=="aim")config!!.targetBearing else target,distance,loc.smoothedLatitude,loc.smoothedLongitude)
        diagnostics?.text="${s.summary()}\nGPS accuracy: ${loc.accuracyMeters}; arrived: $arrived\nLight: ${s.lux}; gyro turn rate: ${s.turnRate}\nCover: ${s.near}; compass: ${s.heading}\nTilt: ${s.tiltX}, ${s.tiltY}\nControls: drag=${session?.motionFallback}, slide=${session?.coverFallback}"
        val dt=if(lastTick==0L)0f else ((now-lastTick)/1000f).coerceAtMost(.1f);lastTick=now
        maze?.active=arrived && !saving
        if(stage=="key" && session?.motionFallback!=true && now-s.lastMotion<1000) {
            fun dead(v: Float)=if(abs(v)<.08f)0f else v
            maze?.step(dead(s.tiltX)*dt*.5f,dead(s.tiltY)*dt*.5f)
        }
        if(stage=="aim") {
            val declination=if(recent)GeomagneticField(loc.smoothedLatitude!!.toFloat(),loc.smoothedLongitude!!.toFloat(),0f,System.currentTimeMillis()).declination else 0f
            val angle=if(useRelative && !useDial)s.relativeTurn else if(useDial)(alignmentDial?.tag as? Float) ?: 180f else s.heading?.plus(declination)
            val error=angle?.let {abs(TrailGuidance.relativeAngle(if(useDial)0f else if(useRelative)90f else config!!.targetBearing,it))}
            lamp?.beamAngle=error ?: 90f
            val reliable=useDial || if(useRelative)now-s.lastTurn<1000 else (s.headingReliable && now-s.lastHeading<1000)
            val fraction=alignHold.update(arrived && reliable && error!=null && error<=15,now)
            status?.text=if(error==null || !reliable)"Compass unavailable or unreliable. Use the lamp dial." else "${if(useDial)"Dial" else "Lamp"}: ${error.toInt()}° from target · hold ${(fraction*100).toInt()}%"
            if(fraction>=1 && !saving){
                saving=true
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        session=session?.copy(aligned=true)
                        app.database.storyDao().putSession(session!!)
                        alignHold.reset();switch("cover")
                    } catch(e: Exception){status?.text="Could not save alignment. Try again.";alignHold.reset()}
                    finally {saving=false}
                }
            }
        }
        if(stage=="cover") {
            val near=if(session?.coverFallback==true)coverSlide&&coverPressed else s.sawFar && s.near==true
            val fraction=coverHold.update(arrived && near,now)
            lamp?.cover=fraction
            status?.text="Cover closing: ${(fraction*100).toInt()}%"
            if(fraction>=1)award()
        }
        lightHint?.text=when {
            scanner?.torchOn==true -> "The light is on. Keep the card inside the camera view."
            s.lux==null -> "Use Light on if the card is hard to see."
            s.lux!! < 50f -> "It looks dark. Use Light on if the card is hard to see."
            else -> "There may be enough light. Scan the card or switch on the light if needed."
        }
        torchButton?.text=if(scanner?.torchOn==true)"Light off" else "Light on"
    }
    @androidx.camera.core.ExperimentalGetImage
    private fun openCamera() {
        if(stage!="card" || scanner!=null || body==null)return
        if(!arrived){status?.text="Reach checkpoint 2 first.";return}
        val preview=PreviewView(requireContext());cameraPreview=preview;body!!.addView(preview,3,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),260)))
        scanner=CardScanner(this,{value ->
            if(stage=="card" && arrived && !saving) {
                if(LightTheWay.matchesCard(value,config!!))award()
                else if(SystemClock.elapsedRealtime()-cardErrorAt>1500){status?.text="This card does not belong to this checkpoint.";cardErrorAt=SystemClock.elapsedRealtime()}
            }
        },{text ->if(stage=="card")status?.text=text}).also {it.start(preview)}
    }
    private fun award() {
        if(saving || !arrived || cp()==null)return
        saving=true;closeScanner()
        val chapter=index();val checkpoint=cp()!!
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                require(AppProviders.authRepository.getCurrentUserId()==uid) {"Your account changed. Open the game again."}
                AppProviders.gameRepository.recordDiscovery(gameId,checkpoint.id,System.currentTimeMillis()).getOrThrow()
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                found=app.database.playerProgressDao().getProgress(gameId,uid)?.completedCheckpointIds.orEmpty()
                arrived=false;arrivalHold.reset();coverHold.reset()
                val b=body ?: return@launch
                stage="reveal";b.removeAllViews();maze=null;compass?.stop();compass=null;live=null;diagnostics=null
                StoryUi.text(b,if(chapter==2)"The light is back!" else "Chapter complete",27f)
                StoryUi.text(b,LightTheWay.reveals[chapter]);StoryUi.text(b,"Saved · +100 points")
                StoryUi.button(b,if(chapter==2)"Complete the quest" else "Next chapter") {switch(if(chapter==2)"ending" else "walk")}
            } catch(e: Exception){status?.text="Could not save: ${e.message}. Your task can be retried.";if(stage=="key")StoryUi.button(body!!,"Retry saving the key") {award()}}
            finally {saving=false}
        }
    }
}







