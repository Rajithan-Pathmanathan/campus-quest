# Campus Quest --- Integration & Handoff Plan

**Document ID:** CQ-INTEGRATION-03\
**Document:** `INTEGRATION_AND_HANDOFF_PLAN.md`\
**Project:** Campus Quest\
**Purpose:** Define how the six-member team integrates independently
developed features into one working Android application without breaking
shared contracts.\
**Source of Truth:** `00_MASTER_DEVELOPMENT_PLAN.md`\
**Supporting Contracts:** `FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md`,
`SHARED_CONTRACTS_AND_AGREEMENTS.md`,
`SENSOR_FUSION_AND_LOCATION_SPECIFICATION.md`, `MOCK_DATA_CATALOG.md`\
**Status:** Team working specification\
**Development Window:** 13 September 2026 -- 28 September 2026

------------------------------------------------------------------------

# 1. Purpose

This document defines the integration strategy for Campus Quest.

The project is divided among six members:

``` text
M1 — UI/UX & Navigation
M2 — Quest/Scan UI
M3 — Location & Geofencing
M4 — Sensor Fusion
M5 — Firebase Cloud & Sync
M6 — Room Data & Integration
```

The purpose of integration is not simply to merge six branches.

The purpose is to produce one coherent application in which:

``` text
Game Creator
    ↓
Create Game
    ↓
Configure Checkpoints
    ↓
Publish
    ↓
New Game Notification
    ↓
Game Player
    ↓
Browse
    ↓
Join
    ↓
Play
    ↓
Discover Checkpoints
    ↓
Room
    ↓
Firestore
    ↓
Game-Specific Leaderboard
```

works as one vertical slice.

------------------------------------------------------------------------

# 2. Integration Principle

Every member owns an implementation area, but no member owns an isolated
application.

The shared architecture is:

``` text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firebase / device services
```

Integration must preserve this boundary.

Do not solve integration problems by allowing:

``` text
UI → Firebase
UI → Room DAO
M2 → M3 private implementation
M3 → Firebase
M4 → Firestore
```

Instead use shared application-level contracts.

------------------------------------------------------------------------

# 3. Primary Integration Goal

The primary integration goal is:

``` text
One creator-created game
        ↓
published
        ↓
visible to another authenticated player
        ↓
player joins
        ↓
player loads dynamic checkpoints
        ↓
player reaches checkpoint
        ↓
geofence activates scan
        ↓
GPS + light + motion fusion
        ↓
proximity final gate
        ↓
checkpoint revealed
        ↓
Room persistence
        ↓
Firestore synchronization
        ↓
game-specific leaderboard
```

This is the main vertical slice.

------------------------------------------------------------------------

# 4. Product Model

Campus Quest is not a single fixed quest containing a permanent list of
six relics.

The production model is:

``` text
User
 ├── can create games
 └── can play games
```

A game contains:

``` text
Game
 └── Checkpoints
```

A player participates through:

``` text
GamePlayer
```

A discovery is identified by:

``` text
User + Game + Checkpoint
```

A leaderboard is identified by:

``` text
Game + User
```

------------------------------------------------------------------------

# 5. Canonical Integration Objects

The primary shared objects are:

``` text
User
Game
Checkpoint
GamePlayer
CheckpointDiscovery
GameLeaderboardEntry
LightSignature
DistanceResult
FusionResult
ScanState
SensorAvailability
```

The production integration layer must not use a global `Relic` object as
the primary shared domain model.

------------------------------------------------------------------------

# 6. Legacy Terminology

Older prototype material may contain:

``` text
Relic
FoundRelic
Quest
R001
R002
...
R006
```

These are not the production architecture.

The six existing records may remain as sample checkpoint seed data
inside a sample game.

The canonical term is:

``` text
Checkpoint
```

------------------------------------------------------------------------

# 7. Team Ownership

## M1 --- UI/UX & Navigation Lead

Owns:

``` text
App shell
Navigation
Game browsing
Game details
Join flow
Creator wizard
Checkpoint editor
Publish UI
Notification deep-link entry
Leaderboard presentation
```

## M2 --- Quest/Scan UI Lead

Owns:

``` text
Checkpoint detail UI
Scan HUD
Fusion meter
Scan progress
Reveal dialog
Scan states
Gameplay presentation
```

## M3 --- Location & Geofencing Lead

Owns:

``` text
Location permission handling
Fused Location Provider
Google Maps
Distance calculations
Dynamic checkpoint geofences
Geofence events
Game-specific map markers
```

## M4 --- Sensor Fusion Lead

Owns:

``` text
Accelerometer
Light sensor
Proximity sensor
Light matching
Motion detection
Fusion engine
Sensor availability
Degraded-mode behavior
Final proximity gate
```

## M5 --- Firebase Cloud & Sync Lead

Owns:

``` text
Firebase Authentication
Firestore
FCM
Cloud schema
Security rules
Cloud repository implementation
Cloud progress
Leaderboard persistence
Notification trigger
```

## M6 --- Room Data & Integration Lead

Owns:

``` text
Room
Entities
DAOs
Offline cache
Local discovery
pendingSync
Room/Firebase synchronization
Build stability
Integration coordination
```

------------------------------------------------------------------------

# 8. Shared Responsibility Rule

Ownership does not mean isolation.

For example:

``` text
M5 owns Firestore
M6 owns Room
```

but both must agree on:

``` text
gameId
checkpointId
userId
foundAt
sync state
duplicate handling
```

Similarly:

``` text
M3 owns location
M4 owns fusion
```

but both must agree on:

``` text
DistanceResult
```

------------------------------------------------------------------------

# 9. Source of Truth Hierarchy

When integrating conflicting information, use:

``` text
1. Master Development Plan
2. Shared Contracts
3. Firebase Schema
4. Sensor/Location Specification
5. Member Workplans
6. Mock Data
7. Individual implementation choices
```

If code contradicts the agreed contract, the code must be corrected
unless the team explicitly changes the contract.

------------------------------------------------------------------------

# 10. Architecture Boundary

The application must maintain:

``` text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Data sources
```

Data sources include:

``` text
Room
Firebase
Location APIs
SensorManager
```

Device APIs may be wrapped in appropriate application services.

------------------------------------------------------------------------

# 11. Repository Boundary

The repository is the main cross-feature data boundary.

M1 and M2 should consume application models.

M3 receives checkpoint configuration.

M4 receives sensor configuration.

M5 implements cloud behavior.

M6 implements local persistence.

The UI must not know:

``` text
Firestore collection paths
Room table names
Firebase SDK implementation details
```

------------------------------------------------------------------------

# 12. Canonical Repository Contract

The shared game repository should support:

``` kotlin
interface GameRepository {

    suspend fun getAvailableGames(): List<Game>

    suspend fun getGameDetails(
        gameId: String
    ): Game?

    suspend fun createGame(
        game: Game
    ): Result<Game>

    suspend fun createCheckpoint(
        gameId: String,
        checkpoint: Checkpoint
    ): Result<Checkpoint>

    suspend fun updateCheckpoint(
        gameId: String,
        checkpoint: Checkpoint
    ): Result<Unit>

    suspend fun publishGame(
        gameId: String
    ): Result<Unit>

    suspend fun joinGame(
        gameId: String
    ): Result<Unit>

    suspend fun getGameCheckpoints(
        gameId: String
    ): List<Checkpoint>

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

The exact implementation may use separate repositories, but
responsibilities must remain equivalent.

------------------------------------------------------------------------

# 13. Why `gameId` Is Mandatory

Checkpoint IDs are scoped to games.

Therefore:

``` kotlin
getFusionSignature(checkpointId)
```

is insufficient.

Correct:

``` kotlin
getFusionSignature(
    gameId,
    checkpointId
)
```

Likewise:

``` kotlin
recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
```

must preserve the game context.

------------------------------------------------------------------------

# 14. Shared Data Model

## Game

``` kotlin
data class Game(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val status: GameStatus,
    val checkpointCount: Int,
    val createdAt: Long,
    val publishedAt: Long?,
    val updatedAt: Long
)
```

## Checkpoint

``` kotlin
data class Checkpoint(
    val id: String,
    val gameId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val lightSignature: LightSignature,
    val clue: String,
    val lore: String,
    val order: Int,
    val motionType: String,
    val rarity: String
)
```

------------------------------------------------------------------------

# 15. Shared Sensor Contracts

M3 produces:

``` kotlin
data class DistanceResult(
    val distanceM: Float,
    val accuracyM: Float
)
```

M4 consumes this.

M4 produces:

``` text
FusionResult
ScanState
SensorAvailability
```

M2 consumes these.

------------------------------------------------------------------------

# 16. Integration Boundary Map

``` text
M1 ↔ M5
Authentication

