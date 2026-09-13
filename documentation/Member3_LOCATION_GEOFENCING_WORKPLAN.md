# MEMBER 3 — LOCATION, GEOFENCING, SENSORS & FUSION WORKPLAN
## Campus Quest — Final Reassigned Team Plan

**Owner:** Member 3 (M3)  
**Primary responsibility:** Location, geofencing, sensor collection, proximity detection, and sensor-fusion engine  
**Branch:** `feature/m3-location-sensors-fusion`  
**Architecture:** Android + Kotlin, MVVM + Repository  
**Status:** Final reassignment version

---

# 1. PURPOSE

M3 owns the **hardware and location intelligence layer** of Campus Quest.

The purpose of this layer is to determine whether the player is physically approaching and interacting with the correct checkpoint using:

1. GPS/location.
2. Geofencing.
3. Ambient light.
4. Accelerometer/motion.
5. Proximity.
6. Weighted sensor fusion.

The canonical gameplay model is:

```text
Dynamic checkpoint configuration
        ↓
Location/geofence detection
        ↓
GPS distance
        ↓
Ambient-light matching
        ↓
Accelerometer/sweep detection
        ↓
Weighted fusion score
        ↓
Fusion threshold
        ↓
Proximity final gate
        ↓
M4 reveal/gameplay state
```

**Important:** proximity is **not** a fourth weighted fusion input. It is a separate final physical confirmation gate.

---

# 2. IMPORTANT REASSIGNMENT

The previous separate ownership of location/geofencing and sensor fusion has now been combined under M3.

M3 therefore owns:

- Fused Location Provider integration.
- Runtime location permissions.
- Location updates.
- Distance calculation.
- Dynamic checkpoint geofences.
- Geofence ENTER detection.
- Map markers required by gameplay.
- Battery/lifecycle handling for location.
- Ambient-light sensor access.
- Accelerometer access.
- Proximity sensor access.
- Motion/sweep detection.
- Light-signature matching.
- GPS normalization.
- Motion normalization.
- Weighted fusion.
- Fusion threshold evaluation.
- Proximity final gate.
- Sensor availability/degradation handling.
- Sensor/location unit tests and integration tests.

M3 does NOT own:

- Creator checkpoint configuration UI.
- Player scan/reveal UI.
- Firebase/Firestore implementation.
- Room implementation.
- Offline synchronization.
- Game-specific leaderboard implementation.
- FCM implementation.

---

# 3. CANONICAL DOMAIN MODEL

M3 consumes dynamic checkpoint configuration.

```kotlin
data class Checkpoint(
    val id: String,
    val gameId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float = 20f,
    val lightSignature: LightSignature,
    val clue: String,
    val lore: String,
    val order: Int = 1,
    val motionType: String = "SWEEP",
    val rarity: String = "COMMON"
)
```

The location/sensor layer must never assume:

```text
R001
R002
R003
R004
R005
R006
```

are the permanent checkpoints.

Those IDs are sample/seed data only.

---

# 4. RESPONSIBILITY BOUNDARY

M3 answers:

> "What is the current physical evidence around the player's device?"

M4 answers:

> "How should that evidence and the current scan state be presented to the player?"

M5 answers:

> "How is the authoritative game/progress data stored in Firebase?"

M6 answers:

> "How is the local data cached and synchronized?"

This separation is essential.

---

# 5. LOCATION STACK

The preferred Android location mechanism is the **Fused Location Provider**.

Conceptual flow:

```text
Android location services
        ↓
Fused Location Provider
        ↓
LocationProvider wrapper
        ↓
M3 location state
        ↓
Distance / geofence logic
        ↓
Fusion engine
```

M3 should isolate Android-specific APIs behind a testable interface.

---

# 6. LOCATION PROVIDER CONTRACT

Suggested abstraction:

```kotlin
interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
    fun observeLocationUpdates(): Flow<Location>
    fun startUpdates()
    fun stopUpdates()
}
```

The exact interface may be adjusted to the project's shared contracts.

The important principle is:

**Gameplay logic must not be tightly coupled to the Android location SDK.**

---

# 7. LOCATION PERMISSIONS

M3 owns location permission handling required for gameplay.

The implementation should distinguish:

- Permission not requested.
- Permission granted.
- Permission denied.
- Permission denied permanently/restricted.
- Location services disabled.
- Location temporarily unavailable.

The UI-facing state should be explicit.

