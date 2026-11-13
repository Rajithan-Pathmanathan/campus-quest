package com.campusquest

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.campusquest.data.local.AppDatabase
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.data.repository.LocationRepositoryImpl
import com.campusquest.data.repository.SensorRepositoryImpl
import com.campusquest.device.location.LocationManagerHelper
import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.domain.repository.GameRepository
import com.campusquest.domain.repository.LocationRepository
import com.campusquest.domain.repository.SensorRepository

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

    val gameRepository: GameRepository by lazy {
        GameRepositoryImpl(
            gameDao = database.gameDao(),
            checkpointDao = database.checkpointDao(),
            playerProgressDao = database.playerProgressDao(),
            syncQueueDao = database.syncQueueDao()
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
        createNotificationChannels()
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
