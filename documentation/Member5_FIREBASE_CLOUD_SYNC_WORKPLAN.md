# M5 — FIREBASE CLOUD & SYNC WORKPLAN
## Campus Quest — Mobile Application Development

**Document ID:** M5-FIREBASE-CLOUD-SYNC  
**Owner:** M5 — Backend Developer: Firebase  
**Project:** Campus Quest  
**Primary responsibility:** Firebase project setup, Authentication (Creators & Players), Firestore schema (`games`, `checkpoints`, `gamePlayers`, `progress`, `leaderboards`), FCM push notification broadcast on game publish (`/topics/new_games`), cloud repository implementation, security rules, real-time game leaderboards, and reliable synchronization boundaries.

---

# 1. Purpose

This document defines M5's complete implementation responsibility and the interfaces M5 must provide to the rest of the Campus Quest team.

M5 owns the cloud/backend side of the application:

```text
Android Application
      ↓
Repository boundary
      ↓
Firebase Authentication
      ↓
Firestore
      ↓
Users / Relics / Progress / Leaderboard
```

M5 must provide cloud functionality without coupling UI, sensor, location, or Room implementation directly to Firebase.

The intended architecture is:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firestore
```

Firebase is authoritative for shared cloud data. Room is responsible for immediate local/offline persistence and pending synchronization.

---

# 2. M5 Role

## 2.1 Primary responsibility

M5 is responsible for:

1. Creating/configuring the Firebase project.
2. Connecting the Android application to Firebase.
3. Configuring Firebase Authentication.
4. Implementing user authentication flows.
5. Designing and implementing Firestore collections/documents.
6. Creating the relic catalog in Firestore.
7. Supporting user progress storage.
8. Supporting leaderboard data.
9. Implementing the cloud repository implementation.
10. Writing and validating Firestore security rules.
11. Testing authenticated and unauthorized access.
12. Supporting synchronization with M6's local Room layer.
13. Providing stable cloud data contracts.
14. Seeding development/test data.
15. Testing offline/reconnect behavior with M6.
16. Documenting Firebase configuration and security decisions.

## 2.2 M5 does not own

M5 must not directly own:

- Scan UI.
- SensorManager.
- Sensor fusion.
- Google Maps.
- Geofencing.
- Room entities.
- Bottom navigation.
- App theme.
- Final visual design.
- Local pending-sync queue.
- Device sensor calibration.

---

# 3. Cloud Architecture

The intended conceptual architecture is:

```text
                    ┌─────────────────┐
                    │   Android UI    │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │    ViewModel    │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │ Repository      │
                    │ Interface      │
                    └──────┬─────┬────┘
                           │     │
                     local │     │ cloud
                           ↓     ↓
                       Room   Firestore
                                 │
                                 ├── users
                                 ├── relics
                                 ├── progress
                                 └── leaderboard
```

M5 implements the cloud side of the repository boundary.

---

# 4. Firebase Services

The project requires:

```text
Firebase Authentication
Firestore Database
```

Other Firebase products should not be introduced unless the team explicitly needs them.

The MVP should remain focused.

---

# 5. Firebase Authentication

## 5.1 Purpose

Authentication provides a stable Firebase user identity.

The application should not depend on development IDs such as:

```text
U001
U002
U003
```

as Firebase Auth identities.

Firebase generates the real authenticated UID.

Development users can still have display/test identifiers in mock data, but cloud ownership should use the authenticated UID.

---

# 6. Authentication Flow

Conceptually:

```text
Launch
 ↓
Check current Firebase user
 ↓
authenticated?
 ├── yes → Main application
 └── no  → Login
```

Login:

```text
Email
Password
 ↓
Firebase Auth
 ↓
success
 ↓
obtain Firebase UID
 ↓
load/create user profile
 ↓
Main application
```

Logout:

```text
Firebase Auth signOut
 ↓
return to login
```

---

# 7. User Profile

Suggested Firestore structure:

```text
users/{uid}
```

Example fields:

```text
uid
displayName
email
createdAt
```

The UID should normally be the document identifier rather than relying only on a separate `uid` field.

Example:

```text
users/
  abc123FirebaseUid/
    displayName: "Nimal Perera"
    email: "nimal@example.com"
    createdAt: ...
```

The exact email addresses used for development should be test accounts.

---

# 8. Canonical Development Users

The project mock catalog defines:

| Development ID | Name |
|---|---|
| U001 | Nimal Perera |
| U002 | Kavindi Silva |
| U003 | Sahan Fernando |
| U004 | Ayesha Jayasinghe |
| U005 | Test Explorer |

These are development/test identities.

They must not be confused with Firebase-generated UIDs.

M5 should map authenticated users to their profile documents.

---

# 9. Relic Collection

Suggested structure:

```text
relics/{relicId}
```

Example:

```text
relics/R001
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