M1 ↔ M5
Games / creator / player data

M2 ↔ M5/M6
Checkpoint data / discovery / leaderboard

M3 ↔ M5/M6
Checkpoint location data

M3 ↔ M4
DistanceResult

M4 ↔ M2
FusionResult / ScanState

M4 ↔ M5/M6
LightSignature

M2 ↔ M6
Discovery persistence

M5 ↔ M6
Cloud/local synchronization
```

------------------------------------------------------------------------

# 17. Boundary 1 --- M1 ↔ M5

## Purpose

Authentication and cloud-backed game data.

Authentication flow:

``` text
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

M1 must not call Firebase Authentication directly from the screen.

------------------------------------------------------------------------

# 18. M1 ↔ M5 Game List

``` text
Games Screen
 ↓
GamesViewModel
 ↓
GameRepository
 ↓
Firestore / Room
 ↓
List<Game>
 ↓
Games Screen
```

The game list must represent published games.

------------------------------------------------------------------------

# 19. M1 ↔ M5 Creator Flow

``` text
Creator UI
 ↓
Creator ViewModel
 ↓
GameRepository.createGame()
 ↓
Firestore
 ↓
Game
```

M1 owns presentation.

M5 owns cloud persistence.

------------------------------------------------------------------------

# 20. M1 ↔ M5 Publish Flow

``` text
Publish Screen
 ↓
ViewModel
 ↓
GameRepository.publishGame(gameId)
 ↓
Firestore
 ↓
PUBLISHED
```

Notification generation is handled by the Firebase side.

------------------------------------------------------------------------

# 21. M1 ↔ M5 Notification Flow

``` text
FCM notification
 ↓
gameId
 ↓
notification/deep-link handler
 ↓
Game Details
 ↓
GameRepository.getGameDetails(gameId)
```

The notification must not contain a hard-coded checkpoint destination.

------------------------------------------------------------------------

# 22. Boundary 2 --- M2 ↔ M5/M6

## Purpose

Gameplay checkpoint data, discovery persistence and leaderboard data.

Correct flow:

``` text
Repository
 ↓
Checkpoint
 ↓
Gameplay UI
```

Not:

``` text
Repository
 ↓
global Relic
 ↓
Quest Screen
```

------------------------------------------------------------------------

# 23. M2 Checkpoint Selection

M2 receives a selected:

``` text
gameId
checkpointId
```

The ViewModel should maintain that context during the scan.

------------------------------------------------------------------------

# 24. M2 Discovery Flow

After successful reveal:

``` text
Scan UI
 ↓
Gameplay ViewModel
 ↓
recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
 ↓
Repository
 ↓
Room
 ↓
Firestore
```

M2 does not insert directly into a Room DAO.

------------------------------------------------------------------------

# 25. M2 Leaderboard Flow

``` text
GameLeaderboardScreen
 ↓
ViewModel
 ↓
observeGameLeaderboard(gameId)
 ↓
Repository
 ↓
Firestore / Room
 ↓
GameLeaderboardEntry
 ↓
UI
```

The leaderboard must remain scoped to the selected game.

------------------------------------------------------------------------

# 26. Boundary 3 --- M3 ↔ M5/M6

M3 needs:

``` text
gameId
checkpointId
lat
lng
radiusM
```

Flow:

``` text
Repository
 ↓
Checkpoint
 ↓
Map / Geofence system
```

M3 does not need to know whether the checkpoint came from:

``` text
Mock
Room
Firestore
```

------------------------------------------------------------------------

# 27. Dynamic Map Requirement

The map must be populated from the selected game's checkpoints.

Correct:

``` text
selected game
 ↓
checkpoints
 ↓
markers
```

Incorrect:

``` text
map
 ↓
hard-coded R001–R006
```

------------------------------------------------------------------------

# 28. Dynamic Geofence Requirement

Geofences must be created from checkpoint configuration.

``` text
Checkpoint
 ├── lat
 ├── lng
 └── radiusM
        ↓
Geofence
```

The geofence must not depend on a fixed production location list.

------------------------------------------------------------------------

# 29. Boundary 4 --- M3 ↔ M4

This is a critical technical boundary.

M3 provides:

``` text
DistanceResult
```

M4 consumes it.

Example:

``` text
distanceM = 18
accuracyM = 6
```

Flow:

``` text
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

------------------------------------------------------------------------

# 30. Distance Contract

The distance result should represent:

``` text
distance from current location
to selected checkpoint
```

It should not represent:

``` text
distance to the nearest arbitrary checkpoint
```

unless the gameplay design explicitly requests nearest-checkpoint
behavior.

------------------------------------------------------------------------

# 31. Boundary 5 --- M4 ↔ M2

M4 provides:

``` text
FusionResult
ScanState
SensorAvailability
```

M2 displays them.

Example:

``` text
score = 87
state = READY_FOR_PROXIMITY
```

M2 must not recalculate the fusion score.

------------------------------------------------------------------------

# 32. Fusion Result Contract

Conceptually:

``` kotlin
data class FusionResult(
    val score: Int,
    val gpsScore: Float,
    val lightScore: Float,
    val motionScore: Float,
    val state: ScanState
)
```

The exact fields may be adjusted as long as the contract remains stable.

------------------------------------------------------------------------

# 33. Boundary 6 --- M4 ↔ M5/M6

M4 needs:

``` text
LightSignature
```

Flow:

``` text
Repository
 ↓
Checkpoint
 ↓
LightSignature
 ↓
M4
 ↓
Light matching
```

The signature is configured per checkpoint.

------------------------------------------------------------------------

# 34. Light Signature

The signature is:

``` text
minLux
maxLux
```

It represents expected ambient light.

It does not represent light emitted by the checkpoint.

------------------------------------------------------------------------

# 35. Boundary 7 --- M2 ↔ M6

Successful discovery:

``` text
M2/ViewModel
 ↓
recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
 ↓
Repository
 ↓
Room
```

M2 does not access:

``` text
FoundCheckpointDao
```

directly.

------------------------------------------------------------------------

# 36. Boundary 8 --- M5 ↔ M6

Cloud/local synchronization:

``` text
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

``` text
gameId
checkpointId
userId
foundAt
success/failure
retry
duplicate handling
```

------------------------------------------------------------------------

# 37. Integration Object: Checkpoint Discovery

The canonical identity is:

``` text
userId + gameId + checkpointId
```

The local and cloud systems must preserve all three.

------------------------------------------------------------------------

# 38. Integration Object: Game Player

The canonical identity is:

``` text
gameId + userId
```

A user can join multiple games.

Therefore:

``` text
User A + Game A
```

is different from:

``` text
User A + Game B
```

------------------------------------------------------------------------

# 39. Integration Object: Leaderboard Entry

The canonical identity is:

``` text
gameId + userId
```

A player may have multiple leaderboard entries across games.

They must never be merged into one global score.

------------------------------------------------------------------------

# 40. Creator-to-Player Integration Flow

``` text
Creator Login
 ↓
Create Game
 ↓
Add Checkpoints
 ↓
Save Draft
 ↓
Publish
 ↓
Notification
 ↓
Player Login
 ↓
Browse Games
 ↓
Open Game Details
 ↓
Join Game
 ↓
Game Map
```

This flow must be integrated before advanced polish.

------------------------------------------------------------------------

# 41. Gameplay Integration Flow

``` text
Game Map
 ↓
Selected Checkpoint
 ↓
Approach
 ↓
Geofence ENTER
 ↓
Scan Mode
 ↓
GPS distance
 ↓
Light matching
 ↓
Motion gesture
 ↓
Fusion score
 ↓
Fusion threshold
 ↓
Proximity final gate
 ↓
Reveal
```

------------------------------------------------------------------------

# 42. Persistence Integration Flow

``` text
Reveal
 ↓
recordDiscovery()
 ↓
Room
 ↓
pendingSync
 ↓
Firestore
 ↓
leaderboard
```

