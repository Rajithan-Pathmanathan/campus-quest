# Campus Quest — Sensor Fusion & Location Specification

**Document ID:** CQ-SHARED-04  
**Document:** Sensor Fusion and Location Specification  
**Project:** Campus Quest  
**Purpose:** Define the shared technical behaviour for GPS, Google Maps, dynamic checkpoint geofencing for active games, accelerometer scanning motion, ambient environmental light signature range matching (`minLux`..`maxLux`), proximity binary confirmation gate, sensor fusion scoring, graceful degradation, and the handoff from location to Scan Mode  
**Development window:** 13 September 2026 – 28 September 2026  
**Status:** Team working specification  
**Depends on:** `07_SHARED_CONTRACTS_AND_AGREEMENTS.md`, `08_MOCK_DATA_CATALOG.md`, `09_FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md`

---

# 1. Purpose

This document is the shared implementation contract for the **device and location mechanics** of Campus Quest.

The feature is intentionally designed around **sensor fusion**.

The core scan mechanic is:

```text
GPS distance
      +
Light signature match
      +
Accelerometer scanning motion
      ↓
   Fusion score
      ↓
Threshold reached
      ↓
Proximity confirmation
      ↓
Relic reveal
```

The proximity sensor is a final close-range gate.

It is **not** one of the weighted fusion inputs.

---

# 2. Why This Specification Exists

Six people are working in parallel.

Without a common technical specification, different members may implement incompatible interpretations such as:

```text
M3 thinks GPS alone starts the puzzle
M4 thinks proximity is part of the weighted score
M2 expects a 0–100 score
M5 stores a different light range
M6 stores a different relic radius
```

This document prevents those mismatches.

---

# 3. Core Ownership

| Feature | Owner | Supporting member |
|---|---|---|
| Google Maps | M3 | M1 |
| Location permission | M3 | M1 |
| Current location | M3 | M4 |
| Distance calculation | M3 | M4 |
| Geofencing | M3 | M4 |
| Accelerometer | M4 | M2 |
| Light sensor | M4 | M5 |
| Proximity sensor | M4 | M2 |
| Fusion algorithm | M4 | M3 |
| Scan state | M4 | M2 |
| Sensor degradation | M4 | M2 |
| Relic coordinates | M5 data + M3 validation | M6 |
| Light signatures | M5 data + M4 validation | M6 |

---

# 4. Device Architecture

The location/sensor architecture is:

```text
                 ┌─────────────────┐
                 │     Relic       │
                 │ coordinates     │
                 │ radius          │
                 │ light signature │
                 └────────┬────────┘
                          │
             ┌────────────┴────────────┐
             ↓                         ↓
      Location Service            Sensor Services
             │                         │
       GPS distance             Light / Motion
             │                  / Proximity
             └────────────┬────────────┘
                          ↓
                  SensorFusionEngine
                          ↓
                     FusionResult
                          ↓
                  Scan State Machine
                          ↓
                    Proximity Gate
                          ↓
                     Relic Reveal
```

M3 owns the location service.

M4 owns sensor services and the fusion engine.

M2 consumes the resulting state and renders it.

---

# 5. Location Responsibilities

M3 must implement:

```text
location permissions
current location
Google Maps
relic markers
distance calculation
geofence registration
geofence ENTER handling
geofence EXIT handling
location lifecycle
battery-conscious location usage
location error states
```

M3 must not implement the fusion mathematics.

---

# 6. Sensor Responsibilities

M4 must implement:

```text
accelerometer
light sensor
proximity sensor
sensor availability
motion interpretation
light matching
fusion scoring
degraded-sensor weighting
scan-state transitions
```

M4 must not implement:

```text
UI layouts
Firebase queries
Room DAOs
Google Maps rendering
```

---

# 7. Location Permission Flow

The application should not attempt to use location without the required permission.

Conceptual flow:

```text
App needs location
       ↓
Check permission
       ↓
Granted? ── No ──→ Request permission
   │
  Yes
   ↓
Start location-dependent feature
```

If permission is denied:

```text
Do not crash
Do not pretend location exists
Show an understandable permission state
Provide a way to retry/request permission where appropriate
```

