# Campus Quest — M3 Workplan: Location & Geofencing

**Document ID:** CQ-M3  
**Role:** M3 — Device Developer: Location & Maps  
**Project:** Campus Quest  
**Development window:** 13 September 2026 – 28 September 2026  
**Primary responsibility:** Google Maps, Fused Location Provider, location permissions, distance calculation, relic markers, geofence registration, geofence ENTER/EXIT handling, and battery-conscious location behaviour  
**Primary dependencies:** M1 app shell/navigation, M4 sensor fusion, M5/M6 relic data

---

# 1. Role Objective

M3 owns the location side of Campus Quest.

The target technical flow is:

```text
Relic coordinates
+
Device location
 ↓
Distance calculation
 ↓
Map presentation
 ↓
Geofence
 ↓
ENTER event
 ↓
Scan Mode becomes available
```

M3 does not reveal relics and does not calculate the sensor-fusion score.

---

# 2. Primary Deliverables

M3 must deliver:

- Google Maps integration
- Map screen integration
- Location permission handling
- Fused Location Provider integration
- Current-location handling
- Location accuracy handling
- Distance calculation
- Relic markers
- Selected relic/location state
- Geofence registration
- Geofence ENTER handling
- Geofence EXIT handling
- Duplicate-event protection
- Battery-conscious location lifecycle
- Mock location support for development
- Physical GPS validation
- Handoff to M4 and M2

---

# 3. Ownership Boundary

M3 owns:

```text
Maps
GPS/location
permissions
distance
geofencing
location lifecycle
location accuracy
```

M3 provides:

```text
LocationResult
DistanceResult
GeofenceEvent
```

conceptually.

---

# 4. Non-Ownership Boundary

M3 does not own:

```text
light sensor
accelerometer
proximity sensor
fusion mathematics
reveal logic
Room
Firestore
Firebase Auth
quest presentation
```

---

# 5. Architecture

Recommended boundary:

```text
UI
 ↓
ViewModel
 ↓
Location/Repository abstraction
 ↓
Fused Location Provider
```

The UI should not directly manage complex location operations.

The architecture materials emphasize separation of concerns and lifecycle-aware application structure. fileciteturn11file0L66-L70

---

# 6. Google Maps

M3 integrates the map into the screen provided by M1.

M1 owns:

```text
navigation
screen shell
theme
```

M3 owns:

```text
map
markers
location display
```

---

# 7. Map Requirements

The map should support:

```text
campus area
current location
relic markers
selected relic
```

The exact map visual design follows the approved application UI.

---

# 8. Map Initial State

When location is not yet available:

```text
show map
show appropriate loading/location state
```

Do not fabricate a real current position.

---

# 9. Location Permission

The application must request the location permission required by the final Android implementation.

Handle:

```text
not requested
granted
denied
revoked
```

---

# 10. Permission Denied

If the user denies location permission:

```text
no crash
```

Provide a readable explanation.

Example:

```text
Location permission is required to find nearby relics.
```

Exact wording may follow M1's UX design.

---

# 11. Permission Revoked

If permission is later revoked through Android settings:

```text
detect state on returning to app
```

Do not assume a previously granted permission remains granted forever.

---

# 12. Permission Test

Test:

```text
fresh install
 ↓
launch
 ↓
location permission denied
```

Expected:

```text
safe limited state
```

Then:

```text
grant permission
```

Expected:

```text
location features become available
```

---

# 13. Fused Location Provider

Use Android's fused location approach for location acquisition rather than implementing separate GPS/network providers unnecessarily.

The objective is:

```text
usable location
+
reasonable battery behaviour
```

---

# 14. Location Result

Conceptual result:

```kotlin
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val timestamp: Long
)
```

The final implementation may use a different model.

---

# 15. Location Accuracy

Always consider:

```text
accuracyM
```

alongside:

```text
latitude
longitude
```

Example:

```text
distance = 18 m
accuracy = 40 m
```

