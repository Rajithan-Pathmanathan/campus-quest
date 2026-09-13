# Campus Quest — Shared Mock Data Catalog

**Document ID:** CQ-SHARED-02  
**Document:** Mock Data Catalog  
**Project:** Campus Quest  
**Purpose:** Shared development data for parallel implementation, testing, integration, and demonstrations  
**Development window:** 13 September 2026 – 28 September 2026  
**Status:** Team working specification  
**Depends on:** `07_SHARED_CONTRACTS_AND_AGREEMENTS.md`

---

## 1. Purpose

This document defines the **single agreed mock-data set** that all six Campus Quest developers should use while building independently.

The goal is simple:

> Every member must be able to develop and test their feature before the complete Firebase, Room, GPS, and sensor implementation is connected.

The mock data therefore provides realistic stand-ins for:

- authenticated users (Creators & Players)
- published & draft games (`game_001`, `game_002`, `game_003_draft`)
- dynamic checkpoints (`cp_001`..`cp_006`)
- ambient environmental light signatures (`minLux`..`maxLux`)
- player participation records (`gamePlayers`)
- game-specific progress & discovery records
- game-specific leaderboard entries
- FCM push notification payloads
- GPS results
- geofence events
- sensor readings
- sensor availability
- fusion results
- scan states
- offline/sync conditions
- error conditions

This is a **development contract**, not a second database design. The real storage and service implementations must continue to follow the shared contracts.

---

# 2. Rules for Using Mock Data

## 2.1 One canonical dataset

All members must use the IDs and values in this document unless a new test case is explicitly added.

Do not create:

- `relic1` in one module and `R001` in another
- different coordinates for the same relic
- different user IDs for the same test account
- different light ranges for the same relic

That creates integration failures that look like code bugs.

---

## 2.2 Mock data must have stable IDs

Use these formats:

| Object | ID format | Example |
|---|---|---|
| User | `U001`, `U002` | `U001` |
| Relic | `R001`–`R006` | `R001` |
| Found record | `FOUND_<UID>_<RID>` | `FOUND_U001_R001` |
| Leaderboard | user ID | `U001` |
| Test scan | `SCAN_<RID>_<CASE>` | `SCAN_R001_GOOD` |

---

## 2.3 Mock data must be replaceable

The UI must not hard-code Firebase values directly.

Bad:

```kotlin
textView.text = "The Founder's Bell"
```

Better:

```kotlin
val relic = repository.getRelic("R001")
textView.text = relic.name
```

The same principle applies to:

- coordinates
- score
- lore
- leaderboard values
- light signatures
- discovery state

---

# 3. Canonical Test Users

Use the following users for development.

| ID | Display Name | Email | Purpose |
|---|---|---|---|
| U001 | Nimal Perera | nimal.test@campusquest.test | Primary demo account |
| U002 | Kavindi Silva | kavindi.test@campusquest.test | Second-user testing |
| U003 | Sahan Fernando | sahan.test@campusquest.test | Leaderboard testing |
| U004 | Ayesha Jayasinghe | ayesha.test@campusquest.test | Offline/progress testing |
| U005 | Test Explorer | explorer.test@campusquest.test | Fresh account / zero progress |

### Primary demo account

Use:

```text
User ID: U001
Display name: Nimal Perera
```

This account should normally start with:

```text
relicsFound = 0
```

when demonstrating the complete discovery flow from the beginning.

---

# 4. Canonical Relic Dataset

The app should have enough relics to demonstrate:

- multiple map markers
- different distances
- different light signatures
- different lore
- multiple discoveries
- leaderboard progress
- nearby/far-away states

Use six relics for the core dataset.

## 4.1 Relic overview

| ID | Name | Radius | Rarity | Purpose |
|---|---|---:|---|---|
| R001 | Founder's Bell | 25 m | Common | Primary demo relic |
| R002 | Scholar's Compass | 25 m | Uncommon | Second quest |
| R003 | Heritage Key | 20 m | Rare | Different radius |
| R004 | Old Library Seal | 30 m | Common | Larger geofence |
| R005 | Garden Chronicle | 25 m | Uncommon | Outdoor test |
| R006 | Clock Tower Relic | 20 m | Rare | Final demo relic |

