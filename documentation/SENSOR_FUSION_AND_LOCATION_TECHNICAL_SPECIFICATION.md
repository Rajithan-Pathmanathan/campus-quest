# SHARED CONTRACTS & INTEGRATION INTERFACES
## Campus Quest — Final Six-Member Architecture Contract

**Status:** Final reassignment-compatible version  
**Purpose:** Define the interfaces shared by all six members so that independently developed features integrate without duplicated logic, mismatched identifiers, or incompatible data models.

---

# 1. PURPOSE

Campus Quest is developed by six members with separate feature ownership.

The shared contract prevents:

```text
different data models
different IDs
different navigation arguments
different Firebase paths
different Room keys
duplicated sensor logic
incompatible repository methods
cross-game data leakage
```

The central rule is:

> Feature ownership may be separate; shared interfaces are common.

---

# 2. FINAL MEMBER OWNERSHIP

| Member | Responsibility |
|---|---|
| M1 | Player UI & Navigation |
| M2 | Creator & Game Management |
| M3 | Location + Sensors + Fusion |
| M4 | Quest & Scan Gameplay |
| M5 | Firebase & Backend |
| M6 | Room + Repository + Sync + Integration |

---

# 3. ARCHITECTURE

Recommended dependency direction:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firebase

M3 physical signal layer
 ↓
M4 scan state / presentation
```

The UI must not directly own persistence or hardware access.

The course architecture material describes MVVM as separating the View from ViewModel state/logic and highlights lifecycle survival and testability as benefits. fileciteturn36file0L163-L177

---

# 4. CORE DOMAIN MODEL

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

---

# 5. LIGHT SIGNATURE CONTRACT

Light signature represents expected ambient environmental light.

```kotlin
data class LightSignature(
    val minLux: Float,
    val maxLux: Float
)
```

Rules:

```text
minLux >= 0
maxLux >= 0
minLux <= maxLux
```

It must not be interpreted as light emitted by a relic.

---

# 6. ID RULES

Every entity has an ID.

```text
gameId
checkpointId
userId
```

IDs must be treated as opaque identifiers.

Do not write logic such as:

```kotlin
if (checkpointId == "R001")
```

to determine normal application behavior.

---

# 7. GAME SCOPING

Every checkpoint belongs to exactly one game:

```text
Checkpoint.gameId
```

Therefore:

```text
(gameId, checkpointId)
```

is the meaningful checkpoint identity within the application.

This prevents collisions when different games contain similarly named or numbered checkpoints.

---

# 8. USER SCOPING

Player progress must be scoped to:

```text
userId
gameId
checkpointId
```

Never assume that progress for one user is valid for another user.

---

# 9. GAME-SCOPED LEADERBOARD

Leaderboard identity:

```text
gameId
```

Firestore:

```text
leaderboards/{gameId}/entries/{uid}
```

There is no global production leaderboard.

---

# 10. FIRESTORE CONTRACT

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

# 11. GAME DOCUMENT

Example:

```json
{
  "id": "demo-campus-quest",
  "title": "Campus Quest",
  "description": "Explore the campus and discover hidden checkpoints.",
  "creatorId": "creator-demo",
  "creatorName": "Campus Quest Team",
  "status": "PUBLISHED",
  "checkpointCount": 6,
  "createdAt": 1760000000000,
  "publishedAt": 1760000100000
}
```

The Firestore document ID should remain the authoritative game ID.

---

# 12. CHECKPOINT DOCUMENT

Example:

```json
{
  "id": "R001",
  "gameId": "demo-campus-quest",
  "name": "The Starting Relic",
  "lat": 0.0,
  "lng": 0.0,
  "radiusM": 20,
  "lightSignature": {
    "minLux": 100,
    "maxLux": 500
  },
  "clue": "Begin where the journey starts.",
  "lore": "The first marker begins the expedition.",
  "order": 1,
  "motionType": "SWEEP",
  "rarity": "COMMON"
}
```

Coordinates are placeholders in this example and must be replaced with verified physical-demo coordinates.

---

# 13. MEMBERSHIP CONTRACT

A player joining a game creates:

```text
gamePlayers/{gameId}_{uid}
```

Minimum information:

```text
gameId
userId
joinedAt
```

Optional:

```text
displayName
status
```

Membership must be game-specific.

---

# 14. PROGRESS CONTRACT

A discovery is identified by:

```text
userId
gameId
checkpointId
```

Example:

```text
progress/player-demo-01/games/demo-campus-quest/checkpoints/R001
```

Recommended fields:

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

# 15. DISCOVERY IDEMPOTENCY

Repeated discovery attempts for the same:

```text
userId + gameId + checkpointId
```

must result in one logical completed checkpoint.

This is required for:

```text
geofence duplicates
retries
offline synchronization
repeated taps
network failures
```

---

# 16. REPOSITORY CONTRACT

The canonical repository boundary is:

```kotlin
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

