package com.campusquest.domain.repository

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.SensorState
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture repository interface for physical hardware sensor signals and multi-sensor fusion.
 * Exposes pure Kotlin flows to Member 4's ViewModel layer without leaking Android hardware APIs.
 */
interface SensorRepository {
    val sensorState: StateFlow<SensorState>
    val fusionResult: StateFlow<FusionResult>

    /**
     * Starts a scan for [checkpoint]: sensors start listening with the checkpoint's light signature
     * and motion type, and the GPS score is continuously derived from the player's distance
     * to the checkpoint (LocationRepository must be tracking for GPS evidence to accrue).
     */
    fun startScanSession(checkpoint: Checkpoint)

    fun stopScanSession()
    fun resetScanSession()
}
