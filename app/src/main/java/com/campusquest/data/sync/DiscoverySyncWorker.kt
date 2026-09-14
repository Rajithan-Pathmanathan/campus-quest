package com.campusquest.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campusquest.CampusQuestApplication

class DiscoverySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "campus_quest_periodic_sync"
        const val WORK_NAME_IMMEDIATE = "campus_quest_immediate_sync"
        private const val TAG = "DiscoverySyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting background sync of pending mutations...")
            val app = applicationContext as? CampusQuestApplication ?: CampusQuestApplication.instance
            app.gameRepository.syncPending()
            Log.d(TAG, "Background sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background sync encountered error, retrying...", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