Example:

```kotlin
sealed interface LocationPermissionState {
    data object Unknown : LocationPermissionState
    data object Granted : LocationPermissionState
    data object Denied : LocationPermissionState
    data object PermanentlyDenied : LocationPermissionState
}
```

M1/M4 consume the state for presentation.

---

# 8. PERMISSION PRINCIPLE

Request permissions when the feature needs them.

Do not request unrelated permissions simply when the application starts.

The player should understand why location access is required for checkpoint discovery.

---

# 9. LOCATION SERVICES STATE

Permission being granted does not guarantee location availability.

M3 must also handle:

```text
Permission granted
+
Location services enabled
+
Location fix available
```

as separate conditions.

Possible state:

```kotlin
data class LocationState(
    val permissionGranted: Boolean = false,
    val servicesEnabled: Boolean = false,
    val location: Location? = null,
    val accuracyM: Float? = null,
    val isUpdating: Boolean = false,
    val error: String? = null
)
```

---

# 10. LOCATION ACCURACY

M3 should expose location accuracy to gameplay.

For example:

```text
GPS accuracy: 7 m
```

This allows the application to communicate uncertainty rather than pretending every GPS reading is exact.

M3 should define the project's accepted accuracy/degradation rules with the team.

---

# 11. CURRENT DISTANCE

For the active checkpoint:

```text
player location
       +
checkpoint coordinate
       ↓
distance in metres
```

The distance should be calculated using a geospatial distance method appropriate for latitude/longitude.

Example output:

```kotlin
distanceM: Float
```

The calculation should not depend on a hard-coded checkpoint.

---

# 12. DISTANCE CONTRACT

Suggested:

```kotlin
interface DistanceCalculator {
    fun distanceMeters(
        from: LatLng,
        to: LatLng
    ): Float
}
```

This makes distance calculation independently testable.

---

# 13. GEOFENCE PURPOSE

Geofencing is used to detect that the player has entered the configured physical area around a checkpoint.

Conceptually:

```text
Checkpoint
   ●
  (   )
 (     )  radiusM
  (   )
```

When the player enters the configured area:

```text
GEOFENCE_ENTER
```

the scan phase can become available.

---

# 14. DYNAMIC GEOFENCING

Geofences must be created dynamically from game data.

Conceptual flow:

```text
Joined game
   ↓
Load game checkpoints
   ↓
For each relevant checkpoint
   ↓
Create geofence using:
    latitude
    longitude
    radiusM
   ↓
Register geofences
```

Do not hard-code geofences for `R001–R006`.

---

# 15. GAME-SCOPED GEOFENCES

Geofence registrations must be associated with:

```text
gameId
checkpointId
```

Example internal identifier:

```text
gameId: demo-campus-quest
checkpointId: cp-001
```

This prevents a checkpoint from another game being treated as active.

---

# 16. GEOFENCE REGISTRATION STRATEGY

M3 should register only the geofences required by the current gameplay context where practical.

The exact strategy may be:

```text
all checkpoints in a manageable game
```

or:

```text
nearby/upcoming checkpoints
```

depending on platform constraints and project implementation.

The chosen strategy must remain game-scoped and dynamically derived.

---

# 17. GEOFENCE ENTER FLOW

Expected behavior:

```text
Player approaches checkpoint
        ↓
Geofence ENTER event
        ↓
Validate gameId/checkpointId
        ↓
Mark checkpoint as scan-eligible
        ↓
M4 receives scan availability
```

Geofence ENTER should not itself reveal the relic.

It is an **eligibility/proximity-area signal**.

---

# 18. GEOFENCE EXIT

Exit events may be useful for state management.

Example:

```text
GEOFENCE_EXIT
```

may cause the scan state to return to:

```text
OUTSIDE_AREA
```

depending on the agreed gameplay state machine.

Do not automatically erase valid discovery progress merely because the user later exits the area.

---

# 19. GEOFENCE FAILURE

Handle:

- Permission denied.
- Registration failure.
- Platform limitations.
- Location disabled.
- Invalid coordinates.
- Invalid radius.
- App/device restrictions.

A geofence failure should not crash the application.

Where possible, continuous location distance can provide a graceful fallback signal, subject to the final project contract.

---

# 20. LIFECYCLE MANAGEMENT

Location updates and sensors consume battery.

M3 must tie active collection to gameplay/lifecycle state.

Conceptually:

