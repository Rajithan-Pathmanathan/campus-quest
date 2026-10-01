package com.campusquest

import com.campusquest.domain.fusion.DiscoveryGateEvaluator
import com.campusquest.domain.fusion.DiscoveryGateInput
import com.campusquest.domain.fusion.FusionConfig
import com.campusquest.domain.fusion.FusionInputs
import com.campusquest.domain.fusion.GeneralizedFusionCalculator
import com.campusquest.domain.model.SensorSignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FusionNormalizationMatrixTest {

    private lateinit var calculator: GeneralizedFusionCalculator
    private lateinit var evaluator: DiscoveryGateEvaluator

    @Before
    fun setUp() {
        calculator = GeneralizedFusionCalculator(
            FusionConfig(
                gpsWeight = 0.40f,
                lightWeight = 0.30f,
                motionWeight = 0.30f,
                threshold = 0.85f
            )
        )
        evaluator = DiscoveryGateEvaluator()
    }

    // =========================================================================
    // 1. All Sensors Available (Base Distribution: 40% GPS, 30% Light, 30% Motion)
    // =========================================================================

    @Test
    fun allSensorsAvailable_perfectResonance_scoresHundredPercent() {
        val inputs = FusionInputs(
            gpsScore = 1.0f,
            lightScore = 1.0f,
            motionScore = 1.0f
        )
        val result = calculator.calculate("G1", "CP1", inputs, proximityNear = true)

        assertEquals(1.0f, result.totalScore, 0.001f)
        assertEquals(100, result.progressPercent)
        assertTrue("85% threshold reached", result.thresholdReached)
        assertTrue(result.proximityNear)
        assertTrue("Both threshold and proximity satisfied", evaluator.evaluate(DiscoveryGateInput(result, true)))
    }

    @Test
    fun allSensorsAvailable_realisticCase_gps90_light100_motion80_scoresNinetyPercent() {
        // 0.4(0.9) + 0.3(1.0) + 0.3(0.8) = 0.36 + 0.30 + 0.24 = 0.90
        val inputs = FusionInputs(
            gpsScore = 0.90f,
            lightScore = 1.00f,
            motionScore = 0.80f
        )
        val result = calculator.calculate("G1", "CP1", inputs, proximityNear = true)

        assertEquals(0.90f, result.totalScore, 0.001f)
        assertEquals(90, result.progressPercent)
        assertTrue("90% >= 85% threshold", result.thresholdReached)
        assertTrue(evaluator.evaluate(result, proximityNear = true))
    }

    // =========================================================================
    // 2. Strict Threshold Boundary Verification (84% vs 85% vs 86%)
    // =========================================================================

    @Test
    fun thresholdBoundaries_strictCutoffAtEightyFivePercent() {
        // 84% score: GPS = 0.84, Light = 0.84, Motion = 0.84
        val result84 = calculator.calculate("G1", "CP1", FusionInputs(0.84f, 0.84f, 0.84f))
        assertEquals(0.84f, result84.totalScore, 0.001f)
        assertFalse("84% must be below 85% threshold", result84.thresholdReached)

        // 85% score: GPS = 0.85, Light = 0.85, Motion = 0.85
        val result85 = calculator.calculate("G1", "CP1", FusionInputs(0.85f, 0.85f, 0.85f))
        assertEquals(0.85f, result85.totalScore, 0.001f)
        assertTrue("85% must reach threshold", result85.thresholdReached)

        // 86% score: GPS = 0.86, Light = 0.86, Motion = 0.86
        val result86 = calculator.calculate("G1", "CP1", FusionInputs(0.86f, 0.86f, 0.86f))
        assertEquals(0.86f, result86.totalScore, 0.001f)
        assertTrue("86% must reach threshold", result86.thresholdReached)
    }

    // =========================================================================
    // 3. Proximity Decoupling & Physical Gate Tests
    // =========================================================================

    @Test
    fun thresholdReached_butProximityFar_blocksDiscovery() {
        val inputs = FusionInputs(gpsScore = 1.0f, lightScore = 1.0f, motionScore = 1.0f)
        val result = calculator.calculate("G1", "CP1", inputs, proximityNear = false)

        assertTrue("Weighted threshold reached at 100%", result.thresholdReached)
        assertFalse("Proximity is not near", result.proximityNear)
        assertFalse("Discovery cannot be claimed without physical proximity gate", evaluator.evaluate(result, proximityNear = false))

        // Once player brings phone close (proximity turns true)
        assertTrue("Discovery unlocked after proximity gate verifies", evaluator.evaluate(result, proximityNear = true))
    }

    // =========================================================================
    // 4. Missing-Sensor Weight Normalization Matrix (null vs 0.0 semantics)
    // =========================================================================

    @Test
    fun missingLightSensor_normalizesToGpsAndMotion() {
        // Device without light sensor (lightScore = null)
        // Normalized weights: GPS = 0.4/0.7 = 0.5714, Motion = 0.3/0.7 = 0.4286
        val inputs = FusionInputs(
            gpsScore = 1.0f,
            lightScore = null, // Missing hardware
            motionScore = 1.0f
        )
        val result = calculator.calculate("G1", "CP1", inputs)

        assertEquals(1.0f, result.totalScore, 0.001f)
        assertNull("Missing sensor score must be null", result.lightScore)
        assertEquals(1.0f, result.motionScore ?: 0f, 0.001f)
        assertTrue(result.thresholdReached)
        assertFalse(result.activeSignals.contains(SensorSignalType.AMBIENT_LIGHT))
    }

    @Test
    fun lightSensorExists_butReadingOutsideRange_scoresZeroWithoutReweighting() {
        // Light sensor exists but current reading does not match (lightScore = 0.0f)
        // Weights: GPS 0.4(1.0) + Light 0.3(0.0) + Motion 0.3(1.0) = 0.70
        val inputs = FusionInputs(
            gpsScore = 1.0f,
            lightScore = 0.0f, // Sensor exists but failed
            motionScore = 1.0f
        )
        val result = calculator.calculate("G1", "CP1", inputs)

        assertEquals(0.70f, result.totalScore, 0.001f)
        assertEquals(0.0f, result.lightScore ?: 0f, 0.001f)
        assertFalse("70% is below threshold", result.thresholdReached)
        assertTrue(result.activeSignals.contains(SensorSignalType.AMBIENT_LIGHT))
    }

    @Test
    fun gpsOnlyDevice_bothLightAndMotionMissing_normalizesGpsToHundredPercent() {
        val inputs = FusionInputs(
            gpsScore = 1.0f,
            lightScore = null,
            motionScore = null
        )
        val result = calculator.calculate("G1", "CP1", inputs)

        assertEquals(1.0f, result.totalScore, 0.001f)
        assertTrue(result.thresholdReached)
    }

    // =========================================================================
    // 5. Zero Active Signals Safety Test (No Divide-by-Zero)
    // =========================================================================

    @Test
    fun zeroActiveSignals_returnsSafeNoSignalResultWithoutException() {
        val inputs = FusionInputs(
            gpsScore = null,
            lightScore = null,
            motionScore = null
        )
        val result = calculator.calculate("G1", "CP1", inputs)

        assertEquals(0.0f, result.totalScore, 0.001f)
        assertEquals(0, result.progressPercent)
        assertFalse(result.thresholdReached)
        assertTrue(result.activeSignals.isEmpty())
    }
}