This is the persistence vertical slice.

------------------------------------------------------------------------

# 43. Complete Integration Flow

``` text
Creator
 ↓
Game
 ↓
Checkpoints
 ↓
Publish
 ↓
FCM
 ↓
Player
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
Firestore
 ↓
Leaderboard
```

------------------------------------------------------------------------

# 44. Integration Phase Strategy

The integration schedule has five phases:

``` text
Phase 1 — Foundation
13–14 Sep

Phase 2 — Feature Development
15–19 Sep

Phase 3 — Incremental Integration
20–23 Sep

Phase 4 — Testing & Stabilization
24–27 Sep

Phase 5 — Finalization
28 Sep
```

The dates are planning targets.

The team should integrate continuously rather than waiting for the final
phase.

------------------------------------------------------------------------

# 45. Phase 1 --- Foundation

Every member establishes:

``` text
branch
package structure
shared contracts
mock data
basic compile
```

M6 checks:

``` text
project builds
```

------------------------------------------------------------------------

# 46. 13 September Integration Check

At the end of the foundation setup:

``` text
Git clone works
Android Studio opens
Gradle builds
Application launches
Shared models compile
```

Do not wait until feature integration to discover a build configuration
problem.

------------------------------------------------------------------------

# 47. Phase 1 Shared Contract Freeze

Before feature work expands:

``` text
Game
Checkpoint
GamePlayer
CheckpointDiscovery
GameLeaderboardEntry
LightSignature
DistanceResult
FusionResult
ScanState
```

should have agreed definitions.

------------------------------------------------------------------------

# 48. Phase 2 --- Feature Development

During feature development:

``` text
M1 → UI/navigation
M2 → scan/gameplay UI
M3 → location/geofencing
M4 → sensor fusion
M5 → Firebase/FCM
M6 → Room/integration
```

Each member should test their feature against mock or local contracts
before requesting full integration.

------------------------------------------------------------------------

# 49. Feature Development Rule

A feature is not considered ready merely because:

``` text
screen exists
```

or:

``` text
class compiles
```

It should have:

``` text
implementation
+
basic test
+
error handling
+
shared contract compatibility
```

------------------------------------------------------------------------

# 50. Phase 3 --- Incremental Integration

Integrate one boundary at a time.

Recommended order:

``` text
1. Authentication
2. Game creation
3. Game browsing
4. Checkpoint loading
5. Game joining
6. Map/location
7. Geofence
8. Sensor fusion
9. Scan UI
10. Discovery → Room
11. Room → Firestore
12. Leaderboard
13. FCM
14. Full creator/player flow
```

------------------------------------------------------------------------

# 51. Authentication Integration

First integration:

``` text
M1 + M5
```

Test:

``` text
Login
 ↓
AuthRepository
 ↓
Firebase Auth
 ↓
Authenticated state
 ↓
Main UI
```

Acceptance:

``` text
login works
logout works
auth state survives restart appropriately
protected screens require authentication
```

------------------------------------------------------------------------

# 52. Game Creation Integration

Next:

``` text
M1 + M5
```

Flow:

``` text
Creator UI
 ↓
Create Game
 ↓
GameRepository
 ↓
Firestore
 ↓
Game DRAFT
```

Acceptance:

``` text
game receives unique ID
creatorId is authenticated UID
status = DRAFT
game can be reopened
```

------------------------------------------------------------------------

# 53. Checkpoint Creation Integration

Next:

``` text
M1 + M5
```

Flow:

``` text
Checkpoint Editor
 ↓
Checkpoint
 ↓
GameRepository
 ↓
games/{gameId}/checkpoints/{checkpointId}
```

Acceptance:

``` text
checkpoint belongs to selected game
location persists
radius persists
light signature persists
order persists
```

------------------------------------------------------------------------

# 54. Game Publishing Integration

Next:

``` text
M1 + M5
```

Flow:

``` text
Draft
 ↓
Validate
 ↓
Publish
 ↓
Firestore PUBLISHED
```

Acceptance:

``` text
draft becomes published
invalid game cannot publish
creator ownership is preserved
```

------------------------------------------------------------------------

# 55. Published Game Browsing Integration

``` text
M1 + M5
```

Flow:

``` text
Games Screen
 ↓
getAvailableGames()
 ↓
Firestore
 ↓
PUBLISHED games
```

Acceptance:

``` text
published game appears
draft does not appear
game details open correctly
```

------------------------------------------------------------------------

# 56. Game Joining Integration

``` text
M1 + M5
```

Flow:

``` text
Game Details
 ↓
Join
 ↓
gamePlayers/{gameId}_{uid}
 ↓
Joined state
```

Acceptance:

``` text
join succeeds
repeat join is safe
membership belongs to current user
```

------------------------------------------------------------------------

# 57. Checkpoint Loading Integration

``` text
M1/M2 + M5/M6
```

Flow:

``` text
selected gameId
 ↓
getGameCheckpoints(gameId)
 ↓
Checkpoint list
```

Acceptance:

``` text
only selected game's checkpoints load
```

------------------------------------------------------------------------

# 58. Map Integration

``` text
M3 + M5/M6
```

Flow:

``` text
Game
 ↓
Checkpoint list
 ↓
M3
 ↓
Map markers
```

Acceptance:

``` text
markers correspond to selected game
coordinates are correct
no hard-coded R001–R006 dependency
```

------------------------------------------------------------------------

# 59. Geofence Integration

``` text
M3
```

Flow:

``` text
Checkpoint
 ↓
lat/lng/radius
 ↓
Geofence
 ↓
ENTER event
 ↓
Scan enabled
```

Acceptance:

``` text
correct checkpoint triggers scan
unrelated checkpoint does not trigger selected scan
```

------------------------------------------------------------------------

# 60. Distance Integration

``` text
M3 + M4
```

Flow:

``` text
Fused Location Provider
 ↓
distance calculation
 ↓
DistanceResult
 ↓
FusionEngine
```

Acceptance:

``` text
distance changes as user moves
accuracy is available
selected checkpoint is used
```

------------------------------------------------------------------------

# 61. Sensor Fusion Integration

``` text
M4
```

Inputs:

``` text
DistanceResult
LightSignature
Accelerometer
```

Output:

``` text
FusionResult
```

Acceptance:

``` text
score changes with valid inputs
sensor availability is reported
threshold behavior is deterministic
```

------------------------------------------------------------------------

# 62. Proximity Integration

Proximity is a final gate.

Correct:

``` text
Fusion score reaches threshold
 ↓
Proximity check
 ↓
Reveal
```

Do not add proximity as a fourth weighted fusion component unless the
product contract is explicitly changed.

------------------------------------------------------------------------

# 63. Scan UI Integration

``` text
M4 + M2
```

Flow:

``` text
FusionResult
 ↓
ScanState
 ↓
M2
 ↓
HUD / meter
```

Acceptance:

``` text
meter displays actual fusion state
UI does not recalculate score
sensor errors are represented correctly
```

------------------------------------------------------------------------

# 64. Reveal Integration

``` text
M2 + M6
```

Flow:

``` text
Scan success
 ↓
Reveal UI
 ↓
recordDiscovery()
 ↓
Room
```

Acceptance:

``` text
discovery is stored
gameId retained
checkpointId retained
userId retained
```

------------------------------------------------------------------------

# 65. Room Integration

``` text
M6
```

Required local concepts:

``` text
GameEntity
CheckpointEntity
GamePlayerEntity
FoundCheckpointEntity
```

Discovery identity:

``` text
gameId + userId + checkpointId
```

------------------------------------------------------------------------

# 66. Room/Firebase Sync Integration

``` text
M6 + M5
```

Flow:

``` text
Room
 ↓
pendingSync
 ↓
repository
 ↓
Firestore
 ↓
success
 ↓
pendingSync = false
```

Acceptance:

``` text
offline discovery survives
network recovery syncs
duplicate sync does not create duplicate progress
```

------------------------------------------------------------------------

# 67. Leaderboard Integration

``` text
M2 + M5/M6
```

Flow:

``` text
Discovery
 ↓
game-scoped progress
 ↓
leaderboard entry
 ↓
GameLeaderboardScreen
```

Acceptance:

``` text
selected game only
correct discovered count
correct total
correct ordering
```

