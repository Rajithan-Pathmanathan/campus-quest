package com.campusquest.ui.quest

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays the "heartbeat" while the player approaches an ember. The interval comes from M3's
 * QuestCues.heartbeatIntervalMs via the ViewModel; this class only vibrates.
 */
class HeartbeatHaptics(context: Context, private val scope: CoroutineScope) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var job: Job? = null
    private var currentIntervalMs: Long? = null

    /** Pass null to stop. Restarting is skipped while the interval is unchanged. */
    fun update(intervalMs: Long?) {
        if (intervalMs == currentIntervalMs) return
        currentIntervalMs = intervalMs
        job?.cancel()
        job = null
        val v = vibrator ?: return
        if (intervalMs == null || !v.hasVibrator()) return
        job = scope.launch {
            while (isActive) {
                v.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
                delay(intervalMs)
            }
        }
    }

    fun stop() = update(null)
}