does not provide strong confidence that the device is actually 18 m from the relic.

---

# 16. Poor Accuracy State

When accuracy is poor:

```text
do not present false precision
```

The UI may show:

```text
GPS accuracy is low.
Move to an open area.
```

according to the final UX.

---

# 17. Distance Calculation

Distance is calculated between:

```text
device location
```

and:

```text
relic coordinates
```

Result:

```text
metres
```

---

# 18. Distance Result

Conceptual:

```kotlin
data class DistanceResult(
    val relicId: String,
    val distanceM: Float,
    val accuracyM: Float
)
```

The exact final model may differ.

---

# 19. Distance Test Values

Use canonical development scenarios.

Example:

```text
R001 radius = 25 m
```

Test:

```text
150 m
55 m
26 m
18 m
```

---

# 20. Distance Interpretation

Example:

```text
150 m
```

→ Far

```text
55 m
```

→ Approaching

```text
26 m
```

→ Outside 25 m radius

```text
18 m
```

→ Inside 25 m radius

These are test scenarios, not claims about actual campus distance.

---

# 21. Distance Boundary

Always test just outside and just inside the radius.

For R001:

```text
24 m
25 m
26 m
```

This catches boundary mistakes.

---

# 22. Accuracy Boundary

Test combinations such as:

```text
distance = 18 m
accuracy = 5 m
```

and:

```text
distance = 18 m
accuracy = 45 m
```

The second should be treated as low-confidence.

---

# 23. Relic Coordinates

Use the canonical mock dataset:

```text
R001 — 6.974850, 79.915300
R002 — 6.975420, 79.914750
R003 — 6.975900, 79.915650
R004 — 6.976300, 79.914900
R005 — 6.974300, 79.916100
R006 — 6.976750, 79.915700
```

These coordinates are development data and must be physically validated before the final campus demonstration.

---

# 24. Relic Radii

Canonical development radii:

| Relic | Radius |
|---|---:|
| R001 | 25 m |
| R002 | 25 m |
| R003 | 20 m |
| R004 | 30 m |
| R005 | 25 m |
| R006 | 20 m |

---

# 25. Relic Markers

Display markers for:

```text
R001
R002
R003
R004
R005
R006
```

The marker data should come from the agreed repository/data model rather than being duplicated in the map code.

---

# 26. Marker Selection

When a user selects a marker:

```text
selectedRelicId
```

should be passed to the appropriate quest/detail flow.

Do not use marker screen position as identity.

---

# 27. Current Location Marker

If permission and location are available:

```text
show current location
```

Use Android/Google Maps-supported behaviour where appropriate.

---

# 28. Location Lifecycle

Location updates should not run unnecessarily.

Conceptual:

```text
Map visible + location needed
 ↓
location active
 ↓
leave map / no longer needed
 ↓
location reduced/stopped
```

The final lifecycle must match the app's actual navigation requirements.

---

# 29. Battery-Conscious Principle

Do not continuously poll high-frequency location across the entire app.

The project specifically aims for battery-conscious location behaviour.

Preferred concept:

```text
normal exploration
 ↓
geofence handles entry detection
 ↓
ENTER
 ↓
Scan Mode
 ↓
more active location/sensor work if required
 ↓
scan ends
 ↓
stop unnecessary active work
```

---

# 30. Geofencing

A geofence represents a region around a relic.

Conceptually:

```text
Relic
 ↓
latitude
longitude
radius
 ↓
Geofence
```

---

# 31. Geofence Registration

For each required relic:

```text
request geofence
```

with:

```text
relicId
latitude
longitude
radius
transition types
```

The exact Android API configuration should follow the final implementation.

---

# 32. Geofence Transition

Core event:

```text
ENTER
```

This is the important trigger.

The purpose is:

```text
user is close enough
 ↓
enable Scan Mode
```

---

# 33. Geofence ENTER Does Not Reveal

Critical rule:

```text
ENTER ≠ DISCOVERED
```

ENTER only means:

```text
Scan can begin.
```

The scan still requires:

```text
GPS
+
Light
+
Accelerometer
 ↓
Fusion threshold
 ↓
Proximity
 ↓
Reveal
```

---

# 34. Geofence EXIT

Handle:

```text
EXIT
```

without corrupting progress.

If a user leaves the area:

```text
previously discovered relic remains discovered
```

---

# 35. Duplicate ENTER

The same event may be received more than once.

Protect against:

```text
ENTER R001
ENTER R001
```

creating:

```text
two discoveries
```

The final discovery idempotency is handled through M6/M5, but M3 should avoid unnecessary duplicate UI events.

---

# 36. Wrong Relic Event

If:

```text
selected = R001
```

and:

```text
ENTER R002
```

is received, do not reveal R001.

The event must retain the relic identity.

---

# 37. Geofence Event Model

Conceptual:

```kotlin
data class GeofenceEvent(
    val relicId: String,
    val transition: GeofenceTransition,
    val timestamp: Long
)
```

Possible transitions:

```text
ENTER
EXIT
```

The final implementation may include additional metadata.

---

# 38. Geofence Receiver

The geofence event handling component should:

```text
receive event
 ↓
identify relic
 ↓
validate event
 ↓
publish event/state
```

Avoid putting the entire scan/reveal process inside the receiver.

---

# 39. Background Event

Geofence events may occur when the app UI is not actively visible.

Therefore:

```text
event handling
```

must be separate from:

```text
visible UI rendering
```

The UI can respond when it becomes active.

---

# 40. Geofence Reliability

Do not promise instant centimetre-level transitions.

Real-world geofencing depends on:

```text
location accuracy
device conditions
Android behaviour
environment
```

The final demo should use realistic expectations.

---

# 41. Geofence Physical Test

On a real device:

```text
stand outside radius
 ↓
approach
 ↓
enter region
```

Observe:

```text
ENTER received
Scan Mode available
```

Record approximate timing.

---

# 42. Physical Boundary Test

For R001:

```text
outside:
~30 m

near boundary:
~25 m

inside:
~18 m
```

Do not rely solely on exact tape-measured distances because GPS itself has uncertainty.

---

# 43. Location Mocking

Before physical testing, use mock location data.

Example:

```text
R001:
150 m
55 m
26 m
18 m
```

This lets M3 and M4 integrate without requiring the team to physically walk around campus for every test.

---

# 44. Mock Location Provider

Create a development-only abstraction or provider where useful.

Example conceptual interface:

```kotlin
interface LocationProvider {
    fun observeLocation(): Flow<LocationResult>
}
```

Then:

```text
MockLocationProvider
```

can be used during UI/integration development.

---

# 45. Real Location Provider

Production implementation:

```text
Fused Location Provider
```

Mock implementation:

```text
MockLocationProvider
```

Both should produce compatible application-level results.

---

# 46. M3 → M4 Contract

M4 needs location information for fusion.

M3 provides:

```text
distanceM
accuracyM
relicId
```

through the agreed contract.

M4 should not calculate a second independent GPS distance if the shared design uses M3's distance result.

---

# 47. M3 → M2 Contract

M2 needs user-facing location state.

Provide:

```text
distance
accuracy/status
geofence event
```

M2 turns this into:

```text
Far
Approaching
Within area
Ready to scan
```

---

# 48. M3 → M5/M6

Relic coordinates should come from the shared relic data.

M3 should not permanently hard-code production coordinates if Firebase/Room is intended to provide them.

---

# 49. Repository Boundary

