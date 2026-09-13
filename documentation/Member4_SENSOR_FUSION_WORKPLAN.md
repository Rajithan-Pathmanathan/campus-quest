# M4 — SENSOR FUSION WORKPLAN
## Campus Quest — Mobile Application Development

**Document ID:** M4-SENSOR-FUSION  
**Owner:** M4 — Device Developer: Sensor Fusion  
**Project:** Campus Quest  
**Primary responsibility:** Accelerometer + light + proximity sensing, fusion computation, sensor availability, graceful degradation, lifecycle/battery handling, and handoff of scan-state results to the UI layer.

---

# 1. Purpose

This document defines exactly what M4 must build, what M4 must not build, how M4 connects to the other five team members, how sensor data becomes a fusion score, how missing sensors are handled, and how the work is tested and integrated.

The implementation must support the project's central mechanic:

```text
M3 GPS / distance
       ↓
enter relic geofence
       ↓
M2 opens Scan Mode
       ↓
M4 reads available sensors
       ↓
GPS proximity + light match + accelerometer motion
       ↓
weighted fusion score
       ↓
proximity sensor final confirmation
       ↓
M2 reveals relic/lore
       ↓
M6 persists locally
       ↓
M5 syncs cloud progress
```

The important architectural rule is:

> **GPS distance, light-signature matching, and accelerometer scanning motion contribute to the fused score. The proximity sensor is a separate final confirmation gate and is NOT part of the fusion score.**

M4 owns the sensor-fusion logic. M4 does not own the map, geofencing, Firebase, Room, or the visual design of Scan Mode.

---

# 2. Source Basis and Project Alignment

The project materials describe a sensor-fusion based relic discovery experience rather than three independent sensor mini-games. The architecture follows the project's MVVM/repository separation: device-facing logic should remain separated from UI logic, while the ViewModel/UI consumes a stable result.

This document therefore treats the following as the project-level boundaries:

- M3 supplies location/distance information.
- M4 supplies sensor measurements and fusion results.
- M2 displays the scan experience.
- M5 owns Firebase/cloud persistence.
- M6 owns Room/local persistence and integration coordination.
- M1 owns the overall application shell/navigation/theme.

Where this document gives exact weights, thresholds, timing values, or implementation names that are not fixed by the project materials, they are explicitly marked **provisional** and must be calibrated/agreed before final use.

---

# 3. M4 Role

## 3.1 Primary responsibility

M4 is responsible for:

1. Accessing Android sensors through `SensorManager`.
2. Detecting whether required sensors exist.
3. Registering/unregistering listeners safely.
4. Reading light sensor values in lux.
5. Reading accelerometer values.
6. Classifying the intended slow scanning motion.
7. Receiving GPS distance/proximity information from M3.
8. Normalizing each available signal.
9. Combining available signals into one fusion score.
10. Reweighting the score when a sensor is unavailable.
11. Exposing a stable result to M2/ViewModel.
12. Handling proximity as a final gate.
13. Avoiding unnecessary sensor usage outside Scan Mode.
14. Testing sensor behavior on at least two Android devices/emulators where possible.
15. Calibrating the final parameters using real campus measurements.

## 3.2 M4 does not own

M4 must not independently implement:

- Google Maps.
- Fused Location Provider.
- Geofence creation.
- Firebase Authentication.
- Firestore persistence.
- Room database schema.
- Bottom navigation.
- App-wide navigation.
- Final screen design.
- Leaderboard business logic.
- User profile logic.

If M4 needs location information, M3 supplies it through the agreed interface.

---

# 4. Core Sensor Model

Campus Quest uses three sensor-related mechanisms plus GPS:

| Signal | Owner | Role |
|---|---|---|
| GPS distance | M3 | Fused-score input |
| Light sensor | M4 | Fused-score input |
| Accelerometer | M4 | Fused-score input |
| Proximity sensor | M4 | Final reveal gate |

The distinction between the last two is critical.

### Fusion inputs

```text
GPS proximity
Light match
Motion match
      ↓
weighted fusion
      ↓
FusionResult.score
```

### Final gate

```text
FusionResult.score >= reveal threshold
             AND
proximity confirms close range
             ↓
       reveal allowed
```

The proximity sensor must not silently increase or decrease the percentage shown by the fusion meter.

---

# 5. Conceptual Architecture

```text
                 ┌──────────────────────┐
                 │ M3 Location Provider  │
                 │ distance to relic     │
                 └──────────┬───────────┘
                            │
                            ▼
┌────────────────────────────────────────────────┐
│                 M4 Sensor Layer                │
│                                                │
│ SensorManager                                  │
│ ├── Light Sensor                                │
│ ├── Accelerometer                               │
│ └── Proximity Sensor                            │
└──────────────────┬─────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────┐
│             SensorFusionEngine                 │
│                                                │
│ light normalization                            │
│ motion classification                          │
│ GPS normalization                               │
│ availability detection                         │
│ weight reallocation                            │
│ combined score                                 │
└──────────────────┬─────────────────────────────┘
                   │
                   ▼
              FusionResult
                   │
             ┌─────┴─────┐
             ▼           ▼
        M2 Scan UI   Proximity Gate
                         │
                         ▼
                   Reveal Decision
```

---

# 6. Android Sensor Ownership

## 6.1 SensorManager

M4 should centralize Android sensor access rather than allowing multiple UI screens to independently register listeners.

Conceptually:

```kotlin
class DeviceSensorManager(
    private val context: Context
) {
    // sensor discovery
    // listener registration
    // listener cleanup
}
```

The exact class name is an implementation decision, but the responsibility must remain centralized.

## 6.2 Sensor discovery

At initialization:

```text
SensorManager
   ↓
getDefaultSensor(TYPE_LIGHT)
getDefaultSensor(TYPE_ACCELEROMETER)
getDefaultSensor(TYPE_PROXIMITY)
```

Each result must be checked for null.

Do not assume that every Android device has every sensor.

---

# 7. SensorAvailability

Use a conceptual availability model:

```kotlin
data class SensorAvailability(
    val lightAvailable: Boolean,
    val accelerometerAvailable: Boolean,
    val proximityAvailable: Boolean
)
```

This model is useful to:

- decide which listeners to register;
- support graceful degradation;
- report diagnostic information;
- test sensor combinations;
- avoid crashes on devices without a specific sensor.

Example:

```text
light = true
accelerometer = true
proximity = true
```

Normal physical-device configuration.

Another possible configuration:

```text
light = false
accelerometer = true
proximity = true
```

The app must not crash or become permanently unusable because the light sensor is missing.

---

# 8. Sensor Lifecycle

Sensor listeners should be active only when needed.

Preferred lifecycle:

```text
Map / Quest List
     ↓
No scan
     ↓
No continuous sensor listeners
     ↓
Geofence entered
     ↓
Scan Mode starts
     ↓
Register required sensors
     ↓
Collect readings
     ↓
Scan ends / leaves screen / lifecycle stops
     ↓
Unregister listeners
```

This reduces unnecessary battery consumption.

M4 must not leave accelerometer and light listeners running for the entire lifetime of the application.

## 8.1 Registration

When Scan Mode becomes active:

```text
register light listener
register accelerometer listener
register proximity listener
```

Only register sensors that actually exist.

## 8.2 Unregistration

When Scan Mode ends:

```text
unregister light listener
unregister accelerometer listener
unregister proximity listener
```

Also clean up when the owning lifecycle reaches the appropriate stopped/destroyed state.

## 8.3 Duplicate registration prevention

The implementation must avoid:

```text
register
register
register
```

for the same active scan.

Use an internal state such as:

```text
sensorsRegistered = true / false
```

or an equivalent lifecycle-safe mechanism.

---

# 9. Light Sensor

## 9.1 Input

Android's light sensor provides an ambient light value, normally represented in lux.

M4 reads the current value:

```text
currentLux
```

The relic provides an expected light signature:

```kotlin
data class LightSignature(
    val minLux: Float,
    val maxLux: Float
)
```

## 9.2 Canonical relic examples

The development catalog defines the following example signatures:

| Relic | Light range |
|---|---:|
| R001 Founder’s Bell | 180–320 lux |
| R002 Scholar’s Compass | 250–450 lux |
| R003 Heritage Key | 80–180 lux |
| R004 Old Library Seal | 400–650 lux |
| R005 Garden Chronicle | 120–250 lux |
| R006 Clock Tower Relic | 300–500 lux |

These are development values and must be physically validated before final demonstration.

## 9.3 Light matching

Conceptually:

```text
if currentLux inside expected range:
    lightScore = high
else:
    lightScore decreases according to distance from expected range
```

A simple initial development implementation may use a bounded score:

```text
inside range → 1.0

outside range:
distance from nearest boundary increases
→ score approaches 0
```

Do not hard-code the exact decay curve as a final requirement without M4 calibration.

## 9.4 Example

For R001:

```text
expected: 180–320 lux
current: 250 lux
```

The value is inside the expected range, so the light-match score can be treated as a strong match.

For:

```text
current: 100 lux
```

the value is outside the expected range and should contribute a weaker score.

---

# 10. Accelerometer

## 10.1 Purpose

The accelerometer detects the user's intended scanning movement.

The project mechanic describes a slow side-to-side sweep rather than arbitrary shaking.

M4 should therefore classify motion rather than simply checking whether the phone is moving.

## 10.2 Raw input

Typical accelerometer values:

```text
x
y
z
```

The exact axis used for scan classification depends on device orientation and implementation.

M4 must document the selected interpretation.

## 10.3 Motion preprocessing

A practical pipeline is:

```text
raw accelerometer
      ↓
magnitude / axis analysis
      ↓
noise filtering
      ↓
movement window
      ↓
slow sweep detection
      ↓
motion score
```

The implementation should avoid requiring a perfectly identical movement pattern from every user.

## 10.4 Motion score

Conceptually:

```text
no meaningful motion → low
random/sharp movement → low/medium
controlled side-to-side sweep → high
```

The exact thresholds must be determined through physical testing.

## 10.5 Avoid overfitting

Do not make the motion detector so strict that:

- a slightly different phone orientation fails;
- a different device fails;
- normal hand movement permanently produces zero;
- the user must reproduce an exact waveform.

The mechanic should feel like a controlled scan, not a laboratory measurement.

---

# 11. GPS Distance Input

M4 consumes the GPS distance calculated by M3.

Example interface:

```kotlin
data class LocationSignal(
    val distanceMeters: Float,
    val accuracyMeters: Float?
)
```

The actual shared contract can use a different name, but the meaning must remain clear.

M4 must not:

- create another Fused Location Provider instance;
- calculate an independent distance using a second location pipeline;
- create a second geofence;
- introduce conflicting proximity rules.

There must be one authoritative location implementation owned by M3.

---

# 12. GPS Normalization

The fusion engine needs a normalized GPS score.

Conceptually:

```text
far from relic
    ↓
low GPS score

inside approach range
    ↓
higher GPS score

very close
    ↓
high GPS score
```

The exact function depends on the project radius and calibration.

A simple initial approach may map distance against the relic's configured radius:

```text
distance >= radius → 0
distance <= 0      → 1
between            → normalized proportion
```

