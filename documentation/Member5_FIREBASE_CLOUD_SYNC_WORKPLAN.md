# MEMBER 5 — FIREBASE, FIRESTORE, AUTHENTICATION & CLOUD BACKEND WORKPLAN
## Campus Quest — Final Reassigned Team Plan

**Owner:** Member 5 (M5)  
**Primary responsibility:** Firebase Authentication, Firestore cloud data, security rules, publishing backend, game membership/progress/leaderboards, and FCM new-game notifications  
**Branch:** `feature/m5-firebase-backend`  
**Architecture:** Android + Kotlin, MVVM + Repository  
**Status:** Final reassignment version

---

# 1. PURPOSE

M5 owns the **cloud/backend side** of Campus Quest.

The cloud layer provides the authoritative shared data required by:

- Game creators.
- Game players.
- Game discovery.
- Game publishing.
- Game membership.
- Checkpoint configuration.
- Player progress.
- Game-specific leaderboards.
- New-game notifications.

M5 is responsible for implementing and securing the Firebase side while exposing a clean repository contract to the Android application.

The intended flow is:

```text
Android UI/ViewModels
        ↓
GameRepository
        ↓
M5 Firebase implementation
        ↓
Firebase Auth
Firestore
FCM
```

M5 does not own the Android UI, sensors, Room, or local synchronization engine.

---

# 2. IMPORTANT REASSIGNMENT

The final six-member allocation assigns M5 to:

- Firebase Authentication.
- Firestore.
- Security rules.
- Game/game-checkpoint cloud persistence.
- Creator ownership.
- Game membership.
- Player progress.
- Game-specific leaderboard backend.
- Publishing state transition.
- FCM new-game notifications.
- Cloud-side validation.
- Firebase repository implementation.
- Cloud integration tests.

M5 does NOT own:

- Creator UI.
- Player UI.
- Navigation shell.
- Location APIs.
- Geofencing.
- Sensors.
- Sensor fusion.
- Scan HUD.
- Reveal UI.
- Room.
- Local offline database.
- Sync engine.

M6 owns Room/repository/local sync responsibilities.

---

# 3. CANONICAL FIRESTORE STRUCTURE

The canonical structure is:

```text
users
games/{gameId}
games/{gameId}/checkpoints/{checkpointId}
gamePlayers/{gameId}_{uid}
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
leaderboards/{gameId}/entries/{uid}
/topics/new_games
```

The exact Firebase implementation can use equivalent supported structures where required, but the logical ownership and scoping must remain the same.

---

# 4. USERS

Conceptual document:

```text
users/{uid}
```

Possible fields:

```text
uid
displayName
email
createdAt
```

Only the fields required by the product should be stored.

M5 must ensure that creator/player identity is derived from Firebase Authentication.

---

# 5. AUTHENTICATION

M5 owns Firebase Authentication integration.

The application must support the authentication method agreed by the project.

M5 provides:

```text
authenticated user ID
authenticated user display name
authentication state
sign-in result
sign-out result
```

M1 consumes authentication state for navigation.

---

# 6. AUTHENTICATION STATE

Suggested abstraction:

```kotlin
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?
)
```

Possible repository interface:

```kotlin
interface AuthRepository {
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun signIn(...): Result<AuthUser>
    suspend fun signOut(): Result<Unit>
}
```

The exact interface must match the shared project architecture.

---

# 7. CREATOR IDENTITY

When a creator creates a game:

```text
Firebase Auth UID
       ↓
game.creatorId
```

Do not allow the client to arbitrarily choose:

```text
creatorId
```

for a new game.

The backend/security rules must verify ownership.

---

# 8. GAME MODEL

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

M5 must persist the game using these logical fields.

---

# 9. CHECKPOINT MODEL

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

The backend must support dynamically created checkpoints.

Do not design the Firestore structure around a permanent six-relic list.

---

# 10. DYNAMIC GAME CREATION

A creator can create:

```text
Game A
  2 checkpoints
```

or:

```text
Game B
  8 checkpoints
```

or another valid number supported by the product rules.

The cloud data model must not require code changes for a new checkpoint.

---

# 11. GAME CREATION FLOW

Conceptually:

```text
Authenticated creator
       ↓
Create Game
       ↓
games/{gameId}
       ↓
status = DRAFT
       ↓
Add checkpoints
       ↓
games/{gameId}/checkpoints/{checkpointId}
```

The game remains a draft until explicitly published.

---

# 12. GAME ID

Game IDs must uniquely identify a game.

M5 should use a safe ID generation strategy.

The ID must not depend on:

```text
R001
R002
```

or another fixed relic naming scheme.

---

# 13. CHECKPOINT ID

