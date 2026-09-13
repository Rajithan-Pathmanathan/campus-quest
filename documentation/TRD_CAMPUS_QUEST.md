# TRD — TECHNICAL REQUIREMENTS DOCUMENT
## Campus Quest — Final Dynamic Creator/Player Architecture

**Status:** Final reassignment-compatible version  
**Platform:** Android  
**Language:** Kotlin  
**Architecture:** MVVM + Repository + Room + Firebase/Firestore  
**Team:** Six-member implementation model

---

# 1. PURPOSE

This Technical Requirements Document defines the technical architecture, interfaces, data requirements, platform requirements, physical-signal mechanism, persistence model, synchronization behavior, security requirements, navigation contracts, testing requirements, and integration rules for Campus Quest.

The system is a dynamic campus treasure-hunt application.

The fundamental product model is:

```text
Creator
   ↓
Game
   ↓
0..N Checkpoints
   ↓
Player joins
   ↓
Dynamic physical discovery
   ↓
Game-scoped progress
   ↓
Game-scoped leaderboard
```

The application must not depend on a permanent fixed set of six checkpoints.

---

# 2. TECHNICAL OBJECTIVES

The implementation must provide:

```text
Android application
Kotlin implementation
MVVM architecture
Repository abstraction
Room local persistence
Firebase Authentication
Cloud Firestore
FCM notifications
Google Maps/location services
Dynamic geofencing
Ambient-light sensing
Accelerometer motion detection
Proximity final gate
GPS + Light + Motion fusion
Offline-aware persistence
Synchronization
Game-specific leaderboard
Creator workflow
Player workflow
```

---

# 3. ARCHITECTURE

Recommended architecture:

```text
Presentation
    ↓
ViewModel
    ↓
Repository
    ↓
┌───────────────┬───────────────┐
│     Room      │   Firebase    │
│ local storage │ cloud storage │
└───────────────┴───────────────┘
```

Physical signal pipeline:

```text
Location / Light / Motion / Proximity
              ↓
             M3
              ↓
       FusionResult
              ↓
             M4
              ↓
        Scan State
              ↓
           Reveal
```

The course architecture material describes MVVM as separating the View from ViewModel state/logic and identifies lifecycle survival and testability as important benefits. fileciteturn36file0L163-L177

---

# 4. TEAM TECHNICAL OWNERSHIP

| Member | Technical responsibility |
|---|---|
| M1 | Player UI & Navigation |
| M2 | Creator & Game Management |
| M3 | Location, Sensors & Fusion |
| M4 | Quest & Scan Gameplay |
| M5 | Firebase & Backend |
| M6 | Room, Repository, Sync & Integration |

Ownership controls implementation responsibility; shared contracts remain common to the team.

---

# 5. DOMAIN MODEL

## 5.1 Game

```kotlin
enum class GameStatus {
    DRAFT,
    PUBLISHED,
    CLOSED
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
```

## 5.2 Checkpoint

