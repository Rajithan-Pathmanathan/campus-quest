# Campus Quest — Integration & Handoff Plan

**Document ID:** CQ-SHARED-05  
**Document:** Integration and Handoff Plan  
**Project:** Campus Quest  
**Purpose:** Define exactly how six parallel workstreams become one working Android application without a last-minute integration bottleneck  
**Development window:** 13 September 2026 – 28 September 2026  
**Status:** Team execution specification  
**Depends on:** `07_SHARED_CONTRACTS_AND_AGREEMENTS.md`, `08_MOCK_DATA_CATALOG.md`, `09_FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md`, `10_SENSOR_FUSION_AND_LOCATION_SPECIFICATION.md`

---

# 1. Purpose

This document defines the team's integration strategy.

The most important rule is:

> **Do not wait until the end to combine six completed modules.**

Campus Quest contains tightly connected features:

```text
Authentication
     ↓
Map
     ↓
Location
     ↓
Geofence
     ↓
Scan Mode
     ↓
Sensor Fusion
     ↓
Proximity
     ↓
Reveal
     ↓
Room
     ↓
Firestore
     ↓
Leaderboard
```

If these are integrated only after every member says "my part is finished", the team can discover interface, lifecycle, permissions, data-model, and hardware problems too late.

The integration strategy therefore uses:

```text
parallel development
+
shared contracts
+
mock-first implementation
+
small integration checkpoints
+
continuous testing
+
feature freeze on 23 September
```

---

# 2. Integration Philosophy

The project uses an **Agile-inspired iterative development approach**.

The practical pattern is:

```text
Build a small piece
      ↓
Test it
      ↓
Integrate it
      ↓
Build the next piece
      ↓
Test again
```

Not:

```text
M1 finishes everything
M2 finishes everything
M3 finishes everything
M4 finishes everything
M5 finishes everything
M6 finishes everything
      ↓
Try to combine everything on Sep 23
```

---

# 3. Six-Person Integration Structure

```text
                       SHARED CONTRACTS
                              │
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
   APPLICATION             DEVICE                 DATA
    M1 + M2                M3 + M4               M5 + M6
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ↓
                         INTEGRATION
```

Each pair has an internal dependency.

Cross-pair integration begins early.

---

# 4. Member Roles During Integration

## M1 — App Shell

Provides:

```text
navigation
authentication UI
main screen structure
profile
theme
screen transitions
```

Receives:

```text
AuthState
User
```

---

## M2 — Quest and Scan UI

Provides:

```text
quest list
quest details
scan screen
fusion meter
reveal screen
lore
leaderboard presentation
```

Receives:

```text
Relic
FusionResult
ScanState
Progress
LeaderboardEntry
```

---

## M3 — Location

Provides:

```text
current location
distance
map markers
geofence events
location state
```

Receives:

```text
Relic
```

---

## M4 — Sensors

Provides:

```text
sensor availability
motion state
light matching
fusion score
proximity state
scan state
```

Receives:

```text
Relic
LightSignature
DistanceResult
```

---

## M5 — Firebase

Provides:

```text
authentication backend
relic data
cloud progress
leaderboard
cloud sync operations
```

---

## M6 — Room/Data Integration

Provides:

```text
Room database
local relic/progress persistence
pendingSync
offline behaviour
integration support
```

---

# 5. Integration Ownership

Integration is **not M6's responsibility alone**.

M6 coordinates integration activities, but:

```text
Each member integrates their own code.
```

For example:

```text
M3 does not hand an unfinished location module to M6
and expect M6 to make it work.

M3 integrates location into the shared application.

M4 integrates sensor fusion into the shared application.

M5 integrates Firebase.

M6 integrates Room/sync.
```

M6's job is to help coordinate, identify conflicts, stabilize builds, and verify cross-module behaviour.

---

# 6. Definition of an Integration-Ready Feature

A feature is ready to integrate only when:

- [ ] it compiles
- [ ] it runs independently
- [ ] it uses the agreed contract
- [ ] it uses canonical mock data
- [ ] it handles its basic error state
- [ ] it does not hard-code another member's implementation
- [ ] it has been tested locally
- [ ] its branch is up to date
- [ ] the member can explain how the feature works
- [ ] the member has told dependent members about the handoff

