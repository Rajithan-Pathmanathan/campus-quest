# M6 — ROOM DATA & INTEGRATION WORKPLAN
## Campus Quest — Mobile Application Development

**Document ID:** M6-ROOM-DATA-INTEGRATION  
**Owner:** M6 — Data & Integration Developer  
**Project:** Campus Quest  
**Primary responsibility:** Room local persistence, offline cache, pending synchronization state, repository integration support, build stability, cross-feature integration, and end-to-end validation coordination.

---

# 1. Purpose

This document defines M6's implementation responsibilities and the boundaries between local persistence and the other Campus Quest modules.

M6 has two connected responsibilities:

```text
1. Local data layer
2. Integration/build coordination
```

The local data layer provides immediate persistence and offline capability.

The integration responsibility ensures that the six independently developed features can be combined progressively rather than waiting until the final days.

The intended architecture is:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
┌───────────────┬────────────────┐
│ Room          │ Firestore      │
│ local         │ cloud          │
└───────────────┴────────────────┘
```

M6 owns the Room side and helps coordinate the boundary with M5's Firebase implementation.

---

# 2. M6 Role

## 2.1 Primary responsibility

M6 is responsible for:

1. Designing Room entities.
2. Creating the Room database.
3. Creating DAOs.
4. Implementing local repository operations.
5. Caching relic configuration locally.
6. Persisting discovered relics immediately.
7. Tracking `pendingSync`.
8. Providing local-first/offline behavior.
9. Supporting synchronization with M5.
10. Preventing duplicate local discoveries.
11. Testing persistence across app restarts.
12. Coordinating incremental integration.
13. Maintaining build stability.
14. Supporting end-to-end testing.
15. Coordinating shared integration checks.
16. Tracking merge/build issues across feature branches.

## 2.2 M6 does not own

M6 must not independently own:

- Firebase Authentication.
- Firestore security rules.
- Google Maps.
- Fused Location Provider.
- Geofencing.
- SensorManager.
- Sensor fusion.
- Scan UI.
- App-wide visual design.

M6 coordinates integration but does not become the sole implementer of everyone else's features.

---

# 3. Why M6 Has an Integration Responsibility

Room alone is not enough work for a six-person, one-month project.

M6 therefore owns:

```text
Room
+
offline persistence
+
sync state
+
integration coordination
+
build verification
+
end-to-end testing support
```

However:

> **Every member remains responsible for integrating and testing their own feature.**

M6 coordinates the process; M6 does not merge broken modules and repair every member's code alone.

---

# 4. Architecture

The target architecture is:

```text
                    UI Layer
                       ↓
                   ViewModel
                       ↓
                   Repository
                 /           \
                ↓             ↓
              Room         Firestore
            (local)         (cloud)
```

This follows the project's MVVM/repository separation.

Room should not be accessed directly from Activities/Fragments where a repository/ViewModel boundary is intended.

Firestore should also remain behind the repository boundary.

---

# 5. Local Data Responsibilities

M6 should provide local persistence for:

```text
Relic
FoundRelic
sync status
```

The local database should support the application's core experience even when the network is unavailable.

---

# 6. Room Database

Conceptual database:

```kotlin
@Database(
    entities = [
        RelicEntity::class,
        FoundRelicEntity::class
    ],
    version = 1
)
abstract class CampusQuestDatabase : RoomDatabase()
```

The exact annotations/imports depend on the project's Room version.

---

# 7. RelicEntity

Suggested model:

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

The exact implementation can be adapted to the final shared domain model.

---

# 8. FoundRelicEntity

Suggested model:

```kotlin
@Entity(
    tableName = "found_relics"
)
data class FoundRelicEntity(
    @PrimaryKey
    val relicId: String,
    val foundAt: Long,
    val pendingSync: Boolean
)
```

Important:

```text
pendingSync = local state
```

It should not be confused with a Firestore field.

---

# 9. Local Schema Principle

The project uses:

```text
Room → immediate/local state
Firestore → shared/cloud state
```

Therefore:

```text
Room FoundRelicEntity
    relicId
    foundAt
    pendingSync