Light signature:

```text
lightSignature:
    minLux
    maxLux
```

M4 consumes this configuration through the repository/domain layer.

M4 must not directly query Firestore.

---

# 10. Canonical Relic Data

Development catalog:

| ID | Name | Latitude | Longitude | Radius | Light |
|---|---|---:|---:|---:|---:|
| R001 | Founder’s Bell | 6.974850 | 79.915300 | 25m | 180–320 |
| R002 | Scholar’s Compass | 6.975420 | 79.914750 | 25m | 250–450 |
| R003 | Heritage Key | 6.975900 | 79.915650 | 20m | 80–180 |
| R004 | Old Library Seal | 6.976300 | 79.914900 | 30m | 400–650 |
| R005 | Garden Chronicle | 6.974300 | 79.916100 | 25m | 120–250 |
| R006 | Clock Tower Relic | 6.976750 | 79.915700 | 20m | 300–500 |

These coordinates and light signatures are development values and must be physically validated before the final demonstration.

---

# 11. Progress Model

The project needs to record successful relic discoveries.

Conceptual cloud structure:

```text
progress/{uid}/found/{relicId}
```

Example:

```text
progress/
  abc123/
    found/
      R001
        relicId: R001
        foundAt: ...
```

This supports:

```text
user
 ↓
found relics
```

The local Room model can additionally contain:

```text
pendingSync
```

The cloud document does not need a `pendingSync` field.

---

# 12. Cloud vs Local Responsibility

Important distinction:

| Data | Room | Firestore |
|---|---|---|
| Cached relics | yes | authoritative |
| Found relic | immediate local copy | authoritative shared record |
| Pending sync | yes | no |
| User profile | cache if needed | authoritative |
| Leaderboard | cached/read | authoritative |
| Sensor readings | no permanent cloud need | no |
| Temporary scan state | no permanent cloud need | no |

M5 must not require a network connection for every scan operation.

M6 owns immediate local persistence.

---

# 13. Repository Boundary

The shared project contract proposes:

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

This is a conceptual shared interface.

The final code may split this into multiple repositories if that produces a cleaner architecture, for example:

```text
AuthRepository
QuestRepository
ProgressRepository
LeaderboardRepository
```

Any change must be agreed with the team before integration.

---

# 14. Cloud Repository Responsibilities

M5's repository implementation should provide operations such as:

```text
fetch relics
fetch one relic
fetch user profile
create/update user profile
record progress
observe leaderboard
sync local pending records
```

M5 should expose domain-level models rather than leaking Firestore-specific document structures into UI code.

---

# 15. Firebase DTO / Domain Separation

A useful pattern is:

```text
Firestore DTO
      ↓
Mapper
      ↓
Domain model
      ↓
Repository
      ↓
ViewModel
```

For example:

```text
FirestoreRelicDto
        ↓
Relic
```

This prevents the entire application from becoming dependent on Firestore field names.

---

# 16. Firestore Relic DTO

Conceptual model:

```kotlin
data class FirestoreRelicDto(
    val id: String? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val radiusM: Double? = null,
    val lightSignature: LightSignatureDto? = null,
    val rarity: String? = null,
    val lore: String? = null
)
```

The exact Firebase serialization approach can follow the Android/Firebase library version used by the team.

---

# 17. Domain Relic

Conceptually:

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

M4 needs:

```text
id
radiusM
lightSignature
```

M3 needs:

```text
id
lat
lng
radiusM
```

M2 needs:

```text
id
name
rarity
lore
```

This demonstrates why a shared domain model is useful.

---

# 18. Light Signature

Conceptual:

```kotlin
data class LightSignature(
    val minLux: Float,
    val maxLux: Float
)
```

The values must be loaded from the relic configuration.

M5 does not determine the final calibrated values independently.

M4 calibrates sensor behavior and communicates agreed values.

---

# 19. Leaderboard

The project includes a leaderboard.

Conceptual collection:

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

Example:

```text
leaderboard/abc123
    uid: abc123
    displayName: Nimal Perera
    relicsFound: 3
    lastUpdate: ...
```

---

# 20. Leaderboard Update Strategy

For the MVP, a simple strategy is sufficient:

```text
successful reveal
      ↓
record progress
      ↓
update user's relic count
      ↓
leaderboard reflects count
```