```text
Scan screen active
    ↓
Start required sensors/location
    ↓
Scan completed / screen left
    ↓
Stop unnecessary updates
```

Avoid leaving high-frequency sensors running throughout the entire application session.

---

# 21. LOCATION UPDATE RATE

The update frequency should be selected based on gameplay needs rather than using the fastest possible rate.

Consider:

- Battery.
- Required responsiveness.
- GPS accuracy.
- Device behavior.

Do not continuously request maximum-frequency updates without a gameplay reason.

---

# 22. AMBIENT LIGHT SENSOR

M3 owns Android ambient-light sensor access.

Conceptual flow:

```text
SensorManager
     ↓
TYPE_LIGHT
     ↓
lux reading
     ↓
LightSignatureMatcher
     ↓
light match score
```

The reading represents **ambient environmental light**.

---

# 23. LIGHT SIGNATURE

Each checkpoint contains:

```kotlin
LightSignature(
    minLux = ...,
    maxLux = ...
)
```

M3 compares the actual ambient reading against that range.

---

# 24. LIGHT MATCHING

Conceptual cases:

```text
actual lux inside expected range
        ↓
strong match

actual lux near expected range
        ↓
partial match

actual lux far outside range
        ↓
weak/no match
```

The exact scoring curve must follow the shared sensor-fusion specification.

M3 must not create inconsistent scoring rules in different screens.

---

# 25. LIGHT NORMALIZATION

The raw light-match result must be converted into the normalized fusion domain.

Conceptually:

```text
lightMatch ∈ [0, 1]
```

where:

```text
0 = no useful match
1 = strong match
```

The exact mathematical function belongs to M3's fusion implementation and shared technical specification.

---

# 26. LIGHT SENSOR UNAVAILABLE

Some devices may not have an ambient-light sensor.

M3 must expose:

```text
LIGHT_SENSOR_UNAVAILABLE
```

rather than crashing.

The team must agree whether the MVP:

1. Applies the documented degradation strategy, or
2. Blocks scan on unsupported hardware.

Do not silently change the fusion formula when a sensor is missing.

---

# 27. ACCELEROMETER

M3 owns accelerometer access.

Conceptual flow:

```text
SensorManager
      ↓
TYPE_ACCELEROMETER
      ↓
motion samples
      ↓
motion/sweep detector
      ↓
motion score
```

---

# 28. SWEEP GESTURE

The canonical motion type is:

```text
SWEEP
```

The detector should identify the intended movement pattern from accelerometer data.

M3 should isolate the algorithm:

```kotlin
interface MotionDetector {
    fun addSample(sample: AccelerometerSample)
    fun currentScore(): Float
    fun reset()
}
```

The exact detector implementation should be based on the project's agreed technical specification.

---

# 29. MOTION SCORE

Normalize motion evidence into:

```text
[0, 1]
```

Example:

```text
0.0 = no convincing sweep
0.5 = partial evidence
1.0 = strong sweep
```

Do not confuse motion score with the final fusion score.

---

# 30. PROXIMITY SENSOR

M3 owns the proximity sensor.

The proximity signal is used as the final physical confirmation.

Conceptually:

```text
Fusion threshold reached
        ↓
Proximity check
        ↓
Near/close condition satisfied?
        ↓
YES → final gate passes
NO  → remain in gated state
```

---

# 31. PROXIMITY IS NOT A FUSION INPUT

This is a critical architecture rule.

Do NOT implement:

```text
GPS × weight
+
Light × weight
+
Motion × weight
+
Proximity × weight
```

The canonical model is:

```text
GPS + Light + Motion
        ↓
Weighted Fusion
        ↓
Fusion Threshold
        ↓
Proximity Final Gate
```

---

# 32. PROXIMITY SENSOR LIMITATIONS

Different devices may expose different proximity behavior.

M3 must normalize the device-specific reading into a simple gameplay state:

```kotlin
enum class ProximityState {
    UNKNOWN,
    FAR,
    NEAR,
    UNAVAILABLE
}
```

The threshold should be based on the sensor's reported capabilities rather than assuming one universal raw distance.

---

# 33. SENSOR FUSION ENGINE

The fusion engine combines:

```text
GPS evidence
Light evidence
Motion evidence
```

into one score.

Conceptual interface:

```kotlin
data class FusionInput(
    val gpsScore: Float,
    val lightScore: Float,
    val motionScore: Float
)

data class FusionResult(
    val score: Float,
    val thresholdReached: Boolean
)
```