---

# 7. Handoff Package

Every handoff should contain:

```text
1. Feature name
2. What it does
3. Input contract
4. Output contract
5. Files/classes added
6. Mock data used
7. Error states
8. How to test it
9. Known limitations
10. Dependencies
```

Example:

```text
Feature:
SensorFusionEngine

Input:
DistanceResult
LightSensor value
MotionState

Output:
FusionResult

Test:
R001
250 lux
SCANNING
18 m

Expected:
high fusion score
```

---

# 8. Contract-First Rule

Before integration, both sides must agree on the interface.

Example:

```text
M3
     ↓
DistanceResult
     ↓
M4
```

Both members must agree on:

```text
distance unit = metres
accuracy unit = metres
```

Do not integrate based on assumptions.

---

# 9. Mock-First Rule

Every integration boundary should work with mock data before the real dependency is connected.

Example:

```text
M2
 ↓
Mock FusionResult
 ↓
Scan UI
```

Then:

```text
M4
 ↓
Real FusionResult
 ↓
Scan UI
```

If the UI works with the mock contract, replacing the source should be much easier.

---

# 10. Integration Boundary Map

The major boundaries are:

```text
M1 ↔ M5
Authentication

M2 ↔ M5/M6
Relics / progress / leaderboard

M3 ↔ M5/M6
Relic coordinates

M3 ↔ M4
Distance

M4 ↔ M5/M6
Light signature / relic

M4 ↔ M2
FusionResult / ScanState

M2 ↔ M6
Record reveal

M5 ↔ M6
Cloud sync
```

---

# 11. Boundary 1 — M1 ↔ M5

## Purpose

Authentication.

Flow:

```text
Login screen
 ↓
M1 ViewModel
 ↓
AuthRepository
 ↓
Firebase Authentication
 ↓
AuthState
 ↓
M1 UI
```

M1 should not call Firebase SDK directly.

---

# 12. Boundary 2 — M2 ↔ M5/M6

## Purpose

Quest and progress.

Flow:

```text
Repository
 ↓
Relic
 ↓
Quest screen
```

and:

```text
Relic revealed
 ↓
recordReveal()
 ↓
Room
 ↓
Firestore
```

---

# 13. Boundary 3 — M3 ↔ M5/M6

M3 needs:

```text
Relic coordinates
radius
```

Flow:

```text
Repository
 ↓
Relic
 ↓
Map
```

M3 does not need to know whether the Relic came from:

```text
Mock
Room
Firestore
```

---

# 14. Boundary 4 — M3 ↔ M4

This is one of the most important technical boundaries.

M3 provides:

```text
DistanceResult
```

M4 consumes it.

Example:

```text
distanceM = 18
accuracyM = 6
```

Flow:

```text
GPS
 ↓
M3
 ↓
DistanceResult
 ↓
M4
 ↓
FusionEngine
```

---

# 15. Boundary 5 — M4 ↔ M2

M4 provides:

```text
FusionResult
ScanState
SensorAvailability
```

M2 displays them.

Example:

```text
score = 87
state = READY_FOR_PROXIMITY
```

M2 should not recalculate the score.

---

# 16. Boundary 6 — M4 ↔ M5/M6

M4 needs:

```text
LightSignature
```

Flow:

```text
Repository
 ↓
Relic
 ↓
M4
 ↓
Light matching
```

The source can initially be:

```text
MockQuestRepository
```

and later:

```text
Room/Firebase repository
```

---

# 17. Boundary 7 — M2 ↔ M6

When the user successfully reveals a relic:

```text
M2/ViewModel
 ↓
recordReveal(R001)
 ↓
Repository
 ↓
Room
```

M2 should not insert directly into a Room DAO.

---

# 18. Boundary 8 — M5 ↔ M6

Cloud/local synchronization:

```text
Room
 ↓
pendingSync
 ↓
Repository
 ↓
Firestore
```

M6 owns local persistence.

M5 owns cloud persistence.

They must agree on:

```text
relicId
foundAt
userId
success/failure
retry
duplicate handling
```

---

# 19. Integration Phases

The integration schedule has five phases:

```text
Phase 1 — Foundation
13–14 Sep

Phase 2 — Feature Development
15–19 Sep

Phase 3 — Incremental Integration
20–23 Sep

Phase 4 — Testing and Stabilization
24–27 Sep

Phase 5 — Finalization
28 Sep
```

---

# 20. Phase 1 — Foundation

## 13 September

Every member establishes:

```text
branch
module/package structure
shared contracts
mock data
basic compile
```

M6 checks:

```text
project builds
```

---

# 21. 13 Sep Integration Check

At the end of the day:

```text
git clone works
Android Studio opens project
Gradle builds
application launches
shared models compile
```

Do not wait until Sep 20 to discover a build configuration problem.

---

# 22. 14 September

The team verifies:

```text
M1 navigation skeleton
M3 map skeleton
M4 sensor skeleton
M5 Firebase initialization
M6 Room skeleton
```

No full integration is expected yet.

But every subsystem should be independently runnable.

---

# 23. Phase 2 — Feature Development

## 15–17 September

Members work in parallel.

M1:

```text
app shell
login
navigation
```

M2:

```text
quest UI
scan UI
```

M3:

```text
GPS
map
distance
```

M4:

```text
sensors
fusion
```

M5:

```text
Auth
Firestore
```

M6:

```text
Room
repository
offline
```

---

# 24. First Cross-Team Integration

Target:

**17 September**

Connect one simple path:

```text
Mock Relic
 ↓
Repository
 ↓
Map
```

This tests the domain model and repository boundary.

---

# 25. Second Cross-Team Integration

Target:

**18 September**

Connect:

```text
Mock Distance
 ↓
Mock Fusion
 ↓
Scan UI
```

Expected:

```text
18 m
87%
```

appears correctly in the application.

This is a deliberately artificial integration test.

---

# 26. Third Cross-Team Integration

Target:

**19 September**

Connect:

```text
Geofence ENTER
 ↓
Scan Mode
```

The goal is not yet complete sensor fusion.

The goal is to prove:

```text
location event
```

can correctly trigger:

```text
scan state
```

---

# 27. Phase 3 — Incremental Integration

## 20 September

Integrate:

```text
M1 + M5
```

Authentication.

Verify:

```text
Login
 ↓
Authenticated
 ↓
Main app
```

---

# 28. 20 September — Data Integration

Also integrate:

```text
M2 + M5/M6
```

Verify:

```text
Relic list
 ↓
Quest details
```

using real repository data.

---

# 29. 21 September — Location Integration

Integrate:

```text
M3 + repository
```

Verify:

```text
Firebase/Room relic
 ↓
Map marker
 ↓
distance
```

No hard-coded map relics should remain in the production path.

---

# 30. 21 September — Sensor Integration

Integrate:

```text
M3
 ↓
DistanceResult
 ↓
M4
```

Then:

```text
M4
 ↓
FusionResult
 ↓
M2
```

---

# 31. 22 September — Full Scan Integration

Target flow:

```text
Relic
 ↓
Map
 ↓
Distance
 ↓
Geofence ENTER
 ↓
Scan
 ↓
GPS
 ↓
Light
 ↓
Motion
 ↓
Fusion
 ↓
Threshold
 ↓
Proximity
 ↓
Reveal
```

This is the most important integration day.

---

# 32. 22 September — Calibration

The team should test the scan at the selected physical campus location.

Record:

```text
GPS accuracy
distance
light value
motion behaviour
fusion score
proximity behaviour
```

M3 and M4 adjust only through the agreed change-control process.

---

# 33. 23 September — End-to-End MVP

By the end of:

**23 September**

the complete core loop must work:

```text
Login
 ↓
Map
 ↓
Find relic
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

---

# 34. 23 September — HARD FEATURE FREEZE

After 23 September:

```text
NO new core features.
```

Only:

```text
bug fixes
usability fixes
performance fixes
compatibility fixes
documentation
testing
```

No new architecture.

No new database redesign.

No new sensor mechanic.

No new navigation redesign.

---

# 35. Why Feature Freeze Is Necessary

Without a freeze:

```text
Sep 24
new feature

