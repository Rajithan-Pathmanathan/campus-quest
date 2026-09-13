# TESTING & ACCEPTANCE STRATEGY
## Campus Quest — Final Six-Member Architecture

**Status:** Final reassignment version  
**Purpose:** Define how M1–M6 prove that Campus Quest works correctly as one integrated Android application.  
**Platform:** Native Android / Kotlin / Android Studio  
**Architecture:** MVVM + Repository + Room + Firebase/Firestore

---

# 1. TESTING OBJECTIVE

Testing must prove more than "the app launches."

Campus Quest contains six tightly connected areas:

```text
Player UI
Creator management
Location/sensors/fusion
Quest/scan gameplay
Firebase/backend
Room/repository/sync
```

The acceptance strategy therefore tests:

```text
individual components
+
interfaces
+
data isolation
+
offline behavior
+
physical discovery
+
complete end-to-end behavior
```

The course architecture material emphasizes that separation of concerns improves maintainability and testability, and that ViewModel logic can be unit-tested independently of the UI. fileciteturn34file1L163-L177

---

# 2. TESTING LEVELS

Campus Quest uses five main levels:

```text
Level 1 — Unit tests
Level 2 — Repository/data tests
Level 3 — Feature/integration tests
Level 4 — End-to-end tests
Level 5 — Physical-device acceptance tests
```

---

# 3. LEVEL 1 — UNIT TESTS

Unit tests verify small pieces of logic in isolation.

Examples:

```text
Game validation
Checkpoint validation
Light matching
GPS normalization
Motion detection
Fusion calculation
ViewModel state transitions
Leaderboard sorting
Sync retry decisions
```

These tests should not require a real Firebase project or physical sensor where a deterministic fake can be used.

---

# 4. LEVEL 2 — DATA/REPOSITORY TESTS

Verify:

```text
Room DAOs
Firestore repository operations
Repository fallback
Cache behavior
Pending sync
Idempotency
Transactions
```

The test should prove that the repository contract remains independent from the underlying storage mechanism.

---

# 5. LEVEL 3 — FEATURE INTEGRATION TESTS

Verify connected components.

Examples:

```text
M1 + M5 + M6
M2 + M5 + M6
M3 + M4
M4 + M6
M5 + M6
```

---

# 6. LEVEL 4 — END-TO-END TESTS

End-to-end tests use the actual application flow:

```text
Login
 ↓
Games
 ↓
Game Details
 ↓
Join
 ↓
Map
 ↓
Checkpoint
 ↓
Scan
 ↓
Fusion
 ↓
Proximity
 ↓
Reveal
 ↓
Discovery
 ↓
Leaderboard
```

---

# 7. LEVEL 5 — PHYSICAL DEVICE TESTING

The final discovery mechanic must be tested on an actual Android device because it depends on:

```text
GPS
ambient light sensor
accelerometer
proximity sensor
geofencing/location behavior
```

An emulator-only test is not sufficient to validate the complete physical discovery mechanic.

---

# 8. TEST OWNERSHIP

| Area | Primary owner | Supporting members |
|---|---|---|
| Player UI | M1 | M4 |
| Navigation | M1 | M2/M4 |
| Creator UI | M2 | M5/M6 |
| Game/checkpoint validation | M2 | M5 |
| Location | M3 | M6 |
| Sensors | M3 | M4 |
| Fusion | M3 | M4 |
| Scan gameplay | M4 | M3 |
| Firebase | M5 | M6 |
| Security rules | M5 | M6 |
| Room | M6 | M5 |
| Repository | M6 | M5 |
| Sync | M6 | M5 |
| End-to-end | M6 | M1–M5 |

M6 coordinates final integration testing, but each member remains responsible for defects in their owned area.

---

# 9. TEST DATA RULE

Tests must not depend exclusively on:

```text
R001
R002
R003
R004
R005
R006
```

These are demo/seed IDs.

Use arbitrary IDs in automated tests.

Example:

```text
test-game-01
test-game-02

cp-a
cp-b
cp-x
```

---

# 10. DYNAMIC GAME TEST

Create:

```text
Game A
2 checkpoints

Game B
8 checkpoints
```

Expected:

```text
both games load correctly
both maps show the correct checkpoints
both progress sets remain isolated
```

No source-code change should be required.

---

# 11. GAME CREATION TEST

### Preconditions

Authenticated creator.

### Steps

```text
Open creator area
Create game
Enter title
Enter description
Save
```