The rarity values are useful for UI testing even if the badge/rarity system remains optional.

---

# 5. Canonical Relic Locations

These are **development coordinates** for the Campus Quest test dataset.

They are placeholders for implementation/testing and must not be presented as verified physical relic locations unless the team has separately validated them on campus.

| ID | Latitude | Longitude | Radius |
|---|---:|---:|---:|
| R001 | 6.974850 | 79.915300 | 25 m |
| R002 | 6.975420 | 79.914750 | 25 m |
| R003 | 6.975900 | 79.915650 | 20 m |
| R004 | 6.976300 | 79.914900 | 30 m |
| R005 | 6.974300 | 79.916100 | 25 m |
| R006 | 6.976750 | 79.915700 | 20 m |

### Important

Before the final physical demonstration:

1. confirm that the selected test locations are usable;
2. measure/observe actual GPS behaviour;
3. adjust the development dataset if necessary;
4. update the shared document before changing code.

Do not silently change coordinates in only one developer's branch.

---

# 6. Canonical Relic Data

## R001 — Founder's Bell

```text
id: R001
name: Founder's Bell
latitude: 6.974850
longitude: 79.915300
radiusM: 25
lightMin: 180
lightMax: 320
rarity: Common
```

Lore:

> A bell associated with the earliest days of the campus. Explorers who locate it uncover a short story about the people and traditions that shaped the university.

Primary use:

- complete end-to-end demo
- map marker
- geofence
- sensor fusion
- proximity reveal
- Room save
- Firestore sync
- leaderboard update

---

## R002 — Scholar's Compass

```text
id: R002
name: Scholar's Compass
latitude: 6.975420
longitude: 79.914750
radiusM: 25
lightMin: 250
lightMax: 450
rarity: Uncommon
```

Lore:

> A symbolic compass representing the pursuit of knowledge and the many paths followed by generations of students.

Primary use:

- second relic
- quest-list testing
- different light range
- repeated discovery flow

---

## R003 — Heritage Key

```text
id: R003
name: Heritage Key
latitude: 6.975900
longitude: 79.915650
radiusM: 20
lightMin: 80
lightMax: 180
rarity: Rare
```

Lore:

> A symbolic key representing access to the stories preserved within the university's heritage.

Primary use:

- smaller geofence
- lower light environment
- rare-item UI testing

---

## R004 — Old Library Seal

```text
id: R004
name: Old Library Seal
latitude: 6.976300
longitude: 79.914900
radiusM: 30
lightMin: 400
lightMax: 650
rarity: Common
```

Lore:

> A seal representing the preservation and sharing of knowledge across generations.

Primary use:

- larger geofence
- bright-environment testing
- map/quest list testing

---

## R005 — Garden Chronicle

```text
id: R005
name: Garden Chronicle
latitude: 6.974300
longitude: 79.916100
radiusM: 25
lightMin: 120
lightMax: 250
rarity: Uncommon
```

Lore:

> A small chapter of campus history connected with the green spaces where students gather and explore.

Primary use:

- outdoor test
- lower light-range testing
- repeated scan testing

---

## R006 — Clock Tower Relic

```text
id: R006
name: Clock Tower Relic
latitude: 6.976750
longitude: 79.915700
radiusM: 20
lightMin: 300
lightMax: 500
rarity: Rare
```

Lore:

> A relic representing time, continuity, and the generations of students who have passed through the campus.

Primary use:

- final quest
- rare-item presentation
- complete multi-relic progress

---

# 7. Mock Firestore Relic Document

A relic should conceptually look like:

```json
{
  "id": "R001",
  "name": "Founder's Bell",
  "lat": 6.974850,
  "lng": 79.915300,
  "radiusM": 25,
  "lightSignature": {
    "min": 180,
    "max": 320
  },
  "rarity": "Common",
  "lore": "A bell associated with the earliest days of the campus."
}
```

The exact SDK mapping may differ between Kotlin data classes and Firestore documents.

---

# 8. Mock Quest States

A relic can appear in different UI states.

## State A — Undiscovered

