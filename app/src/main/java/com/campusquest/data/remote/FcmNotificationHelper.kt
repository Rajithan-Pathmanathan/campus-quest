package com.campusquest.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FcmNotificationHelper(
    private val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {

    companion object {
        private const val TAG = "FcmNotificationHelper"
        const val TOPIC_NEW_GAMES = "new_games"
    }

    suspend fun subscribeToNewGames(): Result<Unit> {
        return try {
            firebaseMessaging.subscribeToTopic(TOPIC_NEW_GAMES).await()
            Log.d(TAG, "Subscribed successfully to topic: $TOPIC_NEW_GAMES")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to topic: $TOPIC_NEW_GAMES", e)
            Result.failure(e)
        }
    }

    suspend fun unsubscribeFromNewGames(): Result<Unit> {
        return try {
            firebaseMessaging.unsubscribeFromTopic(TOPIC_NEW_GAMES).await()
            Log.d(TAG, "Unsubscribed successfully from topic: $TOPIC_NEW_GAMES")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from topic: $TOPIC_NEW_GAMES", e)
            Result.failure(e)
        }
    }

    suspend fun getDeviceToken(): Result<String> {
        return try {
            val token = firebaseMessaging.token.await()
            Log.d(TAG, "Retrieved FCM Token: $token")
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve FCM token", e)
            Result.failure(e)
        }
    }
}