Checkpoint IDs must be unique within the game.

The backend should preserve the ID when editing an existing checkpoint.

An update must not accidentally create a new checkpoint.

---

# 14. CHECKPOINT SCOPING

Every checkpoint belongs to one game:

```text
checkpoint.gameId == gameId
```

The Firestore path should make that relationship explicit:

```text
games/{gameId}/checkpoints/{checkpointId}
```

This is a critical data-isolation rule.

---

# 15. GAME OWNERSHIP

Creator operations must be restricted to the authenticated creator who owns the game.

Conceptually:

```text
request.auth.uid == game.creatorId
```

This rule applies to:

- Edit game.
- Add checkpoint.
- Update checkpoint.
- Delete checkpoint.
- Publish game.
- Other creator-only operations.

The exact Firestore rule syntax must match the final data structure.

---

# 16. PLAYER ACCESS

Players should only be able to access published games according to the product rules.

Draft games must not accidentally appear in the public game-discovery list.

The backend must enforce this rather than relying only on UI filtering.

---

# 17. GAME DISCOVERY

Player game discovery should query published games.

Conceptually:

```text
games
WHERE status == PUBLISHED
```

M1 displays the results.

M5 owns the cloud query and access rules.

---

# 18. GAME DETAILS

The repository should provide:

```kotlin
suspend fun getGameDetails(gameId: String): Game?
```

The cloud implementation retrieves the requested game subject to access rules.

---

# 19. GAME CHECKPOINT RETRIEVAL

The repository should provide:

```kotlin
suspend fun getGameCheckpoints(gameId: String): List<Checkpoint>
```

M5 retrieves:

```text
games/{gameId}/checkpoints
```

and returns them in the agreed order.

Do not depend on Firestore document insertion order as the gameplay order.

Use:

```text
order
```

from the checkpoint model.

---

# 20. CHECKPOINT ORDER

The backend should preserve the creator-defined:

```text
order
```

The query should sort according to the agreed order field or the repository should perform deterministic ordering after retrieval.

---

# 21. GAME MEMBERSHIP

Canonical logical record:

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

The membership record identifies that a player has joined a particular game.

---

# 22. JOIN GAME

Repository contract:

```kotlin
suspend fun joinGame(gameId: String): Result<Unit>
```

M5 implements the cloud membership operation.

The operation should be idempotent.

If the user has already joined:

```text
joinGame()
```

should not create multiple logical memberships.

---

# 23. JOIN AUTHORIZATION

The backend should verify:

```text
request.auth != null
game exists
game is joinable/published
```

according to the product lifecycle.

---

# 24. PLAYER PROGRESS

Canonical logical structure:

```text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

This is intentionally scoped by:

```text
user
+
game
+
checkpoint
```

---

# 25. PROGRESS RECORD

Suggested logical fields:

```text
checkpointId
gameId
userId
foundAt
```

Additional fields may be added if explicitly required.

Do not store a global:

```text
foundRelic = true
```

without game/checkpoint scope.

---

# 26. RECORD DISCOVERY

Repository contract:

```kotlin
suspend fun recordDiscovery(
    gameId: String,
    checkpointId: String,
    foundAt: Long
): Result<Unit>
```

M5 implements the cloud operation.

The operation must verify:

- Authenticated user.
- Valid game.
- Valid checkpoint belonging to game.
- Player membership where required.
- Valid discovery/progress transition.

---

# 27. DISCOVERY IDEMPOTENCY

Repeated discovery requests should not create duplicate progress records.

For example:

```text
same user
same game
same checkpoint
```

should resolve to one logical discovery.

This is especially important because:

- Sensor success may be emitted more than once.
- App lifecycle may repeat requests.
- Offline synchronization may retry.
- Network operations may be retried.

---

# 28. DISCOVERY AUTHORITY

M5 should not treat arbitrary client-provided:

```text
foundAt
gameId
checkpointId
```

as automatically trustworthy.

Security rules and backend design must validate ownership and scope.

The project is not expected to provide perfect anti-cheat protection through Firebase alone, but obvious cross-user/cross-game writes must be prevented.

---

# 29. GAME-SPECIFIC LEADERBOARD

There is **no global leaderboard** in the canonical architecture.

Leaderboard structure:

```text
leaderboards/{gameId}/entries/{uid}
```

Every game has its own leaderboard.

---

# 30. LEADERBOARD ENTRY

Conceptual fields:

```text
userId
displayName
foundCount
score
lastUpdatedAt
```

The exact scoring/ranking fields must follow the product specification.

M5 owns the backend representation.

---

# 31. LEADERBOARD ISOLATION

For:

```text
Game A
```

only Game A progress should contribute to:

```text
leaderboards/GameA
```

For:

```text
Game B
```

only Game B progress contributes to:

```text
leaderboards/GameB
```

Never mix them into a global ranking.

---

# 32. LEADERBOARD UPDATE

When a discovery is recorded:

```text
progress updated
       ↓