The implementation must avoid obvious duplicate-count problems.

If the same relic is revealed multiple times, it should not increase the user's unique relic count multiple times.

---

# 21. Duplicate Discovery Protection

The system should treat a relic as uniquely discovered per user.

Conceptually:

```text
user U
found R001?
 ├── no → create record + increment count
 └── yes → do not count again
```

This is important for leaderboard correctness.

The local Room layer should also avoid creating duplicate successful records.

---

# 22. Timestamp Handling

Use server-compatible timestamp semantics where practical.

Conceptually:

```text
foundAt
createdAt
lastUpdate
```

should be stored consistently.

Avoid storing formatted display strings as the primary timestamp.

Correct:

```text
Timestamp / Date / Instant
```

Then format in the UI.

---

# 23. Firestore Security Principles

M5 owns security rules.

The goal is:

```text
authenticated user
    ↓
can access only allowed data
```

Users should not be able to arbitrarily modify another user's progress.

---

# 24. Security Rules — User Data

Conceptual rule:

```text
users/{uid}
```

should permit the authenticated user to read/write their own profile subject to agreed restrictions.

Example pseudocode:

```text
request.auth != null
&& request.auth.uid == uid
```

The final rule syntax must be validated in Firebase before deployment.

---

# 25. Security Rules — Progress

For:

```text
progress/{uid}/found/{relicId}
```

the authenticated user should only be able to modify their own progress:

```text
request.auth != null
&& request.auth.uid == uid
```

M5 should also consider preventing clients from writing arbitrary leaderboard totals.

---

# 26. Security Rules — Relics

Relics are application configuration.

A sensible MVP approach is:

```text
authenticated users → read
clients → no direct write
```

M5/admin workflow owns writes.

This prevents a client from changing:

```text
coordinates
radius
light signatures
rarity
lore
```

during gameplay.

---

# 27. Security Rules — Leaderboard

Leaderboard data is shared.

Clients should generally be able to:

```text
read leaderboard
```

but should not be trusted to directly set:

```text
relicsFound = 9999
```

The exact secure update architecture must be chosen by M5.

For the MVP, the team should prioritize preventing obvious client-side score manipulation.

---

# 28. Security Rule Validation

Do not assume pseudocode is deployable.

M5 must:

1. write actual Firestore rules;
2. deploy/test them;
3. test authenticated access;
4. test unauthorized access;
5. test cross-user access;
6. test client attempts to modify protected fields.

Record the results.

---

# 29. Required Security Tests

### Test A — Own user profile

```text
User A → User A profile
```

Expected:

```text
allowed according to profile rule
```

### Test B — Other user profile

```text
User A → User B profile
```

Expected:

```text
denied where modification is not permitted
```

### Test C — Own progress

```text
User A → A/R001
```

Expected:

```text
allowed
```

### Test D — Other progress

```text
User A → B/R001
```

Expected:

```text
denied
```

### Test E — Relic modification

Normal client:

```text
change R001 coordinates
```

Expected:

```text
denied
```

### Test F — Leaderboard manipulation

```text
client sets relicsFound = 999
```

Expected:

```text
denied
```

---

# 30. Authentication Error Handling

M5 should map Firebase exceptions into application-level errors.

Examples:

```text
Invalid credentials
Network unavailable
Account already exists
Permission denied
Unknown Firebase error
```

Do not expose raw technical Firebase messages directly to users.

Conceptual:

```kotlin
sealed interface AppError {
    data object InvalidCredentials : AppError
    data object NetworkUnavailable : AppError
    data object PermissionDenied : AppError
    data class Unknown(val cause: Throwable) : AppError
}
```

This is a conceptual model and can be adapted to the project's existing error architecture.

---

# 31. Network Failure

Cloud operations must anticipate:

```text
no internet
slow internet
temporary timeout
Firebase unavailable
```

M5 should not make the entire application crash.

The application should continue using the local layer where possible.

M6 owns offline persistence and pending-sync behavior.

---

# 32. Sync Boundary With M6

The intended flow:

```text
successful scan
      ↓
M6 writes local FoundRelicEntity
      ↓
pendingSync = true
      ↓
cloud available?
      ├── yes → sync to Firestore
      └── no  → remain pending
      ↓
successful cloud write
      ↓
pendingSync = false
```

M5 provides the cloud write operation.

M6 controls the local queue/state.

---

# 33. Sync Idempotency

M5 must support safe repeated sync attempts.

Example:

```text
R001 pending
 ↓
upload
 ↓
network drops before local state changes
 ↓
sync retries
```