------------------------------------------------------------------------

# 68. FCM Integration

``` text
M1 + M5
```

Flow:

``` text
Creator publishes
 ↓
trusted notification trigger
 ↓
/topics/new_games
 ↓
player device
 ↓
notification tap
 ↓
Game Details(gameId)
```

Acceptance:

``` text
published game notification can be received
notification opens correct game
draft does not trigger notification
```

------------------------------------------------------------------------

# 69. Integration Sequence

The preferred complete sequence is:

``` text
M1 + M5
Authentication
        ↓
M1 + M5
Game creation/publishing
        ↓
M1 + M5
Player browse/join
        ↓
M3 + M5/M6
Checkpoint/map
        ↓
M3
Geofence
        ↓
M3 + M4
Distance → Fusion
        ↓
M4 + M2
Fusion → Scan UI
        ↓
M2 + M6
Reveal → Room
        ↓
M5 + M6
Room → Firestore
        ↓
M2 + M5
Leaderboard
        ↓
M1 + M5
FCM notification
```

------------------------------------------------------------------------

# 70. Why Incremental Integration Is Required

If everything is integrated simultaneously:

``` text
GPS
Firebase
Room
Sensors
UI
Leaderboard
FCM
```

then a failure becomes difficult to locate.

Instead:

``` text
Game → Checkpoint
```

then:

``` text
Checkpoint → Map
```

then:

``` text
Distance → Fusion
```

then:

``` text
Fusion → UI
```

then:

``` text
Reveal → Room
```

then:

``` text
Room → Firebase
```

then:

``` text
Firebase → Leaderboard
```

------------------------------------------------------------------------

# 71. Integration Test Order

Always test from the outside toward the core:

``` text
1. App launches
2. Authentication
3. Navigation
4. Game list
5. Game details
6. Join
7. Game checkpoints
8. Map
9. Location
10. Geofence
11. Scan
12. Fusion
13. Proximity
14. Reveal
15. Room
16. Firestore
17. Leaderboard
18. Notification
```

------------------------------------------------------------------------

# 72. Failure Diagnosis --- App Does Not Launch

Check:

``` text
Gradle
 ↓
dependencies
 ↓
manifest
 ↓
resource compilation
 ↓
Kotlin compilation
```

M6 coordinates the initial diagnosis.

------------------------------------------------------------------------

# 73. Failure Diagnosis --- Login

Check:

``` text
Firebase configuration
 ↓
authentication provider
 ↓
AuthRepository
 ↓
AuthState
 ↓
ViewModel
 ↓
UI
```

------------------------------------------------------------------------

# 74. Failure Diagnosis --- Game Not Appearing

Check:

``` text
Firestore game exists
 ↓
status == PUBLISHED
 ↓
query filter
 ↓
repository mapping
 ↓
ViewModel state
 ↓
UI
```

------------------------------------------------------------------------

# 75. Failure Diagnosis --- Checkpoints Missing

Check:

``` text
correct gameId
 ↓
Firestore path
 ↓
checkpoint documents
 ↓
repository query
 ↓
Room cache
 ↓
ViewModel
```

------------------------------------------------------------------------

# 76. Failure Diagnosis --- Map Marker Missing

Check:

``` text
checkpoint data
 ↓
lat/lng
 ↓
permission
 ↓
Google Maps
 ↓
marker creation
```

------------------------------------------------------------------------

# 77. Failure Diagnosis --- Geofence Not Triggering

Check:

``` text
location permission
 ↓
background/required permission behavior
 ↓
geofence registration
 ↓
correct coordinates
 ↓
correct radius
 ↓
selected game/checkpoint
 ↓
geofence event
```

------------------------------------------------------------------------

# 78. Failure Diagnosis --- Fusion Not Changing

Check:

``` text
sensor availability
 ↓
M3 DistanceResult
 ↓
light sensor
 ↓
LightSignature
 ↓
accelerometer
 ↓
FusionEngine
```

------------------------------------------------------------------------

# 79. Failure Diagnosis --- Reveal Does Not Happen

Check:

``` text
fusion threshold
 ↓
proximity gate
 ↓
ScanState
 ↓
reveal condition
```

------------------------------------------------------------------------

# 80. Failure Diagnosis --- Discovery Disappears

Check:

``` text
Room
 ↓
FoundCheckpointEntity
 ↓
pendingSync
 ↓
repository
 ↓
Firestore
```

------------------------------------------------------------------------

# 81. Failure Diagnosis --- Leaderboard Wrong

Check:

``` text
selected gameId
 ↓
progress path
 ↓
discovery count
 ↓
leaderboard update
 ↓
leaderboard query
 ↓
UI
```

Never begin by changing the UI ranking code without checking game scope.

------------------------------------------------------------------------

# 82. Failure Diagnosis --- Notification Opens Wrong Game

Check:

``` text
FCM payload
 ↓
gameId
 ↓
intent/deep link
 ↓
navigation argument
 ↓
Game Details ViewModel
```

------------------------------------------------------------------------

# 83. Cross-Game Isolation Test

Create:

``` text
Game A
2 checkpoints
```

and:

``` text
Game B
3 checkpoints
```

Player discovers:

``` text
Game A → 1
Game B → 2
```

Expected:

``` text
Game A = 1/2
Game B = 2/3
```

There must be no:

``` text
global = 3/5
```

leaderboard representation.

------------------------------------------------------------------------

# 84. Creator Isolation Test

Create:

``` text
Creator A → Game A
Creator B → Game B
```

Attempt:

``` text
Creator A edits Game B
```

Expected:

``` text
denied
```

------------------------------------------------------------------------

# 85. Player Isolation Test

Player A attempts to write:

``` text
Player B's progress
```

Expected:

``` text
denied
```

------------------------------------------------------------------------

# 86. Draft Visibility Test

Creator creates:

``` text
Game C
status = DRAFT
```

Player checks games.

Expected:

``` text
Game C not visible
```

Creator checks own games.

Expected:

``` text
Game C visible
```

------------------------------------------------------------------------

# 87. Dynamic Checkpoint Test

Create a game with:

``` text
1 checkpoint
```

Play it.

Then create another game with:

``` text
4 checkpoints
```

Play it.

The application must work without code changes.

------------------------------------------------------------------------

# 88. Variable Checkpoint Count Test

Test:

``` text
1 checkpoint
2 checkpoints
3 checkpoints
6 checkpoints
```

The application must use:

``` text
totalCheckpoints
```

rather than a hard-coded number.

------------------------------------------------------------------------

# 89. Seed Data Test

The sample six records may be used:

``` text
R001
R002
R003
R004
R005
R006
```

but the gameplay code must still work if these records are replaced
with:

``` text
checkpoint_A
checkpoint_B
```

------------------------------------------------------------------------

# 90. Offline Integration Test

Procedure:

``` text
Join game online
 ↓
Cache game/checkpoints
 ↓
Disable network
 ↓
Discover checkpoint
 ↓
Room stores discovery
 ↓
pendingSync = true
 ↓
Restore network
 ↓
Firestore sync
```

Expected:

``` text
discovery remains
cloud record appears
leaderboard updates
pendingSync clears
```

------------------------------------------------------------------------

# 91. Duplicate Discovery Test

Perform:

``` text
discover checkpoint
sync
sync again
```

Expected:

``` text
one logical discovery
```

not:

``` text
two discoveries
```

------------------------------------------------------------------------

# 92. Restart Integration Test

Procedure:

``` text
Discover checkpoint
 ↓
close app
 ↓
reopen
```

Expected:

``` text
local discovery remains
```

After network:

``` text
cloud state remains consistent
```

------------------------------------------------------------------------

# 93. Permission Integration Test

Test:

``` text
location permission denied
```

Expected:

``` text
clear UI explanation
no crash
```

Test:

``` text
sensor unavailable
```

Expected:

``` text
SensorAvailability reported
graceful behavior
```

------------------------------------------------------------------------

# 94. Device Integration

At minimum test on:

``` text
one development emulator
one physical Android device
```

Physical testing is required for:

``` text
GPS
geofence
light sensor
accelerometer
proximity sensor
```

because emulator behavior may not accurately represent physical sensors.

------------------------------------------------------------------------

# 95. Integration Environment

All members should use:

``` text
same repository
same branch strategy
same package/module conventions
same shared model names
same Firebase environment
```

