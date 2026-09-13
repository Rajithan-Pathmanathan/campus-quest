# Campus Quest — Master Development & Implementation Plan
## Updated 6-Member Team Allocation

**Project:** Campus Quest  
**Module:** INTE 22283 – Mobile Application Development  
**Platform:** Native Android / Kotlin / Android Studio  
**Architecture:** MVVM + Repository + Room + Firebase/Firestore  
**Status:** Updated master implementation plan

---

## 1. Final Six-Member Allocation

| Member | Area | Approx. workload |
|---|---|---:|
| M1 | Player UI + Navigation | 17% |
| M2 | Creator + Game Management | 16% |
| M3 | Location + Sensors + Fusion | 22% |
| M4 | Quest + Scan Gameplay | 14% |
| M5 | Firebase + Backend | 18% |
| M6 | Room + Repository + Sync + Integration | 13% |
| **Total** | | **100%** |

These percentages represent approximate implementation workload/complexity, not lines of code.

### M1 — Player UI & Navigation
Owns the player-facing application experience:
- Login/navigation integration
- Available games list
- Game cards and details
- Join-game flow
- Player navigation
- Game navigation
- Leaderboard UI
- Notification deep-link navigation
- Shared UI components and common states

**Game responsibility:** controls what the player sees and where the player navigates.

### M2 — Creator & Game Management
Owns the creator side:
- Create Game
- Game title/description
- Create/edit/delete checkpoints
- Checkpoint ordering
- Coordinates and geofence radius
- Light-signature configuration
- Clue/lore/rarity/motion settings
- Draft management
- Validation
- Publish flow

**Game responsibility:** controls how creators build and publish games.

### M3 — Location + Sensors + Fusion
Owns the physical discovery system:
- Fused Location Provider
- Location permissions/settings
- Google Maps
- Distance calculation
- Dynamic checkpoint markers
- Dynamic geofences
- Geofence ENTER detection
- Ambient light sensor
- Accelerometer
- Proximity sensor
- Motion/sweep detection
- Light matching
- Normalization
- Weighted fusion
- Fusion threshold
- Proximity final gate
- Sensor lifecycle and degradation

**Game responsibility:** determines whether the player's physical conditions satisfy checkpoint discovery.

Important:
`GPS + Light + Motion → weighted fusion → threshold → Proximity final gate → discovery`

Proximity is **not** a fourth weighted fusion input.

### M4 — Quest & Scan Gameplay
Owns the player's checkpoint discovery experience:
- Checkpoint gameplay screen
- Scan HUD
- Fusion percentage/meter
- Sensor status presentation
- Scan states
- Success/failure states
- Reveal dialog
- Clue/lore presentation
- Discovery presentation
- Scan ViewModel/state handling

**Game responsibility:** controls what the player sees and does during checkpoint discovery.

### M5 — Firebase & Backend
Owns the online/shared backend:
- Firebase Authentication
- Firestore
- Users
- Games
- Checkpoints
- Game memberships
- Progress
- Game-specific leaderboards
- Security rules
- FCM
- New-game notifications
- Cloud repository implementation
- Cloud validation/error handling

**Game responsibility:** stores and distributes shared online game data.

### M6 — Room + Repository + Sync + Integration
Owns local persistence and the application data boundary:
- Room database
- Entities
- DAOs
- Local cache
- Local progress
- Pending synchronization
- Repository implementation/boundary
- Room ↔ Firebase synchronization
- Retries/idempotency
- Transactions
- Migrations
- Cross-user/game isolation
- Integration builds and cross-feature testing

**Game responsibility:** keeps data available locally/offline and connects the application to its data sources.

---

## 2. Architecture

The project uses separation of concerns and MVVM:

```text
UI Layer
(M1 / M2 / M4)
       ↓
ViewModel Layer
(M1 / M2 / M4)
       ↓
Repository Boundary
(M6)
       ↓
 ┌───────────────┐
 ↓               ↓
Room            Firebase
 M6               M5
```

The location/sensor subsystem is separate:

```text
M3
Location + Sensors + Fusion
          ↓
   Scan/Fusion State
          ↓
         M4
     Scan UI
```

The course architecture material emphasizes separation of concerns: UI displays data and forwards actions, ViewModels coordinate UI state/logic, and the repository/data layer hides whether data comes from local storage, network or cache. fileciteturn28file0L178-L191