This is an initial development model, not a final calibrated requirement.

Accuracy should also be considered.

Example:

```text
distance = 8m
GPS accuracy = ±35m
```

should not be interpreted with the same confidence as:

```text
distance = 8m
GPS accuracy = ±3m
```

M3 remains responsible for authoritative location data and geofence behavior.

---

# 13. Fusion Formula

The fusion engine combines available score components.

Conceptual formula:

```text
Fusion Score =
    GPS Score    × GPS Weight
  + Light Score  × Light Weight
  + Motion Score × Motion Weight
```

All active component scores should be normalized to:

```text
0.0 → 1.0
```

The final UI value can then be converted to:

```text
0 → 100%
```

## 13.1 Initial development example

An initial development example could be:

```text
GPS       = 0.40
Light     = 0.35
Motion    = 0.25
```

Therefore:

```text
score =
    gpsScore    * 0.40
  + lightScore  * 0.35
  + motionScore * 0.25
```

**These weights are provisional.**

They are not a fixed requirement from the project materials. M4 must test and calibrate them on actual devices and campus conditions, then communicate the final agreed values through the shared contract.

---

# 14. Graceful Sensor Degradation

This is a required design behavior.

If a sensor is unavailable, the application should not automatically make the relic impossible to discover.

Example:

```text
GPS       available
Light     unavailable
Motion    available
```

The available weights should be renormalized.

If the initial weights are:

```text
GPS       0.40
Light     0.35
Motion    0.25
```

and Light is unavailable:

```text
available total = 0.40 + 0.25 = 0.65
```

Then:

```text
GPS effective weight
= 0.40 / 0.65

Motion effective weight
= 0.25 / 0.65
```

The resulting active weights sum to 1.0.

Conceptually:

```kotlin
val activeWeightTotal = availableWeights.sum()

normalizedWeight =
    originalWeight / activeWeightTotal
```

## 14.1 If only one fusion input remains

If only GPS remains:

```text
Fusion Score = GPS Score
```

The app should continue to function, but M2 may optionally communicate reduced scan fidelity if the shared UX contract calls for it.

## 14.2 Proximity unavailable

Proximity is different because it is the final reveal gate.

If proximity hardware is unavailable, the team must not silently treat it as detected.

The final behavior must be agreed by the team and documented in the shared contract.

A safe development policy is:

```text
proximity unavailable
      ↓
do not falsely claim proximity confirmation
      ↓
use an explicitly agreed fallback for demonstration/testing
```

If the academic requirement requires physical proximity confirmation, a device without the sensor cannot be used to demonstrate that portion.

---

# 15. Proximity Sensor — Final Gate

## 15.1 Critical rule

> **Proximity is NOT part of the fusion score.**

It does not receive a percentage weight.

It does not increase the fusion meter.

It does not reduce the fusion meter.

It only determines whether the final reveal may happen after the fusion condition is satisfied.

## 15.2 Conceptual state

```text
fusionScore = 86%

proximity = not close
→ reveal blocked
```

Then:

```text
fusionScore = 86%

proximity = close
→ reveal allowed
```

## 15.3 Final decision

Conceptually:

```kotlin
revealAllowed =
    fusionScore >= revealThreshold &&
    proximityConfirmed
```

The exact threshold is provisional until calibration.

## 15.4 Why separate it

This separation prevents an accidental implementation where proximity becomes another weighted signal.

Correct:

```text
[GPS + Light + Motion] → percentage
                         ↓
                  threshold reached?
                         ↓
                  proximity gate
                         ↓
                      reveal
```

Incorrect:

```text
GPS + Light + Motion + Proximity
              ↓
        weighted percentage
```

---

# 16. Conceptual SensorFusionEngine Interface

A possible interface:

```kotlin
interface SensorFusionEngine {

    fun startScan(
        relic: Relic,
        locationInput: LocationSignal
    )

    fun stopScan()

    fun updateLocation(
        locationInput: LocationSignal
    )

    fun getCurrentResult(): FusionResult

    fun observeResult(): Flow<FusionResult>
}
```

The exact API can be changed during implementation, but it must provide the same conceptual responsibilities.

---

# 17. FusionResult

Suggested conceptual model:

```kotlin
data class FusionResult(
    val score: Float,
    val gpsScore: Float?,
    val lightScore: Float?,
    val motionScore: Float?,
    val proximityConfirmed: Boolean,
    val availability: SensorAvailability,
    val revealAllowed: Boolean
)
```

Interpretation:

- `score` — fused percentage basis, excluding proximity.
- `gpsScore` — normalized location contribution.
- `lightScore` — normalized light contribution.
- `motionScore` — normalized motion contribution.
- `proximityConfirmed` — final gate state.
- `availability` — sensor availability snapshot.
- `revealAllowed` — final decision.

The nullable component scores are useful because a missing sensor should not be represented as a fake zero measurement.

---

# 18. ScanState

A conceptual state model:

```kotlin
sealed interface ScanState {
    data object Idle : ScanState
    data object Starting : ScanState
    data class Scanning(
        val result: FusionResult
    ) : ScanState
    data class ReadyToReveal(
        val result: FusionResult
    ) : ScanState
    data class Completed(
        val relicId: String
    ) : ScanState
    data class Error(
        val message: String
    ) : ScanState
}
```

The exact Kotlin representation is flexible.

The important states are:

```text
Idle
Starting
Scanning
ReadyToReveal
Completed
Error
```

M2 should not need to know how raw Android sensor events work.

---

# 19. Data Flow During One Scan

Example for R001:

```text
R001 selected
      ↓
M3 reports distance
      ↓
Scan Mode starts
      ↓
M4 discovers sensors
      ↓
Light sensor → current lux
Accelerometer → movement classification
Proximity → close/not close
      ↓
M4 normalizes GPS/light/motion
      ↓
M4 calculates fusion score
      ↓
M2 observes FusionResult
      ↓
meter updates
      ↓
fusion threshold reached
      ↓
proximity checked
      ↓
proximity confirmed
      ↓
revealAllowed = true
      ↓
M2 displays relic reveal
```

---

# 20. R001 Mock Scenario

R001:

```text
id: R001
name: Founder’s Bell
radius: 25m
light: 180–320 lux
```

Mock inputs:

```text
GPS distance: 8m
Light: 250 lux
Motion: controlled sweep
Proximity: close
```

Expected conceptual behavior:

```text
GPS score → high
Light score → high
Motion score → high

Fusion score → high

Proximity → confirmed

Reveal → allowed
```

The exact resulting percentage depends on the final normalization, weights, smoothing, and calibration.

Do not write tests that depend on an arbitrary exact final percentage unless the team has frozen the algorithm.

---

# 21. Mock-First Development

M4 must not wait for M3's real GPS implementation before developing the fusion engine.

Create a mock input source.

Example:

```kotlin
data class MockFusionInput(
    val distanceMeters: Float,
    val lux: Float,
    val motionScore: Float,
    val proximityConfirmed: Boolean
)
```

This allows M4 to test:

```text
R001 + 8m + 250 lux + good motion + proximity
```

before hardware integration.

---

# 22. Required Mock Scenarios

At minimum implement these development scenarios:

### Scenario A — Strong match

```text
distance = close
lux = inside signature
motion = good
proximity = true
```

Expected:

```text
high score
reveal allowed
```

### Scenario B — Far away

```text
distance = far
lux = matching
motion = good
proximity = true
```

Expected:

```text
low/reduced score
reveal blocked
```

### Scenario C — Wrong light

```text
distance = close
lux = outside signature
motion = good
proximity = true
```

Expected:

```text
reduced score
```

### Scenario D — Wrong motion

```text
distance = close
lux = matching
motion = poor
proximity = true
```

Expected:

```text
reduced score
```

### Scenario E — Proximity blocked

```text
fusion score >= threshold
proximity = false
```

Expected:

```text
reveal blocked
```

### Scenario F — Missing light sensor

```text
GPS = available
motion = available
light = unavailable
```

Expected:

```text
score calculated from available signals
no crash
```

### Scenario G — Missing accelerometer

```text
GPS = available
light = available
motion = unavailable
```

Expected:

```text
score calculated from available signals
no crash
```

### Scenario H — Missing proximity

Expected behavior must follow the agreed project fallback and must never falsely report hardware confirmation.

---

# 23. Smoothing

Raw sensor data can fluctuate.

M4 should avoid directly displaying every raw measurement.

Preferred conceptual approach:

```text
raw readings
    ↓
short moving average / smoothing
    ↓
normalized score
    ↓
fusion
    ↓
UI
```

The exact window length is a calibration parameter.

The purpose is to prevent:

```text
72%
73%
69%
81%
64%
```

from appearing as distracting jitter when the user's actual scan quality has not meaningfully changed.

---

# 24. Update Frequency

The UI does not necessarily need to render every hardware sensor event.

A practical architecture can:

```text
receive sensor events frequently
      ↓
process internally
      ↓
publish a controlled result update rate
      ↓
M2 UI
```

The exact rate should be chosen during implementation/testing.

Avoid excessive UI updates that waste resources without improving the scan experience.

---

# 25. Battery Considerations

M4 must apply battery-conscious behavior:

1. Do not register sensors before Scan Mode.
2. Do not keep sensors registered after Scan Mode.
3. Avoid unnecessary high-frequency processing.
4. Avoid duplicate listeners.
5. Avoid creating multiple sensor manager instances unnecessarily.
6. Stop processing after reveal/completion.
7. Release references appropriately with lifecycle cleanup.

M3 owns the battery-conscious GPS/geofence strategy.

M4 owns battery-conscious sensor usage.

---

# 26. Threading and UI Safety

Sensor callbacks can occur frequently.

M4 should avoid blocking the main thread with expensive processing.

Conceptually:

```text
sensor event
   ↓
lightweight processing
   ↓
fusion state
   ↓
observable state
   ↓
ViewModel
   ↓
UI
```

If processing becomes non-trivial, move suitable calculations off the UI thread.

The UI should never directly manipulate `SensorEvent`.

---

# 27. Error Handling

Potential failures:

- sensor unavailable;
- listener registration failure;
- unexpected sensor values;
- invalid relic light signature;
- stale location input;
- lifecycle stopping while scan is active;
- duplicate scan initialization.

The fusion engine should fail predictably.

Example conceptual error states:

```text
SensorUnavailable
InvalidSignature
InvalidLocationInput
ScanNotActive
```

Do not crash the application because a physical sensor is absent.

---

# 28. Sensor Value Validation

M4 should validate incoming data.

Examples:

```text
lux < 0
NaN
Infinity
```

should not be treated as valid measurements.

Similarly, invalid GPS distance values should be rejected rather than creating a misleading score.

A defensive rule:

```text
invalid measurement
      ↓
ignore / mark unavailable
      ↓
recalculate using valid signals
```

The exact fallback behavior should be consistent with the shared contract.

---

# 29. Relic Configuration Input

M4 should receive the relic's signature rather than hard-coding every relic inside the sensor engine.

Preferred:

```text
Relic
 ├── id
 ├── name
 ├── radiusM
 └── lightSignature
```

Then:

```text
SensorFusionEngine
       +
current Relic
       ↓
fusion calculation
```

