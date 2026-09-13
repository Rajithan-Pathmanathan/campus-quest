# MEMBER 6 — ROOM, REPOSITORY, OFFLINE SYNC & INTEGRATION WORKPLAN
## Campus Quest — Final Reassigned Team Plan

**Owner:** Member 6 (M6)  
**Primary responsibility:** Room local data layer, repository orchestration, offline-first persistence, Firebase synchronization, idempotency, migrations, and final cross-feature integration  
**Branch:** `feature/m6-room-sync-integration`  
**Architecture:** Android + Kotlin, MVVM + Repository  
**Status:** Final reassignment version

---

# 1. PURPOSE

M6 owns the **local persistence, repository orchestration, synchronization, and final integration layer** of Campus Quest.

The goal is to make the application reliable when:

- Data is temporarily unavailable from the network.
- The player discovers checkpoints offline.
- Creator drafts need local persistence.
- Firebase operations need retrying.
- The same operation is submitted more than once.
- Android recreates a screen/process.
- Multiple games exist simultaneously.
- Multiple users use the same application architecture.

M6 connects:

```text
UI / ViewModels
      ↓
Repository
      ↓
Room
      ↕
Firebase
```

M6 is also the final **integration owner** for cross-member functionality, build stability, contract consistency, and end-to-end testing.

---

# 2. IMPORTANT REASSIGNMENT

M6 owns:

- Room entities.
- Room database.
- Room DAOs.
- Local game cache.
- Local checkpoint cache.
- Local membership/progress data.
- Pending synchronization.
- Sync queue.
- Retry handling.
- Idempotency support.
- Room ↔ Firebase synchronization.
- Repository orchestration.
- Transactions.
- Migrations.
- Offline behavior.
- Cross-game/user data isolation.
- Integration builds.
- Cross-feature integration testing.
- Final dependency/contract verification.

M6 does NOT own:

- Creator UI.
- Player UI.
- Navigation UI.
- GPS.
- Geofencing.
- Ambient-light sensor.
- Accelerometer.
- Proximity sensor.
- Sensor fusion.
- Scan HUD.
- Reveal UI.
- Firebase security rules as the primary owner.
- FCM notification implementation as the primary owner.

M2 owns creator management.

M3 owns physical discovery signals.

M4 owns quest/scan gameplay.

M5 owns Firebase cloud/backend.

---

# 3. CORE ARCHITECTURE

The intended architecture is:

```text
Presentation
    ↓
ViewModel
    ↓
Use Case / Repository
    ↓
┌──────────────────────────────┐
│ Repository                   │
│                              │
│ Local source: Room           │
│ Remote source: Firebase      │
│ Sync coordinator             │
└──────────────────────────────┘
    ↓                    ↓
  Room               Firestore
```

The UI should not need to know whether data came from Room or Firebase.

---

# 4. CANONICAL DOMAIN MODEL

M6 works with the shared domain models.

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

M6 must not replace these with a permanent relic-centric architecture.

---

# 5. CANONICAL FIRESTORE STRUCTURE

The cloud side is:

```text
users
games/{gameId}
games/{gameId}/checkpoints/{checkpointId}
gamePlayers/{gameId}_{uid}
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
leaderboards/{gameId}/entries/{uid}
/topics/new_games
```

M6 must map the local Room model to the same logical identity and scope.

---

# 6. ROOM ENTITIES

Target entities:

```text
GameEntity
CheckpointEntity
GamePlayerEntity
FoundCheckpointEntity
PendingSyncEntity
```

Optional additional entities may be introduced only when justified.

Do not keep:

```text
RelicEntity
FoundRelicEntity
```

as the primary production architecture.

---

# 7. GAME ENTITY

Conceptual:

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

The exact schema should follow the project's final Room implementation.

---

# 8. CHECKPOINT ENTITY

Checkpoint identity must be game-scoped.

Recommended composite primary key:

```text
gameId + id
```

Conceptually:

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

This prevents identical checkpoint IDs in different games from colliding locally.

---

# 9. GAME PLAYER ENTITY

Conceptual identity:

```text
gameId + userId
```

This mirrors the logical cloud membership:

```text
gamePlayers/{gameId}_{uid}
```

Possible fields:

```text
gameId
userId
joinedAt
status
```

---

# 10. FOUND CHECKPOINT ENTITY

Discovery identity must include:

```text
gameId
userId
checkpointId
```

Recommended composite primary key:

```text
gameId + userId + checkpointId
```

Possible fields:

```text
foundAt
pendingSync
```

This prevents a discovery in one game from being mistaken for a discovery in another.

---

# 11. PENDING SYNC ENTITY

