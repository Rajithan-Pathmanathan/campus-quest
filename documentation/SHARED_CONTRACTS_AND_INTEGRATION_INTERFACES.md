# SHARED CONTRACTS & INTEGRATION INTERFACES
## Campus Quest — Final Six-Member Architecture

**Purpose:** Single shared contract for M1–M6 implementation  
**Status:** Final reassignment version  
**Architecture:** Android + Kotlin, MVVM + Repository, Room + Firebase/Firestore  
**Primary integration principle:** UI → ViewModel → Repository → Local/Remote data

---

# 1. DOCUMENT PURPOSE

This document defines the interfaces that all six members must implement against.

The objective is to prevent different members from independently creating incompatible:

- Data models.
- IDs.
- Repository methods.
- Navigation arguments.
- Sensor states.
- Firestore paths.
- Room entities.
- Sync semantics.
- Notification payloads.
- Progress/leaderboard structures.

The Mobile Application Development architecture material emphasizes separation of concerns: the UI displays data and forwards actions, the ViewModel coordinates UI state and logic, and the data/repository layer handles where data comes from, such as a local database, network, or cache. fileciteturn32file1L581-L586

The same material identifies MVVM as a structure where the View observes ViewModel data while the ViewModel exposes UI-related state without directly referencing the View. fileciteturn32file1L495-L509

Campus Quest therefore uses these boundaries throughout the team implementation.

---

# 2. FINAL MEMBER OWNERSHIP

| Member | Primary responsibility |
|---|---|
| M1 | Player UI & Navigation |
| M2 | Creator & Game Management |
| M3 | Location, Sensors & Fusion |
| M4 | Quest & Scan Gameplay |
| M5 | Firebase & Backend |
| M6 | Room, Repository, Sync & Integration |

---

# 3. ARCHITECTURAL LAYERS

The shared architecture is:

```text
Presentation/UI
      ↓
ViewModel
      ↓
Repository / Use Case boundary
      ↓
┌───────────────────────┐
│ Local + Remote data   │
│                       │
│ Room                  │
│ Firebase/Firestore    │
└───────────────────────┘
```

Hardware-dependent gameplay follows:

```text
Repository / Checkpoint data
          ↓
M3 Location + Sensors + Fusion
          ↓
M4 Quest / Scan Gameplay
```

The architecture keeps implementation details isolated so one layer can be changed without requiring unrelated layers to know how it works. This matches the course material's separation-of-concerns guidance. fileciteturn32file1L581-L586

---

# 4. CORE ARCHITECTURE RULE

No member should bypass an ownership boundary merely because direct access is technically easier.

Do not create:

```text
M4 → SensorManager
M4 → Firestore
M4 → Room
M2 → Firestore directly
M1 → Room directly
M3 → Firestore directly
```

Prefer:

```text
UI
 ↓
ViewModel
 ↓
Repository / shared contract
 ↓
Data source
```

The course material specifically describes the UI as displaying/forwarding, the ViewModel as coordinating, and the repository/data layer as hiding whether data comes from local database, network, or cache. fileciteturn32file1L581-L586

---

# 5. DOMAIN MODEL — GAME

Canonical model:

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

All members must use the same logical meaning for these fields.

---

# 6. GAME FIELD CONTRACT

| Field | Meaning | Owner |
|---|---|---|
| `id` | Unique game identity | M5/M6 |
| `title` | Game title | M2 |
| `description` | Game description | M2 |
| `creatorId` | Authenticated creator UID | M5 |
| `creatorName` | Creator display name | M5/M2 |
| `status` | Lifecycle state | M5 |
| `checkpointCount` | Current checkpoint count | M2/M5/M6 |
| `createdAt` | Creation timestamp | M5 |
| `publishedAt` | Publication timestamp | M5 |

---

# 7. GAME STATUS CONTRACT

Allowed states:

```text
DRAFT
PUBLISHED
CLOSED
```

Canonical lifecycle:

```text
DRAFT
  ↓
PUBLISHED
  ↓
CLOSED
```

The application must not assume arbitrary transitions.

Backend/security validation is authoritative.

---

# 8. DOMAIN MODEL — CHECKPOINT

Canonical model:

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

---

# 9. CHECKPOINT IDENTITY

A checkpoint is identified logically by:

```text
gameId + checkpointId
```

This means:

```text
Game A / CP001
```

and:

```text
Game B / CP001
```

are different checkpoints.

