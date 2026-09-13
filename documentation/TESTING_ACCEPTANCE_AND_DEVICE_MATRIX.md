# Campus Quest — Testing, Acceptance & Device Matrix

**Document ID:** CQ-SHARED-06  
**Document:** Testing, Acceptance and Device Matrix  
**Project:** Campus Quest  
**Purpose:** Define the test strategy, acceptance criteria, device matrix, test cases (covering Game Creator wizard, FCM push notifications, dynamic geofencing, sensor fusion, proximity gate, offline Room caching, Firestore sync, and game-specific leaderboards), cross-game isolation tests, and final release checks for the complete Android application  
**Development window:** 13 September 2026 – 28 September 2026  
**Status:** Team execution specification  
**Depends on:** `07_SHARED_CONTRACTS_AND_AGREEMENTS.md`, `08_MOCK_DATA_CATALOG.md`, `09_FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md`, `10_SENSOR_FUSION_AND_LOCATION_SPECIFICATION.md`, `11_INTEGRATION_AND_HANDOFF_PLAN.md`

---

# 1. Purpose

This document defines how the team will determine whether Campus Quest actually works.

The goal is not merely:

```text
The app compiles.
```

The goal is:

```text
The app launches
 ↓
The user can authenticate
 ↓
The user can navigate
 ↓
The user can find a relic
 ↓
Location works
 ↓
Geofence works
 ↓
Scan starts
 ↓
GPS + Light + Accelerometer produce a fusion score
 ↓
Proximity confirms close range
 ↓
Relic is revealed
 ↓
Discovery is saved locally
 ↓
Discovery synchronizes to Firebase
 ↓
Leaderboard updates
```

The project must also behave safely when things go wrong.

---

# 2. Testing Philosophy

Campus Quest is a mobile application that depends on:

- Android lifecycle
- permissions
- GPS
- geofencing
- sensors
- local storage
- Firebase
- network connectivity
- UI state

Therefore testing must cover:

```text
Happy path
+
Invalid input
+
Missing permission
+
Missing hardware
+
Poor location
+
Offline mode
+
Cloud failure
+
Lifecycle changes
+
Multiple devices
```

A feature is not complete simply because its normal case works.

---

# 3. Testing Levels

The team should use several levels of testing.

## Level 1 — Unit Testing

Test isolated logic such as:

```text
distance calculations
light matching
fusion calculations
state transitions
repository logic
validation
```

These tests should not require manually clicking through the entire app.

The module architecture material emphasizes that ViewModel logic can be unit-tested independently of the UI. fileciteturn11file0L102-L106

---

# 4. Level 2 — Component Testing

Test individual application components:

```text
Login
Map
Quest list
Scan
Reveal
Room
Firebase repository
```

Examples:

```text
Can the quest screen display R001?
Can the scan screen display 87%?
Can Room store a discovery?
```

---

# 5. Level 3 — Integration Testing

Test boundaries between members/modules:

```text
M3 → M4
M4 → M2
M5/M6 → M4
M2 → M6
M6 → M5
M1 → M5
```

The purpose is to find contract mismatches.

---

# 6. Level 4 — End-to-End Testing

Test the whole user journey:

```text
Login
 ↓
Map
 ↓
Approach
 ↓
Geofence
 ↓
Scan
 ↓
Fusion
 ↓
Proximity
 ↓
Reveal
 ↓
Save
 ↓
Sync
 ↓
Leaderboard
```

This is the highest-priority acceptance test.

---

# 7. Level 5 — Physical Device Testing

The sensor and GPS features must be tested on actual Android devices.

A simulator/emulator can help with:

```text
UI
navigation
mock location
general application logic
```

but it cannot replace real hardware validation for:

```text
light
accelerometer
proximity
real-world GPS
battery behaviour
```

---

# 8. Test Environment

Record the following for every physical device:

```text
Device:
Manufacturer:
Model:
Android version:
RAM:
Available sensors:
Screen size:
Network:
Date/time:
```

Example:

```text
Device: Device A
Model: __________________
Android: ________________
Sensors: ________________
```

Do not invent device specifications before the team actually tests them.

---

# 9. Minimum Device Matrix

The project should test on **at least two Android devices where possible**.

Recommended:

| Device | Purpose |
|---|---|
| Device A | Primary development/demo device |
| Device B | Compatibility/device variation |
| Device C | Optional additional device |

The important variation is not the brand name.

The important question is:

```text
Does the application behave correctly on more than one real Android device?
```

---

# 10. Sensor Capability Matrix

Record actual capabilities.

| Device | GPS | Light | Accelerometer | Proximity |
|---|---|---|---|---|
| Device A | ✓ | ? | ✓ | ? |
| Device B | ✓ | ? | ✓ | ? |
| Device C | ? | ? | ? | ? |

Replace `?` with:

```text
Available
Unavailable
Unknown
```

after testing.

---

# 11. Why Sensor Availability Matters

Android devices do not necessarily expose the same sensors.

Therefore:

```text
Device A:
Light ✓
Proximity ✓

Device B:
Light ✓
Proximity ✗
```

must not cause the application to crash.

The sensor-fusion specification requires runtime availability detection and graceful degradation.

---

# 12. Test Case Format

Every test should record:

```text
Test ID:
Feature:
Preconditions:
Input:
Steps:
Expected:
Actual:
Status:
Device:
Tester:
Date:
Evidence:
```

Example:

```text
Test ID: LOC-001
Feature: Location permission
Preconditions: App installed
Input: Permission denied
Steps:
1. Launch app
2. Deny location permission
Expected:
Readable permission state
Actual:
__________
Status:
PASS / FAIL
```

---

# 13. Priority Levels

Use:

```text
P0 — Critical
P1 — High
P2 — Medium
P3 — Low
```

## P0

Blocks:

```text
launch
login
core scan
reveal
data persistence
```

Must be fixed.

## P1

Major feature malfunction.

Fix before final demo.

## P2

Non-critical issue.

Fix if time allows.

## P3

Minor cosmetic/polish issue.

Do not allow it to delay core functionality.

---

# 14. Test Status

Use:

```text
NOT RUN
PASS
FAIL
BLOCKED
NOT APPLICABLE
```

Do not mark:

```text
PASS
```

just because the developer believes it should work.

Run the test.

---

# 15. Test Suite Structure

Use these groups:

```text
AUTH
NAV
MAP
LOC
GEO
SENSOR
FUSION
SCAN
REVEAL
ROOM
FIREBASE
SYNC
LEADERBOARD
OFFLINE
LIFECYCLE
PERMISSION
PERFORMANCE
COMPATIBILITY
```

---

# 16. Authentication Test Cases

## AUTH-001 — Valid Login

**Priority:** P0

Preconditions:

```text
Valid development account exists
```

Steps:

```text
1. Launch app
2. Enter valid email
3. Enter valid password
4. Tap Login
```

Expected:

```text
User becomes authenticated
Main application opens
```

---

# 17. AUTH-002 — Invalid Login

**Priority:** P1

Input:

```text
invalid credentials
```

Expected:

```text
Login fails gracefully
Readable error shown
User remains on login screen
```

No crash.

---

# 18. AUTH-003 — Empty Credentials

**Priority:** P1

Input:

```text
email = empty
password = empty
```

Expected:

```text
Validation message
No unnecessary Firebase request where appropriate
```

---

# 19. AUTH-004 — Logout

**Priority:** P1

Steps:

```text
Login
 ↓
Open profile
 ↓
Logout
```

Expected:

```text
Authenticated state ends
Login screen appears
```

---

# 20. AUTH-005 — Authentication Persistence

**Priority:** P1

Steps:

```text
Login
 ↓
Close app
 ↓
Reopen
```

Expected:

```text
Authentication state behaves according to the chosen Firebase Auth configuration
```

The exact persistence behaviour should be documented by M5.

---

# 21. Navigation Test Cases

## NAV-001

Verify:

```text
Login → Main
```

## NAV-002

Verify:

```text
Main → Map
```

## NAV-003

Verify:

```text
Main → Quests
```

## NAV-004

Verify:

```text
Main → Leaderboard
```

## NAV-005

Verify:

```text
Main → Profile
```

## NAV-006

Verify:

```text
Quest → Scan
```

## NAV-007

Verify:

```text
Scan → Reveal
```

No screen should unexpectedly close or return to the wrong destination.

---

# 22. MAP-001 — Map Loads

**Priority:** P1

Steps:

```text
Login
 ↓
Open Map
```

Expected:

```text
Map renders
```

---

# 23. MAP-002 — Relic Markers

Expected:

```text
R001–R006
```

appear according to the current dataset.

Marker IDs must correspond to canonical relic IDs.

---

# 24. MAP-003 — Current Location

Precondition:

```text
Location permission granted
```

Expected:

```text
Current location available
```

---

# 25. MAP-004 — Location Permission Denied

Expected:

```text
No crash
Readable state
Retry/permission path
```

---

# 26. MAP-005 — Location Unavailable

Simulate or reproduce unavailable location.

Expected:

```text
Readable location error
```

The application should not display fabricated location data.

---

# 27. LOC-001 — Distance Calculation

Input:

```text
Current location
R001 coordinates
```

Expected:

```text
Distance returned in metres
```

---

# 28. LOC-002 — Far Distance

Mock:

```text
150 m
```

Expected:

```text
Far state
```

---

# 29. LOC-003 — Approaching

Mock:

```text
55 m
```

Expected:

```text
Approaching state
```

---

# 30. LOC-004 — Boundary

Mock:

```text
26 m
R001 radius = 25 m
```

Expected:

```text
Outside/uncertain state
```

---

# 31. LOC-005 — Inside

Mock:

```text
18 m
```

Expected:

```text
Inside activation area
```

---

# 32. LOC-006 — Poor Accuracy

Input:

```text
distance = 18 m
accuracy = 45 m
```

Expected:

```text
Poor-confidence location state
```

The application should not claim false GPS precision.

---

# 33. GEO-001 — Geofence ENTER

Input:

```text
R001
ENTER
```

Expected:

```text
Scan Mode becomes available/active
```

The geofence itself must not reveal the relic.

---

# 34. GEO-002 — Geofence EXIT

Input:

```text
R001
EXIT
```

Expected:

```text
Active scan handled appropriately
```

Previously discovered relic remains discovered.

---