The second attempt must not create a second unique discovery.

Using a deterministic document path:

```text
progress/{uid}/found/R001
```

helps make the operation idempotent.

---

# 34. Sync Pending Contract

The shared interface proposes:

```kotlin
suspend fun syncPending()
```

M6 may instead pass pending records into a cloud-specific method.

The important contract is:

```text
M6 owns pending queue
M5 owns cloud write
successful cloud write → acknowledgement
```

Do not allow both M5 and M6 to maintain separate conflicting sync queues.

---

# 35. Cloud Read Strategy

For relic configuration:

```text
Firestore
 ↓
repository
 ↓
Room cache
 ↓
UI/M4/M3
```

A practical MVP approach:

```text
try local cache first
      ↓
if stale/missing → cloud refresh
      ↓
update Room
```

The exact cache policy is M6's responsibility.

M5 must provide reliable cloud retrieval.

---

# 36. Firebase Initialization

M5 must verify:

```text
google-services configuration
Firebase project
application ID/package match
Authentication enabled
Firestore enabled
```

Do not commit secrets or environment-specific credentials incorrectly.

The Android Firebase configuration file must be handled according to the project's repository policy.

---

# 37. Development Firebase Project

M5 should establish a dedicated development/test Firebase environment where possible.

Avoid using production-like data during rapid development.

Development dataset:

```text
6 relics
5 development users
sample progress
sample leaderboard
```

---

# 38. Seed Data

Seed:

```text
R001
R002
R003
R004
R005
R006
```

with:

- name;
- coordinates;
- radius;
- light signature;
- rarity;
- lore.

Seed enough progress to demonstrate leaderboard behavior.

Do not depend on manual data entry immediately before the demo.

---

# 39. Development User Setup

Create test authentication accounts.

Example:

```text
Nimal test account
Kavindi test account
Sahan test account
Ayesha test account
Test Explorer
```

Passwords must not be placed in source code or documentation.

Use secure local/test credential handling.

---

# 40. Firestore Offline Behavior

Firestore may provide its own client-side behavior, but the project still uses Room as the explicit local application persistence layer.

M5 should not replace the Room architecture with an assumption that Firebase offline caching solves all local requirements.

The team needs:

```text
Room → local source for immediate app state
Firestore → cloud shared source
```

---

# 41. Repository Mapping

Conceptual mapping:

```text
QuestRepository.getRelics()
        ↓
Firestore relic query
        ↓
map documents
        ↓
List<Relic>
```

```text
QuestRepository.recordReveal(R001)
        ↓
Firestore progress write
        ↓
acknowledgement
```

```text
observeLeaderboard()
        ↓
Firestore listener/query
        ↓
map documents
        ↓
Flow<List<LeaderboardEntry>>
```

---

# 42. LeaderboardEntry

Conceptual model:

```kotlin
data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val relicsFound: Int,
    val lastUpdate: Instant?
)
```

The exact time type depends on project libraries.

---

# 43. Leaderboard Ordering

Primary ordering:

```text
relicsFound descending
```

Tie-breaking can use:

```text
lastUpdate
```

or another agreed rule.

The tie-breaking rule should be documented if the UI displays ranked positions.

---

# 44. Real-Time Leaderboard

If the team chooses a Firestore snapshot listener:

```text
Firestore change
 ↓
Flow update
 ↓
ViewModel
 ↓
Leaderboard UI
```

This is appropriate for demonstrating live leaderboard changes.

If real-time listening creates unnecessary complexity for the one-month MVP, a refresh/query approach can be used if accepted by the team.

---

# 45. Authentication State Observation

M5 should provide a way for the app to determine:

```text
logged in
logged out
```

The UI should not directly depend on Firebase SDK calls.

Conceptually:

```kotlin
interface AuthRepository {
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun login(...)
    suspend fun register(...)
    suspend fun logout()
}
```

This is a proposed interface.

---

# 46. AuthUser

Domain model:

```kotlin
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)
```

Keep Firebase's `FirebaseUser` object out of UI code where practical.

---

# 47. Registration Flow

If registration is included:

```text
email/password
 ↓
Firebase createUser
 ↓
Firebase UID
 ↓
create users/{uid}
 ↓
Main application
```

If the project uses pre-created test accounts only, M5 can implement login first and keep registration secondary.

---

# 48. Login Testing

Required cases:

```text
valid account
invalid password
unknown account
empty input
network unavailable
logout
re-login
```

The UI error mapping should be handled through the ViewModel/application layer.