---

# 17. REPOSITORY RESPONSIBILITY

The repository decides whether data comes from:

```text
Room
Firebase
cache
pending-sync queue
```

Consumers should not need to know the underlying storage implementation.

---

# 18. M1 REPOSITORY USAGE

M1 may request:

```text
getAvailableGames()
getGameDetails(gameId)
joinGame(gameId)
observeGameLeaderboard(gameId)
```

M1 should not call Firestore directly from player screens.

---

# 19. M2 REPOSITORY USAGE

M2 may request:

```text
createGame()
createCheckpoint()
updateCheckpoint()
publishGame()
```

M2 should not implement its own Firebase data-access layer.

---

# 20. M3 DATA CONTRACT

M3 needs:

```text
gameId
checkpointId
latitude
longitude
radiusM
lightSignature
motionType
```

M3 returns physical-signal results through stable interfaces.

---

# 21. M3 LOCATION RESULT

Recommended:

```kotlin
data class LocationSignal(
    val distanceM: Float,
    val normalizedScore: Float,
    val accuracyM: Float,
    val isInsideGeofence: Boolean
)
```

The exact normalization formula belongs to the sensor/fusion technical specification.

---

# 22. M3 LIGHT RESULT

Recommended:

```kotlin
data class LightSignal(
    val currentLux: Float,
    val normalizedScore: Float,
    val isWithinExpectedRange: Boolean
)
```

---

# 23. M3 MOTION RESULT

Recommended:

```kotlin
data class MotionSignal(
    val normalizedScore: Float,
    val sweepDetected: Boolean
)
```

---

# 24. M3 PROXIMITY RESULT

Recommended:

```kotlin
data class ProximitySignal(
    val isNear: Boolean
)
```

Proximity is separate from the weighted fusion score.

---

# 25. FUSION RESULT

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

The critical distinction is:

```text
fusionScore
```

is calculated from:

```text
GPS + Light + Motion
```

while:

```text
proximityNear
```

is a separate final gate.

---

# 26. FUSION FLOW

```text
GPS
 │
 Light
 │
 Motion
 │
 ▼
Weighted Fusion
 │
 ▼
Fusion Threshold
 │
 ▼
Proximity Final Gate
 │
 ▼
Reveal
```

---

# 27. FUSION CONTRACT RULE

Do not modify the fusion weights in UI code.

M4 may display:

```text
0–100%
```

but does not own:

```text
sensor weighting
normalization
hardware sampling
```

---

# 28. SCAN CONTRACT

M4 consumes M3's physical-signal result.

Example:

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

# 29. SCAN STATE RULE

The scan UI must not directly determine:

```text
GPS accuracy
ambient lux
accelerometer thresholds
proximity hardware values
```

Those values are provided by the physical-signal layer.

---

# 30. REVEAL CONTRACT

When:

```text
fusion threshold reached
AND
proximity final gate passed
```

M4 may transition to:

```text
Reveal
```

The reveal should contain:

```text
checkpoint name
clue/lore
success state
```

---

# 31. DISCOVERY CONTRACT

After successful discovery:

```text
M4
 ↓
Repository.recordDiscovery()
 ↓
M6 local persistence/sync
 ↓
M5 cloud persistence
```

M4 does not write directly to Room or Firestore.

---

# 32. ROOM CONTRACT

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

# 33. ROOM PRIMARY KEYS