unless a documented reason requires otherwise.

------------------------------------------------------------------------

# 96. Branch Strategy

Recommended:

``` text
main
│
├── feature/ui-navigation
├── feature/quest-scan-ui
├── feature/location-geofence
├── feature/sensor-fusion
├── feature/firebase-cloud
└── feature/room-data
```

Each member works primarily on their assigned branch.

------------------------------------------------------------------------

# 97. Branch Naming

Use stable names:

``` text
feature/ui-navigation
feature/quest-scan-ui
feature/location-geofence
feature/sensor-fusion
feature/firebase-cloud
feature/room-data
```

Bug fixes may use:

``` text
fix/<short-description>
```

------------------------------------------------------------------------

# 98. Pull Request Flow

Use:

``` text
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
Build/test
 ↓
Merge
```

Do not use:

``` text
direct push to main
```

for normal feature development.

------------------------------------------------------------------------

# 99. Commit Rule

Commits should be:

``` text
small
focused
buildable where practical
descriptive
```

Avoid one enormous commit containing:

``` text
UI
Firebase
Room
GPS
sensor
```

all at once.

------------------------------------------------------------------------

# 100. Before Opening a PR

The developer must:

``` text
1. Update from latest agreed main
2. Resolve conflicts locally
3. Build project
4. Run relevant tests
5. Test feature
6. Check no credentials are committed
7. Check no unintended hard-coded data
8. Check shared contracts
9. Open PR
```

------------------------------------------------------------------------

# 101. Reviewer Checklist

Reviewer checks:

``` text
[ ] Project builds
[ ] Shared contract respected
[ ] Correct IDs
[ ] gameId preserved
[ ] No direct Firebase from UI
[ ] No direct Room DAO from UI
[ ] No unrelated changes
[ ] Lifecycle handled
[ ] Permissions handled
[ ] Errors handled
[ ] Mock data consistent
[ ] Tests run
[ ] No hard-coded R001 dependency
```

------------------------------------------------------------------------

# 102. Shared Contract Changes

Do not silently change:

``` text
Game
Checkpoint
Repository signatures
Firestore paths
Room identity
FusionResult
DistanceResult
```

If a breaking change is necessary:

``` text
announce
 ↓
discuss
 ↓
update shared contract
 ↓
update dependent members
 ↓
integrate
```

------------------------------------------------------------------------

# 103. Breaking Change Example

Changing:

``` kotlin
recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
```

to:

``` kotlin
recordDiscovery(
    checkpointId
)
```

is a breaking change.

It removes game scope.

Do not make this change casually.

------------------------------------------------------------------------

# 104. Non-Breaking Change Example

Adding an optional field:

``` text
Game.updatedAt
```

may be non-breaking if all consumers remain compatible.

------------------------------------------------------------------------

# 105. Integration Freeze

Before final stabilization, freeze:

``` text
Firestore paths
Room keys
Repository signatures
Game lifecycle
Checkpoint model
Leaderboard scope
Notification routing
Sensor fusion contract
```

After freeze, only bug fixes should normally be merged.

------------------------------------------------------------------------

# 106. Daily Integration Check

At the end of each development day:

``` text
[ ] main builds
[ ] application launches
[ ] no critical merge conflicts
[ ] shared models compile
[ ] Firebase config works
[ ] Room schema works
[ ] current vertical slice still works
```

------------------------------------------------------------------------

# 107. Integration Log

Maintain a simple record:

``` text
Date
Integrated features
Known issues
Blocked members
Next integration target
```

This prevents the team from repeatedly rediscovering the same problems.

------------------------------------------------------------------------

# 108. Handoff Principle

A handoff must transfer:

``` text
what the feature does
what inputs it expects
what outputs it provides
what dependencies it has
how to test it
known limitations
```

Do not hand off only source files.

------------------------------------------------------------------------

# 109. M1 Handoff to M2

M1 provides:

``` text
navigation route
gameId
checkpointId
selected Game
selected Checkpoint
```

M2 should not have to reconstruct navigation context.

------------------------------------------------------------------------

# 110. M1 Handoff to M3

M1/M2 provide:

``` text
selected game
selected checkpoint
```

M3 uses that context to obtain location configuration.

------------------------------------------------------------------------

# 111. M3 Handoff to M4

M3 provides:

``` text
DistanceResult
```

M4 must not duplicate the location-distance calculation.

------------------------------------------------------------------------

# 112. M4 Handoff to M2

M4 provides:

``` text
FusionResult
ScanState
SensorAvailability
```

M2 renders the state.

------------------------------------------------------------------------

# 113. M2 Handoff to M6

M2 provides:

``` text
gameId
checkpointId
foundAt
```

M6 persists it.

------------------------------------------------------------------------

# 114. M5 Handoff to M6

M5 provides:

``` text
Firestore schema
cloud models
repository cloud behavior
sync expectations
security rules
```

M6 provides:

``` text
Room models
DAOs
pendingSync
local state
```

------------------------------------------------------------------------

# 115. M6 Handoff to M5

M6 must define:

``` text
what counts as pending
when sync is attempted
how success is acknowledged
how duplicate sync is handled
```

------------------------------------------------------------------------

# 116. M5/M6 Synchronization Contract

Canonical discovery:

``` text
userId
gameId
checkpointId
foundAt
```

Local:

``` text
pendingSync
```

Cloud:

``` text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

------------------------------------------------------------------------

# 117. No Duplicate Local/Cloud Models

Avoid having:

``` text
Relic
FoundRelic
Checkpoint
FoundCheckpoint
```

all treated as independent production models.

Canonical:

``` text
Checkpoint
CheckpointDiscovery
```

------------------------------------------------------------------------

# 118. No Global Progress Object

Avoid:

``` text
FoundRelics
```

as the application-level progress model.

Progress must be represented with:

``` text
gameId
```

------------------------------------------------------------------------

# 119. No Global Leaderboard Object

Avoid:

``` text
GlobalLeaderboard
```

for gameplay ranking.

Use:

``` text
GameLeaderboard
```

scoped by:

``` text
gameId
```

------------------------------------------------------------------------

# 120. No Global Checkpoint Map

The map should represent:

``` text
selected game checkpoints
```

not every checkpoint in Firebase.

------------------------------------------------------------------------

# 121. Creator-Player Role Integration

The same authenticated account can be:

``` text
Creator for Game A
Player in Game B
```

Do not create separate Firebase users for these roles unless the product
is explicitly changed.

------------------------------------------------------------------------

# 122. Creator Draft Integration

A creator's draft should be accessible through:

``` text
creatorId
```

and:

``` text
status = DRAFT
```

The creator should see their own drafts.

Other players should not.

------------------------------------------------------------------------

# 123. Published Game Integration

Once published:

``` text
status = PUBLISHED
```

the game becomes player-visible.

The player receives:

``` text
Game
Checkpoint list
```

through the repository.

------------------------------------------------------------------------

# 124. Published Configuration Stability

Avoid changing checkpoint definitions while players are actively using a
published game.

This reduces:

``` text
cache mismatch
geofence mismatch
leaderboard mismatch
```

------------------------------------------------------------------------

# 125. Game Closure Integration

If a game is finished:

``` text
status = CLOSED
```

rather than destructively deleting all related data.

Historical progress can remain available.

------------------------------------------------------------------------

# 126. Notification Handoff

M5 provides:

``` text
notification payload
```

M1 provides:

``` text
deep-link destination
```

Shared requirement:

``` text
gameId
```

------------------------------------------------------------------------

# 127. Notification Test

Test:

``` text
publish Game A
```

Notification:

``` text
gameId = Game A
```

Tap:

``` text
Game A Details
```

Then:

``` text
publish Game B
```

Tap:

``` text
Game B Details
```

The navigation must never reuse stale Game A context.

------------------------------------------------------------------------

# 128. Game Context Propagation

During gameplay, preserve:

``` text
gameId
checkpointId
```

through:

``` text
navigation
 ↓
ViewModel
 ↓
location
 ↓
fusion
 ↓
reveal
 ↓