No member may assume checkpoint IDs are globally unique unless the implementation explicitly guarantees that.

---

# 10. CHECKPOINT FIELD CONTRACT

| Field | Meaning | Primary owner |
|---|---|---|
| `id` | Checkpoint identity | M5/M6 |
| `gameId` | Parent game | M2/M5/M6 |
| `name` | Checkpoint name | M2 |
| `lat` | Latitude | M2/M3 |
| `lng` | Longitude | M2/M3 |
| `radiusM` | Geofence radius | M2/M3 |
| `lightSignature` | Expected ambient-light range | M2/M3 |
| `clue` | Player clue | M2 |
| `lore` | Narrative content | M2 |
| `order` | Gameplay sequence | M2/M5/M6 |
| `motionType` | Expected movement type | M2/M3 |
| `rarity` | Gameplay metadata | M2/M4 |

---

# 11. LIGHT SIGNATURE CONTRACT

The expected light signature represents **ambient environmental light**.

It is not light emitted by a relic.

Conceptually:

```kotlin
data class LightSignature(
    val minLux: Float,
    val maxLux: Float
)
```

Required relationship:

```text
minLux >= 0
maxLux >= 0
minLux <= maxLux
```

M2 configures it.

M3 measures and evaluates it.

M4 displays its gameplay state.

M5/M6 persist it.

---

# 12. MOTION CONTRACT

Canonical default:

```text
SWEEP
```

The motion type is configuration data.

M2 allows the creator to configure it.

M3 interprets accelerometer data.

M4 presents the player instruction/status.

---

# 13. RARITY CONTRACT

Rarity is checkpoint metadata.

The accepted values must be controlled by the shared implementation.

Do not allow M2 and M4 to invent different sets.

Example conceptual values:

```text
COMMON
RARE
EPIC
LEGENDARY
```

Only values approved by the final implementation should be used.

---

# 14. GAME REPOSITORY CONTRACT

Canonical shared interface:

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

The interface can evolve only through team-wide agreement.

---

# 15. REPOSITORY OWNERSHIP

M2, M4 and M1 consume the repository.

M5 provides the cloud implementation.

M6 provides the local/repository/sync orchestration.

No UI member should need to know whether an operation is implemented with:

```text
Room
Firestore
both
```

---

# 16. REPOSITORY METHOD OWNERSHIP

| Method | Main consumers | Main implementation |
|---|---|---|
| `getAvailableGames()` | M1 | M5/M6 |
| `getGameDetails()` | M1/M4 | M5/M6 |
| `createGame()` | M2 | M5/M6 |
| `createCheckpoint()` | M2 | M5/M6 |
| `updateCheckpoint()` | M2 | M5/M6 |
| `publishGame()` | M2 | M5 |
| `joinGame()` | M1 | M5/M6 |
| `getGameCheckpoints()` | M3/M4 | M5/M6 |
| `getFusionSignature()` | M3 | M5/M6 |
| `recordDiscovery()` | M4 | M5/M6 |
| `observeGameLeaderboard()` | M1/M4 | M5/M6 |
| `syncPending()` | M6 | M6 |

---

# 17. AUTHENTICATION CONTRACT

M5 owns Firebase Authentication.

Shared logical user:

```kotlin
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?
)
```

The authenticated UID is the identity used for:

```text
creatorId
userId
membership
progress
leaderboard entry
```

---

# 18. AUTH STATE

Suggested contract:

```kotlin
interface AuthRepository {
    fun observeAuthState(): Flow<AuthUser?>

    suspend fun signIn(
        ...
    ): Result<AuthUser>

    suspend fun signOut(): Result<Unit>
}
```

The exact authentication method can follow the project's chosen Firebase Auth setup.

---

# 19. CREATOR OWNERSHIP CONTRACT

For creator operations:

```text
authenticated UID == game.creatorId
```

This must be enforced by the backend/security layer.

M2 should not rely on a creator-entered ID.

---

# 20. PLAYER MEMBERSHIP CONTRACT

A player joining a game creates a logical membership:

```text
gameId + userId
```

Cloud representation:

```text
gamePlayers/{gameId}_{uid}
```

Local representation:

```text
GamePlayerEntity
primary key = gameId + userId
```

---

# 21. MEMBERSHIP IDEMPOTENCY

Calling:

```kotlin
joinGame(gameId)
```

multiple times must not create duplicate logical memberships.

---

# 22. PROGRESS CONTRACT

Cloud logical path:

```text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

Local identity:

```text
userId + gameId + checkpointId
```

This is the canonical discovery scope.

---

# 23. DISCOVERY CONTRACT

Discovery operation:

```kotlin
recordDiscovery(
    gameId = ...,
    checkpointId = ...,
    foundAt = ...
)
```

The authenticated user is obtained from the auth context.

The caller should not be allowed to record progress for another user.

---

# 24. DISCOVERY IDEMPOTENCY

Repeated requests for:

```text
same user
same game
same checkpoint
```

must produce one logical discovery.

This protects against:

- Sensor event duplication.
- UI recreation.
- Network retry.
- Offline sync retry.

---

# 25. LEADERBOARD CONTRACT

There is **no global leaderboard**.

Canonical cloud structure:

```text
leaderboards/{gameId}/entries/{uid}
```

Conceptual model:

```kotlin
data class GameLeaderboardEntry(
    val userId: String,
    val displayName: String,
    val score: Int,
    val foundCount: Int,
    val lastUpdatedAt: Long
)
```

The exact fields must match the final implementation.

---

# 26. LEADERBOARD SCOPE

Every leaderboard query must contain:

```text
gameId
```

Therefore:

```text
Game A leaderboard
```

cannot contain progress from:

```text
Game B
```

---

# 27. FIRESTORE PATH CONTRACT

Canonical logical paths:

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

# 28. ROOM TABLE CONTRACT

Target local tables:

```text
games
checkpoints
game_players
found_checkpoints
pending_sync
```

Recommended logical identities:

```text
games:
    id

checkpoints:
    gameId + id

game_players:
    gameId + userId

found_checkpoints:
    gameId + userId + checkpointId

pending_sync:
    operationId
```

---

# 29. ROOM CHECKPOINT KEY

This is mandatory for correct multi-game isolation:

```text
PRIMARY KEY(gameId, id)
```

Do not use only:

```text
PRIMARY KEY(id)
```

unless the project explicitly guarantees global checkpoint IDs.

---

# 30. ROOM PROGRESS KEY

Recommended:

```text
PRIMARY KEY(gameId, userId, checkpointId)
```

This ensures one logical discovery per user/game/checkpoint.

---

# 31. OFFLINE CONTRACT

The application should follow this logical model where offline operation is supported:

```text
Read:
Room → UI
       ↓
optional Firebase refresh

Write:
UI
 ↓
Repository
 ↓
Room
 ↓
Pending Sync
 ↓
Firebase
```

The exact operation-specific offline behavior is defined below.

---

# 32. OFFLINE GAME READ

Previously available game data may be served from Room.

If network is available:

```text
Firebase
   ↓
Room refresh
   ↓
UI
```

If network is unavailable:

```text
Room
   ↓
UI
```

---

# 33. OFFLINE CHECKPOINT READ

Joined-game checkpoint configuration should be cached locally when required for offline gameplay.

Required configuration includes:

```text
lat
lng
radiusM
minLux
maxLux
motionType
order
```

This allows M3 to operate from cached configuration.

---

# 34. OFFLINE DISCOVERY

If the final MVP supports offline discovery:

```text
M4
 ↓
recordDiscovery()
 ↓
Room transaction
 ├─ FoundCheckpoint
 └─ PendingSync
 ↓
Reveal
```

Then:

```text
network restored
 ↓
syncPending()
 ↓
Firebase
 ↓
mark synced
```

---

# 35. OFFLINE CREATOR DRAFT

If offline creator drafting is included:

```text
M2
 ↓
Repository
 ↓
Room
 ↓
Draft
 ↓
Pending Sync
```

Publication should follow the agreed online/backend validation rules.

Do not allow an offline publish operation merely because local draft saving works.

---

# 36. SYNC CONTRACT

M6 owns synchronization.

Required properties:

```text
retryable
idempotent
game-scoped
user-scoped
observable
recoverable
```

---

# 37. PENDING OPERATION CONTRACT

Conceptual:

```kotlin
data class PendingSyncOperation(
    val operationId: String,
    val userId: String,
    val gameId: String?,
    val checkpointId: String?,
    val operationType: String,
    val createdAt: Long,
    val retryCount: Int,
    val lastAttemptAt: Long?,
    val status: String
)
```

The actual implementation may use a sealed operation model or normalized table.

---

# 38. OPERATION ID

An operation ID must remain stable across retries.

Do not generate a new operation ID for every attempt.

Example:

```text
operation-123
    attempt 1
    attempt 2
    attempt 3