Native Android/Kotlin remains the implementation platform; the module materials identify native development as providing direct access to platform APIs and hardware features. fileciteturn28file1L308-L316

---

## 3. Canonical Domain Model

```kotlin
enum class GameStatus {
    DRAFT, PUBLISHED, CLOSED
}

data class Game(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val status: GameStatus = GameStatus.DRAFT,
    val checkpointCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null
)

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

`R001–R006` are allowed only as sample/seed IDs for a demonstration game. They are not permanent production checkpoints.

---

## 4. Canonical Firestore Schema

```text
users/{uid}

games/{gameId}
games/{gameId}/checkpoints/{checkpointId}

gamePlayers/{gameId}_{uid}

progress/{uid}/games/{gameId}/checkpoints/{checkpointId}

leaderboards/{gameId}/entries/{uid}

/topics/new_games
```

### users
`displayName`, `email`, `createdAt`

### games
`id`, `title`, `description`, `creatorId`, `creatorName`, `status`, `checkpointCount`, `createdAt`, `publishedAt`

### checkpoints
`id`, `gameId`, `name`, `lat`, `lng`, `radiusM`, `lightSignature`, `clue`, `lore`, `order`, `motionType`, `rarity`

### gamePlayers
`gameId`, `userId`, `joinedAt`

### progress
`checkpointId`, `gameId`, `userId`, `foundAt`

### leaderboard
`gameId`, `userId`, `displayName`, `checkpointsDiscovered`, `lastUpdate`

There is **no global leaderboard**. Every game has its own leaderboard.

---

## 5. Room Structure

Recommended entities:

```text
GameEntity
CheckpointEntity
GamePlayerEntity
FoundCheckpointEntity
PendingSyncEntity (optional)
```

Composite keys:

```text
CheckpointEntity       → gameId + checkpointId
GamePlayerEntity       → gameId + userId
FoundCheckpointEntity  → gameId + userId + checkpointId
```

This prevents cross-game and cross-user data mixing.

---

## 6. Repository Contract

```kotlin
interface GameRepository {
    suspend fun getAvailableGames(): List<Game>
    suspend fun getGameDetails(gameId: String): Game?
    suspend fun createGame(game: Game): Result<Game>
    suspend fun createCheckpoint(
        gameId: String,
        checkpoint: Checkpoint
    ): Result<Checkpoint>
    suspend fun updateCheckpoint(
        gameId: String,
        checkpoint: Checkpoint
    ): Result<Unit>
    suspend fun publishGame(gameId: String): Result<Unit>
    suspend fun joinGame(gameId: String): Result<Unit>
    suspend fun getGameCheckpoints(gameId: String): List<Checkpoint>
    suspend fun getFusionSignature(
        gameId: String,
        checkpointId: String
    ): LightSignature
    suspend fun recordDiscovery(
        gameId: String,
        checkpointId: String,
        foundAt: Long
    ): Result<Unit>
    fun observeGameLeaderboard(
        gameId: String
    ): Flow<List<GameLeaderboardEntry>>
    suspend fun syncPending()
}
```

---

## 7. FCM New-Game Notification

```kotlin
data class NewGameNotification(
    val gameId: String,
    val gameTitle: String,
    val creatorName: String,
    val checkpointCount: Int,
    val publishedAt: Long
)
```

Flow:

```text
Creator publishes
      ↓
Firestore status = PUBLISHED
      ↓
FCM /topics/new_games
      ↓
Player receives notification
      ↓
Tap notification
      ↓
Game Details(gameId)
```

M5 owns FCM/backend behavior. M1 owns notification navigation.

---

## 8. Player Flow

```text
Login
  ↓
Games
  ↓
Game Details
  ↓
Join
  ↓
Game Map
  ↓
Approach Checkpoint
  ↓
Geofence ENTER
  ↓
Scan Mode
  ↓
GPS + Light + Motion
  ↓
Fusion Threshold
  ↓
Proximity Final Gate
  ↓
Reveal
  ↓
Room Save
  ↓
Firebase Sync
  ↓
Game Leaderboard
```

---

## 9. Creator Flow

```text
Creator
  ↓
Create Game
  ↓
Game Information
  ↓
Add Checkpoints
  ↓
Configure Checkpoint
  ↓
Save Draft
  ↓
Edit / Reorder
  ↓
Review
  ↓
