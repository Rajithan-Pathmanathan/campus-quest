# Campus Quest — Firebase Schema, Endpoints & Security Specification

**Document ID:** CQ-SHARED-03  
**Document:** Firebase Schema, Endpoints & Security  
**Project:** Campus Quest  
**Purpose:** Define the cloud data model, authentication flow, repository-facing operations, synchronization rules, and security responsibilities for M5 and all dependent members  
**Development window:** 13 September 2026 – 28 September 2026  
**Status:** Team working specification  
**Depends on:** `07_SHARED_CONTRACTS_AND_AGREEMENTS.md` and `08_MOCK_DATA_CATALOG.md`

---

# 1. Purpose

This document defines how Campus Quest uses Firebase as the cloud backend.

The intended architecture is:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Firebase / Room
```

The UI and ViewModels must not directly contain Firestore implementation details.

Firebase is responsible for cloud-backed:

- authentication
- user identity/profile data
- relic definitions
- progress/discovery records
- leaderboard data
- cloud synchronization

Room remains the local persistence mechanism.

The repository is the boundary between the application and these storage mechanisms.

---

# 2. Firebase Responsibilities

M5 owns the Firebase implementation.

M5 is responsible for:

```text
Firebase project setup
Firebase Authentication
Cloud Firestore
Firestore collections
Firestore document structure
security rules
seed data
cloud repository implementation
leaderboard persistence
progress persistence
sync operations
Firebase error handling
```

M6 works with M5 on:

```text
Room ↔ Firestore synchronization
offline records
pendingSync handling
duplicate discovery prevention
integration testing
```

M1 and M2 consume cloud data through the repository.

M3 consumes relic location data through the repository rather than directly reading Firestore.

M4 consumes relic light-signature data through the repository rather than directly reading Firestore.

---

# 3. Firebase Services

The core Firebase services required by the project are:

```text
Firebase Authentication
Cloud Firestore
```

No additional Firebase service should be added unless it is actually needed by the project.

The team should avoid expanding the backend unnecessarily during the limited development period.

---

# 4. Authentication

## 4.1 Authentication goal

Users must be able to:

```text
Open app
 ↓
Login
 ↓
Become authenticated
 ↓
Access main application
```

The authentication implementation should expose application-level state rather than forcing UI code to understand Firebase SDK objects.

---

# 5. Authentication Contract

The application should expose a repository-level contract similar to:

```kotlin
interface AuthRepository {

    fun observeAuthState(): Flow<AuthState>

    suspend fun signIn(
        email: String,
        password: String
    ): Result<User>

    suspend fun signOut()

    fun currentUser(): User?
}
```

This is an implementation contract for the team.

The exact Kotlin types can be adjusted during implementation as long as the responsibilities remain unchanged.

---

# 6. Authentication States

The application should support at least:

```text
LOGGED_OUT
LOADING
AUTHENTICATED
ERROR
```

Example:

```kotlin
sealed class AuthState {
    data object LoggedOut : AuthState()

    data object Loading : AuthState()

    data class Authenticated(
        val user: User
    ) : AuthState()

    data class Error(
        val message: String
    ) : AuthState()
}
```

The UI should observe this state.

It should not repeatedly call Firebase directly from Activity/Fragment code.

---

# 7. Authentication User Data

The canonical application user model is:

```text
User
- id
- displayName
- email
- createdAt
```

Example:

```json
{
  "id": "U001",
  "displayName": "Nimal Perera",
  "email": "nimal.test@campusquest.test",
  "createdAt": "2026-09-13T09:00:00"
}
```

---

# 8. Authentication and Firestore User Identity

The authentication provider's user identifier should be the authoritative identity for cloud operations.

During early development, the mock catalog uses:

```text
U001
U002
U003
U004
U005
```

When real Firebase Authentication generates its own UID, the team must not assume that the Firebase UID will literally be `U001`.

Instead:

```text
Firebase Auth UID
        ↓
User document ID
        ↓
progress/{uid}/found
        ↓