```

can contain synchronization information that does not belong in the cloud document.

---

# 10. Canonical Local Relic Data

M6 should support the same six canonical relics used by the rest of the team:

| ID | Name | Latitude | Longitude | Radius | Light |
|---|---|---:|---:|---:|---:|
| R001 | Founder’s Bell | 6.974850 | 79.915300 | 25m | 180–320 |
| R002 | Scholar’s Compass | 6.975420 | 79.914750 | 25m | 250–450 |
| R003 | Heritage Key | 6.975900 | 79.915650 | 20m | 80–180 |
| R004 | Old Library Seal | 6.976300 | 79.914900 | 30m | 400–650 |
| R005 | Garden Chronicle | 6.974300 | 79.916100 | 25m | 120–250 |
| R006 | Clock Tower Relic | 6.976750 | 79.915700 | 20m | 300–500 |

These values are development values and must be physically validated before final demonstration.

M6 must not create a conflicting second relic catalog.

---

# 11. DAO Responsibilities

Suggested DAOs:

```text
RelicDao
FoundRelicDao
```

## RelicDao

Operations:

```text
insert/replace relics
get all relics
get relic by ID
delete/clear cache if required
```

## FoundRelicDao

Operations:

```text
insert discovery
check whether found
observe found relics
get pending sync records
mark synced
```

---

# 12. RelicDao Conceptual Interface

```kotlin
@Dao
interface RelicDao {

    @Query("SELECT * FROM relics")
    fun observeRelics(): Flow<List<RelicEntity>>

    @Query("SELECT * FROM relics WHERE id = :id")
    suspend fun getRelic(id: String): RelicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(relics: List<RelicEntity>)
}
```

The exact DAO API can be adjusted to the chosen architecture.

---

# 13. FoundRelicDao Conceptual Interface

```kotlin
@Dao
interface FoundRelicDao {

    @Query("SELECT * FROM found_relics")
    fun observeFound(): Flow<List<FoundRelicEntity>>

    @Query(
        "SELECT * FROM found_relics WHERE relicId = :relicId"
    )
    suspend fun getFound(
        relicId: String
    ): FoundRelicEntity?

    @Query(
        "SELECT * FROM found_relics WHERE pendingSync = 1"
    )
    suspend fun getPendingSync(): List<FoundRelicEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(
        entity: FoundRelicEntity
    )

    @Query(
        "UPDATE found_relics SET pendingSync = 0 WHERE relicId = :relicId"
    )
    suspend fun markSynced(relicId: String)
}
```

This is a conceptual starting point.

---

# 14. Duplicate Local Discovery

The primary key:

```text
relicId
```

prevents duplicate local records.

Example:

```text
R001 discovered
 ↓
insert R001
```

Then:

```text
R001 discovered again
 ↓
existing R001
 ↓
do not create another unique record
```

This is important because the leaderboard counts unique relics.

---

# 15. Repository Boundary

A conceptual shared interface is:

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

M6's local implementation should support the local side without leaking Room entities into UI code.

---

# 16. Local Repository

Conceptual:

```kotlin
class LocalQuestRepository(
    private val relicDao: RelicDao,
    private val foundRelicDao: FoundRelicDao
)
```

Responsibilities:

```text
Room read/write
domain/entity mapping
local discovery
pending sync retrieval
sync state updates
```

---

# 17. Entity-to-Domain Mapping

Avoid exposing:

```text
RelicEntity
FoundRelicEntity
```

to M1/M2 UI code.

Use:

```text
Room Entity
     ↓
Mapper
     ↓
Domain model
```

Example:

```kotlin
fun RelicEntity.toDomain(): Relic =
    Relic(
        id = id,
        name = name,
        lat = lat,
        lng = lng,
        radiusM = radiusM,
        lightSignature = LightSignature(
            minLux = lightMin,
            maxLux = lightMax
        ),
        rarity = rarity,
        lore = lore
    )
```

---

# 18. Local-First Relic Loading

A practical flow:

```text
Repository
 ↓
Room relic cache
 ↓
if available → return local
 ↓
if missing/stale → request cloud
 ↓
M5 Firebase implementation
 ↓
update Room
 ↓
return domain relics
```

M6 controls the local cache.

M5 controls the cloud source.

---

# 19. Cache Refresh

M6 should support:

```text
refresh relic cache
```

after M5 successfully retrieves cloud configuration.

For the six-relic MVP, complexity should remain low.

A simple strategy is enough:

```text
replace/update local relic cache
```

The team does not need a sophisticated cache invalidation framework.

---

# 20. Offline Mode

The app should still allow locally available data to be used when offline.

Example:

```text
Internet available
 ↓
relics cached
 ↓
internet lost
 ↓
Room supplies relics
 ↓