```

not:

```text
operation-123
operation-124
operation-125
```

for the same logical operation.

---

# 39. SYNC STATES

Recommended conceptual states:

```text
PENDING
SYNCING
SYNCED
RETRY
FAILED_PERMANENT
```

The final enum/string values may differ.

---

# 40. RETRY RULE

Transient failures should remain retryable.

Permanent failures should not enter an infinite retry loop.

Examples of likely permanent failures:

```text
permission denied
invalid data
unauthorized
deleted resource
```

---

# 41. SENSOR/FUSION CONTRACT

M3 owns physical signal collection.

M4 consumes its result.

The core signal inputs are:

```text
GPS
Light
Motion
```

These are normalized into a common range.

---

# 42. NORMALIZED SENSOR SCORES

Each fusion input is conceptually:

```text
GPS score    ∈ [0,1]
Light score  ∈ [0,1]
Motion score ∈ [0,1]
```

M3 owns the actual normalization.

M4 only displays the resulting values.

---

# 43. FUSION CONTRACT

Canonical:

```text
GPS
 +
Light
 +
Motion
 ↓
Weighted Fusion
 ↓
Fusion Threshold
```

M3 owns:

- Weights.
- Normalization.
- Fusion calculation.
- Threshold evaluation.

M4 owns:

- Meter presentation.
- State presentation.
- Gameplay transitions based on M3's result.

---

# 44. PROXIMITY CONTRACT

Proximity is **not** a fourth weighted fusion input.

Canonical flow:

```text
GPS + Light + Motion
        ↓
Weighted Fusion
        ↓
Fusion Threshold
        ↓
Proximity Final Gate
        ↓
Discovery Eligible
```

This distinction must remain unchanged.

---

# 45. DISCOVERY SIGNAL STATE

Suggested shared state:

```kotlin
data class DiscoverySignalState(
    val gameId: String,
    val checkpointId: String,
    val distanceM: Float? = null,
    val gpsScore: Float = 0f,
    val lightScore: Float = 0f,
    val motionScore: Float = 0f,
    val fusionScore: Float = 0f,
    val fusionThresholdReached: Boolean = false,
    val proximityState: ProximityState = ProximityState.UNKNOWN,
    val finalGatePassed: Boolean = false
)
```

Additional availability/error fields may be included.

---

# 46. PROXIMITY STATE

Suggested:

```kotlin
enum class ProximityState {
    UNKNOWN,
    FAR,
    NEAR,
    UNAVAILABLE
}
```

M3 maps device-specific sensor behavior into this contract.

---

# 47. M3 → M4 CONTRACT

M3 provides:

```text
gameId
checkpointId
distance
GPS state/score
light state/score
motion state/score
fusion score
fusion threshold
proximity state
final gate
sensor/location availability
errors
```

M4 must not independently calculate these values.

---

# 48. M4 → REPOSITORY CONTRACT

After final physical confirmation:

```kotlin
recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
```

M4 does not directly write Room or Firebase.

---

# 49. M2 → M3 CONTRACT

M2 provides checkpoint configuration:

```text
latitude
longitude
radiusM
lightSignature
motionType
```

M3 consumes it.

---

# 50. M3 → M2 CONTRACT

If M3 has constraints for creator configuration, they must be documented.

Examples:

```text
valid coordinate range
valid radius range
valid lux range
supported motion types
```

M2 uses these for validation.

---

# 51. NAVIGATION CONTRACT

Navigation arguments must use stable IDs.

Core arguments:

```text
gameId
checkpointId
```

Do not pass an entire mutable game/checkpoint object through navigation unless the architecture explicitly requires it.

---

# 52. PLAYER NAVIGATION

Conceptual:

```text
player/games

player/game/{gameId}

player/game/{gameId}/checkpoint/{checkpointId}/scan

player/game/{gameId}/checkpoint/{checkpointId}/reveal
```

The exact route syntax is implementation-specific.

---

# 53. CREATOR NAVIGATION

Conceptual:

```text
creator/games

creator/game/new

creator/game/{gameId}

creator/game/{gameId}/checkpoint/new

creator/game/{gameId}/checkpoint/{checkpointId}/edit
```

M1 integrates navigation into the shared app shell.

M2 owns creator screen behavior.

---

# 54. NOTIFICATION CONTRACT

Canonical payload:

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

# 55. FCM TOPIC

MVP topic:

```text
/topics/new_games
```

Publication flow:

```text
M2 Publish UI
      ↓