```text
relicId: R001
found: false
distance: 145 m
```

Expected UI:

```text
Founder's Bell
145 m away
Not discovered
```

---

## State B — Approaching

```text
relicId: R001
found: false
distance: 38 m
```

Expected UI:

```text
Founder's Bell
38 m away
Move closer
```

---

## State C — Geofence entered

```text
relicId: R001
found: false
distance: 18 m
geofence: ENTERED
```

Expected UI:

```text
Founder's Bell
Scan Mode available
```

---

## State D — Scanning

```text
relicId: R001
scanState: SCANNING
fusionScore: 72
```

Expected UI:

```text
Scanning...
72%
```

---

## State E — Fusion threshold reached

```text
relicId: R001
scanState: READY_FOR_PROXIMITY
fusionScore: 85
```

Expected UI:

```text
Signal matched
Move phone close to reveal
```

---

## State F — Proximity confirmed

```text
relicId: R001
scanState: REVEALED
fusionScore: 91
proximity: NEAR
```

Expected UI:

```text
Relic discovered!
Founder's Bell
```

---

# 9. Mock GPS Data

M3 should use these cases to test location-dependent screens without requiring physical movement.

## 9.1 Far away

```text
relicId: R001
distanceM: 150
accuracyM: 8
```

Expected:

```text
Far from relic
```

---

## 9.2 Approaching

```text
relicId: R001
distanceM: 55
accuracyM: 7
```

Expected:

```text
Continue toward relic
```

---

## 9.3 Near boundary

```text
relicId: R001
distanceM: 26
accuracyM: 8
```

Expected:

```text
Outside / uncertain geofence
```

This is intentionally close to the 25 m radius.

---

## 9.4 Inside geofence

```text
relicId: R001
distanceM: 18
accuracyM: 6
```

Expected:

```text
Geofence entered
Start Scan Mode
```

---

## 9.5 Very close

```text
relicId: R001
distanceM: 5
accuracyM: 4
```

Expected:

```text
Very close
```

---

## 9.6 Poor accuracy

```text
relicId: R001
distanceM: 18
accuracyM: 45
```

Expected:

```text
Location accuracy is poor
```

The app should not blindly claim precise proximity when the reported accuracy is poor.

---

# 10. Mock Geofence Events

Use:

```text
ENTER
EXIT
```

Example:

```text
relicId: R001
eventType: ENTER
```

Expected workflow:

```text
Map
 ↓
Geofence ENTER
 ↓
Open/enable Scan Mode
```

For exit:

```text
relicId: R001
eventType: EXIT
```

Expected behaviour:

- stop or disable the active close-range scan where appropriate;
- return to an appropriate quest/location state;
- do not delete previously discovered progress.

---

# 11. Mock Sensor Data

The sensor-fusion developer needs deterministic test values.

The actual Android sensor APIs will later provide live values.

---

## 11.1 Light sensor

Example live values:

```text
80 lux
150 lux
220 lux
300 lux
450 lux
600 lux
```

For R001:

```text
valid range = 180–320 lux
```

Examples:

| Live lux | Expected match |
|---:|---|
| 100 | No |
| 179 | No / boundary |
| 200 | Yes |
| 250 | Yes |
| 300 | Yes |
| 321 | No |
| 500 | No |

The exact scoring curve belongs to the sensor-fusion implementation contract.

---

# 12. Mock Accelerometer Data

Use simplified motion cases.

## Case A — Stationary

```text
motionState: STATIONARY
```

Expected:

```text
motionMatch: low
```

---

## Case B — Slow side-to-side scan

```text
motionState: SCANNING
```

Expected:

```text
motionMatch: high
```

---

## Case C — Fast/random movement

```text
motionState: UNSTABLE
```

Expected:

```text
motionMatch: reduced
```

The UI should not need to know the raw accelerometer algorithm.

The sensor module should expose a higher-level result such as:

```text
MotionState.SCANNING
```

or a normalized motion score.

---

# 13. Mock Proximity Data

Proximity is the **final reveal gate**.

It is not one of the three weighted fusion inputs.

## Far

```text
proximity: FAR
```

Expected:

```text
Do not reveal
```