A pending operation can contain:

```text
operationId
userId
gameId
checkpointId
operationType
payload/reference
createdAt
retryCount
lastAttemptAt
status
```

The exact design can use a normalized queue or operation-specific pending flags.

The important requirements are:

- Retry-safe.
- Idempotent.
- Traceable.
- Game/user scoped.

---

# 12. ROOM DATABASE

M6 owns the Room database configuration.

Conceptually:

```kotlin
@Database(
    entities = [
        GameEntity::class,
        CheckpointEntity::class,
        GamePlayerEntity::class,
        FoundCheckpointEntity::class,
        PendingSyncEntity::class
    ],
    version = CURRENT_VERSION
)
abstract class CampusQuestDatabase : RoomDatabase()
```

The final entity list must match the actual implementation.

---

# 13. DAOS

Suggested DAOs:

```text
GameDao
CheckpointDao
GamePlayerDao
FoundCheckpointDao
PendingSyncDao
```

Each DAO should expose only the operations needed by the repository.

---

# 14. GAME DAO

Potential operations:

```kotlin
observeGames()
getGame(gameId)
getCreatorGames(creatorId)
insertGame(game)
updateGame(game)
deleteGame(game)
```

Use `Flow` for reactive UI data where appropriate.

---

# 15. CHECKPOINT DAO

Potential operations:

```kotlin
observeCheckpoints(gameId)
getCheckpoint(gameId, checkpointId)
insertCheckpoint(checkpoint)
updateCheckpoint(checkpoint)
deleteCheckpoint(gameId, checkpointId)
deleteCheckpointsForGame(gameId)
```

Every query must preserve:

```text
gameId
```

scope.

---

# 16. MEMBERSHIP DAO

Potential operations:

```kotlin
getMembership(gameId, userId)
observeMemberships(userId)
insertMembership(...)
```

The membership key must prevent duplicate logical joins.

---

# 17. FOUND CHECKPOINT DAO

Potential operations:

```kotlin
getFoundCheckpoint(userId, gameId, checkpointId)
observeFoundCheckpoints(userId, gameId)
insertDiscovery(...)
updateSyncStatus(...)
```

The primary key should make repeated insertion of the same logical discovery safe.

---

# 18. PENDING SYNC DAO

Potential operations:

```kotlin
insertPending(...)
getPendingOperations(...)
markCompleted(...)
incrementRetry(...)
removeCompleted(...)
```

The exact schema can be simplified if pending state is stored directly on operation entities.

---

# 19. ROOM RELATIONSHIPS

The local database should preserve:

```text
Game
  └── Checkpoints

User
  └── GameMembership

User
  └── Game
       └── FoundCheckpoint
```

The exact Room relation implementation is optional.

Correct scoping is mandatory.

---

# 20. REPOSITORY ROLE

M6 owns the repository orchestration layer.

The repository should provide a stable domain-level API:

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

The exact interface may evolve through shared team agreement.

---

# 21. REPOSITORY SOURCE SELECTION

For read operations, the repository may follow:

```text
UI
 ↓
Room/local cache
 ↓
Firebase refresh when required
 ↓
Room updated
 ↓
UI observes Room
```

or another documented strategy.

The selected strategy must be consistent across the application.

---

# 22. OFFLINE-FIRST PRINCIPLE

Where the product supports offline operation:

```text
Local Room
```

should remain useful without network access.

Examples:

- Previously loaded published games.
- Joined-game checkpoint data.
- Creator drafts where supported.
- Discovery progress.
- Pending operations.

---

# 23. CACHE PUBLISHED GAMES

When a player retrieves published games:

```text
Firebase
   ↓
Room cache
   ↓
Player UI
```

If network is unavailable later, Room can provide the cached list according to the offline contract.

---

# 24. CACHE CHECKPOINTS

When a player joins a game, checkpoint configuration should be locally available for gameplay.

This is particularly important because M3 may need:

```text
lat
lng
radiusM
minLux
maxLux
motionType
```

during offline gameplay.

---

# 25. CACHE GAME-SCOPED DATA

Do not cache checkpoints only by:

```text
checkpointId
```

because the same ID could exist in another game.

Use:

```text
gameId + checkpointId
```

---

# 26. CREATOR DRAFTS

If offline creator drafting is supported:

```text
M2 Creator UI
      ↓
Repository
      ↓
Room
      ↓
Draft
      ↓
Pending sync
      ↓
Firebase
```

M6 owns the persistence/sync behavior.

M2 only consumes the repository API.

---

# 27. DISCOVERY OFFLINE

For player discovery:

```text
M4 successful scan
      ↓
recordDiscovery()
      ↓
Room
      ↓
pendingSync
      ↓
Reveal
```

When connectivity returns:

```text
pendingSync
      ↓
Firebase
      ↓
success
      ↓
pendingSync removed
```

---

# 28. DISCOVERY IDEMPOTENCY

The logical key:

```text
userId + gameId + checkpointId
```

must uniquely identify a discovery.

If the same operation is submitted:

```text
once
twice
five times
```

the final state should still represent one discovery.

---

# 29. SYNC ENGINE

M6 owns the synchronization coordinator.

Conceptually:

```text
SyncCoordinator
       ↓
PendingSyncDao
       ↓
pending operation
       ↓
Firebase repository
       ↓
success/failure
       ↓
update local status
```

---

# 30. SYNC TRIGGERS

Sync may occur:

- On application start.
- On connectivity restoration.
- After a successful relevant cloud operation.
- When the user explicitly refreshes.
- At another project-approved lifecycle point.

The exact trigger strategy should avoid excessive work.

---

# 31. CONNECTIVITY

M6 may use Android connectivity APIs to determine whether synchronization should be attempted.

Do not repeatedly attempt network operations while clearly offline.

---

# 32. RETRY STRATEGY

Failed operations should be retryable.

Conceptual:

```text
Attempt 1
   ↓
failure
   ↓
Pending
   ↓
Attempt 2
   ↓
failure
   ↓
Pending
```

The implementation should avoid infinite rapid retries.

---

# 33. BACKOFF

Use a reasonable retry/backoff strategy.

For example:

```text
short delay
→ longer delay
→ longer delay
```

The exact timing is implementation-specific.

The important goal is to avoid battery/network abuse.

---

# 34. PERMANENT FAILURES

Not every failure should be retried forever.

Examples:

```text
permission denied
invalid data
unauthorized
deleted game
```

These may require:

```text
FAILED_PERMANENT
```

or equivalent handling.

M6 should preserve enough information to report the failure.

---

# 35. IDEMPOTENCY KEY

Where operation-level IDs are used:

```text
operationId
```

must remain stable across retries.

Do not generate a new operation identity for every retry.

---

# 36. CLOUD IDEMPOTENCY

M5 must expose cloud operations that are safe to retry.

Examples:

```text
joinGame
recordDiscovery
updateCheckpoint
```

M6 should document assumptions about M5's idempotency.

---

# 37. DISCOVERY SYNC ORDER

A discovery should not be marked fully synchronized until the cloud operation has succeeded according to the repository contract.

Conceptually:

```text
local discovery
      ↓
pending
      ↓
cloud success
      ↓
synced
```

---

# 38. SYNC FAILURE UI

Where the product exposes sync state:

```text
Saved locally
Waiting for sync
```

or:

```text
Sync failed
Retrying later
```

M4/M1 may present this state.

M6 provides the underlying state.

---

# 39. TRANSACTIONS

Room transactions should be used when multiple local records must remain consistent.

Example:

```text
insert discovery
+
insert pending sync
```

should be atomic if both are required for correct offline behavior.

---

# 40. DISCOVERY TRANSACTION

Conceptually:

```text
BEGIN TRANSACTION
    insert FoundCheckpoint
    insert PendingSync
COMMIT
```

If either fails:

```text
ROLLBACK
```

This prevents a discovery from existing without a synchronization record when both are required.

---

# 41. GAME CACHE TRANSACTION

When refreshing a game:

```text
update game
+
replace/update checkpoints
```

should be performed consistently.

Avoid a state where:

```text
new game metadata
+
old checkpoints
```

remain unintentionally.

---

# 42. ROOM MIGRATIONS

M6 owns Room migrations.

When the schema changes:

1. Increase database version.
2. Write migration.
3. Test migration.
4. Verify existing data.
5. Run integration tests.

Do not use destructive fallback casually.

---

# 43. MIGRATION TESTING

Test:

```text
old schema
    ↓
migration
    ↓
new schema
```

Verify:

- Existing games remain.
- Checkpoints remain correctly scoped.
- Progress remains.
- Pending sync remains.
- No cross-game data corruption occurs.

---

# 44. DATA CLEANUP

If migrating from the old relic architecture, M6 must carefully remove obsolete local entities/columns.

Do not blindly delete local data during development migrations.

Legacy data should be handled deliberately.

---

# 45. LEGACY RELIC MODEL

The old architecture may contain:

```text
RelicEntity
FoundRelicEntity
R001–R006
```

These must not remain the primary Room architecture.

The new target is:

```text
GameEntity
CheckpointEntity
FoundCheckpointEntity
```