Sep 25
new feature breaks integration

Sep 26
fix

Sep 27
new bug

Sep 28
demo
```

The team needs four full days of stability work.

---

# 36. Integration Test Order

Always test from the outside toward the core:

```text
1. App launches
2. Login
3. Navigation
4. Map
5. Relic data
6. Location
7. Geofence
8. Scan
9. Fusion
10. Proximity
11. Reveal
12. Room
13. Firestore
14. Leaderboard
```

This makes failures easier to localize.

---

# 37. Integration Failure Diagnosis

If the scan does not start:

Check:

```text
permission
 ↓
location
 ↓
geofence
 ↓
ScanState
```

If the fusion does not change:

Check:

```text
sensor availability
 ↓
M3 distance
 ↓
light
 ↓
motion
 ↓
FusionEngine
```

If reveal does not happen:

Check:

```text
fusion threshold
 ↓
proximity
 ↓
ScanState
```

If discovery disappears:

Check:

```text
Room
 ↓
pendingSync
 ↓
Firestore
```

---

# 38. One Boundary at a Time

Do not connect everything simultaneously.

Bad:

```text
GPS
Firebase
Room
sensors
UI
leaderboard
```

all on one branch.

Better:

```text
Relic → Map
```

then:

```text
Distance → Fusion
```

then:

```text
Fusion → Scan UI
```

then:

```text
Reveal → Room
```

then:

```text
Room → Firebase
```

---

# 39. Integration Branch Strategy

Recommended:

```text
main
│
├── feature/ui-navigation
├── feature/quest-scan-ui
├── feature/location-geofence
├── feature/sensor-fusion
├── feature/firebase-cloud
└── feature/room-data
```

Each member works primarily on their own feature branch.

---

# 40. Pull Request Rule

Use:

```text
Feature branch
 ↓
Commit
 ↓
Push
 ↓
Pull Request
 ↓
Review
 ↓
Merge
```

Do not use:

```text
direct push to main
```

---

# 41. Before Opening a Pull Request

The developer should:

```text
1. Pull/rebase latest main as agreed
2. Resolve conflicts locally
3. Build project
4. Run relevant tests
5. Test feature
6. Check no debug credentials are committed
7. Check no temporary hard-coded data is unintentionally used
8. Open PR
```

---

# 42. Reviewer Checklist

Reviewer checks:

- [ ] project builds
- [ ] shared contract respected
- [ ] correct IDs
- [ ] no direct Firebase from UI
- [ ] no direct Room DAO from UI
- [ ] no unrelated changes
- [ ] lifecycle handled
- [ ] permissions handled
- [ ] errors handled
- [ ] mock data consistent
- [ ] feature tested

---

# 43. Integration Order by Member

## First

```text
M1 + M5
```

Authentication.

## Second

```text
M2 + M5/M6
```

Quest data.

## Third

```text
M3 + M5/M6
```

Relic coordinates.

## Fourth

```text
M3 + M4
```

GPS to fusion.

## Fifth

```text
M4 + M2
```

Fusion to UI.

## Sixth

```text
M2 + M6
```

Reveal to local storage.

## Seventh

```text
M6 + M5
```

Local to cloud.

---

# 44. Full Integration Dependency Graph

```text
                 Firebase/Auth
                     │
                     ↓
                    M1
                     │
                     ↓
                  Main App
                     │
          ┌──────────┴──────────┐
          ↓                     ↓
         M3                     M2
      Location              Quest UI
          │                     │
          ↓                     ↓
      Distance              Scan UI
          │                     ↑
          ↓                     │
         M4 ────────────────────┘
      Sensor Fusion
          │
          ↓
      Proximity Gate
          │
          ↓
        Reveal
          │
          ↓
         M6
        Room
          │
          ↓
         M5
      Firestore
          │
          ↓
     Leaderboard
          │
          ↓
         M2