persistence
```

Losing `gameId` at any boundary can cause cross-game corruption.

------------------------------------------------------------------------

# 129. Game Context Debugging

When diagnosing a gameplay bug, log or inspect:

``` text
current gameId
current checkpointId
current userId
```

Do not log sensitive authentication credentials.

------------------------------------------------------------------------

# 130. Location Debugging

M3 should be able to show:

``` text
selected gameId
selected checkpointId
target coordinates
current coordinates
distance
accuracy
geofence status
```

during development.

Debug UI/logging should be disabled or minimized for final release.

------------------------------------------------------------------------

# 131. Sensor Debugging

M4 should be able to inspect:

``` text
light reading
expected minLux
expected maxLux
motion state
GPS score
light score
motion score
fusion score
proximity state
```

during development.

------------------------------------------------------------------------

# 132. Room Debugging

M6 should be able to inspect:

``` text
gameId
userId
checkpointId
foundAt
pendingSync
```

for local records.

------------------------------------------------------------------------

# 133. Firebase Debugging

M5 should be able to inspect:

``` text
gameId
creatorId
status
checkpoint count
membership
progress
leaderboard
notification payload
```

without exposing credentials.

------------------------------------------------------------------------

# 134. End-to-End Debug Record

For one test discovery:

``` text
gameId = game_demo_001
checkpointId = checkpoint_001
userId = test-user
```

Trace:

``` text
Map
 ↓
Geofence
 ↓
Fusion
 ↓
Reveal
 ↓
Room
 ↓
Firestore
 ↓
Leaderboard
```

All stages must retain the same identifiers.

------------------------------------------------------------------------

# 135. Integration Test Data

Minimum test dataset:

``` text
Game A
 ├── Checkpoint A1
 └── Checkpoint A2

Game B
 ├── Checkpoint B1
 ├── Checkpoint B2
 └── Checkpoint B3
```

Players:

``` text
Player A
Player B
```

Creator:

``` text
Creator A
```

Additional creators/players may be added.

------------------------------------------------------------------------

# 136. Multi-Game Test

``` text
Creator A
 ↓
Game A
```

and:

``` text
Creator A
 ↓
Game B
```

Player:

``` text
joins both
```

This proves that one user can create and play multiple games.

------------------------------------------------------------------------

# 137. Multi-Creator Test

``` text
Creator A → Game A
Creator B → Game B
```

Player sees:

``` text
Game A
Game B
```

but creators retain independent ownership.

------------------------------------------------------------------------

# 138. Multi-Player Leaderboard Test

``` text
Game A
 ├── Player A → 2/3
 ├── Player B → 1/3
 └── Player C → 0/3
```

Expected ranking:

``` text
1. Player A
2. Player B
3. Player C
```

------------------------------------------------------------------------

# 139. Cross-Game Leaderboard Test

``` text
Game A:
Player A → 2/3

Game B:
Player A → 1/2
```

Expected:

``` text
Game A leaderboard
Player A → 2/3

Game B leaderboard
Player A → 1/2
```

No merged score.

------------------------------------------------------------------------

# 140. Game-Specific Map Test

Game A:

``` text
A1
A2
```

Game B:

``` text
B1
B2
B3
```

When Game A is selected:

``` text
only A1/A2
```

should be represented as its gameplay checkpoints.

------------------------------------------------------------------------

# 141. Dynamic Geofence Test

Select:

``` text
Game A / A1
```

Geofence:

``` text
A1 coordinates + A1 radius
```

Select:

``` text
Game B / B1
```

Geofence:

``` text
B1 coordinates + B1 radius
```

The geofence must update with selected context.

------------------------------------------------------------------------

# 142. Fusion Test

For selected checkpoint:

``` text
GPS score
+
light score
+
motion score
```

produces:

``` text
fusion score
```

Then:

``` text
proximity
```

gates final reveal.

------------------------------------------------------------------------

# 143. Sensor Degradation Test

If the light sensor is unavailable:

``` text
SensorAvailability
 ↓
FusionEngine
```

should use the documented degraded behavior.

M2 should display the resulting state rather than inventing a value.

------------------------------------------------------------------------

# 144. Location Accuracy Test

If GPS accuracy is poor:

``` text
DistanceResult.accuracyM
```

must be available to the fusion system.

M4 decides how the configured fusion logic handles it.

------------------------------------------------------------------------

# 145. Permission Recovery Test

Procedure:

``` text
deny location
 ↓
show permission explanation
 ↓
grant permission
 ↓
retry
```

Expected:

``` text
location becomes available
```

without requiring an unnecessary application reinstall.

------------------------------------------------------------------------

# 146. Lifecycle Test

Test:

``` text
open map
background app
return
```

and:

``` text
open scan
background app
return
```

Ensure sensors/location resources are handled safely.

------------------------------------------------------------------------

# 147. Rotation/Configuration Test

Where applicable:

``` text
screen recreation
```

must not lose:

``` text
gameId
checkpointId
scan state
```

if the active gameplay design expects continuity.

------------------------------------------------------------------------

# 148. App Restart Test

After restart:

``` text
authenticated state
cached game data
local discoveries
```

should behave according to the agreed persistence model.

------------------------------------------------------------------------

# 149. Network Transition Test

Test:

``` text
online
 ↓
offline
 ↓
online
```

during:

``` text
game browsing
gameplay
discovery
sync
```

------------------------------------------------------------------------

# 150. FCM Offline Test

If a notification is delayed or missed:

``` text
Games list
```

must still show the published game when online.

FCM is not the authoritative source for game availability.

------------------------------------------------------------------------

# 151. Final Vertical Slice Test

The complete test:

``` text
1. Creator logs in
2. Creator creates game
3. Creator adds checkpoints
4. Creator saves draft
5. Creator publishes
6. Player receives notification
7. Player opens Game Details
8. Player joins
9. Player opens map
10. Player approaches checkpoint
11. Geofence enters
12. Scan starts
13. GPS/light/motion produce fusion
14. Fusion threshold reached
15. Proximity gate passes
16. Checkpoint revealed
17. Discovery saved to Room
18. Discovery synced to Firestore
19. Leaderboard updates
20. Player sees game-specific ranking
```

This is the final integration acceptance path.

------------------------------------------------------------------------

# 152. Minimum Demonstration Slice

If time is limited, demonstrate:

``` text
Creator Login
 ↓
Create Game
 ↓
Add 1 Checkpoint
 ↓
Publish
 ↓
Player sees Game
 ↓
Join
 ↓
Map
 ↓
Reach checkpoint
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
Firestore
 ↓
Leaderboard
```

This proves the architecture without requiring a six-checkpoint game.

------------------------------------------------------------------------

# 153. Stretch Integration

Only after the core slice is stable consider:

``` text
multiple simultaneous games
richer creator editing
advanced notification settings
relay/team features
additional analytics
```

These must not destabilize the MVP.

------------------------------------------------------------------------

# 154. Integration Priority

Priority order:

``` text
P0
Build + authentication

P0
Game creation/publishing

P0
Game browsing/join

P0
Dynamic checkpoints

P0
Location/geofence

P0
Sensor fusion

P0
Discovery persistence

P0
Game leaderboard

P0
Security

P1
FCM polish

P1
Offline robustness

P1
UI refinement

