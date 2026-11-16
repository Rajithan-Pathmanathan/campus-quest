package com.campusquest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameLeaderboardEntry
import com.campusquest.domain.model.GameStatus
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.LocationPermissionState
import com.campusquest.domain.model.LocationState
import com.campusquest.domain.model.PlayerProgress
import com.campusquest.domain.model.SensorState
import com.campusquest.domain.model.TrialStatus
import com.campusquest.domain.repository.GameRepository
import com.campusquest.domain.repository.LocationRepository
import com.campusquest.domain.repository.SensorRepository
import com.campusquest.ui.quest.QuestScanFragment
import com.campusquest.ui.quest.QuestScanViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * DEBUG BUILDS ONLY. Shows the real scan screen with fake data so every state can be reviewed on a phone
 * without walking to a checkpoint. Tap a chip along the bottom to jump to a state.
 */
class ScanPreviewActivity : AppCompatActivity() {

    private val games = PreviewGames()
    private val sensors = PreviewSensors()
    private val location = PreviewLocation()
    private lateinit var container: FrameLayout

    private val lat0 = 6.9722948
    private val lng0 = 79.9163835

    private fun metresNorth(m: Double) = location.at(lat0 + m / 111_320.0, lng0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
        QuestScanFragment.viewModelFactoryOverride = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuestScanViewModel(games, sensors, location, userId = { "preview" }) as T
        }

        val root = FrameLayout(this)
        container = FrameLayout(this).apply { id = View.generateViewId() }
        root.addView(container, FrameLayout.LayoutParams(-1, -1))
        root.addView(chipRow(), FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { topMargin = 0 })
        setContentView(root)