## Near

```text
proximity: NEAR
```

Expected:

```text
Allow reveal if fusion threshold has already been reached
```

---

# 14. Mock Sensor Availability

The application must be tested both with and without individual sensors.

## All sensors available

```text
light: AVAILABLE
accelerometer: AVAILABLE
proximity: AVAILABLE
```

Expected:

```text
Normal fusion
```

---

## No light sensor

```text
light: UNAVAILABLE
accelerometer: AVAILABLE
proximity: AVAILABLE
```

Expected:

```text
Continue using available signals
```

The application should not crash or permanently block the quest.

---

## No accelerometer

```text
light: AVAILABLE
accelerometer: UNAVAILABLE
proximity: AVAILABLE
```

Expected:

```text
Continue using available signals
```

---

## No proximity sensor

```text
light: AVAILABLE
accelerometer: AVAILABLE
proximity: UNAVAILABLE
```

Expected behaviour must follow the team's agreed graceful-degradation rule. The implementation must not fake a proximity reading as if the hardware were present.

---

## No optional sensors

```text
light: UNAVAILABLE
accelerometer: UNAVAILABLE
proximity: UNAVAILABLE
```

Expected:

```text
Show a clear unsupported/limited-device state
```

Do not crash.

---

# 15. Mock Fusion Results

The UI developer should not implement the fusion mathematics.

M4 provides a normalized result.

Example:

```text
fusionScore = 0
fusionScore = 25
fusionScore = 50
fusionScore = 72
fusionScore = 85
fusionScore = 91
fusionScore = 100
```

Recommended UI test thresholds:

```text
0–59    Not matched
60–79   Getting closer
80–89   Strong match
90–100  Very strong match
```

The **actual reveal threshold must remain the value agreed in the sensor-fusion specification**, not an independently chosen UI rule.

---

# 16. Canonical End-to-End Scan Cases

## Case 1 — Successful scan

```text
GPS distance: 18 m
Light: 250 lux
Motion: SCANNING
Fusion score: 87
Proximity: NEAR
```

Expected:

```text
Geofence entered
→ Scan Mode
→ Fusion increases
→ Threshold reached
→ Proximity confirmed
→ Relic revealed
→ Save locally
→ Sync
→ Leaderboard updated
```

---

## Case 2 — Too far

```text
GPS distance: 150 m
Light: 250 lux
Motion: SCANNING
Fusion score: 20
Proximity: FAR
```

Expected:

```text
Do not reveal
```

---

## Case 3 — Correct environment, wrong movement

```text
GPS distance: 15 m
Light: 250 lux
Motion: STATIONARY
Fusion score: 55
Proximity: NEAR
```

Expected:

```text
Do not reveal
```

The user should continue scanning.

---

## Case 4 — Good fusion, not close enough

```text
GPS distance: 18 m
Light: 250 lux
Motion: SCANNING
Fusion score: 88
Proximity: FAR
```

Expected:

```text
Do not reveal yet
```

The final proximity condition has not been satisfied.

---

## Case 5 — Sensor unavailable

```text
GPS distance: 18 m
Light: UNAVAILABLE
Motion: SCANNING
Proximity: NEAR
```

Expected:

```text
Use graceful degradation
```

The exact reweighting must come from the sensor-fusion specification.

---

# 17. Mock Progress Data

## New user

```text
userId: U001
foundRelics: []
relicsFound: 0
```

---

## One relic found

```text
userId: U001
foundRelics:
  - R001

relicsFound: 1
```

---

## Multiple relics found

```text
userId: U001
foundRelics:
  - R001
  - R002
  - R005

relicsFound: 3
```

---

# 18. Mock FoundRelic Records

Example:

```json
{
  "userId": "U001",
  "relicId": "R001",
  "foundAt": "2026-09-20T10:30:00",
  "pendingSync": true
}
```

For local Room storage, use the agreed local representation.

The shared contract recommends `pendingSync` for the local state.

Avoid creating a second field with a different meaning such as:

```text
synced
pending
uploadRequired
needsUpload
```

unless the team explicitly changes the contract.

---

# 19. Mock Leaderboard

Use:

| Rank | User | Relics Found |
|---:|---|---:|
| 1 | Sahan Fernando | 5 |
| 2 | Kavindi Silva | 4 |
| 3 | Nimal Perera | 3 |
| 4 | Ayesha Jayasinghe | 2 |
| 5 | Test Explorer | 0 |

Example Firestore-style document:

```json
{
  "uid": "U003",
  "displayName": "Sahan Fernando",
  "relicsFound": 5
}
```

The leaderboard UI should not calculate rankings from unrelated UI state. It should observe the repository's leaderboard data.

---

# 20. Offline Mock Scenario

This scenario is mandatory because the application is expected to support local persistence and synchronization.

Starting state:

```text
U001
R001 discovered
pendingSync = true
Internet = OFF
```

Expected:

```text
Room stores discovery
↓
UI immediately shows R001 as discovered
↓
Firebase upload is postponed
```

Then:

```text
Internet = ON
↓
syncPending()
↓
Firestore updated
↓
pendingSync = false
```

The exact conflict/sync implementation follows the shared contract.

---

# 21. Firebase Failure Mock

Simulate:

```text
Firestore unavailable
```

Expected:

```text
Do not crash
Do not lose locally saved discovery
Show appropriate offline/sync state
Retry later
```

The UI should distinguish between:

```text
Discovery saved locally
```

and:

```text
Discovery successfully synced to cloud
```

where the application design exposes that distinction.

---

# 22. Authentication Mock States

## Logged out

```text
authState: LOGGED_OUT
```

Expected:

```text
Show Login
```

---

## Logging in

```text
authState: LOADING
```

Expected:

```text
Show loading state
Disable repeated submission where appropriate
```

---

## Logged in

```text
authState: AUTHENTICATED
userId: U001
```

Expected:

```text
Open main application
```

---

## Login error

```text
authState: ERROR
error: INVALID_CREDENTIALS
```

Expected:

```text
Show readable error
Allow retry
```

---

# 23. UI State Mock Catalog

Every major screen should have more than its happy path.

Minimum states:

```text
Loading
Content
Empty
Error
Offline
Permission denied
Unsupported sensor
```

For example, the quest screen should be tested as:

```text
QUEST_LOADING
QUEST_CONTENT
QUEST_EMPTY
QUEST_ERROR
QUEST_OFFLINE
```

The scan screen should be tested as:

```text
SCAN_NOT_AVAILABLE
SCAN_READY
SCAN_SCANNING
SCAN_NEAR_THRESHOLD
SCAN_WAITING_FOR_PROXIMITY
SCAN_REVEALED
SCAN_SENSOR_LIMITED
SCAN_ERROR
```

---

# 24. Mock Data for M1 — App Shell & Navigation

M1 needs:

```text
U001
authenticated = true
displayName = Nimal Perera
```

Navigation test destinations:

```text
Map
Quests
Leaderboard
Profile
Scan
Relic Details
Relic Reveal
```

M1 should be able to navigate through the application before Firebase is fully connected.

---

# 25. Mock Data for M2 — Quest & Scan UI

M2 should use:

```text
R001
R002
R003
```

and scan states:

```text
READY
SCANNING
72%
85%
WAITING_FOR_PROXIMITY
REVEALED
```

M2 should never wait for M3/M4 hardware work before building the UI.

---

# 26. Mock Data for M3 — Location & Maps

M3 should use:

```text
R001–R006
```

and GPS cases:

```text
150 m
55 m
26 m
18 m
5 m
poor accuracy
```

M3 should test:

```text
permission granted
permission denied
geofence ENTER
geofence EXIT
location unavailable
poor accuracy
```

---

# 27. Mock Data for M4 — Sensor Fusion

M4 should use:

### Light

```text
80
100
150
180
200
250
300
320
321
450
600
```

### Motion

```text
STATIONARY
SCANNING
UNSTABLE
```

### Proximity

```text
FAR
NEAR
UNAVAILABLE
```

### Combined cases

```text
GOOD
BAD_LIGHT
BAD_MOTION
FAR
PROXIMITY_BLOCKED
SENSOR_MISSING
```

---

# 28. Mock Data for M5 — Firebase