---

# 49. Firestore Data Validation

M5 must validate required fields.

For relic:

```text
id != blank
name != blank
lat valid
lng valid
radius > 0
light min <= max
rarity valid
lore present where required
```

Do not allow malformed relic configuration to reach M4/M3 silently.

---

# 50. Coordinates

Latitude:

```text
-90 → +90
```

Longitude:

```text
-180 → +180
```

For this project, the canonical campus coordinates are around:

```text
6.97 latitude
79.91 longitude
```

The exact coordinates remain development values until physical validation.

---

# 51. Firestore Collection Naming

Use consistent names.

Recommended:

```text
users
relics
progress
leaderboard
```

Avoid inconsistent variants such as:

```text
Relic
relic
relicData
quests
treasures
```

unless the team explicitly changes the canonical schema.

---

# 52. Field Naming

Recommended:

```text
displayName
createdAt
foundAt
lastUpdate
relicsFound
radiusM
lightSignature
minLux
maxLux
```

Avoid mixing:

```text
radius
radius_m
radiusMeters
```

in different parts of the app.

---

# 53. Firestore Schema Documentation

M5 must maintain a schema table:

| Collection | Document | Required fields | Client access |
|---|---|---|---|
| users | uid | displayName, email, createdAt | own |
| relics | relicId | id, name, lat, lng, radiusM, lightSignature, rarity, lore | read |
| progress | uid/relicId | relicId, foundAt | own |
| leaderboard | uid | uid, displayName, relicsFound, lastUpdate | read / controlled write |

The exact security rules must accompany this table.

---

# 54. M5 and M4

M4 needs:

```text
relic ID
light signature
radius
```

M5 must ensure cloud relic documents contain these fields consistently.

M5 does not choose sensor weights.

M4 owns:

```text
light normalization
motion classification
fusion
proximity
```

---

# 55. M5 and M3

M3 needs:

```text
lat
lng
radiusM
```

M5 supplies these through the repository/domain model.

M3 owns:

```text
Google Maps
Fused Location Provider
geofencing
distance calculation
```

M5 does not duplicate any location service.

---

# 56. M5 and M2

M2 needs:

```text
name
rarity
lore
progress
leaderboard
```

M5 provides cloud data through repository/domain models.

M2 should not import Firestore classes directly.

---

# 57. M5 and M6

M6 needs:

```text
cloud read/write functions
acknowledgement of successful sync
authentication identity
```

M6 owns:

```text
Room
pendingSync
local cache
sync orchestration
```

The boundary must remain explicit.

---

# 58. Mock-First Development

M5 should not block the frontend while Firebase is being built.

M2/M1 can use:

```text
FakeQuestRepository
FakeAuthRepository
```

M5 can then replace the fake implementation during integration.

This allows:

```text
UI development
+
backend development
```

to proceed in parallel.

---

# 59. Mock Cloud Dataset

M5 should maintain a development dataset that exactly follows the canonical shared catalog.

Example:

```text
R001 Founder’s Bell
R002 Scholar’s Compass
R003 Heritage Key
R004 Old Library Seal
R005 Garden Chronicle
R006 Clock Tower Relic
```

Do not introduce a second conflicting dataset.

---

# 60. Backend Test Matrix

| Area | Test |
|---|---|
| Auth | valid login |
| Auth | invalid credentials |
| Auth | logout |
| Users | profile creation |
| Users | profile retrieval |
| Relics | six relics load |
| Relics | malformed data rejected/handled |
| Progress | first discovery |
| Progress | duplicate discovery |
| Progress | cross-user access denied |
| Leaderboard | ordering |
| Leaderboard | duplicate prevention |
| Security | unauthorized writes denied |
| Sync | retry after network failure |

---

# 61. Integration Test — R001

Required backend integration scenario:

```text
login as test user
 ↓
load R001
 ↓
scan succeeds
 ↓
record R001
 ↓
Firestore progress/R001 exists
 ↓
leaderboard count updates
 ↓
reload app
 ↓
R001 remains discovered
```

This is one of the key MVP acceptance tests.

---

# 62. Duplicate R001 Test

Perform:

```text
discover R001
discover R001 again
```

Expected:

```text
one unique progress record
leaderboard count increases only once
```

The local and cloud layers must agree.

---

# 63. Multi-User Test

Use two accounts:

```text
User A discovers R001
User B discovers R002
```

Expected:

```text
A → R001
B → R002
```

Each user's progress remains separate.

Leaderboard:

```text
A = 1
B = 1
```

---

# 64. Security Test Before Demo