```

---

# 45. Integration Environment

The team should maintain one agreed development environment.

At minimum document:

```text
Android Studio version
JDK version
Gradle/AGP version
compile SDK
minimum SDK
Firebase project
Google Maps configuration
package/application ID
```

Do not let each member independently upgrade major build dependencies during integration week.

---

# 46. Dependency Freeze

After:

**20 September**

avoid unnecessary changes to:

```text
Gradle
AGP
Kotlin
Android SDK
major libraries
Firebase dependencies
Maps dependencies
Room dependencies
```

unless fixing a blocking issue.

---

# 47. Shared Package Structure

A reasonable conceptual structure is:

```text
com.campusquest
├── data
│   ├── local
│   ├── remote
│   ├── repository
│   └── mock
├── domain
│   ├── model
│   ├── location
│   └── sensor
├── ui
│   ├── auth
│   ├── map
│   ├── quest
│   ├── scan
│   ├── leaderboard
│   └── profile
└── util
```

The exact package structure may differ.

The principle is separation of concerns.

The module architecture material specifically teaches separation between UI, ViewModel/logic, and data/repository responsibilities. fileciteturn11file0L178-L190

---

# 48. ViewModel Integration Rule

ViewModels should expose observable UI state.

Conceptually:

```text
Repository
 ↓
ViewModel
 ↓
StateFlow
 ↓
UI
```

The architecture material identifies observable state such as `LiveData` or `StateFlow` as a mechanism for keeping UI state synchronized with the ViewModel. fileciteturn11file0L102-L106

---

# 49. Avoid Activity/Fragment Business Logic

Do not put:

```text
fusion calculation
Firestore queries
Room insertion
geofence business rules
```

inside Activities/Fragments.

Activities/Fragments should primarily:

```text
display state
forward user actions
observe ViewModel
```

This follows the module's separation-of-concerns principle. fileciteturn11file0L178-L183

---

# 50. Integration With XML UI

If the team uses XML/View-based layouts, keep the UI layer responsible for presentation.

The Android layout guide identifies ConstraintLayout as a modern option for responsive interfaces and notes that FrameLayout is useful for fragment/overlay containers. fileciteturn11file1L341-L378

Do not mix layout implementation with data-access logic.

---

# 51. Integration With RecyclerView

For quest and leaderboard lists:

```text
Repository
 ↓
ViewModel
 ↓
List state
 ↓
RecyclerView adapter
 ↓
UI
```

The adapter should not query Firebase or Room.

---

# 52. Scan Integration Contract

The scan screen should conceptually consume:

```kotlin
data class ScanUiState(
    val relic: Relic?,
    val scanState: ScanState,
    val fusionScore: Int,
    val proximity: Proximity,
    val sensorAvailability: SensorAvailability
)
```

The exact model may be adjusted.

M2 owns presentation.

M4 owns sensor-derived values.

---

# 53. Reveal Integration

The reveal should happen only when the required conditions are satisfied.

Conceptually:

```text
fusionScore >= threshold
AND
proximity == NEAR
```

Then:

```text
Reveal
 ↓
recordReveal(relicId)
```

Do not let M2 create a fake successful discovery simply because the user tapped a button.

A UI-only demo button may exist during mock development but must not remain the real success path.

---

# 54. Room Integration

M6 should provide:

```text
RelicDao
FoundRelicDao
```

through repository-level operations.

The UI should never do:

```kotlin
database.foundRelicDao().insert(...)
```

directly.

---

# 55. Firebase Integration

M5 provides the cloud implementation behind the repository.

M6 provides the local implementation.

The application should be able to switch:

```text
Mock
```

to:

```text
Room + Firebase
```

without rewriting screen logic.

---

# 56. Local-First Discovery Integration

The final flow should be:

```text
Reveal
 ↓
recordReveal()
 ↓
Room insert
 ↓
UI updates immediately
 ↓
pendingSync = true
 ↓
cloud upload
 ↓
success
 ↓
pendingSync = false
```

If upload fails:

```text
pendingSync remains true
```

---

# 57. Leaderboard Integration

After cloud progress is successfully recorded:

```text
progress
 ↓
leaderboard update
 ↓
observeLeaderboard()
 ↓
M2
 ↓