### Expected

```text
Game exists with:
creatorId = authenticated user
status = DRAFT
valid id
valid timestamps
```

---

# 12. CHECKPOINT CREATION TEST

### Steps

```text
Open draft
Add checkpoint
Enter name
Set coordinates
Set radius
Set light range
Set clue
Set lore
Set motion type
Set rarity
Save
```

### Expected

```text
Checkpoint has:
gameId
unique id within game
valid coordinates
valid radius
valid light range
valid order
```

---

# 13. CHECKPOINT EDIT TEST

Change:

```text
name
radius
clue
light range
```

Expected:

```text
same checkpointId
same gameId
updated fields
no duplicate checkpoint
```

---

# 14. CHECKPOINT DELETE TEST

### Steps

```text
Select checkpoint
Choose delete
Confirm
```

Expected:

```text
checkpoint removed from draft
other checkpoints unaffected
order is recalculated where required
```

---

# 15. CHECKPOINT REORDER TEST

Given:

```text
CP1 order 1
CP2 order 2
CP3 order 3
```

Reorder:

```text
CP3
CP1
CP2
```

Expected:

```text
CP3 = 1
CP1 = 2
CP2 = 3
```

---

# 16. INVALID COORDINATE TEST

Test:

```text
latitude < -90
latitude > 90
longitude < -180
longitude > 180
```

Expected:

```text
validation failure
checkpoint not accepted
clear error shown
```

---

# 17. INVALID RADIUS TEST

Test:

```text
radius = 0
radius < 0
unreasonably large value
```

Expected:

```text
validation prevents invalid configuration
```

The exact accepted operational range must come from the final technical configuration.

---

# 18. INVALID LIGHT RANGE TEST

Test:

```text
minLux < 0
maxLux < 0
minLux > maxLux
```

Expected:

```text
validation failure
```

---

# 19. DRAFT TEST

Create a game with:

```text
3 checkpoints
```

Save as draft.

Close/reopen the app.

Expected:

```text
draft remains available
checkpoint configuration remains intact
```

---

# 20. PUBLISH VALIDATION TEST

Attempt to publish:

```text
game with no title
game with no checkpoints
game with invalid checkpoint
```

Expected:

```text
publish blocked
validation error shown
status remains DRAFT
```

---

# 21. PUBLISH SUCCESS TEST

Given a valid draft:

```text
Publish
```

Expected:

```text
status = PUBLISHED
publishedAt populated
players can discover the game
```

---

# 22. PUBLISHED GAME DISCOVERY TEST

After publication:

```text
Player opens games list
```

Expected:

```text
published game appears
draft game does not appear in normal player discovery
```

---

# 23. CREATOR OWNERSHIP TEST

Create:

```text
Creator A → Game A
Creator B → Game B
```

Attempt:

```text
Creator A edits Game B
```

Expected:

```text
operation denied
```

Backend rules must enforce this; UI hiding the game is not sufficient.

---

# 24. AUTHENTICATION TEST

Test:

```text
valid sign-in
invalid credentials
sign-out
session restoration
unauthenticated access
```

Expected:

```text
correct authentication state
correct navigation
protected operations blocked when unauthenticated
```

---

# 25. PLAYER GAME LIST TEST

Verify:

```text
loading state
success state
empty state
error state
retry
```

The UI should never remain indefinitely in a loading state after a terminal failure.

---

# 26. GAME DETAILS TEST

For a valid `gameId`:

```text
Game title
description
creator
checkpoint count
status
```

must load correctly.

Invalid/nonexistent ID:

```text
Not Found
```

state should be shown.

---

# 27. JOIN GAME TEST

### Steps

```text
Player opens Game A
Tap Join
```

Expected:

```text
gamePlayers/{gameId}_{uid}
```

logical membership exists.

Repeated Join:

```text
no duplicate membership
```

---

# 28. GAME MEMBERSHIP ISOLATION TEST

Given:

```text
User 1 joins Game A
User 2 joins Game B
```

Expected:

```text
User 1 has no Game B membership
User 2 has no Game A membership
```

---

# 29. MAP TEST

After joining:

```text
Game A
```

the map should show only Game A checkpoints.

Expected:

```text
correct markers
correct coordinates
correct checkpoint identity
```

---

# 30. GAME-SCOPED GEOFENCE TEST

Register checkpoints for Game A.

Expected:

```text
Game A geofences active
Game B geofences not mixed into Game A
```

---

# 31. GEOFENCE ENTER TEST