# 35. GEO-003 — Wrong Relic Event

Input:

```text
ENTER R002
```

while:

```text
R001
```

is selected.

Expected:

```text
Application does not incorrectly reveal R001
```

---

# 36. GEO-004 — Duplicate ENTER

Input:

```text
ENTER R001
ENTER R001
```

Expected:

```text
No duplicate scan/reveal
```

---

# 37. SENSOR-001 — All Sensors Available

Expected:

```text
Normal sensor-fusion operation
```

---

# 38. SENSOR-002 — Light Sensor Unavailable

Expected:

```text
Availability detected
Graceful degradation
No crash
```

---

# 39. SENSOR-003 — Accelerometer Unavailable

Expected:

```text
Availability detected
Graceful degradation
No crash
```

---

# 40. SENSOR-004 — Proximity Unavailable

Expected:

```text
No fabricated proximity value
Clear limited-device behaviour
No crash
```

---

# 41. SENSOR-005 — All Optional Sensors Unavailable

Expected:

```text
Limited/unsupported state
No crash
```

---

# 42. SENSOR-006 — Stationary

Input:

```text
STATIONARY
```

Expected:

```text
Low motion contribution
```

---

# 43. SENSOR-007 — Scanning Motion

Input:

```text
SCANNING
```

Expected:

```text
Higher motion contribution
```

---

# 44. SENSOR-008 — Unstable Motion

Input:

```text
UNSTABLE
```

Expected:

```text
Reduced/appropriate contribution
```

---

# 45. SENSOR-009 — Correct Light

For R001:

```text
180–320 lux
```

Example:

```text
250 lux
```

Expected:

```text
Strong light match
```

---

# 46. SENSOR-010 — Incorrect Light

For R001:

```text
500 lux
```

Expected:

```text
Low/no light match
```

---

# 47. SENSOR-011 — Proximity FAR

Expected:

```text
Reveal blocked
```

---

# 48. SENSOR-012 — Proximity NEAR

Expected:

```text
Proximity condition satisfied
```

but only reveal if the fusion threshold is already reached.

---

# 49. FUSION-001 — Low Score

Input:

```text
score = 20
```

Expected:

```text
Not matched
```

---

# 50. FUSION-002 — Medium Score

Input:

```text
score = 72
```

Expected:

```text
Scan continues
```

---

# 51. FUSION-003 — Threshold

Input:

```text
score = agreed threshold
```

Expected:

```text
READY_FOR_PROXIMITY
```

The threshold must come from the shared configuration.

---

# 52. FUSION-004 — Score Above Threshold

Input:

```text
score = 90+
```

Expected:

```text
READY_FOR_PROXIMITY
```

---

# 53. FUSION-005 — Proximity Blocks Reveal

Input:

```text
fusion = 90
proximity = FAR
```

Expected:

```text
No reveal
```

---

# 54. FUSION-006 — Proximity Allows Reveal

Input:

```text
fusion >= threshold
proximity = NEAR
```

Expected:

```text
Reveal condition satisfied
```

---

# 55. FUSION-007 — Missing Light Sensor

Expected:

```text
Available signals reweighted
```

according to the M4-defined degradation configuration.

---

# 56. FUSION-008 — Missing Motion Sensor

Expected:

```text
Available signals reweighted
```

No crash.

---

# 57. FUSION-009 — Score Range

Run several combinations.

Expected:

```text
0 <= score <= 100
```

No negative percentage.

No percentage above 100.

---

# 58. SCAN-001 — Scan Activation

Input:

```text
Geofence ENTER
```

Expected:

```text
Scan state becomes READY/SCANNING
```

---

# 59. SCAN-002 — Scan Progress

Input:

```text
fusion scores:
20
45
72
87
```

Expected:

```text
UI reflects changing score
```

---

# 60. SCAN-003 — Scan Instructions

Verify that the UI clearly communicates:

```text
what the user should do
```

For example:

```text
move slowly
scan around the area
move closer when prompted
```

The wording can follow the final UI design.

---

# 61. SCAN-004 — Limited Sensor Mode

Expected:

```text
User can understand that the device is operating with limited sensor capability
```

---

# 62. REVEAL-001 — Successful Reveal

Use canonical case:

```text
R001
distance = 18 m
light = 250 lux
motion = SCANNING
fusion = 87
proximity = NEAR
```

Expected:

```text
R001 revealed
```

---

# 63. REVEAL-002 — Reveal Blocked by Fusion

Input:

```text
fusion = 55
proximity = NEAR
```

Expected:

```text
No reveal
```

---

# 64. REVEAL-003 — Reveal Blocked by Proximity

Input:

```text
fusion = 87
proximity = FAR
```

Expected:

```text
No reveal
```

---

# 65. REVEAL-004 — Duplicate Reveal

Discover:

```text
R001
```

then attempt:

```text
R001
```

again.

Expected:

```text
No duplicate progress record
```

---

# 66. ROOM-001 — Save Discovery

Expected:

```text
R001 stored locally
```

---

# 67. ROOM-002 — Read Discovery

Restart the application.

Expected:

```text
R001 still appears discovered
```

---

# 68. ROOM-003 — Pending Sync

When cloud is unavailable:

```text
R001 discovered
```

Expected:

```text
pendingSync = true
```

---

# 69. ROOM-004 — Clear Pending Sync

Restore network.

Expected:

```text
cloud upload succeeds
pendingSync = false
```

---

# 70. ROOM-005 — Duplicate Local Discovery

Insert:

```text
R001
```

twice.

Expected:

```text
one logical discovery
```

---

# 71. FIREBASE-001 — Relic Read

Expected:

```text
R001–R006 available
```

according to the development dataset.

---

# 72. FIREBASE-002 — Relic Light Signature

Request:

```text
R001
```

Expected:

```text
180–320 lux
```

---

# 73. FIREBASE-003 — User Document

Authenticated user:

```text
U001
```

Expected:

```text
/users/{uid}
```

exists according to the configured identity strategy.

---

# 74. FIREBASE-004 — Progress Write

Discover:

```text
R001
```

Expected:

```text
progress/{uid}/found/R001
```

exists.

---

# 75. FIREBASE-005 — Duplicate Progress Write

Write R001 again.

Expected:

```text
No duplicate logical discovery
```

---

# 76. FIREBASE-006 — Unauthorized Progress

Authenticated:

```text
U001
```

attempt:

```text
U002 progress
```

Expected:

```text
Denied
```

---

# 77. FIREBASE-007 — Unauthorized Relic Modification

Authenticated user attempts:

```text
modify R001
```

Expected:

```text
Denied
```

---

# 78. FIREBASE-008 — Unauthorized Leaderboard Manipulation

Attempt to modify:

```text
relicsFound = 999
```

Expected:

```text
Denied or controlled according to the final architecture
```

---

# 79. SYNC-001 — Successful Sync

Initial:

```text
Room pendingSync = true
Internet = ON
```

Expected:

```text
Firestore updated
pendingSync cleared
```

---

# 80. SYNC-002 — Failed Sync

Initial:

```text
pendingSync = true
Internet = OFF
```

Expected:

```text
pendingSync remains true
discovery remains locally available
```

---

# 81. SYNC-003 — Retry

Sequence:

```text
Internet OFF
 ↓
sync fails
 ↓
Internet ON
 ↓
syncPending()
```

Expected:

```text
sync succeeds
```

---

# 82. SYNC-004 — Idempotent Retry

Sequence:

```text
cloud record already exists
Room says pending
```

Expected:

```text
sync completes safely
no duplicate logical record
pendingSync cleared
```

---

# 83. LEADERBOARD-001 — Load Leaderboard

Expected:

```text
users ordered by relicsFound
```

---

# 84. LEADERBOARD-002 — After Discovery

Initial:

```text
U001 = 0
```

Discover:

```text
R001
```

Expected:

```text
U001 = 1
```

according to the final cloud update flow.

---

# 85. LEADERBOARD-003 — Multiple Users

Use:

```text
U001 = 3
U002 = 4
U003 = 5
U004 = 2
U005 = 0
```

Expected descending order:

```text
U003
U002
U001
U004
U005
```

---

# 86. OFFLINE-001 — Offline Launch

Disable network.

Launch app.

Expected:

```text
Application starts if required local data is available
```

---

# 87. OFFLINE-002 — Offline Discovery

Disable network.

Complete a valid scan.

Expected:

```text
Discovery saved locally
pendingSync = true
```

---

# 88. OFFLINE-003 — Reconnect

Restore network.

Expected:

```text
Pending discovery uploads
```

---

# 89. OFFLINE-004 — Firebase Unavailable

Simulate cloud failure.

Expected:

```text
No crash
local data preserved
retry possible
```

---

# 90. PERMISSION-001 — Location Denied

Expected:

```text
Clear permission state
```

---

# 91. PERMISSION-002 — Location Granted

Expected:

```text
Map/location features available
```

---

# 92. PERMISSION-003 — Permission Revoked

Grant permission.

Then revoke it through Android settings.

Return to app.

Expected:

```text
Application detects changed permission state
```

---

# 93. LIFECYCLE-001 — Background/Foreground

Sequence:

```text
Open Scan
 ↓
Background app
 ↓
Return
```

Check:

```text
UI state
sensor listeners
location handling
```

---

# 94. LIFECYCLE-002 — Rotation

If rotation is supported:

```text
Open screen
 ↓
Rotate
```

Expected:

```text
Important state remains coherent
```

The architecture material explains that ViewModel exists partly to survive configuration changes such as rotation. fileciteturn11file0L102-L106

---

# 95. LIFECYCLE-003 — App Restart During Progress

Sequence:

```text
Start scan
 ↓
Close app
 ↓
Reopen
```

Expected:

```text
No corrupted discovery
No duplicate record
```

---

# 96. BATTERY-001 — Sensor Lifecycle

Start Scan Mode.

Verify sensors are active.

Exit Scan Mode.

Verify unnecessary listeners are stopped.

---

# 97. BATTERY-002 — Location Lifecycle

Verify location work is not unnecessarily active when the feature does not need it.

---

# 98. BATTERY-003 — Long Scan

Run Scan Mode for several minutes.

Observe:

```text
unusual battery drain
device heating
```

Record observations.

This does not require laboratory-grade power measurement for the student project; the purpose is to identify obvious inefficient behaviour.

---

