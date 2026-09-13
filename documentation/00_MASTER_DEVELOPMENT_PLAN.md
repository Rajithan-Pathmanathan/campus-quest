# CAMPUS QUEST
# MASTER DEVELOPMENT, TEAM EXECUTION & INTEGRATION PLAN

**Project:** Campus Quest  
**Module:** Mobile Application Development  
**Platform:** Native Android / Android Studio  
**Architecture:** MVVM + Repository + Room + Firebase  
**Team:** 6 members  
**Development period:** One-month implementation window  
**Document type:** Master execution document  
**Status:** Working master plan — update only through agreed change control

---

# 0. DOCUMENT CONTROL

## 0.1 Purpose

This document is the single master execution plan for developing Campus Quest.

It brings together:

- project definition;
- complete user journey;
- screen flow;
- architecture;
- six-member responsibilities;
- dependencies;
- shared contracts;
- mock data;
- Firebase;
- Room;
- sensor fusion;
- location/geofencing;
- integration;
- testing;
- Git workflow;
- daily schedule;
- risks;
- demo requirements;
- final submission preparation.

The purpose is to prevent the six members from building isolated pieces that only meet for the first time near the deadline.

## 0.2 Master rule

The team develops in parallel but integrates incrementally.

```text
Plan
 ↓
Build independently with mocks where necessary
 ↓
Integrate small pieces early
 ↓
Test continuously
 ↓
Calibrate physical features
 ↓
Freeze core contracts
 ↓
Final integration
 ↓
Final testing
```

## 0.3 Source and implementation status

The project materials establish the mobile-development concepts, Android architecture, lifecycle, MVVM, separation of concerns, native/cross-platform considerations, layouts, Room/Firestore concepts, and the Campus Quest project direction.

Some implementation values in this document are **proposed working values**, not lecturer-mandated values. In particular:

- exact sensor-fusion weights;
- fusion/reveal thresholds;
- smoothing parameters;
- motion sensitivity;
- final light ranges;
- some repository/interface names;
- exact Firebase security-rule syntax;
- exact local schema decisions.

Those items must be tested, calibrated, and frozen by the team before the final build.

---

# 1. PROJECT DEFINITION

## 1.1 Project name

**Campus Quest**

## 1.2 Concept

Campus Quest is a location-aware campus treasure-hunt application in which users discover virtual relics around a campus.

The core differentiator is a **sensor-fusion discovery mechanic**.

The user does not simply walk to a coordinate and press a button. After entering the target's geofenced area, the application opens Scan Mode. A live fusion score is generated from:

1. GPS proximity/distance;
2. ambient light signature matching;
3. accelerometer-based scanning motion.

After the fusion condition is satisfied, the **proximity sensor provides a separate final close-range confirmation** before the relic is revealed.

## 1.3 Core loop

```text
Firebase Authentication
        ↓
Map
        ↓
Choose / approach relic
        ↓
Fused Location Provider
        ↓
Geofence ENTER
        ↓
Scan Mode
        ↓
GPS proximity + Light + Accelerometer
        ↓
Weighted fusion score
        ↓
Fusion threshold reached
        ↓
Proximity final gate
        ↓
Relic revealed
        ↓
Lore / reward
        ↓
Room local persistence
        ↓
Firestore synchronization
        ↓
Leaderboard
```

## 1.4 Critical mechanic rule

```text
GPS + LIGHT + ACCELEROMETER
            ↓
       FUSION SCORE
            ↓
     threshold reached?
            ↓
     PROXIMITY GATE
            ↓
         REVEAL
```

**Proximity is NOT part of the fusion score.**

It has no fusion weight.

It is only the final confirmation gate.

## 1.5 Scope principle

The lecturer expects the project to demonstrate concepts taught in the module, but the application must remain achievable within the available one-month implementation period.

Therefore:

### Must-have

- Android application;
- authentication;
- map;
- location;
- geofencing;
- sensor fusion;
- proximity confirmation;
- relic reveal;
- narrative/lore;
- Material UI;
- MVVM;
- repository separation;
- Room;
- Firebase/Firestore;
- leaderboard;
- offline/local persistence;
- graceful sensor degradation;
- testing.

### Stretch

- cross-platform proof of concept;
- team relay mode;
- crowd-aware routing;
- collectible badges/rarity expansion;
- advanced CameraX legendary relic.

Stretch work is cut first if the MVP is behind schedule.

---

# 2. PLATFORM DECISION

## 2.1 Selected platform

The project uses **native Android development with Android Studio**.

The module's platform-selection material distinguishes native, hybrid, and cross-platform approaches and identifies deep hardware access as a reason to favor native development.

Campus Quest uses:

- accelerometer;
- light sensor;
- proximity sensor;
- location;
- geofencing;
- Android lifecycle;
- Android-specific APIs.

Therefore native Android is a defensible choice for this particular project.

## 2.2 Why not make cross-platform the primary implementation?

Cross-platform development could reduce duplicated code when targeting Android and iOS, but this project has only one primary Android target and requires direct access to Android sensor/location APIs within a short academic implementation period.

The team therefore prioritizes:

```text
hardware access
+
Android integration
+
one-month delivery
```

rather than building an additional cross-platform abstraction.

A cross-platform proof of concept remains optional only.

---

# 3. PROJECT OBJECTIVES

## 3.1 Functional objectives

The application should allow a user to:

1. authenticate;
2. view campus relics;
3. navigate toward relic locations;
4. enter a geofenced relic area;
5. activate Scan Mode;
6. observe a live fusion score;
7. perform the intended scanning motion;
8. match the relic's light signature;
9. approach sufficiently close according to location;
10. receive proximity confirmation;
11. reveal the relic;
12. read its lore;
13. save discovery progress;
14. continue using cached data when offline;
15. synchronize progress when connectivity returns;
16. view leaderboard/progress.

## 3.2 Technical objectives

The project should demonstrate:

- MVVM;
- repository pattern;
- separation of concerns;
- Android lifecycle handling;
- Material UI;
- navigation;
- Fused Location Provider;
- geofencing;
- sensors;
- sensor fusion;
- Room;
- Firebase Authentication;
- Firestore;
- synchronization;
- testing.

---

# 4. USER PERSONA / ROLE

The primary user is a student or campus visitor participating in a campus treasure hunt.

The user should not need to understand:

- GPS algorithms;
- sensor APIs;
- Firestore;
- Room;
- MVVM.

The complexity belongs inside the implementation.

The visible experience should communicate:

```text
Where should I go?
        ↓
Am I close?
        ↓
How strong is the scan?
        ↓
What should I do?
        ↓
Did I find it?
        ↓
What did I discover?
```

---

# 5. COMPLETE USER JOURNEY

## 5.1 Entry

```text
Open app
 ↓
Splash / initialization
 ↓
Authentication check
```

If not authenticated:

```text
Login
 ↓
Firebase Auth
 ↓
success
 ↓
Main application
```

If already authenticated:

```text
Main application
```

## 5.2 Explore

```text
Map
 ↓
Relic markers
 ↓
Select relic
 ↓
Quest details
```

The user sees:

- relic name;
- rarity;
- approximate target;
- short description;
- objective;
- progress/status.

## 5.3 Approach

The user walks toward the target.

M3 manages:

- location permission;
- Fused Location Provider;
- distance;
- geofence.

The application should avoid continuously polling at an unnecessarily high rate when geofencing is sufficient.

## 5.4 Geofence entry

```text
ENTER target geofence
 ↓
application recognizes proximity
 ↓
Scan Mode becomes available/active
```

The geofence event is owned by M3.

## 5.5 Scan

M2 presents:

- scan instructions;
- live percentage;
- sensor/fusion feedback;
- progress state.

M4 supplies:

- GPS score;
- light score;
- motion score;
- fusion result;
- sensor availability;
- proximity state;
- reveal permission.

## 5.6 Final confirmation

When the fusion threshold is reached:

```text
fusion threshold reached
        ↓
check proximity
```

If proximity is not confirmed:

```text
keep reveal blocked
```

If proximity is confirmed:

```text
reveal allowed
```

## 5.7 Reveal

M2 displays:

- relic identity;
- visual reveal;
- rarity;
- lore;
- success state.

## 5.8 Persistence

After a successful reveal:

```text
Repository
 ↓
Room
 ↓
FoundRelicEntity
 ↓
pendingSync = true
```

Then:

```text
Firestore
 ↓
successful sync
 ↓
pendingSync = false
```

## 5.9 Progress

The user can see:

- discovered relics;
- total count;
- rarity/progress where implemented;
- leaderboard position.

---

# 6. SCREEN FLOW

## 6.1 Screen map

```text
Splash
  ↓
Login
  ↓
Main Shell
 ├── Map
 │    ↓
 │   Relic Details
 │    ↓
 │   Scan Mode
 │    ↓
 │   Reveal
 │    ↓
 │   Lore / Success
 │
 ├── Quests / Progress
 │
 ├── Leaderboard
 │
 └── Profile
```

## 6.2 Splash

Responsibilities:

- initialize app;
- determine authentication state;
- route to Login or Main Shell.

No business logic beyond startup coordination.

## 6.3 Login

Inputs:

```text
email
password
```

Outputs:

```text
success
failure
loading
```

M5 owns Firebase authentication.

M1 owns the screen and navigation.

## 6.4 Main shell

M1 owns:

- bottom navigation;
- toolbar;
- theme;
- navigation;
- shared UI structure.

## 6.5 Map

M3 owns:

- Google Maps;
- location permission;
- current location;
- relic markers;
- distance;
- geofence integration.

M2 supplies presentation around quest selection where appropriate.

## 6.6 Quest details

M2 owns:

- relic information;
- instructions;
- quest status;
- transition into Scan Mode.

## 6.7 Scan Mode

M2 owns visual presentation.

M4 owns the scan calculation.

The UI must not read raw `SensorEvent` values directly.

## 6.8 Reveal

M2 owns:

- animation;
- success message;
- relic information;
- lore.

M6/M5 receive the durable completion event through the repository boundary.

## 6.9 Progress

Displays locally/cloud-backed:

- discovered relics;
- count;
- completion state.

## 6.10 Leaderboard

Displays:

- ranking;
- display name;
- unique relic count.

M5 owns cloud leaderboard data.

---

# 7. SYSTEM ARCHITECTURE

## 7.1 Architecture

```text
┌───────────────────────────────┐
│            UI Layer           │
│ Activities / Fragments / UI   │
└───────────────┬───────────────┘
                ↓
┌───────────────────────────────┐
│          ViewModel            │
│ UI state + user actions       │
└───────────────┬───────────────┘
                ↓
┌───────────────────────────────┐
│          Repository           │
│ application data boundary     │
└───────────────┬───────────────┘
                ↓
       ┌────────┴─────────┐
       ↓                  ↓
┌──────────────┐   ┌──────────────┐
│     Room     │   │  Firestore   │
│ local/cache  │   │ cloud/shared │
└──────────────┘   └──────────────┘
```

## 7.2 MVVM rationale

The module materials emphasize that MVVM:

- separates View from UI-related state/logic;
- supports lifecycle survival through ViewModel;
- improves testability;
- works with observable state.

The project therefore avoids putting the complete business logic into Activities/Fragments.

## 7.3 Separation of concerns

The intended responsibility split is:

```text
UI
→ displays and forwards actions

ViewModel
→ coordinates UI state

Repository
→ determines where data comes from

Room
→ local persistence

Firestore
→ cloud persistence

M3
→ location/geofencing

M4
→ sensors/fusion

M5
→ Firebase

M6
→ Room/integration
```

---

# 8. COMPONENT BOUNDARIES

## 8.1 M1

Owns:

- app shell;
- navigation;
- theme;
- profile;
- login presentation;
- shared UI structure.

## 8.2 M2

Owns:

- quest UI;
- scan UI;
- fusion meter presentation;
- reveal;
- lore;
- progress presentation;
- leaderboard presentation.

## 8.3 M3

Owns:

- location permission;
- Fused Location Provider;
- Google Maps;
- distance;
- geofence.

## 8.4 M4

Owns:

- light sensor;
- accelerometer;
- proximity;
- sensor availability;
- motion classification;
- light matching;
- fusion;
- degradation;
- sensor lifecycle.

## 8.5 M5

Owns:

- Firebase project;
- Firebase Auth;
- Firestore;
- cloud repository;
- user profiles;
- cloud progress;
- leaderboard;
- security rules.

## 8.6 M6

Owns:

- Room;
- local cache;
- local progress;
- pending synchronization;
- build stability;
- integration coordination.

---

# 9. SIX-MEMBER TEAM STRUCTURE

| Member | Role | Primary ownership |
|---|---|---|
| M1 | Frontend Lead | App shell, navigation, theme, profile, auth UI |
| M2 | Frontend Developer | Quest, Scan Mode, reveal, progress, leaderboard UI |
| M3 | Device Developer | Location, Maps, distance, geofencing |
| M4 | Device Developer | Sensors, fusion, proximity |
| M5 | Backend Developer | Firebase Auth, Firestore, cloud sync |
| M6 | Data & Integration Developer | Room, offline state, integration/build |

---

# 10. DETAILED MEMBER RESPONSIBILITIES

## 10.1 M1 — UI/UX/NAVIGATION

### Own

- Splash;
- Login screen presentation;
- main shell;
- bottom navigation;
- toolbar;
- profile;
- app theme;
- navigation graph;
- shared UI conventions.

### Dependencies

- M5 auth state;
- M2 screens;
- shared domain models.

### First milestone

By September 19:

```text
entire app navigable with fake data
```

## 10.2 M2 — QUEST/SCAN UI

### Own

- quest list;
- quest details;
- Scan Mode;
- scan instructions;
- fusion meter;
- reveal;
- lore;
- progress presentation;
- leaderboard presentation.

### Dependencies

- M4 `FusionResult`;
- M3 location/geofence state;
- M5 data;
- M6 progress.

### First milestone

By September 19:

```text
core game screens work with fake sensor data
```

## 10.3 M3 — LOCATION/GEOFENCING

### Own

- permission;
- Google Maps;
- Fused Location Provider;
- current location;
- distance;
- geofences;
- ENTER event;
- battery-conscious location.

### First milestone

September 13–15:

```text
map + current location
```

September 16–18:

```text
distance + geofence
```

September 19:

```text
physical geofence
```

## 10.4 M4 — SENSOR FUSION