M5 publishGame()
      ↓
Firestore status = PUBLISHED
      ↓
FCM notification event
      ↓
/topics/new_games
```

---

# 56. NOTIFICATION DEEP LINK

The notification must contain:

```text
gameId
```

so M1 can navigate to:

```text
Game Details(gameId)
```

M5 owns payload generation.

M1 owns navigation.

---

# 57. UI STATE CONTRACT

ViewModels should expose explicit state.

Examples:

```text
Loading
Success
Error
```

For save/publish:

```text
Idle
Saving
Success
Error
```

For scan:

```text
Idle
Scanning
Fusion Ready
Waiting for Proximity
Success
Reveal
Error
```

The architecture course material highlights UI state and persistent state as separate concerns and notes that mobile lifecycle events can otherwise cause state loss. fileciteturn32file1L565-L577

---

# 58. VIEWMODEL CONTRACT

A ViewModel:

- Owns UI-related state.
- Coordinates actions.
- Observes repository/signal data.
- Survives configuration changes.
- Does not directly reference UI views.

This follows the course material's MVVM definition and rationale. fileciteturn32file1L495-L509

---

# 59. VIEW CONTRACT

UI screens:

- Display ViewModel state.
- Forward user actions.
- Trigger navigation according to the navigation contract.
- Do not implement repository/database/sensor logic.

---

# 60. ROOM CONTRACT

M6 owns:

```text
RoomDatabase
DAOs
Entities
Migrations
Local queries
Local transactions
```

Other members consume these through the repository.

---

# 61. FIREBASE CONTRACT

M5 owns:

```text
Firebase Auth
Firestore
Security Rules
FCM
Cloud validation
Cloud leaderboard
```

Other members consume these through repository/data contracts.

---

# 62. LOCATION CONTRACT

M3 owns:

```text
Fused Location Provider
Location permission
Location updates
Distance
Geofencing
```

M2 provides configuration.

M4 consumes results.

---

# 63. SENSOR CONTRACT

M3 owns:

```text
Light
Accelerometer
Proximity
Motion detection
Light matching
Fusion
```

M4 consumes the resulting state.

---

# 64. CREATOR CONTRACT

M2 owns:

```text
Game creation
Game editing
Checkpoint creation
Checkpoint editing
Checkpoint deletion
Checkpoint ordering
Drafts
Publish UI
Creator validation
```

M5 owns server-side enforcement.

---

# 65. PLAYER UI CONTRACT

M1 owns:

```text
Game discovery
Game details presentation
Join UI
Player navigation
Leaderboard presentation
Notification deep links
Shared UI components
```

M4 owns the actual scan gameplay screen.

---

# 66. QUEST/SCAN CONTRACT

M4 owns:

```text
Scan HUD
Fusion meter presentation
Sensor status presentation
Scan state machine
Proximity-gate presentation
Reveal
Discovery-recording trigger
```

M3 owns the physical calculations.

---

# 67. LEADERBOARD UI CONTRACT

M1 owns presentation.

M5 owns cloud data.

M6 owns repository/local integration.

M4 may provide navigation/refresh context after discovery.

---

# 68. ERROR CONTRACT

Recommended domain-level categories:

```kotlin
sealed interface AppError {
    data object Unauthenticated : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object NetworkUnavailable : AppError
    data object ValidationFailed : AppError
    data object Conflict : AppError
    data object SensorUnavailable : AppError
    data object LocationUnavailable : AppError
    data object SyncFailed : AppError
    data object Unknown : AppError
}
```

The final implementation can use another sealed hierarchy.

---

# 69. ERROR MAPPING

Infrastructure errors should be mapped before reaching UI.

Example:

```text
FirebaseException
      ↓
Repository error
      ↓
ViewModel UI state
      ↓
User-facing message
```

Do not expose raw Firebase/Room exceptions throughout the UI.

---

# 70. MULTI-GAME CONTRACT

Every game-specific operation must preserve:

```text
gameId
```

This applies to:

- Checkpoints.
- Membership.
- Progress.
- Leaderboard.
- Geofences.
- Scan state.
- Cached data.
- Sync operations.

---

# 71. MULTI-USER CONTRACT

Every user-specific operation must preserve:

```text
userId
```

This applies to:

- Membership.
- Progress.
- Leaderboard entries.
- Pending sync operations.
- Local discovery state.

---

# 72. NO GLOBAL PROGRESS

Do not create a single:

```text
foundCount
```

that represents progress across all games.

Progress is game-specific.

---

# 73. NO GLOBAL LEADERBOARD

Do not create:

```text
global leaderboard
```

as the production architecture.

Every game has its own leaderboard.

---

# 74. NO FIXED CHECKPOINT ARCHITECTURE

Do not implement gameplay around:

```text
R001
R002
R003
R004
R005
R006
```

These are seed/demo identifiers only.

Production data is:

```text
Game
  ↓
