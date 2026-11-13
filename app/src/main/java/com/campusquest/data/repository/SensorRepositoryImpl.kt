package com.campusquest.data.repository

import com.campusquest.device.location.DistanceCalculator
import com.campusquest.device.location.HaversineDistanceCalculator
import com.campusquest.device.sensor.SensorFusionEngine
import com.campusquest.domain.fusion.GpsScoreCalculator
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.FusionResult
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.SensorState
import com.campusquest.domain.repository.LocationRepository
import com.campusquest.domain.repository.SensorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Owns the single SensorFusionEngine and feeds it live GPS evidence from LocationRepository.
 */
class SensorRepositoryImpl(
    private val fusionEngine: SensorFusionEngine,
    private val locationRepository: LocationRepository,
    private val distanceCalculator: DistanceCalculator = HaversineDistanceCalculator(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : SensorRepository {

    override val sensorState: StateFlow<SensorState> = fusionEngine.sensorState
    override val fusionResult: StateFlow<FusionResult> = fusionEngine.fusionResult

    private var gpsJob: Job? = null

    override fun startScanSession(checkpoint: Checkpoint) {
        gpsJob?.cancel()

        fusionEngine.startListening(
            gameId = checkpoint.gameId,
            checkpointId = checkpoint.id,
            targetSignature = checkpoint.lightSignature,
            motionType = MotionType.fromString(checkpoint.motionType),
            gpsScore = 0.0f
        )

        gpsJob = scope.launch {
            locationRepository.locationState.collect { location ->
                fusionEngine.updateGpsScore(
                    GpsScoreCalculator.score(
                        location = location,
                        targetLat = checkpoint.lat,
                        targetLng = checkpoint.lng,
                        radiusMeters = checkpoint.radiusM,
                        distance = distanceCalculator::calculateDistanceMeters
                    )
                )
            }
        }
    }

    override fun stopScanSession() {
        gpsJob?.cancel()
        gpsJob = null
        fusionEngine.stopListening()
    }

    override fun resetScanSession() {
        fusionEngine.reset()
    }
}
