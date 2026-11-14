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
   // Get both from the app container — never construct SensorFusionEngine/LocationManagerHelper in UI.
   val app = requireActivity().application as CampusQuestApplication
   app.locationRepository.startTracking()        // after location permission is granted
   app.sensorRepository.startScanSession(checkpoint) // GPS score is fed automatically from distance
   // onStop: app.sensorRepository.stopScanSession(); app.locationRepository.stopTracking()
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


---

## 3. Game Logic & MVP Policies (decided in `fix/m3-integration-issues`)

### 3.1 Fusion scores
| Signal | Weight | Score |
|---|---|---|
| GPS | 0.40 | `GpsScoreCalculator`: `1 − distance / (2 × radius)`, clamped 0..1. Centre = 1.0, geofence edge = 0.5, 2×radius = 0.0. No fix or accuracy worse than 35 m → **0.0**. |
| Light | 0.30 | `LightSignature.score(lux)`: 1.0 inside `[minLux, maxLux]`, linear fall-off to 0.0 at 25 % of the range width outside it (minimum 20 lux). |
| Motion | 0.30 | Binary: 1.0 once the checkpoint's gesture (`MotionType.fromString(checkpoint.motionType)`, unknown → SWEEP) is detected. |

Threshold **0.85**. With full light + motion, GPS must be ≥ 0.625, i.e. the player must be within **0.75 × radius** of the checkpoint. Being anywhere inside the geofence is not enough on its own (workplan §36).

### 3.2 Missing hardware (workplan §26, §56, §75)
| Missing | Behaviour |
|---|---|
| GPS fix / permission | GPS score 0 → threshold unreachable. Scan screen asks for permission and shows "Get closer to the checkpoint". |
| Light sensor | Light channel excluded; GPS + motion weights re-normalized. |
| Accelerometer | Motion channel excluded; GPS + light weights re-normalized. |
| Proximity sensor | Final gate bypassed (treated as NEAR). Diagnostics warns "Close-range physical gate bypassed". |

GPS is never re-normalized away: otherwise a device without a fix could discover from anywhere.

### 3.3 Lifecycle
- One `SensorFusionEngine` per app, owned by `SensorRepositoryImpl` (`CampusQuestApplication.sensorRepository`).
- `startScanSession(checkpoint)` re-scopes game/checkpoint, resets sensors, and resets GPS evidence to 0 — nothing leaks between checkpoints or games.
- `SensorFusionEngine.close()` cancels its observer (tests / teardown).

### 3.4 Permissions & geofencing
- Foreground location (FINE/COARSE) is requested at runtime on the Scan screen; `LocationManagerHelper.startLocationUpdates` refuses to start without it.
- Background location is **not** requested in the MVP. On Android 10+ `GeofencingClient` therefore cannot register geofences; geofence ENTER is foreground-only and `ScopedGeofenceFallback` (distance-based) is the source of truth for scan eligibility. `GeofenceBroadcastReceiver` events are in-memory and are lost if the process is not running.
- `LocationState` / `LocationPermissionState` live in `domain.model` and contain no Android types.

---

## 4. Story Game Logic (added in `feat/m3-story-game-logic`)

Campus Quest is played as a story: the sensors are part of the fiction, and **players never see raw numbers** (no "GPS 40%", no "312 lx", no fusion percentage). M3 provides the logic; the screens (M1/M4) only display the words and effects below. All of it is plain Kotlin with no Android types, in `domain/quest/QuestCues.kt`.

### 4.1 Zones around each checkpoint (`nextQuestZone`)
| Zone | Distance | What the player gets |
|---|---|---|
| `FAR` | > 200 m | Rounded distance + compass direction |
| `OUTER` | 200 → 100 m | Heartbeat vibration starts (`heartbeatIntervalMs`) |
| `MIDDLE` | 100 → 10 m | The chapter **trial** (see 4.3) |
| `RELIC` | ≤ 10 m **and** GPS accuracy ≤ 15 m | "✨ The relic appears!": enable the scan |

- Hysteresis: once `RELIC`, it stays until the player is > 20 m away (no flicker at the edge).
- Weak GPS (accuracy > 15 m) can never trigger `RELIC`, so players can't "arrive" by GPS noise.
- Call `nextQuestZone(previousZone, distanceM, accuracyM)` on each location update; pass `FAR` as `previousZone` when the target checkpoint changes. Constants are in `QuestZones`.

### 4.2 Player-facing cues (`QuestCues`)
| Raw value | Show instead | Function |
|---|---|---|
| Distance | "~250 m", "~45 m", **"Very close…"** under 20 m | `distanceText(m)` |
| Getting closer | Freezing → Cold → Warm → Hot → **Burning!** | `warmth(m)` |
| Closeness (haptics) | heartbeat every 1.5 s at 200 m → 0.3 s near the relic | `heartbeatIntervalMs(zone, m)` |
| Zone | "The Resonator hears a heartbeat!" … | `zoneLine(zone)` |
| Light vs the checkpoint's light | "Too much sun! … Find shade." / "Too dark… step into the sunlight." / "It's afraid of the light…" | `lightHint(signature, lux)` |
| Fusion progress | "A faint glow…" → "The ember flickers…" → "The ember is blazing!" | `glow(progressPercent).label` |
| Trial progress | "2 more shakes!", "Spilled! Steady… start again." | `trialHint(motionType, trialStatus)` |
| Proximity gate | "Cup the ember — hold the Resonator close." | `shieldHint(near)` |

`LightMode.of(signature)` classifies a checkpoint's light as `BRIGHT` (min ≥ 10,000 lx), `DARK` (max ≤ 100 lx) or `SHADE` (everything else), so the hint knows what to say.

### 4.3 Motion trials (what `motionType` now means)
| `motionType` | Trial | Detector | Rule |
|---|---|---|---|
| `SHAKE` | Wake the ember | `RepeatedGestureDetector(ShakeDetector())` | 3 separate shakes (450 ms cooldown so one shake isn't counted twice) |
| `SWEEP` | Clear the cobwebs | `RepeatedGestureDetector(SweepDetector())` | 3 separate sweeps |
| `TILT` | Carry the water jar | `SteadyHoldDetector` | Keep the phone within 15° of its starting pose for 10 s; tipping further "spills" and restarts |

Progress is published on `SensorState.trialStatus` (`TrialStatus`: `completed`, `remaining`, `started`, `justSpilled`, `holdFraction` for an animation). `motionDetected` becomes true only when the whole trial is complete, so fusion scoring is unchanged.

### 4.4 Game 1: *The Founder's Lantern* (content for M6/M1 to seed)
Story: the founders' lantern fell and its flame split into three embers hiding across campus (one in the light, one in the shade, one in the dark). The player carries the **Resonator** and relights the lantern.

| # | Chapter | Location | Coordinates | Light (lux) | Trial |
|---|---|---|---|---|---|
| 1 | The Ember in the Light | Tennis Court | 6.9722948878782995, 79.91638354596967 | 10,000 – 120,000 (bright) | `SHAKE` |
| 2 | The Ember in the Shade | Buddha Statue | 6.974319507376335, 79.91600245459793 | 500 – 10,000 (shade) | `TILT` |
| 3 | The Ember in the Dark | Medical Centre | 6.976196957961348, 79.91471980379754 | 0 – 60 (dark) | `SWEEP` |

Chapters are ~230 m and ~250 m apart. Use `radiusM = 15` (GPS scoring scale); the relic itself appears at `QuestZones.RELIC_RADIUS_M` (10 m). Memory (`lore`) texts still need real facts about each place. Light thresholds and the 10 m radius are first guesses to tune after an on-campus walk test.