game-specific leaderboard updated
```

The implementation may use:

- Transaction.
- Cloud Function/event.
- Controlled client write with strict rules.

The selected implementation must prevent unauthorized score manipulation.

---

# 33. LEADERBOARD SECURITY

A client should not be able to freely submit:

```text
score = 999999
```

and become first place.

Where leaderboard values are derived from progress, they should be derived from authoritative progress data or protected by backend validation.

---

# 34. REAL-TIME LEADERBOARD

Repository contract:

```kotlin
fun observeGameLeaderboard(
    gameId: String
): Flow<List<GameLeaderboardEntry>>
```

M1/M4 may consume this for presentation.

M5 provides the Firestore-backed implementation.

---

# 35. GAME PUBLISHING

Repository contract:

```kotlin
suspend fun publishGame(gameId: String): Result<Unit>
```

Publishing is a controlled state transition:

```text
DRAFT → PUBLISHED
```

M5 owns the cloud operation.

---

# 36. PUBLISH VALIDATION

The backend must not rely exclusively on M2's UI validation.

Before allowing publication, validate the required game data.

Conceptually:

```text
valid title
valid description
required checkpoint count
valid checkpoint locations
valid radius
valid light signature
valid checkpoint order
required content
valid gameplay metadata
```

The exact publication requirements must match the canonical project specification.

---

# 37. PUBLISH OWNERSHIP

Only the game creator may publish:

```text
request.auth.uid == game.creatorId
```

An unrelated user must not be able to publish another creator's draft.

---

# 38. PUBLISH ATOMICITY

Avoid leaving the game in a partially published state.

The intended logical outcome is:

```text
game.status = PUBLISHED
game.publishedAt = timestamp
```

together.

Where the chosen Firebase design supports it, use an atomic transaction/batch or equivalent controlled operation.

---

# 39. PUBLISHED GAME DISCOVERY

After:

```text
status = PUBLISHED
```

the game becomes eligible for player discovery.

M1 then displays it through the game-list flow.

---

# 40. FCM NEW-GAME NOTIFICATION

Publishing a game should trigger notification to registered users.

MVP target:

```text
FCM topic:
/topics/new_games
```

Conceptual flow:

```text
Creator publishes
      ↓
Firestore status = PUBLISHED
      ↓
Publication event
      ↓
FCM topic notification
      ↓
/topics/new_games
      ↓
Registered devices
```

---

# 41. NOTIFICATION PAYLOAD

Canonical model:

```kotlin
data class NewGameNotification(
    val gameId: String,
    val gameTitle: String,
    val creatorName: String,
    val checkpointCount: Int,
    val publishedAt: Long
)
```

The notification must contain enough information to route the player to the correct game.

---

# 42. NOTIFICATION DEEP LINK

When a user taps a new-game notification:

```text
Notification
    ↓
gameId
    ↓
Game Details
```

M1 owns the navigation/deep-link UI integration.

M5 owns the cloud notification mechanism and payload.

---

# 43. TOPIC SUBSCRIPTION

Registered users should subscribe to:

```text
/topics/new_games
```

according to the agreed notification implementation.

The application should not create a separate topic per game unless the project explicitly requires it.

---

# 44. NOTIFICATION FAILURE

A notification failure must not invalidate the successful publication itself.

Conceptually:

```text
Game published = authoritative state
Notification = secondary delivery mechanism
```

The UI should not report:

```text
Publish failed
```

solely because notification delivery has a problem, if the game was already successfully published.

---

# 45. REPOSITORY IMPLEMENTATION

The canonical repository boundary is:

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

M5 primarily implements the cloud portion.

M6 integrates local persistence/sync around the repository.

---

# 46. CLOUD REPOSITORY RESPONSIBILITY

M5's implementation should translate:

```text
Repository operation
       ↓
Firestore query/write
       ↓
Firebase result
       ↓