dynamic Checkpoint[]
```

---

# 75. SEED DATA CONTRACT

Primary demo game:

```text
demo-campus-quest
```

Sample checkpoints:

```text
R001
R002
R003
R004
R005
R006
```

These must be treated as seed/demo data.

Tests must also use arbitrary IDs.

---

# 76. SECOND GAME TEST CONTRACT

Use a second demo/test game such as:

```text
demo-science-trail
```

to validate:

```text
game isolation
checkpoint isolation
progress isolation
leaderboard isolation
```

---

# 77. DYNAMIC CHECKPOINT TEST

The application must support:

```text
Game A → 2 checkpoints
Game B → 8 checkpoints
```

without code changes.

---

# 78. DATA FLOW — CREATOR

```text
M2 Creator UI
      ↓
M2 ViewModel
      ↓
GameRepository
      ↓
M6 local / M5 cloud
      ↓
Game + Checkpoints
      ↓
Draft
```

Publishing:

```text
M2 Publish
      ↓
GameRepository.publishGame()
      ↓
M5 validation
      ↓
PUBLISHED
      ↓
FCM
```

---

# 79. DATA FLOW — PLAYER

```text
M1 Games UI
      ↓
GameRepository
      ↓
Published Games
      ↓
Game Details
      ↓
Join
      ↓
Checkpoint cache
      ↓
Game Map
```

---

# 80. DATA FLOW — PHYSICAL DISCOVERY

```text
Checkpoint configuration
        ↓
M3
Location + Light + Motion
        ↓
Weighted Fusion
        ↓
Fusion Threshold
        ↓
Proximity Final Gate
        ↓
M4
Scan Success
        ↓
recordDiscovery()
        ↓
M6 Room
        ↓
M5 Firebase
        ↓
Game Leaderboard
```

---

# 81. LIFECYCLE CONTRACT

Android lifecycle events must not cause permanent loss of important state.

The architecture course material specifically notes that Android activities can be destroyed/recreated and that state which must survive should not be held only by the Activity. ViewModel is designed to survive configuration changes. fileciteturn32file1L546-L549

Therefore:

```text
UI transient state
   ↓
ViewModel

Persistent data
   ↓
Room/Firebase
```

---

# 82. SENSOR LIFECYCLE

M3 controls sensor registration.

M4 indicates when scan gameplay is active through the agreed contract.

Sensors should not remain active unnecessarily after scan completion.

---

# 83. LOCATION LIFECYCLE

M3 controls location updates/geofences.

M4 should not manually register or unregister the location provider.

---

# 84. DATABASE LIFECYCLE

M6 owns Room lifecycle.

ViewModels must not create database instances directly.

---

# 85. FIREBASE LIFECYCLE

M5 owns Firebase initialization/data-source lifecycle.

UI members must not initialize Firebase services independently.

---

# 86. TRANSACTION CONTRACT

Where local operations must be atomic, M6 should use Room transactions.

Example:

```text
Discovery
  +
PendingSync
```

must be created together when both are required.

---

# 87. SYNC TRANSACTION CONTRACT

A cloud success should update local sync state consistently.

Conceptually:

```text
Cloud success
      ↓
mark operation synced
      ↓
remove/complete pending operation
```

This should not leave an operation indefinitely marked pending after confirmed success.

---

# 88. CLOUD RETRY CONTRACT

M5 cloud operations must be safe for M6 to retry where the operation is logically retryable.

Examples:

```text
join
discovery
checkpoint update
```

---

# 89. PUBLISH CONTRACT

M2 performs:

```text
client validation
```

M5 performs:

```text
server/cloud validation
```

M6 may persist draft state locally.

Final publication authority belongs to the cloud layer.

---

# 90. PUBLISH NOTIFICATION CONTRACT

Publishing and notification are related but separate outcomes:

```text
Game published
```

is the authoritative game state.

```text
Notification delivered
```

is a secondary delivery result.

A notification problem must not falsely change a successfully published game back to draft.

---

# 91. DELETE CONTRACT

Creator deletion of a checkpoint:

```text
M2 confirmation
      ↓