leaderboard.uid
```

The mock IDs are development identifiers, not a guarantee about production Firebase-generated UIDs.

If the team intentionally configures deterministic test IDs, document that decision before implementation.

---

# 9. Firestore Collection Structure

Core collections:

```text
users
relics
progress
leaderboard
```

Stretch-only collection:

```text
relayPairs
```

The core application should not depend on `relayPairs`.

---

# 10. Users Collection

Path:

```text
/users/{uid}
```

Example:

```text
/users/U001
```

Fields:

```text
displayName
email
createdAt
```

Example:

```json
{
  "displayName": "Nimal Perera",
  "email": "nimal.test@campusquest.test",
  "createdAt": "2026-09-13T09:00:00"
}
```

---

# 11. User Document Rules

A user document should represent the authenticated user's profile.

Do not store:

```text
password
```

inside Firestore.

Authentication credentials belong to Firebase Authentication.

---

# 12. Relics Collection

Path:

```text
/relics/{relicId}
```

Example:

```text
/relics/R001
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

Example:

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

---

# 13. Relic Ownership

Relics are shared quest definitions.

They are not owned by an individual user.

Therefore:

```text
R001
```

represents the same relic for every user.

User-specific discovery status belongs under:

```text
progress/{uid}/found
```

not inside the relic document.

---

# 14. Progress Collection

Recommended structure:

```text
/progress/{uid}/found/{relicId}
```

Example:

```text
/progress/U001/found/R001
```

This structure makes the discovery naturally user-specific.

---

# 15. Found Relic Document

Fields:

```text
relicId
foundAt
```

Example:

```json
{
  "relicId": "R001",
  "foundAt": "2026-09-20T10:30:00"
}
```

The local Room entity additionally tracks:

```text
pendingSync
```

Do not require `pendingSync` to be a cloud field.

It is primarily a local synchronization state.

---

# 16. Why pendingSync Is Local

The application needs to know:

```text
Has this local discovery been uploaded yet?
```

That is a synchronization concern.

Therefore:

```text
Room:
FoundRelicEntity
- relicId
- foundAt
- pendingSync
```

Cloud:

```text
progress/{uid}/found/{relicId}
- relicId
- foundAt
```

This avoids mixing local synchronization mechanics into the canonical cloud discovery record.

---

# 17. Discovery Document ID

Use:

```text
relicId
```

as the discovery document ID.

Example:

```text
progress/U001/found/R001
```

This provides a natural uniqueness constraint.

The same user cannot create multiple cloud documents for the same relic using the same path.

---

# 18. Duplicate Discovery

If:

```text
U001
```

discovers:

```text
R001
```

then a second discovery attempt should not create:

```text
R001-copy
R001-2
FOUND-002
```

The canonical cloud record remains:

```text
progress/U001/found/R001
```

The local database should use an equivalent uniqueness rule.

---

# 19. Leaderboard Collection

Path:

```text
/leaderboard/{uid}
```

Fields:

```text
uid
displayName
relicsFound
lastUpdate
```

Example:

```json
{
  "uid": "U001",
  "displayName": "Nimal Perera",
  "relicsFound": 3,
  "lastUpdate": "2026-09-22T14:20:00"
}
```

---

# 20. Leaderboard Source of Truth

The leaderboard should represent cloud-backed progress.

The application should not treat a locally calculated UI number as the authoritative leaderboard.

Recommended conceptual flow:

```text
Discovery
 ↓
Room save
 ↓
Cloud sync
 ↓
Cloud progress
 ↓
Leaderboard update
 ↓
Leaderboard observation
```

The exact transaction/batch strategy may be selected by M5 and M6.

---

# 21. Leaderboard Ordering

The UI should display users in descending order of:

```text
relicsFound
```

Example:

```text
5
4
3
2
0
```

For ties, the team should use a deterministic secondary ordering, such as display name or last update, and keep it consistent.

Do not allow each screen to invent its own tie-breaking rule.

---

# 22. Stretch Relay Pairs

Optional collection:

```text
/relayPairs/{pairId}
```

Possible fields:

```text
memberA
memberB
createdAt
status
```

This feature is stretch scope.

If the core application is behind schedule:

```text
DO NOT IMPLEMENT
```

The core Firebase schema must work without it.

---

# 23. Firestore Data Relationships

Conceptually:

```text
users
  │
  │ uid
  ↓
progress/{uid}/found/{relicId}
  │
  │ relicId
  ↓
relics/{relicId}

users
  │
  │ uid
  ↓
leaderboard/{uid}
```

This means:

- user identity comes from Auth
- user profile comes from `users`
- shared quest definitions come from `relics`
- personal discoveries come from `progress`
- ranking information comes from `leaderboard`

---

# 24. Repository Operations