---

# 8. Permission States

At minimum:

```text
UNKNOWN
REQUESTING
GRANTED
DENIED
```

The implementation may add more detailed states if required by Android behaviour.

---

# 9. Location Lifecycle

Location work should follow the Android application lifecycle.

Avoid leaving unnecessary location listeners running permanently.

The module architecture material emphasizes that mobile applications must handle lifecycle events such as backgrounding and configuration changes carefully. fileciteturn11file0L66-L70

The application should therefore:

```text
Start location work when required
 ↓
Use updates only when required
 ↓
Stop unnecessary active updates
```

---

# 10. Battery-Conscious Strategy

The project should avoid continuous high-frequency location polling when it is not necessary.

The intended pattern is:

```text
Map/navigation phase
        ↓
normal location handling
        ↓
approach relic
        ↓
geofence
        ↓
ENTER
        ↓
activate Scan Mode
```

The geofence is therefore an important battery-conscious transition mechanism.

---

# 11. Google Maps Responsibilities

M3 owns:

```text
Map initialization
Current-location display
Relic markers
Camera movement where appropriate
Marker selection
Map-related permission state
```

M1 owns the surrounding screen/navigation structure.

M3 supplies map/location state to the UI rather than placing application business logic inside the map screen.

---

# 12. Relic Marker Data

M3 receives:

```text
Relic.id
Relic.name
Relic.lat
Relic.lng
Relic.radiusM
```

from the repository.

The marker should identify the corresponding relic ID.

Example:

```text
R001
Founder's Bell
```

The map must not invent separate IDs.

---

# 13. Current Location Contract

A location service should expose a domain-level result.

Conceptually:

```kotlin
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val timestamp: Long
)
```

The exact implementation can differ.

The important point is that the rest of the application should not depend directly on raw Android location objects.

---

# 14. Distance Calculation

M3 is responsible for calculating the user's distance to the selected relic.

Conceptually:

```text
current user location
        +
relic latitude/longitude
        ↓
distance in metres
```

The result should be represented in metres.

Example:

```text
18 m
```

not:

```text
0.0002 degrees
```

---

# 15. Distance Contract

Conceptually:

```kotlin
data class DistanceResult(
    val distanceM: Float,
    val accuracyM: Float
)
```

M3 can provide this result to M4.

---

# 16. Why Accuracy Matters

GPS distance is not perfectly precise.

Example:

```text
distance = 18 m
accuracy = 45 m
```

does not provide the same confidence as:

```text
distance = 18 m
accuracy = 5 m
```

The UI should therefore be able to communicate poor location quality.

The application must not imply false precision.

---

# 17. Geofence Concept

A geofence defines a circular region around a relic.

Example:

```text
R001
radius = 25 m
```

When the user enters the region:

```text
ENTER
```

the application can activate Scan Mode.

---

# 18. Geofence Registration

M3 should register geofences using the relic definitions.

Conceptually:

```text
Get relics
 ↓
For each relic
 ↓
Create geofence
 ↓
Register geofence
```

The exact number of simultaneously registered geofences should consider Android limitations and the project's scope.

If the final implementation needs to register only nearby relics, document that decision.

---

# 19. Geofence Events

Core events:

```text
ENTER
EXIT
```

The application does not need a complex geofence state machine for the MVP.

---

# 20. ENTER Behaviour

When:

```text
R001 ENTER
```

occurs:

```text
Geofence event
 ↓
Identify R001
 ↓
Notify application
 ↓
Enable Scan Mode
 ↓
Begin/continue sensor fusion
```

The geofence should not directly reveal the relic.

Entering the geofence only makes the close-range scan available.

---

# 21. EXIT Behaviour

When:

```text
R001 EXIT
```

occurs:

```text
Update location/scan state
```

The exact UI behaviour may be:

```text
return to approaching state
```

or:

```text
stop active scan
```

depending on the current screen state.

Previously discovered relics must remain discovered.

---

# 22. Geofence vs Fusion

These are different concepts.

### Geofence

Answers:

```text
Is the user inside the relic's activation area?
```

