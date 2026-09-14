package com.campusquest.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private fun getSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    fun schedulePeriodicSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<DiscoverySyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(getSyncConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DiscoverySyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun triggerImmediateSync(context: Context) {
        val immediateRequest = OneTimeWorkRequestBuilder<DiscoverySyncWorker>()
            .setConstraints(getSyncConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DiscoverySyncWorker.WORK_NAME_IMMEDIATE,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }
}