user completes scan
 ↓
Room stores discovery
 ↓
pendingSync = true
```

The discovery should not be lost merely because the network disappears.

---

# 21. Discovery Persistence Flow

Successful reveal:

```text
M2 / ViewModel
      ↓
Repository.recordReveal(R001)
      ↓
Room
      ↓
FoundRelicEntity(
    relicId = R001,
    foundAt = now,
    pendingSync = true
)
```

Then synchronization occurs.

---

# 22. Sync Flow

Conceptual:

```text
Room
 ↓
pending records
 ↓
M5 cloud repository
 ↓
Firestore
 ↓
success
 ↓
Room markSynced()
```

If cloud upload fails:

```text
Room pendingSync remains true
```

The next sync attempt can retry.

---

# 23. Sync Must Be Idempotent

Example failure:

```text
upload R001
 ↓
Firestore write succeeds
 ↓
network drops before local markSynced
```

Next attempt:

```text
upload R001 again
```

must not create another unique discovery.

Use:

```text
progress/{uid}/found/{relicId}
```

as the deterministic cloud document identity.

---

# 24. Sync Algorithm

Conceptual:

```text
for each pending record:

    send record to cloud

    if successful:
        mark local record synced

    else:
        keep pending
```

Do not mark:

```text
pendingSync = false
```

before receiving successful cloud acknowledgement.

---

# 25. Sync Failure Handling

Potential failures:

```text
No internet
Timeout
Permission denied
Firebase unavailable
Malformed cloud response
```

Behavior:

```text
do not delete local discovery
do not mark synced
keep pending
retry later
```

A permission/security error should be surfaced for debugging rather than endlessly retried.

---

# 26. Sync Retry Strategy

The MVP does not require a complex background synchronization framework unless the team chooses to add one.

A practical approach:

```text
app startup
foreground transition
manual refresh
after successful network recovery
```

trigger:

```text
syncPending()
```

The exact trigger schedule can be refined during integration.

---

# 27. M6 and Firebase

M6 must not duplicate M5's Firebase implementation.

Correct:

```text
M6 local repository
      ↓
shared cloud interface
      ↓
M5 Firebase repository
```

Incorrect:

```text
M6 Room
+
M6 independent Firestore code
+
M5 independent Firestore code
```

There must be one agreed cloud implementation.

---

# 28. M6 and Authentication

M5 owns Firebase Auth.

M6 may need:

```text
current authenticated UID
```

for sync.

M6 should receive that identity through the agreed repository/auth boundary rather than directly depending on Firebase SDK calls throughout Room code.

---

# 29. User-Specific Progress

Found relics belong to a user.

Therefore the cloud path is:

```text
progress/{uid}/found/{relicId}
```

The local database can use:

```text
userId
```

if the team supports multiple authenticated users on one physical device.

For a simple MVP where one app installation normally corresponds to one active user, the schema may omit userId from local records, but this decision must be documented.

---

# 30. Important Multi-User Decision

If users can log out and another user can log in on the same device, local progress isolation becomes important.

Safer conceptual model:

```kotlin
FoundRelicEntity(
    userId,
    relicId,
    foundAt,
    pendingSync
)
```

with:

```text
PrimaryKey(userId, relicId)
```

However, if the project assumes one active local user at a time and clears user-specific data on logout, a simpler schema can be used.

**M6 must agree this decision with M5 and M1 before final implementation.**

---

# 31. Recommended MVP Choice

For robustness, use:

```text
userId + relicId
```

as the composite identity for local discovered relics.

Conceptually:

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

This prevents User A's local discoveries from appearing for User B.

---

# 32. Room Database Versioning

M6 must treat schema changes carefully.

If:

```text
version 1
```

is already deployed locally and entities change, increase the version and provide a migration or explicitly recreate the development database where acceptable.

Do not casually delete user data in a final build.

---

# 33. MVP Migration Policy

During early development:

```text
destructive recreation
```

may be acceptable if the database is disposable.

Before final testing:

```text
use controlled schema version
```

and verify upgrade behavior.

---

# 34. Room Threading

Room database operations should not block the UI thread.

Use:

```text
suspend functions
Flow
appropriate coroutine context
```

The exact dispatcher strategy should follow the project's existing architecture.

---

# 35. Flow Usage

For reactive local state:

```kotlin
fun observeFoundRelics():
    Flow<List<FoundRelic>>
```

This allows:

```text
Room change
 ↓
