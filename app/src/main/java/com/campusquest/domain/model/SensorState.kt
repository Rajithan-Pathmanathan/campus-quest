package com.campusquest.domain.model

/**
 * Snapshot of physical hardware sensor telemetry captured by M3.
 * Exposes clean, typed sensor readings to M4 without exposing Android SensorManager APIs.
 */
data class SensorState(
    val currentLux: Float? = null,
    val lightMatched: Boolean? = null,
    val linearAccelerationMagnitude: Float? = null,
    val motionDetected: Boolean? = null,
    val isNear: Boolean? = null,
    val activeMotionType: MotionType? = null,
    val availableSensors: Set<SensorSignalType> = emptySet(),
    val timestamp: Long = System.currentTimeMillis()
)