Publish
```

Creators can define games with different numbers of checkpoints. The production system must not depend on six fixed checkpoints.

---

## 10. Discovery Mechanism

### Location
M3 obtains current location and calculates distance to the current game's checkpoints.

### Geofence
M3 registers game-scoped checkpoint geofences and detects ENTER events.

### Scan
M4 presents scan mode.

### Fusion
M3 evaluates:
- GPS/distance signal;
- ambient light match;
- accelerometer motion/sweep.

### Threshold
The normalized weighted fusion score must reach the configured threshold.

### Final gate
The proximity condition confirms close physical presence.

### Discovery
Only then is the checkpoint recorded as discovered.

---

## 11. Ambient Light Rule

`lightSignature` represents the expected **ambient environmental light range**.

It is not light emitted by a relic.

Example:

```text
minLux = 100
maxLux = 250
```

M3 compares current ambient lux with the configured checkpoint range.

---

## 12. Offline-First Behavior

```text
User action
    ↓
Room local save
    ↓
pendingSync = true
    ↓
Connectivity restored
    ↓
Sync
    ↓
Firebase
    ↓
pendingSync = false
```

Firebase is the authoritative shared source.

Room provides immediate local persistence and offline capability.

---

## 13. Synchronization Ownership

M6 owns synchronization.

It must handle:
- retries;
- duplicate writes;
- failed requests;
- app restarts;
- connectivity changes;
- duplicate discoveries;
- game/user scoping;
- authoritative cloud state.

M5 owns the Firebase implementation; M6 owns the local/cloud synchronization boundary.

---

## 14. Parallel Development

All six members can work in parallel after shared contracts are agreed.

```text
M1 → mock player data
M2 → mock repository
M3 → mock checkpoints/location
M4 → mock fusion states
M5 → Firebase implementation
M6 → Room/repository implementation
             ↓
      Contract integration
             ↓
        E2E testing
```

M3 and M4 do not need to block each other:
- M3 can test the physical detection engine with mock data.
- M4 can build the scan UI using mock fusion states.

M1/M2 can build screens with mock repositories.

M5/M6 can implement their data layers against the same domain models.

---

## 15. Integration Boundaries

### M1 ↔ M6/M5
Player UI → ViewModel → Repository → local/cloud data.

### M2 ↔ M6/M5
Creator UI → ViewModel → Repository → local/cloud data.

### M3 ↔ M4
M3 produces location/sensor/fusion state; M4 displays it.

### M5 ↔ M6
M5 provides Firebase; M6 provides Room and synchronization.

---

# 16. Implementation Phases

## Phase 0 — Project Setup
All members:
- Git repository;
- Android Studio/SDK;
- Gradle;
- package structure;
- dependencies;
- Firebase project;
- Room setup;
- shared models;
- coding conventions.

**Done:** clean project builds.

## Phase 1 — Foundation
M1: app shell/navigation.  
M2: creator skeleton.  
M3: location/sensor interfaces.  
M4: scan UI/state skeleton.  
M5: Firebase/Auth foundation.  
M6: Room/repository foundation.

## Phase 2 — Data Layer
M5: Firestore schema and cloud operations.  
M6: Room entities, DAOs, repository and pending sync.

**Done:** common domain models work across local and cloud layers.

## Phase 3 — Authentication
M5: Firebase Auth.  
M1: login UI and auth-state navigation.

## Phase 4 — Creator
M2: game/checkpoint creation, editing, draft, validation, publish.  
M5: cloud persistence.  
M6: local/repository support.

**Done:** Create → Save → Reopen → Edit → Publish.

## Phase 5 — Player Game Discovery
M1: games list/details/join.  
M5: published games.  
M6: local cache/repository.

**Done:** player can discover and join a published game.

## Phase 6 — Maps & Geofencing
M3:
- map;
- permissions;
- current location;
- markers;
- distance;
- dynamic geofences;
- ENTER detection.

M1/M4 consume the resulting state.

## Phase 7 — Sensor Fusion
M3:
- light;
- accelerometer;
- motion;
- normalization;
- weighted fusion;
- threshold;
- proximity final gate;
- lifecycle/degradation.

M4 displays the resulting state.

## Phase 8 — Discovery Persistence
M4 triggers the discovery result through the shared contract.  
M6 stores locally.  
M5 synchronizes to Firebase.

## Phase 9 — Game Leaderboard
M5: leaderboard backend.  
M6: optional local cache.  
M1: leaderboard UI.

## Phase 10 — FCM
M5: topic subscription/notification/payload.  
M1: notification deep link.

## Phase 11 — Full Integration
Test:

```text
Creator
 ↓