### Own

- SensorManager;
- light;
- accelerometer;
- proximity;
- motion classification;
- light matching;
- normalized scoring;
- weighted fusion;
- degradation;
- lifecycle.

### Important

GPS input comes from M3.

M4 must not create a duplicate location pipeline.

### First milestone

September 18:

```text
sensor fusion works with mock inputs
```

September 22:

```text
campus calibration
```

September 23:

```text
sensor contract frozen
```

## 10.5 M5 — FIREBASE

### Own

- Firebase setup;
- Auth;
- user profiles;
- relic collection;
- progress;
- leaderboard;
- security rules;
- cloud repository.

### First milestone

September 19:

```text
repository/cloud path working
```

## 10.6 M6 — ROOM/INTEGRATION

### Own

- Room database;
- DAOs;
- local repository;
- relic cache;
- discovered relic persistence;
- pendingSync;
- cloud acknowledgement boundary;
- build verification;
- integration coordination.

### First milestone

September 19:

```text
R001 local persistence works
```

---

# 11. DEPENDENCY MATRIX

| Feature | M1 | M2 | M3 | M4 | M5 | M6 |
|---|---:|---:|---:|---:|---:|---:|
| App shell | Own | - | - | - | - | - |
| Login UI | Own | - | - | Auth | Auth | - |
| Map | - | UI support | Own | distance input | relic data | cache |
| Quest | - | Own | location | sensor | relic data | progress |
| Scan UI | - | Own | distance | Own | signature | - |
| Fusion | - | display | GPS | Own | signature | - |
| Reveal | - | Own | proximity context | final gate | - | persistence |
| Room | - | - | - | - | sync | Own |
| Firebase | - | - | - | - | Own | sync |
| Leaderboard | shell | presentation | - | - | data | cache |
| Testing | support | support | Own tests | Own tests | Own tests | coordination |

---

# 12. SHARED DOMAIN MODELS

## 12.1 Relic

```kotlin
data class Relic(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val lightSignature: LightSignature,
    val rarity: String,
    val lore: String
)
```

## 12.2 LightSignature

```kotlin
data class LightSignature(
    val minLux: Float,
    val maxLux: Float
)
```

## 12.3 LocationSignal

Conceptual:

```kotlin
data class LocationSignal(
    val distanceMeters: Float,
    val accuracyMeters: Float?
)
```

M3 provides this information.

## 12.4 SensorAvailability

```kotlin
data class SensorAvailability(
    val lightAvailable: Boolean,
    val accelerometerAvailable: Boolean,
    val proximityAvailable: Boolean
)
```

## 12.5 FusionResult

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

## 12.6 LeaderboardEntry

```kotlin
data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val relicsFound: Int,
    val lastUpdate: Instant?
)
```

Exact time representation may follow the project's dependency choices.

---

# 13. REPOSITORY CONTRACT

A shared conceptual interface:

```kotlin
interface QuestRepository {

    suspend fun getRelics(): List<Relic>

    suspend fun getFusionSignature(
        relicId: String
    ): LightSignature

    suspend fun recordReveal(
        relicId: String
    )

    fun observeLeaderboard():
        Flow<List<LeaderboardEntry>>

    suspend fun syncPending()
}
```

This is a proposed contract and may be split into specialized repositories if the team agrees.

Possible split:

```text
AuthRepository
QuestRepository
ProgressRepository
LeaderboardRepository
```

The team must avoid uncontrolled contract changes after integration begins.

---

# 14. AUTH CONTRACT

Conceptual:

```kotlin
interface AuthRepository {

    fun observeAuthState(): Flow<AuthUser?>

    suspend fun login(
        email: String,
        password: String
    )

    suspend fun logout()
}
```

If registration is implemented:

```kotlin
suspend fun register(...)
```

may be added.

## AuthUser

```kotlin
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)
```

Firebase `FirebaseUser` should not spread into UI code unnecessarily.

---

# 15. FIREBASE SCHEMA

## 15.1 users

```text
users/{uid}
```

Fields:

```text
displayName
email
createdAt
```

Firebase-generated UID is authoritative.

Development IDs such as U001 are not Firebase UIDs.

## 15.2 relics

```text
relics/{relicId}
```

Fields:

```text
id
name
lat
lng
radiusM
lightSignature
rarity
lore
```

## 15.3 progress

```text
progress/{uid}/found/{relicId}
```

Fields:

```text
relicId
foundAt
```

## 15.4 leaderboard

```text
leaderboard/{uid}
```

Fields:

```text
uid
displayName
relicsFound
lastUpdate
```

---

# 16. CANONICAL DEVELOPMENT DATA

## 16.1 Users

| ID | Name |
|---|---|
| U001 | Nimal Perera |
| U002 | Kavindi Silva |
| U003 | Sahan Fernando |
| U004 | Ayesha Jayasinghe |
| U005 | Test Explorer |

These are development identities only.

## 16.2 Relics

| ID | Name | Lat | Lng | Radius | Light | Rarity |
|---|---|---:|---:|---:|---:|---|
| R001 | Founder’s Bell | 6.974850 | 79.915300 | 25m | 180–320 | Common |
| R002 | Scholar’s Compass | 6.975420 | 79.914750 | 25m | 250–450 | Uncommon |
| R003 | Heritage Key | 6.975900 | 79.915650 | 20m | 80–180 | Rare |
| R004 | Old Library Seal | 6.976300 | 79.914900 | 30m | 400–650 | Common |
| R005 | Garden Chronicle | 6.974300 | 79.916100 | 25m | 120–250 | Uncommon |
| R006 | Clock Tower Relic | 6.976750 | 79.915700 | 20m | 300–500 | Rare |

These are development values.

**Coordinates must be physically validated before final demonstration.**

**Light signatures must be calibrated by M4 before being treated as final values.**

---

# 17. ROOM DATABASE

## 17.1 RelicEntity

Suggested:

```kotlin
@Entity(tableName = "relics")
data class RelicEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val lightMin: Float,
    val lightMax: Float,
    val rarity: String,
    val lore: String
)
```

## 17.2 FoundRelicEntity

Recommended robust local form:

```kotlin
@Entity(
    tableName = "found_relics",
    primaryKeys = ["userId", "relicId"]
)
data class FoundRelicEntity(
    val userId: String,
    val relicId: String,
    val foundAt: Long,
    val pendingSync: Boolean
)
```

If the team deliberately uses a single-user local assumption, the userId decision must be documented.

## 17.3 DAO responsibilities

### RelicDao

```text
observeRelics
getRelic
upsertAll
```

### FoundRelicDao

```text
observeFound
getFound
getPendingSync
insert
markSynced
```

---

# 18. LOCAL/CLOUD RESPONSIBILITY

| Requirement | Room | Firebase |
|---|---|---|
| Relic cache | Yes | Authoritative |
| Found relic local state | Yes | Authoritative shared copy |
| pendingSync | Yes | No |
| User profile | optional cache | Yes |
| Leaderboard | optional cache | Yes |
| Raw sensor data | No | No |
| Temporary scan state | No | No |

---

# 19. OFFLINE STRATEGY

The application should not lose a successful discovery because the network disappears.

Flow:

```text
Scan succeeds
 ↓
recordReveal()
 ↓
Room stores discovery
 ↓
pendingSync = true
 ↓
network available?
 ├── yes → upload
 └── no → retain
```

Later:

```text
network restored
 ↓
syncPending()
 ↓
Firestore success
 ↓
pendingSync = false
```

---

# 20. SYNC IDEMPOTENCY

Use deterministic cloud identity:

```text
progress/{uid}/found/{relicId}
```

If the same upload occurs twice:

```text
same document
```

rather than creating multiple unique discoveries.

Local primary identity should also prevent duplicates.

---

# 21. SENSOR FUSION SPECIFICATION

## 21.1 Inputs

Fusion inputs:

```text
GPS proximity
Light signature
Accelerometer motion
```

Final gate:

```text
Proximity
```

## 21.2 Normalization

All fusion components should produce:

```text
0.0 → 1.0
```

Then:

```text
score → 0 → 100%
```

## 21.3 Initial development example

Provisional example:

```text
GPS       = 0.40
Light     = 0.35
Motion    = 0.25
```

Formula:

```text
score =
    gpsScore    × 0.40
  + lightScore  × 0.35
  + motionScore × 0.25
```

These weights are **not final requirements**.

M4 must calibrate them and communicate the final values.

---

# 22. LIGHT MATCHING

For a relic:

```text
minLux
maxLux
```

If:

```text
minLux <= currentLux <= maxLux
```

the match should be strong.

Outside the range:

```text
distance from nearest boundary
        ↓
lower score
```

The exact decay curve is provisional.

Example:

```text
R001 = 180–320 lux
current = 250 lux
→ strong match
```

---

# 23. ACCELEROMETER MOTION

The intended behavior is a slow side-to-side scanning motion.

Conceptual pipeline:

```text
accelerometer
 ↓
preprocessing
 ↓
noise handling
 ↓
movement window
 ↓
sweep classification
 ↓
motion score
```

Expected qualitative behavior:

```text
stationary → low
random movement → low/medium
controlled sweep → high
sharp shaking → low/medium
```

Exact thresholds must be calibrated.

Do not require a perfect waveform.

---

# 24. GPS FUSION INPUT

M3 supplies:

```text
distanceMeters
accuracyMeters
```

M4 normalizes distance.

A simple provisional approach:

```text
outside target radius → low
closer to target → higher
very close → high
```

Accuracy should be considered when interpreting confidence.

M4 must not create a second GPS provider.

---

# 25. SENSOR DEGRADATION

If a fusion sensor is missing:

```text
remove unavailable component
redistribute active weights
```

Example:

```text
GPS 0.40
Light 0.35
Motion 0.25
```

Light missing:

```text
available total = 0.65
```

Effective:

```text
GPS = 0.40 / 0.65
Motion = 0.25 / 0.65
```

Active weights sum to:

```text
1.0
```

The app should not simply force a missing sensor to zero.

---

# 26. PROXIMITY GATE

Critical rule:

```text
Proximity has no fusion weight.
```

Correct:

```text
GPS + Light + Motion
        ↓
Fusion Score
        ↓
threshold
        ↓
Proximity
        ↓
Reveal
```

Incorrect:

```text
GPS + Light + Motion + Proximity
        ↓
Fusion Score
```

Conceptual:

```kotlin
revealAllowed =
    fusionScore >= revealThreshold &&
    proximityConfirmed
```

The exact reveal threshold is provisional until calibration.

---

# 27. SENSOR LIFECYCLE

Sensor listeners should only run during Scan Mode.

```text
Map
 ↓
No sensor listeners
 ↓
Scan starts
 ↓
register available sensors
 ↓
scan
 ↓
reveal/exit
 ↓
unregister sensors
```

M4 must test:

- repeated scans;
- screen changes;
- background/foreground;
- lifecycle recreation;
- duplicate registration.

---

# 28. BATTERY STRATEGY

M3:

- geofencing;
- efficient location updates.

M4:

- sensor registration only during Scan Mode;
- no permanent accelerometer listener;
- no permanent light listener;
- no permanent proximity listener;
- avoid excessive UI updates;
- stop processing after scan completion.

---

# 29. LOCATION/GEOFENCING SPECIFICATION

## 29.1 M3 responsibilities

- location permissions;
- Maps SDK;
- current location;
- distance calculation;
- geofence creation;
- geofence ENTER event;
- location accuracy;
- battery-conscious strategy.

## 29.2 Flow

```text
Relic configuration
 ↓
geofence registered
 ↓
user approaches
 ↓
ENTER event
 ↓
Scan Mode
```

## 29.3 Important rule

Do not build continuous high-frequency location polling when geofencing provides the required trigger.

## 29.4 Distance

Distance should be expressed in:

```text
meters
```

and supplied through the agreed interface.

---

# 30. LOCATION PERMISSION FLOW

Conceptually:

```text
Map opened
 ↓
permission?
 ├── granted → location
 └── denied → explain / restricted state
```

The app must not crash when permission is denied.

---

# 31. GEOFENCE DESIGN

Each relic can have:

```text
latitude
longitude
radiusM
```

The configured radius is part of relic data.

Development examples:

```text
R001 = 25m
R002 = 25m
R003 = 20m
R004 = 30m
R005 = 25m
R006 = 20m
```

These values remain subject to physical validation.

---

# 32. GEOFENCE TO SCAN TRANSITION

M3 should provide an event such as:

```text
RelicEntered(relicId)
```

M2/ViewModel can then:

```text
open/enable Scan Mode
```

M4 starts sensors only once Scan Mode begins.

---

# 33. MOCK-FIRST DEVELOPMENT

No member should wait for every dependency before writing code.

## M1 mock

Use:

```text
fake navigation
fake user
fake relics
```

## M2 mock

Use:

```text
fake FusionResult
```

## M3 mock

Use:

```text
fake distance
fake geofence event
```

## M4 mock

Use:

```text
fake GPS
fake lux
fake motion
fake proximity
```

## M5 mock

Use:

```text
FakeAuthRepository
FakeQuestRepository
```

## M6 mock

Use:

```text
in-memory/local fake repository
```

---

# 34. MOCK SCENARIOS

## Scenario A — Perfect/strong scan

```text
distance close
light matching
motion good
proximity true
```

Expected:

```text
high fusion
reveal allowed
```

## Scenario B — Too far

```text
distance far
light matching
motion good
```

Expected:

```text
reduced score
```

## Scenario C — Wrong light

```text
distance close
light mismatch
motion good
```

Expected:

```text
reduced score
```

## Scenario D — Wrong motion

```text
distance close
light matching
motion poor
```

Expected:

```text
reduced score
```

## Scenario E — Proximity blocks

```text
fusion threshold reached
proximity false
```

Expected:

```text
reveal blocked
```

## Scenario F — Light unavailable

Expected:

```text
no crash
available weights redistributed
```

---

# 35. R001 VERTICAL SLICE

R001 is the primary integration target.

```text
R001 Founder’s Bell
 ↓
Map marker
 ↓
geofence
 ↓
Scan Mode
 ↓
GPS + light + motion
 ↓
fusion
 ↓
proximity
 ↓
reveal
 ↓
Room
 ↓
Firestore
 ↓
leaderboard
```

If the project falls behind, make R001 reliable before expanding polish.

---

# 36. TESTING STRATEGY