Flow emission
 ↓
ViewModel
 ↓
Progress UI
```

M2 can observe progress without manually polling the database.

---

# 36. Integration Coordinator Responsibilities

M6 should maintain an integration board containing:

```text
Feature
Owner
Branch
Status
Dependency
Last tested commit
Known issue
```

Example:

| Feature | Owner | Status |
|---|---|---|
| Navigation | M1 | ready |
| Scan UI | M2 | ready |
| Location | M3 | integration |
| Sensor fusion | M4 | integration |
| Firebase | M5 | ready |
| Room | M6 | ready |

---

# 37. Integration Rule

Never wait until September 27 to combine everything.

The team should integrate incrementally:

```text
M1 + M2
 ↓
M3
 ↓
M4
 ↓
M6
 ↓
M5
 ↓
full application
```

This is not a strict order; features should be integrated as soon as their contracts are usable.

---

# 38. First Integration Target

The first meaningful integrated flow should be:

```text
Login
 ↓
Map
 ↓
Select R001
 ↓
Scan
 ↓
Fake/real fusion
 ↓
Reveal
 ↓
Room persistence
```

Firebase can initially be mocked.

This gives the team a working vertical slice early.

---

# 39. Second Integration Target

Then:

```text
Login
 ↓
Map
 ↓
R001
 ↓
M3 location
 ↓
M4 sensor fusion
 ↓
M2 reveal
 ↓
M6 Room
```

This is the physical MVP path.

---

# 40. Third Integration Target

Finally:

```text
Login
 ↓
Firebase Auth
 ↓
Relic from Firestore
 ↓
M3 geofence
 ↓
M4 fusion
 ↓
M2 reveal
 ↓
Room
 ↓
Firestore progress
 ↓
Leaderboard
```

This is the complete cloud-backed path.

---

# 41. Build Stability

M6 should verify the project after major merges.

Minimum checks:

```text
Gradle sync
compile
unit tests
assemble debug
launch
basic navigation
```

Do not wait until final submission to discover that the combined project no longer builds.

---

# 42. Integration Build Checklist

After each major integration:

- [ ] Gradle sync passes.
- [ ] Kotlin compilation passes.
- [ ] Resources compile.
- [ ] Manifest is valid.
- [ ] Navigation works.
- [ ] App launches.
- [ ] No obvious runtime crash.
- [ ] Core screen opens.
- [ ] Feature-specific tests pass.

---

# 43. Branch Coordination

M6 may coordinate:

```text
main
feature/ui-navigation
feature/quest-scan
feature/location-geofence
feature/sensor-fusion
feature/firebase-sync
feature/room-data
```

The exact branch names can follow the shared Git agreement.

No one should directly push unfinished changes to `main`.

---

# 44. Integration Branch

If the team uses:

```text
integration
```

M6 may coordinate it as a temporary staging branch.

Example:

```text
feature branches
       ↓
integration
       ↓
testing
       ↓
main
```

This is optional.

The team's shared Git agreement must define the final workflow.

---

# 45. Merge Order

Merge based on dependency readiness, not member number.

A sensible sequence:

```text
shared models/contracts
 ↓
M1 shell
 ↓
M2 screens
 ↓
M6 Room
 ↓
M3 location
 ↓
M4 fusion
 ↓
M5 Firebase
```

However, individual features can be integrated earlier through mocks.

---

# 46. Conflict Management

If a merge conflict occurs:

1. Identify which contract changed.
2. Ask the relevant feature owners to resolve behavior.
3. M6 should not silently choose a functional interpretation.
4. Run affected tests.
5. Document any contract change.

---

# 47. Shared Model Changes

Changes to shared models are high-risk.

Examples:

```text
Relic fields
FusionResult
FoundRelic
LeaderboardEntry
repository methods
```

Before changing:

```text
tell affected members
```

After changing:

```text
update shared contract
update implementations
run tests
```

---

# 48. Room and Repository Mapping

Recommended:

```text
Room Entity
    ↓
DAO
    ↓
Local Repository
    ↓
Repository/domain boundary
    ↓