Recommended:

```text
CheckpointEntity:
gameId + id

GamePlayerEntity:
gameId + userId

FoundCheckpointEntity:
gameId + userId + checkpointId
```

This is essential for cross-game and cross-user isolation.

---

# 34. GAME ENTITY

Example:

```kotlin
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val status: String,
    val checkpointCount: Int,
    val createdAt: Long,
    val publishedAt: Long?
)
```

---

# 35. CHECKPOINT ENTITY

Recommended:

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

# 36. FOUND CHECKPOINT ENTITY

Recommended:

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

# 37. OFFLINE CONTRACT

If network connectivity is unavailable:

```text
read cached game/checkpoint data
        ↓
perform supported local operation
        ↓
save locally
        ↓
mark pending sync
        ↓
sync when connectivity returns
```

The UI should receive an explicit state rather than silently assuming cloud success.

---

# 38. SYNC CONTRACT

For a pending discovery:

```text
Room pending record
        ↓
Repository sync
        ↓
Firestore
        ↓
successful acknowledgement
        ↓
pendingSync = false
```

---

# 39. SYNC FAILURE

If upload fails:

```text
retain local discovery
retain pending state
retry later
```

A temporary network failure must not erase a successful local discovery.

---

# 40. SYNC IDEMPOTENCY

A retry must be safe.

Repeated upload of:

```text
userId
gameId
checkpointId
```

must not create multiple logical completions.

---

# 41. FCM CONTRACT

Publishing a game triggers a notification to registered users.

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

# 42. FCM NAVIGATION

Notification tap:

```text
FCM
 ↓
extract gameId
 ↓
Player navigation
 ↓
Game Details(gameId)
```

Never hard-code:

```text
demo-campus-quest
```

as the notification destination.

---

# 43. NAVIGATION CONTRACT

Important player routes should carry IDs explicitly.

Examples:

```text
Games
GameDetails(gameId)
Join(gameId)
GameMap(gameId)
Checkpoint(gameId, checkpointId)
Scan(gameId, checkpointId)
Reveal(gameId, checkpointId)
Leaderboard(gameId)
```

---

# 44. GAME DETAILS

Required navigation argument:

```text
gameId
```

Game details should load the actual game using that ID.

---

# 45. GAME MAP

Required:

```text
gameId
```

The map loads:

```text
getGameCheckpoints(gameId)
```

It must not load a universal checkpoint list.

---

# 46. CHECKPOINT SCREEN

Required:

```text
gameId
checkpointId
```

This prevents ambiguity when two games contain checkpoints with the same ID.

---

# 47. LEADERBOARD SCREEN

Required:

```text
gameId
```

Leaderboard data must come from:

```text
observeGameLeaderboard(gameId)
```

---

# 48. CREATOR NAVIGATION

Recommended routes:

```text
CreatorHome
CreateGame
EditGame(gameId)
CheckpointList(gameId)
EditCheckpoint(gameId, checkpointId)
PublishGame(gameId)
```

---

# 49. AUTH CONTRACT

Authentication produces:

```text
uid
displayName
role
```

The `uid` is the authoritative user identity for:

```text
membership
progress
leaderboard
ownership
```

Do not use display names as primary identity.

---

# 50. CREATOR OWNERSHIP

A creator may modify a game only when:

```text
game.creatorId == authenticatedUser.uid
```

Final authorization is enforced by backend security rules.

UI checks alone are insufficient.

---

# 51. PLAYER MEMBERSHIP

Joining a game creates a membership scoped to:

```text
gameId + uid
```

A player cannot gain membership in another game through a reused local state.

---

# 52. FIRESTORE SECURITY CONTRACT

Security rules must enforce:

```text
authenticated users
creator ownership
game membership
user-specific progress
game-specific leaderboard access
```

Client-side hiding is not a security mechanism.

---

# 53. PUBLISH CONTRACT

M2 requests:

```text
publishGame(gameId)
```

M5 validates:

```text
authenticated creator
ownership
game state
required fields
checkpoint validity
```

Then changes:

```text
DRAFT → PUBLISHED
```

and sets:

```text
publishedAt
```

---