```kotlin
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

## 5.3 Light Signature

```kotlin
data class LightSignature(
    val minLux: Float,
    val maxLux: Float
)
```

Light signature represents expected ambient environmental light.

It does not represent light emitted by a relic.

---

# 6. IDENTITY AND SCOPING

The authoritative identities are:

```text
userId
gameId
checkpointId
```

A checkpoint is always interpreted in game scope:

```text
gameId + checkpointId
```

Progress is scoped by:

```text
userId + gameId + checkpointId
```

Leaderboard data is scoped by:

```text
gameId + userId
```

No implementation should rely on display names as identity.

---

# 7. DYNAMIC GAME REQUIREMENT

The application must support creator-defined games.

A game can contain a variable number of checkpoints.

Examples:

```text
2 checkpoints
4 checkpoints
6 checkpoints
8 checkpoints
```

The code must not contain:

```kotlin
val checkpoints = listOf(R001, R002, R003, R004, R005, R006)
```

as the production source of checkpoint configuration.

`R001–R006` are permitted only as sample/demo seed data.

---

# 8. CREATOR TECHNICAL REQUIREMENTS

M2's creator feature must support:

```text
Create game
Edit game
Save draft
Add checkpoint
Edit checkpoint
Delete checkpoint
Reorder checkpoints
Configure coordinates
Configure radius
Configure light signature
Configure clue
Configure lore
Configure rarity
Configure motion type
Publish game
```

---

# 9. GAME DRAFT

Draft state:

```text
DRAFT
```

A draft may be modified by its creator.

Draft data must not appear as an active published game to ordinary players.

---

# 10. PUBLISHING

Publishing requires:

```text
authenticated creator
ownership
valid game fields
valid checkpoint configuration
```

Successful publication changes:

```text
DRAFT → PUBLISHED
```

and records:

```text
publishedAt
```

---

# 11. PLAYER TECHNICAL REQUIREMENTS

M1 must support:

```text
Login
Browse published games
View game details
Join game
Open game map
Open checkpoint gameplay
View progress
View game-specific leaderboard
Open games from notifications
```

---

# 12. AUTHENTICATION

Firebase Authentication provides the authoritative user identity.

The application must handle:

```text
login success
login failure
logout
expired/invalid session
unauthenticated navigation
```

The authenticated UID is used for:

```text
creator ownership
game membership
progress
leaderboard
```

---

# 13. FIRESTORE STRUCTURE

Canonical structure:

```text
users/{uid}

games/{gameId}

games/{gameId}/checkpoints/{checkpointId}

gamePlayers/{gameId}_{uid}

progress/{uid}/games/{gameId}/checkpoints/{checkpointId}

leaderboards/{gameId}/entries/{uid}
```

FCM topic:

```text
/topics/new_games
```

---

# 14. GAME DOCUMENT

Required fields:

```text
id
title
description
creatorId
creatorName
status
checkpointCount
createdAt
publishedAt
```

---

# 15. CHECKPOINT DOCUMENT

Required fields:

```text
id
gameId
name
lat
lng
radiusM
lightSignature
clue
lore
order
motionType
rarity
```

---

# 16. MEMBERSHIP

A player joining a game creates a game-scoped membership.

Canonical identifier:

```text
gamePlayers/{gameId}_{uid}
```

Required:

```text
gameId
userId
joinedAt
```

---

# 17. PROGRESS

Canonical progress scope:

```text
userId
gameId
checkpointId
```

A discovery record should contain:

```text
userId
gameId
checkpointId
foundAt
```

Optional:

```text
scanScore
syncedAt
```

---

# 18. IDEMPOTENT DISCOVERY

Repeated attempts to record the same:

```text
userId + gameId + checkpointId
```

must not create duplicate logical completion.

This protects against:

```text
duplicate geofence events
button repetition
network retries
offline synchronization
```

---

# 19. LEADERBOARD

There is no global leaderboard in the production architecture.

Canonical path:

```text
leaderboards/{gameId}/entries/{uid}
```

The leaderboard belongs to the game currently being played.

---

# 20. REPOSITORY

The repository is the common data boundary.

Canonical interface:

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

---

# 21. REPOSITORY RULE

Consumers use:

```text
GameRepository
```

rather than directly depending on:

```text
Firestore
Room
```

The repository may combine:

```text
cloud
cache
local writes
pending synchronization
```

without exposing implementation details to the UI.

---

# 22. ROOM REQUIREMENTS

M6 owns local persistence.

Required entities:

```text
GameEntity
CheckpointEntity
GamePlayerEntity
FoundCheckpointEntity
```

Optional:

```text
PendingSyncEntity
```

---

# 23. ROOM KEYS

Recommended composite keys:

```text
CheckpointEntity:
gameId + id

GamePlayerEntity:
gameId + userId