# 99. PERFORMANCE-001 — App Launch

Check:

```text
launch time
```

from a clean start.

The team should record observations rather than inventing an arbitrary benchmark.

---

# 100. PERFORMANCE-002 — Quest List

Load all six relics.

Expected:

```text
smooth rendering
no obvious UI freezing
```

---

# 101. PERFORMANCE-003 — Leaderboard

Load the canonical leaderboard.

Expected:

```text
no obvious lag
```

---

# 102. PERFORMANCE-004 — Sensor Processing

During Scan Mode:

```text
UI remains responsive
```

Sensor processing should not block the main UI thread.

---

# 103. COMPATIBILITY-001 — Device A

Run:

```text
authentication
map
scan
reveal
Room
Firebase
```

Record result.

---

# 104. COMPATIBILITY-002 — Device B

Repeat the same core flow.

Compare:

```text
GPS
sensor availability
UI
performance
permissions
```

---

# 105. COMPATIBILITY-003 — Different Sensor Capability

Use a device with a missing sensor where possible.

Verify:

```text
graceful degradation
```

---

# 106. COMPLETE END-TO-END TEST

**Test ID:** E2E-001  
**Priority:** P0

Use:

```text
User: U001
Relic: R001
```

Setup:

```text
Internet ON
Location permission GRANTED
Required sensors AVAILABLE
```

Execute:

```text
1. Launch
2. Login
3. Open Map
4. Locate R001
5. Approach
6. Enter geofence
7. Enter Scan Mode
8. Perform scanning motion
9. Match light environment
10. Reach fusion threshold
11. Bring device close
12. Reveal
13. Save
14. Sync
15. Open leaderboard
```

Expected:

```text
R001 discovered
U001 progress increased
cloud progress updated
leaderboard updated
```

---

# 107. COMPLETE FAILED-SCAN TEST

**Test ID:** E2E-002

Input:

```text
distance = 18 m
light = incorrect
motion = stationary
```

Expected:

```text
No reveal
```

---

# 108. COMPLETE PROXIMITY-BLOCK TEST

**Test ID:** E2E-003

Input:

```text
fusion >= threshold
proximity = FAR
```

Expected:

```text
No reveal
```

---

# 109. COMPLETE OFFLINE TEST

**Test ID:** E2E-004

Setup:

```text
Internet OFF
```

Execute successful scan.

Expected:

```text
Room save
pendingSync = true
```

Restart.

Expected:

```text
Discovery retained
```

Reconnect.

Expected:

```text
sync succeeds
```

---

# 110. COMPLETE SENSOR-DEGRADATION TEST

**Test ID:** E2E-005

Use a device without one supported sensor where possible.

Expected:

```text
application detects missing capability
degradation/fallback occurs
no crash
```

---

# 111. COMPLETE PERMISSION TEST

**Test ID:** E2E-006

Deny location permission.

Expected:

```text
Map/location flow stops safely
```

Then grant permission.

Expected:

```text
location features become usable
```

---

# 112. COMPLETE RESTART TEST

**Test ID:** E2E-007

Sequence:

```text
Login
 ↓
Discover R001
 ↓
Close app
 ↓
Reopen
```

Expected:

```text
authentication/progress state behaves correctly
```

---

# 113. Regression Testing

Whenever a major feature changes, rerun the affected tests.

Example:

If M4 changes fusion:

Run:

```text
FUSION-001
FUSION-002
FUSION-003
FUSION-005
FUSION-006
FUSION-007
FUSION-008
REVEAL-001
REVEAL-002
REVEAL-003
E2E-001
```

Do not test only the changed class.

---

# 114. Regression Rule

After:

```text
critical bug fix
```

run:

```text
the original failing test
+
related tests
+
complete smoke test
```

---

# 115. Daily Smoke Test

From the first integrated build onward, run:

```text
1. Launch
2. Login
3. Open Map
4. Select R001
5. Enter/trigger Scan
6. Produce mock/real successful fusion
7. Reveal
8. Verify Room
9. Verify sync
10. Verify leaderboard
```

This should take only a few minutes once the system is stable.

---

# 116. Smoke Test Definition

A smoke test asks:

> Is the application fundamentally alive?

It is not the complete test suite.

If smoke testing fails:

```text
stop adding features
fix the build/core flow
```

---

# 117. Bug Report Format

Every bug should contain:

```text
Bug ID:
Title:
Priority:
Device:
Android version:
Build:
Preconditions:
Steps to reproduce:
Expected:
Actual:
Screenshot/video:
Logs:
Owner:
Status:
```

---

# 118. Example Bug

```text
Bug ID: BUG-014
Title: R001 does not enter scan mode
Priority: P0
Device: Device A
Preconditions: Location permission granted
Steps:
1. Open Map
2. Approach R001
3. Enter radius
Expected:
Scan Mode available
Actual:
Map remains in approaching state
Owner:
M3
```

This is much more useful than:

```text
"Scan is broken."
```

---

# 119. Bug Priority Rules

## P0

Examples:

```text
app crash on launch
cannot login
cannot reveal any relic
discovery data lost
```

Fix immediately.

## P1

Examples:

```text
leaderboard incorrect
offline sync broken
sensor degradation crashes
```

Fix before final demo.