M5 must demonstrate:

```text
User A cannot modify User B's progress.
Client cannot change relic coordinates.
Client cannot arbitrarily increase leaderboard score.
```

If these tests fail, security rules are not ready.

---

# 65. Error/Loading States

M5 must provide enough repository-level information for UI to represent:

```text
Loading
Success
Empty
Network error
Permission denied
Unexpected error
```

The exact UI is M1/M2 responsibility.

---

# 66. Logging

Development logs may include:

```text
AUTH_SUCCESS uid=...
RELIC_FETCH count=6
PROGRESS_WRITE relic=R001
SYNC_SUCCESS relic=R001
LEADERBOARD_UPDATE count=...
```

Do not log passwords, tokens, or unnecessary personal data.

Disable or reduce verbose backend logs in final builds.

---

# 67. Secrets and Credentials

Never commit:

```text
passwords
API secrets
service-account private keys
```

Test account passwords must remain outside source code.

The standard Android Firebase configuration must follow repository/project policy.

If service-account credentials are ever needed for administrative seeding, they must not be bundled into the mobile application.

---

# 68. Admin Data Management

Relic configuration should be treated as controlled data.

During development, M5 may use:

```text
Firebase Console
seed script/tool
```

depending on the chosen workflow.

The mobile client should not have unrestricted administrative write permissions.

---

# 69. Firestore Query Design

Avoid downloading unrelated data unnecessarily.

Examples:

```text
load relic catalog
query user's progress
observe leaderboard
```

Queries should be limited to what the screen/use case requires.

The MVP has only six relics, so a full relic catalog read is acceptable.

Do not prematurely optimize a six-document dataset into a complicated backend architecture.

---

# 70. Firebase Cost Awareness

The project is small.

M5 should:

- avoid unnecessary repeated listeners;
- avoid writing the same progress repeatedly;
- avoid polling every second;
- use deterministic document IDs;
- cache data locally through M6;
- unsubscribe listeners when screens are no longer active.

The goal is reliable academic implementation, not production-scale infrastructure.

---

# 71. Firebase Emulator

If practical, use Firebase Emulator Suite or equivalent testing tools for security-rule validation.

If unavailable or too time-consuming, perform controlled testing in the development Firebase project.

The requirement is validation, not a specific testing tool.

---

# 72. Firestore Security Rule Status

M5 must mark rules as:

```text
DRAFT
TESTED
DEPLOYED
```

Do not describe draft pseudocode as production-ready security.

---

# 73. Day-by-Day Execution Schedule

## September 13 — Firebase Setup

Tasks:

- create/select Firebase project;
- connect Android app;
- enable Authentication;
- enable Firestore;
- verify build.

Deliverable:

```text
Android app successfully connected to Firebase
```

---

## September 14 — Authentication

Tasks:

- implement Firebase Auth repository;
- login;
- auth-state observation;
- logout;
- test account.

Deliverable:

```text
login/logout works
```

---

## September 15 — User Profiles

Tasks:

- create `users/{uid}`;
- map Auth user to profile;
- load profile;
- test first-login creation.

Deliverable:

```text
authenticated user has cloud profile
```

---

## September 16 — Relics

Tasks:

- create canonical six relic documents;
- implement relic retrieval;
- implement DTO/domain mapping;
- test R001–R006.

Deliverable:

```text
six relics load from Firestore
```

---

## September 17 — Progress

Tasks:

- implement found relic path;
- deterministic relic document ID;
- duplicate protection;
- timestamps.

Deliverable:

```text
recordReveal works
```

---

## September 18 — Leaderboard

Tasks:

- implement leaderboard model;
- update count;
- observe/query leaderboard;
- test ordering.

Deliverable:

```text
leaderboard works
```

---

## September 19 — Repository Integration

Tasks:

- connect shared `QuestRepository`;
- replace mock cloud implementation;
- test M2/M6 interfaces.

Deliverable:

```text
repository boundary working
```

---

## September 20 — Sync With M6

Tasks:

- define pending record handoff;
- implement cloud acknowledgement;
- test offline → online retry.

Deliverable:

```text
pending local discovery can sync
```

---

## September 21 — Security Rules

Tasks:

- user access rules;
- progress access rules;
- relic read protection;
- leaderboard protection;
- cross-user tests.

Deliverable:

```text
security rules tested
```

---

## September 22 — Integration Testing

Test:

```text
login
→ relic load
→ scan
→ reveal
→ progress
→ leaderboard
```

Deliverable:

```text
R001 cloud-backed end-to-end path
```

---