FoundCheckpointEntity:
gameId + userId + checkpointId
```

This is required to preserve game/user isolation.

---

# 24. ROOM CHECKPOINT ENTITY

```kotlin
@Entity(
    tableName = "checkpoints",
    primaryKeys = ["gameId", "id"]
)
data class CheckpointEntity(
    val id: String,
    val gameId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val minLux: Float,
    val maxLux: Float,
    val clue: String,
    val lore: String,
    val order: Int,
    val motionType: String,
    val rarity: String
)
```

---

# 25. ROOM DISCOVERY ENTITY

```kotlin
@Entity(
    tableName = "found_checkpoints",
    primaryKeys = ["gameId", "userId", "checkpointId"]
)
data class FoundCheckpointEntity(
    val gameId: String,
    val userId: String,
    val checkpointId: String,
    val foundAt: Long,
    val pendingSync: Boolean
)
```

---

# 26. OFFLINE REQUIREMENT

When the network is unavailable, supported locally cached data must remain available.

For a successful offline discovery:

```text
physical scan succeeds
        ↓
Room write
        ↓
pendingSync = true
```

When connectivity returns:

```text
pending operation
        ↓
repository sync
        ↓
Firestore
        ↓
pendingSync = false
```

---

# 27. SYNC REQUIREMENTS

Synchronization must:

```text
retain successful local discoveries
retry temporary failures
avoid duplicate cloud completions
preserve game/user scope
survive application restart
```

---

# 28. LOCATION REQUIREMENTS

M3 must use the Android fused location provider for location acquisition.

Required handling:

```text
permission
location updates
accuracy
distance
lifecycle
cleanup
```

---

# 29. LOCATION DATA

A location sample should provide enough information for:

```text
distance calculation
accuracy assessment
timestamp validation
normalized location score
```

---

# 30. LOCATION SIGNAL

Recommended contract:

```kotlin
data class LocationSignal(
    val distanceM: Float,
    val normalizedScore: Float,
    val accuracyM: Float,
    val isInsideGeofence: Boolean
)
```

---

# 31. GEOFENCING

Each checkpoint has:

```text
radiusM
```

M3 dynamically registers geofences for the current game.

Recommended identifier:

```text
{gameId}:{checkpointId}
```

Example:

```text
demo-campus-quest:R001
```

---

# 32. GAME-SCOPED GEOFENCING

When the active game changes:

```text
remove old game's geofences
register new game's geofences
```

An event from an old game must not activate the current game's scan.

---

# 33. GEOFENCE ENTER

Geofence ENTER means:

```text
checkpoint is physically nearby
```

It does not mean:

```text
checkpoint discovered
```

Discovery requires the scan mechanic.

---

# 34. AMBIENT LIGHT

M3 reads the ambient light sensor where available.

Input:

```text
currentLux
```

Expected checkpoint range:

```text
minLux
maxLux
```

---

# 35. LIGHT SIGNAL

Recommended:

```kotlin
data class LightSignal(
    val currentLux: Float,
    val normalizedScore: Float,
    val isWithinExpectedRange: Boolean
)
```

---

# 36. LIGHT VALIDATION

Required:

```text
minLux >= 0
maxLux >= 0
minLux <= maxLux
```

---

# 37. LIGHT CALIBRATION

The final physical demo must use measured environmental light values.

Procedure:

```text
visit checkpoint
collect multiple samples
observe variation
select range
test range
save configuration
```

Example seed lux values are not guaranteed production calibration values.

---

# 38. ACCELEROMETER

M3 uses accelerometer data for the configured motion mechanic.

Initial supported value:

```text
SWEEP
```

The motion detector must produce a deterministic score/state from collected samples.

---

# 39. MOTION SIGNAL

Recommended:

```kotlin
data class MotionSignal(
    val normalizedScore: Float,
    val sweepDetected: Boolean
)
```

---

# 40. PROXIMITY

M3 may use the device proximity sensor for close-range confirmation.

Recommended:

```kotlin
data class ProximitySignal(
    val isNear: Boolean
)
```

Proximity is a separate final gate.

---

# 41. SENSOR FUSION

The weighted fusion inputs are exactly:

```text
GPS
Light
Motion
```

Conceptually:

```text
GPS score
+
Light score
+
Motion score
        ↓
