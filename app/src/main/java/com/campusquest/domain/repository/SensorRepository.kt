package com.campusquest.domain.repository

import com.campusquest.domain.model.FusionState
import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SensorRepository {
    val sensorReadings: Flow<SensorReading>
    val fusionState: StateFlow<FusionState>

    fun startListening(targetSignature: LightSignature, motionType: String)
    fun stopListening()
    fun resetVerification()
}