leaderboard UI
```

The team must avoid multiple competing leaderboard calculations.

---

# 58. Integration Test: R001

Use:

```text
R001
Founder's Bell
```

Scenario:

```text
User U001
Distance 18 m
Light 250 lux
Motion SCANNING
Fusion 87
Proximity NEAR
```

Expected:

```text
Reveal R001
```

Then:

```text
Room contains R001
pendingSync handled
Firestore contains R001
leaderboard count increases
```

---

# 59. Integration Test: Failed Reveal

Use:

```text
Distance 18 m
Light 250 lux
Motion SCANNING
Fusion 87
Proximity FAR
```

Expected:

```text
No reveal
No discovery record
```

---

# 60. Integration Test: Offline Reveal

Use:

```text
Internet OFF
```

Successful scan:

```text
R001 revealed
```

Expected:

```text
Room:
R001
pendingSync = true
```

Close/reopen app.

Expected:

```text
R001 remains discovered
```

Restore internet.

Expected:

```text
sync succeeds
pendingSync = false
```

---

# 61. Integration Test: Missing Sensor

Use a device without the required sensor where possible.

Expected:

```text
Sensor availability detected
 ↓
degraded mode/fallback
 ↓
no crash
```

Do not claim a sensor was used if the hardware is absent.

---

# 62. Integration Test: Permission Denied

Location permission:

```text
DENIED
```

Expected:

```text
No location-dependent operation
Clear message
Retry path
No crash
```

---

# 63. Integration Test: App Restart

Sequence:

```text
Discover R001
 ↓
Close app
 ↓
Reopen
```

Expected:

```text
R001 still appears discovered
```

This verifies local persistence.

---

# 64. Integration Test: Duplicate Discovery

Sequence:

```text
Discover R001
 ↓
Attempt R001 again
```

Expected:

```text
one discovery
```

not:

```text
two progress entries
```

---

# 65. Integration Test: Firebase Failure

Simulate:

```text
Firestore unavailable
```

Expected:

```text
local discovery retained
pendingSync remains true
app remains usable
retry possible
```

---

# 66. Integration Test: Geofence Exit

Sequence:

```text
ENTER R001
 ↓
Scan
 ↓
EXIT R001
```

Expected:

```text
active scan handled appropriately
```

Previously discovered relics must remain discovered.

---

# 67. Integration Test: Lifecycle

Test:

```text
Scan screen
 ↓
background app
 ↓
return
```

Check:

```text
sensor listeners
location updates
UI state
ViewModel state
```

The application architecture should avoid losing important state when an Activity/view is recreated. The module material highlights ViewModel's role in surviving configuration changes. fileciteturn11file0L102-L106

---

# 68. Integration Test: Rotation

If the application supports rotation:

```text
Open scan
Rotate
```

Expected:

```text
important UI state remains coherent
```

Do not rely on Activity fields for persistent UI state.

---

# 69. Daily Integration Rhythm

From:

**20–23 September**

use:

```text
Morning
↓
10–15 minute integration sync

Development
↓

Midday
↓
quick build/integration check

Development
↓

Evening
↓
build + smoke test
```

The team does not need a long meeting.

The goal is early detection.

---

# 70. Integration Meeting Questions

Every integration sync should answer:

```text
1. What did I integrate?
2. What contract did I consume/provide?
3. Does it compile?
4. What is blocked?
5. Did I change a shared contract?
6. What must another member test?
```

---

# 71. Blocker Escalation

If a member is blocked for more than a short focused debugging session:

```text
Stop working alone
 ↓
Tell dependent member
 ↓
Show exact error
 ↓
Check contract
 ↓
Use mock implementation
 ↓