weighted fusion
        ↓
fusion threshold
```

---

# 42. PROXIMITY FINAL GATE

The final discovery condition is:

```text
fusionScore >= threshold
AND
proximityNear == true
```

Proximity must not be included as a fourth weighted fusion input.

---

# 43. FUSION RESULT

Recommended:

```kotlin
data class FusionResult(
    val gpsScore: Float,
    val lightScore: Float,
    val motionScore: Float,
    val fusionScore: Float,
    val thresholdReached: Boolean,
    val proximityNear: Boolean
)
```

---

# 44. FUSION ENGINE

Recommended interface:

```kotlin
interface FusionEngine {
    fun calculate(
        location: LocationSignal,
        light: LightSignal,
        motion: MotionSignal
    ): FusionResult
}
```

---

# 45. FUSION WEIGHTS

Represent weights centrally:

```kotlin
data class FusionWeights(
    val gps: Float,
    val light: Float,
    val motion: Float
)
```

If normalized weighted averaging is used:

```text
gps + light + motion = 1.0
```

Weights must not be duplicated in UI code.

---

# 46. FUSION NORMALIZATION

Each weighted input should be normalized to:

```text
0.0–1.0
```

before combination.

Final fusion output should also remain within:

```text
0.0–1.0
```

or use one consistent percentage representation.

---

# 47. THRESHOLD

A scan reaches the fusion stage when:

```text
fusionScore >= configured threshold
```

This is not final success.

---

# 48. SUCCESS CONDITION

Final physical success:

```text
current game
AND
current checkpoint
AND
fusionScore >= threshold
AND
proximityNear
```

Only then can M4 show the successful reveal.

---

# 49. M3/M4 BOUNDARY

M3 produces:

```text
LocationSignal
LightSignal
MotionSignal
ProximitySignal
FusionResult
availability/degradation state
```

M4 produces:

```text
ScanState
scan UI
progress meter
success/failure state
reveal
```

---

# 50. SCAN STATE

Recommended:

```kotlin
sealed interface ScanState {
    data object Idle : ScanState
    data object WaitingForLocation : ScanState
    data object CollectingSignals : ScanState
    data class Progress(
        val fusionScore: Float
    ) : ScanState
    data object WaitingForProximity : ScanState
    data object Success : ScanState
    data class Failed(
        val reason: String
    ) : ScanState
}
```

---

# 51. SCAN NAVIGATION

Player routes should carry explicit scope.

Examples:

```text
GameDetails(gameId)
GameMap(gameId)
Checkpoint(gameId, checkpointId)
Scan(gameId, checkpointId)
Reveal(gameId, checkpointId)
Leaderboard(gameId)
```

---

# 52. CREATOR NAVIGATION

Examples:

```text
CreatorHome
CreateGame
EditGame(gameId)
CheckpointList(gameId)
EditCheckpoint(gameId, checkpointId)
PublishGame(gameId)
```

---

# 53. FCM

Publishing a game should generate a new-game notification.

Topic:

```text
/topics/new_games
```

Payload:

```kotlin
data class NewGameNotification(
    val gameId: String,
    val gameTitle: String,
    val creatorName: String,
    val checkpointCount: Int,
    val publishedAt: Long
)
```

---

# 54. FCM DEEP LINK

Notification tap:

```text
notification
 ↓
gameId
 ↓