---

# 34. WEIGHTED FUSION

The canonical conceptual formula is:

```text
fusionScore =
    gpsWeight    × gpsScore
  + lightWeight  × lightScore
  + motionWeight × motionScore
```

The exact weights and threshold must come from the shared project technical specification.

Do not invent a different formula in the implementation.

---

# 35. NORMALIZED INPUTS

All weighted inputs should be normalized consistently:

```text
GPS score    ∈ [0,1]
Light score  ∈ [0,1]
Motion score ∈ [0,1]
```

Therefore the resulting fusion score can be consistently interpreted.

---

# 36. GPS SCORE

GPS score should represent how strongly the current physical position matches the checkpoint.

A conceptual relationship is:

```text
closer to checkpoint
       ↓
higher GPS evidence
```

The exact normalization function should follow the project's agreed technical specification.

Do not equate:

```text
distance < radius
```

directly with:

```text
gpsScore = 1
```

unless the specification explicitly defines it that way.

---

# 37. FUSION THRESHOLD

The weighted fusion score is compared with a defined threshold:

```text
fusionScore >= threshold
```

If true:

```text
FUSION_READY
```

If false:

```text
FUSION_INCOMPLETE
```

The threshold is separate from the proximity gate.

---

# 38. PROXIMITY FINAL GATE

After:

```text
fusionScore >= threshold
```

M3 evaluates proximity.

Only when the proximity condition is satisfied does the final physical evidence become:

```text
DISCOVERY_ELIGIBLE
```

M4 can then proceed with the reveal/gameplay state.

---

# 39. COMPLETE SENSOR STATE

M3 should expose a consolidated state similar to:

```kotlin
data class SensorFusionState(
    val gpsScore: Float = 0f,
    val lightScore: Float = 0f,
    val motionScore: Float = 0f,
    val fusionScore: Float = 0f,
    val fusionThresholdReached: Boolean = false,
    val proximityState: ProximityState = ProximityState.UNKNOWN,
    val finalGatePassed: Boolean = false,
    val lightAvailable: Boolean = true,
    val motionAvailable: Boolean = true,
    val proximityAvailable: Boolean = true
)
```

The exact shared model may differ.

---

# 40. SENSOR LIFECYCLE

Sensors should be registered only when needed.

Example:

```text
Scan starts
 ↓
register light listener
register accelerometer listener
register proximity listener
 ↓
collect evidence
 ↓
scan succeeds/fails/exits
 ↓
unregister listeners
```

This prevents unnecessary battery use.

---

# 41. RESET BETWEEN CHECKPOINTS

Sensor state must not leak between checkpoints.

When switching:

```text
Game A / Checkpoint 1
        ↓
Game A / Checkpoint 2
```

reset:

- Motion detector.
- Accumulated fusion state where appropriate.
- Proximity gate state.
- Temporary scan buffers.

Never use evidence from one checkpoint as evidence for another.

---

# 42. RESET BETWEEN GAMES

When switching:

```text
Game A
 ↓
Game B
```

all game-specific location/sensor state must be re-scoped.

The system must not use Game A's checkpoint configuration while scanning Game B.

---

# 43. GAME-SCOPED SENSOR CONTEXT

The active sensor context should identify:

```text
gameId
checkpointId
```

Example:

```kotlin
data class ActiveCheckpointContext(
    val gameId: String,
    val checkpointId: String
)
```

This makes accidental cross-game sensor use easier to detect.

---

# 44. LOCATION + SENSOR ORCHESTRATION

M3 may provide a coordinator such as:

```kotlin
class DiscoverySignalCoordinator
```

Its responsibility is to combine:

```text
LocationProvider
LightSensor
MotionDetector
ProximitySensor
FusionEngine
```

It should not own UI rendering.

---

# 45. UI BOUNDARY WITH M4

M4 needs a clean signal state.

M3 should expose values such as:

```text
GPS status
distance
GPS score
light status
light score
motion status
motion score
fusion percentage
fusion threshold status
proximity status
final gate status
```

M4 converts these into the scan HUD.

---

# 46. M4 MUST NOT ACCESS SENSORS DIRECTLY

M4 should not contain:

```kotlin
SensorManager
FusedLocationProviderClient
LocationServices
GeofencingClient
```

M4 consumes M3's state/contract.

This prevents UI code from becoming coupled to hardware.