Create Game
 ↓
Create Checkpoints
 ↓
Publish
 ↓
Firebase
 ↓
FCM
 ↓
Player
 ↓
Game Details
 ↓
Join
 ↓
Map
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
Room
 ↓
Firebase Sync
 ↓
Leaderboard
```

## Phase 12 — Final Testing
Run module-level, integration and end-to-end testing.

---

# 17. Testing Ownership

| Member | Primary tests |
|---|---|
| M1 | Navigation, screen states, player flows |
| M2 | Creator validation, draft, checkpoint forms, publishing |
| M3 | Location, distance, geofence, light, motion, fusion, proximity, lifecycle |
| M4 | Scan state machine, loading, success, failure, reveal |
| M5 | Auth, Firestore, security rules, FCM |
| M6 | Room, DAO, repository, offline, sync, retries, duplicate prevention |

Highest-risk areas:
1. sensor fusion;
2. geofencing;
3. Room/Firebase synchronization;
4. game/user isolation;
5. creator-to-player publishing;
6. notification deep linking;
7. end-to-end discovery.

---

# 18. Required Failure Cases

At minimum test:
- location permission denied;
- location disabled;
- poor GPS accuracy;
- checkpoint outside range;
- geofence not triggered;
- unavailable light sensor;
- unavailable accelerometer;
- unavailable proximity sensor;
- invalid light range;
- insufficient fusion score;
- failed proximity gate;
- network unavailable;
- Firebase failure;
- Room failure;
- duplicate discovery;
- app backgrounded during scan;
- screen recreation;
- restart before sync;
- invalid notification game ID;
- unauthorized game access;
- invalid creator publish;
- cross-game progress leakage.

---

# 19. Dynamic Game Rules

Production data must be creator-defined:

```text
Game
 ├── Checkpoint A
 ├── Checkpoint B
 ├── Checkpoint C
 └── ... any reasonable number
```

`R001–R006` are only seed/demo records.

Use at least two games during testing to prove:
- checkpoint isolation;
- progress isolation;
- leaderboard isolation;
- correct map markers;
- correct game details.

---

# 20. Legacy Model Rule

Do not build new production features around the old fixed relic model.

Avoid making these the primary architecture:

```text
relics/{relicId}
progress/{uid}/found/{relicId}
FoundRelicEntity
global leaderboard
```

Canonical vocabulary:

```text
Game
Checkpoint
GamePlayer
Progress
GameLeaderboardEntry
```

---

# 21. Suggested Package Structure

```text
com.campusquest
├── auth
├── game
│   ├── player
│   ├── creator
│   ├── details
│   └── leaderboard
├── checkpoint
│   ├── ui
│   ├── scan
│   └── reveal
├── location
│   ├── map
│   ├── geofence
│   └── location
├── sensor
│   ├── light
│   ├── motion
│   ├── proximity
│   └── fusion
├── data
│   ├── local
│   │   ├── room
│   │   └── dao
│   ├── remote
│   │   └── firebase
│   └── repository
├── notification
└── core
    ├── model
    ├── navigation
    ├── result
    └── util
```

---

# 22. Git Structure

Recommended branches:

```text
main
develop