domain/result
```

UI/ViewModels must not need to know Firestore collection paths.

---

# 47. ERROR MAPPING

Map Firebase failures into meaningful repository results.

Examples:

```text
permission denied
not found
network unavailable
already exists
invalid data
unauthenticated
unknown error
```

Do not expose raw SDK exceptions throughout the UI.

---

# 48. AUTHENTICATION ERROR

If a user is unauthenticated:

```text
AuthRepository
```

should report the state.

Creator/player operations requiring authentication should fail cleanly.

---

# 49. FIRESTORE ERROR HANDLING

Possible cases:

```text
Firestore unavailable
permission denied
document not found
invalid query
timeout
```

The repository should return structured failures.

M1/M2/M4 then present appropriate messages.

---

# 50. OFFLINE CLOUD BEHAVIOR

M5 does not own the local offline cache.

M6 owns:

```text
Room
pending writes
sync
retry
```

M5 must provide cloud operations that can safely receive retried requests.

This requires idempotent behavior where appropriate.

---

# 51. SYNC CONTRACT WITH M6

M6 may call M5's cloud implementation during synchronization.

The cloud layer must safely process:

```text
create/update
discovery record
membership
```

without producing duplicates.

M5 should document:

- Idempotency keys/paths.
- Expected conflict behavior.
- Server validation.
- Retry-safe operations.

---

# 52. CONFLICT HANDLING

Potential conflicts include:

```text
local draft changed
cloud draft changed
```

or:

```text
discovery already exists
```

M6 owns synchronization policy.

M5 must return enough information for M6 to resolve or report the conflict.

---

# 53. TIMESTAMPS

Use a consistent timestamp strategy.

Relevant fields include:

```text
createdAt
publishedAt
joinedAt
foundAt
lastUpdatedAt
```

Where server timestamps are preferred by the architecture, use the agreed Firebase timestamp mechanism.

Do not mix incompatible timestamp formats across collections.

---

# 54. SERVER AUTHORITY

Where timing affects leaderboard/progress ordering, prefer authoritative server timestamps where feasible.

Client timestamps may be accepted as part of the MVP only if the project contract allows them.

Do not silently change timestamp semantics.

---

# 55. SECURITY RULES — GENERAL PRINCIPLES

Security rules should enforce:

```text
authentication
ownership
game visibility
membership
game/checkpoint scope
progress scope
leaderboard protection
```

Client-side checks are not sufficient.

---

# 56. USERS SECURITY

A user should only modify their own user document where the product permits user editing.

Conceptually:

```text
request.auth.uid == userId
```

---

# 57. GAMES SECURITY

Creators:

```text
create own game
read own drafts
update own drafts
publish own game
```

Players:

```text
read published games
```

according to the product rules.

---

# 58. CHECKPOINT SECURITY

Creators may manage checkpoints only under games they own.

Players may read checkpoints for games they are authorized to play.

Players must not modify creator checkpoint configuration.

---

# 59. MEMBERSHIP SECURITY

A player should be able to create/manage their own membership record.

One user's membership must not be writable by another user.

---

# 60. PROGRESS SECURITY

A user may create/update their own progress only for games they are allowed to play.

A player must not be able to write:

```text
progress/anotherUser
```

---

# 61. LEADERBOARD SECURITY

Prevent arbitrary client manipulation of ranking values.

Where possible:

```text
progress
    ↓
trusted calculation
    ↓
leaderboard
```

rather than:

```text
client
    ↓
arbitrary score
```

---

# 62. FIRESTORE INDEXING

M5 should identify queries that require indexes.

Likely examples:

```text
games WHERE status == PUBLISHED ORDER BY ...
leaderboard entries ORDER BY score
checkpoints ORDER BY order
```

The exact indexes depend on the implemented query structure.

Keep required index configuration in the project repository.

---

# 63. CLOUD DATA VALIDATION

Validate:

```text
latitude
longitude
radiusM
minLux
maxLux
order
status
creatorId
gameId
checkpointId
```

according to the agreed ranges and relationships.

Do not rely entirely on Android-side validation.

---

# 64. LOCATION DATA VALIDATION

Coordinates must be valid:

```text
latitude ∈ [-90, 90]
longitude ∈ [-180, 180]
```

The checkpoint must belong to the game specified by its path.

---

# 65. RADIUS VALIDATION

Reject:

```text
radius <= 0
```

and any value outside the agreed project range.

M3 consumes the radius for geofencing.

---

# 66. LIGHT SIGNATURE VALIDATION

Validate:

```text
minLux >= 0
maxLux >= 0
minLux <= maxLux
```

and any additional project-defined constraints.

---

# 67. ORDER VALIDATION

Checkpoint ordering must be deterministic.

Reject or normalize invalid ordering according to the project contract.

Avoid duplicate/ambiguous ordering if the gameplay specification requires unique sequence numbers.

---

# 68. GAME STATUS TRANSITIONS

Valid lifecycle should be explicit.

Conceptually:

```text
DRAFT → PUBLISHED
PUBLISHED → CLOSED
```

Do not permit arbitrary:

```text
CLOSED → DRAFT
```

or:

```text
PUBLISHED → DRAFT
```

unless the product explicitly supports it.

---

# 69. CLOSED GAMES

A closed game should remain available for historical progress/leaderboard behavior as defined by the product, while new gameplay/join operations are restricted.

M5 must implement the final lifecycle rules consistently.

---

# 70. DATA CONSISTENCY

When creating/updating related data:

```text
Game
Checkpoint
Membership
Progress
Leaderboard
```

ensure references remain valid.

Avoid orphaned checkpoint records or progress records referencing nonexistent games.

---

# 71. TRANSACTIONS / BATCHES

Use Firestore transactions/batches where multiple related writes must succeed together.

Examples:

```text
publish game metadata
```

or:

```text
discovery + derived leaderboard update
```

where the selected implementation requires atomicity.

---

# 72. CLOUD FUNCTIONS / SERVER-SIDE LOGIC

If the MVP uses Firebase Cloud Functions or an equivalent server-side event mechanism, M5 owns that implementation.

Potential uses:

```text
published game event
      ↓