        scenario("Start") { }
    }

    override fun onDestroy() {
        QuestScanFragment.viewModelFactoryOverride = null
        super.onDestroy()
    }

    private fun chipRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 8, 12, 8)
        }
        fun chip(label: String, action: () -> Unit) {
            row.addView(TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 12f
                setPadding(26, 14, 26, 14)
                setBackgroundColor(0x99000000.toInt())
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = 10 })
        }
        // each chip re-opens the screen so the story starts clean, then moves the fake world
        chip("Start") { scenario("Start") { } }
        chip("Far") { scenario("Far") { metresNorth(900.0) } }
        chip("Cold") { scenario("Cold") { metresNorth(250.0) } }
        chip("Warm") { scenario("Warm") { metresNorth(90.0) } }
        chip("Hot") { scenario("Hot") { metresNorth(25.0) } }
        chip("Relic") { scenario("Relic") { metresNorth(2.0) } }
        chip("Scan low") { scenario("Scan low") { metresNorth(2.0); begin(); fusion(30) } }
        chip("Scan mid") { scenario("Scan mid") { metresNorth(2.0); begin(); fusion(62) } }
        chip("Scan hot") { scenario("Scan hot") { metresNorth(2.0); begin(); fusion(90) } }
        chip("Needs shield") { scenario("Needs shield") { metresNorth(2.0); begin(); fusion(95, threshold = true) } }
        chip("Reveal") { scenario("Reveal") { metresNorth(2.0); begin(); fusion(95, threshold = true, near = true) } }
        chip("Reveal last") { games.completed = listOf("cp-1"); scenario("Reveal last", "cp-2") { metresNorth(2.0); begin(); fusion(95, true, true) } }
        chip("Closed") { games.status = GameStatus.CLOSED; scenario("Closed") { } }
        chip("Save error") { games.failRecord = true; scenario("Save error") { metresNorth(2.0); begin(); fusion(95, true, true) } }
        return HorizontalScrollView(this).apply { addView(row); isHorizontalScrollBarEnabled = false }
    }

    private fun begin() {
        // wait for the screen to show the button, then tap it for the user
        container.postDelayed({
            container.findViewById<View>(R.id.btn_primary)?.performClick()
        }, 700)
    }

    private fun fusion(percent: Int, threshold: Boolean = false, near: Boolean = false) {
        container.postDelayed({
            sensors.emit(
                FusionResult(
                    gameId = "game-preview", checkpointId = games.currentCheckpointId, gpsScore = 1f,
                    lightScore = 1f, motionScore = 1f, totalScore = percent / 100f, progressPercent = percent,
                    thresholdReached = threshold, proximityNear = near, activeSignals = emptySet()
                )
            )
        }, 1100)
    }

    private fun scenario(name: String, checkpoint: String = "cp-1", setup: () -> Unit) {
        title = "Scan Preview: $name"
        games.currentCheckpointId = checkpoint
        location.reset()
        sensors.reset()
        supportFragmentManager.beginTransaction()
            .replace(container.id, QuestScanFragment().apply {
                arguments = Bundle().apply {
                    putString("gameId", "game-preview")
                    putString("checkpointId", checkpoint)
                }
            })
            .commitNow()
        container.postDelayed(setup, 500)
        // the screen has read the fake game by now; go back to a normal published game for the next chip
        container.postDelayed({ games.status = GameStatus.PUBLISHED; games.completed = emptyList() }, 1500)
    }

    // ---- fakes ----

    private class PreviewGames : GameRepository {
        var status = GameStatus.PUBLISHED
        var completed: List<String> = emptyList()
        var failRecord = false
        var currentCheckpointId = "cp-1"

        private fun cp(id: String, order: Int, name: String, light: LightSignature, motion: String) = Checkpoint(
            id = id, gameId = "game-preview", name = name, lat = 6.9722948, lng = 79.9163835, radiusM = 15f,
            lightSignature = light, motionType = motion, order = order, rarity = "Rare",
            clue = "Under the old clock, where the stone remembers the sun.",
            lore = "The founders carried this flame across the quad on the night the first lecture hall opened."
        )

        private val points = listOf(
            cp("cp-1", 1, "The Ember in the Light", LightSignature(10_000f, 120_000f), "SHAKE"),
            cp("cp-2", 2, "The Ember in the Shade", LightSignature(500f, 10_000f), "TILT")
        )

        override suspend fun getGameCheckpoints(gameId: String) = points
        override suspend fun getGameDetails(gameId: String): Game? =
            Game(id = gameId, title = "The Founder's Lantern", description = "", creatorId = "c", creatorName = "C", status = status)

        override suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit> {
            if (failRecord) { failRecord = false; return Result.failure(RuntimeException("preview")) }
            return Result.success(Unit)
        }

        override fun observePlayerProgress(gameId: String, userId: String): Flow<PlayerProgress?> =
            flowOf(PlayerProgress(gameId, userId, completedCheckpointIds = completed))

        override suspend fun getAllPlayerProgress(userId: String) = emptyList<PlayerProgress>()
        override suspend fun getAvailableGames() = emptyList<Game>()
        override suspend fun getCreatorGames(creatorId: String) = emptyList<Game>()
        override suspend fun createGame(game: Game) = Result.success(game)
        override suspend fun updateGame(game: Game) = Result.success(Unit)
        override suspend fun createCheckpoint(gameId: String, checkpoint: Checkpoint) = Result.success(checkpoint)
        override suspend fun updateCheckpoint(gameId: String, checkpoint: Checkpoint) = Result.success(Unit)
        override suspend fun deleteCheckpoint(gameId: String, checkpointId: String) = Result.success(Unit)
        override suspend fun deleteGame(gameId: String) = Result.success(Unit)
        override suspend fun publishGame(gameId: String) = Result.success(Unit)
        override suspend fun joinGame(gameId: String) = Result.success(Unit)
        override suspend fun getFusionSignature(gameId: String, checkpointId: String) = LightSignature(0f, 1000f)
        override fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>> = flowOf(emptyList())
        override suspend fun syncPending() {}
    }

    private class PreviewSensors : SensorRepository {
        private val _sensor = MutableStateFlow(SensorState(trialStatus = TrialStatus(remaining = 3)))
        private val _fusion = MutableStateFlow(blank())
        override val sensorState: StateFlow<SensorState> = _sensor
        override val fusionResult: StateFlow<FusionResult> = _fusion
        private var current: Checkpoint? = null

        private fun blank(cp: Checkpoint? = null) = FusionResult(
            gameId = cp?.gameId.orEmpty(), checkpointId = cp?.id.orEmpty(), gpsScore = 0f, lightScore = 0f,
            motionScore = 0f, totalScore = 0f, progressPercent = 0, thresholdReached = false,
            proximityNear = false, activeSignals = emptySet()
        )

        override fun startScanSession(checkpoint: Checkpoint) { current = checkpoint; _fusion.value = blank(checkpoint) }
        override fun stopScanSession() {}
        override fun resetScanSession() {}
        fun emit(r: FusionResult) { _fusion.value = r }
        fun reset() { _fusion.value = blank(); current = null }
    }

    private class PreviewLocation : LocationRepository {
        private val _location = MutableStateFlow(LocationState())
        override val locationState: StateFlow<LocationState> = _location
        override fun startTracking(intervalMs: Long, minDistanceM: Float) {}
        override fun stopTracking() {}
        override fun updatePermissionState(state: LocationPermissionState) {}
        fun at(lat: Double, lng: Double) {
            _location.value = LocationState(
                permissionState = LocationPermissionState.Granted, servicesEnabled = true,
                smoothedLatitude = lat, smoothedLongitude = lng, accuracyMeters = 5f
            )
        }
        fun reset() { _location.value = LocationState() }
    }
}