GameDetails(gameId)
```

No hard-coded destination game is permitted.

---

# 55. SECURITY

M5 must implement Firestore security rules that enforce:

```text
authentication
creator ownership
membership
user-scoped progress
game-scoped leaderboard access
```

Client-side UI checks are not sufficient authorization.

---

# 56. CREATOR OWNERSHIP

A creator may modify a game only when:

```text
game.creatorId == authenticatedUser.uid
```

The backend must enforce this condition.

---

# 57. GAME ISOLATION

The application must pass:

```text
Game A progress ≠ Game B progress
```

even when:

```text
checkpointId is identical
```

Example:

```text
Game A / R001
Game B / R001
```

are different scoped checkpoints.

---

# 58. USER ISOLATION

The application must pass:

```text
Player A / Game A / R001
```

does not imply:

```text
Player B / Game A / R001
```

is completed.

---

# 59. LIFECYCLE

M3 must clean up:

```text
sensor listeners
location callbacks
temporary scan resources
```

when the relevant lifecycle ends.

M4 must not create duplicate physical pipelines on rotation.

---

# 60. BATTERY

High-frequency physical sampling should be limited to active scan periods.

Preferred:

```text
approach/geofence
 ↓
activate intensive scan
 ↓
collect signals
 ↓
success/cancel
 ↓