The shared repository should provide application-level operations.

Minimum operations:

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

This interface deliberately hides whether data comes from:

```text
Room
Firestore
network
mock repository
```

---

# 25. Repository Data Flow

For relic definitions:

```text
ViewModel
 ↓
QuestRepository
 ↓
Room/cache if available
 ↓
Firestore when required
 ↓
Relic
 ↓
ViewModel
 ↓
UI
```

For discovery:

```text
Scan success
 ↓
ViewModel
 ↓
QuestRepository.recordReveal()
 ↓
Room
 ↓
pendingSync
 ↓
Firestore
```

---

# 26. M3 Firebase Dependency

M3 needs:

```text
Relic.id
Relic.lat
Relic.lng
Relic.radiusM
```

M3 does not need to know:

```text
Firestore collection names
Firebase SDK classes
Firestore document references
```

M3 should receive a `Relic` domain object from the repository.

---

# 27. M4 Firebase Dependency

M4 needs:

```text
Relic.id
LightSignature.min
LightSignature.max
```

M4 should not read Firestore directly.

Expected flow:

```text
Repository
 ↓
Relic
 ↓
SensorFusionEngine
```

This maintains separation of concerns.

---

# 28. M2 Firebase Dependency

M2 needs:

```text
Relic.name
Relic.lore
Relic.rarity
Relic progress state
FusionResult
```

M2 should not contain Firebase queries.

---

# 29. M1 Firebase Dependency

M1 primarily needs:

```text
User
AuthState
```

The application shell should react to authentication state.

M1 should not manage Firestore persistence.

---

# 30. M6 Firebase Dependency

M6 needs:

```text
FoundRelicEntity
pendingSync
syncPending()
```

M6 works with M5 to connect:

```text
Room
 ↕
Repository
 ↕
Firestore
```

---

# 31. Firebase Seed Data

The initial Firestore dataset should contain:

```text
R001
R002
R003
R004
R005
R006
```

using the canonical values from:

`08_MOCK_DATA_CATALOG.md`

Do not create a different production-like dataset during the development phase unless the team agrees to update the shared catalog.

---

# 32. Canonical Relic Seed Table

| ID | Name | Latitude | Longitude | Radius | Light Min | Light Max |
|---|---|---:|---:|---:|---:|---:|
| R001 | Founder's Bell | 6.974850 | 79.915300 | 25 | 180 | 320 |
| R002 | Scholar's Compass | 6.975420 | 79.914750 | 25 | 250 | 450 |
| R003 | Heritage Key | 6.975900 | 79.915650 | 20 | 80 | 180 |
| R004 | Old Library Seal | 6.976300 | 79.914900 | 30 | 400 | 650 |
| R005 | Garden Chronicle | 6.974300 | 79.916100 | 25 | 120 | 250 |
| R006 | Clock Tower Relic | 6.976750 | 79.915700 | 20 | 300 | 500 |

These values are development data and require physical validation before final demonstration.

---

# 33. Authentication Setup Procedure

M5 should perform:

```text
1. Create/select Firebase project
2. Add Android application
3. Configure package/application identity
4. Add Firebase configuration
5. Enable Authentication provider required by the project
6. Create development test users
7. Verify sign-in
8. Create user profile records
9. Verify sign-out
10. Verify auth persistence
```

Do this early.

Do not wait until the integration week to discover that authentication configuration is broken.

---

# 34. Firestore Setup Procedure

M5 should:

```text
1. Enable Cloud Firestore
2. Select appropriate development mode/environment
3. Create/seed relic documents
4. Create test user documents
5. Create test leaderboard documents
6. Test progress writes
7. Test progress reads
8. Test query ordering
9. Implement security rules
10. Test rules with authorized and unauthorized access
```

Security rules must not be postponed until the final day.

---

# 35. Firebase Development Environment

During development, the team should clearly distinguish:

```text
development Firebase project/data
```

from:

```text
final demonstration data
```

If the team uses the Firebase Emulator Suite, it may be used for deterministic local testing.

If the emulator is not used, M5 must provide a clearly documented development Firebase environment.

Do not let every developer create separate Firebase projects without agreement.

---

# 36. Firebase Configuration Sharing

Do not commit private credentials or secrets into source control.

The team should follow the project's agreed Firebase configuration strategy.

The Android Firebase configuration file may be required by the application, but any sensitive credential handling must follow the team's repository/security policy.