FCM notification

discovery event
      ↓
leaderboard update
```

The team should avoid adding unnecessary server complexity if a simpler secure MVP implementation is sufficient.

---

# 73. FCM TOKEN MANAGEMENT

If the project requires device-specific token storage, M5 should define the structure and security rules.

For the MVP topic approach, the main requirement is:

```text
/topic/new_games
```

subscription.

Do not store unnecessary device information.

---

# 74. FCM NOTIFICATION CONTENT

Notification should communicate:

```text
New game available
Game title
Creator
Checkpoint count
```

and carry:

```text
gameId
```

for navigation.

---

# 75. FIREBASE CONFIGURATION

M5 is responsible for ensuring:

- Firebase project is correctly configured.
- Android Firebase configuration is present.
- Required Firebase services are enabled.
- Firestore is configured.
- Authentication is configured.
- FCM is configured.

Do not commit secrets or private credentials into the repository.

---

# 76. FIREBASE ENVIRONMENT

The team should distinguish:

```text
development/test
```

from:

```text
production/demo
```

where practical.

Seed data must not be confused with permanent production data.

---

# 77. SEED DATA

The primary demo game may contain:

```text
demo-campus-quest
R001
R002
R003
R004
R005
R006
```

These are seed/demo values.

M5 must ensure the backend remains capable of:

```text
arbitrary game IDs
arbitrary checkpoint IDs
dynamic checkpoint counts
```

---

# 78. SECOND DEMO GAME

Testing should include a second game such as:

```text
demo-science-trail
```

This verifies:

- Game isolation.
- Game-specific progress.
- Game-specific leaderboard.
- Dynamic checkpoint loading.
- Creator/player access boundaries.

---

# 79. MULTI-GAME TEST

Create:

```text
Game A
Game B
```

Verify:

```text
A checkpoints ≠ B checkpoints
A progress ≠ B progress
A leaderboard ≠ B leaderboard
```

---

# 80. MULTI-USER TEST

Test:

```text
Creator A
Creator B
Player A
Player B
```

Verify:

- Creator A cannot edit Creator B's game.
- Player A cannot write Player B's progress.
- Player A cannot modify creator data.
- Leaderboards remain correctly scoped.

---

# 81. REPOSITORY TESTS

Test repository operations:

```text
getAvailableGames
getGameDetails
createGame
createCheckpoint
updateCheckpoint
publishGame
joinGame
getGameCheckpoints
recordDiscovery
observeGameLeaderboard
```

Use Firebase emulator/mocks where appropriate.

---

# 82. AUTH TESTS

Test:

```text
unauthenticated
authenticated player
authenticated creator
sign out
invalid credentials
```

Verify protected operations are rejected when unauthenticated.

---

# 83. FIRESTORE SECURITY RULE TESTS

Security rules should be tested explicitly.

Examples:

```text
creator can update own draft
creator cannot update another creator's game
player can read published game
player cannot modify checkpoint
player can write own progress
player cannot write another user's progress
client cannot arbitrarily modify leaderboard score
```

---

# 84. PUBLISH SECURITY TEST

Attempt:

```text
Creator A → publish Game B owned by Creator B
```

Expected:

```text
DENIED
```

---

# 85. CHECKPOINT SECURITY TEST

Attempt:

```text
Player → update checkpoint
```

Expected:

```text
DENIED
```

---

# 86. PROGRESS SECURITY TEST

Attempt:

```text
User A → write progress/UserB/...
```

Expected:

```text
DENIED
```

---

# 87. LEADERBOARD SECURITY TEST

Attempt:

```text
Client → arbitrary score
```

Expected:

```text
DENIED
```

or safely validated/recalculated.

---

# 88. FCM TEST

Test:

```text
publish game
   ↓