Find smallest failing boundary
```

Do not spend an entire day silently debugging an integration issue.

---

# 72. Temporary Fallbacks

During integration, a temporary fallback may be used:

```text
MockRepository
MockFusionEngine
MockLocationService
```

but it must be clearly marked:

```text
DEVELOPMENT ONLY
```

and replaced before final acceptance.

---

# 73. Debug Logging

During development, log useful information such as:

```text
relicId
distance
accuracy
light value
motion state
fusion score
proximity
scan state
sync state
```

Example:

```text
R001
distance=18m
accuracy=6m
light=250lux
motion=SCANNING
fusion=87
proximity=NEAR
state=REVEALED
```

Remove excessive debug output before final submission where appropriate.

---

# 74. Integration Evidence

Each member should capture evidence of their contribution.

Examples:

### M1

```text
login
navigation
profile
```

### M2

```text
quest
scan
reveal
leaderboard
```

### M3

```text
map
marker
distance
geofence
```

### M4

```text
sensor readings
fusion score
proximity
degradation
```

### M5

```text
Firebase Auth
Firestore
security rules
sync
```

### M6

```text
Room
offline
pendingSync
duplicate prevention
```

---

# 75. Integration Evidence Naming

Use consistent names:

```text
M1_Login.png
M1_Navigation.png

M2_Quest.png
M2_Scan.png
M2_Reveal.png

M3_Map.png
M3_Geofence.png

M4_Fusion.png
M4_Proximity.png

M5_Firebase.png
M5_Security.png

M6_Room.png
M6_OfflineSync.png
```

This makes final documentation easier.

---

# 76. Integration Completion Criteria

By the end of:

**23 September**

all of these must work:

- [ ] application launches
- [ ] authentication
- [ ] navigation
- [ ] map
- [ ] relic data
- [ ] location
- [ ] geofence
- [ ] scan mode
- [ ] GPS contribution
- [ ] light contribution
- [ ] motion contribution
- [ ] fusion score
- [ ] threshold
- [ ] proximity gate
- [ ] reveal
- [ ] Room save
- [ ] Firebase sync
- [ ] leaderboard
- [ ] basic error handling

---

# 77. Phase 4 — Testing and Stabilization

## 24 September

Functional testing.

Test:

```text
normal successful flow
failed scan
duplicate discovery
navigation
authentication
```

---

# 78. 25 September

Device testing.

Test at least two Android devices where possible:

```text
GPS
sensors
permissions
screen
performance
```

---

# 79. 26 September

Resilience testing.

Test:

```text
offline
Firebase failure
sensor unavailable
location unavailable
app restart
background/foreground
```

---

# 80. 27 September

Regression testing.

Run the complete flow repeatedly:

```text
Login
→ Map
→ Find
→ Scan
→ Reveal
→ Save
→ Sync
→ Leaderboard
```

Fix only remaining issues.

Do not add new functionality.

---

# 81. 28 September

Finalization.

Each member provides:

```text
final contribution
test evidence
documentation
known limitations
```

M6 coordinates:

```text
final build
final branch
final APK/package
final documentation package
```

---

# 82. Integration Risk Table

| Risk | Impact | Prevention |
|---|---|---|
| Different models | High | Shared contracts |
| Different IDs | High | Mock catalog |
| Late integration | Critical | Early checkpoints |
| Firebase unavailable | High | Mock + Room |
| Sensor missing | High | Graceful degradation |
| GPS inaccurate | High | Accuracy handling |
| Merge conflicts | Medium | Feature branches |
| Dependency changes | High | Dependency freeze |
| Direct database access | High | Repository rule |
| UI contains business logic | Medium | MVVM |
| Last-minute features | Critical | Feature freeze |

---

# 83. If Integration Is Behind on 20 September

Do not panic and do not start adding people randomly to every feature.

Use:

```text
Identify failing boundary
 ↓
Replace dependency with mock
 ↓
Make interface work
 ↓
Integrate
 ↓
Reconnect real dependency
```

---

# 84. If Integration Is Behind on 21 September

Prioritize:

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
```

Cut:

```text
stretch features
badge polish
advanced routing
relay mechanics
CameraX
```

---

# 85. If Integration Is Behind on 22 September

The only acceptable goal is:

```text
one complete relic
```

working end-to-end.

Use:

```text
R001
```

Do not attempt to make all six relics perfect first.

---

# 86. If Integration Is Behind on 23 September

Freeze the smallest viable product:

```text
U001
R001
```

with:

```text
Login
Map
Geofence
Fusion
Proximity
Reveal
Room
Firebase
Leaderboard
```