Repository delete/update
      ↓
M5/M6 persistence
```

Published-game destructive behavior must follow the final product lifecycle rules.

---

# 92. UPDATE CONTRACT

Editing an existing checkpoint must preserve:

```text
checkpointId
gameId
```

Only the changed fields should change.

An edit must not accidentally create a second checkpoint.

---

# 93. ORDER CONTRACT

Checkpoint sequence is controlled by:

```text
order
```

Do not infer sequence from:

```text
creation time
document ID
alphabetical name
```

unless explicitly defined by the product.

---

# 94. NAVIGATION STATE CONTRACT

Pass:

```text
gameId
checkpointId
```

and reload authoritative/current data through the repository where appropriate.

Do not rely on a stale object passed through multiple screens as the only source of truth.

---

# 95. TESTING CONTRACT

Every shared contract must have tests at the appropriate layer.

Examples:

```text
Domain/model
Repository
Room DAO
Firebase
Security rules
Sensor/fusion
ViewModel
Navigation
End-to-end
```

---

# 96. CROSS-MEMBER TEST RESPONSIBILITY

| Test area | Primary owner | Supporting |
|---|---|---|
| Player UI | M1 | M4 |
| Creator UI | M2 | M1/M6 |
| Location | M3 | M6 |
| Sensors/fusion | M3 | M4 |
| Scan UI | M4 | M3 |
| Firebase | M5 | M6 |
| Room | M6 | M5 |
| Sync | M6 | M5 |
| End-to-end | M6 | M1–M5 |
| Security | M5 | M6 |

---

# 97. CONTRACT CHANGE PROCESS

Before changing a shared contract:

1. Identify affected members.
2. Explain why the change is required.
3. Agree on the new contract.
4. Update this document.
5. Update affected implementations.
6. Update tests.
7. Integrate.
8. Verify end-to-end behavior.

---

# 98. PROHIBITED SILENT CHANGES

Do not silently change:

```text
Game fields
Checkpoint fields
repository method signatures
game/checkpoint identity
Firestore paths
Room primary keys
fusion architecture
proximity gate behavior
FCM payload
navigation arguments
```

---

# 99. DOCUMENTATION SOURCE PRIORITY

When resolving an implementation disagreement:

```text
1. Final product/technical source of truth
2. Shared contracts
3. Integration decisions agreed by team
4. Member workplan
5. Existing implementation
```

Do not preserve obsolete code merely because it already exists.

---

# 100. COURSE ARCHITECTURE ALIGNMENT

The course material states that software architecture organizes components and interactions so the system remains maintainable, testable, and extensible. fileciteturn32file1L469-L473

The course also describes MVVM as improving testability and lifecycle handling through an independent ViewModel. fileciteturn32file1L495-L509

Campus Quest therefore keeps:

```text
UI
ViewModel
Repository
Data sources
```

separate.

---

# 101. UI/UX INTEGRATION RULE

The course UI/UX material states that the design process should move through wireframing, prototyping, and usability testing, and that the Figma prototype is used to review navigation before implementation. fileciteturn32file0L152-L174 fileciteturn32file0L209-L235

Therefore, implementation members should follow the approved Campus Quest UI/UX design rather than independently redesigning screens during coding.

---

# 102. RESPONSIVE UI CONTRACT

The course UI/UX material requires support for different orientations/sizes and emphasizes safe areas and reachable primary actions. fileciteturn32file0L243-L265

The implementation should therefore account for:

```text
phone sizes
orientation
system bars
keyboard
touch reachability
```

---

# 103. ACCESSIBILITY CONTRACT

The course material specifies:

```text
48dp × 48dp minimum touch targets
body text no smaller than 12sp
4.5:1 text/background contrast
```

for the referenced Android UI guidance. fileciteturn32file0L303-L311

M1/M2/M4 must apply these principles to their UI.

---

# 104. FINAL INTERFACE MAP

```text
                     AUTH
                      │
                      ▼
                 ┌─────────┐
                 │   M1    │
                 │ Player  │
                 └────┬────┘
                      │
                      ▼
                 Repository
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
        M6 Room                 M5 Firebase
          │                       │
          └───────────┬───────────┘
                      │
                Shared Domain
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
        M2 Creator              M3 Physical
                                  Signals
                                    │
                                    ▼
                                  M4 Scan