# 54. PUBLISH NOTIFICATION CONTRACT

After successful publication:

```text
Firestore status = PUBLISHED
        ↓
new-game event/notification
        ↓
FCM topic
        ↓
registered players
```

A failed notification must not undo a successful game publication.

---

# 55. GAME STATUS

Allowed:

```text
DRAFT
PUBLISHED
CLOSED
```

Typical lifecycle:

```text
DRAFT
  ↓
PUBLISHED
  ↓
CLOSED
```

---

# 56. CHECKPOINT VALIDATION

Before publishing:

```text
valid game
valid checkpoint IDs
valid coordinates
positive radius
valid light range
valid order
non-empty clue
```

Exact product validation may impose additional rules.

---

# 57. DYNAMIC CHECKPOINT RULE

The architecture must support:

```text
0..N checkpoints
```

before publication rules are applied.

It must not assume:

```text
exactly 6
```

---

# 58. SAMPLE DATA RULE

The following are demo identifiers only:

```text
R001
R002
R003
R004
R005
R006
```

Production logic must work with arbitrary IDs.

---

# 59. SECOND GAME CONTRACT TEST

Example:

```text
demo-science-trail
S001–S004
```

The same application must support both:

```text
demo-campus-quest
demo-science-trail
```

without code changes.

---

# 60. MULTI-GAME ISOLATION

A request must always carry enough scope to determine:

```text
which game
which checkpoint
which user
```

When applicable.

---

# 61. MULTI-USER ISOLATION

Local and cloud state must not allow:

```text
Player A progress
```

to appear as:

```text
Player B progress
```

---

# 62. ERROR CONTRACT

Use explicit states for:

```text
loading
success
empty
permission denied
network unavailable
authentication failure
validation failure
sensor unavailable
location unavailable
scan failure
sync failure
```

Do not represent every failure as:

```text
generic error
```

---

# 63. LIFECYCLE CONTRACT

Sensor/location resources must be managed according to lifecycle.

M3 owns:

```text
start
pause/stop
resume
cleanup
```

M4 observes scan state.

UI rotation must not create duplicate sensor listeners or geofences.

---

# 64. BACKGROUND CONTRACT

Background location/geofence behavior must follow Android permission and lifecycle requirements.

M3 owns hardware/location handling.

M1/M4 consume resulting state.

---

# 65. PERMISSION CONTRACT

Relevant permissions must be requested only when needed.

At minimum, location functionality must handle:

```text
permission granted
permission denied
permission permanently restricted
```

Sensor availability must also be checked where hardware is required.

---

# 66. SENSOR DEGRADATION

If a required signal becomes unavailable:

```text
do not fabricate a successful value
```

Instead:

```text
report unavailable/degraded state
```

The final behavior must follow the sensor-fusion technical specification.

---

# 67. DATA MAPPING CONTRACT

Mappings:

```text
Game ↔ GameEntity
Checkpoint ↔ CheckpointEntity
GamePlayer ↔ GamePlayerEntity
FoundCheckpoint ↔ FoundCheckpointEntity
```

Mappings should be centralized rather than duplicated across screens.

---

# 68. REPOSITORY RESULT CONTRACT

Use:

```kotlin
Result<T>
```

for operations where failure must be surfaced.

For streams:

```kotlin
Flow<T>
```

may be used for observable state such as leaderboards.

---

# 69. UI STATE CONTRACT

ViewModels may expose:

```kotlin
StateFlow
```

or equivalent observable state.

Typical state:

```text
Loading
Content
Empty
Error
```

Feature-specific state may extend this model.

---

# 70. NO DIRECT DATA-SOURCE RULE

Do not implement:

```kotlin
FirebaseFirestore.getInstance()
```

inside:

```text
Composable/screen
Activity
Fragment
```

when the operation belongs to the repository layer.

Likewise, Room DAOs should not be queried directly by UI.

---

# 71. NO DIRECT HARDWARE IN UI

Do not instantiate or manage:

```text
SensorManager
FusedLocationProviderClient
GeofencingClient
```

inside M4 scan UI.

M3 owns those operations.

---

# 72. TEST CONTRACT

Each interface must have deterministic tests.