### Fusion

Answers:

```text
How strongly do the available signals indicate the relic?
```

### Proximity

Answers:

```text
Is the device close enough for final reveal?
```

Therefore:

```text
Geofence ≠ Fusion ≠ Proximity
```

---

# 23. Scan Activation

The intended sequence is:

```text
Map
 ↓
User approaches relic
 ↓
Geofence ENTER
 ↓
Scan Mode becomes available
 ↓
Sensor fusion begins
```

The application should not require the user to continuously scan from hundreds of metres away.

---

# 24. Sensor Inventory

M4 works with:

```text
TYPE_ACCELEROMETER
TYPE_LIGHT
TYPE_PROXIMITY
```

The exact Android sensor availability must be checked at runtime.

Do not assume every Android device has every sensor.

---

# 25. Accelerometer Purpose

The accelerometer is used to detect the intended slow scanning movement.

The project concept uses a controlled side-to-side scanning gesture.

It is not intended to recognize arbitrary complex gestures.

---

# 26. Motion States

Recommended domain states:

```text
STATIONARY
SCANNING
UNSTABLE
UNAVAILABLE
```

The exact algorithm for classifying these states belongs to M4.

---

# 27. Motion Detection Principles

The implementation should:

```text
Read accelerometer values
 ↓
Process movement
 ↓
Reduce noise
 ↓
Determine movement pattern
 ↓
Produce normalized motion result
```

Do not send raw sensor arrays directly to M2.

M2 should receive a clean domain-level state or score.

---

# 28. Light Sensor Purpose

The light sensor provides the environmental signal.

Each relic has a stored light signature:

```text
min lux
max lux
```

Example:

```text
R001
180–320 lux
```

The live environment is compared against the stored signature.

---

# 29. Light Match

Conceptual process:

```text
Read current lux
       ↓
Read relic light signature
       ↓
Compare
       ↓
LightMatch
```

Example:

```text
Current: 250 lux
Signature: 180–320 lux
Result: strong match
```

---

# 30. Light Signature Source

M4 should receive the signature from:

```text
QuestRepository
```

rather than directly querying Firestore.

The signature originates from the relic data defined by M5.

---

# 31. Proximity Sensor Purpose

The proximity sensor is used as the final close-range confirmation.

Example:

```text
Fusion score = 91
Proximity = FAR
```

Result:

```text
Do not reveal
```

Then:

```text
Fusion score = 91
Proximity = NEAR
```

Result:

```text
Reveal allowed
```

---

# 32. Proximity Is Not a Fusion Input

This rule is mandatory.

Do not implement:

```text
GPS + Light + Accelerometer + Proximity
```

as the weighted fusion.

The intended model is:

```text
GPS + Light + Accelerometer
             ↓
        Fusion score
             ↓
      threshold reached
             ↓
       Proximity gate
             ↓
          Reveal
```

---

# 33. Fusion Inputs

The three core fusion inputs are:

```text
1. GPS distance
2. Light signature match
3. Accelerometer scanning motion
```

Each produces a normalized contribution.

Conceptually:

```text
GPS contribution
Light contribution
Motion contribution
        ↓
weighted combination
        ↓
0–100 score
```

---

# 34. Fusion Score Contract

M4 should expose:

```kotlin
data class FusionResult(
    val score: Int,
    val gpsContribution: Float,
    val lightContribution: Float?,
    val motionContribution: Float?,
    val availableSignals: Int
)
```

The exact fields can be simplified.

M2 primarily needs:

```text
score
scan state
```

Debug builds may expose individual contributions.

---

# 35. Score Range

The public score should be normalized to:

```text
0–100
```

Never expose arbitrary raw values such as:

```text
-15.3
472.8
0.82
```

to the UI as the primary fusion percentage.

---

# 36. Fusion Weights

The exact final weights must be agreed by M4 and documented before feature freeze.

The conceptual model is:

```text
score =
    GPS contribution × GPS weight
  + light contribution × Light weight
  + motion contribution × Motion weight
```

The weights must sum to:

```text
1.0
```

or:

```text
100%
```

depending on the implementation representation.