M5 needs:

```text
U001–U005
R001–R006
leaderboard records
found records
authentication states
Firestore success/failure
```

M5 should seed the same canonical records used by the other members.

---

# 29. Mock Data for M6 — Room & Integration

M6 needs:

```text
RelicEntity
FoundRelicEntity
leaderboard cache if implemented
pendingSync records
offline records
duplicate discovery attempts
```

Important duplicate case:

```text
U001 discovers R001
U001 attempts to discover R001 again
```

Expected:

```text
No duplicate progress entry
```

The exact database constraint should be implemented according to the shared data contract.

---

# 30. Recommended Mock Repository

During early development, the application can use an in-memory implementation.

Conceptually:

```kotlin
class MockQuestRepository : QuestRepository {

    override suspend fun getRelics(): List<Relic> {
        return mockRelics
    }

    override suspend fun getFusionSignature(
        relicId: String
    ): LightSignature {
        return mockRelics
            .first { it.id == relicId }
            .lightSignature
    }

    override suspend fun recordReveal(
        relicId: String
    ) {
        // Add to mock progress
    }

    override fun observeLeaderboard():
        Flow<List<LeaderboardEntry>> {
        return MutableStateFlow(mockLeaderboard)
    }

    override suspend fun syncPending() {
        // Simulate successful sync
    }
}
```

This is an example implementation pattern.

The exact production interface must remain aligned with:

`07_SHARED_CONTRACTS_AND_AGREEMENTS.md`.

---

# 31. Mock Data Location in the Repository

Recommended structure:

```text
app/
└── src/
    └── main/
        └── java/.../
            ├── data/
            │   ├── mock/
            │   │   ├── MockUsers.kt
            │   │   ├── MockRelics.kt
            │   │   ├── MockLeaderboard.kt
            │   │   ├── MockScanCases.kt
            │   │   └── MockSensorData.kt
            │   ├── local/
            │   └── remote/
            ├── domain/
            └── ui/
```

If the team's actual package structure differs, keep the same logical separation.

---

# 32. Suggested Kotlin Mock Definitions

## Mock relics

```kotlin
val mockRelics = listOf(
    Relic(
        id = "R001",
        name = "Founder's Bell",
        lat = 6.974850,
        lng = 79.915300,
        radiusM = 25,
        lightSignature = LightSignature(
            min = 180,
            max = 320
        ),
        rarity = "Common",
        lore = "A bell associated with the earliest days of the campus."
    ),

    Relic(
        id = "R002",
        name = "Scholar's Compass",
        lat = 6.975420,
        lng = 79.914750,
        radiusM = 25,
        lightSignature = LightSignature(
            min = 250,
            max = 450
        ),
        rarity = "Uncommon",
        lore = "A symbolic compass representing the pursuit of knowledge."
    )
)
```

The remaining relics should use the canonical values in this document.

---

# 33. Mock Data Must Support Parallel Development

The purpose of this document is not only testing.

It enables this workflow:

```text
M1
↓
builds UI using MockQuestRepository

M2
↓
builds Scan UI using mock FusionResult

M3
↓
builds LocationService using mock locations

M4
↓
builds FusionEngine using mock sensor values

M5
↓
builds Firebase implementation using same IDs

M6
↓
builds Room implementation using same entities
```

Then:

```text
Mock implementation
        ↓
shared interfaces
        ↓
real implementation
        ↓
integration
```

This prevents a backend-first waterfall.

---

# 34. Where Each Developer Gets Their Data

| Member | Primary mock data |
|---|---|
| M1 | Users, navigation states, relic summaries |
| M2 | Relics, lore, scan states, fusion results |
| M3 | Relic coordinates, distances, geofence events |
| M4 | Light, accelerometer, proximity, sensor availability |
| M5 | Users, relics, progress, leaderboard |
| M6 | Relics, found records, offline/sync states |

---

# 35. Data Ownership

Mock data ownership does not mean one member can change shared values without agreement.