Never commit:

```text
service account private keys
private JSON credentials
passwords
API secrets not intended for client distribution
```

---

# 37. Firestore Security Model

The basic security principle is:

```text
Authenticated users
        ↓
can access allowed application data
```

and:

```text
Unauthenticated users
        ↓
must not access private user progress
```

More specifically:

- users should only modify their own user/profile data where writes are allowed;
- users should only write their own progress;
- users should not modify arbitrary users' progress;
- relic definitions should not be writable by normal clients;
- leaderboard writes should be controlled rather than freely editable by clients.

---

# 38. Proposed Firestore Security Rules Concept

The exact Firebase Rules syntax must be validated by M5 before deployment.

Conceptually:

```text
users/{uid}
    read: authenticated
    write: only uid == authenticated user

relics/{relicId}
    read: authenticated
    write: admin/development authority only

progress/{uid}/found/{relicId}
    read: only authenticated user matching uid
    create/update: only authenticated user matching uid

leaderboard/{uid}
    read: authenticated
    write: controlled
```

The team must not treat this pseudocode as production-ready rules.

M5 must test the actual deployed rules.

---

# 39. Why Client-Controlled Leaderboard Writes Are Risky

If any client can freely execute:

```text
leaderboard/U001.relicsFound = 999
```

then the leaderboard can be manipulated.

The core application should therefore prefer a design where leaderboard values are derived from legitimate progress or otherwise protected by rules.

For this student project, M5 and M6 should implement the simplest reliable strategy that prevents arbitrary user edits while remaining feasible within the deadline.

---

# 40. Progress Security

A user should not be able to write:

```text
progress/U002/found/R001
```

while authenticated as:

```text
U001
```

The security model should enforce ownership based on the authenticated UID.

---

# 41. Relic Security

Normal users need to read relic definitions.

They should not be able to alter:

```text
lat
lng
radiusM
lightSignature
lore
```

from the application client.

Otherwise a user could modify the quest definition.

---

# 42. User Security

Users may need to update their own:

```text
displayName
```

depending on the final profile feature.

They should not be allowed to arbitrarily change:

```text
uid
```

or another user's profile.

---

# 43. Firestore Error Contract

The repository should convert Firebase-specific failures into application-level errors.

Do not expose raw SDK exceptions throughout the UI.

Example:

```kotlin
sealed class AppError {
    data object NetworkUnavailable : AppError()
    data object PermissionDenied : AppError()
    data object AuthenticationFailed : AppError()
    data object NotFound : AppError()
    data object SyncFailed : AppError()
    data object Unknown : AppError()
}
```

The exact class structure can differ.

The principle is:

```text
Firebase exception
 ↓
Repository
 ↓
AppError
 ↓
ViewModel
 ↓
UI message/state
```

---

# 44. Authentication Error Examples

Map errors to readable states.

Examples:

```text
Invalid credentials
Network unavailable
Account not found
Too many attempts
Unknown authentication error
```

Do not display raw Firebase exception text as the main user experience.

---

# 45. Firestore Read Error

If relic retrieval fails:

```text
Repository
 ↓
error
 ↓
ViewModel
 ↓
UI
```

The UI should provide:

```text
Retry
```

where appropriate.

If cached relic data exists, the application may use the cache according to the local-first strategy.

---

# 46. Firestore Write Error

If a discovery is successfully saved to Room but cloud upload fails:

```text
Room:
pendingSync = true
```

The user should not lose the discovery.

The repository should make the record available for later synchronization.

---

# 47. Sync Operation

The core synchronization operation is:

```kotlin
suspend fun syncPending()
```

Conceptual process:

```text
Query Room for pendingSync = true
 ↓
For each pending discovery
 ↓
Write to Firestore
 ↓
If successful:
    mark pendingSync = false
 ↓
If failed:
    keep pendingSync = true
```

Do not clear `pendingSync` before the cloud write has succeeded.

---

# 48. Sync Ordering

For a discovery:

```text
1. Verify local record
2. Upload cloud progress
3. Confirm success
4. Update local pendingSync
5. Refresh leaderboard where required
```

This avoids claiming that a record was synced when the cloud operation actually failed.

---

# 49. Sync Retry

A failed sync should remain retryable.

Example:

```text
pendingSync = true
```

after failure.

Later:

```text
internet available
 ↓
syncPending()
```