feature/m1-player-ui-navigation
feature/m2-creator-game-management
feature/m3-location-sensor-fusion
feature/m4-quest-scan
feature/m5-firebase-backend
feature/m6-room-repository-sync
```

Example commits:

```text
feat(m1): add games list navigation
feat(m2): add checkpoint editor
feat(m3): add dynamic checkpoint geofences
feat(m4): add scan fusion meter UI
feat(m5): add game Firestore repository
feat(m6): add Room checkpoint DAO
```

Shared-contract changes require team communication before merging.

---

# 23. Definition of Done

A feature is complete when:
- implementation exists;
- correct architectural layer is used;
- required states are handled;
- error cases are considered;
- tests are appropriate;
- dynamic Game/Checkpoint rules are respected;
- shared interfaces match;
- application builds;
- integration dependencies work;
- code is committed and reviewed.

### Member-specific

**M1:** player can navigate using real repository data.

**M2:** creator can create/configure, save and publish a game.

**M3:** physical location/sensor/fusion system produces the agreed discovery result.

**M4:** player can complete the checkpoint scanning experience.

**M5:** cloud authentication, data, leaderboard and notifications work securely.

**M6:** local storage, repository and synchronization work reliably.

---

# 24. Final End-to-End Acceptance Scenario

### Creator
1. Sign in.
2. Create game.
3. Add multiple checkpoints.
4. Configure checkpoints.
5. Save draft.
6. Reopen/edit.
7. Publish.

### System
8. Firestore changes game to `PUBLISHED`.
9. New-game FCM notification is sent.

### Player
10. Receives notification.
11. Opens Game Details.
12. Joins.
13. Opens map.
14. Approaches checkpoint.
15. Enters geofence.
16. Scan mode opens.
17. GPS/light/motion are evaluated.
18. Fusion threshold is reached.
19. Proximity final gate succeeds.
20. Checkpoint is revealed.
21. Discovery is saved locally.
22. Pending data syncs to Firebase.
23. Game-specific leaderboard updates.

---

# 25. Final Ownership Matrix

| Feature | M1 | M2 | M3 | M4 | M5 | M6 |
|---|---|---|---|---|---|---|
| Player UI | **Lead** | | | Support | | |
| Navigation | **Lead** | Support | | Support | | |
| Creator UI | | **Lead** | | | | |
| Game creation | | **Lead** | | | Support | Support |
| Checkpoint configuration | | **Lead** | Support | | | |
| Publishing UI | | **Lead** | | | | |
| Maps | | | **Lead** | | | |
| Location | | | **Lead** | | | |
| Geofencing | | | **Lead** | | | |
| Light sensor | | | **Lead** | | | |
| Accelerometer | | | **Lead** | | | |
| Proximity | | | **Lead** | | | |
| Sensor fusion | | | **Lead** | | | |
| Scan UI | | | Support | **Lead** | | |
| Reveal UI | | | | **Lead** | | |
| Firebase Auth | | | | | **Lead** | |
| Firestore | | | | | **Lead** | Support |
| FCM | Support | | | | **Lead** | |
| Room | | | | | | **Lead** |
| Repository | | | | | Support | **Lead** |
| Offline storage | | | | | | **Lead** |
| Sync | | | | | Support | **Lead** |
| Leaderboard backend | | | | | **Lead** | Support |
| Leaderboard UI | **Lead** | | | Support | | |
| Integration | Support | Support | Support | Support | Support | **Lead** |

---

# 26. Documents That Must Follow This Allocation

Directly update:
1. `Member1_UI_UX_NAVIGATION_WORKPLAN.md`
2. `Member2_QUEST_SCAN_UI_WORKPLAN.md`
3. `Member3_LOCATION_GEOFENCING_WORKPLAN.md`
4. `Member4_SENSOR_FUSION_WORKPLAN.md`
5. `Member5_FIREBASE_CLOUD_SYNC_WORKPLAN.md`
6. `Member6_ROOM_DATA_INTEGRATION_WORKPLAN.md`
7. `SHARED_CONTRACTS_AND_INTEGRATION_INTERFACES.md`
8. `INTEGRATION_HANDOFF_AND_MILESTONE_PLAN.md`
9. `GIT_CHANGE_CONTROL_AND_TEAM_DEVELOPMENT_WORKFLOW.md`

Review ownership references in:
- `TRD_CAMPUS_QUEST.md`
- `TESTING_AND_ACCEPTANCE_STRATEGY.md`

Product-level documents should remain unchanged unless requirements themselves change:
- `PRD_CAMPUS_QUEST.md`
- `APP_FLOW_DOCUMENT_CAMPUS_QUEST.md`
- `UI_UX_DESIGN_SPECIFICATION_CAMPUS_QUEST.md`
- `SENSOR_FUSION_AND_LOCATION_TECHNICAL_SPECIFICATION.md`

---

# 27. Final Team Structure

```text
M1 — Player Experience
M2 — Game Creation
M3 — Physical Detection
M4 — Gameplay/Scan Experience
M5 — Cloud Backend
M6 — Local Data + Integration
              ↓
        CAMPUS QUEST
```

The team allocation changes ownership while preserving the existing product architecture. The key structural changes are:

- Creator responsibilities move from M1 to M2.
- Location and Sensor Fusion are combined under M3.
- M4 becomes the Quest/Scan Gameplay owner.
- M6 explicitly owns Repository + Sync + Integration.
- M5 remains the Firebase/Backend owner.

This is the finalized six-member allocation for the Campus Quest implementation.