---

# 47. CREATOR CONFIGURATION BOUNDARY

M2 creates the checkpoint configuration:

```text
lat
lng
radiusM
minLux
maxLux
motionType
```

M3 consumes it.

Therefore:

```text
M2 = configuration
M3 = physical interpretation
```

---

# 48. REPOSITORY BOUNDARY

M3 should obtain checkpoint configuration through the repository/use-case layer.

Do not directly query Firestore from sensor code.

Conceptually:

```text
M3 coordinator
    ↓
GameRepository
    ↓
checkpoint configuration
```

The repository implementation is owned by M5/M6.

---

# 49. OFFLINE LOCATION/SENSOR OPERATION

Sensor calculations themselves can occur locally.

The app does not need a network connection to calculate:

- GPS distance.
- Light score.
- Motion score.
- Fusion score.
- Proximity state.

The checkpoint configuration must already be locally available for offline gameplay.

M6 owns the local cache.

---

# 50. STALE CONFIGURATION

M3 should not silently use stale checkpoint configuration if the repository exposes a version/updated timestamp and indicates that the local configuration is invalid.

The exact stale-data policy belongs to the shared offline contract.

---

# 51. SENSOR DATA PRIVACY

Sensor readings should be used for the gameplay purpose.

Do not persist continuous raw sensor streams unless explicitly required.

Prefer:

```text
raw sensor sample
   ↓
derived score/state
   ↓
discard raw sample
```

This also reduces storage and processing overhead.

---

# 52. PERFORMANCE

M3 should avoid unnecessary processing.

For accelerometer:

- Sample at an appropriate rate.
- Process only during active scan.
- Use a bounded buffer/window.
- Reset after scan.

For light:

- Do not process more readings than needed.

For location:

- Use a gameplay-appropriate update interval.

For proximity:

- Listen only during relevant gate/scan state where possible.

---

# 53. BATTERY MANAGEMENT

The application should avoid:

```text
GPS always on
+
all sensors always on
+
all geofences always active
```

throughout the entire session.

Prefer:

```text
Normal gameplay
   ↓
Low-cost location/geofence monitoring
   ↓
Checkpoint entered
   ↓
Active sensor fusion
   ↓
Discovery / exit
   ↓
Stop active sensor collection
```

---

# 54. BACKGROUND BEHAVIOR

M3 must account for Android lifecycle/background limitations.

The implementation should define what happens when:

- App goes to background.
- Screen locks.
- User returns to app.
- Scan is interrupted.
- Activity/fragment is recreated.

Do not assume sensor listeners survive arbitrary lifecycle transitions.

---

# 55. INTERRUPTION

If the player leaves the scan screen:

```text
stop or suspend active sensor collection
```

according to the agreed gameplay behavior.

When returning:

```text
restore active checkpoint context
reinitialize required sensors
```

without carrying invalid transient sensor evidence.

---

# 56. DEVICE CAPABILITY MATRIX

Test at least conceptually against:

| Capability | Supported | Unsupported |
|---|---|---|
| GPS | Normal scan | Graceful error/degradation |
| Light sensor | Light scoring | Defined fallback |
| Accelerometer | Motion scoring | Defined fallback |
| Proximity | Final gate | Defined fallback |
| Location services | Active | User guidance |
| Permission | Granted | Permission UI |

The MVP policy for unsupported sensors must be explicitly documented rather than inferred during implementation.

---

# 57. ERROR STATES

M3 should expose structured errors such as:

```text
LOCATION_PERMISSION_REQUIRED
LOCATION_SERVICES_DISABLED
LOCATION_UNAVAILABLE
GEOFENCE_REGISTRATION_FAILED
LIGHT_SENSOR_UNAVAILABLE
MOTION_SENSOR_UNAVAILABLE
PROXIMITY_SENSOR_UNAVAILABLE
INVALID_CHECKPOINT_CONFIGURATION
```

M4/M1 can map these to user-facing messages.

---

# 58. SECURITY / TRUST MODEL

Sensor fusion provides evidence that the user is physically interacting with a checkpoint.

It is not a complete anti-cheat system.

Do not claim:

```text
GPS + sensors = impossible to cheat
```

The backend remains authoritative for progress recording.

M5/M6 should ensure discovery records are scoped correctly and written idempotently.

---

# 59. TESTABLE ABSTRACTIONS

Use abstractions so hardware is mockable.

Suggested:

```kotlin
interface LocationProvider
interface LightSensorProvider
interface MotionSensorProvider
interface ProximitySensorProvider
interface DistanceCalculator
interface MotionDetector
interface LightSignatureMatcher
interface FusionEngine
```

The exact interfaces can be consolidated if the project architecture prefers fewer types.

---

# 60. FAKE LOCATION PROVIDER

For tests:

```kotlin
class FakeLocationProvider
```

should allow controlled locations:

```text
100 m away
50 m away
20 m away
5 m away
```

This allows deterministic GPS/fusion tests.

---

# 61. FAKE LIGHT SENSOR

Example test values:

```text
expected: 100–300 lux

actual: 200 → strong match
actual: 110 → strong/near match
actual: 500 → weak/no match
```

The expected score must follow the canonical matcher.

---

# 62. FAKE MOTION SENSOR

Feed controlled accelerometer samples representing:

```text
no movement
random movement
partial sweep
valid sweep
```

Verify that the motion detector produces the expected normalized result.

---

# 63. FAKE PROXIMITY SENSOR

Test:

```text
UNKNOWN
FAR
NEAR
UNAVAILABLE
```

Verify that only the defined `NEAR` condition passes the final gate.

---

# 64. FUSION ENGINE UNIT TESTS

Test:

```text
all strong → threshold reached
all weak → threshold not reached
GPS strong + light weak + motion strong
GPS weak + light strong + motion strong
boundary at exact threshold
just below threshold
just above threshold
```

Do not test only the happy path.

---

# 65. PROXIMITY GATE UNIT TESTS

Test:

```text
fusion below threshold + near
    → FAIL

fusion above threshold + far
    → FAIL

fusion above threshold + near
    → PASS

fusion exactly threshold + near
    → PASS if threshold is inclusive
```

The comparison operator must match the technical specification.

---

# 66. CROSS-CHECKPOINT TEST

Verify:

```text
Checkpoint A evidence
        ↓
switch to B
        ↓
A evidence does not contribute to B
```

This is a critical isolation test.

---

# 67. CROSS-GAME TEST

Verify:

```text
Game A / CP-A1
Game B / CP-B1
```

and ensure that:

```text
CP-A1 configuration
```

cannot be accidentally used for:

```text
CP-B1 scan
```

---

# 68. GEOFENCE TESTS

Test:

```text
valid checkpoint → registration succeeds
invalid coordinate → rejected
invalid radius → rejected
ENTER → correct checkpoint identified
ENTER → correct game identified
EXIT → appropriate state
multiple checkpoints → correct mapping
```

---

# 69. LOCATION DISTANCE TESTS

Test known coordinate pairs.

Include:

- Same point.
- Very short distance.
- Medium distance.
- Larger distance.
- Latitude/longitude boundary cases relevant to supported campus geography.

---

# 70. SENSOR LIFECYCLE TESTS

Verify:

```text
scan starts → listeners registered
scan exits → listeners removed
scan succeeds → listeners removed
scan fails → listeners removed
screen recreated → no duplicate listeners
```

Duplicate listeners can cause incorrect scoring and battery drain.

---

# 71. ACCEPTANCE TEST — APPROACH

### Given

A player has joined a game.

### When

The player approaches a checkpoint.

### Then

The system calculates current distance and detects entry into the configured geofence when the platform reports it.

---

# 72. ACCEPTANCE TEST — SENSOR FUSION

### Given

The player is inside the appropriate checkpoint area.

### When

GPS, light, and motion evidence satisfy the configured fusion threshold.

### Then

The system reports:

```text
fusionThresholdReached = true
```

but does not reveal the checkpoint yet if the proximity final gate has not passed.

---

# 73. ACCEPTANCE TEST — PROXIMITY GATE

### Given

Fusion threshold has been reached.

### When

Proximity changes to the required near condition.

### Then

The final gate becomes:

```text
finalGatePassed = true
```

and M4 can continue the reveal flow.

---

# 74. ACCEPTANCE TEST — PROXIMITY FAILURE

### Given

Fusion threshold has been reached.

### When

The proximity condition remains unsatisfied.

### Then

The final gate remains blocked.

---

# 75. ACCEPTANCE TEST — SENSOR UNAVAILABLE

### Given

A device lacks a required sensor.

### When

The player starts a scan.

### Then

The application follows the documented MVP degradation/blocking behavior and does not crash.

---