The application should not require the user to rediscover the relic.

---

# 50. Duplicate Sync

If a record is already present in Firestore:

```text
progress/U001/found/R001
```

and Room still believes it is pending, the synchronization operation should be idempotent.

The final state should be:

```text
cloud record exists
pendingSync = false
```

The team should not create duplicate progress records.

---

# 51. Offline-First Discovery

The required behaviour is:

```text
Successful scan
 ↓
Save locally
 ↓
Show discovery
 ↓
Attempt cloud sync
```

If the network is unavailable:

```text
Save locally
 ↓
pendingSync = true
 ↓
continue using app
```

This is important because mobile connectivity can be intermittent.

---

# 52. Cache Strategy

The team should use Room for local persistence.

Possible read strategy:

```text
Room
 ↓
show available local data
 ↓
refresh from Firestore when network is available
 ↓
update Room
```

The exact caching policy can be simplified if time is limited, but the application must preserve discovered progress locally.

---

# 53. Source-of-Truth Clarification

Use this rule:

### Cloud authority

Firestore is authoritative for:

```text
shared relic definitions
cloud user records
cloud progress
cloud leaderboard
```

### Local immediate persistence

Room is authoritative for:

```text
local/offline availability
immediate discovery persistence
pending synchronization state
```

When connected, cloud and local state should converge.

This distinction prevents the team from treating Room and Firestore as competing sources of truth.

---

# 54. Firebase Operations Needed for MVP

M5 must implement:

```text
AUTH-01 Sign in
AUTH-02 Observe auth state
AUTH-03 Sign out

RELIC-01 Get relics
RELIC-02 Get relic/light signature

PROGRESS-01 Record discovery
PROGRESS-02 Read user discoveries

SYNC-01 Upload pending discoveries
SYNC-02 Retry failed uploads

LEADERBOARD-01 Observe leaderboard
LEADERBOARD-02 Update leaderboard through controlled mechanism
```

These labels are internal project references.

---

# 55. Repository Contract Table

| Operation | Consumer | Cloud data |
|---|---|---|
| `getRelics()` | M2/M3 | `relics` |
| `getFusionSignature()` | M4 | `relics.lightSignature` |
| `recordReveal()` | M2/ViewModel | `progress` |
| `observeLeaderboard()` | M2 | `leaderboard` |
| `syncPending()` | M6 | `progress` |
| Auth operations | M1 | `users` + Auth |

---

# 56. M5 Implementation Order

M5 should implement in this order:

```text
1. Firebase project
2. Android connection
3. Authentication
4. Firestore relic collection
5. Relic read operation
6. User document creation/read
7. Progress write
8. Progress read
9. Leaderboard
10. Security rules
11. Error mapping
12. Repository implementation
13. Sync support with M6
14. Integration testing
```

This order minimizes blocked dependencies.

---

# 57. M5 Day-by-Day Plan

## 13 Sep

Deliver:

```text
Firebase project
Android connection
initial Firebase documentation
```

Verify:

```text
App can initialize Firebase
```

---

## 14 Sep

Implement:

```text
Authentication foundation
```

Verify:

```text
login works
logout works
auth state can be observed
```

---

## 15 Sep

Complete:

```text
authentication
users collection
```

M1 should receive a usable AuthRepository contract.

---

## 16 Sep

Implement:

```text
relic collection
getRelics()
```

M3 and M2 should be able to consume mock/real relic data.

---

## 17 Sep

Implement:

```text
light signature retrieval
progress structure
```

Coordinate with M4.

---

## 18 Sep

Implement:

```text
leaderboard
```

Coordinate with M2.

---

## 19 Sep

Implement:

```text
cloud repository implementation
```

Verify:

```text
Mock → real Firebase swap
```

---

## 20–21 Sep

Integrate:

```text
Room
Firebase
Repository
```

with M6.

---

## 22–23 Sep

Verify the full flow:

```text
Login
 ↓
Map
 ↓
Relic
 ↓
Scan
 ↓
Reveal
 ↓
Room
 ↓
Firestore
 ↓
Leaderboard
```

---

## 24–27 Sep

No major backend features.

Focus on:

```text
security
offline
sync
failure handling
duplicate writes
regression testing
```

---

## 28 Sep

Provide:

```text
final Firebase documentation
security evidence
test evidence
demo-ready environment
```

---