Examples:

```text
Repository → fake data source
Fusion → deterministic sensor inputs
ViewModel → fake repository
Sync → fake network
Navigation → route/argument tests
```

---

# 73. INTEGRATION TEST CONTRACT

At minimum verify:

```text
Creator creates game
Creator creates checkpoint
Creator publishes
Player discovers game
Player joins
Map loads checkpoint
Geofence activates
Scan consumes sensor state
Reveal occurs after fusion + proximity
Discovery saves
Sync occurs
Leaderboard updates
```

---

# 74. SHARED CONTRACT OWNERSHIP

| Contract | Primary owner | Required reviewers |
|---|---|---|
| Game model | M2 | M5, M6 |
| Checkpoint model | M2 | M3, M5, M6 |
| Repository | M6 | M5 + affected UI members |
| Location/sensor | M3 | M4 |
| Scan state | M4 | M3 |
| Firebase paths | M5 | M6 |
| Room keys | M6 | M5 |
| Navigation | M1 | M2/M4 |
| FCM | M5 | M1 |
| Leaderboard | M5 | M1/M6 |

---

# 75. CONTRACT CHANGE PROCESS

```text
1. Identify problem.
2. Identify affected interfaces.
3. Notify affected members.
4. Propose new contract.
5. Agree on contract.
6. Update this document.
7. Update implementations.
8. Run affected tests.
9. Run integration tests.
10. Merge.
```

---

# 76. CONTRACT VERSIONING

Record meaningful interface changes.

Example:

```text
Shared Contracts v1.0
Shared Contracts v1.1 — repository addition
Shared Contracts v1.2 — navigation scope update
```

A breaking change should be explicitly marked.

---

# 77. BREAKING CHANGE EXAMPLE

Changing:

```text
Scan(checkpointId)
```

to:

```text
Scan(gameId, checkpointId)
```

is a breaking navigation contract.

Affected:

```text
M1
M3
M4
M6
```

All affected consumers must be updated before the old contract is removed.

---

# 78. FIRESTORE/ROOM CONSISTENCY

The same logical identity must remain consistent:

```text
Cloud:
gameId + checkpointId

Local:
gameId + checkpointId
```

Do not create one storage layer that identifies a checkpoint only by `checkpointId` while another requires `gameId + checkpointId`.

---

# 79. OFFLINE IDENTITY

Offline records must preserve:

```text
userId
gameId
checkpointId
```

A queued operation without sufficient identity must not be synchronized.

---

# 80. SYNC CONFLICT RULE

If cloud and local state conflict:

```text
do not silently overwrite data
```

Apply the approved synchronization policy.

For discoveries, idempotent completion semantics should ensure that a repeated successful discovery remains one logical discovery.

---

# 81. LEADERBOARD CONTRACT

The leaderboard consumes game-scoped progress.

Required:

```text
gameId
userId
score/ranking fields
```

M5 owns cloud persistence/calculation.

M1 owns presentation.

M6 supports local/offline repository behavior where required.

---

# 82. CREATOR DRAFT CONTRACT

Drafts may exist locally and/or in Firestore according to repository policy.

A draft must contain:

```text
creatorId
gameId
game fields
checkpoint configuration
status = DRAFT
```

A draft must not be visible as a published player game.

---

# 83. PUBLISHED GAME DISCOVERY

Player game browsing should return only games appropriate for discovery, normally:

```text
status = PUBLISHED
```

Closed/draft games should not appear as active discoverable games.

---

# 84. CLOSED GAME

When a game is closed:

```text
status = CLOSED
```

The final product rules determine whether:

```text
existing players may view progress
new players may join
leaderboard remains visible
```

Those policies should be implemented consistently across M1, M5, and M6.

---

# 85. ASSET CONTRACT

UI assets should be referenced by stable resource names.

Avoid hard-coded absolute filesystem paths.

M1 owns player UI assets.

M2 owns creator UI assets.

M4 owns scan/reveal visual states.

---

# 86. UI/UX CONTRACT

All feature screens should follow the approved UI/UX specification.

Maintain:

```text
consistent navigation
responsive layouts
safe-area handling
readable typography
usable touch targets
loading/error/empty states
```