Do not let M2 independently invent weights.

---

# 37. Example Fusion Calculation

Illustrative example only:

```text
GPS contribution      = 0.90
Light contribution    = 0.80
Motion contribution   = 0.70

GPS weight            = 0.40
Light weight          = 0.35
Motion weight         = 0.25
```

Then:

```text
score =
(0.90 × 0.40)
+
(0.80 × 0.35)
+
(0.70 × 0.25)
```

The result is normalized to a percentage.

This is an example of the calculation structure, not a mandatory final weight configuration.

---

# 38. GPS Contribution

GPS should provide a normalized proximity contribution.

Conceptually:

```text
farther away → lower contribution
closer → higher contribution
```

The exact normalization must be defined by M4/M3 during implementation and validated physically.

Do not assume:

```text
1 metre = exactly 1%
```

unless the team deliberately adopts that rule.

---

# 39. Light Contribution

The light signal should be strongest when:

```text
current lux
```

is within the relic's expected signature.

Possible conceptual behaviour:

```text
inside range → high score
near range → partial score
far outside → low score
```

The exact curve can be linear, bounded, or otherwise deterministic.

It must remain stable enough to test.

---

# 40. Motion Contribution

The accelerometer contribution should reward the intended scan movement.

Conceptually:

```text
SCANNING → high
STATIONARY → low
UNSTABLE → reduced
```

Do not require perfect human movement.

The mechanic should be demonstrable on real devices.

---

# 41. Fusion Threshold

The threshold is the point at which:

```text
FusionResult.score
```

is high enough to move to:

```text
READY_FOR_PROXIMITY
```

The exact numeric threshold must be stored in one shared implementation constant/configuration.

Do not hard-code:

```text
80
```

in M2 and:

```text
85
```

in M4.

---

# 42. Recommended Threshold Configuration

Conceptually:

```kotlin
object FusionConfig {
    const val REVEAL_PREPARE_THRESHOLD = 80
}
```

The final value should be validated by M4 during campus calibration.

---

# 43. Scan State Machine

Recommended states:

```text
NOT_AVAILABLE
READY
SCANNING
READY_FOR_PROXIMITY
REVEALED
LIMITED
ERROR
```

Conceptual transitions:

```text
NOT_AVAILABLE
      ↓ geofence ENTER
READY
      ↓ sensor readings
SCANNING
      ↓ threshold
READY_FOR_PROXIMITY
      ↓ proximity NEAR
REVEALED
```

---

# 44. Scan State: NOT_AVAILABLE

Use when:

```text
user is outside the activation area
```

Expected UI:

```text
Approach the relic to begin scanning.
```

---

# 45. Scan State: READY

Use when:

```text
user has entered the activation area
```

Expected UI:

```text
Start scanning
```

or an automatically activated scan interface, depending on the final UX.

---

# 46. Scan State: SCANNING

Use when sensor data is actively being processed.

M4 continuously updates:

```text
FusionResult
```

M2 displays:

```text
current percentage
```

---

# 47. Scan State: READY_FOR_PROXIMITY

Use when:

```text
fusion threshold reached
```

but:

```text
proximity = FAR
```

Expected UI:

```text
Signal matched.
Move closer to reveal the relic.
```

---

# 48. Scan State: REVEALED

Use when:

```text
fusion threshold reached
AND
proximity gate satisfied
```

Then:

```text
reveal relic
record discovery
```

The reveal itself belongs to the application flow; M4 provides the condition.

---

# 49. Scan State: LIMITED

Use when required sensors are unavailable but the application can still operate under graceful degradation.

Example:

```text
Light sensor unavailable
GPS available
Accelerometer available
```

The UI may communicate:

```text
Limited sensor mode
```

without blocking the entire application unnecessarily.

---

# 50. Sensor Availability Contract

M4 should expose availability explicitly.

Conceptually:

```kotlin
data class SensorAvailability(
    val accelerometer: Boolean,
    val light: Boolean,
    val proximity: Boolean
)
```

This allows the UI and tests to understand the device capability.

---

# 51. Graceful Degradation

The application must not assume:

```text
all sensors exist
```