notification event
   ↓
FCM topic
   ↓
device receives notification
   ↓
payload contains gameId
```

The notification must route to the correct game.

---

# 89. NOTIFICATION DEEP-LINK TEST

Given:

```text
gameId = game-alpha
```

when notification is tapped:

```text
Game Details(game-alpha)
```

must open.

M1 owns final navigation behavior.

M5 owns payload correctness.

---

# 90. CLOUD ERROR TESTS

Test:

```text
network unavailable
permission denied
document missing
unauthenticated
duplicate request
invalid game ID
invalid checkpoint ID
```

Verify repository results are meaningful.

---

# 91. ACCEPTANCE TEST — CREATE GAME

### Given

Authenticated creator.

### When

Creator saves a new game.

### Then

Firestore contains a draft game owned by that creator.

---

# 92. ACCEPTANCE TEST — ADD CHECKPOINT

### Given

A creator-owned draft game.

### When

Creator saves a checkpoint.

### Then

The checkpoint exists under:

```text
games/{gameId}/checkpoints/{checkpointId}
```

and contains the configured location, radius, light signature and gameplay metadata.

---

# 93. ACCEPTANCE TEST — PUBLISH

### Given

A valid creator-owned draft.

### When

Creator publishes.

### Then

The game becomes:

```text
PUBLISHED
```

and receives a publication timestamp according to the chosen timestamp policy.

---

# 94. ACCEPTANCE TEST — PLAYER DISCOVERY

### Given

A published game.

### When

A player loads available games.

### Then

The game is discoverable.

Draft games are not presented as publicly published games.

---

# 95. ACCEPTANCE TEST — JOIN

### Given

Authenticated player and published game.

### When

Player joins.

### Then

A game-specific membership record exists.

Repeated join requests do not create duplicate logical memberships.

---

# 96. ACCEPTANCE TEST — DISCOVERY

### Given

A joined player.

### When

M4 requests discovery recording after M3's successful physical scan.

### Then

The player's progress is recorded under the correct:

```text
user
game
checkpoint
```

scope.

---

# 97. ACCEPTANCE TEST — LEADERBOARD

### Given

Two players in one game.

### When

Their checkpoint discoveries differ.

### Then

The game-specific leaderboard reflects the appropriate ranking.

---

# 98. ACCEPTANCE TEST — GAME ISOLATION

### Given

A player has progress in:

```text
Game A
```

### When

The player opens:

```text
Game B
```

### Then

Game A progress does not appear as Game B progress.

---

# 99. ACCEPTANCE TEST — NOTIFICATION

### Given

A creator publishes a game.

### When

The publication event is processed.

### Then

the new-game notification contains:

```text
gameId
gameTitle
creatorName
checkpointCount
publishedAt
```

according to the final payload.

---

# 100. ACCEPTANCE TEST — UNAUTHORIZED CREATOR

### Given

Creator A owns Game A.

### When

Creator B attempts to modify Game A.

### Then

Firebase security rules reject the operation.

---

# 101. ACCEPTANCE TEST — PLAYER CANNOT EDIT

### Given

Player is a member of a game.

### When

Player attempts to change checkpoint configuration.

### Then

The operation is rejected.

---

# 102. OFFLINE SYNC HANDOFF

M6 may submit pending cloud operations after reconnecting.

M5 must ensure cloud operations are:

- Retry-safe.
- Idempotent where required.
- Correctly scoped.
- Able to distinguish already-applied operations from new operations.

---

# 103. CLOUD / LOCAL SOURCE OF TRUTH

For shared authoritative data:

```text
Firebase
```

is the cloud source of truth.

M6 may maintain local cached copies for offline operation.

M5 should not assume Room is authoritative for cloud security decisions.

---

# 104. CLOUD DATA MODEL DOCUMENTATION

M5 must maintain a clear mapping:

```text
Domain Model
      ↓
Firestore Path
      ↓
Firestore Fields
      ↓
Security Rules
      ↓
Repository Method
```

This makes integration easier for M6 and the rest of the team.

---

# 105. SUGGESTED PACKAGE STRUCTURE

A possible Android-side structure:

```text
data/
    firebase/
        FirebaseAuthDataSource.kt
        FirestoreGameDataSource.kt
        FcmNotificationManager.kt

repository/
    FirebaseAuthRepository.kt
    FirebaseGameRepository.kt

model/
    GameDto.kt
    CheckpointDto.kt
    LeaderboardEntryDto.kt
    NotificationPayload.kt
```

Exact package names should follow the existing project.

---

# 106. DTO / DOMAIN MAPPING

If DTOs are used:

```text
Firestore DTO
      ↓