## September 23 — Contract Freeze

Freeze:

```text
collection names
field names
domain models
repository methods
sync semantics
security assumptions
```

Document any changes.

---

## September 24 — Full Integration

Work with:

- M1;
- M2;
- M3;
- M4;
- M6.

Test the complete application.

---

## September 25 — Multi-User Testing

Test:

```text
two users
multiple relics
duplicate discovery
leaderboard
```

---

## September 26 — Offline/Recovery Testing

Test:

```text
online
offline
discover
close app
restore network
sync
```

---

## September 27 — Security and Reliability Fixes

Prioritize:

1. security;
2. duplicate data;
3. failed sync;
4. auth failures;
5. crashes;
6. minor polish.

---

## September 28 — Final Freeze

Confirm:

```text
Firebase connected
Auth works
6 relics available
progress works
leaderboard works
security tested
offline sync works
R001 end-to-end works
```

---

# 74. M5 → M1 Handoff

M1 needs:

```text
auth state
display name
login success/failure state
```

M1 does not need Firebase SDK access.

---

# 75. M5 → M2 Handoff

M2 needs:

```text
Relic
progress
leaderboard
auth/profile information where required
```

M2 must not directly query Firestore.

---

# 76. M5 → M3 Handoff

M3 needs:

```text
lat
lng
radiusM
relicId
```

The location module remains M3-owned.

---

# 77. M5 → M4 Handoff

M4 needs:

```text
lightSignature
relicId
```

M4 owns all sensor interpretation.

---

# 78. M5 → M6 Handoff

M6 needs:

```text
cloud write operation
cloud read operation
auth UID
sync acknowledgement
```

M6 owns Room and local queue state.

---

# 79. Git Branch

M5 branch:

```text
feature/firebase-sync
```

No direct push to `main`.

---

# 80. Suggested Commits

```text
feat: connect Firebase project
feat: add Firebase authentication
feat: add user profile repository
feat: add Firestore relic catalog
feat: add progress repository
feat: add leaderboard repository
feat: add cloud repository implementation
feat: add sync acknowledgement
test: add Firebase repository tests
test: add security rule tests
docs: document Firebase schema
fix: prevent duplicate progress records
```

---

# 81. Pull Request Checklist

Before PR:

- [ ] project builds;
- [ ] authentication tested;
- [ ] six relics load;
- [ ] progress writes;
- [ ] duplicates prevented;
- [ ] leaderboard works;
- [ ] security rules tested;
- [ ] sync boundary agreed;
- [ ] no direct UI Firebase calls;
- [ ] no Room code in Firebase classes;
- [ ] no sensor/location code in Firebase classes;
- [ ] README/configuration updated where required.

---

# 82. Definition of Ready

M5 is ready when:

- Firebase project is available;
- Android application ID is known;
- shared domain models are available or mocked;
- canonical relic catalog is known;
- repository boundary is agreed;
- test accounts can be created.

---

# 83. Definition of Done

M5 is done when:

- [ ] Firebase project connected;
- [ ] Authentication implemented;
- [ ] user profile implemented;
- [ ] six relics seeded;
- [ ] relic repository implemented;
- [ ] progress implemented;
- [ ] duplicate discovery handled;
- [ ] leaderboard implemented;
- [ ] cloud repository integrated;
- [ ] M6 sync boundary implemented;
- [ ] security rules deployed/tested;
- [ ] cross-user access tested;
- [ ] offline/retry behavior tested;
- [ ] R001 cloud-backed end-to-end flow works;
- [ ] documentation updated;
- [ ] PR reviewed and merged.

---

# 84. Risks

## Risk 1 — Firebase configuration failure

Mitigation:

```text
connect Firebase on Sep 13
```

Do not postpone configuration.

## Risk 2 — Authentication blocks the team

Mitigation:

```text
M1/M2 continue with fake repository
```

## Risk 3 — Firestore schema changes late

Mitigation:

```text
freeze schema by Sep 23
```

## Risk 4 — Duplicate progress

Mitigation:

```text
deterministic document IDs
```

## Risk 5 — Leaderboard manipulation

Mitigation:

```text
restrict client writes
```

## Risk 6 — Offline sync complexity

Mitigation:

```text
M6 owns local queue
M5 owns cloud acknowledgement
```

## Risk 7 — Security rules left until the end

Mitigation:

```text
draft early
test by Sep 21
```

---

# 85. If M5 Falls Behind

Priority:

### 1

```text
Authentication
```

### 2

```text
Relic retrieval
```

### 3

```text
Progress recording
```