ViewModel
```

M6 should not expose DAO methods directly to M2.

---

# 49. Local Progress Queries

Required capabilities:

```text
is relic found?
get all found relics
get count
get pending sync
mark synced
```

This supports:

```text
progress screen
quest state
leaderboard sync
offline operation
```

---

# 50. Progress Count

The local count should be based on unique relics.

Conceptually:

```text
COUNT(DISTINCT relicId)
```

or the equivalent schema behavior.

Do not count:

```text
scan attempts
```

as discoveries.

---

# 51. Relic Completion State

A relic should have a simple local state:

```text
not found
found/pending sync
found/synced
```

This is enough for MVP.

Do not introduce unnecessary complex workflow states.

---

# 52. Local Cache Freshness

For six relics, M6 can keep the caching policy simple.

Possible policy:

```text
load Room cache
 ↓
if empty → request cloud
 ↓
if cloud succeeds → replace/update Room
```

The project does not need a complicated stale-while-revalidate framework unless required.

---

# 53. Offline Discovery Scenario

Test:

```text
1. Login.
2. Load relic catalog.
3. Disable network.
4. Start R001 scan.
5. Complete scan.
6. Verify R001 appears in local progress.
7. Verify pendingSync = true.
8. Re-enable network.
9. Run sync.
10. Verify Firestore progress exists.
11. Verify pendingSync = false.
```

This is a critical M6 acceptance test.

---

# 54. App Restart Scenario

Test:

```text
discover R001
 ↓
close app
 ↓
reopen app
 ↓
progress still shows R001
```

This verifies that persistence is actually local rather than only in-memory.

---

# 55. User Switch Scenario

If multi-user local support is implemented:

```text
User A → discovers R001
logout
User B → login
```

Expected:

```text
B does not inherit A's local progress
```

Then:

```text
B discovers R002
```

Expected:

```text
A → R001
B → R002
```

---

# 56. Database Inspection

During development, inspect Room data where practical.

Verify:

```text
relics
found_relics
pendingSync
timestamps
user identity if included
```

This helps diagnose integration failures.

---

# 57. Error Handling

M6 should distinguish:

```text
Local database failure
Cloud sync failure
No pending records
Permission failure
Invalid local data
```

Do not delete local records just because cloud synchronization fails.

---

# 58. Sync Logging

Development log example:

```text
SYNC_START pending=1
SYNC_UPLOAD relic=R001
SYNC_SUCCESS relic=R001
ROOM_MARK_SYNCED relic=R001
SYNC_COMPLETE pending=0
```

Failure:

```text
SYNC_UPLOAD_FAILED relic=R001 reason=NETWORK
ROOM_RETAIN_PENDING relic=R001
```

Do not log passwords or sensitive authentication data.

---

# 59. M6 and M2

M2 needs:

```text
found state
progress count
successful reveal persistence
```

M6 provides this through repository/domain APIs.

M2 must not directly use Room DAOs.

---

# 60. M6 and M3

M3 does not need direct Room access.

M3 provides:

```text
location events
distance
geofence state
```

M6 persists only data that is part of the application's durable state.

Raw GPS streams should not be stored unnecessarily.

---

# 61. M6 and M4

M4 provides:

```text
successful reveal result
```

M6 persists:

```text
relicId
timestamp
sync state
```

M6 should not store every raw accelerometer/light reading.

The project only needs the discovery result for durable progress.

---

# 62. M6 and M5

This is M6's most important integration boundary.

```text
M6:
Room + pending queue

M5:
Firebase + cloud write