Testing must happen continuously.

Three levels:

```text
Unit
 ↓
Component/instrumented
 ↓
End-to-end
```

---

# 37. UNIT TESTS

## M1

- navigation state;
- ViewModel state;
- basic UI-state transformations.

## M2

- scan-state transitions;
- reveal state;
- progress display mapping.

## M3

- distance normalization;
- permission-state handling;
- geofence event mapping.

## M4

- light normalization;
- light matching;
- motion classification;
- fusion;
- degradation;
- proximity gate.

## M5

- data mapping;
- repository error mapping;
- duplicate-progress logic where testable.

## M6

- entity/domain mapping;
- local repository;
- duplicate prevention;
- sync state transitions.

---

# 38. M4 FUSION TESTS

Required:

```text
all signals available
light missing
motion missing
multiple signals missing
only GPS
```

Proximity:

```text
threshold false + proximity true = false
threshold true + proximity false = false
threshold true + proximity true = true
```

---

# 39. ROOM TESTS

Required:

```text
insert R001
read R001
insert R001 twice
query found relics
query pending
mark synced
restart persistence
```

---

# 40. FIREBASE TESTS

Required:

```text
valid login
invalid login
logout
relic retrieval
progress write
duplicate discovery
leaderboard update
cross-user protection
protected relic write
leaderboard manipulation attempt
```

---

# 41. LOCATION TESTS

Required:

```text
permission granted
permission denied
location unavailable
distance calculation
geofence enter
geofence exit
accuracy variation
repeated geofence events
```

---

# 42. END-TO-END TESTS

## Test 1 — Login

```text
launch
→ login
→ main shell
```

## Test 2 — R001

```text
login
→ map
→ R001
→ geofence
→ scan
→ reveal
→ Room
```

## Test 3 — Cloud

```text
reveal
→ Firestore
→ leaderboard
```

## Test 4 — Offline

```text
cache relic
→ disable network
→ discover
→ Room pending
→ restore network
→ sync
```

## Test 5 — Restart

```text
discover
→ close
→ reopen
→ discovered state remains
```

---

# 43. TWO-DEVICE MATRIX

Test on at least two physical Android devices where available.

| Area | Device A | Device B |
|---|---|---|
| Light available | test | test |
| Accelerometer | test | test |
| Proximity | test | test |
| Light stability | test | test |
| Motion | test | test |
| Proximity gate | test | test |
| Fusion | test | test |
| Missing sensor behavior | test | test |
| Room | test | test |
| Firebase | test | test |
| Full R001 | test | test |

Sensor readings should not be expected to be identical across devices.

---

# 44. CAMPUS CALIBRATION

Target date:

**September 22**

## 44.1 Light

Measure representative locations:

- bright outdoor area;
- shaded outdoor area;
- corridor;
- indoor room;
- target relic areas.

Record:

```text
relic
location
lux
time
device
environment
```

## 44.2 Motion

Test:

```text
stationary
slow left-right
slow right-left
walking
random movement
fast shaking
```

## 44.3 GPS

M3 validates target coordinates and real distance behavior.

## 44.4 Proximity

M4 validates:

```text
near
far
device orientation
sensor response
```

---

# 45. PARAMETER CALIBRATION RECORD

Every final parameter should have:

```text
Parameter
Initial value
Test condition
Observed issue
Final value
Reason
Device
Date
```

Parameters include:

- fusion weights;
- reveal threshold;
- motion sensitivity;
- smoothing;
- light mismatch decay;
- GPS normalization;
- update rate.

---

# 46. FIREBASE SECURITY

M5 owns security rules.

## Users

Authenticated user should be restricted to appropriate own-user operations.

## Progress

A user should not modify another user's progress.

## Relics

Normal clients should read but not arbitrarily modify relic configuration.

## Leaderboard

Clients should not freely set:

```text
relicsFound = 9999
```

The final rules must be actual deployed/tested Firestore rules, not only pseudocode.

---

# 47. SECURITY TEST MATRIX

| Test | Expected |
|---|---|
| User reads own profile | allowed as designed |
| User modifies own profile | allowed as designed |
| User modifies other profile | denied where protected |
| User writes own progress | allowed |
| User writes other progress | denied |
| Client edits relic coordinates | denied |
| Client changes leaderboard count arbitrarily | denied |

---

# 48. DATA FLOW

## 48.1 Relic loading

```text
Firestore
 ↓
M5
 ↓
Repository
 ↓
M6 Room cache
 ↓
ViewModel
 ↓
UI / M3 / M4
```

## 48.2 Scan

```text
M3 distance
+
M4 light
+
M4 motion
 ↓
SensorFusionEngine
 ↓
FusionResult
 ↓
M2 ViewModel
 ↓
Scan UI
```

## 48.3 Reveal

```text
FusionResult
 ↓
proximity gate
 ↓
revealAllowed
 ↓
M2 reveal
 ↓
recordReveal
 ↓
Room
 ↓
pendingSync
 ↓
Firebase
```

---

# 49. COMPLETE DATA FLOW

```text
AUTH
 ↓
Firebase UID
 ↓
USER PROFILE
 ↓
RELIC CATALOG
 ↓
ROOM CACHE
 ↓
MAP
 ↓
GEOFENCE
 ↓
SCAN MODE
 ↓
GPS ─────────┐
LIGHT ───────┼→ FUSION ENGINE
MOTION ──────┘
                  ↓
             FUSION SCORE
                  ↓
            threshold reached
                  ↓
             PROXIMITY
                  ↓
               REVEAL
                  ↓
             ROOM SAVE
                  ↓
            pendingSync
                  ↓
             FIRESTORE
                  ↓
             LEADERBOARD
```

---

# 50. AGILE-INSPIRED DEVELOPMENT APPROACH

The project should be described as:

> **Agile-inspired iterative development**

rather than claiming strict Scrum if the team is not implementing formal Scrum roles/events.

The approach means:

- independent feature development;
- parallel work;
- small increments;
- frequent testing;
- frequent integration;
- working software prioritized;
- core functionality before stretch functionality.

---

# 51. ITERATION STRUCTURE

## Iteration 1 — Foundation

**September 13–16**

Focus:

```text
project setup
authentication
navigation
map
sensor foundation
Room
Firebase
```

## Iteration 2 — Feature completion

**September 17–19**

Focus:

```text
scan UI
fusion
geofence
cloud data
progress
```

## Iteration 3 — MVP integration

**September 20–23**

Focus:

```text
real GPS
real sensors
Room
Firebase
R001
```

## Iteration 4 — Testing/refinement

**September 24–27**

Focus:

```text
full flow
multi-relic
offline
security
devices
bugs
```

## Finalization

**September 28**

Focus:

```text
final build
demo
documentation
submission
```

---

# 52. MASTER DAY-BY-DAY SCHEDULE

## September 13

### M1

- shell foundation;
- navigation.

### M2

- screen skeletons.

### M3

- Maps setup;
- permission.

### M4

- sensor discovery.

### M5

- Firebase project.

### M6

- Room foundation.

**Integration target:** project builds.

---

## September 14

### M1

- login UI.

### M2

- quest UI.

### M3

- current location.

### M4

- light sensor.

### M5

- Auth.

### M6

- relic cache.

**Integration target:** login + local relics.

---