Conceptual:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Location Provider
```

The final project may separate location into its own service/repository abstraction.

---

# 50. Location Error States

Support:

```text
PERMISSION_DENIED
LOCATION_UNAVAILABLE
POOR_ACCURACY
PROVIDER_ERROR
```

Exact enum names are implementation choices.

---

# 51. Permission Error

Expected:

```text
safe UI state
```

No crash.

---

# 52. Provider Error

If location provider fails:

```text
notify state
```

Do not return:

```text
0,0
```

as a fake location.

---

# 53. Null Location

If no location is available:

```text
LocationResult = unavailable
```

rather than:

```text
latitude = 0
longitude = 0
```

unless the model explicitly defines a valid nullable representation.

---

# 54. Accuracy Filtering

Consider rejecting or reducing confidence in extremely poor location readings.

The exact threshold should be agreed during physical testing.

Do not invent a final accuracy threshold independently from M4.

---

# 55. Location Update Frequency

Avoid unnecessarily high update frequency.

The exact interval should balance:

```text
responsiveness
battery
accuracy
```

For the one-month student project, simplicity and reliability are more important than micro-optimizing the interval.

---

# 56. Scan Mode Location

Once the user enters Scan Mode:

```text
M3 can provide updated distance
```

to M4 as required.

The scan should not depend on an unnecessarily aggressive global location loop.

---

# 57. Scan End

When:

```text
REVEALED
```

or:

```text
scan cancelled
```

stop location work that is no longer required by the scan.

Coordinate with M4.

---

# 58. Map Camera

The map may:

```text
show campus
center on current location
center on selected relic
```

Avoid repeatedly recentering the camera on every location update unless explicitly desired.

---

# 59. Map Performance

Avoid:

```text
recreating all markers every update
```

Prefer updating only what changed.

---

# 60. Marker Data Consistency

Every marker must map:

```text
marker → relicId
```

correctly.

A marker pointing to R001 must open:

```text
R001
```

not merely:

```text
first relic in list
```

---

# 61. M3 Unit Tests

Minimum:

```text
LOC-001 distance calculation
LOC-002 far state
LOC-003 approaching state
LOC-004 boundary
LOC-005 inside
LOC-006 poor accuracy
```

---

# 62. M3 Geofence Tests

Minimum:

```text
GEO-001 ENTER
GEO-002 EXIT
GEO-003 wrong relic
GEO-004 duplicate ENTER
```

---

# 63. M3 Permission Tests

Minimum:

```text
PERMISSION-001 denied
PERMISSION-002 granted
PERMISSION-003 revoked
```

---

# 64. M3 Map Tests

Minimum:

```text
MAP-001 map loads
MAP-002 markers
MAP-003 current location
MAP-004 permission denied
MAP-005 location unavailable
```

---

# 65. M3 Mock Scenarios

## Scenario A — Far

```text
R001
distance = 150 m
accuracy = 8 m
```

Expected:

```text
Far
```

---

# 66. Scenario B — Approaching

```text
R001
distance = 55 m
accuracy = 8 m
```

Expected:

```text
Approaching
```

---

# 67. Scenario C — Boundary Outside

```text
R001
distance = 26 m
accuracy = 5 m
```

Expected:

```text
Outside
```

---

# 68. Scenario D — Inside

```text
R001
distance = 18 m
accuracy = 5 m
```

Expected:

```text
Inside/scan area
```

---

# 69. Scenario E — Poor Accuracy

```text
R001
distance = 18 m
accuracy = 45 m
```

Expected:

```text
low confidence
```

---

# 70. Scenario F — Unavailable

```text
location = unavailable
```

Expected:

```text
location unavailable
```

---

# 71. M3 Physical Device Testing

At least:

```text
Device A
Device B
```

where available.

Test:

```text
permission
current location
map
distance
geofence
background/foreground
```

---

# 72. GPS Device Test Matrix

| Test | Device A | Device B |
|---|---|---|
| Permission | | |
| Current location | | |
| Accuracy | | |
| R001 distance | | |
| Geofence ENTER | | |
| Geofence EXIT | | |
| Background event | | |

---

# 73. M3 Battery Test

Check:

```text
Map open
Map closed
Scan active
Scan completed
```

Verify that location work changes appropriately.

---

# 74. Battery Red Flag

Investigate if:

```text
location updates continue indefinitely
```

after:

```text
leaving Map
```

or:

```text
completing Scan
```

when no other feature requires them.

---

# 75. Lifecycle Test

Run:

```text
Map
 ↓
