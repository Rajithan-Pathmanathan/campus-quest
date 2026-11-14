package com.campusquest

import com.campusquest.device.location.HaversineDistanceCalculator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeofenceHysteresisTest {

    private lateinit var calculator: HaversineDistanceCalculator

    @Before
    fun setUp() {
        calculator = HaversineDistanceCalculator()
    }

    @Test
    fun hysteresis_enteringBoundaryRequiresExactRadius() {
        val targetLat = 37.4275
        val targetLng = -122.1697
        val radiusM = 25.0f

        // Point at 24m (inside)
        val lat24m = 37.427715
        val insideInitially = calculator.isWithinRadiusWithHysteresis(
            currentLat = lat24m,
            currentLng = targetLng,
            targetLat = targetLat,
            targetLng = targetLng,
            radiusMeters = radiusM,
            isCurrentlyInside = false
        )
        assertTrue("Point at 24m should trigger enter", insideInitially)

        // Point at 26m (outside) when not currently inside
        val lat26m = 37.427734
        val outsideInitially = calculator.isWithinRadiusWithHysteresis(
            currentLat = lat26m,
            currentLng = targetLng,
            targetLat = targetLat,
            targetLng = targetLng,
            radiusMeters = radiusM,
            isCurrentlyInside = false
        )
        assertFalse("Point at 26m should not trigger enter", outsideInitially)
    }

    @Test
    fun hysteresis_maintainsInsideStateWithinHysteresisMargin() {
        val targetLat = 37.4275
        val targetLng = -122.1697
        val radiusM = 25.0f // With 3m margin, exit threshold is 28.0m

        // Point at 26.5m (between 25m and 28m) when already inside
        val lat26_5m = 37.427738
        val staysInside = calculator.isWithinRadiusWithHysteresis(
            currentLat = lat26_5m,
            currentLng = targetLng,
            targetLat = targetLat,
            targetLng = targetLng,
            radiusMeters = radiusM,
            isCurrentlyInside = true, // Already inside
            hysteresisMarginMeters = 3.0f
        )
        assertTrue("Should stay inside within the 3m hysteresis margin (at 26.5m)", staysInside)

        // Point at 29m (beyond 28m) when already inside
        val lat29m = 37.427761
        val exits = calculator.isWithinRadiusWithHysteresis(
            currentLat = lat29m,
            currentLng = targetLng,
            targetLat = targetLat,
            targetLng = targetLng,
            radiusMeters = radiusM,
            isCurrentlyInside = true,
            hysteresisMarginMeters = 3.0f
        )
        assertFalse("Should exit when distance exceeds radius + margin (at 29m)", exits)
    }
}