## September 15

### M1

- navigation polish.

### M2

- quest details.

### M3

- distance.

### M4

- accelerometer.

### M5

- user profile.

### M6

- found relic.

**Integration target:** quest → local data.

---

## September 16

### M1

- shell stabilization.

### M2

- Scan Mode skeleton.

### M3

- geofence.

### M4

- proximity.

### M5

- relic Firestore.

### M6

- pendingSync.

**Integration target:** geofence → scan.

---

## September 17

### M1

- shared UI consistency.

### M2

- fusion meter.

### M3

- geofence physical test.

### M4

- fusion engine.

### M5

- progress.

### M6

- repository.

**Integration target:** fake sensor scan.

---

## September 18

### M1

- navigation testing.

### M2

- reveal.

### M3

- location refinement.

### M4

- unit tests.

### M5

- leaderboard.

### M6

- repository integration.

**Integration target:** mock full scan.

---

## September 19

### All members

First major integration.

Target:

```text
Login
→ Map
→ R001
→ mock/real geofence
→ Scan
→ Reveal
→ Room
```

No new optional features should be prioritized over this vertical slice.

---

## September 20

Connect:

```text
M3 real distance
M4 real sensors
M6 Room
M5 Firebase
```

Target:

```text
real R001 scan
```

---

## September 21

Focus:

```text
lifecycle
offline
repeated scans
permissions
error handling
```

---

## September 22

Campus calibration:

```text
light
motion
GPS
proximity
```

---

## September 23

Freeze:

```text
contracts
weights
thresholds
schemas
field names
sync semantics
```

No unnecessary architecture changes after freeze.

---

## September 24

Full end-to-end:

```text
Auth
→ Map
→ Geofence
→ Scan
→ Fusion
→ Reveal
→ Room
→ Firebase
→ Leaderboard
```

---

## September 25

Test:

```text
R001–R006
```

---

## September 26

Test:

```text
two devices
multiple users
offline/reconnect
```

---

## September 27

Bug-fixing day.

Priority:

```text
crashes
data loss
incorrect reveal
security
sensor failures
navigation
polish
```

---

## September 28

Final:

```text
clean build
installation
smoke test
demo
documentation
submission
```

---

# 53. INTEGRATION SCHEDULE

## Integration 0

```text
M1 shell + fake data
```

## Integration 1

```text
M1 + M2
```

## Integration 2

```text
M2 + M3
```

## Integration 3

```text
M2 + M4
```

## Integration 4

```text
M2 + M3 + M4
```

## Integration 5

```text
M6 Room
```

## Integration 6

```text
M5 Firebase
```

## Integration 7

```text
complete application
```

---

# 54. INTEGRATION RULES

1. Integrate early.
2. Do not wait until the last week.
3. Use mocks when dependencies are incomplete.
4. Every feature owner tests their own feature.
5. M6 coordinates integration but does not own every bug.
6. Shared contract changes must be communicated.
7. Never silently replace another member's implementation.
8. Run a smoke test after major merges.
9. Keep `main` buildable.
10. Prioritize R001.

---

# 55. GIT WORKFLOW

Recommended branches:

```text
main
├── feature/ui-navigation
├── feature/quest-scan
├── feature/location-geofence
├── feature/sensor-fusion
├── feature/firebase-sync
└── feature/room-data
```

No direct unfinished pushes to `main`.

---

# 56. PULL REQUEST RULES

Every PR should contain:

```text
Summary
Changes
Tests
Known issues
Dependencies
Shared contract changes
```

Before merge:

```text
build passes
tests pass
feature works
no obvious regression
```

---

# 57. COMMIT CONVENTION

Examples:

```text
feat: add map relic markers
feat: add geofence enter handling
feat: add light sensor matcher
feat: add fusion engine
feat: add Firebase authentication
feat: add Room found relic DAO
fix: prevent duplicate discovery
test: add sensor degradation tests
docs: update shared contract
```

Avoid:

```text
update
changes
final
stuff
```

as commit messages.

---

# 58. SHARED CONTRACT CHANGE CONTROL

A shared contract includes:

- domain models;
- repository interfaces;
- Firebase fields;
- Room fields;
- fusion result;
- location signal;
- scan state;
- sync semantics.

Change process:

```text
identify change
 ↓
notify affected members
 ↓
agree
 ↓
update shared document
 ↓
update implementations
 ↓
test
 ↓
merge
```

---

# 59. DEFINITION OF READY

A feature is Ready when:

- owner identified;
- inputs identified;
- outputs identified;
- dependencies identified;
- mock strategy available;
- acceptance criteria known.

---

# 60. DEFINITION OF DONE

A feature is Done when:

- implementation complete;
- tests pass;
- integrated with relevant module;
- no known blocking defect;
- documentation updated;
- PR reviewed/merged.

---

# 61. PROJECT DEFINITION OF DONE

Campus Quest is Done when:

## Core

- [ ] authentication;
- [ ] map;
- [ ] relic markers;
- [ ] geofence;
- [ ] Scan Mode;
- [ ] GPS fusion;
- [ ] light fusion;
- [ ] accelerometer fusion;
- [ ] proximity gate;
- [ ] reveal;
- [ ] lore;
- [ ] progress.

## Architecture

- [ ] MVVM;
- [ ] repository;
- [ ] separation of concerns;
- [ ] lifecycle-safe implementation.

## Data

- [ ] Room;
- [ ] Firebase;
- [ ] sync;
- [ ] leaderboard.

## Reliability

- [ ] missing sensors handled;
- [ ] offline discovery preserved;
- [ ] duplicate discovery prevented;
- [ ] security rules tested;
- [ ] two-device testing.

## Final

- [ ] R001 works;
- [ ] R001–R006 validated;
- [ ] final build works;
- [ ] demo works;
- [ ] documentation complete.

---

# 62. RISK MANAGEMENT

## Risk 1 — Scope becomes too large

Response:

```text
cut stretch features
```

Do not cut:

```text
core scan
location
Room
Firebase
```

before cutting optional features.

## Risk 2 — Hardware differences

Response:

```text
graceful degradation
two-device testing
calibration
```

## Risk 3 — GPS inaccurate

Response:

```text
geofence + distance + accuracy
```

Do not require an unrealistic exact coordinate.

## Risk 4 — Firebase integration late

Response:

```text
mock repository
integrate by Sep 20
```

## Risk 5 — Room/Firebase responsibility confusion

Response:

```text
Room = local
Firebase = cloud
```

## Risk 6 — Integration only at end

Response:

```text
vertical slice by Sep 19
```

## Risk 7 — Sensor fusion feels unreliable

Response:

```text
calibrate Sep 22
freeze Sep 23
```

## Risk 8 — Security rules left until final day

Response:

```text
test Sep 21
```

---

# 63. IF WE ARE BEHIND — DECISION TREE

## Situation A — App does not build

Stop feature work.

```text
fix build
 ↓
run smoke test
```

## Situation B — Core navigation works but sensors fail

Prioritize:

```text
R001
```

## Situation C — Sensor fusion works but Firebase is incomplete

Use local Room for the working demo while M5 finishes cloud integration.

Do not remove Firebase from the final MVP if it is a required project feature.

## Situation D — Advanced feature unfinished

Cut it.

## Situation E — Six relics are not all calibrated