The course UI/UX materials emphasize responsive layouts and appropriate touch targets as part of usable mobile interfaces. fileciteturn36file0L209-L235

---

# 87. INTEGRATION HANDOFF

A feature is ready for another member when it provides:

```text
stable interface
sample/mock data
expected inputs
expected outputs
error states
test evidence
```

---

# 88. M3 → M4 HANDOFF

M3 provides:

```text
location signal
light signal
motion signal
proximity signal
fusion result
availability/degradation state
```

M4 provides:

```text
scan presentation
state transitions
success/failure/reveal behavior
```

---

# 89. M5 → M6 HANDOFF

M5 provides:

```text
Firestore structure
repository cloud implementation
security rules
FCM integration
cloud errors
```

M6 integrates:

```text
Room
cache
pending sync
repository orchestration
```

---

# 90. M6 → M1/M2/M4 HANDOFF

M6 provides:

```text
repository API
observable state
offline behavior
sync state
```

Feature members consume these interfaces rather than directly accessing Room.

---

# 91. M2 → M3 HANDOFF

M2 creates checkpoint configuration:

```text
coordinates
radius
light signature
motion type
```

M3 consumes the saved checkpoint configuration.

M3 does not assume that coordinates are hard-coded.

---

# 92. M1 → M4 HANDOFF

M1 owns navigation and player-facing route integration.

M4 owns scan/reveal screens.

Shared route:

```text
gameId
checkpointId
```

must remain consistent.

---

# 93. INTEGRATION ORDER

Recommended:

```text
Shared contracts
 ↓
M6 foundation
 ↓
M5 cloud
 ↓
M1 navigation
 ↓
M2 creator
 ↓
M3 physical pipeline
 ↓
M4 scan
 ↓
M6 sync
 ↓
FCM/deep links
 ↓
full E2E
```

---

# 94. CONTRACT TEST MATRIX

| Contract | Test |
|---|---|
| Game | create/read/update |
| Checkpoint | create/read/update/delete |
| IDs | arbitrary IDs |
| Navigation | correct game/checkpoint args |
| Repository | fake data source |
| Firebase | security + path |
| Room | composite key isolation |
| Fusion | deterministic scores |
| Proximity | separate final gate |
| Sync | retry/idempotency |
| FCM | correct gameId deep link |
| Leaderboard | game isolation |

---

# 95. FINAL INTEGRATION GOLDEN PATH

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
Game Details(gameId)
 ↓
Join
 ↓
Game Map(gameId)
 ↓
Dynamic checkpoint
 ↓
Geofence ENTER
 ↓
Scan(gameId, checkpointId)
 ↓
GPS + Light + Motion
 ↓
Fusion threshold
 ↓
Proximity final gate
 ↓
Reveal
 ↓
recordDiscovery(gameId, checkpointId)
 ↓
Room
 ↓
Firebase sync
 ↓
leaderboard(gameId)
```

---

# 96. FINAL NON-NEGOTIABLE CONTRACTS

```text
1. Game is dynamic.
2. Checkpoints belong to games.
3. Progress belongs to user + game + checkpoint.
4. Leaderboards belong to games.
5. R001–R006 are demo data only.
6. Repository hides persistence details.
7. M3 owns physical signal collection and fusion.
8. M4 owns scan/reveal gameplay.
9. Fusion = GPS + Light + Motion.
10. Proximity is a separate final gate.
11. M5 owns Firebase/cloud security/FCM.
12. M6 owns Room/repository/sync/integration.
13. UI does not directly access Firebase, Room, or hardware.
14. Navigation carries explicit game/checkpoint scope.
15. Offline discoveries are retained locally until synchronized.
16. Repeated synchronization is idempotent.
17. No global leaderboard.
18. No silent shared-contract changes.
```

---

# 97. FINAL CONTRACT STATUS

This document is the common implementation reference for:

```text
M1
M2
M3
M4
M5
M6
```

When a member encounters an interface not covered here, the interface should be agreed and documented before independent implementations diverge.

---

**END OF SHARED CONTRACTS & INTEGRATION INTERFACES**