## P2

Examples:

```text
one non-core UI state incorrect
minor formatting
```

Fix if feasible.

## P3

Examples:

```text
minor spacing
cosmetic polish
```

Do not delay core testing.

---

# 120. Bug Ownership

Assign based on the responsible module:

```text
Auth → M5 with M1
Navigation → M1
Quest UI → M2
Map/GPS → M3
Sensors/Fusion → M4
Room/Sync → M6 with M5
Integration conflict → relevant members together
```

M6 should not automatically become the owner of every bug.

---

# 121. Testing Timeline

## 13–14 Sep

Unit/component foundations.

Test:

```text
models
mock data
navigation skeleton
sensor discovery
repository skeleton
```

---

# 122. 15–17 Sep

Feature-level testing.

M1:

```text
login/navigation
```

M2:

```text
quest/scan UI
```

M3:

```text
map/location
```

M4:

```text
sensor logic
```

M5:

```text
Firebase
```

M6:

```text
Room
```

---

# 123. 18–19 Sep

Cross-module testing.

Focus:

```text
M3 → M4
M4 → M2
M5/M6 → M4
```

---

# 124. 20–21 Sep

Integration testing.

Focus:

```text
Auth
Relic data
Map
Room
Firebase
```

---

# 125. 22 Sep

Full sensor integration test.

Run:

```text
GPS
Light
Motion
Fusion
Proximity
```

on physical devices.

---

# 126. 23 Sep

MVP acceptance test.

Must pass:

```text
Login
Map
Geofence
Scan
Fusion
Proximity
Reveal
Room
Firebase
Leaderboard
```

This is the **feature-complete checkpoint**.

---

# 127. 24 Sep — Functional Testing Day

Focus on:

```text
normal flow
failed flow
duplicate flow
navigation
authentication
```

---

# 128. 25 Sep — Device Testing Day

Focus on:

```text
Device A
Device B
sensor differences
GPS differences
permissions
```

---

# 129. 26 Sep — Resilience Testing Day

Focus on:

```text
offline
Firebase failure
sensor missing
poor GPS
app restart
background/foreground
```

---

# 130. 27 Sep — Regression Day

Run:

```text
complete smoke test
complete end-to-end tests
critical regression suite
```

No new features.

---

# 131. 28 Sep — Final Acceptance

Perform:

```text
final build
final smoke test
final demo rehearsal
final evidence collection
final known-limitations review
```

---

# 132. Acceptance Criteria — Authentication

Pass when:

- [ ] valid login works
- [ ] invalid login handled
- [ ] logout works
- [ ] auth state is reflected in UI
- [ ] no raw Firebase exception is the primary UI
- [ ] authentication survives expected lifecycle behaviour

---

# 133. Acceptance Criteria — Navigation

Pass when:

- [ ] all core destinations open
- [ ] back navigation behaves correctly
- [ ] Scan can be reached
- [ ] Reveal can be reached
- [ ] no dead-end core screen

---

# 134. Acceptance Criteria — Map

Pass when:

- [ ] map loads
- [ ] current location works
- [ ] relic markers display
- [ ] selected relic can be identified
- [ ] permission denial is handled
- [ ] poor location state is handled

---

# 135. Acceptance Criteria — Geofencing

Pass when:

- [ ] geofences register
- [ ] ENTER is detected
- [ ] Scan Mode activates
- [ ] EXIT is handled
- [ ] geofence does not itself reveal relic
- [ ] duplicate events do not create duplicate discovery

---

# 136. Acceptance Criteria — Sensor Fusion

Pass when:

- [ ] GPS contributes
- [ ] light contributes
- [ ] accelerometer contributes
- [ ] score is 0–100
- [ ] threshold is centralized
- [ ] proximity is separate from weighted fusion
- [ ] missing sensors are handled
- [ ] listeners are lifecycle-safe

---

# 137. Acceptance Criteria — Proximity

Pass when:

```text
fusion below threshold + near
```

does not reveal.

and:

```text
fusion at/above threshold + near
```

can reveal.

Also:

```text
fusion at/above threshold + far
```

must not reveal.

---

# 138. Acceptance Criteria — Room

Pass when:

- [ ] discovery is saved locally
- [ ] duplicate discovery is prevented
- [ ] progress survives restart
- [ ] pending sync is represented
- [ ] local data is available offline

---

# 139. Acceptance Criteria — Firebase

Pass when:

- [ ] Auth works
- [ ] relics load
- [ ] progress writes
- [ ] duplicate progress is safe
- [ ] leaderboard works
- [ ] security rules are tested
- [ ] cloud failure is handled

---

# 140. Acceptance Criteria — Synchronization

Pass when:

```text
online:
Room → Firestore → success

offline:
Room → pendingSync

reconnect:
pendingSync → Firestore → cleared
```

---

# 141. Acceptance Criteria — Leaderboard

Pass when:

- [ ] leaderboard loads
- [ ] values correspond to progress
- [ ] ordering is consistent
- [ ] users cannot arbitrarily manipulate counts
- [ ] UI refreshes appropriately

---

# 142. Acceptance Criteria — Offline

Pass when:

- [ ] application handles loss of connectivity
- [ ] discovery is not lost
- [ ] pending sync is retained
- [ ] retry works
- [ ] no duplicate cloud record is created

