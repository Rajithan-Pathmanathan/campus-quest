package com.campusquest

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import java.util.concurrent.TimeUnit
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import com.campusquest.domain.repository.GameRepository
import com.campusquest.domain.repository.LocationRepository
import com.campusquest.domain.repository.SensorRepository
import com.campusquest.ui.common.MainActivity
import com.campusquest.ui.quest.QuestScanFragment
import com.campusquest.ui.quest.QuestScanViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso tests for the checkpoint scan screen (M4 workplan §62). The real [QuestScanFragment] runs on a
 * phone, but its repositories are fakes, so every scan state can be reached without walking outside.
 */
@RunWith(AndroidJUnit4::class)
class QuestScanScreenTest {

    private val games = FakeGames()
    private val sensors = FakeSensors()
    private val location = FakeLocation()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
        val i = InstrumentationRegistry.getInstrumentation()
        listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION").forEach {
            i.uiAutomation.executeShellCommand("pm grant ${i.targetContext.packageName} android.permission.$it").close()
        }
        games.checkpoints = listOf(checkpoint("cp-alpha-001", 1), checkpoint("cp-alpha-002", 2))
        QuestScanFragment.viewModelFactoryOverride = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuestScanViewModel(games, sensors, location, userId = { "u1" }) as T
        }
    }

    @After
    fun tearDown() {
        QuestScanFragment.viewModelFactoryOverride = null
        if (::scenario.isInitialized) scenario.close()
    }

    private fun openScan(checkpointId: String = "cp-alpha-001") {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { a ->
            val frame = FrameLayout(a).apply {
                id = View.generateViewId()
                setBackgroundColor(0xFF111827.toInt())
            }
            (a.findViewById<View>(android.R.id.content) as ViewGroup)
                .addView(frame, ViewGroup.LayoutParams(-1, -1))
            val fragment = QuestScanFragment().apply {
                arguments = Bundle().apply {
                    putString("gameId", "game-alpha")
                    putString("checkpointId", checkpointId)
                }
            }
            a.supportFragmentManager.beginTransaction().add(frame.id, fragment).commitNow()
            // The compass redraws on every rotation-sensor event, so the main thread is never idle and
            // Espresso would wait for it. Stop it; the screen's own texts are what these tests check.
            fragment.view?.findViewById<com.campusquest.ui.quest.TrailCompassView>(R.id.trail_compass)?.stop()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun shown(id: Int) = allOf(withId(id), isDisplayed())

    /** Checks text of a view that may be below the fold in the scrolling screen. */
    private fun checkScrolled(id: Int, text: String) {
        onView(withId(id)).perform(scrollTo()).check(matches(withText(containsString(text))))
    }

    /** The UI updates asynchronously from the ViewModel; retry the assertion for a few seconds. */
    private fun eventually(block: () -> Unit) {
        var last: Throwable? = null
        repeat(40) {
            try { block(); return } catch (e: Throwable) { last = e; Thread.sleep(150) }
        }
        throw AssertionError("Screen said: headline='${textOf(R.id.tv_headline)}' shield='${textOf(R.id.tv_shield_hint)}'", last)
    }

    private fun aligned(near: Boolean) = sensors.emit(
        FakeSensors.fusion("game-alpha", "cp-alpha-001", percent = 90, threshold = true, near = near)
    )

    @Test
    fun showsCheckpointNameAndWaitsForLocation() {
        openScan()
        eventually {
            onView(shown(R.id.tv_checkpoint_name)).check(matches(withText("Ember cp-alpha-001")))
            onView(shown(R.id.tv_headline)).check(matches(withText(containsString("position"))))
        }
        onView(withId(R.id.btn_primary)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun farAwayShowsRoundedDistanceAndNoPercentage() {
        openScan()
        location.at(6.9822948, 79.9163835)
        eventually {
            onView(shown(R.id.tv_headline)).check(matches(withText(containsString("ember"))))
        }
        val text = textOf(R.id.tv_headline) + textOf(R.id.tv_distance)
        assertFalse(text, text.contains('%'))
    }

    @Test
    fun relicAppearsThenScanStartsWhenTheButtonIsTapped() {
        openScan()
        location.atRelic()
        eventually { onView(shown(R.id.btn_primary)).check(matches(withText("Wake the Resonator"))) }
        onView(shown(R.id.btn_primary)).perform(click())
        eventually {
            onView(shown(R.id.tv_headline)).check(matches(withText(containsString("ember"))))
            onView(shown(R.id.btn_primary)).check(doesNotExist())
        }
    }

    @Test
    fun revealStaysHiddenUntilTheProximityGatePasses() {
        openScan()
        location.atRelic()
        eventually { onView(shown(R.id.btn_primary)).check(matches(isDisplayed())) }
        onView(shown(R.id.btn_primary)).perform(click())

        aligned(near = false)
        eventually {
            checkScrolled(R.id.tv_shield_hint, "Cup the ember")
            // No dialog yet: the text must not exist in whichever window has focus.
            onView(withText("Next chapter")).check(doesNotExist())
        }
        assertEquals(0, games.recorded.size)

        aligned(near = true)
        eventually {
            onView(withText("Next chapter")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("lore-cp-alpha-001")).inRoot(isDialog()).check(matches(isDisplayed()))
        }
        assertEquals(1, games.recorded.size)
    }

    @Test
    fun lastCheckpointOffersToCompleteTheQuest() {
        games.completed = listOf("cp-alpha-001")
        openScan("cp-alpha-002")
        location.atRelic()
        eventually { onView(shown(R.id.btn_primary)).check(matches(isDisplayed())) }
        onView(shown(R.id.btn_primary)).perform(click())
        sensors.emit(FakeSensors.fusion("game-alpha", "cp-alpha-002", 90, threshold = true, near = true))
        eventually {
            onView(withText("Complete the quest")).inRoot(isDialog()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun closedGameShowsScanUnavailable() {
        games.status = GameStatus.CLOSED
        openScan()
        eventually {
            onView(shown(R.id.tv_headline)).check(matches(withText("Scan unavailable")))
            onView(shown(R.id.tv_error_message)).check(matches(withText("This game is no longer active.")))
            onView(shown(R.id.btn_primary)).check(matches(withText("Back to the quest")))
        }
    }

    @Test
    fun saveFailureShowsRetryAndRetryRecoversWithoutASecondScan() {
        games.failNextRecord = true
        openScan()
        location.atRelic()
        eventually { onView(shown(R.id.btn_primary)).check(matches(isDisplayed())) }
        onView(shown(R.id.btn_primary)).perform(click())
        aligned(near = true)

        eventually {
            onView(shown(R.id.btn_primary)).check(matches(withText("Try again")))
            onView(shown(R.id.tv_error_message)).check(matches(isDisplayed()))
        }
        onView(shown(R.id.btn_primary)).perform(click())
        eventually {
            onView(withText("Next chapter")).inRoot(isDialog()).check(matches(isDisplayed()))
        }
        assertEquals(2, games.recorded.size)
        assertEquals(1, sensors.started)
    }

    private fun textOf(id: Int): String {
        var out = ""
        scenario.onActivity { a -> out = a.findViewById<android.widget.TextView>(id)?.text?.toString().orEmpty() }
        return out
    }

    // ---- fakes (androidTest cannot see the unit-test fakes) ----

    private fun checkpoint(id: String, order: Int) = Checkpoint(
        id = id, gameId = "game-alpha", name = "Ember $id", lat = 6.9722948, lng = 79.9163835,
        radiusM = 15f, lightSignature = LightSignature(0f, 1000f), clue = "clue-$id", lore = "lore-$id",
        order = order, rarity = "RARE"
    )

    private class FakeGames : GameRepository {
        var checkpoints: List<Checkpoint> = emptyList()
        var completed: List<String> = emptyList()
        var status = GameStatus.PUBLISHED
        var failNextRecord = false
        val recorded = mutableListOf<String>()

        override suspend fun getGameCheckpoints(gameId: String) = checkpoints
        override suspend fun getGameDetails(gameId: String): Game? =
            Game(id = gameId, title = "Quest", description = "", creatorId = "c", creatorName = "C", status = status)

        override suspend fun recordDiscovery(gameId: String, checkpointId: String, foundAt: Long): Result<Unit> {
            recorded += checkpointId
            if (failNextRecord) { failNextRecord = false; return Result.failure(RuntimeException("disk")) }
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

    private class FakeSensors : SensorRepository {
        private val _sensor = MutableStateFlow(SensorState())
        private val _fusion = MutableStateFlow(fusion("", ""))
        override val sensorState: StateFlow<SensorState> = _sensor
        override val fusionResult: StateFlow<FusionResult> = _fusion
        var started = 0
        override fun startScanSession(checkpoint: Checkpoint) {
            started++
            _fusion.value = fusion(checkpoint.gameId, checkpoint.id)
        }
        override fun stopScanSession() {}
        override fun resetScanSession() {}
        fun emit(r: FusionResult) { _fusion.value = r }

        companion object {
            fun fusion(
                gameId: String, checkpointId: String, percent: Int = 0,
                threshold: Boolean = false, near: Boolean = false
            ) = FusionResult(
                gameId = gameId, checkpointId = checkpointId, gpsScore = 1f, lightScore = 1f, motionScore = 1f,
                totalScore = percent / 100f, progressPercent = percent, thresholdReached = threshold,
                proximityNear = near, activeSignals = emptySet()
            )
        }
    }

    private class FakeLocation : LocationRepository {
        private val _location = MutableStateFlow(LocationState())
        override val locationState: StateFlow<LocationState> = _location
        override fun startTracking(intervalMs: Long, minDistanceM: Float) {}
        override fun stopTracking() {}
        override fun updatePermissionState(state: LocationPermissionState) {}
        fun at(lat: Double, lng: Double, accuracy: Float = 5f) {
            _location.value = LocationState(
                permissionState = LocationPermissionState.Granted, servicesEnabled = true,
                smoothedLatitude = lat, smoothedLongitude = lng, accuracyMeters = accuracy
            )
        }
        fun atRelic() = at(6.9722948, 79.9163835)
    }
}
