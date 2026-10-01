package com.campusquest.domain.model

/**
 * Structured diagnostic telemetry provided by M3 for developer overlay and M4 Scan HUD diagnostics.
 */
data class SensorDiagnostics(
    val lightAvailable: Boolean,
    val accelerometerAvailable: Boolean,
    val proximityAvailable: Boolean,
    val currentLux: Float?,
    val linearAcceleration: Float?,
    val proximityNear: Boolean?,
    val gpsAccuracyMeters: Float?,
    val warnings: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
