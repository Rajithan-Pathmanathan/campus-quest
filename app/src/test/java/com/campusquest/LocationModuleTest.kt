package com.campusquest

import android.location.Location
import com.campusquest.device.location.GpsSmoothingFilter
import com.campusquest.device.location.HaversineDistanceCalculator
import com.campusquest.device.location.LocationPermissionState
import com.campusquest.device.location.LocationState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationModuleTest {

    private lateinit var calculator: HaversineDistanceCalculator
    private lateinit var smoothingFilter: GpsSmoothingFilter

    @Before
    fun setUp() {
        calculator = HaversineDistanceCalculator()
        smoothingFilter = GpsSmoothingFilter(alpha = 0.5f, maxAcceptedAccuracyMeters = 30f)
    }

    @Test
    fun haversineDistance_samePointReturnsZero() {
        val lat = 37.4275
        val lng = -122.1697
        val distance = calculator.calculateDistanceMeters(lat, lng, lat, lng)
        assertEquals(0.0f, distance, 0.01f)
    }

    @Test
    fun haversineDistance_oneDegreeLatitudeApproximates111Km() {
        // 1 degree latitude roughly equals 111.195 km at equator
        val distance = calculator.calculateDistanceMeters(0.0, 0.0, 1.0, 0.0)
        assertEquals(111195.0f, distance, 500.0f)
    }

    @Test
    fun haversineDistance_knownCampusPoints() {
        // Distance between Stanford Oval (37.4300, -122.1700) and Memorial Church (37.4275, -122.1700) is approx ~278m
        val ovalLat = 37.4300
        val ovalLng = -122.1700
        val churchLat = 37.4275
        val churchLng = -122.1700

        val distance = calculator.calculateDistanceMeters(ovalLat, ovalLng, churchLat, churchLng)
        assertTrue("Distance should be approx 278m (actual: $distance)", distance in 270f..285f)
    }

    @Test
    fun isWithinRadius_correctlyEvaluatesGeofenceThreshold() {
        val centerLat = 37.4275
        val centerLng = -122.1697
        val targetRadius = 25.0f

        // Point 10 meters away
        val insidePointLat = 37.42759 // ~10m north
        val insidePointLng = -122.1697
        assertTrue(calculator.isWithinRadius(insidePointLat, insidePointLng, centerLat, centerLng, targetRadius))

        // Point 50 meters away
        val outsidePointLat = 37.42795 // ~50m north
        val outsidePointLng = -122.1697
        assertFalse(calculator.isWithinRadius(outsidePointLat, outsidePointLng, centerLat, centerLng, targetRadius))
    }

    @Test
    fun calculateBearing_cardinalDirections() {
        // Due North from (0,0) to (1,0) should be 0° (or 360°)
        val bearingNorth = calculator.calculateBearingDegrees(0.0, 0.0, 1.0, 0.0)
        assertEquals(0.0f, bearingNorth, 1.0f)

        // Due East from (0,0) to (0,1) should be 90°
        val bearingEast = calculator.calculateBearingDegrees(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0f, bearingEast, 1.0f)

        // Due South from (1,0) to (0,0) should be 180°
        val bearingSouth = calculator.calculateBearingDegrees(1.0, 0.0, 0.0, 0.0)
        assertEquals(180.0f, bearingSouth, 1.0f)
    }

    @Test
    fun gpsSmoothingFilter_appliesExponentialMovingAverage() {
        val loc1 = mockk<Location>()
        every { loc1.latitude } returns 10.0
        every { loc1.longitude } returns 20.0
        every { loc1.hasAccuracy() } returns true
        every { loc1.accuracy } returns 5.0f
        every { loc1.time } returns 1000L

        val firstResult = smoothingFilter.filter(loc1)
        assertEquals(10.0, firstResult.latitude, 0.001)
        assertEquals(20.0, firstResult.longitude, 0.001)

        val loc2 = mockk<Location>()
        every { loc2.latitude } returns 12.0
        every { loc2.longitude } returns 22.0
        every { loc2.hasAccuracy() } returns true
        every { loc2.accuracy } returns 5.0f
        every { loc2.time } returns 2000L

        // With alpha = 0.5: smoothed = 0.5 * 12 + 0.5 * 10 = 11.0
        val secondResult = smoothingFilter.filter(loc2)
        assertEquals(11.0, secondResult.latitude, 0.001)
        assertEquals(21.0, secondResult.longitude, 0.001)
    }

    @Test
    fun locationState_formattedAccuracyAndValidFix() {
        val rawLoc = mockk<Location>()
        val state = LocationState(
            permissionState = LocationPermissionState.Granted,
            servicesEnabled = true,
            location = rawLoc,
            accuracyMeters = 5.2f
        )

        assertTrue(state.hasValidFix)
        assertEquals("±5.2m", state.formattedAccuracy())
    }
}