# 58. M5 Communication Requirements

M5 must communicate with:

### M1

For:

```text
AuthRepository
AuthState
User
```

### M2

For:

```text
Relic
Lore
Rarity
Progress
Leaderboard
```

### M3

For:

```text
coordinates
radius
```

### M4

For:

```text
light signatures
```

### M6

For:

```text
Room ↔ Firestore sync
pendingSync
progress
```

---

# 59. Required Handoff to M1

M5 should provide:

```text
AuthRepository
AuthState
User model
login result behaviour
logout behaviour
error states
```

M1 should not need to inspect Firebase SDK code.

---

# 60. Required Handoff to M2

M5 should provide:

```text
List<Relic>
user progress
leaderboard Flow
recordReveal()
```

M2 should be able to build the game UI against the interface even if M5's implementation is temporarily unavailable.

---

# 61. Required Handoff to M3

M5 should provide:

```text
Relic(
    id,
    lat,
    lng,
    radiusM
)
```

M3 owns the actual location and geofencing behaviour.

---

# 62. Required Handoff to M4

M5 should provide:

```text
LightSignature(
    min,
    max
)
```

M4 owns sensor readings and fusion calculations.

---

# 63. Required Handoff to M6

M5 should provide:

```text
Firestore progress operations
Firestore write/read behaviour
sync success/failure result
```

M6 owns the local persistence side.

---

# 64. Testing Matrix for Firebase

M5 must test:

| Case | Expected |
|---|---|
| Valid login | Success |
| Invalid login | Readable error |
| Logout | Logged out |
| Read relics | Six relics |
| Read light signature | Correct values |
| Record discovery | Cloud record exists |
| Duplicate discovery | No duplicate record |
| Offline write | Local pending record |
| Retry | Cloud record eventually created |
| Unauthorized progress access | Denied |
| Unauthorized relic write | Denied |
| Unauthorized leaderboard manipulation | Denied/controlled |
| Firebase unavailable | Graceful error |
| App restart | Auth/local state behaves correctly |

---

# 65. Security Test Examples

## Test A — Own progress

Authenticated:

```text
U001
```

Attempt:

```text
progress/U001/found/R001
```

Expected:

```text
Allowed according to application rules
```

---

## Test B — Other user's progress

Authenticated:

```text
U001
```

Attempt:

```text
progress/U002/found/R001
```

Expected:

```text
Denied
```

---

## Test C — Relic modification

Authenticated:

```text
U001
```

Attempt:

```text
relics/R001.radiusM = 9999
```

Expected:

```text
Denied
```

---

## Test D — Leaderboard manipulation

Authenticated:

```text
U001
```

Attempt:

```text
leaderboard/U001.relicsFound = 999
```

Expected:

```text
Denied or controlled by the selected architecture
```

---

# 66. Security Rule Development Procedure

M5 should not write rules once and assume they are correct.

Use:

```text
Write rule
 ↓
Deploy/test
 ↓
Test allowed access
 ↓
Test denied access
 ↓
Test another user's access
 ↓
Test unauthenticated access
 ↓
Record result
```

---

# 67. Avoiding Direct Firebase Access in UI

Bad:

```kotlin
FirebaseFirestore.getInstance()
    .collection("relics")
    .get()
```

inside:

```text
Activity
Fragment
Composable
View
```

Preferred:

```text
UI
 ↓
ViewModel
 ↓
QuestRepository
 ↓
Firebase
```

This follows the module's separation-of-concerns and MVVM approach.

The architecture material explicitly emphasizes that the UI should display data, the ViewModel coordinates UI-related logic, and the repository/data layer hides the underlying storage mechanism. fileciteturn11file0L178-L190

---

# 68. Why the Repository Boundary Matters

Without the repository boundary:

```text
Activity
 ↓
Firebase
 ↓
Room
 ↓
sensor
 ↓
more UI code
```

The application becomes difficult to test and change.

With the boundary:

```text
ViewModel
 ↓
Repository
 ↓
Room / Firebase
```

the implementation can be replaced without rewriting the UI.

This directly supports the MVVM/separation-of-concerns approach taught in the module. fileciteturn11file0L92-L106

---

# 69. Mock-to-Real Replacement

Early development:

```text
ViewModel
 ↓
MockQuestRepository
 ↓
Mock data
```

Later:

```text
ViewModel
 ↓
QuestRepository
 ↓
Room + Firebase
```