This allows M5/M6 to supply relic data without changing M4's algorithm.

---

# 30. No Hard-Coded Firebase Access

M4 must not directly query Firestore for relic signatures.

Correct:

```text
M2 / ViewModel
      ↓
Repository
      ↓
Room / Firestore
      ↓
Relic configuration
      ↓
FusionEngine
```

Incorrect:

```text
FusionEngine
      ↓
Firebase
```

The fusion engine should remain testable without a network connection.

---

# 31. Interface With M2

M2 needs:

```text
current score
component scores if useful
sensor availability
proximity status
reveal permission
scan state
```

M2 does not need:

```text
SensorEvent
SensorManager
raw accelerometer implementation
raw lux processing algorithm
```

## M2 handoff

M4 provides a stable result such as:

```kotlin
FusionResult(
    score = ...,
    gpsScore = ...,
    lightScore = ...,
    motionScore = ...,
    proximityConfirmed = ...,
    availability = ...,
    revealAllowed = ...
)
```

M2 maps this into the visual scan meter and states.

---

# 32. Interface With M3

M3 provides:

```text
distanceMeters
accuracyMeters
```

and/or the exact agreed location-signal model.

M3 does not need to know:

- light normalization;
- accelerometer classification;
- fusion weighting;
- proximity behavior.

M4 does not modify M3's geofence implementation.

---

# 33. Interface With M5

M5 provides/obtains cloud-backed relic configuration.

M4 requires:

```text
relic ID
light signature
radius
```

M4 returns a scan outcome that may eventually be used by the application layer to record a successful reveal.

M4 should not directly write:

```text
Firestore progress
leaderboard
users
```

---

# 34. Interface With M6

M6 consumes successful relic completion/progress events for local persistence.

M4 should expose a clear event or result indicating:

```text
relic completed
```

M6 can then persist:

```text
relicId
foundAt
pendingSync
```

M4 should not directly write Room entities unless the team explicitly agrees to a different architecture.

---

# 35. Integration Contract

The team must freeze the following before final integration:

```text
FusionResult structure
sensor availability representation
distance input structure
light signature structure
fusion score range
reveal threshold
proximity gate semantics
sensor degradation behavior
final weights
```

If any item changes after freeze:

1. M4 updates the implementation.
2. M4 updates this contract or the shared contract document.
3. M4 tells M2/M3/M6 immediately.
4. Existing tests are rerun.
5. The change is committed in Git.

---

# 36. Provisional Parameters vs Final Parameters

The following must be treated as provisional during development:

- fusion weights;
- reveal threshold;
- motion sensitivity;
- smoothing window;
- light mismatch decay;
- GPS normalization curve;
- acceptable sensor update frequency.

The project should not claim these values are academically fixed unless the lecturer/project specification explicitly states them.

M4 must produce a small calibration record:

```text
Parameter
Initial value
Test condition
Observed issue
Final value
Reason
Date
Device
```

This becomes useful evidence for the final report/demo.

---

# 37. Campus Calibration Plan — September 22

M4 should perform physical calibration on campus.

## Test locations

Use at least several representative environments:

1. outdoor bright area;
2. shaded/outdoor transition;
3. indoor corridor;
4. indoor room/library-like environment;
5. target relic locations where practical.

## Light measurements

For each relic location record:

```text
location
time
lux
weather/lighting condition
device
```

Determine whether the development ranges are realistic.

## Motion

Test:

```text
slow left-right sweep
slow right-left sweep
small hand movement
walking movement
fast shaking
stationary phone
```

The detector should distinguish intended scanning from clearly unrelated motion.

## GPS

M3 leads GPS measurement.

M4 should verify that the distance values received from M3 behave sensibly near the relic.

---

# 38. Two-Device Sensor Matrix

Test on at least two physical Android devices where available.

Suggested matrix:

| Test | Device A | Device B |
|---|---|---|
| Light sensor present | ✓ | ✓/— |
| Accelerometer present | ✓ | ✓ |
| Proximity present | ✓/— | ✓/— |
| Light reading stability | test | test |
| Motion detection | test | test |
| Proximity confirmation | test | test |
| Fusion score | test | test |
| Missing-sensor fallback | test | test |
| Lifecycle cleanup | test | test |
| Battery behavior | observe | observe |

Do not assume two devices report identical lux values or accelerometer behavior.

---

# 39. Unit Testing

M4 should unit-test the algorithm independently from Android hardware.

## Required unit test categories

### Normalization

Test:

```text
minimum
maximum
middle
below range
above range
invalid values
```

### Light matching

Test:

```text
inside signature
exact boundary
slightly outside
far outside
```

### Motion classification

Test representative processed motion samples.

### Fusion

Test:

```text
all signals available
one signal unavailable
two signals unavailable
only GPS available
```

### Proximity

Test:

```text
threshold not reached + proximity true → false
threshold reached + proximity false → false
threshold reached + proximity true → true
```

### Degradation

Test that active weights always normalize correctly.

---

# 40. Component/Instrumented Testing

On Android hardware/emulator, verify:

- sensors are discovered;
- listeners register;
- listeners unregister;
- scan starts correctly;
- scan stops correctly;
- lifecycle transitions do not leak listeners;
- unavailable sensors do not crash;
- result state reaches ViewModel/UI;
- proximity gate behaves correctly.

Hardware-dependent tests should be separated from pure algorithm tests.

---

# 41. Testable Fusion Example

Suppose:

```text
gpsScore = 0.8
lightScore = 1.0
motionScore = 0.6
```

Using the **initial development example only**:

```text
GPS = 0.40
Light = 0.35
Motion = 0.25
```

Then:

```text
score =
(0.8 × 0.40)
+
(1.0 × 0.35)
+
(0.6 × 0.25)

= 0.32 + 0.35 + 0.15
= 0.82
```

Therefore:

```text
82%
```

This is an illustration of the algorithm, not a claim that 40/35/25 are the final weights.

---

# 42. Degradation Example

If light is unavailable:

```text
gpsScore = 0.8
motionScore = 0.6
```

Initial weights:

```text
GPS = 0.40
Motion = 0.25
```

Available total:

```text
0.65
```

Effective weights:

```text
GPS    = 0.40 / 0.65
Motion = 0.25 / 0.65
```

The two effective weights sum to:

```text
1.0
```

This prevents the missing sensor from artificially lowering every score simply because its weight was left unused.

---

# 43. What M4 Must Show M2

During integration, M4 should demonstrate:

1. Scan starts.
2. Sensor availability is detected.
3. Fusion percentage changes as inputs change.
4. Light mismatch affects score.
5. Motion affects score.
6. GPS distance affects score.
7. Missing sensor does not crash the scan.
8. Proximity does not change the percentage.
9. Proximity blocks reveal when not confirmed.
10. Proximity permits reveal when confirmed.

This is more valuable than showing raw sensor logs.

---

# 44. Debug Logging

During development only, use structured logs.

Example:

```text
SCAN_START relic=R001
SENSOR_AVAILABILITY light=true accel=true proximity=true
GPS distance=8.2
LIGHT lux=252
MOTION score=0.74
FUSION score=0.84
PROXIMITY close=false
REVEAL allowed=false
```

Do not leave excessive logging enabled in the final release build.

Never log sensitive user information unnecessarily.

---

# 45. Suggested Package Structure

A possible structure:

```text
sensor/
├── DeviceSensorManager.kt
├── SensorAvailability.kt
├── LightMatcher.kt
├── MotionClassifier.kt
├── SensorFusionEngine.kt
├── FusionResult.kt
├── ScanState.kt
└── ProximityGate.kt
```

Location input may be represented in a separate shared/domain package:

```text
domain/
└── LocationSignal.kt
```

The exact package layout can follow M1/M6 project conventions.

---

# 46. Separation of Responsibilities

### DeviceSensorManager

Owns:

```text
SensorManager
sensor discovery
listener registration
listener cleanup
```

### LightMatcher

Owns:

```text
lux → light score
```

### MotionClassifier

Owns:

```text
accelerometer data → motion score
```

### SensorFusionEngine

Owns:

```text
component scores
weights
degradation
combined score
```

### ProximityGate

Owns:

```text
near/far state
final confirmation
```

### ViewModel

Owns:

```text
UI state
screen lifecycle coordination
```

This separation makes M4 easier to test.

---

# 47. Avoid These Anti-Patterns

Do not:

### 47.1 Put sensor code directly in an Activity

Bad:

```kotlin
class ScanActivity : Activity(), SensorEventListener {
    // entire fusion algorithm here
}
```

This makes lifecycle, testing, and reuse harder.

### 47.2 Put Firebase calls in sensor classes

Sensor logic must remain independent of cloud storage.

### 47.3 Put Room calls in sensor classes

Persistence belongs to M6/repository architecture.

### 47.4 Register sensors permanently

This wastes resources.

### 47.5 Treat missing sensor as score zero

Use graceful degradation.

### 47.6 Include proximity in the percentage

This violates the project mechanic.

### 47.7 Duplicate GPS

M3 owns location.

### 47.8 Hard-code final weights without calibration

Initial weights are only development examples.

---

# 48. Git Branch

M4 branch:

```text
feature/sensor-fusion
```

Do not push unfinished experimental code directly to `main`.

Suggested commits:

```text
feat: add sensor availability detection
feat: add light signature matcher
feat: add accelerometer motion classifier
feat: add fusion engine
feat: add proximity reveal gate
test: add fusion engine unit tests
test: add sensor degradation scenarios
fix: clean sensor listeners on lifecycle stop
docs: record sensor calibration results
```

Keep commits focused.

---

# 49. Pull Request Requirements

Before requesting merge, M4 must provide:

- summary of changes;
- tests performed;
- device used;
- known limitations;
- any changed shared interfaces;
- final/provisional parameter status;
- screenshots/log evidence where useful.

PR reviewers should verify:

```text
no duplicate GPS
no Firebase access
no Room access
no UI-specific sensor logic
proximity separated from fusion
listeners cleaned up
tests pass
```

---

# 50. Day-by-Day Execution Schedule

## September 13 — Foundation

Tasks:

- inspect current Android project;
- identify package architecture;
- create sensor package;
- obtain `SensorManager`;
- detect light/accelerometer/proximity;
- define conceptual models;
- create branch.

Deliverable:

```text
sensor availability working
```

---

## September 14 — Light Sensor

Tasks:

- implement light listener;
- capture lux;
- validate values;
- create `LightMatcher`;
- support relic signatures;
- add unit tests.

Deliverable:

```text
lux → normalized light score
```

---

## September 15 — Accelerometer

Tasks:

- implement accelerometer listener;
- inspect raw data;
- select orientation-independent or documented orientation approach;
- implement motion preprocessing;
- create initial motion classifier.

Deliverable:

```text
controlled scan → meaningful motion score
```

---

## September 16 — Proximity

Tasks:

- implement proximity listener;
- distinguish near/far;
- create final gate;
- ensure proximity is not included in fusion calculation.

Deliverable:

```text
proximity confirmation works independently
```

---

## September 17 — Fusion Engine

Tasks:

- implement normalized component inputs;
- implement provisional weighting;
- implement combined score;
- implement degradation/reweighting;
- create `FusionResult`.