```

---

# 105. FINAL MEMBER BOUNDARY TABLE

| Capability | M1 | M2 | M3 | M4 | M5 | M6 |
|---|---|---|---|---|---|---|
| Player UI | Lead | | | Support | | |
| Navigation | Lead | Support | | Support | | |
| Creator UI | | Lead | | | | |
| Game creation | | Lead | | | Support | Support |
| Checkpoint config | | Lead | Support | | | Support |
| Maps | | | Lead | | | |
| Location | | | Lead | | | |
| Geofencing | | | Lead | | | |
| Light sensor | | | Lead | | | |
| Accelerometer | | | Lead | | | |
| Proximity | | | Lead | | | |
| Fusion | | | Lead | | | |
| Scan UI | | | Support | Lead | | |
| Reveal | | | | Lead | | |
| Auth | | | | | Lead | |
| Firestore | | | | | Lead | Support |
| FCM | Support | | | | Lead | |
| Room | | | | | | Lead |
| Repository | | | | | Support | Lead |
| Offline | | | | | | Lead |
| Sync | | | | | Support | Lead |
| Leaderboard backend | | | | | Lead | Support |
| Leaderboard UI | Lead | | | Support | | |
| Integration | Support | Support | Support | Support | Support | Lead |

---

# 106. FINAL END-TO-END CONTRACT

The complete system must support:

```text
Creator
  ↓
Create Game
  ↓
Add Dynamic Checkpoints
  ↓
Save Draft
  ↓
Publish
  ↓
Firebase
  ↓
FCM New Game Notification
  ↓
Player
  ↓
Game Details
  ↓
Join
  ↓
Local Checkpoint Cache
  ↓
Map / Approach
  ↓
Geofence
  ↓
Scan
  ↓
GPS + Light + Motion
  ↓
Weighted Fusion
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
Sync
  ↓
Firebase Progress
  ↓
Game-Specific Leaderboard
```

---

# 107. FINAL NON-NEGOTIABLE RULES

```text
1. Games are dynamic.
2. Checkpoints are dynamic.
3. R001–R006 are seed/demo data only.
4. Every checkpoint is game-scoped.
5. Every discovery is user + game + checkpoint scoped.
6. Every leaderboard is game-specific.
7. There is no global leaderboard.
8. M2 owns creator management.
9. M3 owns location/sensors/fusion.
10. M4 owns quest/scan gameplay.
11. M5 owns Firebase/cloud.
12. M6 owns Room/repository/sync/integration.
13. UI does not directly access Room/Firebase/sensors.
14. Fusion uses GPS + light + motion.
15. Proximity is a separate final gate.
16. Client validation does not replace backend validation.
17. Offline writes must be retry-safe where supported.
18. Shared contract changes require team agreement.
```

---

# 108. SHARED CHECKLIST BEFORE INTEGRATION

```text
[ ] Same Game model
[ ] Same Checkpoint model
[ ] Same GameStatus values
[ ] Same LightSignature representation
[ ] Same repository signatures
[ ] Same game/checkpoint IDs
[ ] Same Room composite keys
[ ] Same Firestore paths
[ ] Same progress scope
[ ] Same leaderboard scope
[ ] Same FCM payload
[ ] Same navigation arguments
[ ] Same sensor/fusion boundary
[ ] Proximity remains separate final gate
[ ] No hard-coded checkpoint sequence
[ ] No global leaderboard
[ ] No direct Firebase from UI
[ ] No direct Room from UI
[ ] No direct sensors from M4
[ ] Offline behavior agreed
[ ] Sync behavior agreed
[ ] Security behavior agreed
[ ] End-to-end test passes
```

---

# 109. FINAL HANDOFF RULE

Every member should be able to answer:

```text
What do I own?
What interface do I consume?
What interface do I provide?
What must I not implement?
What data identity must I preserve?
What tests prove my feature works?
```

If an implementation cannot answer these questions, the feature is not ready for integration.

---

# 110. FINAL DOCUMENT STATUS

This document is the shared implementation contract for the final six-member Campus Quest allocation.

Any future changes must be treated as cross-team changes when they affect:

```text
models
repository
navigation
Firebase paths
Room schema
sensor/fusion contract
progress
leaderboard
notifications
offline/sync
```

Product behavior should not be changed merely to accommodate an individual implementation preference.

---

**END OF SHARED CONTRACTS & INTEGRATION INTERFACES**
