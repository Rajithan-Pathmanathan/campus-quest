package com.campusquest.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Firebase Cloud Messaging wiring.
 * - Every signed-in phone subscribes to [TOPIC_NEW_QUESTS] and stores its token on /users/{uid}.
 * - After a quest is published, [announceNewQuest] asks the notification endpoint to push
 *   "A new quest has been hidden on campus!" to that topic. An Android app cannot send to a
 *   topic itself, so this needs a small server: see server/notify-new-quest (Vercel).
 *   The endpoint URL comes from local.properties `NEW_QUEST_NOTIFY_URL`; blank = disabled.
 */
class PushNotifications(
    private val messaging: FirebaseMessaging,
    private val auth: FirebaseAuth,
    private val remote: FirestoreService,
    private val notifyUrl: String
) {
    suspend fun onSignedIn(uid: String) {
        try {
            messaging.subscribeToTopic(TOPIC_NEW_QUESTS).await()
            remote.saveFcmToken(uid, messaging.token.await())
        } catch (e: Exception) {
            Log.w(TAG, "FCM setup failed: ${e.message}")
        }
    }

    suspend fun onNewToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching { remote.saveFcmToken(uid, token) }
    }

    suspend fun announceNewQuest(gameId: String) {
        if (notifyUrl.isBlank()) return
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token ?: return
        withContext(Dispatchers.IO) {
            val connection = URL(notifyUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $idToken")
                connection.outputStream.use { it.write("""{"gameId":"$gameId"}""".toByteArray()) }
                val code = connection.responseCode
                if (code !in 200..299) Log.w(TAG, "New-quest endpoint answered $code")
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        const val TOPIC_NEW_QUESTS = "new_games"
        private const val TAG = "PushNotifications"
    }
}