Deliverable:

```text
mock sensor inputs → fusion result
```

---

## September 18 — Unit Testing

Tasks:

- normalization tests;
- light tests;
- motion tests;
- fusion tests;
- degradation tests;
- proximity-gate tests.

Deliverable:

```text
algorithm test suite
```

---

## September 19 — M2/M3 Mock Integration

M3 supplies mocked distance.

M2 supplies/consumes mocked scan state.

Test:

```text
mock GPS
+
mock light
+
mock motion
+
mock proximity
→
M2 scan UI
```

Deliverable:

```text
full scan logic with fake inputs
```

---

## September 20 — Real GPS Integration

Connect M3's real location signal.

Verify:

```text
M3 distance
→
M4 GPS score
```

No duplicate location implementation.

Deliverable:

```text
real distance influences scan
```

---

## September 21 — Stability and Lifecycle

Test:

- open Scan Mode;
- leave Scan Mode;
- rotate/recreate where applicable;
- app background/foreground;
- repeated scans;
- listener cleanup;
- sensor availability.

Deliverable:

```text
no obvious listener leaks / duplicate registrations
```

---

## September 22 — Campus Calibration

Physical testing.

Measure:

- lux;
- motion behavior;
- proximity behavior;
- GPS interaction.

Review R001–R006 signatures.

Deliverable:

```text
calibration record
```

---

## September 23 — Freeze Sensor Contract

Finalize, after testing:

- weights;
- thresholds;
- smoothing;
- motion parameters;
- degradation behavior;
- proximity fallback;
- result structure.

Update shared contract.

Deliverable:

```text
M4 sensor contract frozen
```

---

## September 24 — Integration

Work with:

- M2;
- M3;
- M6;
- M5 where data configuration is involved.

Deliverable:

```text
R001 end-to-end scan
```

---

## September 25 — Multi-Relic Validation

Test:

```text
R001
R002
R003
R004
R005
R006
```

Verify signatures are loaded dynamically.

---

## September 26 — Device Testing

Repeat core tests on available devices.

Focus on:

- light variation;
- accelerometer orientation;
- proximity behavior;
- lifecycle;
- performance.

---

## September 27 — Bug Fixes

Only fix high-value issues.

Priorities:

1. crash;
2. reveal logic failure;
3. incorrect fusion;
4. lifecycle leak;
5. sensor compatibility;
6. UI polish.

---

## September 28 — Final Freeze

Confirm:

```text
sensor module builds
tests pass
contract documented
R001 works
multi-relic scan works
proximity gate works
no duplicate location
no persistence coupling
```

Prepare demo evidence.

---

# 51. M4 Dependencies

| Dependency | Needed from | Why |
|---|---|---|
| Relic model | Shared/M5/M6 | light signature |
| Location signal | M3 | GPS fusion input |
| Scan state integration | M2 | display result |
| Local persistence event | M6 | completed relic |
| Firebase-backed relic data | M5 | cloud configuration |
| App shell | M1 | final navigation |

M4 can develop with mocks before all dependencies are ready.

---

# 52. M4 → M2 Handoff Checklist

Before handoff:

- [ ] `FusionResult` agreed.
- [ ] Score range agreed.
- [ ] Component scores defined.
- [ ] Sensor availability defined.
- [ ] Reveal threshold agreed.
- [ ] Proximity semantics documented.
- [ ] Proximity excluded from fusion.
- [ ] Mock scenarios demonstrated.
- [ ] Missing-sensor behavior demonstrated.
- [ ] Lifecycle cleanup verified.

---

# 53. M4 → M3 Handoff Checklist

- [ ] M4 accepts M3 distance.
- [ ] M4 does not create another location pipeline.
- [ ] Distance units are meters.
- [ ] Accuracy field meaning is documented.
- [ ] Stale location behavior is documented.
- [ ] Geofence remains M3-owned.

---

# 54. M4 → M6 Handoff Checklist

- [ ] Successful reveal event defined.
- [ ] Relic ID is stable.
- [ ] Completion does not write Room directly.
- [ ] M6 can observe/receive completion.
- [ ] Offline behavior remains M6-owned.

---

# 55. M4 → M5 Handoff Checklist

- [ ] Relic signature model agreed.
- [ ] Relic IDs match Firestore.
- [ ] M4 does not directly access Firestore.
- [ ] Cloud configuration can be consumed through repository/domain layer.

---

# 56. Definition of Ready

M4 work is ready to start when:

- Android project builds;
- M4 branch exists;
- sensor package location is agreed;
- shared Relic model is known or mocked;
- M3 distance can be mocked;
- M2 can consume a conceptual result;
- initial provisional parameters are documented.

---

# 57. Definition of Done

M4 is done when:

- [ ] light sensor implemented;
- [ ] accelerometer implemented;
- [ ] proximity implemented;
- [ ] sensor availability handled;
- [ ] lifecycle registration/unregistration handled;
- [ ] fusion engine implemented;
- [ ] GPS input consumed from M3;
- [ ] graceful degradation implemented;
- [ ] proximity kept outside fusion;
- [ ] unit tests pass;
- [ ] hardware testing completed;
- [ ] campus calibration completed;
- [ ] final parameters documented;
- [ ] M2 integration completed;
- [ ] M3 integration completed;
- [ ] R001 end-to-end scan works;
- [ ] no direct Firebase/Room coupling;
- [ ] PR merged after review.

---

# 58. Risks

## Risk 1 — Light sensor unavailable

Mitigation:

```text
detect at runtime
reweight active signals
```

## Risk 2 — Light values vary by device

Mitigation:

```text
calibrate
use ranges rather than one exact lux
```

## Risk 3 — Motion detector too strict

Mitigation:

```text
test multiple users/devices
use tolerant classification
```

## Risk 4 — GPS inaccurate

Mitigation:

```text
M3 provides accuracy
use geofence + distance
do not depend on one exact coordinate reading
```

## Risk 5 — Sensor listener leak

Mitigation:

```text
centralized lifecycle management
explicit unregister
repeat open/close tests
```

## Risk 6 — Team changes result structure late

Mitigation:

```text
freeze shared contract by Sep 23
```

## Risk 7 — Proximity incorrectly added to score

Mitigation:

```text
separate ProximityGate class
unit-test score independently
```

---

# 59. If M4 Falls Behind

Priority order:

### Priority 1

```text
R001 working end-to-end
```

### Priority 2

```text
GPS + light + motion fusion
```

### Priority 3

```text
proximity final gate
```

### Priority 4

```text
sensor degradation
```

### Priority 5

```text
multi-device refinement
```

### Priority 6

```text
advanced smoothing/polish
```

Do not spend time on advanced sensor analytics while the basic scan cannot reliably reveal R001.

---

# 60. Evidence for Lecturer/Demo

M4 should be able to demonstrate:

1. Android sensor detection.
2. Live light reading.
3. Live motion detection.
4. Fusion score changing.
5. GPS contribution.
6. Missing-sensor fallback.
7. Proximity as separate gate.
8. Successful R001 reveal.
9. Unit tests.
10. Physical calibration.

A useful verbal explanation:

> “The scan score is calculated by combining GPS proximity, ambient-light signature matching, and accelerometer-based scanning motion. Each signal is normalized and weighted, and unavailable sensors are removed and their weights redistributed. The proximity sensor is deliberately separate: it is used only as the final close-range confirmation before revealing the relic.”

---

# 61. Final Technical Checklist

## Sensors

- [ ] SensorManager initialized.
- [ ] Light sensor checked.
- [ ] Accelerometer checked.
- [ ] Proximity sensor checked.
- [ ] Null sensor handling exists.

## Light

- [ ] Lux captured.
- [ ] Signature supplied dynamically.
- [ ] Match normalized.
- [ ] Invalid values handled.

## Motion

- [ ] Raw acceleration processed.
- [ ] Noise considered.
- [ ] Slow sweep recognized.
- [ ] Random movement does not automatically score as perfect.

## GPS

- [ ] M3 owns location.
- [ ] M4 consumes distance.
- [ ] No duplicate Fused Location Provider.

## Fusion

- [ ] Components normalized.
- [ ] Weights documented.
- [ ] Provisional values clearly marked until calibration.
- [ ] Missing signals reweighted.
- [ ] Final score bounded.
- [ ] Proximity excluded.

## Proximity

- [ ] Near/far detected.
- [ ] Final gate implemented.
- [ ] No contribution to percentage.
- [ ] Missing hardware handled honestly.

## Lifecycle

- [ ] Register on scan start.
- [ ] Unregister on scan end.
- [ ] Repeated scans safe.
- [ ] Background/foreground behavior tested.

## Integration

- [ ] M2 result contract implemented.
- [ ] M3 distance contract implemented.
- [ ] M5 relic configuration compatible.
- [ ] M6 completion handoff compatible.

## Testing

- [ ] Unit tests.
- [ ] Hardware tests.
- [ ] Missing-sensor tests.
- [ ] Lifecycle tests.
- [ ] Campus calibration.
- [ ] Two-device validation.

---

# 62. Final M4 Deliverables

By the end of the project, M4 should provide:

```text
1. Sensor availability implementation
2. Light sensor implementation
3. Accelerometer implementation
4. Proximity sensor implementation
5. LightMatcher
6. MotionClassifier
7. SensorFusionEngine
8. FusionResult
9. ScanState integration
10. Graceful degradation
11. Lifecycle-safe sensor management
12. Unit tests
13. Hardware test evidence
14. Campus calibration record
15. Final parameter record
16. Shared interface documentation
17. R001 end-to-end demonstration
```

---

# 63. One-Page M4 Summary

```text
M4 OWNS
────────────────────────────────────────
Light sensor
Accelerometer
Proximity sensor
SensorManager
Motion classification
Light matching
Fusion algorithm
Sensor availability
Graceful degradation
Sensor lifecycle
Calibration
────────────────────────────────────────

M4 RECEIVES
────────────────────────────────────────
Relic configuration
GPS distance from M3
────────────────────────────────────────

M4 RETURNS
────────────────────────────────────────
FusionResult
Scan state
Fusion percentage
Component scores
Proximity status
Reveal decision
────────────────────────────────────────

IMPORTANT RULE
────────────────────────────────────────
GPS + LIGHT + MOTION
        ↓
    FUSION SCORE

PROXIMITY
        ↓
FINAL REVEAL GATE

PROXIMITY IS NOT PART OF THE FUSION SCORE
────────────────────────────────────────

FIRST PRIORITY IF BEHIND
────────────────────────────────────────
Make R001 work reliably end-to-end.
────────────────────────────────────────
```

# 64. Final Handoff Statement

M4's output should be a reusable, testable sensor-fusion component that does not depend directly on UI, Firebase, Room, or a duplicate location system.

The clean boundary is:

```text
M3
Location
  ↓
M4
Sensor fusion
  ↓
M2
Scan UI
  ↓
M6/M5
Persistence and sync
```

The fusion engine should remain independently testable with mock inputs so that hardware availability, campus conditions, and network status do not prevent development.

The final calibrated values must be documented before the September 23 contract freeze. The initial values in this workplan are development examples only.