P2
Stretch features
```

------------------------------------------------------------------------

# 155. Integration Definition of Done

A boundary is done when:

``` text
contract exists
implementation exists
consumer integrated
producer integrated
basic test passes
failure case handled
no direct layer violation
```

------------------------------------------------------------------------

# 156. Feature Definition of Done

A feature is done when:

``` text
[ ] implemented
[ ] compiles
[ ] tested
[ ] integrated with repository
[ ] error state handled
[ ] lifecycle handled
[ ] shared IDs preserved
[ ] documentation consistent
```

------------------------------------------------------------------------

# 157. Final Stabilization Phase

During 24--27 September:

``` text
feature development slows
integration testing increases
breaking changes stop
bugs are prioritized
```

The team should avoid adding major architecture changes during
stabilization.

------------------------------------------------------------------------

# 158. Bug Priority

Use:

``` text
P0 — app cannot build/launch
P1 — core gameplay broken
P2 — major feature incorrect
P3 — UI/edge-case issue
P4 — cosmetic issue
```

Fix P0/P1 before P3/P4.

------------------------------------------------------------------------

# 159. Critical P0 Examples

``` text
Gradle build failure
Firebase configuration failure
application crash on launch
database migration crash
```

------------------------------------------------------------------------

# 160. Critical P1 Examples

``` text
cannot create game
cannot publish
published game invisible
cannot join
geofence never works
scan cannot complete
discovery not saved
leaderboard corrupt
```

------------------------------------------------------------------------

# 161. P2 Examples

``` text
notification formatting issue
incorrect empty state
minor cache delay
secondary navigation problem
```

------------------------------------------------------------------------

# 162. P3/P4 Examples

``` text
spacing
animation
icon alignment
minor wording
```

Do not spend final integration days on P4 issues while P1 gameplay
remains broken.

------------------------------------------------------------------------

# 163. Final Regression Pass

Before final demonstration:

``` text
[ ] authentication
[ ] creator flow
[ ] player flow
[ ] dynamic game list
[ ] dynamic checkpoints
[ ] map
[ ] geofence
[ ] sensor fusion
[ ] proximity
[ ] reveal
[ ] Room
[ ] Firestore
[ ] leaderboard
[ ] FCM
[ ] offline
[ ] permissions
[ ] restart
[ ] security
```

------------------------------------------------------------------------

# 164. Documentation Consistency Check

Before final freeze, search the documentation for obsolete production
terminology:

``` text
global relic list
fixed quest
R001-only flow
global leaderboard
global progress
relicId-only discovery
hard-coded locations
```

These may appear in historical/seed-data explanations, but must not
remain as the production architecture.

------------------------------------------------------------------------

# 165. Code Consistency Check

Search the codebase for:

``` text
RelicEntity
FoundRelicEntity
getRelics()
recordReveal(R001)
leaderboard/{uid}
relics/{relicId}
```

Any remaining occurrence must be classified as:

``` text
legacy compatibility
test/seed data
or
incorrect production implementation
```

------------------------------------------------------------------------

# 166. Required Final Repository Search

Before final merge, search for hard-coded:

``` text
R001
R002
R003
R004
R005
R006
```

Production gameplay code should not depend on them.

Seed/demo data may use them.

------------------------------------------------------------------------

# 167. Required Final Firestore Search

Verify the live schema uses:

``` text
users
games
games/{gameId}/checkpoints
gamePlayers
progress/{uid}/games/{gameId}/checkpoints
leaderboards/{gameId}/entries
```

Do not leave an old global schema as the active production path.

------------------------------------------------------------------------

# 168. Required Final Room Search

Verify the local architecture uses:

``` text
GameEntity
CheckpointEntity
GamePlayerEntity
FoundCheckpointEntity
```

with game-scoped identities.

------------------------------------------------------------------------

# 169. Required Final Navigation Search

Verify navigation supports:

``` text
Games
Game Details
Join
Game Map
Checkpoint/Scan
Leaderboard
Creator Game Management
Checkpoint Editor
Publish
```

Notification entry must carry:

``` text
gameId
```

------------------------------------------------------------------------

# 170. Required Final Sensor Search

Verify:

``` text
GPS
Light
Motion
```

are the fusion inputs.

Verify:

``` text
Proximity
```

is the final gate.

------------------------------------------------------------------------

# 171. Required Final Firebase Search

Verify:

``` text
creatorId
gameId
checkpointId
userId
```

are preserved wherever required.

------------------------------------------------------------------------

# 172. Integration Meeting Protocol

When a member is blocked by another member:

``` text
1. Identify exact boundary.
2. State expected input.
3. State actual input.
4. Identify contract mismatch.
5. Agree smallest correction.
6. Update shared contract if necessary.
7. Re-test boundary.
```

Do not solve cross-member conflicts by creating hidden duplicate models.

------------------------------------------------------------------------

# 173. Integration Handoff Template

Every handoff should contain:

``` text
Feature:
Owner:
Consumer:
Inputs:
Outputs:
Repository method:
Data model:
Dependencies:
Test procedure:
Known limitations:
```

------------------------------------------------------------------------

# 174. M1 Handoff Example

``` text
Feature:
Game Details → Join

Owner:
M1

Consumer:
M1 gameplay navigation / M5 repository

Inputs:
gameId

Output:
joined state

Repository:
joinGame(gameId)

Dependency:
authenticated user

Test:
open published game → Join → membership exists
```

------------------------------------------------------------------------

# 175. M3 Handoff Example

``` text
Feature:
Checkpoint geofence

Owner:
M3

Consumer:
M2/M4

Inputs:
Checkpoint location/radius

Output:
geofence ENTER event / DistanceResult

Dependencies:
location permissions

Test:
enter configured radius → selected scan becomes available
```

------------------------------------------------------------------------

# 176. M4 Handoff Example

``` text
Feature:
Fusion engine

Owner:
M4

Consumer:
M2

Inputs:
DistanceResult
LightSignature
Motion data

Output:
FusionResult
ScanState
SensorAvailability

Test:
valid inputs → expected score/state
```

------------------------------------------------------------------------

# 177. M5 Handoff Example

``` text
Feature:
Cloud progress

Owner:
M5

Consumer:
M6/M2

Inputs:
userId
gameId
checkpointId
foundAt

Output:
cloud progress

Path:
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}

Test:
discovery sync → cloud record
```

------------------------------------------------------------------------

# 178. M6 Handoff Example

``` text
Feature:
Offline discovery

Owner:
M6

Consumer:
M2/M5

Inputs:
gameId
userId
checkpointId
foundAt

Output:
local discovery + pendingSync

Test:
offline discovery → restart → reconnect → cloud sync
```

------------------------------------------------------------------------

# 179. Shared Integration Checklist

``` text
[ ] Game model stable
[ ] Checkpoint model stable
[ ] GamePlayer model stable
[ ] Discovery model stable
[ ] Leaderboard model stable
[ ] Repository contract stable
[ ] Auth contract stable
[ ] Sensor contract stable
[ ] Firebase schema stable
[ ] Room schema stable
```

------------------------------------------------------------------------

# 180. Final Security Integration

Integration is not complete until security is tested.

Test:

``` text
anonymous user
cross-user user
non-owner creator
cross-game player
forged IDs
draft access
published access
```

------------------------------------------------------------------------

# 181. Final Data Integrity Integration

Verify:

``` text
checkpoint.gameId == parent game
membership.gameId == selected game
membership.userId == authenticated UID
progress.gameId == selected game
progress.checkpointId == selected checkpoint
progress.userId == authenticated UID
leaderboard.gameId == selected game
leaderboard.userId == authenticated UID
```

------------------------------------------------------------------------

# 182. Final Offline Integration

Verify:

``` text
Room survives temporary network failure
pendingSync is retained
sync retries
successful sync clears pending state
duplicate sync remains idempotent
```

------------------------------------------------------------------------

# 183. Final Notification Integration

Verify:

``` text
publish
 ↓
notification
 ↓
gameId
 ↓
correct Game Details
```

Also verify:

``` text
draft save
 ↓
no notification
```

------------------------------------------------------------------------

# 184. Final Creator Integration

Verify:

``` text
creator can create
creator can edit draft
creator can add checkpoints
creator can publish
creator cannot edit another creator's game
```

------------------------------------------------------------------------

# 185. Final Player Integration

Verify:

``` text
player can browse
player can open details
player can join
player can play
player can discover
player can see game-specific progress
player can see game-specific leaderboard
```

------------------------------------------------------------------------

# 186. Final Game Isolation Integration

Verify:

``` text
Game A data stays in Game A
Game B data stays in Game B
```

This applies to:

``` text
checkpoints
membership
progress
leaderboards
map markers
geofences
scan configuration
```

------------------------------------------------------------------------

# 187. Final Dynamic Configuration Integration

Verify that a newly created game works without:

``` text
new Kotlin code
new hard-coded coordinates
new hard-coded checkpoint ID
new hard-coded leaderboard
```

The application should consume configuration from the data model.

------------------------------------------------------------------------

# 188. Final Demo Preparation

Prepare:

``` text
one creator account
two player accounts
one sample game
three or more checkpoints if physically practical
Firebase project
physical Android device
network connection
backup test data
```

The six-checkpoint sample can be used if convenient.

------------------------------------------------------------------------

# 189. Demo Fallback

If live FCM fails:

``` text
show published game in Games list
```

If network fails during discovery:

``` text
demonstrate Room/offline save
```

The demo should not depend on one fragile external component.

------------------------------------------------------------------------

# 190. Demo Evidence

Capture evidence for:

``` text
Creator game creation
Checkpoint configuration
Published status
Notification
Player join
Map
Geofence
Fusion meter
Reveal
Room/local state
Firestore progress
Leaderboard
```

------------------------------------------------------------------------

# 191. Final Build Candidate

The final candidate must:

``` text
build from clean checkout
install
launch
authenticate
create/play a game
persist discoveries
display leaderboard
```

A developer-specific local configuration must not be required unless
documented.

------------------------------------------------------------------------

# 192. Clean Checkout Test

Use a fresh checkout:

``` text
git clone
 ↓
