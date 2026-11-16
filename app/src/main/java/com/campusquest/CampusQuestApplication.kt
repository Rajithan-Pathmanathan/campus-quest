package com.campusquest

import androidx.room.withTransaction
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.campusquest.data.local.AppDatabase
import com.campusquest.data.repository.AuthRepositoryImpl
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.data.repository.LocationRepositoryImpl
import com.campusquest.data.repository.SensorRepositoryImpl
import com.campusquest.device.location.LocationManagerHelper
import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository
import com.campusquest.domain.repository.LocationRepository
import com.campusquest.domain.repository.SensorRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.campusquest.data.remote.FirestoreService
import com.campusquest.data.remote.FirestoreServiceImpl
import com.campusquest.data.remote.PushNotifications
import com.campusquest.data.repository.SyncWorker
import kotlinx.coroutines.launch
import com.campusquest.device.location.geofence.GeofenceManagerHelper
import com.campusquest.device.location.geofence.QuestGeofenceCoordinator
import com.campusquest.device.location.geofence.GeofenceAlerts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CampusQuestApplication : Application() {

    companion object {
        const val CHANNEL_QUEST_ALERTS = "campus_quest_alerts"
        const val CHANNEL_GEOFENCE_EVENTS = "campus_quest_geofences"

        lateinit var instance: CampusQuestApplication
            private set
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    /** App-wide scope for fire-and-forget work (profile/token sync, geofences). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Cloud backend; null only if Firebase could not start (missing google-services.json). */
    val firestore: FirestoreService? by lazy {
        runCatching { FirestoreServiceImpl(FirebaseFirestore.getInstance()) }.getOrNull()
    }

    val pushNotifications: PushNotifications? by lazy {
        val cloud = firestore ?: return@lazy null
        runCatching {
            PushNotifications(FirebaseMessaging.getInstance(), FirebaseAuth.getInstance(), cloud, BuildConfig.NEW_QUEST_NOTIFY_URL)
        }.getOrNull()
    }

    val authRepository: AuthRepository? by lazy {
        try {
            AuthRepositoryImpl(
                auth = FirebaseAuth.getInstance(),
                onSignedIn = { user ->
                    // App scope: the login screen (and its scope) closes right after sign-in.
                    appScope.launch {
                        firestore?.let { cloud -> runCatching { cloud.saveUserProfile(user) } }
                        pushNotifications?.onSignedIn(user.uid)
                    }
                    SyncWorker.enqueue(this)
                },
                deleteUserData = { uid ->
                    firestore?.deleteUserData(uid)
                    database.playerProgressDao().deleteUserProgress(uid)
                }
            )
        } catch (_: Exception) {
            null
        }
    }

    val gameRepository: GameRepository by lazy {
        GameRepositoryImpl(
            gameDao = database.gameDao(),
            checkpointDao = database.checkpointDao(),
            playerProgressDao = database.playerProgressDao(),
            syncQueueDao = database.syncQueueDao(),
            authRepository = authRepository,
            inTransaction = { block -> database.withTransaction { block() } },
            remote = firestore,
            storyDao = database.storyDao(),
            userName = { authRepository?.currentUserState?.value?.displayName?.takeIf { it.isNotBlank() } ?: GameRepositoryImpl.DEFAULT_PLAYER_NAME },
            onQueued = { SyncWorker.enqueue(this) },
            onPublished = { gameId -> pushNotifications?.announceNewQuest(gameId) }
        )
    }

    // Member 3: real geofence around the player's next checkpoint (see QuestGeofenceCoordinator).
    val questGeofences: QuestGeofenceCoordinator by lazy {
        QuestGeofenceCoordinator(
            geofences = GeofenceManagerHelper(this),
            repository = gameRepository,
            userId = { authRepository?.getCurrentUserId()?.takeIf { it.isNotBlank() } ?: GameRepositoryImpl.DEFAULT_LOCAL_USER_ID },
            scope = appScope,
            locations = locationRepository.locationState,
            onFallbackEnter = { gameId, checkpointId -> appScope.launch { runCatching { GeofenceAlerts.notifyIfUnfound(this@CampusQuestApplication, gameId, checkpointId) } } }
        )
    }

    // Member 3: single app-wide instances. UI must use these, never SensorManager/LocationManager directly.
    val locationRepository: LocationRepository by lazy {
        LocationRepositoryImpl(LocationManagerHelper(this))
    }

    val sensorRepository: SensorRepository by lazy {
        SensorRepositoryImpl(
            fusionEngine = SensorFusionEngine(this),
            locationRepository = locationRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppCheckSetup.install()
        createNotificationChannels()
        // Upload anything left from a previous session, and keep retrying in the background.
        SyncWorker.enqueue(this)
        SyncWorker.schedulePeriodic(this)
        // Returning players: refresh profile + FCM token once per launch.
        authRepository?.currentUserState?.value?.let { user ->
            appScope.launch {
                firestore?.let { runCatching { it.saveUserProfile(user) } }
                pushNotifications?.onSignedIn(user.uid)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val questAlertsChannel = NotificationChannel(
                CHANNEL_QUEST_ALERTS,
                "New Campus Quests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for newly published campus quests and challenges"
            }

            val geofenceChannel = NotificationChannel(
                CHANNEL_GEOFENCE_EVENTS,
                "Checkpoint Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when approaching active quest checkpoints"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(questAlertsChannel)
            manager.createNotificationChannel(geofenceChannel)
        }
    }
}