| Data | Owner | Changes require |
|---|---|---|
| User test accounts | M5 | Team agreement |
| Relic definitions | M5 | M3 + M4 + M5 agreement |
| Coordinates | M3 | M5 informed |
| Geofence radius | M3 | M4 informed if it affects fusion |
| Light signatures | M4 | M5 informed |
| Lore | M2 | M1 informed |
| Fusion test values | M4 | M2 informed |
| Room test records | M6 | M5 informed |
| Leaderboard examples | M5 | M6 informed |

---

# 36. How to Add New Mock Data

Do not simply add random values to your local branch.

Use this procedure:

```text
1. Identify missing test case
2. Create a unique ID
3. Define expected behaviour
4. Check whether an existing case already covers it
5. Tell affected members
6. Update this catalog
7. Merge the shared data change
8. Update dependent tests
```

Example:

```text
Need:
Test relic with missing light signature

New ID:
R007

Expected:
Sensor fusion handles missing signature without crash
```

Then add it to the shared catalog before using it across branches.

---

# 37. Mandatory Test Cases Before Integration

At minimum, the team must demonstrate:

### Authentication

- valid login
- invalid login
- logged-out state

### Location

- permission granted
- permission denied
- far from relic
- near relic
- geofence ENTER
- geofence EXIT
- poor accuracy

### Sensors

- all sensors available
- light unavailable
- accelerometer unavailable
- proximity unavailable
- stationary
- scanning movement
- wrong light environment
- correct light environment

### Fusion

- low score
- medium score
- threshold reached
- proximity blocks reveal
- proximity permits reveal
- degraded sensor mode

### Database

- save discovery
- read discovery
- duplicate discovery
- app restart
- offline save

### Cloud

- successful sync
- failed sync
- retry
- leaderboard update

---

# 38. Full End-to-End Mock Scenario

Use this scenario when the team needs to prove that the application works before every real service is connected.

## Initial

```text
User: U001
Relics found: 0
Internet: ON
Location permission: GRANTED
All sensors: AVAILABLE
```

## Step 1 — Map

```text
R001 distance: 150 m
```

Display:

```text
Founder's Bell
150 m away
```

## Step 2 — Approach

```text
R001 distance: 18 m
```

## Step 3 — Geofence

```text
ENTER
```

## Step 4 — Scan

```text
Light: 250 lux
Motion: SCANNING
Fusion: 87
```

## Step 5 — Proximity

```text
Proximity: NEAR
```

## Step 6 — Reveal

```text
R001 discovered
```

## Step 7 — Local save

```text
Room:
R001
pendingSync = true
```

## Step 8 — Cloud sync

```text
Firestore:
U001 → R001
```

## Step 9 — Leaderboard

```text
Nimal Perera
Relics Found: 1
```

This is the primary integration scenario.

---

# 39. Full Offline Scenario

Initial:

```text
U001
Internet: OFF
```

User discovers:

```text
R001
```

Expected:

```text
Room:
R001
pendingSync = true
```

User closes the app.

User reopens it.

Expected:

```text
R001 still appears as discovered
```

Internet becomes available.

Expected:

```text
syncPending()
→ Firestore updated
→ pendingSync cleared
```

This scenario must be tested before final submission.

---

# 40. Full Sensor-Degradation Scenario

Initial:

```text
GPS: available
Light sensor: unavailable
Accelerometer: available
Proximity: available
```

The application should:

```text
Detect unavailable sensor
↓
Exclude unavailable signal
↓
Reweight available signals according to agreed algorithm
↓
Continue scan where supported
```

It must not:

```text
Crash
```

or:

```text
pretend a missing sensor returned a real value
```

---

# 41. Data That Should NOT Be Mocked Permanently

The following should ultimately come from their real implementations:

```text
Current GPS location
Real geofence transitions
Real accelerometer readings
Real ambient light
Real proximity readings
Firebase authentication
Firestore persistence
Room persistence
Network availability
```

Mocks are for development and controlled testing.

They are not a substitute for final device testing.

---

# 42. Physical Validation Before Final Demo

The canonical coordinates and sensor signatures are development values.

Before the final demonstration, the team must validate:

```text
GPS behaviour
Geofence radius
Light ranges
Motion detection
Proximity behaviour
Battery impact
Permission flow
Offline sync
```