The intended behaviour is:

```text
Check available sensors
 ↓
Use available fusion inputs
 ↓
Recalculate weights
 ↓
Continue where supported
```

This is a required robustness feature.

---

# 52. Reweighting Example

Suppose the normal weights are:

```text
GPS = 0.40
Light = 0.35
Motion = 0.25
```

If light is unavailable:

```text
GPS + Motion
```

must be reweighted according to the agreed degradation strategy.

For example, conceptually:

```text
GPS = 0.60
Motion = 0.40
```

This is an illustrative example.

The final degraded weights must be chosen and documented by M4.

---

# 53. Missing Proximity Sensor

Proximity is different from the fusion inputs.

If the proximity sensor is unavailable:

```text
Do not fabricate a NEAR value.
```

The team must decide and document the final fallback rule before feature freeze.

The fallback must be honest about hardware limitations.

---

# 54. Missing All Sensors

If all required sensors are unavailable:

```text
Do not crash.
```

Possible state:

```text
LIMITED
```

with a clear message explaining that the device cannot perform the full sensor-based scan.

The exact fallback must remain within the project's scope.

---

# 55. Sensor Registration

M4 should:

```text
Check sensor availability
 ↓
Register listeners when Scan Mode starts
 ↓
Process values
 ↓
Unregister listeners when Scan Mode stops
```

Avoid keeping high-frequency sensor listeners active throughout the entire application lifecycle.

---

# 56. Sensor Lifecycle

When the user leaves Scan Mode:

```text
Stop sensor processing
 ↓
Unregister listeners
```

When Scan Mode resumes:

```text
Check availability
 ↓
Register listeners
```

This reduces unnecessary battery use.

---

# 57. Sensor Data Smoothing

Raw sensors can be noisy.

M4 may use:

```text
moving average
low-pass filtering
thresholding
debouncing
```

where useful.

The exact filter is an implementation choice.

The team should favour a simple deterministic approach that can be explained during the demo.

---

# 58. Avoid Overengineering Sensor Processing

Do not spend the limited project period building:

```text
machine-learning gesture recognition
complex signal processing
advanced sensor fusion frameworks
```

unless the core mechanism is already stable.

The assignment needs a reliable demonstration of the concepts taught in the module.

---

# 59. Complete Sensor-Fusion Interface

A useful implementation boundary is:

```kotlin
interface SensorFusionEngine {

    fun start(relic: Relic)

    fun stop()

    fun updateLocation(
        distanceM: Float,
        accuracyM: Float
    )

    fun updateLight(
        lux: Float
    )

    fun updateMotion(
        state: MotionState
    )

    fun updateProximity(
        proximity: Proximity
    )

    fun observeResult():
        Flow<FusionResult>
}
```

This is a proposed team implementation contract.

M4 owns it.

M2 should consume its output rather than implement sensor calculations.

---

# 60. Location-to-Fusion Handoff

M3 → M4:

```text
DistanceResult
```

M4 uses:

```text
distanceM
accuracyM
```

M4 should not need:

```text
GoogleMap
Marker
MapFragment
```

---

# 61. Fusion-to-UI Handoff

M4 → M2:

```text
FusionResult
ScanState
SensorAvailability
```

M2 renders:

```text
percentage
instructions
sensor limitation
proximity prompt
success state
```

---

# 62. Relic-to-Sensor Handoff

M5/M6 → M4:

```text
Relic
```

M4 extracts:

```text
lightSignature
```

and uses:

```text
lat/lng/radius
```

only when required by the agreed integration design.

---

# 63. End-to-End Contract

The complete handoff is:

```text
M5/M6
Relic data
     ↓
M3
Location + Geofence
     ↓
M4
GPS + Light + Motion
     ↓
FusionResult
     ↓
M4
Proximity gate
     ↓
M2
Scan UI
     ↓
M2/ViewModel
Record reveal
     ↓
M6/M5
Room + Firestore
```

---

# 64. Primary Demo Scenario

Use R001.

Initial:

```text
R001
Founder's Bell
radius = 25 m
light = 180–320 lux
```

User:

```text
150 m away
```

Then:

```text
55 m
```

Then:

```text
18 m
```

Geofence:

```text
ENTER
```

Sensor values:

```text
250 lux
SCANNING motion
```

Fusion:

```text
87%
```

Proximity:

```text
NEAR
```

Result:

```text
REVEAL
```

---

# 65. Successful Flow

```text
Map
 ↓
Location permission
 ↓
Current location
 ↓
Relic marker
 ↓
Distance calculation
 ↓
Geofence ENTER
 ↓
Scan Mode
 ↓
GPS contribution
 ↓
Light contribution
 ↓
Motion contribution
 ↓
Fusion score
 ↓
Threshold
 ↓
Proximity
 ↓
Reveal
```

This is the central technical flow of Campus Quest.

---

# 66. Failed Scan Example

```text
Distance = 18 m
Light = 500 lux
Motion = STATIONARY
Fusion = 35%
Proximity = NEAR
```

Expected:

```text
Do not reveal
```

Reason:

```text
Proximity alone is insufficient.
```

---

# 67. Another Failed Scan Example

```text
Distance = 18 m
Light = 250 lux
Motion = SCANNING
Fusion = 88%
Proximity = FAR
```

Expected:

```text
Do not reveal yet.
```

Reason:

```text
Final proximity gate is not satisfied.
```

---

# 68. Sensor-Degraded Example

```text
Distance = 18 m
Light = unavailable
Motion = SCANNING
Proximity = NEAR
```

Expected:

```text
Limited fusion
```

using the agreed available-signal weights.

---

# 69. Location-Degraded Example

```text
Distance = 18 m
Accuracy = 45 m
```

Expected:

```text
Low confidence location state
```

The application should avoid presenting:

```text
"Exactly 18 metres away"
```

as if the value were highly reliable.

---

# 70. GPS and Geofence Testing

M3 must test:

```text
permission granted
permission denied
location unavailable
poor accuracy
far away
approaching
boundary
inside geofence
exit
app background
app foreground
```

---

# 71. Sensor Testing

M4 must test:

```text
all sensors available
light missing
accelerometer missing
proximity missing
all missing
stationary
slow scanning
unstable movement
correct light
incorrect light
threshold reached
threshold not reached
proximity far
proximity near
```

---

# 72. Device Testing

The project should be tested on at least two Android devices where possible.

Test differences such as:

```text
sensor availability
sensor range
GPS accuracy
screen size
Android version
permission behaviour
```

Do not validate the entire sensor mechanic only on the developer's own phone.

---

# 73. Physical Light Calibration

The canonical mock ranges are:

```text
R001 = 180–320
R002 = 250–450
R003 = 80–180
R004 = 400–650
R005 = 120–250
R006 = 300–500
```

These are development values.

Before final demo:

```text
measure actual campus environment
 ↓
compare readings
 ↓
adjust signatures if needed
 ↓
update shared mock catalog
 ↓
update Firebase seed data
 ↓
update tests
```

Do not change only one copy of the signature.

---

# 74. Physical GPS Calibration

M3 should test the actual selected campus locations.

Record:

```text
expected coordinate
actual observed coordinate
accuracy
distance behaviour
geofence behaviour
```

If the radius is too small for reliable demonstration, discuss the change with M4/M5 before modifying the shared data.

---

# 75. Physical Motion Calibration

M4 should test:

```text
normal walking
standing still
slow side-to-side scan
fast movement
random shaking
```

The intended scanning motion should be:

```text
simple
repeatable
easy to explain
easy to demonstrate
```

---

# 76. Physical Proximity Calibration

Proximity sensors differ between devices.

M4 must verify:

```text
NEAR
FAR
```

behaviour on the actual demo devices.

The implementation should use the Android sensor's reported values rather than assuming a universal physical distance.

---

# 77. Battery Test

At minimum compare:

```text
normal Map usage
```

against:

```text
active Scan Mode
```

Check that sensors and location are not unnecessarily running after:

```text
scan complete
scan cancelled
user leaves Scan Mode
```

---

# 78. Common Implementation Mistakes

Avoid:

### Mistake 1