Mapper
      ↓
Domain Game / Checkpoint
```

Do not expose Firestore-specific document snapshots to UI/ViewModels.

---

# 107. FIREBASE DEPENDENCY ISOLATION

Firebase-specific classes should remain in the data/infrastructure layer.

Avoid:

```kotlin
FirebaseFirestore.getInstance()
```

inside:

```text
M2 ViewModel
M4 ViewModel
M3 FusionEngine
```

All access should go through the repository/data boundary.

---

# 108. NO SENSOR LOGIC IN M5

M5 must not implement:

```text
GPS
Light sensor
Accelerometer
Proximity
Fusion
```

M3 owns these.

M5 only receives the resulting discovery operation.

---

# 109. NO ROOM LOGIC IN M5

M5 does not own:

```text
@Entity
@Dao
RoomDatabase
PendingSyncEntity
```

M6 owns these.

---

# 110. NO UI LOGIC IN M5

M5 should return:

```text
Result
Flow
domain data
error states
```

not manipulate screens, dialogs, or navigation.

---

# 111. PERFORMANCE

Firestore access should avoid unnecessary reads.

Examples:

- Query only published games for public discovery.
- Retrieve only required checkpoint data.
- Observe only the current game's leaderboard.
- Avoid repeatedly downloading all games/checkpoints on every recomposition.

M6's local cache can reduce repeated cloud reads.

---

# 112. PAGINATION

If the number of games becomes large, pagination may be required.

For the MVP, a simpler query may be sufficient if the dataset remains small.

Do not over-engineer pagination unless the project scope requires it.

---

# 113. CACHE INTERACTION

M5 should coordinate with M6 regarding Firestore cache behavior.

M5 provides authoritative cloud access.

M6 decides the application's explicit Room caching/sync strategy.

Do not introduce two competing caching systems without agreement.

---

# 114. TEST ENVIRONMENT

Recommended testing approaches include:

- Firebase Emulator where practical.
- Repository fakes.
- Security rules tests.
- Controlled seed data.
- Test accounts.
- Test games.

The exact setup should match the team's development environment.

---

# 115. SEED DATA REQUIREMENTS

Seed data should cover:

```text
1 published demo game
1 second demo game
1 draft game
multiple players
multiple memberships
multiple discoveries
game-specific leaderboard entries
```

This enables meaningful integration testing.

---

# 116. LEGACY CLEANUP

M5 should remove or isolate cloud structures based on:

```text
relics
foundRelics
global leaderboard
fixed R001–R006 production assumptions
```

if they conflict with the new architecture.

Legacy seed records may remain for demo compatibility.

---

# 117. NO GLOBAL LEADERBOARD

Do not implement:

```text
leaderboards/global
```

as the primary leaderboard architecture.

The canonical structure is:

```text
leaderboards/{gameId}/entries/{uid}
```

---

# 118. NO FIXED CHECKPOINT COLLECTION

Do not create a structure such as:

```text
relics/R001
relics/R002
...
```

as the primary production data model.

Use:

```text
games/{gameId}/checkpoints/{checkpointId}
```

---

# 119. CHANGE CONTROL

Before modifying shared cloud contracts:

1. Identify the affected repository/model.
2. Inform M6 and dependent members.
3. Confirm Firestore/security impact.
4. Update shared contracts.
5. Update rules/indexes.
6. Update tests.
7. Communicate migration requirements.

Firebase schema changes can break multiple team members, so changes must be coordinated.

---

# 120. GIT WORKFLOW

Branch:

```text
feature/m5-firebase-backend
```

Commit examples:

```text
feat(auth): integrate firebase authentication
feat(firebase): add game firestore datasource
feat(firebase): add checkpoint persistence
feat(firebase): add game membership
feat(firebase): add discovery progress
feat(firebase): add game leaderboard
feat(firebase): add publish workflow
feat(fcm): add new game topic notification
feat(security): add firestore ownership rules
test(firebase): add repository integration tests
test(security): add firestore rules tests
```

Do not mix Room implementation into M5 commits.

---

# 121. DEFINITION OF DONE

M5 is complete when:

### Authentication

- Firebase Auth works.
- Authenticated user identity is available.
- Protected operations require authentication.

### Games

- Games can be created.
- Draft games can be stored.
- Published games can be discovered.
- Creator ownership is enforced.

### Checkpoints

- Dynamic checkpoints can be stored.
- Checkpoints are game-scoped.
- Checkpoint ordering is preserved.
- Creator-only modification is enforced.

### Membership

- Players can join published games.
- Membership is game-specific.
- Duplicate joins are handled safely.

### Progress

- Discoveries are stored by user/game/checkpoint.
- Duplicate discovery requests are safe.
- Unauthorized progress writes are blocked.

### Leaderboard

- Each game has its own leaderboard.
- Leaderboard data cannot be arbitrarily manipulated.
- Real-time observation works where required.

### Publishing

- Draft → Published transition works.
- Server-side validation exists.
- Only the creator can publish.

### Notifications

- `/topics/new_games` is supported.
- Published-game notification payload includes `gameId`.
- Notification delivery is integrated.
- Deep-link information is correct.

### Architecture

- Firebase is isolated behind repository/data boundaries.
- M6 can integrate cloud synchronization.
- UI members do not need direct Firestore access.

### Testing

- Auth tests pass.
- Repository tests pass.
- Security-rule tests pass.
- Multi-user tests pass.
- Multi-game isolation tests pass.
- FCM tests pass.

---

# 122. FINAL M5 ARCHITECTURE

The cloud architecture is:

```text
                 Firebase Authentication
                          │
                          ▼
                 Authenticated User
                          │
                          ▼
                    GameRepository
                          │
            ┌─────────────┼─────────────┐
            ▼             ▼             ▼
        Firestore        FCM       Security Rules
            │             │
     ┌──────┼──────┐      │
     ▼      ▼      ▼      ▼
   Games  Progress Leaderboards
     │
     ▼
 Checkpoints