Move into a checkpoint's configured radius.

Expected:

```text
ENTER state detected
scan becomes eligible according to gameplay rules
```

False enter events should not automatically reveal a checkpoint.

---

# 32. DISTANCE TEST

Given a known checkpoint coordinate:

```text
device coordinate
checkpoint coordinate
```

verify distance calculation against known test coordinates.

Expected:

```text
distance is within accepted test tolerance
```

---

# 33. LOCATION UNAVAILABLE TEST

Disable location.

Expected:

```text
clear location-unavailable state
no crash
scan does not incorrectly succeed
```

---

# 34. LOCATION PERMISSION DENIED TEST

Deny location permission.

Expected:

```text
permission explanation
appropriate recovery path
no crash
```

---

# 35. LIGHT SENSOR TEST

Use a controllable/fake light value for unit tests.

Given:

```text
expected minLux = 100
expected maxLux = 300
current = 200
```

Expected:

```text
light score indicates match
```

Outside range:

```text
current = 500
```

Expected:

```text
lower/non-match score according to normalization
```

---

# 36. LIGHT SIGNATURE TEST

Confirm that the configured value represents:

```text
ambient environmental light
```

not a fictional emitted relic-light signal.

---

# 37. ACCELEROMETER TEST

Test motion patterns:

```text
expected sweep
no movement
random movement
insufficient movement
```

Expected:

```text
motion score changes according to the configured motion detector
```

---

# 38. PROXIMITY SENSOR TEST

Test:

```text
near
far
unavailable
```

Expected:

```text
correct ProximityState
```

---

# 39. SENSOR LIFECYCLE TEST

Start scan.

Then:

```text
pause app
resume app
```

Expected:

```text
sensor listeners are released/re-established correctly
no duplicate listeners
no crash
```

---

# 40. FUSION NORMALIZATION TEST

Verify each input is normalized:

```text
GPS ∈ [0,1]
Light ∈ [0,1]
Motion ∈ [0,1]
```

Values outside expected raw ranges must not produce invalid fusion values.

---

# 41. FUSION WEIGHT TEST

Given controlled scores:

```text
GPS = 0.8
Light = 0.6
Motion = 1.0
```

calculate expected weighted result using the final configured weights.

Expected:

```text
implementation result == independently calculated expected result
```

---

# 42. FUSION THRESHOLD TEST

Test:

```text
fusion below threshold
fusion exactly at threshold
fusion above threshold
```

Expected:

```text
below → not ready
at/above → threshold reached
```

The exact threshold is a technical configuration value, not a UI assumption.

---

# 43. PROXIMITY FINAL-GATE TEST

This is a critical acceptance test.

Scenario:

```text
GPS + Light + Motion
        ↓
Fusion threshold reached
```

but:

```text
Proximity = FAR
```

Expected:

```text
NO DISCOVERY
NO REVEAL
```

Then:

```text
Proximity = NEAR
```

Expected:

```text
final gate can pass
```

Proximity must remain separate from the weighted fusion calculation.

---

# 44. FALSE-POSITIVE TEST

Give:

```text
high GPS
high light
high motion
```

while the device remains outside the required final proximity condition.

Expected:

```text
checkpoint is NOT revealed
```

---

# 45. SCAN STATE MACHINE TEST

Minimum conceptual states:

```text
IDLE
SCANNING
FUSION_READY
WAITING_FOR_PROXIMITY
SUCCESS
REVEAL
ERROR
```

Test valid transitions and invalid transitions.

---

# 46. SCAN SUCCESS TEST

Required:

```text
geofence/scan eligibility
+
fusion threshold
+
proximity final gate
```

Expected:

```text
SUCCESS
```

---

# 47. REVEAL TEST

Reveal should occur only after successful discovery conditions.

Expected content can include:

```text
checkpoint name
clue
lore
rarity
```

according to the approved UI/product specification.

---

# 48. DISCOVERY RECORDING TEST

After successful scan:

```text
recordDiscovery(gameId, checkpointId, foundAt)
```

Expected:

```text
local discovery stored
cloud progress eventually stored
```

---

# 49. DUPLICATE DISCOVERY TEST

Perform successful discovery twice.

Expected:

```text
one logical discovery
one leaderboard contribution
no duplicate points
```

This must work even if the second request is caused by a retry.

---

# 50. PROGRESS ISOLATION TEST

User discovers:

```text
Game A / CP1
```

Expected:

```text
Game A progress = CP1 found
Game B progress = unchanged
```

---

# 51. LEADERBOARD TEST

Given:

```text
Game A:
User 1 → 3 discoveries
User 2 → 1 discovery
```

Expected ranking:

```text
User 1
User 2
```

Game B must have its own independent ranking.

---

# 52. NO GLOBAL LEADERBOARD TEST

Create progress in two games.

Expected:

```text
Game A leaderboard
≠
Game B leaderboard
```

No combined global ranking should appear.

---

# 53. ROOM CACHE TEST

With network available:

```text
Firebase data
 ↓
Room
```

Then disable network.

Expected:

```text
previously cached data remains readable
```

---

# 54. OFFLINE DISCOVERY TEST

Where offline discovery is enabled:

```text
Network OFF
 ↓
successful physical discovery
 ↓
Room FoundCheckpoint
 ↓
PendingSync
```

Expected:

```text
player still receives the correct local success/reveal behavior
```

---

# 55. SYNC RESTORATION TEST

After offline discovery:

```text
Network ON
```

Expected:

```text
PendingSync
 ↓
Firebase
 ↓
operation marked synced
```

---

# 56. RETRY TEST

Simulate transient network failure.

Expected:

```text
operation remains retryable
retry occurs according to policy
successful retry is idempotent
```

---

# 57. PERMANENT FAILURE TEST

Simulate an operation that should not be retried indefinitely.

Expected:

```text
operation becomes terminal/failed
no infinite retry loop
user/system receives appropriate state
```

---

# 58. SYNC DUPLICATION TEST

Submit the same pending operation more than once.

Expected:

```text
one logical cloud result
one logical local result
```

---

# 59. ROOM TRANSACTION TEST

For a discovery operation requiring:

```text
FoundCheckpoint
+
PendingSync
```

simulate interruption during the operation.

Expected:

```text
no impossible half-written local state
```

---

# 60. ROOM MIGRATION TEST

For each schema change:

```text
old database
 ↓
migration
 ↓
new database
```

Expected:

```text
existing valid data preserved
new schema available
```

---

# 61. FIRESTORE SECURITY TEST

Test unauthorized operations:

```text
User A modifies User B progress
Creator A modifies Creator B game
Unauthenticated user writes protected data
```

Expected:

```text
Firestore security rules reject operation
```

---

# 62. FIRESTORE DATA VALIDATION TEST

Attempt invalid cloud data:

```text
invalid game status
missing creator
invalid coordinates
invalid light range
invalid game/checkpoint relationship
```

Expected:

```text
backend rejects invalid operation
```

---

# 63. FCM TEST

### Scenario

```text
Creator publishes Game A
```

Expected:

```text
new-game notification generated
payload contains gameId
```

Player taps it.

Expected:

```text
Game Details(Game A)
```

---

# 64. FCM FAILURE TEST

If notification delivery fails:

Expected:

```text
game remains PUBLISHED
cloud game state is not rolled back merely because notification delivery failed
```

---

# 65. NAVIGATION TEST

Test:

```text
back
deep link
recreation
invalid gameId
invalid checkpointId
```

Expected:

```text
correct destination
correct ID preservation
safe recovery from invalid destination
```

---

# 66. CONFIGURATION-CHANGE TEST

Rotate during:

```text
game details
creator form
scan
reveal
leaderboard
```

Expected:

```text
important UI state is not unnecessarily lost
```

The course material identifies ViewModel as specifically useful for surviving configuration changes and improving testability. fileciteturn34file1L173-L177

---

# 67. BACKGROUND/RESUME TEST

During scan:

```text
background application
return
```

Expected:

```text
no duplicate sensor registration
no corrupted scan state
correct location/sensor lifecycle
```

---

# 68. UI LOADING TEST

Every network/database-dependent screen must have:

```text
loading
success
empty
error
retry
```

states where applicable.

---

# 69. UI ERROR TEST

Test:

```text
network unavailable
permission denied
invalid data
not found
sensor unavailable
sync failure
```

Expected:

```text
human-readable error
recovery action where possible
no raw stack trace
```

---

# 70. UI/UX CONSISTENCY TEST

Compare implemented screens against the approved design.

Check:

```text
layout
spacing
typography
colors
buttons
icons
navigation
states
```

The course UI/UX material states that corrected designs should be prototyped before implementation and that the Android implementation should match the Figma design. fileciteturn34file0L182-L201 fileciteturn34file0L327-L343

---