with explicit game/user scope.

---

# 46. MULTI-GAME ISOLATION

Local queries must always preserve game boundaries.

Example:

```text
Game A
  CP-A1
  CP-A2

Game B
  CP-B1
  CP-B2
```

A query for Game A must never return:

```text
CP-B1
CP-B2
```

---

# 47. MULTI-USER ISOLATION

Progress queries must preserve user boundaries.

Example:

```text
User A / Game A
User B / Game A
```

must remain separate.

A local query for User A must never return User B's discoveries.

---

# 48. USER SIGN-OUT

On sign-out, M6 must follow the application's data-retention policy.

At minimum:

- Stop user-specific sync work.
- Do not display another user's private progress.
- Re-scope repository queries to the new authenticated identity.

Whether cached user data is retained or removed must follow the final security/privacy design.

---

# 49. USER SWITCHING TEST

Test:

```text
User A signs in
 ↓
Game A progress
 ↓
Sign out
 ↓
User B signs in
```

Verify:

```text
User A progress
```

does not appear as:

```text
User B progress
```

---

# 50. GAME SWITCHING TEST

Test:

```text
Game A
 ↓
Game B
```

Verify:

- Checkpoint cache switches correctly.
- Progress switches correctly.
- Pending operations remain correctly scoped.
- No sensor configuration leaks through repository state.

---

# 51. CREATOR → PLAYER ROLE FLOW

Where one account can create and play games, ensure:

```text
Creator data
```

and:

```text
Player progress
```

remain logically distinct.

The same authenticated user can own games while also having player progress in another game.

---

# 52. REPOSITORY THREADING

Room and network operations must not block the main thread.

Use:

```text
suspend
Flow
appropriate coroutine dispatching
```

according to the project's architecture.

---

# 53. FLOW / OBSERVABLE DATA

Use reactive local data where it improves UI consistency.

For example:

```kotlin
fun observeGameCheckpoints(
    gameId: String
): Flow<List<Checkpoint>>
```

This allows UI to react automatically after local sync.

---

# 54. SINGLE SOURCE OF DOMAIN MAPPING

If DTO/entity/domain mapping is used:

```text
Firestore DTO
      ↓
Domain
      ↓
Room Entity
```

or:

```text
Room Entity
      ↓
Domain
```

should be centralized.

Do not duplicate mapping logic across M1/M2/M4.

---

# 55. REPOSITORY ERROR MODEL

The repository should expose meaningful results.

Examples:

```text
Success
Unauthenticated
NotFound
PermissionDenied
NetworkUnavailable
ValidationError
Conflict
Unknown
```

The exact sealed result type can be defined jointly with the team.

---

# 56. M5 INTEGRATION

M6 depends on M5 for:

- Firebase repository implementation.
- Cloud write/read behavior.
- Idempotency.
- Security rules.
- Cloud errors.

M6 should not duplicate Firebase implementation.

---

# 57. M2 INTEGRATION

M6 supports M2 with:

- Draft persistence.
- Local game/checkpoint cache.
- Repository methods.
- Sync state.

M2 should only use the repository boundary.

---

# 58. M3 INTEGRATION

M6 provides M3 with locally cached checkpoint configuration:

```text
lat
lng
radiusM
lightSignature
motionType
```

M3 should not query Room directly.

---

# 59. M4 INTEGRATION

M6 supports M4 with:

```text
recordDiscovery
progress
offline state
```

M4 does not write Room directly.

---

# 60. M1 INTEGRATION

M6 provides repository data for:

- Available games.
- Game details.
- Membership.
- Leaderboard observation through the shared repository.
- Cached player data.

M1 should remain independent of Room implementation details.

---

# 61. INTEGRATION OWNER RESPONSIBILITY

M6 should maintain a working integrated branch/build.

The integration process should verify:

```text
M1 UI
+
M2 Creator
+
M3 Sensors
+
M4 Gameplay
+
M5 Firebase
+
M6 Room/Sync
```

work together rather than only compiling individually.

---

# 62. INTEGRATION ORDER

Recommended:

```text
1. Shared models/contracts
2. M6 Room foundation
3. M5 Firebase foundation
4. Repository integration
5. M1 player game discovery
6. M2 creator game creation
7. M2 checkpoint creation
8. M3 dynamic checkpoint consumption
9. M4 scan gameplay
10. Discovery recording
11. Offline sync
12. Leaderboard
13. FCM
14. Full end-to-end test
```

---

# 63. INTEGRATION BUILD CHECK

Before merging member branches:

```text
./gradlew assembleDebug
```

or the project's configured build command.

Also run:

```text
unit tests
instrumentation/UI tests
```