stop intensive sampling
```

---

# 61. SENSOR FAILURE

Possible states:

```text
sensor available
sensor unavailable
degraded
```

The system must not fabricate successful sensor values.

---

# 62. LOCATION FAILURE

Handle:

```text
permission denied
location unavailable
poor accuracy
stale location
```

The scan UI must receive an explicit state.

---

# 63. OFFLINE PHYSICAL DISCOVERY

Physical calculation must not require a live Firebase connection.

If checkpoint configuration is locally cached:

```text
location
light
motion
fusion
proximity
```

can operate locally.

Persistence then uses:

```text
Room → pending sync → Firebase
```

---

# 64. TESTING REQUIREMENTS

Testing must cover:

```text
unit
repository
Room
Firebase/security
sensor/fusion
navigation
UI
integration
offline/sync
physical-device
```

---

# 65. LOCATION TESTS

Minimum:

```text
inside radius
outside radius
near/far distances
poor accuracy
permission denied
stale location
multiple checkpoints
```

---

# 66. GEOFENCE TESTS

Minimum:

```text
ENTER
EXIT
duplicate ENTER
old-game event
game switch
registration
unregistration
```

---

# 67. LIGHT TESTS

Minimum:

```text
inside range
below range
above range
boundary
invalid range
sensor unavailable
```

---

# 68. MOTION TESTS

Minimum:

```text
valid sweep
weak movement
random movement
no movement
partial sweep
sensor unavailable
```

---

# 69. FUSION TESTS

Test:

```text
all strong
all weak
GPS strong only
Light strong only
Motion strong only
boundary threshold
clamped values
```

Expected results must be deterministic.

---

# 70. PROXIMITY TEST MATRIX

| Fusion | Proximity | Expected |
|---|---|---|
| below threshold | false | fail |
| below threshold | true | fail |
| threshold reached | false | wait/no reveal |
| threshold reached | true | success |

---

# 71. REPOSITORY TESTS

Test:

```text
game creation
checkpoint creation
game loading
checkpoint loading
publish
join
discovery
leaderboard
sync
```

---

# 72. ROOM TESTS

Test:

```text
game isolation
user isolation
checkpoint composite keys
duplicate discovery
pending sync
migration
transaction behavior
```

---

# 73. FIREBASE TESTS

Test:

```text
creator ownership
unauthorized writes
player membership
progress isolation
leaderboard isolation
published game visibility
FCM payload
```

---

# 74. DYNAMIC GAME TESTS

At minimum:

```text
2 checkpoints
4 checkpoints
6 checkpoints
8 checkpoints
```

Verify:

```text
creator
map
geofence
scan
progress
leaderboard
```

No code change should be necessary between counts.

---

# 75. CROSS-GAME TEST

Create:

```text
Game A / A001
Game B / B001
```

Complete A001.

Verify:

```text
B001 remains incomplete.
```

---

# 76. SAME-ID TEST

Create:

```text
Game A / R001
Game B / R001
```

Verify:

```text
Game A:R001
Game B:R001
```

remain independent.

---

# 77. OFFLINE TEST

```text
cache game
disable network
complete scan
save locally
restore network
sync
verify cloud
```

---

# 78. ROTATION TEST

During scan:

```text
rotate device
```

Verify:

```text
no duplicate listeners
no duplicate discovery
scan state preserved appropriately
```

---

# 79. PHYSICAL DEVICE REQUIREMENT

The final sensor mechanic must be tested on the actual Android device.

Verify:

```text
GPS
ambient light
accelerometer
proximity
geofence behavior
battery
lifecycle
```

Emulator-only testing is insufficient for the physical-signal mechanic.

---

# 80. BUILD REQUIREMENTS

Every integrated build must:

```text
compile successfully
run on target Android version/device
pass smoke tests
contain no committed secrets
```

---

# 81. CONFIGURATION

Environment-specific values should not be scattered through feature code.

Examples:

```text
Firebase configuration
map configuration
fusion weights
fusion threshold
```

should have controlled configuration ownership.

---

# 82. LOGGING

Development diagnostics may include:

```text
gameId
checkpointId
distance
accuracy
lux
motion state
individual scores
fusion score
threshold state
proximity state
```

Never log:

```text
passwords
authentication tokens
private credentials
```

---

# 83. PERFORMANCE

Avoid:

```text
unnecessary sensor listeners
permanent high-frequency sampling
duplicate location updates
duplicate Firestore listeners
unbounded retry loops
```

---

# 84. ERROR MODEL

The application should represent meaningful states for:

```text
Loading
Empty
PermissionDenied
NetworkUnavailable
AuthenticationFailure
ValidationFailure
LocationUnavailable
SensorUnavailable
ScanFailure
SyncFailure
Success
```

---

# 85. DATA MAPPING

Centralize mappings:

```text
Game ↔ GameEntity
Checkpoint ↔ CheckpointEntity
GamePlayer ↔ GamePlayerEntity
FoundCheckpoint ↔ FoundCheckpointEntity
```

Avoid repeating conversion logic across screens.

---

# 86. LEGACY RULE

Legacy relic-centric classes such as:

```text
RelicEntity
FoundRelicEntity
```

must not remain the primary architecture.

If retained temporarily, their purpose must be explicitly:

```text
migration
legacy compatibility
test
seed
```

---

# 87. HARDCODED DATA RULE

Production logic must not hard-code:

```text
R001
R002
R003
R004
R005
R006
```

as required application checkpoints.

Seed data may contain those identifiers.

---

# 88. NO GLOBAL LEADERBOARD

The technical implementation must not create a production dependency on:

```text
global leaderboard
```

Each game uses:

```text
leaderboards/{gameId}/entries
```

---

# 89. UI TECHNICAL REQUIREMENTS

M1/M2/M4 screens should follow the approved UI/UX specification.

Layouts must account for:

```text
different screen sizes
orientation
safe areas
readability
touch targets
loading states
error states
empty states
```

The course UI/UX material covers responsive layout concerns and touch-target/readability guidance for mobile interfaces. fileciteturn36file0L209-L235

---

# 90. INTEGRATION DEPENDENCIES

```text
M2 → M5/M6
Game/checkpoint creation
```

```text
M3 → M4
Physical signals/fusion
```

```text
M5 → M6
Cloud implementation
```

```text
M6 → M1/M2/M4
Repository/offline/sync
```

```text
M1 ↔ M4
Navigation
```

```text
M5 ↔ M1
FCM/deep links
```

---

# 91. INTEGRATION ORDER

Recommended:

```text
Shared contracts
 ↓
M6 foundation
 ↓
M5 Firebase
 ↓
M1 navigation
 ↓
M2 creator
 ↓
M3 physical pipeline
 ↓
M4 scan
 ↓
M6 synchronization
 ↓
FCM/deep links
 ↓
full E2E
```

---

# 92. ACCEPTANCE GOLDEN PATH

```text
Creator login
 ↓
Create game
 ↓
Add N checkpoints
 ↓
Save draft
 ↓
Publish
 ↓
FCM notification
 ↓
Player opens notification
 ↓
Game Details
 ↓
Join
 ↓
Game Map
 ↓
Dynamic checkpoint
 ↓
Geofence ENTER
 ↓