background
 ↓
foreground
```

Expected:

```text
location state recovers
no duplicate listeners
```

---

# 76. Rotation/Lifecycle

If the app supports rotation:

```text
Map
 ↓
rotate
```

Expected:

```text
map/location state remains coherent
```

---

# 77. Geofence Persistence

If the app restarts:

```text
registered geofences
```

must behave according to the chosen Android implementation.

M3 should document actual behaviour rather than assuming it.

---

# 78. M3 Handoff to M4

Provide:

```text
DistanceResult
accuracy
relicId
update lifecycle
```

Example:

```text
R001
18 m
5 m accuracy
```

---

# 79. M3 Handoff to M2

Provide:

```text
distance state
geofence ENTER
geofence EXIT
```

M2 uses these to control/present the scan experience.

---

# 80. M3 Handoff to M5/M6

Confirm:

```text
relic coordinate source
relic IDs
radius values
```

---

# 81. M3 Git Branch

Recommended:

```text
feature/location-geofence
```

---

# 82. M3 Commit Examples

```text
feat: integrate Google Maps
feat: add fused location provider
feat: add relic markers
feat: calculate relic distance
feat: register relic geofences
feat: handle geofence enter
fix: handle denied location permission
fix: prevent duplicate geofence events
test: add distance boundary tests
```

---

# 83. M3 PR Checklist

- [ ] Map loads
- [ ] Permission handled
- [ ] Location handled
- [ ] Accuracy handled
- [ ] Distance tested
- [ ] Markers use relic IDs
- [ ] Geofence ENTER tested
- [ ] EXIT tested
- [ ] Duplicate event handled
- [ ] Battery behaviour considered
- [ ] No direct fusion logic
- [ ] Relevant tests pass

---

# 84. M3 Development Schedule — 13 September

Implement:

```text
Map foundation
Location Provider foundation
```

Use mock relics.

---

# 85. 14 September

Implement:

```text
location permission
current location
location state
```

---

# 86. 15 September

Implement:

```text
distance calculation
accuracy handling
relic markers
```

---

# 87. 16 September

Implement:

```text
geofence registration
```

---

# 88. 17 September

Implement:

```text
ENTER/EXIT handling
duplicate protection
event-to-relic mapping
```

---

# 89. 18 September

Test:

```text
mock location
mock geofence
Map
distance
```

---

# 90. 19 September Checkpoint

M3 must demonstrate:

```text
Map
 ↓
Relic marker
 ↓
Mock/current location
 ↓
Distance
 ↓
