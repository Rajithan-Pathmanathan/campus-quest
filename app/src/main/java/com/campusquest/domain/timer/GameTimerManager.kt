package com.campusquest.domain.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object GameTimerManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Inactive)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    sealed class TimerState {
        object Inactive : TimerState()
        data class Active(
            val gameId: String,
            val timeLimitMinutes: Int,
            val remainingSeconds: Int,
            val totalSeconds: Int,
            val isExpired: Boolean
        ) : TimerState() {
            val formattedTime: String
                get() {
                    val m = remainingSeconds / 60
                    val s = remainingSeconds % 60
                    return String.format("⏱️ %02d:%02d", m, s)
                }
        }
    }

    fun startOrResumeTimer(gameId: String, timeLimitMinutes: Int = 30) {
        val currentState = _timerState.value
        if (currentState is TimerState.Active && currentState.gameId == gameId && !currentState.isExpired) {
            return
        }

        timerJob?.cancel()
        val totalSeconds = (timeLimitMinutes * 60).coerceAtLeast(60)

        _timerState.value = TimerState.Active(
            gameId = gameId,
            timeLimitMinutes = timeLimitMinutes,
            remainingSeconds = totalSeconds,
            totalSeconds = totalSeconds,
            isExpired = false
        )

        timerJob = scope.launch {
            var left = totalSeconds
            while (left > 0) {
                delay(1000)
                left--
                _timerState.value = TimerState.Active(
                    gameId = gameId,
                    timeLimitMinutes = timeLimitMinutes,
                    remainingSeconds = left,
                    totalSeconds = totalSeconds,
                    isExpired = left <= 0
                )
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = TimerState.Inactive
    }

    /** Returns bonus points based on time remaining ratio (e.g. up to 150 points for fast completion). */
    fun calculateTimeBonus(gameId: String): Int {
        val state = _timerState.value
        if (state is TimerState.Active && state.gameId == gameId && !state.isExpired) {
            val ratio = state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()
            return (ratio * 150f).toInt().coerceAtLeast(0)
        }
        return 0
    }
}