Then use the remaining time to stabilize it.

A reliable one-relic end-to-end demonstration is more valuable than six partially working relics.

---

# 87. If Integration Is Behind on 24–25 September

Cut stretch functionality immediately.

Keep only:

```text
core quest loop
```

Then focus on:

```text
crashes
permissions
sensor failure
offline
sync
```

---

# 88. If Integration Is Behind on 26 September

Stop changing architecture.

Only fix:

```text
critical bugs
demo blockers
data-loss issues
crashes
permission failures
```

---

# 89. If Integration Is Behind on 27 September

No new features.

Only:

```text
regression
demo rehearsal
final bug fixes
```

---

# 90. Integration Communication Template

Use this format in the team chat:

```text
[INTEGRATION]

Feature:
Status:
Contract:
Input:
Output:
Tested with:
Result:
Blocked by:
Need from:
```

Example:

```text
[INTEGRATION]

Feature: GPS → Sensor Fusion
Status: Ready
Contract: DistanceResult
Input: 18m / 6m accuracy
Output: FusionResult
Tested with: R001
Result: 87%
Blocked by: none
Need from: M2 verification
```

---

# 91. Contract Change Template

Use:

```text
[CONTRACT CHANGE]

Changed by:
Date:
Contract:
Old:
New:
Reason:
Affected members:
Tests requiring update:
Approved by:
```

No silent interface changes.

---

# 92. Integration Definition of Done

The integration is complete when:

```text
Every core feature
        ↓
uses shared contract
        ↓
works with real implementation
        ↓
works with expected failure states
        ↓
is integrated into main application
        ↓
has been tested
```

---

# 93. Final End-to-End Acceptance Test

## Setup

```text
User:
U001

Relic:
R001

Internet:
ON

Location:
GRANTED

Sensors:
AVAILABLE
```

## Execute

```text
1. Launch app
2. Login
3. Open Map
4. Locate R001
5. Approach
6. Enter geofence
7. Open/enter Scan Mode
8. Perform scan motion
9. Match light environment
10. Reach fusion threshold
11. Bring device close
12. Reveal relic
13. Save discovery
14. Sync cloud
15. Open leaderboard
```

## Expected

```text
R001 discovered
U001 progress = 1
leaderboard reflects discovery
```

---

# 94. Final Failure Acceptance Tests

The application must also survive:

```text
Location denied
Sensor unavailable
Poor GPS
No internet
Firebase failure
Duplicate discovery
App restart
Background/foreground
```

A feature is not considered complete merely because its happy path works.

---

# 95. Final Integration Checklist

## Architecture

- [ ] MVVM separation maintained
- [ ] repository boundary maintained
- [ ] UI does not directly access storage
- [ ] ViewModel owns UI-related state

## Location

- [ ] map
- [ ] GPS
- [ ] distance
- [ ] geofence
- [ ] permission

## Sensors

- [ ] light
- [ ] accelerometer
- [ ] proximity
- [ ] fusion
- [ ] degradation

## Data

- [ ] Room
- [ ] Firebase
- [ ] sync
- [ ] duplicate prevention

## UI

- [ ] login
- [ ] map
- [ ] quest
- [ ] scan
- [ ] reveal
- [ ] leaderboard
- [ ] profile

## Reliability

- [ ] offline
- [ ] Firebase failure
- [ ] sensor missing
- [ ] GPS poor accuracy
- [ ] app restart
- [ ] permissions

## Process

- [ ] PR review
- [ ] shared contracts respected
- [ ] mock data consistent
- [ ] feature freeze observed
- [ ] final regression completed

---

# 96. Final Integration Principle

The team should always be able to answer:

```text
What is broken?
```

with a specific boundary:

```text
Auth
Map
Location
Geofence
Sensor
Fusion
UI
Room
Firebase
Sync
Leaderboard
```

rather than:

```text
"The whole app doesn't work."
```

That is the purpose of incremental integration.

The final application is not six separate projects merged together.

It is:

```text
Six developers
      ↓
Shared contracts
      ↓
Independent modules
      ↓
Frequent integration
      ↓
One tested application
```