If the physical environment produces substantially different light readings, M4 must recalibrate the light signatures.

If GPS accuracy makes a radius unreliable, M3 and M4 must agree on an adjustment.

Do not change only the UI to hide a hardware problem.

---

# 43. Mock Data Freeze

## First freeze

**18 September 2026**

By this point:

- IDs should be stable
- basic relic definitions should be stable
- user test accounts should be stable
- mock scan cases should exist

## Integration freeze

**23 September 2026**

After this:

- do not casually change IDs
- do not casually change field meanings
- do not casually change coordinates
- do not casually change sensor test expectations

Changes after the freeze require team agreement.

---

# 44. Mock Data Change Log

Use this format:

```text
Date:
Changed by:
Object:
Old value:
New value:
Reason:
Affected members:
Tests updated:
```

Example:

```text
Date: 2026-09-22
Changed by: M4
Object: R001 lightSignature
Old value: 180–320
New value: 160–340
Reason: Physical campus calibration
Affected members: M2, M5, M6
Tests updated: R001 light-match cases
```

---

# 45. Quick Reference — Canonical IDs

```text
USERS
U001  Nimal Perera
U002  Kavindi Silva
U003  Sahan Fernando
U004  Ayesha Jayasinghe
U005  Test Explorer

RELICS
R001  Founder's Bell
R002  Scholar's Compass
R003  Heritage Key
R004  Old Library Seal
R005  Garden Chronicle
R006  Clock Tower Relic
```

---

# 46. Quick Reference — Primary Demo

```text
USER
U001

RELIC
R001

DISTANCE
18 m

LIGHT
250 lux

MOTION
SCANNING

FUSION
87%

PROXIMITY
NEAR

RESULT
REVEALED

LOCAL
pendingSync = true

CLOUD
synced

LEADERBOARD
Nimal Perera = 1 relic
```

---

# 47. Relationship With the Other Shared Documents

This document provides **test/development values**.

It does not replace:

```text
07_SHARED_CONTRACTS_AND_AGREEMENTS.md
```

The next technical documents should define:

```text
09_FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md
10_SENSOR_FUSION_AND_LOCATION_SPECIFICATION.md
11_INTEGRATION_AND_HANDOFF_PLAN.md
12_TESTING_ACCEPTANCE_AND_DEVICE_MATRIX.md
13_TEAM_GIT_COMMUNICATION_AND_CHANGE_CONTROL.md
```

The six member workplans will reference this catalog instead of inventing separate mock datasets.

---

# 48. Final Mock Data Checklist

Before feature development:

- [ ] U001–U005 available
- [ ] R001–R006 available
- [ ] all relic coordinates defined
- [ ] all radii defined
- [ ] all light signatures defined
- [ ] all lore available
- [ ] leaderboard data available
- [ ] GPS cases available
- [ ] geofence ENTER/EXIT cases available
- [ ] light sensor cases available
- [ ] accelerometer cases available
- [ ] proximity cases available
- [ ] sensor-unavailable cases available
- [ ] fusion-score cases available
- [ ] offline case available
- [ ] Firebase failure case available
- [ ] duplicate-discovery case available

Before integration:

- [ ] all developers use the same IDs
- [ ] all developers use the same data models
- [ ] no UI hard-codes production data
- [ ] mock repository can be swapped for real repository
- [ ] M2 can display M4's mock fusion result
- [ ] M3 can provide M4 with mock GPS distance
- [ ] M5 and M6 use matching relic/progress IDs

Before final demo:

- [ ] coordinates physically validated
- [ ] geofence tested
- [ ] sensor ranges calibrated
- [ ] degraded-sensor behaviour tested
- [ ] offline sync tested
- [ ] final mock/seed data frozen

---

# 49. Core Principle

The mock-data system exists to make the six-person team **independent without making the six implementations incompatible**.

The rule is:

```text
Build independently
       ↓
Use shared IDs
       ↓
Use shared models
       ↓
Use shared interfaces
       ↓
Test with shared mock data
       ↓
Integrate incrementally
       ↓
Replace mocks with real implementations
```

No member should need to wait for another member to finish the entire application before beginning their own work.
