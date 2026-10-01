package com.campusquest.device.sensor

import com.campusquest.device.location.LocationState
import com.campusquest.domain.model.SensorDiagnostics
import com.campusquest.domain.model.SensorSignalType
import com.campusquest.domain.model.SensorState

/**
 * Diagnostic and calibration analysis helper for physical device testing and Scan HUD overlays.
 * Transforms raw telemetry states into structured SensorDiagnostics data.
 */
class SensorDiagnosticsHelper {

    /**
     * Generates a structured snapshot of device sensor health and calibration warnings.
     */
    fun createDiagnostics(
        sensorState: SensorState,
        locationState: LocationState? = null
    ): SensorDiagnostics {
        val lightAvailable = sensorState.availableSensors.contains(SensorSignalType.AMBIENT_LIGHT)
        val accelerometerAvailable = sensorState.availableSensors.contains(SensorSignalType.ACCELEROMETER_MOTION)
        val proximityAvailable = sensorState.availableSensors.contains(SensorSignalType.PROXIMITY_GATE)

        val warnings = mutableListOf<String>()

        if (!lightAvailable) {
            warnings.add("Ambient light sensor unavailable (Dynamic fusion will re-weight)")
        }
        if (!accelerometerAvailable) {
            warnings.add("Accelerometer unavailable (Motion detection disabled)")
        }
        if (!proximityAvailable) {
            warnings.add("Proximity sensor unavailable (Close-range physical gate bypassed)")
        }
        if (locationState != null && (locationState.accuracyMeters == null || locationState.accuracyMeters > 15f)) {
            warnings.add("GPS accuracy degraded (±${"%.1f".format(locationState.accuracyMeters ?: 0f)}m > 15m threshold)")
        }

        return SensorDiagnostics(
            lightAvailable = lightAvailable,
            accelerometerAvailable = accelerometerAvailable,
            proximityAvailable = proximityAvailable,
            currentLux = sensorState.currentLux,
            linearAcceleration = sensorState.linearAccelerationMagnitude,
            proximityNear = sensorState.isNear,
            gpsAccuracyMeters = locationState?.accuracyMeters,
            warnings = warnings,
            timestamp = System.currentTimeMillis()
        )
    }
}