Geofence ENTER
```

---

# 91. 20 September

Integrate with:

```text
M4 distance input
```

---

# 92. 21 September

Integrate with:

```text
M2 Scan availability
```

---

# 93. 22 September

Physical campus validation.

Measure:

```text
GPS accuracy
geofence behaviour
distance behaviour
```

---

# 94. 23 September

Freeze location architecture.

Only fix:

```text
bugs
calibration
reliability
```

---

# 95. 24 September

Run functional tests.

---

# 96. 25 September

Run Device A/B compatibility tests.

---

# 97. 26 September

Run:

```text
poor GPS
permission
background
restart
```

tests.

---

# 98. 27 September

Regression.

No new location features.

---

# 99. 28 September

Final physical demo verification.

---

# 100. M3 Acceptance — Maps

- [ ] map loads
- [ ] campus visible
- [ ] markers visible
- [ ] marker IDs correct
- [ ] current location works
- [ ] permission handled

---

# 101. M3 Acceptance — Location

- [ ] distance calculated
- [ ] accuracy exposed
- [ ] poor accuracy handled
- [ ] unavailable state handled
- [ ] no fake coordinates

---

# 102. M3 Acceptance — Geofence

- [ ] registration works
- [ ] ENTER works
- [ ] EXIT works
- [ ] correct relic ID retained
- [ ] duplicate event handled
- [ ] ENTER does not reveal relic

---

# 103. M3 Acceptance — Battery

- [ ] location not unnecessarily active
- [ ] Scan lifecycle considered
- [ ] unnecessary updates stop
- [ ] no obvious runaway background processing

---

# 104. M3 Acceptance — Integration

- [ ] M1 Map container integrated
- [ ] M4 distance input integrated
- [ ] M2 scan availability integrated
- [ ] M5/M6 relic data integrated
- [ ] E2E flow tested

---

# 105. Common M3 Risks

## Risk 1 — GPS is inaccurate

Mitigation:

```text
accuracy-aware state
physical calibration
```

## Risk 2 — Geofence is delayed

Mitigation:

```text
test realistic behaviour
do not depend on exact instant transition
```

## Risk 3 — Battery drain

Mitigation:

```text
lifecycle-aware updates
geofence-first strategy
```

## Risk 4 — Coordinates are wrong

Mitigation:

```text
physical validation
```

## Risk 5 — M4 duplicates distance calculation

Mitigation:

```text
shared DistanceResult contract
```

---

# 106. M3 Must Not

Do not:

```text
reveal relic on ENTER
implement sensor fusion
include light/accelerometer logic
write progress directly to Firestore
write progress directly to Room
create duplicate relic datasets
poll GPS constantly without reason
use 0,0 as fake location
```

---

# 107. M3 Should

M3 should:

```text
provide reliable location state
provide distance
provide geofence events
respect permissions
respect lifecycle
consider accuracy
protect battery
coordinate physical testing
```

---

# 108. Final M3 End-to-End Flow

The final demonstration should show:

```text
Open Map
 ↓
See relic
 ↓
Move toward relic
 ↓
Enter geofence
 ↓
Scan becomes available
 ↓
M4 receives distance
 ↓
M2 displays scan
```

M3's responsibility ends at the location/geofence boundary.

---

# 109. Final M3 Checklist

## Maps

- [ ] Google Maps
- [ ] markers
- [ ] selected relic
- [ ] current location

## Location

- [ ] permissions
- [ ] Fused Location Provider
- [ ] distance
- [ ] accuracy
- [ ] unavailable state

## Geofencing

- [ ] registration
- [ ] ENTER
- [ ] EXIT
- [ ] duplicate protection
- [ ] correct relic ID

## Battery

- [ ] lifecycle
- [ ] limited active updates
- [ ] scan cleanup

## Testing

- [ ] mock tests
- [ ] physical GPS test
- [ ] two-device test
- [ ] E2E integration

---

# 110. M3 Definition of Ready

A task is ready when:

- [ ] required coordinates/data known
- [ ] expected location state known
- [ ] consuming module identified
- [ ] mock scenario available
- [ ] acceptance criteria known

---

# 111. M3 Definition of Done

A location feature is done when:

- [ ] implementation works
- [ ] permission state handled
- [ ] unavailable state handled
- [ ] accuracy considered
- [ ] relevant tests pass
- [ ] mock integration works
- [ ] physical device tested where applicable
- [ ] cross-module contract verified
- [ ] PR reviewed
- [ ] documentation updated if contract changed

---

# 112. Final Principle

M3 provides the spatial trigger for Campus Quest:

```text
Where am I?
 ↓
How far is the relic?
 ↓
Am I inside the activation area?
 ↓
Trigger Scan Mode
```

M3 should keep that responsibility reliable and focused.

The actual discovery remains:

```text
Location
+
Light
+
Motion
 ↓
Fusion
 ↓
Proximity
 ↓
Reveal
```

M3 provides the location component; it does not become the entire puzzle system.