Shared:
sync contract
```

M6 should be able to call the cloud repository without knowing Firestore collection implementation details.

---

# 63. M6 and M1

M1 needs:

```text
application state
login-aware navigation
progress information
```

M6 supplies persistent progress through repository/domain APIs.

---

# 64. Testing Strategy

M6 should maintain three levels:

```text
Unit
Integration
End-to-end
```

## Unit

Test:

```text
mappers
local repository
duplicate logic
sync state transitions
```

## Integration

Test:

```text
DAO
database
repository
cloud sync boundary
```

## End-to-end

Test:

```text
login
map
scan
reveal
save
sync
leaderboard
```

---

# 65. Room Unit/Integration Test Cases

Required:

### Test A

Insert R001.

Expected:

```text
R001 retrievable
```

### Test B

Insert R001 twice.

Expected:

```text
one unique record
```

### Test C

Create pending R001.

Expected:

```text
pending list contains R001
```

### Test D

Mark R001 synced.

Expected:

```text
pending list excludes R001
```

### Test E

Restart database.

Expected:

```text
data remains
```

---

# 66. Sync Test Cases

### Successful sync

```text
pending → cloud success → synced
```

### Failed sync

```text
pending → network failure → still pending
```

### Repeated sync

```text
pending → success → retry
```

Expected:

```text
no duplicate cloud discovery
```

---

# 67. Build Verification Matrix

| Check | Frequency |
|---|---|
| compile | every significant merge |
| unit tests | every significant merge |
| debug build | daily |
| app launch | daily |
| integrated smoke test | after feature merge |
| full end-to-end | Sep 24 onward |
| final build | Sep 28 |

---

# 68. Day-by-Day Execution Schedule

## September 13 — Room Foundation

Tasks:

- inspect existing project architecture;
- add Room dependencies if missing;
- create database;
- create entities;
- create DAOs;
- create branch.

Deliverable:

```text
Room database compiles and initializes
```

---

## September 14 — Relic Cache

Tasks:

- implement `RelicDao`;
- implement local relic repository;
- insert canonical relics;
- test retrieval;
- map entity → domain.

Deliverable:

```text
local relic catalog works
```

---

## September 15 — Found Relics

Tasks:

- implement `FoundRelicEntity`;
- implement discovery DAO;
- duplicate prevention;
- progress count;
- local observe APIs.

Deliverable:

```text
local progress works
```

---

## September 16 — Pending Sync

Tasks:

- add `pendingSync`;
- query pending records;
- mark synced;
- define cloud acknowledgement boundary.

Deliverable:

```text
local sync queue works
```

---

## September 17 — Repository Integration

Tasks:

- connect local repository to shared interface;
- integrate with ViewModel;
- replace temporary local mocks.

Deliverable:

```text
Room-backed application data
```

---

## September 18 — M1/M2 Integration

Test:

```text
screen
→ repository
→ Room
```

Deliverable:

```text
progress survives screen changes
```

---

## September 19 — M3/M4 Integration

Test:

```text
location
→ scan
→ fusion
→ successful reveal
→ Room
```

Deliverable:

```text
physical/local vertical slice
```

---

## September 20 — M5 Sync Integration

Connect:

```text
Room pending
→ Firebase
→ mark synced
```

Deliverable:

```text
cloud synchronization
```

---

## September 21 — Offline Testing

Test:

```text
offline discovery
restart
network restore
sync
```

Deliverable:

```text
offline-safe progress
```

---

## September 22 — Full Integration

Run:

```text
Auth
→ Map
→ Geofence
→ Scan
→ Fusion
→ Reveal
→ Room
→ Firebase
```

Fix integration blockers.

---

## September 23 — Contract Freeze

Confirm:

- entity fields;
- domain mappings;
- repository methods;
- sync semantics;
- user identity strategy;
- cloud/local responsibilities.

---

## September 24 — End-to-End Test

Test R001 from login through leaderboard.

Deliverable:

```text
complete vertical slice
```

---

## September 25 — Multi-Relic Testing

Test:

```text
R001–R006
```

Verify local progress.

---

## September 26 — Multi-User Testing

Test:

```text
User A
User B
```

and local/cloud isolation.

---

## September 27 — Build Stabilization

Priorities:

1. compile failures;
2. crashes;
3. data loss;
4. sync errors;
5. navigation failures;
6. UI polish.

---

## September 28 — Final Build

Confirm:

```text
clean build
install
launch
login
scan
persist
sync
leaderboard
```

Create final build evidence.

---

# 69. Integration Smoke Test

After every major integration, run:

```text
1. Launch
2. Login
3. Open map
4. Open quest
5. Open Scan Mode
6. Complete mocked/real scan
7. Reveal relic
8. Open progress
9. Verify relic found
10. Restart app
11. Verify relic still found
```

If Firebase is integrated:

```text
12. Verify cloud progress
13. Verify leaderboard
```

---

# 70. Definition of Ready

M6 is ready when:

- Android project builds;
- Room dependency can be added;
- shared Relic model is available;
- repository contract is available;
- Firebase sync boundary is defined;
- M1/M2 can use fake local data.

---

# 71. Definition of Done

M6 is done when:

- [ ] Room database created;
- [ ] RelicEntity implemented;
- [ ] FoundRelicEntity implemented;
- [ ] DAOs implemented;
- [ ] local repository implemented;
- [ ] entity/domain mapping implemented;
- [ ] duplicate local discovery prevented;
- [ ] pendingSync implemented;
- [ ] sync boundary with M5 works;
- [ ] offline discovery works;
- [ ] persistence after restart works;
- [ ] multi-user behavior agreed/tested;
- [ ] build verification process established;
- [ ] end-to-end R001 works;
- [ ] full app builds;
- [ ] final integration tested;
- [ ] PR reviewed and merged.

---

# 72. Risks

## Risk 1 — Room schema changes late

Mitigation:

```text
freeze entity contract by Sep 23
```

## Risk 2 — M6 becomes the only integrator

Mitigation:

```text
each member integrates their own feature
```

## Risk 3 — Firebase and Room duplicate responsibilities

Mitigation:

```text
Room = local
Firebase = cloud
```

## Risk 4 — Data lost during offline mode

Mitigation:

```text
pendingSync retained until cloud acknowledgement
```

## Risk 5 — Duplicate discoveries

Mitigation:

```text
deterministic local/cloud IDs
```

## Risk 6 — Multi-user data leakage

Mitigation:

```text
use userId + relicId locally
or explicitly clear/isolate user state
```

## Risk 7 — Build breaks late

Mitigation:

```text
daily build verification
incremental integration
```

---

# 73. If M6 Falls Behind

Priority:

### 1

```text
FoundRelicEntity + DAO
```

### 2

```text
successful discovery persistence
```

### 3

```text
restart persistence
```

### 4

```text
pendingSync
```

### 5

```text
Firebase sync
```

### 6

```text
advanced cache refinement
```

The MVP must never lose a successful relic discovery because the network is unavailable.

---

# 74. What M6 Should Not Overbuild

Do not spend the one-month project on:

- complex offline conflict resolution;
- large-scale cache invalidation;
- elaborate synchronization frameworks;
- background services unless necessary;
- analytics pipelines;
- raw sensor history storage;
- storing every GPS point;
- complicated local event sourcing.

The app has six relics and a small team.

Keep the local data layer reliable and understandable.

---

# 75. Data Retention

Store only durable information needed by the application:

```text
relic configuration
discovered relics
timestamps
sync state
```

Do not persist:

```text
every accelerometer event
every light reading
every GPS update
```

unless a separate academic requirement specifically needs them.

---

# 76. Final Data Flow

Complete intended flow:

```text
Firebase Auth
      ↓