open Android Studio
 ↓
sync
 ↓
build
 ↓
install
 ↓
launch
```

This catches:

``` text
ignored files
missing configuration
local-only dependencies
```

------------------------------------------------------------------------

# 193. Final Git Hygiene

Before submission:

``` text
[ ] no passwords
[ ] no service account keys
[ ] no API secrets
[ ] no debug dumps
[ ] no local absolute paths
[ ] no unnecessary generated files
[ ] no temporary test code
```

------------------------------------------------------------------------

# 194. Final Integration Freeze

After final candidate is accepted:

``` text
main
 ↓
release candidate
```

Only critical fixes should be merged.

Every critical fix should be:

``` text
tested
reviewed
documented
```

------------------------------------------------------------------------

# 195. Final Responsibility Matrix

  -------------------------------------------------------------------------------
  Area                  M1         M2         M3         M4         M5         M6
  ------------- ---------- ---------- ---------- ---------- ---------- ----------
  Navigation          Lead    Support         \-         \-         \-         \-

  Game browsing       Lead    Support         \-       Data       Data      Cache

  Creator flow        Lead         \-         \-         \-      Cloud      Local

  Checkpoint          Lead    Support   Location     Sensor      Cloud      Local
  editor                                             fields            

  Scan UI               \-       Lead    Support    Support         \-         \-

  Location              \-         \-       Lead      Input       Data      Cache

  Geofence              \-    Support       Lead         \-         \-         \-

  Sensor fusion         \-    Display      Input       Lead         \-         \-

  Auth                  UI         \-         \-         \-       Lead    Support

  Firestore             \-         \-         \-         \-       Lead    Support

  FCM              UI/deep         \-         \-         \-       Lead         \-
                      link                                             

  Room                  \-   Consumer         \-         \-    Support       Lead

  Offline sync          \-   Consumer         \-         \-      Cloud       Lead

  Leaderboard           UI    Lead UI         \-         \-      Cloud      Cache

  Integration      Support    Support    Support    Support    Support       Lead
  -------------------------------------------------------------------------------

------------------------------------------------------------------------

# 196. Communication Rules

When communicating an integration issue, use:

``` text
BOUNDARY:
M3 → M4

EXPECTED:
DistanceResult

ACTUAL:
raw distance Float

IMPACT:
Fusion module cannot consume location data

REQUEST:
Use agreed DistanceResult contract
```

This is better than:

``` text
"Your code doesn't work."
```

------------------------------------------------------------------------

# 197. Conflict Resolution Rule

When two implementations disagree:

``` text
1. Check shared contract.
2. Check master plan.
3. Prefer canonical model.
4. Avoid duplicate compatibility layers unless temporary.
5. Update dependent code.
6. Re-run integration tests.
```

------------------------------------------------------------------------

# 198. Avoid Integration by Duplication

Do not create:

``` text
M2Checkpoint
M3Checkpoint
M4Checkpoint
M5Checkpoint
M6Checkpoint
```

as separate representations of the same domain object.

Use the shared:

``` text
Checkpoint
```

model where appropriate.

------------------------------------------------------------------------

# 199. Avoid Integration by Direct Access

Do not solve:

``` text
M2 needs data
```

with:

``` text
M2 → Firestore
```

or:

``` text
M2 → Room DAO
```

Use:

``` text
M2 → ViewModel → Repository
```

------------------------------------------------------------------------

# 200. Avoid Integration by Hard-Coding

Do not solve:

``` text
Firebase not ready
```

by permanently hard-coding:

``` text
R001
R002
```

Use mock repository implementations behind the same contract if
necessary.

------------------------------------------------------------------------

# 201. Mock Repository Strategy

During early integration:

``` text
MockGameRepository
```

may return:

``` text
Game
Checkpoint
Leaderboard
```

using the same application-level interfaces.

Later:

``` text
Firebase/Room repository
```

can replace the mock.

The consumer should not need to change.

------------------------------------------------------------------------

# 202. Mock-to-Real Transition

``` text
Mock Repository
 ↓
UI integration
 ↓
M3/M4 integration
 ↓
Room integration
 ↓
Firebase integration
```

The contract stays stable.

------------------------------------------------------------------------

# 203. Mock Data Rule

Mock data should represent the new model:

``` text
Game
Checkpoint
GamePlayer
Progress
Leaderboard
```

R001--R006 may exist as seed checkpoint IDs.

They must belong to a game.

------------------------------------------------------------------------

# 204. Mock Multi-Game Data

Minimum mock scenario:

``` text
GAME_A
 ├── A1
 └── A2

GAME_B
 ├── B1
 ├── B2
 └── B3
```

Players:

``` text
PLAYER_A
PLAYER_B
```

This should be sufficient to test game isolation.

------------------------------------------------------------------------

# 205. Repository Swap Test

The UI should work with:

``` text
MockGameRepository
```

and:

``` text
RealGameRepository
```

without changing the UI contract.

This proves the repository boundary is working.

------------------------------------------------------------------------

# 206. Firebase/Room Swap Test

The repository may initially use:

``` text
Mock
```

then:

``` text
Room
```

then:

``` text
Room + Firebase
```

without changing the consumer-level model.

------------------------------------------------------------------------

# 207. Integration Completion Criteria

Integration is complete when:

``` text
[ ] Creator flow works
[ ] Player flow works
[ ] Dynamic game data works
[ ] Dynamic checkpoints work
[ ] Location works
[ ] Geofencing works
[ ] Sensor fusion works
[ ] Proximity gate works
[ ] Discovery works
[ ] Room works
[ ] Firestore works
[ ] Leaderboard works
[ ] FCM works
[ ] Offline sync works
[ ] Security works
```

------------------------------------------------------------------------

# 208. Final Vertical Slice Acceptance

The application must demonstrate:

``` text
Creator
  ↓
Game
  ↓
Checkpoint
  ↓
Publish
  ↓
Notification
  ↓
Player
  ↓
Join
  ↓
Location
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
Firestore
  ↓
Leaderboard
```

------------------------------------------------------------------------

# 209. Final Cross-Game Acceptance

The application must also demonstrate:

``` text
Game A
 └── Player progress A

Game B
 └── Player progress B
```

with no cross-game contamination.

------------------------------------------------------------------------

# 210. Final Documentation Acceptance

Every final document must agree on:

``` text
Game Creator
Game Player
Game
Checkpoint
GamePlayer
game-scoped progress
game-specific leaderboard
FCM new-game notification
Room + Firestore
GPS + Light + Motion
Proximity final gate
```

------------------------------------------------------------------------

# 211. Final Integration Statement

Campus Quest is integrated successfully when the six members'
implementations behave as one system rather than six independent
features.

The target system is:

``` text
                         CAMPUS QUEST
                              |
               +--------------+--------------+
               |                             |
         GAME CREATOR                   GAME PLAYER
               |                             |
         Create Game                    Browse Games
               |                             |
        Configure Checkpoints            Game Details
               |                             |
            Save Draft                     Join
               |                             |
            Publish                     Game Map
               |                             |
         FCM Notification              Geofence
                                             |
                                            Scan
                                             |
                                   GPS + Light + Motion
                                             |
                                      Fusion Threshold
                                             |
                                      Proximity Gate
                                             |
                                          Reveal
                                             |
                                           Room
                                             |
                                         Firestore
                                             |
                                  Game Leaderboard
```

The integration strategy therefore prioritizes:

``` text
shared contracts
+
game-scoped identity
+
incremental boundaries
+
repository abstraction
+
offline persistence
+
cloud synchronization
+
repeatable end-to-end testing
```

The final application must not depend on the old fixed six-relic
architecture.

The six sample records may remain as demonstration seed data, but the
integrated application must work with arbitrary creator-defined games
and checkpoints.
