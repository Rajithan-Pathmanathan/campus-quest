package com.campusquest.domain.model

/**
 * Snapshot of physical hardware sensor telemetry captured by M3.
 */
data class SensorState(
    val currentLux: Float? = null,
    val lightMatched: Boolean? = null,
    val currentAcceleration: Float? = null,
    val motionDetected: Boolean? = null,
    val isNear: Boolean? = null,
    val availableSensors: Set<SensorSignalType> = emptySet(),
    val timestamp: Long = System.currentTimeMillis()
)