# 71. RESPONSIVE TEST

Test:

```text
small phone
normal phone
large phone
portrait
landscape
```

Check:

```text
no clipped controls
no overlapping text
safe-area compliance
keyboard behavior
reachable primary actions
```

The course UI/UX material explicitly addresses orientation, size, safe areas, reachability, grouping, and spacing. fileciteturn34file0L243-L265

---

# 72. ACCESSIBILITY TEST

Check:

```text
touch targets
text scaling
contrast
content descriptions
focus/navigation
```

The course material specifies a 48dp × 48dp touch target minimum and gives the covered text/contrast guidance. fileciteturn34file0L303-L311

---

# 73. PERFORMANCE TEST

Monitor:

```text
startup
screen navigation
map rendering
sensor processing
database operations
Firestore requests
memory usage
battery impact
```

Particular attention:

```text
sensor listeners
location updates
geofences
```

must not remain active unnecessarily.

---

# 74. NETWORK TEST MATRIX

| Condition | Expected |
|---|---|
| Fast network | Normal operation |
| Slow network | Loading state, eventual result |
| Network unavailable | Cached/offline behavior |
| Network restored | Sync/recovery |
| Intermittent network | Retry-safe behavior |
| Cloud failure | Error without corruption |

---

# 75. SENSOR TEST MATRIX

| Sensor | Available | Unavailable | Expected |
|---|---|---|---|
| GPS | ✓ | ✓ | Clear state |
| Light | ✓ | ✓ | Clear state |
| Accelerometer | ✓ | ✓ | Clear state |
| Proximity | ✓ | ✓ | Clear state |

No unavailable sensor should cause an application crash.

---

# 76. SECURITY TEST MATRIX

```text
[ ] Unauthenticated read/write restrictions
[ ] Creator ownership
[ ] Player membership
[ ] User progress isolation
[ ] Game leaderboard isolation
[ ] Invalid cloud data
[ ] Unauthorized checkpoint modification
```

---

# 77. MULTI-GAME TEST MATRIX

```text
Game A
 ├─ CP-A1
 └─ CP-A2

Game B
 ├─ CP-B1
 ├─ CP-B2
 └─ CP-B3
```

Verify:

```text
map isolation
progress isolation
leaderboard isolation
geofence isolation
cache isolation
sync isolation
```

---

# 78. MULTI-USER TEST MATRIX

```text
User A
User B
```

Verify:

```text
membership isolation
progress isolation
leaderboard entries
creator ownership
pending sync ownership
```

---

# 79. LEGACY ARCHITECTURE TEST

Search the codebase for primary dependencies on:

```text
RelicEntity
FoundRelicEntity
hard-coded R001–R006 logic
global leaderboard
```

Expected:

```text
no obsolete implementation controls production behavior
```

Compatibility code may remain only when explicitly documented as legacy/seed support.

---

# 80. INTEGRATION TEST 1

```text
Login
 ↓
Games
 ↓
Game Details
 ↓
Join
```

Owners:

```text
M1 + M5 + M6
```

---

# 81. INTEGRATION TEST 2

```text
Join
 ↓
Load dynamic checkpoints
 ↓
Map
```

Owners:

```text
M1 + M3 + M5 + M6
```

---

# 82. INTEGRATION TEST 3

```text
Map
 ↓
Geofence
 ↓
Scan
```

Owners:

```text
M1 + M3 + M4 + M6
```

---

# 83. INTEGRATION TEST 4

```text
Scan
 ↓
Fusion
 ↓
Proximity
 ↓
Reveal
```

Owners:

```text
M3 + M4
```

---

# 84. INTEGRATION TEST 5

```text
Reveal
 ↓
Record discovery
 ↓
Room
 ↓
Firebase
 ↓
Leaderboard
```

Owners:

```text
M4 + M5 + M6
```

---

# 85. INTEGRATION TEST 6

```text
Creator
 ↓
Create
 ↓
Publish
 ↓
FCM
 ↓
Player
```

Owners:

```text
M1 + M2 + M5 + M6
```

---

# 86. PRIMARY END-TO-END ACCEPTANCE TEST

### Preconditions

```text
Creator account exists.
Player account exists.
Physical Android device has required sensors/location.
Firebase is configured.
```

### Steps