Ensure:

```text
R001
```

works reliably, then validate additional relics.

## Situation F — UI polish is incomplete

Prioritize:

```text
functionality
stability
readability
```

over animations.

---

# 64. BACKUP MVP

If severe time pressure occurs, the fallback MVP is:

```text
Login
 ↓
Map
 ↓
R001
 ↓
Geofence
 ↓
Scan
 ↓
GPS + Light + Motion
 ↓
Proximity
 ↓
Reveal
 ↓
Room
 ↓
Firebase
```

Everything else is secondary.

---

# 65. ERROR HANDLING

## Authentication

```text
invalid credentials
network failure
unknown error
```

## Location

```text
permission denied
location unavailable
poor accuracy
```

## Sensors

```text
sensor unavailable
invalid reading
listener failure
```

## Firebase

```text
permission denied
network unavailable
write failure
```

## Room

```text
database error
migration error
```

Errors should become user-appropriate states rather than raw technical crashes.

---

# 66. STATE MANAGEMENT

The project should distinguish:

## UI state

Examples:

```text
loading
error
scan progress
dialog visibility
```

## Persistent state

Examples:

```text
authenticated user
found relics
sync status
```

## Temporary device state

Examples:

```text
current lux
motion score
proximity
```

Raw sensor state does not need to become durable application data.

---

# 67. LIFECYCLE RULES

The Android architecture materials emphasize lifecycle-aware design.

Therefore:

- ViewModel should hold appropriate UI state;
- sensor listeners should not remain active unnecessarily;
- fragments/screens should not hold stale view references;
- persistent data should not live only in Activity state;
- Room stores durable local state.

---

# 68. UI/UX PRINCIPLES

The UI should make the scan mechanic understandable.

## Scan instructions

Explain:

```text
Move slowly side to side
Stay near the relic area
Watch the scan meter
Get close enough for final confirmation
```

Do not expose technical terms such as:

```text
normalized sensor vector
```

to the user.

## Feedback

The meter should visibly respond to:

- better proximity;
- better light match;
- better scanning motion.

---

# 69. ACCESSIBILITY AND USABILITY

At minimum:

- readable text;
- sufficient contrast;
- clear buttons;
- meaningful error messages;
- no reliance solely on color;
- touch targets large enough;
- progress understandable without technical knowledge.

---

# 70. MATERIAL UI

The module includes Material Design concepts.

Use consistent:

- typography;
- spacing;
- buttons;
- cards;
- navigation;
- colors;
- states.

Avoid creating a different visual language for every screen.

M1 owns the shared theme.

M2 follows the shared theme for quest/scan screens.

---

# 71. LAYOUT STRATEGY

For XML-based screens, use appropriate Android layouts.

ConstraintLayout can be used where flexible positioning is needed.

FrameLayout is useful for layered content.

The team should avoid deeply nested layouts when a simpler structure works.

The project may use the UI approach already selected in the Android project; the master plan does not require switching technologies merely for consistency.

---

# 72. PERFORMANCE PRINCIPLES

Do not optimize prematurely.

Priorities:

```text
correctness
stability
battery
readability
```

Avoid:

- unnecessary sensor listeners;
- unnecessary location updates;
- repeated Firebase writes;
- repeated Room queries;
- large UI redraws.

---

# 73. DATA SECURITY

Never commit:

- passwords;
- private keys;
- service-account credentials;
- unnecessary tokens.

Do not log:

- passwords;
- authentication secrets;
- unnecessary personal data.

---

# 74. DEMO FLOW

The final demonstration should tell one complete story.

## Step 1

Launch application.

## Step 2

Login.

## Step 3

Show map.

## Step 4

Select R001.

## Step 5

Move into target area.

## Step 6

Show Scan Mode.

## Step 7

Demonstrate slow scan motion.

## Step 8

Show fusion meter increasing.

## Step 9

Show proximity still blocks reveal if not close enough.

## Step 10

Confirm proximity.

## Step 11

Reveal Founder’s Bell.

## Step 12

Show lore.

## Step 13

Show progress.

## Step 14

Show leaderboard.

## Step 15

Optionally demonstrate offline/local persistence and later synchronization.

---

# 75. DEMO EXPLANATION

Recommended technical explanation:

> “We use MVVM with a repository layer to separate the UI from data sources. Location and geofencing are handled separately from the sensor-fusion component. Once the user enters the relic geofence, Scan Mode combines GPS proximity, ambient-light signature matching, and accelerometer-based scanning motion into a normalized weighted score. Missing fusion sensors are handled by redistributing the active weights. The proximity sensor is deliberately separate and acts only as the final close-range reveal gate. Successful discoveries are stored locally with Room and synchronized to Firestore.”

---

# 76. LECTURER-QUESTION PREPARATION

## Why MVVM?

Because it separates UI from UI state/business coordination, improves testability, and handles lifecycle changes more cleanly.

## Why Repository?

It hides whether data comes from Room, Firestore, or another source.

## Why Room?

Immediate local persistence and offline capability.

## Why Firebase?

Authentication and shared cloud data/leaderboard.

## Why geofencing?

Battery-conscious trigger for entering the target area.

## Why sensor fusion?

It creates the core differentiated discovery mechanic rather than relying on one signal.

## Why proximity separately?

It is a final close-range confirmation rather than a component of the fusion percentage.

## What if a sensor is missing?

The application detects availability and reweights available fusion signals where possible.

## What if internet disappears?

Room retains the successful discovery and marks it pending until synchronization succeeds.

---

# 77. DOCUMENTATION RESPONSIBILITY

| Documentation | Owner |
|---|---|
| Master plan | Team / coordinator |
| UI/navigation | M1 |
| Quest/scan behavior | M2 |
| Location/geofence | M3 |
| Sensor fusion/calibration | M4 |
| Firebase/security | M5 |
| Room/sync/integration | M6 |
| Final integration evidence | M6 + all members |
| Testing evidence | Each owner + M6 |
| Final demo flow | Team |

---

# 78. REQUIREMENT → EVIDENCE MAPPING

| Requirement | Evidence |
|---|---|
| Authentication | login demo |
| MVVM | architecture/code |
| Repository | repository interface/code |
| Map | screen/demo |
| Location | live location |
| Geofence | enter event |
| Sensor fusion | live score |
| Light | live lux/match behavior |
| Accelerometer | sweep behavior |
| Proximity | final gate |
| Room | persisted progress |
| Firebase | cloud progress |
| Leaderboard | leaderboard screen |
| Offline | offline test |
| Degradation | missing-sensor test |
| Security | denied access test |
| Testing | test results |
| Multi-device | device matrix |

---

# 79. FINAL SUBMISSION CHECKLIST

## Application

- [ ] app builds;
- [ ] app installs;
- [ ] login works;
- [ ] navigation works;
- [ ] map works;
- [ ] relics display;
- [ ] geofence works;
- [ ] scan works;
- [ ] fusion works;
- [ ] proximity gate works;
- [ ] reveal works;
- [ ] lore works;
- [ ] progress works;
- [ ] leaderboard works.

## Data

- [ ] Room works;
- [ ] Firebase works;
- [ ] sync works;
- [ ] duplicate discovery prevented;
- [ ] offline discovery preserved.

## Testing