authenticated UID
      ↓
Repository
      ↓
Relic data
      ↓
Room cache
      ↓
M3 map/geofence
      ↓
M4 sensor fusion
      ↓
M2 reveal
      ↓
recordReveal()
      ↓
Room FoundRelicEntity
      ↓
pendingSync = true
      ↓
M5 Firestore
      ↓
success
      ↓
pendingSync = false
      ↓
Leaderboard
```

---

# 77. R001 Acceptance Scenario

The most important M6 test:

```text
User logs in
      ↓
R001 loaded
      ↓
R001 scan completed
      ↓
relic reveal shown
      ↓
Room stores R001
      ↓
app restarts
      ↓
R001 remains found
      ↓
network available
      ↓
R001 syncs to Firestore
      ↓
leaderboard reflects unique count
```

If this works reliably, the local/integration layer is supporting the core MVP.

---

# 78. Final Integration Checklist

## Architecture

- [ ] UI does not directly use Room.
- [ ] UI does not directly use Firebase.
- [ ] Repository boundary is maintained.
- [ ] M6 does not duplicate M5 cloud code.

## Room

- [ ] Database compiles.
- [ ] RelicEntity works.
- [ ] FoundRelicEntity works.
- [ ] DAOs work.
- [ ] Mappers work.

## Progress

- [ ] Discovery stored.
- [ ] Duplicate prevented.
- [ ] Count correct.
- [ ] Restart persistence works.

## Offline

- [ ] Relic cache available.
- [ ] Discovery works offline.
- [ ] Pending state retained.
- [ ] Sync succeeds after reconnection.

## Multi-user

- [ ] user identity strategy agreed.
- [ ] progress isolated.
- [ ] logout/login tested.

## Integration

- [ ] M1 integrated.
- [ ] M2 integrated.
- [ ] M3 integrated.
- [ ] M4 integrated.
- [ ] M5 integrated.

## Build

- [ ] debug build works.
- [ ] install works.
- [ ] app launches.
- [ ] smoke test passes.
- [ ] final build generated.

---

# 79. Suggested Package Structure

Possible structure:

```text
data/
├── local/
│   ├── CampusQuestDatabase.kt
│   ├── dao/
│   │   ├── RelicDao.kt
│   │   └── FoundRelicDao.kt
│   └── entity/
│       ├── RelicEntity.kt
│       └── FoundRelicEntity.kt
│
├── repository/
│   └── LocalQuestRepository.kt
│
└── mapper/
    ├── RelicMapper.kt
    └── FoundRelicMapper.kt