```text
Activity calculates fusion
```

Correct:

```text
SensorFusionEngine
```

---

### Mistake 2

```text
M2 reads accelerometer
```

Correct:

```text
M4 exposes scan result
```

---

### Mistake 3

```text
M3 queries Firestore
```

Correct:

```text
Repository → Relic → M3
```

---

### Mistake 4

```text
Proximity contributes to weighted score
```

Correct:

```text
Proximity is final gate
```

---

### Mistake 5

```text
Sensor listener remains active forever
```

Correct:

```text
register during scan
unregister when scan ends
```

---

### Mistake 6

```text
Missing sensor crashes the scan
```

Correct:

```text
detect availability
apply degradation/fallback
```

---

# 79. Integration Sequence

The recommended integration order is:

## Phase 1

```text
M3 → M4
```

Provide:

```text
DistanceResult
```

## Phase 2

```text
M4 → M2
```

Provide:

```text
FusionResult
ScanState
```

## Phase 3

```text
M5/M6 → M4
```

Provide:

```text
Relic
LightSignature
```

## Phase 4

```text
M2 → M6/M5
```

Provide:

```text
recordReveal()
```

---

# 80. Mock-First Development

Before physical sensors work:

```text
M3 can mock distance
M4 can mock sensor values
M2 can mock FusionResult
```

Example:

```text
distance = 18
light = 250
motion = SCANNING
fusion = 87
proximity = NEAR
```

This lets M2 finish the complete scan UI before device integration.

---

# 81. Definition of Ready — M4

M4 should not begin implementation without:

- [ ] shared `Relic` model
- [ ] `LightSignature`
- [ ] agreed fusion result shape
- [ ] mock sensor cases
- [ ] M3 distance contract
- [ ] M2 scan-state expectations

---

# 82. Definition of Done — M3

M3 is complete when:

- [ ] location permission works
- [ ] map loads
- [ ] current location works
- [ ] relic markers work
- [ ] distance works
- [ ] geofence registration works
- [ ] ENTER is detected
- [ ] EXIT is handled
- [ ] poor accuracy is handled
- [ ] location lifecycle is controlled
- [ ] unnecessary location work stops
- [ ] M4 receives correct distance data

---

# 83. Definition of Done — M4

M4 is complete when:

- [ ] accelerometer works
- [ ] motion state works
- [ ] light sensor works
- [ ] light matching works
- [ ] proximity works
- [ ] availability detection works
- [ ] fusion score is 0–100
- [ ] weights are documented
- [ ] degraded weighting works
- [ ] threshold is centralized
- [ ] proximity is final gate
- [ ] listeners are lifecycle-safe
- [ ] scan state works
- [ ] at least two device tests completed where available
- [ ] M2 receives stable outputs

---

# 84. Definition of Done — Integration

The device subsystem is integrated when:

```text
Relic
 ↓
Map marker
 ↓
Location
 ↓
Geofence
 ↓
Scan Mode
 ↓
Sensor fusion
 ↓
Threshold
 ↓
Proximity
 ↓
Reveal
```

works on a real Android device.

---

# 85. M3 Timeline

## 13 Sep

```text
Google Maps
Location permission
Current location
```

Deliver:

```text
working map screen
```

---

## 14 Sep

```text
Relic markers
Distance calculation
```

---

## 15 Sep

```text
Location states
Poor accuracy handling
```

---

## 16 Sep

```text
Geofence registration
```

---

## 17 Sep

```text
ENTER/EXIT handling
```

---

## 18 Sep

```text
Physical geofence test
```

---

## 19 Sep

```text
Location module complete
M4 handoff
```

---

## 20–21 Sep

Integrate:

```text
DistanceResult → M4
```

---

## 22–23 Sep

End-to-end:

```text
Map → Geofence → Scan
```

---

## 24–27 Sep

Testing and bug fixing only.

---

## 28 Sep

Final evidence and documentation.

---

# 86. M4 Timeline

## 13 Sep

```text
Sensor discovery
Accelerometer values
```

---

## 14 Sep

```text
Motion processing
```

---

## 15 Sep

