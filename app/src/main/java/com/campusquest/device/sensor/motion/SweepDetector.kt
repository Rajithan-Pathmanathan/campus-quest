package com.campusquest.device.sensor.motion

import kotlin.math.abs

/**
 * Sweep gesture detector implementing a state machine:
 * IDLE -> MOVING_DIRECTION -> DIRECTION_REVERSAL within timeWindowMs -> SWEEP_DETECTED.
 */
class SweepDetector(
    private val accelerationThreshold: Float = 6.0f,
    private val timeWindowMs: Long = 1500L
) : MotionDetector {

    enum class SweepState {
        IDLE,
        MOVING_POSITIVE,
        MOVING_NEGATIVE,
        SWEEP_DETECTED
    }

    private var state: SweepState = SweepState.IDLE
    private var stateStartTime: Long = 0L

    override fun process(
        linear: GravityFilter.LinearAcceleration,
        gravity: GravityFilter.GravityVector,
        timestamp: Long
    ): Boolean {
        if (state == SweepState.SWEEP_DETECTED) {
            return true
        }

        // Check timeout
        if (state != SweepState.IDLE && (timestamp - stateStartTime) > timeWindowMs) {
            state = SweepState.IDLE
            stateStartTime = 0L
        }

        // Dominant horizontal axis (dx or dy)
        val horizontalAcc = if (abs(linear.dx) >= abs(linear.dy)) linear.dx else linear.dy

        when (state) {
            SweepState.IDLE -> {
                if (horizontalAcc > accelerationThreshold) {
                    state = SweepState.MOVING_POSITIVE
                    stateStartTime = timestamp
                } else if (horizontalAcc < -accelerationThreshold) {
                    state = SweepState.MOVING_NEGATIVE
                    stateStartTime = timestamp
                }
            }
            SweepState.MOVING_POSITIVE -> {
                if (horizontalAcc < -accelerationThreshold) {
                    state = SweepState.SWEEP_DETECTED
                }
            }
            SweepState.MOVING_NEGATIVE -> {
                if (horizontalAcc > accelerationThreshold) {
                    state = SweepState.SWEEP_DETECTED
                }
            }
            SweepState.SWEEP_DETECTED -> {
                // Done
            }
        }

        return state == SweepState.SWEEP_DETECTED
    }

    override val isVerified: Boolean get() = state == SweepState.SWEEP_DETECTED
    val currentState: SweepState get() = state

    override fun reset() {
        state = SweepState.IDLE
        stateStartTime = 0L
    }
}