Scan
 ↓
GPS + Light + Motion
 ↓
Fusion threshold
 ↓
Proximity final gate
 ↓
Reveal
 ↓
Record discovery
 ↓
Room
 ↓
Firebase sync
 ↓
Game leaderboard
```

---

# 93. FINAL TECHNICAL ACCEPTANCE

The release candidate must demonstrate:

```text
[ ] Dynamic game creation
[ ] Dynamic checkpoint creation
[ ] Draft
[ ] Publish
[ ] Player discovery
[ ] Join
[ ] Dynamic map
[ ] Dynamic geofences
[ ] GPS signal
[ ] Light signal
[ ] Motion signal
[ ] Weighted fusion
[ ] Separate proximity gate
[ ] Scan state machine
[ ] Reveal
[ ] Room persistence
[ ] Firebase sync
[ ] Game-specific leaderboard
[ ] FCM notification
[ ] Deep link
[ ] Offline behavior
[ ] Multi-game isolation
[ ] Multi-user isolation
[ ] Security rules
[ ] Physical-device validation
```

---

# 94. NON-FUNCTIONAL REQUIREMENTS

The application should be:

```text
maintainable
testable
responsive
lifecycle-safe
offline-aware
secure
game-scoped
user-scoped
```

---

# 95. FINAL ARCHITECTURAL RULES

```text
1. MVVM separates presentation from UI state/logic.
2. Repository separates consumers from storage implementations.
3. Room provides local persistence.
4. Firebase provides cloud persistence/authentication.
5. M3 owns physical signal collection and fusion.
6. M4 owns scan/reveal gameplay.
7. GPS + Light + Motion are the weighted fusion inputs.
8. Proximity is a separate final gate.
9. Checkpoints are dynamic.
10. R001–R006 are seed/demo identifiers only.
11. Progress is scoped to user + game + checkpoint.
12. Leaderboards are game-specific.
13. Geofences are game-scoped.
14. Navigation carries game/checkpoint IDs.
15. UI does not directly access Room/Firebase/hardware.
16. Offline discoveries are retained until synchronized.
17. Synchronization is idempotent.
18. Backend security enforces authorization.
19. Debug bypasses are disabled in final builds.
20. Physical sensor behavior must be tested on a real device.
```

---

# 96. FINAL TECHNICAL STRUCTURE

```text
app/
 ├── presentation/
 │    ├── auth/
 │    ├── player/
 │    ├── creator/
 │    ├── scan/
 │    └── leaderboard/
 │
 ├── domain/
 │    ├── model/
 │    ├── repository/
 │    └── fusion/
 │
 ├── data/
 │    ├── local/
 │    │    ├── room/
 │    │    └── dao/
 │    ├── remote/
 │    │    ├── firestore/
 │    │    └── fcm/
 │    └── repository/
 │
 ├── location/
 │    ├── provider/
 │    ├── geofence/
 │    └── distance/
 │
 ├── sensors/
 │    ├── light/
 │    ├── motion/
 │    └── proximity/
 │
 └── sync/
      ├── pending/
      └── retry/
```

Exact package names may be adapted during implementation provided responsibility boundaries remain intact.

---

# 97. FINAL DOCUMENTATION RULE

When a shared technical interface changes, update the affected documentation:

```text
Shared Contracts
TRD
Master Development Plan
Member workplan
Testing Strategy
Integration Plan
Git Workflow
```

The code and documentation must describe the same architecture.

---

# 98. FINAL TRD STATUS

This TRD is aligned with the final six-member responsibility model:

```text
M1 — Player UI & Navigation
M2 — Creator & Game Management
M3 — Location + Sensors + Fusion
M4 — Quest & Scan Gameplay
M5 — Firebase & Backend
M6 — Room + Repository + Sync + Integration
```

The technical architecture is dynamic, game-scoped, user-scoped, offline-aware, and designed around the physical discovery mechanic.

---

**END OF TRD — TECHNICAL REQUIREMENTS DOCUMENT**