- [ ] unit tests;
- [ ] integration tests;
- [ ] physical-device tests;
- [ ] two-device tests;
- [ ] security tests;
- [ ] offline tests;
- [ ] lifecycle tests.

## Documentation

- [ ] architecture;
- [ ] team responsibilities;
- [ ] data models;
- [ ] Firebase schema;
- [ ] Room schema;
- [ ] sensor fusion;
- [ ] location;
- [ ] testing;
- [ ] Git workflow;
- [ ] demo.

---

# 80. FINAL PROJECT HEALTH CHECK

Before final freeze, ask:

### Can we build?

```text
Yes / No
```

### Can a new user login?

```text
Yes / No
```

### Can the user reach R001?

```text
Yes / No
```

### Does geofence trigger?

```text
Yes / No
```

### Does Scan Mode work?

```text
Yes / No
```

### Does the fusion score respond?

```text
Yes / No
```

### Does proximity gate correctly?

```text
Yes / No
```

### Does reveal work?

```text
Yes / No
```

### Does Room save?

```text
Yes / No
```

### Does Firebase sync?

```text
Yes / No
```

### Does leaderboard update?

```text
Yes / No
```

### Does it survive restart?

```text
Yes / No
```

### Does it behave acceptably offline?

```text
Yes / No
```

---

# 81. FINAL ARCHITECTURE DIAGRAM

```text
                         CAMPUS QUEST
                              │
                              ▼
                    ┌──────────────────┐
                    │      UI / M1     │
                    │ Navigation/Theme │
                    └────────┬─────────┘
                             ▼
                    ┌──────────────────┐
                    │      M2 UI       │
                    │ Quest / Scan     │
                    │ Reveal / Progress│
                    └────────┬─────────┘
                             ▼
                    ┌──────────────────┐
                    │    ViewModels    │
                    └────────┬─────────┘
                             ▼
                    ┌──────────────────┐
                    │    Repository    │
                    └───────┬───┬──────┘
                            │   │
                    ┌───────┘   └────────┐
                    ▼                    ▼
             ┌─────────────┐      ┌─────────────┐
             │   M6 Room   │      │ M5 Firebase │
             │ local/cache │      │ cloud/auth  │
             └─────────────┘      └─────────────┘

             ┌─────────────────────────────┐
             │        DEVICE SERVICES      │
             │                             │
             │ M3: GPS / Maps / Geofence   │
             │ M4: Light / Motion / Prox   │
             └──────────────┬──────────────┘
                            │
                            ▼
                    ┌──────────────────┐
                    │ SensorFusionEngine│
                    │ GPS + Light +     │
                    │ Motion            │
                    └────────┬─────────┘
                             ▼
                       FusionResult
                             │
                             ▼
                    Proximity Final Gate
                             │
                             ▼
                           Reveal
```

---

# 82. FINAL TEAM RESPONSIBILITY MAP

```text
M1
APP SHELL
│
├── Navigation
├── Theme
├── Profile
└── Login UI

M2
GAME EXPERIENCE
│
├── Quest
├── Scan UI
├── Fusion Meter
├── Reveal
├── Lore
└── Progress/Leaderboard UI

M3
LOCATION
│
├── Maps
├── GPS
├── Distance
└── Geofencing

M4
SENSORS
│
├── Light
├── Accelerometer
├── Proximity
├── Fusion
└── Calibration

M5
CLOUD
│
├── Auth
├── Firestore
├── Progress
├── Leaderboard
└── Security

M6
DATA + INTEGRATION
│
├── Room
├── Offline
├── pendingSync
├── Build stability
└── Integration coordination
```

---

# 83. NON-NEGOTIABLE PROJECT RULES

1. **Proximity is not part of the fusion score.**
2. **M3 owns location.**
3. **M4 must not duplicate GPS.**
4. **M5 owns Firebase.**
5. **M6 owns Room.**
6. **UI must not directly access Firebase or Room.**
7. **Use repository boundaries.**
8. **Use mocks when dependencies are incomplete.**
9. **Integrate before the final week.**
10. **Do not treat provisional sensor weights as final.**
11. **Validate relic coordinates physically.**
12. **Calibrate light/motion parameters physically.**
13. **Missing sensors must be handled gracefully where possible.**
14. **A successful discovery must not be lost because of temporary network failure.**
15. **Duplicate discoveries must not inflate progress.**
16. **Security rules must be tested before final submission.**
17. **Every member integrates their own feature.**
18. **M6 coordinates integration; M6 is not responsible for fixing every member's feature.**
19. **Keep `main` buildable.**
20. **If behind, prioritize a reliable R001 end-to-end vertical slice.**

---

# 84. FINAL SUCCESS CRITERIA

The project is successful when the team can demonstrate this sequence reliably:

```text
USER
 ↓
Login
 ↓
Map
 ↓
Relic
 ↓
Geofence ENTER
 ↓
Scan Mode
 ↓
GPS proximity
+
Light signature
+
Accelerometer sweep
 ↓
Fusion percentage
 ↓
Threshold
 ↓
Proximity confirmation
 ↓
Relic reveal
 ↓
Lore
 ↓
Room persistence
 ↓
Firebase synchronization
 ↓
Leaderboard
```

The implementation should be understandable enough that each member can explain their own responsibility and the interfaces between their module and the other five modules.

---

# 85. FINAL “IF EVERYTHING WORKS” CHECK

```text
[✓] Native Android selected
[✓] MVVM architecture
[✓] Repository separation
[✓] Six members have clear ownership
[✓] Parallel development
[✓] Mock-first strategy
[✓] Map and geofencing
[✓] Sensor fusion
[✓] Proximity final gate
[✓] Graceful degradation
[✓] Room persistence
[✓] Firebase Auth
[✓] Firestore
[✓] Leaderboard
[✓] Offline sync
[✓] Testing strategy
[✓] Campus calibration
[✓] Two-device testing
[✓] Git/PR workflow
[✓] Daily schedule
[✓] R001 vertical slice
[✓] Final demo path
```

---

# 86. FINAL MASTER STATEMENT

Campus Quest should be developed as one application through six parallel ownership areas, not as six independent mini-projects.

The central engineering strategy is:

```text
clear ownership
+
shared contracts
+
mock-first development
+
incremental integration
+
continuous testing
+
physical calibration
```

The central gameplay strategy is:

```text
LOCATION
   +
LIGHT
   +
MOTION
   ↓
FUSION
   ↓
PROXIMITY
   ↓
REVEAL
```

The central data strategy is:

```text
ROOM
   ↓
immediate local persistence
   ↓
pendingSync
   ↓
FIRESTORE
   ↓
shared progress / leaderboard
```

The central architectural strategy is:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firestore
```

The central project-management strategy is:

```text
build core first
integrate early
test continuously
calibrate physical behavior
freeze contracts
cut stretch features when necessary
```

If the team becomes constrained by time, the application should be reduced around one reliable end-to-end relic journey rather than allowing six partially completed subsystems to survive until the final day.

**Primary final target:**

```text
Login
→ Map
→ R001
→ Geofence
→ Sensor Fusion
→ Proximity
→ Reveal
→ Room
→ Firebase
→ Leaderboard
```

Once this path is reliable, expand to R002–R006 and then spend remaining time on robustness, testing, visual refinement, and optional features.