```

The exact package organization should follow the existing project structure.

---

# 80. Testing Device Matrix

M6 should coordinate final tests across available devices.

| Test | Device A | Device B |
|---|---|---|
| Room initialization | ✓ | ✓ |
| Relic cache | ✓ | ✓ |
| Discovery persistence | ✓ | ✓ |
| Restart persistence | ✓ | ✓ |
| Offline discovery | ✓ | ✓ |
| Sync after reconnect | ✓ | ✓ |
| Auth + Room identity | ✓ | ✓ |
| Full R001 flow | ✓ | ✓ |

Sensor-specific testing remains primarily M4's responsibility.

---

# 81. Integration Communication

M6 should communicate blockers in a structured format:

```text
BLOCKER:
R001 completion is not reaching Room.

AFFECTED:
M2, M4, M6

EXPECTED:
recordReveal("R001")

ACTUAL:
no FoundRelicEntity created

LAST WORKING COMMIT:
<commit>

REQUEST:
M2 verify ViewModel callback.
M4 verify reveal event.
```

Avoid vague messages such as:

```text
Room is broken.
```

---

# 82. Daily Integration Check

At the end of each development day, M6 should check:

```text
main builds?
latest integration builds?
critical branches merged?
known blocker?
shared contract changed?
next dependency?
```

A short daily status table is sufficient.

---

# 83. Integration Status Template

```text
DATE:
BUILD:
LAST VERIFIED:

M1:
M2:
M3:
M4:
M5:
M6:

CURRENT BLOCKER:
NEXT INTEGRATION:
```

This can be maintained in the shared team documentation.

---

# 84. Final Deliverables

M6 must provide:

```text
1. CampusQuestDatabase
2. RelicEntity
3. FoundRelicEntity
4. RelicDao
5. FoundRelicDao
6. Local repository
7. Entity/domain mappers
8. Local relic cache
9. Local discovery persistence
10. pendingSync handling
11. Firebase sync integration boundary
12. Offline/retry testing
13. Restart persistence testing
14. Multi-user strategy
15. Integration smoke-test checklist
16. Build verification record
17. End-to-end R001 evidence
18. Final integrated build verification
```

---

# 85. One-Page M6 Summary

```text
M6 OWNS
────────────────────────────────────────
Room database
RelicEntity
FoundRelicEntity
DAOs
Local repository
Local cache
Offline discovery
pendingSync
Sync coordination
Build stability
Integration coordination
End-to-end testing support
────────────────────────────────────────

M6 RECEIVES
────────────────────────────────────────
Relic domain model
Successful reveal events
Firebase cloud interface
Auth identity
────────────────────────────────────────

M6 RETURNS
────────────────────────────────────────
Local relic data
Found relic state
Progress count
Pending sync records
Cloud sync acknowledgement
Stable integrated build
────────────────────────────────────────

IMPORTANT RULES
────────────────────────────────────────
Room = local persistence
Firebase = cloud persistence
M6 does not duplicate M5 Firebase code
M6 does not own sensors
M6 does not own GPS
M6 does not own UI
Every member integrates their own feature
────────────────────────────────────────

CORE OFFLINE RULE
────────────────────────────────────────
Successful discovery must never be lost
just because the network is unavailable.
────────────────────────────────────────

FIRST PRIORITY IF BEHIND
────────────────────────────────────────
Persist R001 locally and make the
complete R001 flow build reliably.
────────────────────────────────────────
```

# 86. Final Handoff Statement

M6 should provide the stable local data foundation that allows Campus Quest to work as an application rather than a collection of independent feature branches.

The intended boundary is:

```text
M2/M4
   ↓
successful reveal
   ↓
Repository
   ↓
M6 Room
   ↓
local progress
   ↓
pendingSync
   ↓
M5 Firebase
   ↓
cloud progress / leaderboard
```

At the same time, M6's integration responsibility must remain coordination rather than ownership of every feature.

The project should integrate incrementally, test the combined application repeatedly, and prioritize a reliable R001 vertical slice before optional refinements.

The local database schema, repository contract, user identity strategy, and synchronization semantics should be frozen by September 23 after all affected members review them.
