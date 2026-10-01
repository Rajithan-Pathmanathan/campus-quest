package com.campusquest.domain.repository

import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture repository interface for physical hardware sensor signals and multi-sensor fusion.
 * Exposes pure Kotlin flows to Member 4's ViewModel layer without leaking Android hardware APIs.
 */
interface SensorRepository {
    val sensorState: StateFlow<SensorState>
    val fusionResult: StateFlow<FusionResult>

    fun startScanSession(
        gameId: String,
        checkpointId: String,
        targetSignature: LightSignature,
        motionType: MotionType,
        initialGpsScore: Float = 1.0f
    )

    fun updateGpsScore(score: Float)
    fun stopScanSession()
    fun resetScanSession()
}