```text
1. Creator logs in.
2. Creator creates a new game.
3. Creator adds at least two checkpoints.
4. Creator configures location/light/motion data.
5. Creator saves draft.
6. Creator publishes.
7. Player receives notification.
8. Player opens notification.
9. Player views game details.
10. Player joins.
11. Player opens map.
12. Player approaches checkpoint.
13. Geofence becomes active/eligible.
14. Player enters scan.
15. GPS/light/motion signals update.
16. Fusion reaches threshold.
17. Proximity final gate passes.
18. Reveal is shown.
19. Discovery is recorded.
20. Leaderboard reflects the discovery.
```

### Expected

The entire flow completes without manual database editing or hard-coded checkpoint intervention.

---

# 87. END-TO-END FAILURE TEST

Repeat the primary flow but deliberately fail:

```text
location
light
motion
proximity
network
```

at different stages.

Expected:

```text
safe failure
clear state
no false discovery
recoverable flow
```

---

# 88. ACCEPTANCE CRITERIA — CORE

The application passes core acceptance when:

```text
[ ] Player can authenticate
[ ] Player can browse published games
[ ] Player can view details
[ ] Player can join
[ ] Creator can create a game
[ ] Creator can add arbitrary checkpoints
[ ] Creator can publish
[ ] Dynamic checkpoints load
[ ] Map works
[ ] Geofence works
[ ] GPS works
[ ] Light works
[ ] Motion works
[ ] Fusion works
[ ] Proximity final gate works
[ ] Scan works
[ ] Reveal works
[ ] Discovery persists
[ ] Leaderboard updates
```

---

# 89. ACCEPTANCE CRITERIA — DATA

```text
[ ] Room works
[ ] Firebase works
[ ] User isolation works
[ ] Game isolation works
[ ] Duplicate discovery prevented
[ ] Sync works
[ ] Retry works
[ ] Transactions work
[ ] Security rules work
```

---

# 90. ACCEPTANCE CRITERIA — NOTIFICATION

```text
[ ] Publish generates notification event
[ ] Payload contains gameId
[ ] Notification opens correct game
[ ] Wrong game is never opened because of stale/missing ID
```

---

# 91. ACCEPTANCE CRITERIA — UI

```text
[ ] Figma-approved flow implemented
[ ] Loading states
[ ] Error states
[ ] Empty states
[ ] Back navigation
[ ] Safe areas
[ ] Small-screen support
[ ] Touch targets
[ ] Readable text
[ ] Consistent components
```

---

# 92. BUG SEVERITY

## P0 — Blocking

Examples:

```text
App cannot launch
Data corruption
Authentication completely broken
Core discovery impossible
Security vulnerability
```

Must be fixed before demo.

## P1 — Major

Examples:

```text
Creator cannot publish
Player cannot join
Fusion incorrectly succeeds
Leaderboard incorrect
Sync permanently loses data
```

Must be fixed before final build.

## P2 — Moderate

Examples:

```text
non-critical screen issue
recoverable UI problem
minor data display issue
```

Fix if time allows.

## P3 — Cosmetic

Examples:

```text
minor spacing
visual polish
non-critical animation
```

Lowest priority.

---

# 93. BUG REPORT FORMAT

Every significant bug should contain:

```text
Title
Environment
Device
Build/version
Preconditions
Steps to reproduce
Expected result
Actual result
Screenshots/logs
Severity
Owner
```

---

# 94. REGRESSION TESTING

After fixing a P0/P1 issue:

```text
reproduce
 ↓
fix
 ↓
targeted test
 ↓
related feature test
 ↓
full smoke test
```

Do not assume a fix is isolated.

---

# 95. SMOKE TEST

Before every integration/demo build:

```text
[ ] App launches
[ ] Login works
[ ] Games list opens
[ ] Game details opens
[ ] Join works
[ ] Map opens
[ ] Scan opens
[ ] Core sensor pipeline runs
[ ] Reveal works
[ ] Leaderboard loads
```

---

# 96. RELEASE CANDIDATE TEST

Before final submission:

```text
Fresh install
 ↓
Login
 ↓
Creator flow
 ↓
Publish
 ↓
Player flow
 ↓
Discovery
 ↓
Offline test
 ↓
Restart app
 ↓
Reopen data
```

Expected:

```text
no unrecoverable state
```

---

# 97. CLEAN INSTALL TEST

Remove application data/install fresh.

Verify:

```text
no hidden dependency on previous Room data
no hidden dependency on previous authentication state
app initializes correctly
```

---

# 98. RESTART TEST

After successful discovery:

```text
force close app
reopen
```

Expected:

```text
discovered checkpoint remains discovered
leaderboard remains correct
game membership remains correct
```

---

# 99. BATTERY/LIFECYCLE TEST

Run gameplay for a realistic session.

Observe:

```text
location updates
sensor registration
background behavior
```

Expected:

```text
no obvious uncontrolled continuous processing
```

---

# 100. FINAL DEMO CHECKLIST

### Device

```text
[ ] Physical Android device
[ ] GPS available
[ ] Light sensor available
[ ] Accelerometer available
[ ] Proximity sensor available
[ ] Internet available
[ ] Firebase configured
```

### Accounts

```text
[ ] Creator account
[ ] Player account
[ ] Optional second player
```

### Data

```text
[ ] Demo game
[ ] Second test game
[ ] Dynamic checkpoints
```

### Application

```text
[ ] Login
[ ] Creator
[ ] Publish
[ ] Notification
[ ] Player
[ ] Map
[ ] Scan
[ ] Reveal
[ ] Leaderboard
```

---

# 101. TEST EXECUTION ORDER

Use this order to reduce wasted time:

```text
1. Build smoke test
2. Authentication
3. Room
4. Firebase
5. Repository
6. Creator
7. Player discovery
8. Map/location
9. Sensors
10. Fusion
11. Scan
12. Discovery
13. Leaderboard
14. FCM
15. Offline/sync
16. Security
17. Lifecycle
18. Full end-to-end
19. Final UI review
```

---

# 102. TEST EVIDENCE

For important acceptance tests, retain:

```text
screenshots
screen recordings
Firebase console evidence where appropriate
Room/database evidence where appropriate
test results
logs
```

Especially retain evidence for:

```text
creator → publish
notification
dynamic checkpoint
sensor fusion
proximity gate
discovery
leaderboard
offline sync
security
```

---

# 103. TEST CASE ID CONVENTION

Recommended:

```text
AUTH-001
PLAYER-001
CREATOR-001
MAP-001
LOC-001
SENSOR-001
FUSION-001
SCAN-001
DATA-001
ROOM-001
FIREBASE-001
SYNC-001
FCM-001
SEC-001
E2E-001
UI-001
```

---

# 104. SAMPLE TEST CASE FORMAT

```text
ID:
FUSION-001

Purpose:
Verify weighted fusion calculation.

Given:
GPS = 0.8
Light = 0.6
Motion = 1.0

When:
Fusion is calculated.

Then:
Expected result matches the configured weighted formula.
```

---

# 105. CRITICAL TEST CASE

```text
ID:
SCAN-001

Purpose:
Prevent false discovery when fusion passes but proximity does not.

Given:
GPS + Light + Motion produce fusion >= threshold.

And:
Proximity = FAR.

Then:
No discovery.
No reveal.
```

---

# 106. CRITICAL DATA TEST

```text
ID:
DATA-001

Purpose:
Verify game isolation.

Given:
Game A and Game B both have checkpoint IDs that may overlap.

When:
User discovers a checkpoint in Game A.

Then:
Game B progress remains unchanged.
```

---

# 107. CRITICAL SECURITY TEST

```text
ID:
SEC-001

Purpose:
Prevent cross-user progress modification.

Given:
User A is authenticated.

When:
User A attempts to write User B's progress.

Then:
Firebase security rules reject the request.
```

---

# 108. CRITICAL OFFLINE TEST

```text
ID:
SYNC-001

Purpose:
Verify offline discovery recovery.

Given:
Checkpoint configuration is cached.

When:
Network is disabled and discovery succeeds.

Then:
Local discovery is saved and pending synchronization is created.

When:
Network returns.

Then:
Cloud synchronization completes without duplicate scoring.
```

---

# 109. FINAL PASS RULE

A milestone passes only when:

```text
implementation works
+
tests pass
+
shared contract is respected
+
no P0 blocker exists
```

---

# 110. FINAL SUBMISSION PASS RULE

The final application passes when:

```text
all core acceptance criteria pass
+
no unresolved P0/P1 defect
+
primary end-to-end test passes
+
security isolation passes
+
game isolation passes
+
physical sensor test passes
+
final build installs cleanly
```

---

# 111. TEST RESPONSIBILITY SUMMARY

## M1

Must prove:

```text
player UI
navigation
game discovery
join
leaderboard presentation
notification deep links
```

## M2

Must prove:

```text
creator UI
game creation
checkpoint management
validation
draft
publish
```

## M3

Must prove:

```text
location
geofence
light
accelerometer
proximity
normalization
fusion
lifecycle
```

## M4

Must prove:

```text
scan
state machine
fusion presentation
proximity gate
reveal
discovery trigger
```

## M5

Must prove:

```text
Auth
Firestore
security
publish
progress
leaderboard
FCM
cloud validation
```

## M6

Must prove:

```text
Room
repository
cache
pending sync
retry
idempotency
transactions
migrations
integration
```

---

# 112. FINAL TEAM ACCEPTANCE SESSION

Before final submission, the six members should execute the primary flow together.

Suggested order:

```text
M2 → creates/publishes
M5 → confirms cloud state/notification
M1 → opens player flow
M6 → confirms local/repository behavior
M3 → operates physical signal pipeline
M4 → completes scan/reveal
M5 → confirms cloud progress
M6 → confirms sync/local state
M1 → displays leaderboard
```

---

# 113. FINAL QUALITY GATE

The team should not declare the application complete because:

```text
each member's screen works independently
```

The application is complete only when:

```text
Creator data
      ↓
Cloud/local persistence
      ↓
Player discovery
      ↓
Physical gameplay
      ↓
Discovery
      ↓
Synchronization
      ↓
Game leaderboard
```

works as one continuous system.

---

# 114. ARCHITECTURE TESTING ALIGNMENT

The course material explains that without clear architecture, UI, business logic, and data access become mixed together and are harder to test, debug, and extend. fileciteturn34file1L137-L141

This testing plan therefore deliberately tests the boundaries:

```text
UI
ViewModel
Repository
Room
Firebase
Sensor layer
```

rather than relying only on manual end-to-end testing.

---

# 115. FINAL NON-NEGOTIABLE TESTS

```text
1. Dynamic games work.
2. Dynamic checkpoint counts work.
3. Game isolation works.
4. User isolation works.
5. Creator ownership works.
6. Firebase security works.
7. Room keys preserve scope.
8. Duplicate discovery is prevented.
9. GPS + Light + Motion form the weighted fusion.
10. Proximity remains a separate final gate.
11. Fusion alone cannot reveal a checkpoint.
12. Discovery persists.
13. Offline sync is retry-safe where supported.
14. Leaderboards are game-specific.
15. No global leaderboard controls production behavior.
16. R001–R006 are not required by production logic.
17. Notification deep links use gameId.
18. Lifecycle does not corrupt gameplay.
19. Sensor failure does not crash the app.
20. The complete end-to-end flow passes on a physical device.
```

---

# 116. FINAL TEST STATUS TABLE

| Area | Required status |
|---|---|
| Build | PASS |
| Authentication | PASS |
| Player UI | PASS |
| Creator UI | PASS |
| Dynamic games | PASS |
| Dynamic checkpoints | PASS |
| Firebase | PASS |
| Room | PASS |
| Repository | PASS |
| Location | PASS |
| Geofencing | PASS |
| Light | PASS |
| Motion | PASS |
| Fusion | PASS |
| Proximity gate | PASS |
| Scan | PASS |
| Reveal | PASS |
| Discovery | PASS |
| Leaderboard | PASS |
| FCM | PASS |
| Offline | PASS where implemented |
| Sync | PASS |
| Security | PASS |
| Lifecycle | PASS |
| Physical device | PASS |
| End-to-end | PASS |

---

# 117. FINAL SIGN-OFF

Each member signs off only after their area passes:

```text
Member
Area
Unit tests
Integration tests
Known issues
Status
```

M6 performs the final integration sign-off after all member areas have passed their relevant acceptance tests.

---

# 118. END-TO-END GOLDEN PATH

The single most important test remains:

```text
Creator Login
    ↓
Create Game
    ↓
Add Dynamic Checkpoints
    ↓
Save Draft
    ↓
Publish
    ↓
FCM New Game Notification
    ↓
Player Opens Notification
    ↓
Game Details
    ↓
Join
    ↓
Map
    ↓
Approach Checkpoint
    ↓
Geofence
    ↓
Scan
    ↓
GPS + Light + Motion
    ↓
Fusion Threshold
    ↓
Proximity Final Gate
    ↓
Reveal
    ↓
Record Discovery
    ↓
Room
    ↓
Firebase Sync
    ↓
Game-Specific Leaderboard
```

If this flow passes on a physical device and the isolation/security/offline tests pass, the core Campus Quest architecture has been demonstrated.

---

**END OF TESTING & ACCEPTANCE STRATEGY**