# 76. ACCEPTANCE TEST — LOCATION PERMISSION

### Given

Location permission is denied.

### When

The player starts a location-dependent gameplay action.

### Then

The application requests/communicates the required permission state rather than failing silently.

---

# 77. ACCEPTANCE TEST — DYNAMIC CHECKPOINT

### Given

A creator creates a new checkpoint with:

```text
lat
lng
radius
light signature
motion type
```

### When

A player joins the published game.

### Then

M3 uses that checkpoint configuration dynamically without requiring a code change.

---

# 78. ACCEPTANCE TEST — NO HARDCODED CHECKPOINTS

Create a game with:

```text
2 checkpoints
```

and another with:

```text
8 checkpoints
```

Verify that location/geofence setup works for both.

---

# 79. M3 → M4 HANDOFF

M3 provides:

```text
active checkpoint
distance
gpsScore
lightScore
motionScore
fusionScore
fusionThresholdReached
proximityState
finalGatePassed
sensor availability
location availability
error state
```

M4 uses these to render the scan experience.

---

# 80. M3 → M2 HANDOFF

M3 provides creator-side integration requirements for:

```text
latitude
longitude
radiusM
minLux
maxLux
motionType
```

If a configuration value has constraints, document them for M2.

---

# 81. M3 → M5 HANDOFF

M3 informs M5 about:

- Required checkpoint fields.
- Game/checkpoint IDs used by discovery.
- Sensor-derived discovery state that may be recorded.
- Any backend validation assumptions.

M3 does not write Firestore directly.

---

# 82. M3 → M6 HANDOFF

M3 informs M6 about any locally required:

```text
checkpoint configuration
game-scoped active state
pending discovery result
```

M6 decides the Room schema and synchronization implementation.

---

# 83. SUGGESTED PACKAGE STRUCTURE

A possible structure:

```text
location/
    LocationProvider.kt
    FusedLocationProviderImpl.kt
    DistanceCalculator.kt
    GeofenceManager.kt

sensor/
    LightSensorProvider.kt
    AccelerometerProvider.kt
    ProximitySensorProvider.kt
    MotionDetector.kt
    LightSignatureMatcher.kt

fusion/
    FusionEngine.kt
    FusionModels.kt
    DiscoverySignalCoordinator.kt

permission/
    LocationPermissionManager.kt
```

Adapt names to the project's existing package conventions.

---

# 84. NO UI BUSINESS LOGIC IN SENSOR CLASSES

Avoid:

```kotlin
sensorClass.showToast(...)
sensorClass.navigate(...)
sensorClass.showDialog(...)
```

Sensor classes should expose state/results.

M4/M1 decide how those states are presented.

---

# 85. NO FIREBASE IN SENSOR CLASSES

Avoid:

```kotlin
FirebaseFirestore.getInstance()
```

inside:

```text
FusionEngine
LightSensorProvider
MotionDetector
GeofenceManager
```

Persistence belongs behind the repository boundary.

---

# 86. NO ROOM IN SENSOR CLASSES

Similarly, sensor/location components must not depend directly on:

```text
RoomDatabase
DAO
@Entity
```

Local persistence belongs to M6.

---

# 87. GIT WORKFLOW

Branch:

```text
feature/m3-location-sensors-fusion
```

Commit examples:

```text
feat(location): add fused location provider wrapper
feat(location): add distance calculator
feat(location): add dynamic geofence manager
feat(sensor): add ambient light provider
feat(sensor): add motion detector
feat(sensor): add proximity provider
feat(fusion): add normalized signal models
feat(fusion): add weighted fusion engine
feat(fusion): add proximity final gate
test(location): add distance tests
test(fusion): add fusion threshold tests
test(sensor): add motion and light tests
```

Keep unrelated UI/Firebase/Room changes out of M3 commits.

---

# 88. CHANGE CONTROL

Before modifying:

- Fusion weights.
- Fusion threshold.
- Normalization.
- Sensor contracts.
- Location contracts.
- Repository contracts.

inform the team and update the shared technical documentation.

These values directly affect gameplay behavior.

---

# 89. DEFINITION OF DONE

M3 is complete when:

### Location

- Fused Location Provider is integrated.
- Permissions are handled.
- Current location is exposed.
- Distance calculation works.
- Accuracy is exposed.
- Lifecycle is handled.

### Geofencing

