package com.campusquest

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.campusquest.data.local.AppDatabase
import com.campusquest.data.remote.FirestoreService
import com.campusquest.data.remote.FirestoreServiceImpl
import com.campusquest.data.repository.AuthRepositoryImpl
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.data.sync.SyncScheduler
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.domain.repository.GameRepository

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

    val firestoreService: FirestoreService by lazy {
        FirestoreServiceImpl()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl()
    }

    val gameRepository: GameRepository by lazy {
        GameRepositoryImpl(
            gameDao = database.gameDao(),
            checkpointDao = database.checkpointDao(),
            playerProgressDao = database.playerProgressDao(),
            syncQueueDao = database.syncQueueDao(),
            firestoreService = firestoreService,
            authRepository = authRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initFirebaseSafely()
        createNotificationChannels()
        scheduleSync()
    }

    private fun initFirebaseSafely() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId(packageName)
                    .setApiKey("AIzaSyFakeKeyCampusQuestOfflineFallback")
                    .setProjectId("campus-quest-offline")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            android.util.Log.w("CampusQuestApp", "Firebase auto-init skipped or running in mock mode: ${e.message}")
        }
    }

    private fun scheduleSync() {
        try {
            SyncScheduler.schedulePeriodicSync(this)
        } catch (e: Exception) {
            // Handled during test environments or headless runners
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
