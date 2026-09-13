# Member 3 Integration Guide: Location, Geofencing, Sensors & Fusion

**Responsible:** Member 3 (Location, Sensors & Fusion)  
**Branch:** `feature/m3-location-sensors-fusion`  
**Status:** Complete & Frozen for Team Integration  

---

## 1. Subsystem Architecture Overview

Member 3 owns all physical-signal acquisition and processing:

```
                    MEMBER 3 SUBSYSTEM
                            │
       ┌────────────────────┼────────────────────┐
       ↓                    ↓                    ↓
 LocationState         SensorState          FusionResult
 (GPS/Bearing)      (Light/Motion/Prox)   (Weighted Score)
       │                    │                    │
       └────────────────────┼────────────────────┘
                            ↓
                  CheckpointEligibility
                  (eligibleForScan: Boolean)
                            │
               ┌────────────┴────────────┐
               ↓                         ↓
          Member 1                  Member 4
          (Map UI)                 (Scan HUD)
```

---

## 2. Integration Contracts for Dependent Members

### 2.1 For Member 1 (Player UI & Campus Map)
Member 1 renders the live map and navigates players toward checkpoints.

1. **Location Tracking ([`LocationRepository`](file:///d:/MIT/Academics/Year%2002/Semester%2002/Mobile%20Application%20Development/Slides/Mobile%20Application%20Project/campus-quest/app/src/main/java/com/campusquest/domain/repository/LocationRepository.kt)):**
   ```kotlin
   val locationState: StateFlow<LocationState>
   val permissionState: StateFlow<LocationPermissionState>
   fun startTracking(accuracyMode: LocationAccuracyMode)
   fun stopTracking()
   ```
2. **Map Geofence Overlays ([`MapGeofenceDataProvider`](file:///d:/MIT/Academics/Year%2002/Semester%2002/Mobile%20Application%20Development/Slides/Mobile%20Application%20Project/campus-quest/app/src/main/java/com/campusquest/device/location/MapGeofenceDataProvider.kt)):**
   ```kotlin
   val visualState: GeofenceVisualState = provider.createVisualState(checkpoint, currentLocation)
   // Contains: distanceMeters, radiusMeters, bearingDegrees, state (INSIDE / OUTSIDE)
   // Member 1 decides visual styling, colors, stroke and animation.
   ```
3. **Compass & Heading ([`BearingCalculator`](file:///d:/MIT/Academics/Year%2002/Semester%2002/Mobile%20Application%20Development/Slides/Mobile%20Application%20Project/campus-quest/app/src/main/java/com/campusquest/device/location/BearingCalculator.kt)):**
   ```kotlin
   val bearing: Float = BearingCalculator.calculateBearing(userLat, userLng, cpLat, cpLng) // 0..360°
   ```

---

### 2.2 For Member 4 (Quest & Scan Gameplay)
Member 4 builds the Scan HUD, fusion meter, and clue reveal dialogs.

1. **Scan Eligibility ([`CheckpointEligibility`](file:///d:/MIT/Academics/Year%2002/Semester%2002/Mobile%20Application%20Development/Slides/Mobile%20Application%20Project/campus-quest/app/src/main/java/com/campusquest/domain/model/CheckpointEligibility.kt)):**
   - Check `eligibility.eligibleForScan` to enable the "Open Scan HUD" button.
2. **Sensor Repository & Fusion Result ([`SensorRepository`](file:///d:/MIT/Academics/Year%2002/Semester%2002/Mobile%20Application%20Development/Slides/Mobile%20Application%20Project/campus-quest/app/src/main/java/com/campusquest/domain/repository/SensorRepository.kt)):**
   ```kotlin
   repository.startScanSession(gameId, checkpointId, targetSignature, motionType)
   repository.fusionResult.collectLatest { result ->
       progressBar.progress = result.progressPercent // 0..100
       if (result.canClaimDiscovery) {
           // Discovery Ready! threshold >= 85% AND proximityNear == true
           showRevealDialog()
       }
   }
   ```
3. **UI Testing Simulation Harness ([`SensorSimulationFixture`](file:///d:/MIT/Academics/Year%2002/Semester%2002/Mobile%20Application%20Development/Slides/Mobile%20Application%20Project/campus-quest/app/src/main/java/com/campusquest/device/sensor/SensorSimulationFixture.kt)):**
   - M4 can simulate signals offline without moving physically:
     ```kotlin
     val fixture = SensorSimulationFixture()
     fixture.simulateLightMatch(lux = 450f, matched = true)
     fixture.simulateMotionDetected(detected = true)
     fixture.simulateProximity(isNear = true)
     ```
4. **Diagnostic Overlay ([`SensorDiagnostics`](file:///d:/MIT/Academics/Year%2002/Semester%2002/Mobile%20Application%20Development/Slides/Mobile%20Application%20Project/campus-quest/app/src/main/java/com/campusquest/domain/model/SensorDiagnostics.kt)):**
   - Inspect `diag.warnings`, `diag.currentLux`, `diag.proximityNear`, and `diag.gpsAccuracyMeters`.

---

### 2.3 For Member 2 & Member 5 (Creator & Backend)
1. **Dynamic Checkpoint IDs & Keys:**
   - Geofence key format: `"${gameId}#${checkpointId}"` via `GeofenceKey.build(gameId, checkpointId)`.
   - Never hardcode static IDs (`R001`–`R006` are test seeds only).
2. **Checkpoint Physical Fields:**
   - `radiusM: Float` (e.g. 15.0f – 50.0f)
   - `lightSignature: LightSignature` (minLux .. maxLux)
   - `motionType: String` ("SWEEP", "TILT", "SHAKE")

---

### 2.4 For Member 6 (Room & Offline Sync)
- Discovery events require `thresholdReached == true` AND `proximityNear == true`.
- When offline, M6 caches discovery timestamp and player progress locally in Room for later synchronization.