The ViewModel should ideally require little or no change.

This is the main reason the shared contracts must be agreed before parallel implementation.

---

# 70. Firebase Integration Definition of Done

M5's backend is considered complete only when:

- [ ] Firebase initializes successfully
- [ ] authentication works
- [ ] user document works
- [ ] six relics are seeded
- [ ] relic retrieval works
- [ ] light signatures are retrievable
- [ ] discovery can be persisted
- [ ] duplicate discovery is handled
- [ ] leaderboard works
- [ ] offline failure is handled
- [ ] pending sync works
- [ ] retry works
- [ ] security rules are deployed
- [ ] security rules have been tested
- [ ] errors are mapped to app-level states
- [ ] repository implementation follows the shared contract
- [ ] M1 can use auth without Firebase code
- [ ] M2 can use quest/progress data
- [ ] M3 can use location data
- [ ] M4 can use light signatures
- [ ] M6 can synchronize Room with Firebase

---

# 71. Final Cloud Architecture

The final conceptual architecture is:

```text
                    ┌───────────────┐
                    │      UI       │
                    └───────┬───────┘
                            ↓
                    ┌───────────────┐
                    │   ViewModel   │
                    └───────┬───────┘
                            ↓
                    ┌───────────────┐
                    │  Repository   │
                    └───────┬───────┘
                            │
                 ┌──────────┴──────────┐
                 ↓                     ↓
             ┌───────┐            ┌───────────┐
             │ Room  │            │ Firestore │
             └───────┘            └───────────┘
                 │                     │
                 └──────────┬──────────┘
                            ↓
                         Sync
```

This structure keeps the application maintainable and testable.

---

# 72. Final Firebase Checklist

## Setup

- [ ] Firebase project created
- [ ] Android app connected
- [ ] Authentication enabled
- [ ] Firestore enabled
- [ ] development environment documented

## Data

- [ ] users collection
- [ ] relics collection
- [ ] progress/{uid}/found
- [ ] leaderboard collection
- [ ] six canonical relics seeded

## Code

- [ ] AuthRepository
- [ ] QuestRepository
- [ ] relic retrieval
- [ ] progress persistence
- [ ] leaderboard observation
- [ ] syncPending
- [ ] error mapping

## Security

- [ ] own progress protected
- [ ] other users' progress protected
- [ ] relic writes protected
- [ ] leaderboard writes protected
- [ ] unauthenticated access tested

## Integration

- [ ] M1 authentication integrated
- [ ] M2 quest/progress integrated
- [ ] M3 relic location integrated
- [ ] M4 light signature integrated
- [ ] M6 Room sync integrated

## Testing

- [ ] online
- [ ] offline
- [ ] retry
- [ ] duplicate
- [ ] permission denial
- [ ] Firebase failure
- [ ] app restart
- [ ] security tests

---

# 73. Important Scope Rule

The team has a limited development period.

Therefore:

```text
Core Firebase
    ↓
MUST WORK

Stretch Firebase
    ↓
ONLY IF CORE IS STABLE
```

Do not spend the final days implementing:

```text
relayPairs
complex analytics
advanced cloud routing
extra social features
```

while the core discovery synchronization is still unreliable.

---

# 74. Relationship With Other Shared Documents

This document defines:

```text
Firebase
Firestore schema
Auth
cloud operations
security
sync responsibilities
```

It does not fully define:

```text
sensor mathematics
GPS implementation
geofencing implementation
Room implementation details
integration sequence
complete testing matrix
Git rules
individual member schedules
```

Those are covered by:

```text
07_SHARED_CONTRACTS_AND_AGREEMENTS.md
08_MOCK_DATA_CATALOG.md
10_SENSOR_FUSION_AND_LOCATION_SPECIFICATION.md
11_INTEGRATION_AND_HANDOFF_PLAN.md
12_TESTING_ACCEPTANCE_AND_DEVICE_MATRIX.md
13_TEAM_GIT_COMMUNICATION_AND_CHANGE_CONTROL.md
```

---

# 75. Final Rule for M5

M5 should think of Firebase as a **service behind the Repository**, not as something the whole application directly talks to.

The correct dependency direction is:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Firebase / Room
```

not:

```text
UI
 ↓
Firebase
 ↓
more Firebase
 ↓
Room
```

This keeps the six-person implementation parallel, testable, and replaceable.
