package com.campusquest.story

import android.Manifest
import android.content.pm.PackageManager
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
import kotlinx.coroutines.*
import kotlin.math.abs

/** Story screen owns hardware only while resumed. Awards go through the shared transactional repository. */
class StoryGameFragment : Fragment() {
    private val app get() = CampusQuestApplication.instance
    private val gameId get() = arguments?.getString("gameId").orEmpty()
    private var uid=""
    private var config: StoryConfigEntity?=null
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
    private var keyStarted=false
    private var tiltStatus: TextView?=null
    private var oilView: LampOilView?=null
    private var oilGame: LampOilGame?=null
    private var oilStarted=false
    private var lastOilStatus=OilPourStatus.READY
    private var scanner: CardScanner?=null
    private var cameraPreview: PreviewView?=null
    private var torchButton: Button?=null
    private var stage="loading"
    private var arrived=false
    private var saving=false
    private var lastTick=0L
    private var cardErrorAt=0L
    private val arrivalHold=StoryArrival()
    private val locationAccess = LocationAccess(this, { refreshLocation() }, { status?.text=it })
    private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshLocation()
        if(stage=="card" && cameraGranted()) openCamera()
        else if(stage=="card") com.campusquest.ui.common.PermissionNotice.show(this, com.campusquest.ui.common.AppPermission.CAMERA)
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
        keyStarted=false;oilStarted=false
        arrivalHold.reset();arrived=false
        super.onPause()
    }
    override fun onDestroyView() {
        closeScanner();body=null;compass=null;maze=null;oilView=null;status=null;live=null;diagnostics=null
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
    private fun switch(next: String) { closeScanner();stage=next;draw() }
    private fun draw() {
        val b=body ?: return
        closeScanner();compass?.stop();compass=null;maze=null;torchButton=null
        b.removeAllViews();live=null;lightHint=null;diagnostics=null;oilView=null;tiltStatus=null
        StoryUi.button(b,"Back to quest") {findNavController().navigateUp()}
        StoryUi.text(b,"LIGHT THE WAY",28f)
        status=StoryUi.text(b,"")
        if(stage=="loading") {status?.text="Loading your walk…";return}
        if(stage=="error")return
        if(stage=="preflight") {
            StoryUi.text(b,"Can you help light the way?",23f)
            StoryUi.text(b,"The welcome walk starts soon, but the old meeting lamp is not working. Find its key, follow the repair note and bring its light back.")
            StoryUi.text(b, "Tilt controls: ${if(sensors?.hasTilt==true)"accelerometer available" else "unavailable — required for this game"}\nSmooth movement check: ${if(sensors?.hasGyro==true)"gyroscope available" else "accelerometer-only mode"}",14f)
            StoryUi.text(b, "Camera: ${requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)} · Flashlight: ${requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)}",14f)
            StoryUi.text(b,"You will tilt the phone to free the key and later pour oil into the lamp. Move slowly so the oil does not spill.")
            live=StoryUi.text(b,"")
            StoryUi.button(b,"Allow location") {locationAccess.request()}
            StoryUi.button(b,"Start / resume the walk") {
                if(sensors?.hasTilt!=true){status?.text="This game needs an accelerometer for tilt controls. This phone cannot play the key puzzle.";return@button}
                if(sensors?.tilt?.fresh(SystemClock.elapsedRealtime())!=true){status?.text="Waiting for the tilt sensor. Hold your phone still briefly and try again.";return@button}
                switch(if(index()<0)"ending" else "walk")
            }
            return
        }
        if(stage=="ending") {
            StoryUi.text(b,"The light is back!",26f);StoryUi.text(b,LightTheWay.reveals[2])
            StoryUi.text(b,"Key found · Repair card scanned · Lamp filled\n3 chapters completed · 300 points")
            StoryUi.button(b,"View my results") {findNavController().navigate(R.id.leaderboardFragment,Bundle().apply {putString("gameId",gameId)})}
            return
        }
        if(cp()==null){stage="ending";draw();return}
        val chapter=index()
        StoryUi.button(b,"Allow location") {locationAccess.request()}
        StoryUi.text(b,"Chapter ${chapter+1} - ${LightTheWay.names[chapter]}",23f)
        StoryUi.text(b,"Inventory: ${if(found.isEmpty())"No items yet" else if(found.size==1)"Key found" else "Key and lamp oil found"}",14f)
        live=StoryUi.text(b,"")
        if(stage=="walk") {
            val c=TrailCompassView(requireContext());c.setBackgroundResource(R.drawable.bg_expedition);compass=c;b.addView(c,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),300)));if(isResumed)c.start()
            StoryUi.text(b,LightTheWay.clues[chapter])
            StoryUi.text(b,"Follow safe paths. The arrow points directly toward the place.",14f)
            StoryUi.button(b,"Begin this chapter") {
                if(!arrived){status?.text="Reach the marked area and wait for an accurate GPS fix.";return@button}
                when(index()){0 -> switch("key");1 -> switch("card");else -> {
                    oilGame=LampOilGame();oilStarted=false;lastOilStatus=OilPourStatus.READY;switch("oil")
                }}
            }
            StoryUi.button(b,"Open map") {findNavController().navigate(R.id.campusMapFragment,Bundle().apply {putString("gameId",gameId)})}
        } else if(stage=="key") {
            StoryUi.text(b,"The key is trapped behind eight barriers. Stop in a safe place. Tilt gently through the alternating gaps to the gold opening at the top right.")
            keyStarted=false
            tiltStatus=StoryUi.text(b,"Hold your phone comfortably, then tap Start tilting.",16f)
            maze=KeyBoxView(requireContext()).also {m -> m.onSolved={award()};b.addView(m,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),320)))}
            StoryUi.button(b,"Start tilting / recalibrate") {
                sensors?.calibrate();keyStarted=true
                tiltStatus?.text="Keep still briefly while tilt is calibrated…"
            }
            StoryUi.button(b,"Restart key puzzle") {maze?.box=KeyBox();keyStarted=false;maze?.invalidate();tiltStatus?.text="Tap Start tilting when you are ready."}
        } else if(stage=="card") {
            StoryUi.text(b,"The repair card is inside the shaded pocket. Point the camera at it. Use the light if it is dark, or enter the code below the card.")
            StoryUi.button(b,"Open camera") {if(cameraGranted())openCamera() else permissions.launch(arrayOf(Manifest.permission.CAMERA))}
            lightHint=StoryUi.text(b, "Use the light if the card is hard to see.",14f)
            torchButton=StoryUi.button(b,"Light on") {scanner?.toggleTorch() ?: run {status?.text="Open the camera first."}}
            val code=StoryUi.input(b,"Printed card code")
            StoryUi.button(b,"Check card code") {if(!arrived){status?.text="Return to this checkpoint first."}else if(LightTheWay.matchesCode(code.text.toString(),config!!))award() else status?.text="That code does not match this repair card."}
        } else if(stage=="oil") {
            StoryUi.text(b,"The repair card says the lamp is empty. Tilt your phone slowly to pour the oil into the lamp. Keep the stream inside the opening.")
            oilView=LampOilView(requireContext()).also {b.addView(it,LinearLayout.LayoutParams(-1,StoryUi.pixels(requireContext(),260)))}
            tiltStatus=StoryUi.text(b,"Hold your phone comfortably, then tap Start pouring.",16f)
            StoryUi.button(b,"Start pouring / recalibrate") {
                sensors?.calibrate();oilStarted=true
                tiltStatus?.text="Hold still briefly while tilt is calibrated."
            }
            StoryUi.button(b,"Restart pouring") {
                oilGame=LampOilGame();oilStarted=false;lastOilStatus=OilPourStatus.READY
                oilView?.fill=0f;oilView?.tilt=0f;oilView?.status=OilPourStatus.READY
                tiltStatus?.text="Hold your phone comfortably, then tap Start pouring."
            }
        }
        if(stage!="walk")StoryUi.button(b,"Back to navigation") {switch("walk")}
        val diag=StoryUi.text(b,"");diag.visibility=View.GONE;diagnostics=diag
        StoryUi.button(b,"Show / hide device readings") {diag.visibility=if(diag.visibility==View.VISIBLE)View.GONE else View.VISIBLE}
    }
    private fun tick() {
        if(body==null || config==null)return
        val s=sensors ?: return
        s.rotation=view?.display?.rotation ?: Surface.ROTATION_0
        val now=SystemClock.elapsedRealtime()
        if(stage=="preflight"){live?.text="Tilt sensor: ${if(s.tilt.fresh(now))"ready" else "hold the phone still for a moment"}";return}
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
        compass?.update(target,distance,loc.smoothedLatitude,loc.smoothedLongitude)
        diagnostics?.text="${s.summary()}\nGPS accuracy: ${loc.accuracyMeters}; arrived: $arrived\nLight: ${s.lux}; gyro turn rate: ${s.turnRate}\nTilt: ${s.tiltX}, ${s.tiltY}\nControls: tilt only"
        val dt=if(lastTick==0L)0f else ((now-lastTick)/1000f).coerceAtMost(.1f);lastTick=now
        maze?.active=arrived && !saving && keyStarted && s.tilt.fresh(now)
        if(stage=="key") {
            tiltStatus?.text=when {
                !s.hasTilt -> "Tilt sensor unavailable. This puzzle requires an accelerometer."
                !arrived -> "Puzzle paused: return to the circle and wait for GPS."
                !keyStarted -> "Hold your phone comfortably and tap Start tilting."
                !s.tilt.fresh(now) -> "Waiting for tilt readings. Hold still briefly, or tap recalibrate."
                abs(s.tiltX)+abs(s.tiltY)<.04f -> "Tilt ready. Gently lower the side you want the key to move toward."
                else -> "Tilt is moving the key. Keep steering around the bars."
            }
            if(maze?.active==true)maze?.step(s.tiltX*dt*.65f,s.tiltY*dt*.65f)
        }
        if(stage=="oil") {
            val game=oilGame ?: LampOilGame().also {oilGame=it}
            val tilt=maxOf(abs(s.tiltX),abs(s.tiltY))
            val turnRate=if(s.hasGyro && now-s.lastTurn<1000)s.turnRate else null
            game.update(tilt,turnRate,dt,arrived && oilStarted && s.tilt.fresh(now))
            oilView?.fill=game.fill;oilView?.tilt=tilt;oilView?.status=game.status
            val justSpilled=game.status != lastOilStatus &&
                (game.status==OilPourStatus.TOO_FAST || game.status==OilPourStatus.TOO_FAR)
            if(justSpilled) {
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            lastOilStatus=game.status
            tiltStatus?.text=when {
                !arrived -> "Pouring paused. Return to the checkpoint circle."
                !oilStarted -> "Hold your phone comfortably, then tap Start pouring."
                !s.tilt.fresh(now) -> "Waiting for tilt readings. Hold still or tap recalibrate."
                game.status==OilPourStatus.READY -> "Tilt slowly until the oil starts flowing. Lamp ${Math.round(game.fill*100)}% full."
                game.status==OilPourStatus.POURING -> "Good. Keep this angle and move slowly. Lamp ${Math.round(game.fill*100)}% full."
                game.status==OilPourStatus.TOO_FAST -> "Too fast. Some oil spilled. Slow down."
                game.status==OilPourStatus.TOO_FAR -> "The bottle is tilted too far. Lift it slightly."
                game.status==OilPourStatus.RETURN_UPRIGHT -> "The lamp is full. Return the phone upright."
                game.status==OilPourStatus.SEALING -> "Hold still while the lamp is sealed: ${Math.round(game.seal*100)}%."
                game.status==OilPourStatus.COMPLETE -> "The lamp is ready."
                else -> "Pouring paused."
            }
            if(game.status==OilPourStatus.COMPLETE)award()
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
                arrived=false;arrivalHold.reset()
                val b=body ?: return@launch
                stage="reveal";b.removeAllViews();maze=null;oilView=null;compass?.stop();compass=null;live=null;diagnostics=null
                StoryUi.text(b,if(chapter==2)"The light is back!" else "Chapter complete",27f)
                StoryUi.text(b,LightTheWay.reveals[chapter]);StoryUi.text(b,"Saved · +100 points")
                StoryUi.button(b,if(chapter==2)"Complete the quest" else "Next chapter") {switch(if(chapter==2)"ending" else "walk")}
            } catch(e: Exception){status?.text="Could not save: ${e.message}. Your task can be retried.";if(stage=="key")StoryUi.button(body!!,"Retry saving the key") {award()}}
            finally {saving=false}
        }
    }
}