as applicable.

---

# 64. SHARED CONTRACT CHECK

Before integration, verify consistency for:

```text
Game
Checkpoint
GameStatus
LightSignature
GameLeaderboardEntry
GameRepository
navigation arguments
discovery result
notification payload
```

---

# 65. CONTRACT DRIFT

Contract drift occurs when two members implement different versions of the same model.

Examples:

```text
M2 uses checkpointId
M5 uses relicId
```

or:

```text
M3 expects minLux/maxLux
M2 stores a single lightValue
```

M6 should identify these conflicts before integration.

---

# 66. FINAL INTEGRATION MATRIX

| Feature | Owner | M6 Integration Responsibility |
|---|---|---|
| Player UI | M1 | Repository data contract |
| Navigation | M1 | Argument/data consistency |
| Creator UI | M2 | Draft persistence |
| Game creation | M2 | Repository + Room |
| Checkpoint configuration | M2 | Entity mapping |
| Location | M3 | Cached checkpoint data |
| Geofencing | M3 | Dynamic checkpoint data |
| Sensor fusion | M3 | No direct persistence coupling |
| Scan UI | M4 | Discovery repository |
| Reveal | M4 | Progress persistence |
| Firebase | M5 | Cloud source |
| Room | M6 | Primary owner |
| Sync | M6 | Primary owner |
| Leaderboard | M5 | Repository integration |
| FCM | M5 | Notification/navigation integration |

---

# 67. END-TO-END TEST — CREATOR TO PLAYER

Full flow:

```text
Creator login
 ↓
Create game
 ↓
Save draft
 ↓
Add checkpoint
 ↓
Configure location
 ↓
Configure radius
 ↓
Configure light
 ↓
Add clue/lore
 ↓
Publish
 ↓
FCM notification
 ↓
Player opens game
 ↓
Player joins
 ↓
Checkpoint cached locally
 ↓
Player approaches
 ↓
M3 detects signals
 ↓
M4 scan
 ↓
Fusion threshold
 ↓
Proximity gate
 ↓
Reveal
 ↓
Discovery recorded locally
 ↓
Sync to Firebase
 ↓
Leaderboard updated
```

M6 should help verify the complete chain.

---

# 68. END-TO-END TEST — OFFLINE DISCOVERY

```text
Player has joined game
 ↓
Checkpoint configuration cached
 ↓
Network disabled
 ↓
Player performs valid scan
 ↓
Discovery recorded locally
 ↓
Reveal shown
 ↓
Network restored
 ↓
Pending sync executes
 ↓
Firebase progress updated
 ↓
Leaderboard updated
```

---

# 69. END-TO-END TEST — OFFLINE CREATOR DRAFT

If offline creator drafts are supported:

```text
Creator
 ↓
Create/edit draft offline
 ↓
Room
 ↓
Network restored
 ↓
Sync
 ↓
Firebase
 ↓
Draft available
```

---

# 70. END-TO-END TEST — MULTI-GAME

Create:

```text
Game A
  CP-A1
  CP-A2

Game B
  CP-B1
  CP-B2
```

Verify:

```text
Room isolation
Firebase isolation
progress isolation
leaderboard isolation
geofence configuration isolation
scan state isolation
```

---

# 71. END-TO-END TEST — MULTI-USER

Test:

```text
Creator A
Creator B
Player A
Player B
```

Verify:

```text
ownership
membership
progress
leaderboard
cached data
```

remain correctly scoped.

---

# 72. SYNC DUPLICATE TEST

Simulate:

```text
same discovery
sent multiple times
```

Verify:

```text
one logical discovery
```

and:

```text
no duplicate leaderboard points
```

---

# 73. SYNC INTERRUPTION TEST

Test:

```text
sync starts
 ↓
network lost
 ↓
operation remains pending
 ↓
network restored
 ↓
operation retries
 ↓
success
```

---

# 74. PROCESS DEATH TEST

Test where practical:

```text
operation pending
 ↓
app process killed
 ↓
app reopened
 ↓
pending operation restored
 ↓
sync
```

The exact support level depends on the MVP lifecycle requirements.

---

# 75. ROOM CORRUPTION / INVALID DATA

The application should handle unexpected local data safely.

Do not allow malformed cached data to crash the UI.

Repository mapping should validate where necessary.

---

# 76. STALE CACHE TEST

Test:

```text
cached game
   ↓
cloud game updated
   ↓
refresh
   ↓
local cache updated
```

Verify the final local representation matches the agreed source-of-truth policy.

---

# 77. DELETED / CLOSED GAME TEST

If cloud game becomes unavailable or closed:

```text
Room cached game
        ↓
Firebase refresh
        ↓
updated lifecycle state
```

The repository should expose the new state.

Do not silently keep presenting a stale active game if the contract requires refresh.

---

# 78. REPOSITORY TEST MATRIX

| Operation | Online | Offline | Retry |
|---|---|---|---|
| Get games | Yes | Cache | Refresh |
| Get details | Yes | Cache | Refresh |
| Create game | Yes | Local if supported | Yes |
| Create checkpoint | Yes | Local if supported | Yes |
| Update checkpoint | Yes | Local if supported | Yes |
| Publish | Yes | Usually blocked/pending per contract | Yes |
| Join | Yes | Local/pending if supported | Yes |
| Record discovery | Yes | Local + pending | Yes |
| Leaderboard | Yes | Cached if supported | Refresh |
| Sync pending | Yes | No | Yes |

The final offline policy must follow the canonical product/technical contract.

---

# 79. ROOM UNIT TESTS

Test:

```text
insert
update
delete
query
composite key
game filtering
user filtering
ordering
```

---

# 80. ROOM INTEGRATION TESTS

Verify:

```text
Game A + CP-A1
Game B + CP-B1
```

do not collide.

Verify:

```text
User A + Game A
User B + Game A
```

do not collide.

---

# 81. DAO TESTS

At minimum:

```text
GameDao
CheckpointDao
GamePlayerDao
FoundCheckpointDao
PendingSyncDao
```

should have meaningful tests for their critical queries.

---

# 82. MIGRATION TESTS

For every schema migration:

```text
old database
 ↓
migration
 ↓
new database
```

verify that important data survives.

---

# 83. SYNC TESTS

Test:

```text
success
network failure
permission failure
duplicate operation
conflict
retry
permanent failure
```

---

# 84. REPOSITORY INTEGRATION TESTS

Verify the repository does not leak implementation details.

For example:

```text
M4 calls recordDiscovery()
```

without knowing whether the data goes to:

```text
Room
Firebase
both
```

---

# 85. OFFLINE ACCEPTANCE CRITERIA

The application is considered offline-ready only if:

- Required cached game/checkpoint data is available.
- Discovery can be recorded locally where supported.
- Pending work survives expected lifecycle events.
- Synchronization retries.
- Duplicate operations do not create duplicate records.
- User/game scope remains correct.

---

# 86. PERFORMANCE

M6 should avoid:

- Loading the entire database for one checkpoint.
- Repeatedly downloading unchanged data.
- Blocking the main thread.
- Excessive sync loops.
- Unbounded pending queues.

Use targeted queries and incremental synchronization.

---

# 87. STORAGE MANAGEMENT

Cached data should have a clear retention policy.

Potential categories:

```text
Active joined games
Creator drafts
Completed games
Old cached games
Pending sync operations
```

The final cleanup policy should be agreed before implementation.

---

# 88. SYNC OBSERVABILITY

During development, provide enough logging to determine:

```text
operation ID
operation type
game ID
checkpoint ID
attempt count
success/failure
```

Do not log sensitive information unnecessarily.

---

# 89. DEBUGGING SUPPORT

M6 should be able to answer:

```text
Why was this discovery not synced?
Why does this checkpoint not appear?
Why is an old game still cached?
Why did a duplicate request not create a duplicate?
```

A small structured debug log is useful.

---

# 90. NO DIRECT ROOM ACCESS FROM UI

Avoid:

```kotlin
checkpointDao.get(...)
```

inside M1/M2/M4 ViewModels.

Use:

```text
Repository
```

instead.

---

# 91. NO DIRECT FIREBASE ACCESS FROM UI

Similarly avoid:

```kotlin
FirebaseFirestore.getInstance()
```

inside UI/ViewModels.

The repository boundary must remain intact.

---

# 92. DATA MAPPING

Suggested:

```text
Room Entity
    ↓
Mapper
    ↓
Domain Model
```

and:

```text
Firebase DTO
    ↓
Mapper
    ↓
Domain Model
```

This prevents infrastructure details from spreading across the app.

---

# 93. TEST DATA

M6 should maintain test data covering:

```text
demo-campus-quest
demo-science-trail
draft game
multiple players
multiple memberships
multiple discoveries
multiple leaderboard entries
pending sync operations
```

---

# 94. LEGACY TEST DATA

R001–R006 may remain as sample records.

But integration tests should also use arbitrary IDs:

```text
cp-alpha-001
cp-beta-001
```

to prove the architecture is dynamic.

---

# 95. GIT WORKFLOW

Branch:

```text
feature/m6-room-sync-integration
```

Commit examples:

```text
feat(room): add game entity and dao
feat(room): add checkpoint entity and dao
feat(room): add game player entity
feat(room): add found checkpoint entity
feat(room): add pending sync queue
feat(repository): add local data source
feat(sync): add pending operation coordinator
feat(sync): add retry handling
feat(repository): integrate firebase and room
test(room): add isolation tests
test(sync): add idempotency tests
test(integration): add end-to-end flow
```

Avoid implementing unrelated UI or sensor features in M6 commits.

---

# 96. INTEGRATION BRANCH

M6 should maintain or coordinate an integration branch according to the team's Git workflow.

Recommended conceptual structure:

```text
main
 ├── feature/m1-player-ui
 ├── feature/m2-creator-game-management
 ├── feature/m3-location-sensors-fusion
 ├── feature/m4-quest-scan-gameplay
 ├── feature/m5-firebase-backend
 └── feature/m6-room-sync-integration
```

M6 validates merged combinations before final delivery.

---

# 97. MERGE ORDER

A practical merge sequence is:

```text
shared contracts
 ↓
M6 Room foundation
 ↓
M5 Firebase
 ↓
repository integration
 ↓
M1/M2/M3/M4 feature integration
 ↓
full tests
```

The exact order can change if dependencies require it.

---

# 98. BUILD BREAK MANAGEMENT

If a member's merge breaks the build:

1. Identify the failing contract.
2. Reproduce.
3. Notify the responsible member.
4. Fix with the smallest compatible change.
5. Update tests.
6. Re-run integration.

Do not hide failures by deleting tests or bypassing architecture boundaries.

---

# 99. SHARED CONTRACT CHANGES

M6 should monitor changes to:

```text
Game
Checkpoint
GameStatus
LightSignature
GameRepository
GameLeaderboardEntry
navigation arguments
discovery contracts
```

If a change affects multiple members, update the shared contracts document.

---

# 100. FINAL INTEGRATION CHECKLIST

```text
[ ] Project builds
[ ] Authentication works
[ ] Player game discovery works
[ ] Creator game creation works
[ ] Creator checkpoint creation works
[ ] Dynamic checkpoints load
[ ] Room cache works
[ ] Firebase works
[ ] Repository boundary works
[ ] Join works
[ ] M3 receives checkpoint configuration
[ ] M4 receives M3 signal state
[ ] Discovery is recorded
[ ] Offline discovery works if supported
[ ] Pending sync works
[ ] Leaderboard updates
[ ] FCM notification works
[ ] Notification deep link works
[ ] Multi-game isolation works
[ ] Multi-user isolation works
[ ] Security tests pass
[ ] Room tests pass
[ ] Sync tests pass
[ ] End-to-end tests pass
```

---

# 101. DEFINITION OF DONE — ROOM

M6 is complete for Room when:

- All required entities exist.
- Composite keys enforce required scopes.
- DAOs provide required operations.
- Queries are game/user scoped.
- Critical operations are tested.
- Migrations are defined.
- No obsolete relic entity is the primary model.

---

# 102. DEFINITION OF DONE — REPOSITORY

M6 is complete for the repository when:

- UI/ViewModels use domain-level methods.
- Room and Firebase are hidden behind boundaries.
- Read strategy is documented.
- Write strategy is documented.
- Errors are mapped.
- Offline behavior is defined.
- Duplicate operations are safe.

---

# 103. DEFINITION OF DONE — SYNC

M6 is complete for synchronization when:

- Pending operations are persisted.
- Retry works.
- Backoff is implemented where required.
- Idempotency is supported.
- Permanent failures are handled.
- Sync state survives expected lifecycle events.
- Cross-user/game scope is preserved.

---

# 104. DEFINITION OF DONE — INTEGRATION

M6 is complete for integration when:

- All six members' features build together.
- Shared contracts are consistent.
- End-to-end creator-to-player flow works.
- Offline flow works according to the final contract.
- Multi-game isolation works.
- Multi-user isolation works.
- Critical tests pass.
- Demo build is reproducible.

---

# 105. FINAL M6 ARCHITECTURE

The final data path is:

```text
                 UI / ViewModels
                        ↓
                  GameRepository
                        ↓
             ┌──────────┴──────────┐
             ↓                     ↓
           Room                 Firebase
             ↓                     ↓
       Local cache            Cloud source
             │                     │
             └──────────┬──────────┘
                        ↓
                 Sync Coordinator
                        ↓
                  Pending Queue
                        ↓
                 Retry / Idempotency
```

For discovery:

```text
M3 Physical Evidence
        ↓
M4 Scan Success
        ↓
recordDiscovery()
        ↓
M6 Repository
        ↓
Room transaction
   ├─ FoundCheckpoint
   └─ PendingSync
        ↓
M5 Firebase
        ↓
Cloud Progress
        ↓
Game Leaderboard
```

---

# 106. CORE DESIGN PRINCIPLE

M6 provides the **reliability layer** between the application and the cloud.

The application should behave consistently whether:

```text
online
offline
reconnecting
retrying
recreated
switching games
switching users
```

The most important data-isolation rule is:

```text
User + Game + Checkpoint
```

for discovery/progress.

The most important local checkpoint rule is:

```text
Game + Checkpoint
```

for checkpoint identity.

The most important architecture rule is:

```text
UI → Repository → Room/Firebase
```

not:

```text
UI → Room
```

or:

```text
UI → Firebase
```

---

# 107. M6 QUICK CHECKLIST

```text
[ ] GameEntity
[ ] CheckpointEntity
[ ] GamePlayerEntity
[ ] FoundCheckpointEntity
[ ] PendingSyncEntity
[ ] Room database
[ ] GameDao
[ ] CheckpointDao
[ ] GamePlayerDao
[ ] FoundCheckpointDao
[ ] PendingSyncDao
[ ] Composite keys
[ ] Game isolation
[ ] User isolation
[ ] Repository
[ ] Room data source
[ ] Firebase data source integration
[ ] Offline cache
[ ] Creator draft persistence
[ ] Offline discovery
[ ] Pending sync
[ ] Retry
[ ] Backoff
[ ] Idempotency
[ ] Transactions
[ ] Migrations
[ ] Sign-out isolation
[ ] Multi-game testing
[ ] Multi-user testing
[ ] Repository tests
[ ] Room tests
[ ] Sync tests
[ ] Integration tests
[ ] Full end-to-end test
[ ] Build verification
[ ] Legacy cleanup
```

---

# 108. FINAL HANDOFF PACKAGE

M6 should provide:

1. Room database.
2. Room entities.
3. Room DAOs.
4. Room migrations.
5. Local data source.
6. Repository orchestration.
7. Firebase repository integration.
8. Offline cache.
9. Pending sync mechanism.
10. Retry/backoff logic.
11. Idempotency handling.
12. Transaction handling.
13. Cross-game/user isolation.
14. Integration branch/build.
15. End-to-end tests.
16. Sync debugging/logging guidance.
17. Migration notes.
18. Final integration report.
19. Any shared-contract changes.

---

# 109. DOCUMENTS M6 SHOULD KEEP IN SYNC

When M6 changes architecture or data behavior, review:

- `00_MASTER_DEVELOPMENT_PLAN_UPDATED.md`
- `SHARED_CONTRACTS_AND_INTEGRATION_INTERFACES.md`
- `TRD_CAMPUS_QUEST.md`
- `PRD_CAMPUS_QUEST.md`
- `APP_FLOW_DOCUMENT_CAMPUS_QUEST.md`
- `MOCK_DATA_CATALOG_AND_SEED_DATA_SPECIFICATION.md`
- `TESTING_AND_ACCEPTANCE_STRATEGY.md`
- `GIT_CHANGE_CONTROL_AND_TEAM_DEVELOPMENT_WORKFLOW.md`
- `INTEGRATION_HANDOFF_AND_MILESTONE_PLAN.md`

Product documents should only be changed when actual product behavior changes.

---

# 110. FINAL TEAM ARCHITECTURE AFTER ALL REASSIGNMENTS

```text
M1
Player UI + Navigation
        │
        ▼
M2
Creator + Game Management
        │
        ▼
Shared Game/Checkpoint Contracts
        │
 ┌──────┴─────────┐
 ▼                ▼
M3               M4
Location/        Quest/
Sensors/Fusion   Scan Gameplay
 │                │
 └──────┬─────────┘
        ▼
       M6
Room + Repository + Sync
        │
        ▼
       M5
Firebase + Firestore + FCM
```

The actual runtime dependency is better represented as:

```text
                 ┌─────────────────┐
                 │ M1 Player UI    │
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │ M2 Creator UI    │
                 └────────┬────────┘
                          │
                    Repository
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
           M6 Room                  M5 Cloud
             │                         │
             └────────────┬────────────┘
                          │
                    Shared Domain
                          │
                ┌─────────┴─────────┐
                ▼                   ▼
              M3                    M4
       Physical Signals       Scan Gameplay
```

This keeps the implementation responsibilities separated while allowing all six members to work in parallel.

---

**END OF MEMBER 6 ROOM, REPOSITORY, OFFLINE SYNC & INTEGRATION WORKPLAN**