```text
Motion state classification
```

---

## 16 Sep

```text
Light sensor
```

---

## 17 Sep

```text
Light signature matching
```

---

## 18 Sep

```text
Proximity
```

---

## 19 Sep

```text
Initial fusion engine
using mock GPS
```

---

## 20 Sep

```text
M3 GPS integration
```

---

## 21 Sep

```text
Full sensor fusion
```

---

## 22 Sep

```text
Physical calibration
```

---

## 23 Sep

```text
Weights
threshold
sensor degradation
freeze
```

---

## 24–27 Sep

Device testing and bug fixing.

---

## 28 Sep

Final evidence and documentation.

---

# 87. Communication Checkpoints

M3 must communicate with M4 on:

```text
distance unit
accuracy meaning
geofence state
```

M4 must communicate with M2 on:

```text
score range
scan states
threshold
sensor limitation messages
```

M4 must communicate with M5 on:

```text
light signature fields
```

M4 and M6 must communicate on:

```text
relic model compatibility
```

---

# 88. Change Control

Any change to:

```text
fusion weights
threshold
light signature
geofence radius
sensor-state meaning
```

must be communicated to all affected members.

Use:

```text
Change:
Reason:
Old:
New:
Affected members:
Tests updated:
```

---

# 89. Feature Freeze

By:

**23 September 2026**

the device subsystem must be considered feature-complete.

After that:

```text
No new sensor mechanics
No new gesture system
No new fusion algorithm
No major location redesign
```

unless required to fix a critical defect.

---

# 90. Stretch Features

Do not allow these to delay the core mechanic:

```text
CameraX legendary relic
crowd-aware routing
advanced relay mechanics
complex sensor intelligence
```

The core requirement is:

```text
GPS + Light + Accelerometer
        ↓
Fusion
        ↓
Proximity
        ↓
Reveal
```

---

# 91. Final Technical Summary

The Campus Quest sensor mechanic is:

```text
               GPS
                │
                │
        ┌───────┴───────┐
        │               │
      Light        Accelerometer
        │               │
        └───────┬───────┘
                ↓
          Fusion Engine
                ↓
           Score 0–100
                ↓
        Threshold reached?
          │            │
         No           Yes
          │            ↓
       Continue     Proximity
                       │
                   Near?
                  │     │
                 No    Yes
                  │     ↓
               Wait    Reveal
```

This is the central device-side implementation contract.

---

# 92. Final Checklist

## Location

- [ ] permissions
- [ ] map
- [ ] current location
- [ ] markers
- [ ] distance
- [ ] accuracy
- [ ] geofence
- [ ] ENTER
- [ ] EXIT
- [ ] lifecycle
- [ ] battery

## Sensors

- [ ] accelerometer
- [ ] light
- [ ] proximity
- [ ] availability
- [ ] motion state
- [ ] light match
- [ ] proximity gate

## Fusion

- [ ] GPS input
- [ ] light input
- [ ] motion input
- [ ] normalized score
- [ ] documented weights
- [ ] threshold
- [ ] degraded mode

## Integration

- [ ] M3 → M4 distance
- [ ] M4 → M2 fusion
- [ ] M5/M6 → M4 relic/signature
- [ ] M2 → repository reveal
- [ ] real-device end-to-end test

## Final validation

- [ ] two devices where possible
- [ ] permissions tested
- [ ] sensor absence tested
- [ ] GPS accuracy tested
- [ ] offline behaviour tested with M6
- [ ] battery behaviour checked
- [ ] feature freeze respected

---

# 93. Final Principle

The device subsystem should expose **meaningful domain results**, not raw hardware complexity.

```text
Raw GPS
Raw sensors
      ↓
Device services
      ↓
Domain results
      ↓
ViewModel
      ↓
UI
```

The UI should know:

```text
18 m away
87%
Move closer
Sensor limited
Relic revealed
```

It should not need to know:

```text
LocationManager internals
SensorEvent arrays
Firestore document paths
filter coefficients
geofence implementation details
```

This separation keeps the application aligned with the MVVM and separation-of-concerns approach used in the module. fileciteturn11file0L178-L191