```

Creator flow:

```text
M2 Creator UI
      ↓
GameRepository
      ↓
Firestore
      ↓
DRAFT
      ↓
Publish validation
      ↓
PUBLISHED
      ↓
FCM /topics/new_games
```

Player discovery:

```text
M1 Player UI
      ↓
GameRepository
      ↓
Published games
      ↓
Game Details
      ↓
Join
      ↓
Game-scoped progress
```

---

# 123. CORE DESIGN PRINCIPLE

M5 provides the **authoritative shared cloud layer**.

The cloud model must preserve three critical boundaries:

```text
Creator ownership
+
Game scope
+
User scope
```

Therefore:

```text
Game
  ↓
Checkpoint
```

is game-scoped,

```text
User
  ↓
Game
  ↓
Progress
```

is user + game scoped,

and:

```text
Game
  ↓
Leaderboard
```

is game-specific.

There is no global leaderboard and no permanent fixed checkpoint list.

---

# 124. M5 QUICK CHECKLIST

```text
[ ] Firebase project configuration
[ ] Firebase Authentication
[ ] Auth state
[ ] User identity
[ ] Game creation
[ ] Game draft persistence
[ ] Game retrieval
[ ] Published game discovery
[ ] Dynamic checkpoint persistence
[ ] Checkpoint update
[ ] Checkpoint delete
[ ] Checkpoint ordering
[ ] Creator ownership
[ ] Game membership
[ ] Join idempotency
[ ] Player progress
[ ] Discovery idempotency
[ ] Game-specific leaderboard
[ ] Leaderboard security
[ ] Publish validation
[ ] Draft → Published
[ ] FCM setup
[ ] /topics/new_games
[ ] Notification payload
[ ] Notification deep-link data
[ ] Firestore security rules
[ ] Firestore indexes
[ ] Cloud error mapping
[ ] Multi-user tests
[ ] Multi-game tests
[ ] Security-rule tests
[ ] Repository tests
[ ] FCM tests
[ ] M6 sync handoff
```

---

# 125. FINAL HANDOFF PACKAGE

M5 should provide:

1. Firebase Auth integration.
2. Firestore data sources.
3. Firebase repository implementation.
4. Firestore security rules.
5. Required indexes/configuration.
6. Game/checkpoint cloud model.
7. Membership implementation.
8. Progress implementation.
9. Game-specific leaderboard implementation.
10. Publish operation.
11. FCM topic notification implementation.
12. Notification payload/deep-link contract.
13. Firebase error mapping.
14. Emulator/test configuration where used.
15. Seed/demo data.
16. Security-rule tests.
17. Repository tests.
18. M6 cloud-sync handoff documentation.
19. Any required shared-contract changes.

---

# 126. FINAL INTEGRATION CONTRACT

The rest of the team should be able to work without knowing Firebase internals.

The intended dependency is:

```text
M1/M2/M4
   ↓
Repository interface
   ↓
M5 cloud implementation
```

and:

```text
M6
   ↓
Repository/sync boundary
   ↓
M5 cloud implementation
```

This keeps Firebase implementation isolated and allows the application to use the same domain-level contracts for cloud and local data.

---

**END OF MEMBER 5 FIREBASE, FIRESTORE, AUTHENTICATION & CLOUD BACKEND WORKPLAN**