### 4

```text
Leaderboard
```

### 5

```text
Security rules
```

### 6

```text
Offline sync refinement
```

The MVP must be able to:

```text
login
→ load relic
→ complete R001
→ persist progress
```

before advanced cloud functionality is polished.

---

# 86. Required Evidence

M5 should collect:

1. Firebase project connection evidence.
2. Authentication success.
3. Firestore six-relic dataset.
4. Successful R001 progress write.
5. Duplicate discovery test.
6. Leaderboard update.
7. Security-rule denial test.
8. Offline/reconnect synchronization test.
9. Multi-user isolation test.

These can support the final report and presentation.

---

# 87. Final M5 Technical Checklist

## Authentication

- [ ] Firebase Auth enabled.
- [ ] Login works.
- [ ] Logout works.
- [ ] Auth state observable.
- [ ] Test accounts available.
- [ ] Errors mapped.

## Users

- [ ] `users/{uid}` implemented.
- [ ] Firebase UID used correctly.
- [ ] Display name stored.
- [ ] Cross-user protection tested.

## Relics

- [ ] `relics/{relicId}` implemented.
- [ ] R001–R006 seeded.
- [ ] coordinates correct as development values.
- [ ] radius available.
- [ ] light signature available.
- [ ] lore available.
- [ ] read access tested.

## Progress

- [ ] `progress/{uid}/found/{relicId}` implemented.
- [ ] timestamp stored.
- [ ] duplicate prevented.
- [ ] cross-user protection tested.

## Leaderboard

- [ ] leaderboard model implemented.
- [ ] count updates.
- [ ] duplicates do not inflate count.
- [ ] ordering works.
- [ ] unauthorized score manipulation blocked.

## Sync

- [ ] M6 boundary defined.
- [ ] pending records can be uploaded.
- [ ] retry is safe.
- [ ] successful upload acknowledged.

## Security

- [ ] actual rules deployed.
- [ ] own-user access tested.
- [ ] cross-user access tested.
- [ ] relic writes protected.
- [ ] leaderboard writes protected.

---

# 88. Final Deliverables

M5 must provide:

```text
1. Firebase project configuration
2. Firebase Authentication
3. AuthRepository / equivalent
4. User profile implementation
5. Firestore relic collection
6. Canonical six relic records
7. Progress implementation
8. Leaderboard implementation
9. Cloud repository implementation
10. Sync/cloud acknowledgement interface
11. Firestore security rules
12. Security test evidence
13. Offline/retry integration evidence
14. Firebase schema documentation
15. Test account setup
16. R001 cloud-backed end-to-end demonstration
```

---

# 89. One-Page M5 Summary

```text
M5 OWNS
────────────────────────────────────────
Firebase project
Authentication
User profiles
Firestore relics
Cloud progress
Leaderboard
Cloud repository
Security rules
Cloud sync boundary
────────────────────────────────────────

M5 RECEIVES
────────────────────────────────────────
Domain/repository contracts
Relic model
Local pending records from M6
────────────────────────────────────────

M5 RETURNS
────────────────────────────────────────
Authenticated UID
Relic data
Progress cloud writes
Leaderboard data
Sync acknowledgement
────────────────────────────────────────

IMPORTANT RULES
────────────────────────────────────────
Firebase UID ≠ development ID
M5 does not own Room
M5 does not own sensors
M5 does not own GPS
M5 does not put Firebase calls in UI
Client must not freely manipulate leaderboard totals
────────────────────────────────────────

CLOUD MODEL
────────────────────────────────────────
users/{uid}
relics/{relicId}
progress/{uid}/found/{relicId}
leaderboard/{uid}
────────────────────────────────────────

FIRST PRIORITY IF BEHIND
────────────────────────────────────────
Login → load R001 → record discovery.
────────────────────────────────────────
```

# 90. Final Handoff Statement

M5's implementation should make Firebase an infrastructure layer behind the repository boundary rather than a dependency spread throughout the Android application.

The intended boundary is:

```text
M1/M2
   ↓
ViewModel
   ↓
Repository
   ↓
M5 Firebase implementation
   ↓
Firebase Auth / Firestore
```

while local persistence remains:

```text
Repository
   ↓
M6 Room implementation
```

The most important integration target is a reliable:

```text
authenticated user
→ R001 loaded
→ scan completed
→ progress stored
→ leaderboard updated
→ progress remains available after restart
```

Security rules must be tested before the final integration freeze. The canonical relic coordinates and light signatures remain development values until physical validation/calibration is completed by the appropriate team members.
