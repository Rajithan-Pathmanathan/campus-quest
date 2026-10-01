package com.campusquest.data.repository

import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorState
import com.campusquest.domain.repository.SensorRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation of SensorRepository wrapping the SensorFusionEngine.
 */
class SensorRepositoryImpl(
    private val fusionEngine: SensorFusionEngine
) : SensorRepository {

    override val sensorState: StateFlow<SensorState> = fusionEngine.sensorState
    override val fusionResult: StateFlow<FusionResult> = fusionEngine.fusionResult

    override fun startScanSession(
        gameId: String,
        checkpointId: String,
        targetSignature: LightSignature,
        motionType: MotionType,
        initialGpsScore: Float
    ) {
        fusionEngine.startListening(
            gameId = gameId,
            checkpointId = checkpointId,
            targetSignature = targetSignature,
            motionType = motionType,
            gpsScore = initialGpsScore
        )
    }

    override fun updateGpsScore(score: Float) {
        fusionEngine.updateGpsScore(score)
    }

    override fun stopScanSession() {
        fusionEngine.stopListening()
    }

    override fun resetScanSession() {
        fusionEngine.reset()
    }
}