---

# 143. Acceptance Criteria — Device Compatibility

Pass when:

- [ ] two Android devices tested where possible
- [ ] core flow works on both
- [ ] sensor differences handled
- [ ] permission differences handled
- [ ] no device-specific crash blocks demo

---

# 144. Acceptance Criteria — Battery

Pass when:

- [ ] unnecessary sensor listeners stop
- [ ] unnecessary location work stops
- [ ] no obvious runaway background processing
- [ ] Scan Mode does not remain active indefinitely after completion

---

# 145. Evidence Matrix

| Area | Evidence |
|---|---|
| Login | Screenshot/video |
| Navigation | Screenshot/video |
| Map | Screenshot |
| Location | Device screenshot/video |
| Geofence | Test record/video |
| Sensors | Sensor debug evidence |
| Fusion | Fusion meter/video |
| Proximity | Scan/reveal video |
| Room | Database/test evidence |
| Firebase | Console/test evidence |
| Security | Rule test evidence |
| Offline | Video/test record |
| Leaderboard | Screenshot |
| Compatibility | Device test matrix |

---

# 146. Evidence Rule

Evidence should prove the feature actually works.

Weak evidence:

```text
Code screenshot
```

Better evidence:

```text
Application running
 ↓
real interaction
 ↓
expected result
```

For hardware features, video is often more convincing than a static screenshot.

---

# 147. Test Data

Use:

```text
08_MOCK_DATA_CATALOG.md
```

as the canonical test-data source.

Primary:

```text
U001
R001
```

for the main end-to-end scenario.

---

# 148. Test Data Change Rule

If the physical test requires a change to:

```text
coordinates
radius
light signature
threshold
```

update:

```text
mock catalog
Firebase seed data
tests
documentation
```

Do not silently change only one implementation.

---

# 149. Test Environment Reset

Before repeatable integration tests, reset where necessary:

```text
user progress
Room records
pendingSync
Firebase test records
leaderboard
```

Otherwise a previous successful discovery may make a later test appear to pass incorrectly.

---

# 150. Fresh User Test

Use:

```text
U005
```

for a clean-state test.

Expected:

```text
0 discovered
```

before beginning.

---

# 151. Multi-Discovery Test

Use:

```text
U001
```

and discover:

```text
R001
R002
R005
```

Expected:

```text
3 discoveries
```

and no duplicates.

---

# 152. Full Relic Dataset Test

Verify:

```text
R001
R002
R003
R004
R005
R006
```

can all be loaded and displayed.

This does not mean all six must be physically completed during every regression run.

---

# 153. Physical Calibration Record

For each physical relic location, record:

```text
Relic:
Date:
Device:
GPS accuracy:
Observed distance:
Light reading:
Motion behaviour:
Proximity behaviour:
Fusion score:
Result:
```

---

# 154. Example Calibration Record

```text
Relic: R001
Date: 2026-09-22
Device: Device A
GPS accuracy: ______
Observed distance: ______
Light reading: ______
Motion behaviour: ______
Proximity behaviour: ______
Fusion score: ______
Result: PASS / FAIL
```

---

# 155. Sensor Comparison Record

For each device:

```text
Light sensor:
Accelerometer:
Proximity:
GPS:
```

Record:

```text
available?
behaviour?
usable?
```

This creates evidence for graceful degradation.

---

# 156. Final Device Matrix Template

| Test | Device A | Device B | Device C |
|---|---|---|---|
| Launch | | | |
| Login | | | |
| Map | | | |
| GPS | | | |
| Geofence | | | |
| Light | | | |
| Accelerometer | | | |
| Proximity | | | |
| Fusion | | | |
| Reveal | | | |
| Room | | | |
| Firebase | | | |
| Offline | | | |
| Restart | | | |

Use:

```text
PASS
FAIL
N/A
```

---

# 157. Final Acceptance Run

On 28 September, perform the final run from a clean or deliberately prepared test state.

Use:

```text
U001
R001
```

Execute:

```text
Login
Map
Approach
Geofence
Scan
Fusion
Proximity
Reveal
Room
Sync
Leaderboard
```

Record:

```text
PASS / FAIL
```

---

# 158. Final Demo Readiness Criteria

The project is demo-ready only if:

- [ ] app launches reliably
- [ ] login works
- [ ] main navigation works
- [ ] map works
- [ ] at least one relic works end-to-end
- [ ] geofence works
- [ ] fusion works
- [ ] proximity works
- [ ] reveal works
- [ ] Room works
- [ ] Firebase works
- [ ] leaderboard works
- [ ] no known P0 issues
- [ ] no unresolved data-loss P1 issue
- [ ] primary demo device validated

---

# 159. Demo Device Preparation

Before the final demonstration:

```text
Charge device
Enable required connectivity
Grant required permissions
Verify location
Verify sensors
Verify Firebase
Clear/reset test data if required
Run one complete smoke test
```

Do not discover on demo morning that:

```text
location permission is denied
```

or:

```text
Firebase is pointing to the wrong project
```

---

# 160. Demo Recovery Plan

If the physical sensor environment becomes unreliable:

```text
1. Do not panic
2. Verify sensor availability
3. Verify location
4. Use the prepared test device
5. Use the agreed mock/demo fallback only if permitted by the final demonstration plan
6. Explain limitations honestly
```

Do not modify the architecture immediately before the presentation.

---

# 161. Known Limitations

Document legitimate limitations such as:

```text
GPS accuracy varies
sensor availability varies by device
light conditions vary
geofence transitions may not be instantaneous
network availability affects sync timing
```

A limitation is acceptable if:

```text
it is understood
it is handled safely
it does not destroy the core demonstration
```

---

# 162. Final Testing Responsibilities

## M1

Own:

```text
navigation
auth UI
screen states
lifecycle/UI behaviour
```

## M2

Own:

```text
quest UI
scan UI
reveal UI
leaderboard presentation
```

## M3

Own:

```text
GPS
map
geofence
location permissions
```

## M4

Own:

```text
sensors
fusion
proximity
degradation
```

## M5

Own:

```text
Firebase
Auth backend
Firestore
security
cloud errors
```

## M6

Own:

```text
Room
offline
sync
integration coordination
```

---

# 163. Testing Communication

When reporting a failed test:

```text
[TEST FAIL]

Test ID:
Device:
Expected:
Actual:
Screenshot/video:
Likely module:
Owner:
```

Example:

```text
[TEST FAIL]

Test ID: FUSION-006
Device: Device B
Expected: Reveal allowed
Actual: Scan remains at 87%
Screenshot: attached
Likely module: M4
Owner: M4
```

---

# 164. Daily Test Reporting

From 20 September onward, each member should report:

```text
Tests run:
Passed:
Failed:
Blocked:
New bugs:
Fixed bugs:
```

This keeps the team aware of stability.

---

# 165. Testing Stop Rule

If a change breaks a P0 flow:

```text
stop feature development
 ↓
fix P0
 ↓
rerun smoke test
 ↓
continue
```

Do not continue adding features on top of a broken core build.

---

# 166. Final Regression Suite

Before submission, run at minimum:

```text
AUTH-001
AUTH-002
AUTH-004

MAP-001
MAP-002
MAP-003
MAP-004

LOC-001
LOC-005
LOC-006

GEO-001
GEO-002

SENSOR-001
SENSOR-002
SENSOR-003
SENSOR-004
SENSOR-006
SENSOR-007
SENSOR-009
SENSOR-010
SENSOR-011
SENSOR-012

FUSION-003
FUSION-005
FUSION-006
FUSION-007
FUSION-008
FUSION-009

REVEAL-001
REVEAL-002
REVEAL-003
REVEAL-004

ROOM-001
ROOM-002
ROOM-003
ROOM-004
ROOM-005

FIREBASE-001
FIREBASE-004
FIREBASE-006
FIREBASE-007
FIREBASE-008

SYNC-001
SYNC-002
SYNC-003
SYNC-004

LEADERBOARD-001
LEADERBOARD-002

OFFLINE-002
OFFLINE-003

LIFECYCLE-001
LIFECYCLE-003

BATTERY-001
BATTERY-002

E2E-001
E2E-004
E2E-005
E2E-006
```

---

# 167. Final P0 Checklist

Before final submission:

- [ ] no launch crash
- [ ] login works
- [ ] map works
- [ ] location works
- [ ] geofence works
- [ ] scan starts
- [ ] fusion works
- [ ] proximity works
- [ ] relic can be revealed
- [ ] discovery is saved
- [ ] discovery is not lost offline
- [ ] cloud sync works
- [ ] leaderboard works

---

# 168. Final P1 Checklist

- [ ] invalid login handled
- [ ] permission denial handled
- [ ] poor GPS handled
- [ ] missing light sensor handled
- [ ] missing accelerometer handled
- [ ] missing proximity handled
- [ ] duplicate discovery handled
- [ ] Firebase failure handled
- [ ] retry works
- [ ] app restart works
- [ ] background/foreground tested
- [ ] two-device compatibility tested

---

# 169. Final Test Archive

Keep:

```text
test matrix
bug list
screenshots
videos
logs where useful
device information
calibration records
security test results
final acceptance result
```

This becomes useful evidence for the project report and presentation.

---

# 170. Final Test Report Structure

Recommended final report section:

```text
1. Testing approach
2. Test environment
3. Device matrix
4. Functional testing
5. Sensor testing
6. Location testing
7. Integration testing
8. Offline/sync testing
9. Security testing
10. Compatibility testing
11. Defects found
12. Defects resolved
13. Known limitations
14. Final acceptance result
```

---

# 171. Final Testing Principle

The team should prove:

```text
Works normally
+
Fails safely
+
Recovers correctly
+
Works on more than one device
```

not merely:

```text
"It worked once on my phone."
```

---

# 172. Final Acceptance Statement

Campus Quest should be considered accepted only when the team can demonstrate a reliable core journey:

```text
Authenticated user
       ↓
Campus map
       ↓
Relic location
       ↓
Geofence entry
       ↓
Sensor-fusion scan
       ↓
Proximity confirmation
       ↓
Relic reveal
       ↓
Local persistence
       ↓
Cloud synchronization
       ↓
Leaderboard update
```

and can show that major failure conditions do not cause data loss or application crashes.

This is the final testing standard for the project.
