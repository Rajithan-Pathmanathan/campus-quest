package com.campusquest

import com.campusquest.device.sensor.GeneralizedFusionCalculator
import com.campusquest.domain.model.SensorSignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeneralizedFusionCalculatorTest {

    private lateinit var calculator: GeneralizedFusionCalculator

    @Before
    fun setUp() {
        calculator = GeneralizedFusionCalculator(
            baseGpsWeight = 0.40f,
            baseLightWeight = 0.30f,
            baseMotionWeight = 0.30f,
            thresholdCutoff = 0.85f
        )
    }

    @Test
    fun allSensorsAvailable_allSatisfied_producesHundredPercentAndThreshold() {
        val inputs = GeneralizedFusionCalculator.SignalInputs(
            gpsScore = 1.0f,
            lightMatched = true,
            motionDetected = true,
            proximityNear = true
        )

        val result = calculator.calculate("G1", "CP1", inputs)

        assertEquals(1.0f, result.totalScore, 0.001f)
        assertEquals(100, result.progressPercent)
        assertTrue(result.thresholdReached)
        assertTrue(result.proximityNear)
        assertTrue(result.canClaimDiscovery)
        assertEquals(3, result.activeSignals.size)
    }

    @Test
    fun allSensorsAvailable_gpsAndLightSatisfied_scoresSeventyPercent_belowThreshold() {
        val inputs = GeneralizedFusionCalculator.SignalInputs(
            gpsScore = 1.0f,
            lightMatched = true,
            motionDetected = false,
            proximityNear = false
        )

        val result = calculator.calculate("G1", "CP1", inputs)

        // 0.40 * 1.0 + 0.30 * 1.0 + 0.30 * 0.0 = 0.70
        assertEquals(0.70f, result.totalScore, 0.001f)
        assertEquals(70, result.progressPercent)
        assertFalse("70% is below 85% threshold", result.thresholdReached)
        assertFalse(result.canClaimDiscovery)
    }

    @Test
    fun missingLightSensor_normalizesWeightsToGpsAndMotion() {
        val inputs = GeneralizedFusionCalculator.SignalInputs(
            gpsScore = 1.0f,
            lightMatched = null, // Sensor missing
            motionDetected = true,
            proximityNear = true
        )

        val result = calculator.calculate("G1", "CP1", inputs)

        // Active weights: GPS (0.4) + Motion (0.3) = 0.7
        // Normalized: GPS = 0.4/0.7 = 0.5714, Motion = 0.3/0.7 = 0.4286
        // Total = 0.5714 * 1.0 + 0.4286 * 1.0 = 1.0
        assertEquals(1.0f, result.totalScore, 0.001f)
        assertNull("Light score should be null when sensor missing", result.lightScore)
        assertEquals(1.0f, result.motionScore ?: 0f, 0.001f)
        assertTrue(result.thresholdReached)
        assertTrue(result.canClaimDiscovery)
        assertFalse(result.activeSignals.contains(SensorSignalType.AMBIENT_LIGHT))
    }

    @Test
    fun missingLightSensor_gpsOnlyMatched_calculatesExactNormalizedScore() {
        val inputs = GeneralizedFusionCalculator.SignalInputs(
            gpsScore = 1.0f,
            lightMatched = null,
            motionDetected = false,
            proximityNear = false
        )

        val result = calculator.calculate("G1", "CP1", inputs)

        // Total = 0.4 / 0.7 = ~0.5714 (57%)
        assertEquals(0.5714f, result.totalScore, 0.005f)
        assertEquals(57, result.progressPercent)
        assertFalse(result.thresholdReached)
    }

    @Test
    fun gpsOnlyDevice_bothLightAndMotionMissing_normalizesGpsToHundredPercent() {
        val inputs = GeneralizedFusionCalculator.SignalInputs(
            gpsScore = 1.0f,
            lightMatched = null,
            motionDetected = null,
            proximityNear = false
        )

        val result = calculator.calculate("G1", "CP1", inputs)

        assertEquals(1.0f, result.totalScore, 0.001f)
        assertTrue(result.thresholdReached)
        assertFalse("Cannot claim discovery until proximity gate is satisfied", result.canClaimDiscovery)
    }

    @Test
    fun thresholdReached_butProximityFar_cannotClaimDiscovery() {
        val inputs = GeneralizedFusionCalculator.SignalInputs(
            gpsScore = 1.0f,
            lightMatched = true,
            motionDetected = true,
            proximityNear = false // Not near
        )

        val result = calculator.calculate("G1", "CP1", inputs)

        assertTrue(result.thresholdReached)
        assertFalse("Proximity is not near", result.proximityNear)
        assertFalse(result.canClaimDiscovery)
    }
}