- Geofences are dynamically created from checkpoint data.
- Geofences are game/checkpoint scoped.
- ENTER events identify the correct checkpoint.
- Failures are handled.

### Sensors

- Ambient light is available to the gameplay layer.
- Accelerometer motion is available.
- Proximity is available where supported.
- Unsupported sensors are handled according to the agreed MVP policy.

### Fusion

- GPS evidence is normalized.
- Light evidence is normalized.
- Motion evidence is normalized.
- Weighted fusion is implemented.
- Threshold evaluation is implemented.
- Proximity is a separate final gate.

### Architecture

- Hardware APIs are isolated behind testable abstractions.
- M4 does not directly access hardware.
- M3 does not directly implement Room/Firebase.
- Dynamic checkpoint configuration is supported.

### Testing

- Unit tests pass.
- Fusion boundary tests pass.
- Geofence tests pass.
- Sensor lifecycle tests pass.
- Cross-game/checkpoint isolation tests pass.

---

# 90. LEGACY CLEANUP

Remove or migrate code that assumes:

```text
R001 → fixed coordinate
R002 → fixed coordinate
...
R006 → fixed coordinate
```

The production location system must consume:

```text
Game
  ↓
Checkpoint[]
  ↓
lat/lng/radius
```

R001–R006 can remain in seed/demo data only.

---

# 91. FINAL M3 ARCHITECTURE

The physical-discovery pipeline is:

```text
Firestore / Room
      ↓
GameRepository
      ↓
Checkpoint configuration
      ↓
┌───────────────────────────────┐
│ M3 Physical Discovery Layer   │
│                               │
│ Location                      │
│ Geofence                      │
│ Light                         │
│ Accelerometer                 │
│ Proximity                     │
│ Distance                      │
│ Motion detection              │
│ Light matching                │
│ Normalization                 │
│ Weighted fusion               │
│ Proximity final gate          │
└───────────────────────────────┘
      ↓
DiscoverySignalState
      ↓
M4 Quest / Scan UI
      ↓
Reveal
```

---

# 92. CORE DESIGN PRINCIPLE

M3 should provide **reliable physical evidence**, not UI and not persistence.

The canonical decision chain is:

```text
GPS evidence
      +
Light evidence
      +
Motion evidence
      ↓
Weighted Fusion
      ↓
Fusion Threshold
      ↓
Proximity Final Gate
      ↓
Discovery Eligible
```

Never change this to:

```text
GPS + Light + Motion + Proximity
```

as four weighted signals.

The separation between **fusion** and **final proximity confirmation** is a core Campus Quest mechanic and must remain consistent across implementation, testing, UI, and documentation.

---

# 93. M3 QUICK CHECKLIST

```text
[ ] Fused Location Provider
[ ] Location permission
[ ] Location services state
[ ] Current location
[ ] Location accuracy
[ ] Distance calculator
[ ] Dynamic geofences
[ ] Game-scoped geofences
[ ] Checkpoint-scoped geofences
[ ] Geofence ENTER
[ ] Geofence EXIT handling
[ ] Geofence error handling
[ ] Ambient light provider
[ ] Light signature matcher
[ ] Light normalization
[ ] Accelerometer provider
[ ] Motion/sweep detector
[ ] Motion normalization
[ ] Proximity provider
[ ] Proximity state
[ ] GPS normalization
[ ] Weighted fusion
[ ] Fusion threshold
[ ] Separate proximity final gate
[ ] Lifecycle management
[ ] Battery management
[ ] Sensor availability handling
[ ] Cross-game isolation
[ ] Cross-checkpoint isolation
[ ] Unit tests
[ ] Integration tests
[ ] M4 handoff
[ ] M5/M6 integration
[ ] Final documentation update
```

---

# 94. FINAL HANDOFF PACKAGE

M3 should provide:

1. Location provider abstraction.
2. Geofence manager.
3. Distance calculator.
4. Light sensor abstraction.
5. Light signature matcher.
6. Accelerometer abstraction.
7. Motion/sweep detector.
8. Proximity abstraction.
9. Fusion engine.
10. Proximity final-gate logic.
11. Discovery signal state/contract.
12. Unit tests.
13. Hardware/integration test notes.
14. Sensor availability/degradation rules.
15. Lifecycle/battery notes.
16. Integration instructions for M4.
17. Any shared-contract changes.

---

**END OF MEMBER 3 LOCATION, GEOFENCING, SENSORS & FUSION WORKPLAN**
