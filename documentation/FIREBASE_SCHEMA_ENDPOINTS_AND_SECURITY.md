# Campus Quest --- Firebase Schema, Endpoints & Security Specification

**Document ID:** CQ-FIREBASE-02\
**Document:** `FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md`\
**Project:** Campus Quest\
**Owner:** M5 --- Firebase Cloud & Sync\
**Supporting Owner:** M6 --- Room Data & Integration\
**Status:** Team working specification\
**Development Window:** 13 September 2026 -- 28 September 2026\
**Source of Truth:** `00_MASTER_DEVELOPMENT_PLAN.md`

------------------------------------------------------------------------

# 1. Document Purpose

This document is the Firebase-specific implementation contract for
Campus Quest.

It defines:

-   Firebase Authentication;
-   user profiles;
-   Game Creator data;
-   Game Player participation;
-   games;
-   dynamically configured checkpoints;
-   player discovery/progress;
-   game-specific leaderboards;
-   FCM new-game notifications;
-   repository-facing cloud operations;
-   synchronization boundaries;
-   Firestore security rules;
-   validation requirements;
-   offline/cloud interaction;
-   duplicate-discovery prevention;
-   testing requirements;
-   Firebase implementation ownership.

The application is not a fixed six-relic catalogue.

The authoritative product model is:

``` text
Game Creator
    ↓
Create Game
    ↓
Add / Configure Checkpoints
    ↓
Save Draft
    ↓
Publish
    ↓
New Game Notification
    ↓
Game Players Browse
    ↓
Join Game
    ↓
Play Game
    ↓
Discover Checkpoints
    ↓
Game-Specific Progress
    ↓
Game-Specific Leaderboard
```

The existing `R001`--`R006` records are permitted only as seed/demo
checkpoints belonging to a sample game.

They are not the production data model.

------------------------------------------------------------------------

# 2. Architectural Position

Campus Quest uses the following application boundary:

``` text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firebase
```

Firebase implementation details must not leak into UI code.

The UI should not:

-   call Firestore directly;
-   construct Firestore paths;
-   update leaderboard documents directly;
-   decide whether a user is authorized to edit a game;
-   manually publish FCM messages;
-   bypass the repository to create progress.

The repository is the application boundary.

``` text
M1 / M2 / M3 / M4
        ↓
   Repository
        ↓
      M5
        ↓
 Firebase
```

M6 owns the local Room side and collaborates with M5 on synchronization.

------------------------------------------------------------------------

# 3. Firebase Scope

## 3.1 Required Firebase capabilities

The MVP requires:

``` text
Firebase Authentication
Cloud Firestore
Firebase Cloud Messaging
```

Authentication provides:

-   user identity;
-   Firebase UID;
-   login state;
-   authenticated access.

Firestore provides:

-   user profiles;
-   games;
-   checkpoints;
-   game participation;
-   progress;
-   leaderboard data.

FCM provides:

-   new-game notifications;
-   notification tap routing into the relevant game.

------------------------------------------------------------------------

## 3.2 Deliberately excluded backend features

The MVP does not require:

-   Realtime Database;
-   Cloud Storage;
-   Firebase Hosting;
-   Firebase Functions unless the implementation team chooses them for a
    real notification trigger;
-   a custom REST backend;
-   a separate leaderboard server;
-   a second cloud database.

Do not introduce another backend merely to solve a problem that
Firestore can handle.

------------------------------------------------------------------------

# 4. Core Firebase Data Model

The canonical Firestore model is:

``` text
users/{uid}

games/{gameId}
games/{gameId}/checkpoints/{checkpointId}

gamePlayers/{gameId}_{uid}

progress/{uid}/games/{gameId}/checkpoints/{checkpointId}

leaderboards/{gameId}/entries/{uid}
```

FCM topic:

``` text
/topics/new_games
```

Conceptual relationships:

``` text
                         USER
                          │
             ┌────────────┴────────────┐
             │                         │
         creates                    joins
             │                         │
             ▼                         ▼
           GAME ◄────────────── GAME PLAYER
             │
             │ contains
             ▼
       CHECKPOINT
             │
             │ discovered by
             ▼
          PROGRESS
             │
             │ contributes to
             ▼
        LEADERBOARD
```

------------------------------------------------------------------------

# 5. Firebase Identifier Rules

## 5.1 Firebase UID

Firebase Authentication generates the authoritative user identifier.

Example:

``` text
uid = "firebase-generated-user-id"
```

Development identities such as:

``` text
U001
U002
U003
```

are seed/demo identifiers only.

They must not be treated as real Firebase Authentication UIDs in
production.

------------------------------------------------------------------------

## 5.2 Game ID

Every game has a unique ID.

Recommended form:

``` text
game_<unique-id>
```

Example:

``` text
game_001
game_002
```

Firestore-generated document IDs are also acceptable.

The important requirement is uniqueness.

------------------------------------------------------------------------

## 5.3 Checkpoint ID

A checkpoint ID must be unique within its game.

Example:

``` text
checkpoint_001
checkpoint_002
```

The identity of a checkpoint is therefore:

``` text
gameId + checkpointId
```

A checkpoint ID must never be treated as globally unique across all
games.

For example:

``` text
game_001 / checkpoint_001
game_002 / checkpoint_001
```

are two different checkpoints.

------------------------------------------------------------------------

# 6. User Profile Schema

## 6.1 Path

``` text
/users/{uid}
```

Example:

``` text
/users/abc123
```

## 6.2 Fields

``` text
uid
displayName
email
createdAt
updatedAt
```

Recommended conceptual document:

``` json
{
  "uid": "abc123",
  "displayName": "Nimal Perera",
  "email": "nimal@example.com",
  "createdAt": 1726220000000,
  "updatedAt": 1726220000000
}
```

The `uid` field should match the document ID.

------------------------------------------------------------------------

# 7. User Profile Ownership

A normal authenticated user may read their own profile.

A normal authenticated user may update their own profile fields that the
application intentionally exposes.

A user must not be able to change:

``` text
uid
```

A user must not be able to modify another user's profile.

The client must never be trusted to assign another user's UID.

------------------------------------------------------------------------

# 8. Game Schema

## 8.1 Path

``` text
/games/{gameId}
```

## 8.2 Canonical fields

``` text
id
title
description
creatorId
creatorName
status
checkpointCount
createdAt
publishedAt
updatedAt
```

Recommended model:

``` kotlin
data class Game(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val status: GameStatus = GameStatus.DRAFT,
    val checkpointCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
```

------------------------------------------------------------------------

# 9. Game Status

The canonical lifecycle is:

``` text
DRAFT
  ↓
PUBLISHED
  ↓
CLOSED
```

## 9.1 DRAFT

A draft:

-   belongs to a creator;
-   can be edited by its creator;
-   may contain zero or more checkpoints;
-   is not available to normal players;
-   does not appear in the public published-game list;
-   does not trigger a new-game notification.

## 9.2 PUBLISHED

A published game:

-   is visible to players;
-   can be opened from the games list;
-   can be joined;
-   can be played;
-   has a stable published checkpoint configuration;
-   may trigger the new-game notification.

## 9.3 CLOSED

A closed game:

-   is no longer accepting normal new participation;
-   remains available for historical data where appropriate;
-   retains progress and leaderboard records;
-   must not be silently converted back into a different game.

------------------------------------------------------------------------

# 10. Game State Transition Rules

Allowed MVP transitions:

``` text
DRAFT → PUBLISHED
PUBLISHED → CLOSED
```

The client must not arbitrarily write:

``` text
CLOSED → DRAFT
```

or:

``` text
PUBLISHED → DRAFT
```

unless the team explicitly implements an administrative workflow.

For the MVP, keep the state machine simple.

------------------------------------------------------------------------

# 11. Game Creator Ownership

The creator is identified by:

``` text
creatorId == request.auth.uid
```

A creator can:

-   create their own game;
-   edit their own draft;
-   add checkpoints to their own draft;
-   update checkpoints in their own draft;
-   publish their own draft.

A creator cannot:

-   edit another user's game;
-   change another user's `creatorId`;
-   publish another user's game;
-   delete another user's game;
-   modify another game's checkpoints.

------------------------------------------------------------------------

# 12. Game Visibility

Normal players should only receive games whose status is:

``` text
PUBLISHED
```

The public game list should conceptually query:

``` text
games
where status == PUBLISHED
order by publishedAt descending
```

Draft games should not appear in the player-facing published-games list.

------------------------------------------------------------------------

# 13. Game Checkpoint Schema

## 13.1 Path

``` text
/games/{gameId}/checkpoints/{checkpointId}
```

## 13.2 Canonical fields

``` text
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

Recommended model:

``` kotlin
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

------------------------------------------------------------------------

# 14. Light Signature Schema

A checkpoint stores an expected ambient-light range.

It does not represent light emitted by the checkpoint.

Canonical representation:

``` text
lightSignature
    minLux
    maxLux
```

Example:

``` json
{
  "minLux": 180.0,
  "maxLux": 320.0
}
```

The values are environmental calibration parameters.

M4 owns the calibration process.

------------------------------------------------------------------------

# 15. Location Fields

Each checkpoint contains:

``` text
lat
lng
radiusM
```

Example:

``` text
lat = 6.974850
lng = 79.915300
radiusM = 25
```

Coordinates must be physically validated before the final demonstration.

The database stores configuration.

M3 owns the actual location and geofence runtime behavior.

------------------------------------------------------------------------

# 16. Checkpoint Geofence Boundary

The checkpoint's:

``` text
radiusM
```

defines the intended geofence radius.

M3 consumes:

``` text
gameId
checkpointId
lat
lng
radiusM
```

The geofence is therefore dynamically generated from the selected game's
checkpoint configuration.

There must be no production assumption that:

``` text
R001 = fixed location
```

or:

``` text
R002 = fixed location
```

The creator determines the checkpoint location.

------------------------------------------------------------------------

# 17. Game Player Membership

## 17.1 Path

``` text
/gamePlayers/{gameId}_{uid}
```

The document ID combines:

``` text
gameId + uid
```

This creates one participation record for each user/game combination.

------------------------------------------------------------------------

# 18. GamePlayer Schema

Canonical fields:

``` text
gameId
userId
joinedAt
displayName
status
```

Recommended model:

``` kotlin
data class GamePlayer(
    val gameId: String,
    val userId: String,
    val joinedAt: Long,
    val displayName: String,
    val status: String = "ACTIVE"
)
```

------------------------------------------------------------------------

# 19. Game Membership Identity

The logical primary key is:

``` text
(gameId, userId)
```

This means:

``` text
game_001 + userA
```

and:

``` text
game_002 + userA
```

are separate memberships.

A player may participate in multiple games.

Their progress must remain isolated.

------------------------------------------------------------------------

# 20. Join Game Rules

A player can join a game when:

``` text
request.auth != null
```

and:

``` text
game.status == PUBLISHED
```

The application should create:

``` text
gamePlayers/{gameId}_{uid}
```

only once.

Repeated Join actions must be idempotent.

Expected behavior:

``` text
First Join
    ↓
membership created

Second Join
    ↓
existing membership detected
    ↓
no duplicate membership
```

------------------------------------------------------------------------

# 21. Progress Schema

## 21.1 Path

``` text
/progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

This is the canonical player discovery record.

The identity is:

``` text
uid + gameId + checkpointId
```

------------------------------------------------------------------------

# 22. Progress Fields

Canonical fields:

``` text
gameId
checkpointId
userId
foundAt
```

Optional fields may include:

``` text
updatedAt
```

The MVP should avoid storing raw sensor streams.

------------------------------------------------------------------------

# 23. Discovery Record Example

``` json
{
  "userId": "abc123",
  "gameId": "game_001",
  "checkpointId": "checkpoint_001",
  "foundAt": 1727000000000
}
```

The cloud record means:

``` text
this authenticated player discovered this checkpoint
in this specific game
at this time
```

------------------------------------------------------------------------

# 24. Discovery Uniqueness

The canonical discovery identity is:

``` text
(uid, gameId, checkpointId)
```

Therefore a player cannot legitimately create two independent completion
records for the same checkpoint in the same game.

The application must treat:

``` text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

as idempotent.

------------------------------------------------------------------------

# 25. Cross-Game Progress Isolation

This is a non-negotiable rule.

Suppose:

``` text
Player A
Game 1
3 checkpoints discovered
```

and:

``` text
Player A
Game 2
1 checkpoint discovered
```

The player must have:

``` text
Game 1 progress = 3
Game 2 progress = 1
```

The application must never display:

``` text
Global progress = 4
```

as the game leaderboard score.

------------------------------------------------------------------------

# 26. Leaderboard Schema

## 26.1 Path

``` text
/leaderboards/{gameId}/entries/{uid}
```

This is the canonical leaderboard structure.

Each game has its own leaderboard.

------------------------------------------------------------------------

# 27. Leaderboard Fields

Canonical fields:

``` text
gameId
userId
displayName
checkpointsDiscovered
totalCheckpoints
isCompleted
completionDurationSeconds
lastUpdate
```

Example:

``` json
{
  "gameId": "game_001",
  "userId": "abc123",
  "displayName": "Nimal Perera",
  "checkpointsDiscovered": 3,
  "totalCheckpoints": 6,
  "isCompleted": false,
  "completionDurationSeconds": null,
  "lastUpdate": 1727000000000
}
```

------------------------------------------------------------------------

# 28. Leaderboard Scope

The leaderboard is always scoped by:

``` text
gameId
```

The correct query is conceptually:

``` text
leaderboards/{selectedGameId}/entries
```

The incorrect model is:

``` text
leaderboard/{uid}
```

where all games are combined.

------------------------------------------------------------------------

# 29. Leaderboard Ordering

Primary ordering:

``` text
checkpointsDiscovered DESC
```

For equal scores, use a deterministic secondary rule.

The team must choose one rule and keep it consistent.

Examples:

``` text
lastUpdate ASC
```

or:

``` text
displayName ASC
```

The exact secondary rule is an implementation decision.

------------------------------------------------------------------------

# 30. Leaderboard Calculation

A leaderboard entry represents cloud-backed game progress.

Conceptually:

``` text
Game
 ↓
Total checkpoints

Player
 ↓
Discovered checkpoints

Leaderboard
 ↓
Discovered / Total
```

For example:

``` text
Game has 6 checkpoints

Player A = 4 discovered
Player B = 3 discovered
Player C = 1 discovered
```

Leaderboard:

``` text
1. Player A — 4/6
2. Player B — 3/6
3. Player C — 1/6
```

------------------------------------------------------------------------

# 31. Leaderboard Completion

A player is considered complete when:

``` text
checkpointsDiscovered == totalCheckpoints
```

The exact completion-duration calculation may be implemented by M5/M6
after the core progress flow is stable.

Completion duration is not required to make the basic leaderboard
functional.

------------------------------------------------------------------------

# 32. Notification Model

Publishing a game should result in a new-game notification.

Conceptual flow:

``` text
Creator
  ↓
Publish Game
  ↓
Firestore status = PUBLISHED
  ↓
Notification event
  ↓
FCM topic /topics/new_games
  ↓
Registered users
  ↓
Tap notification
  ↓
Open Game Details
```

------------------------------------------------------------------------

# 33. FCM Topic

The MVP topic is:

``` text
/topics/new_games
```

Authenticated users who opt into new-game notifications may subscribe to
this topic.

Do not create one FCM topic per game for the MVP.

------------------------------------------------------------------------

# 34. Notification Payload

Recommended conceptual payload:

``` kotlin
data class NewGameNotification(
    val gameId: String,
    val gameTitle: String,
    val creatorName: String,
    val checkpointCount: Int,
    val publishedAt: Long
)
```

The payload must contain enough information to route the user to:

``` text
Game Details(gameId)
```

------------------------------------------------------------------------

# 35. Notification Deep Link

When the player taps a notification:

``` text
Notification
   ↓
gameId
   ↓
Game Details
```

The notification must not route to a hard-coded checkpoint.

Incorrect:

``` text
notification → R001
```

Correct:

``` text
notification → gameId → Game Details
```

------------------------------------------------------------------------

# 36. Notification Timing

A notification must be associated with a successful publish operation.

The system must not notify users when a game is merely saved as a draft.

Correct:

``` text
DRAFT
 ↓
save
 ↓
no notification
```

Correct:

``` text
DRAFT
 ↓
PUBLISH
 ↓
PUBLISHED
 ↓
notification
```

------------------------------------------------------------------------

# 37. Duplicate Notification Protection

The application should avoid sending multiple "new game" notifications
for repeated UI taps.

Publishing must be idempotent at the application level.

A game already marked:

``` text
PUBLISHED
```

must not be treated as a newly published draft simply because the
Publish button is pressed again.

------------------------------------------------------------------------

# 38. Firestore Source of Truth

For shared cloud state:

``` text
Firestore = authoritative shared source
```

Room is:

``` text
local persistence + cache + pending sync
```

Therefore:

``` text
Room
 ↓
local/offline state

Firestore
 ↓
shared cloud state
```

------------------------------------------------------------------------

# 39. Room ↔ Firebase Boundary

M6 records successful discovery locally.

Conceptual flow:

``` text
Scan succeeds
 ↓
Room discovery record
 ↓
pendingSync = true
 ↓
Repository
 ↓
Firestore
 ↓
cloud acknowledgement
 ↓
pendingSync = false
```

M5 must not implement a second independent local persistence mechanism.

M6 must not duplicate Firestore business logic.

------------------------------------------------------------------------

# 40. Cloud Synchronization Contract

The repository should expose:

``` kotlin
suspend fun syncPending()
```

The synchronization process should:

1.  identify pending local discoveries;
2.  verify the user identity;
3.  verify the game/checkpoint relationship;
4.  submit the discovery;
5.  preserve idempotency;
6.  confirm cloud success;
7.  mark the local record synchronized.

------------------------------------------------------------------------

# 41. Cloud Discovery Validation

Before accepting a discovery, the application should conceptually
ensure:

``` text
authenticated user
+
valid game
+
valid checkpoint belonging to game
+
player is participating in game
```

The sensor-fusion and physical validation happen on the device.

Firebase security controls should prevent a user from writing arbitrary
records for another user.

------------------------------------------------------------------------

# 42. Important Security Limitation

Firestore rules can enforce:

-   authenticated identity;
-   document ownership;
-   basic field/path relationships;
-   game ownership;
-   immutable identity fields.

Firestore rules should not be treated as a replacement for the entire
gameplay validation engine.

The application must not claim that Firestore security rules alone prove
that a player physically visited a checkpoint.

------------------------------------------------------------------------

# 43. Firestore Security Principles

The rules must follow:

``` text
DENY BY DEFAULT
```

Then explicitly allow the required operations.

Security must be based on:

``` text
request.auth.uid
```

not on:

``` text
request.resource.data.userId
```

alone.

A client can attempt to submit a forged `userId`.

The authenticated UID is authoritative.

------------------------------------------------------------------------

# 44. Security Rule Helper

Conceptual helper:

``` text
isSignedIn():
    request.auth != null

isOwner(uid):
    isSignedIn() &&
    request.auth.uid == uid
```

Equivalent Firestore rules helpers may be implemented using the
project's chosen rules syntax.

------------------------------------------------------------------------

# 45. User Profile Security

Read:

``` text
authenticated user → permitted
```

Write:

``` text
user may write own profile
```

Forbidden:

``` text
user A → modify user B
```

The UID must remain immutable.

------------------------------------------------------------------------

# 46. Game Security

## Create

Authenticated users may create games where:

``` text
creatorId == request.auth.uid
```

## Read

Players may read:

``` text
PUBLISHED
```

games.

Creators may read their own drafts.

## Update

Only the creator may update their own draft.

## Publish

Only the creator may publish their own game.

------------------------------------------------------------------------

# 47. Published Game Immutability

Once a game is published, the team should avoid allowing ordinary client
writes that silently change its core gameplay definition.

Important fields include:

``` text
creatorId
createdAt
publishedAt
```

At minimum:

``` text
creatorId
```

must never be transferable.

The MVP may choose to make published checkpoints immutable.

This is recommended because players may already have cached or
downloaded the published configuration.

------------------------------------------------------------------------

# 48. Checkpoint Security

A checkpoint belongs to:

``` text
games/{gameId}
```

Therefore a checkpoint write must verify that:

``` text
the authenticated user owns the game
```

and:

``` text
the game is editable
```

For the MVP, checkpoint creation and editing should be restricted to
draft games owned by the creator.

------------------------------------------------------------------------

# 49. GamePlayer Security

A user can create their own membership:

``` text
gamePlayers/{gameId}_{request.auth.uid}
```

The submitted:

``` text
userId
```

must equal:

``` text
request.auth.uid
```

A player must not create a membership record for another user.

A player must not modify another user's membership.

------------------------------------------------------------------------

# 50. Progress Security

The progress path contains the authenticated UID:

``` text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

The security rule must require:

``` text
request.auth.uid == uid
```

This prevents:

``` text
Player A
    ↓
writing
    ↓
Player B's progress
```

------------------------------------------------------------------------

# 51. Progress Immutability

The identity of a discovery must not be changed after creation.

These values should be treated as immutable:

``` text
userId
gameId
checkpointId
```

A client should not be able to transform:

``` text
game_001 / checkpoint_001
```

into:

``` text
game_002 / checkpoint_006
```

through an update.

------------------------------------------------------------------------

# 52. Duplicate Discovery

The canonical Firestore document path itself provides an idempotency
boundary:

``` text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

Repeated synchronization of the same discovery should update or confirm
the same document rather than create multiple records.

The repository must not generate random IDs for discovery records.

------------------------------------------------------------------------

# 53. Leaderboard Security

Players may read leaderboard entries for published/accessible games.

Clients should not be trusted to arbitrarily rewrite:

``` text
checkpointsDiscovered
totalCheckpoints
isCompleted
```

If direct client leaderboard writes are used in the MVP, the rules and
repository must tightly constrain them.

A stronger architecture is:

``` text
validated progress
       ↓
controlled leaderboard update
```

rather than:

``` text
client
 ↓
arbitrary leaderboard score
```

------------------------------------------------------------------------

# 54. Recommended MVP Leaderboard Strategy

The team should keep leaderboard updates simple.

After a successful discovery:

``` text
1. Write/confirm progress.
2. Calculate the player's game-specific discovered count.
3. Update the corresponding leaderboard entry.
```

All operations must use:

``` text
gameId
```

as the scope.

------------------------------------------------------------------------

# 55. Firestore Transactions and Batches

Transactions or batched writes may be used where they materially improve
consistency.

Do not introduce complex transaction logic before the basic vertical
slice works.

Priority:

``` text
correctness
>
simplicity
>
consistency
>
optimization
```

M5 and M6 should agree on the final implementation.

------------------------------------------------------------------------

# 56. Repository Contract

The canonical shared repository contract is:

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

The `gameId` must be present wherever checkpoint identity is otherwise
ambiguous.

------------------------------------------------------------------------

# 57. Why `gameId` Is Required in Checkpoint Operations

This is important:

``` kotlin
getFusionSignature(checkpointId)
```

is insufficient.

A checkpoint ID may only be unique within a game.

Correct:

``` kotlin
getFusionSignature(
    gameId,
    checkpointId
)
```

Similarly:

``` kotlin
recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
```

is required.

------------------------------------------------------------------------

# 58. Authentication Repository

The Firebase-specific authentication boundary should be conceptually:

``` kotlin
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

The exact return types may be adapted to the project's implementation.

------------------------------------------------------------------------

# 59. Authentication State

Minimum states:

``` text
LOGGED_OUT
LOADING
AUTHENTICATED
ERROR
```

The UI observes application-level authentication state.

The UI must not spread:

``` text
FirebaseUser
FirebaseAuth
FirebaseException
```

through every screen.

------------------------------------------------------------------------

# 60. Firebase Error Handling

Repository methods should convert Firebase failures into
application-level errors.

Examples:

``` text
PERMISSION_DENIED
NETWORK_UNAVAILABLE
NOT_FOUND
ALREADY_EXISTS
INVALID_DATA
AUTH_REQUIRED
UNKNOWN
```

The exact sealed class may be chosen by the implementation team.

------------------------------------------------------------------------

# 61. Offline Behavior

When the network disappears during gameplay:

``` text
Player discovers checkpoint
        ↓
Room saves discovery
        ↓
pendingSync = true
        ↓
Player continues
```

The player must not lose a successful local discovery merely because
Firestore is temporarily unavailable.

------------------------------------------------------------------------

# 62. Offline Game Data

The application may cache:

``` text
Game
Checkpoint
GamePlayer
Discovery
Leaderboard snapshot
```

Room owns the local representation.

Firebase remains the shared cloud source.

------------------------------------------------------------------------

# 63. Offline Restrictions

Offline mode must not falsely imply that the player can perform all
cloud operations.

For example:

``` text
creating/publishing a game
```

may require connectivity unless the team explicitly implements offline
creator synchronization.

The MVP should prioritize offline gameplay/discovery persistence rather
than offline publishing.

------------------------------------------------------------------------

# 64. Firebase Availability States

Repository operations should distinguish:

``` text
success
network failure
permission failure
not found
validation failure
unknown failure
```

The UI can then display meaningful feedback.

------------------------------------------------------------------------

# 65. Game Creation Flow

Creator flow:

``` text
Login
 ↓
Creator Mode
 ↓
Create Game
 ↓
Enter title
 ↓
Enter description
 ↓
Save Draft
 ↓
Game created
```

Firestore write:

``` text
games/{gameId}
```

with:

``` text
status = DRAFT
creatorId = authenticated UID
```

------------------------------------------------------------------------

# 66. Add Checkpoint Flow

``` text
Open Draft
 ↓
Add Checkpoint
 ↓
Enter checkpoint name
 ↓
Select coordinates
 ↓
Configure radius
 ↓
Configure light range
 ↓
Add clue/lore
 ↓
Save
```

Firestore:

``` text
games/{gameId}/checkpoints/{checkpointId}
```

------------------------------------------------------------------------

# 67. Publish Flow

``` text
Creator
 ↓
Review Game
 ↓
Validate Game
 ↓
Publish
 ↓
status = PUBLISHED
 ↓
publishedAt assigned
 ↓
notification event
```

Minimum publish validation:

``` text
title is valid
description is valid
at least one checkpoint exists
checkpoint coordinates are valid
checkpoint radius is valid
light range is valid
checkpoint IDs are unique within game
```

------------------------------------------------------------------------

# 68. Publishing Validation

A game should not publish if:

``` text
checkpointCount == 0
```

or if a checkpoint contains invalid location data.

The exact validation thresholds are implementation decisions.

The important requirement is that a published game must contain playable
checkpoint configuration.

------------------------------------------------------------------------

# 69. Player Browse Flow

Player:

``` text
Login
 ↓
Games
 ↓
Published games
 ↓
Select game
 ↓
Game Details
```

The game list reads published games.

It must not expose the creator's drafts to normal players.

------------------------------------------------------------------------

# 70. Player Join Flow

``` text
Game Details
 ↓
Join
 ↓
gamePlayers/{gameId}_{uid}
 ↓
Game Map
```

The repository handles the membership operation.

The UI does not directly write Firestore.

------------------------------------------------------------------------

# 71. Gameplay Data Flow

``` text
Selected Game
 ↓
Game Checkpoints
 ↓
Map
 ↓
Selected Checkpoint
 ↓
Geofence
 ↓
Scan
 ↓
GPS + Light + Motion
 ↓
Fusion Score
 ↓
Proximity Final Gate
 ↓
Reveal
 ↓
Room
 ↓
Firestore
 ↓
Game Leaderboard
```

------------------------------------------------------------------------

# 72. Sensor Data Is Not Firebase Data

Raw:

``` text
accelerometer samples
light sensor samples
proximity readings
```

must not be uploaded as normal gameplay data.

The sensor system produces a local result.

Firebase stores the resulting discovery/progress state.

------------------------------------------------------------------------

# 73. Fusion Result Boundary

M4 owns:

``` text
GPS input
Light match
Motion match
Weighted fusion
Sensor degradation
```

The final local result is used to determine whether the checkpoint can
be revealed.

Firebase receives the successful discovery state, not the entire sensor
stream.

------------------------------------------------------------------------

# 74. Proximity Boundary

Proximity is not a fusion component.

Correct:

``` text
GPS + Light + Motion
        ↓
   Fusion Score
        ↓
 threshold
        ↓
 Proximity
        ↓
 Reveal
```

Firebase does not calculate the proximity gate.

------------------------------------------------------------------------

# 75. Creator and Player Roles

Campus Quest does not require a separate Firebase Authentication system
for creators and players.

A Firebase-authenticated user can act as:

``` text
Game Creator
```

for games they create, and:

``` text
Game Player
```

for games they join.

The role is contextual.

------------------------------------------------------------------------

# 76. Creator Identity

The game stores:

``` text
creatorId
creatorName
```

The authoritative ownership check is:

``` text
creatorId == request.auth.uid
```

`creatorName` is presentation data and must not replace UID-based
authorization.

------------------------------------------------------------------------

# 77. Display Name Changes

If a user changes their display name, existing game and leaderboard
documents may retain a historical display name.

The team may synchronize display-name changes later.

Do not make display-name synchronization a blocker for the MVP.

Identity is:

``` text
uid
```

not:

``` text
displayName
```

------------------------------------------------------------------------

# 78. Seed Data

The six existing records may be retained as sample data:

``` text
R001
R002
R003
R004
R005
R006
```

They must belong to a sample game.

Recommended conceptual mapping:

``` text
game_demo_001
    ├── R001
    ├── R002
    ├── R003
    ├── R004
    ├── R005
    └── R006
```

In the new architecture these are checkpoint records.

------------------------------------------------------------------------

# 79. Sample Game

Recommended seed game:

``` text
id:
game_demo_001

title:
Campus Quest Demo

status:
PUBLISHED

checkpointCount:
6
```

The exact title can be changed by the team.

------------------------------------------------------------------------

# 80. Sample Checkpoint Mapping

The existing development records can be represented as:

``` text
R001 → checkpoint in game_demo_001
R002 → checkpoint in game_demo_001
R003 → checkpoint in game_demo_001
R004 → checkpoint in game_demo_001
R005 → checkpoint in game_demo_001
R006 → checkpoint in game_demo_001
```

Their coordinates and light ranges remain development values until
physically validated and calibrated.

------------------------------------------------------------------------

# 81. Legacy Global Relic Model

The following is not the production architecture:

``` text
/relics/{relicId}
```

It may exist only in historical documentation or migration notes.

It must not be used as the application's canonical cloud schema.

Likewise:

``` text
/leaderboard/{uid}
```

is not the production leaderboard model.

------------------------------------------------------------------------

# 82. Legacy Global Progress Model

Do not use:

``` text
progress/{uid}/found/{relicId}
```

as the canonical production model.

Use:

``` text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

This preserves game isolation.

------------------------------------------------------------------------

# 83. Firestore Query Contract --- Published Games

Conceptual query:

``` text
collection: games
filter:
    status == PUBLISHED
order:
    publishedAt DESC
```

The repository returns:

``` kotlin
List<Game>
```

The UI renders the list.

------------------------------------------------------------------------

# 84. Firestore Query Contract --- Game Details

Conceptual read:

``` text
games/{gameId}
```

Then:

``` text
games/{gameId}/checkpoints
```

The repository combines these into the game-detail presentation model as
required.

------------------------------------------------------------------------

# 85. Firestore Query Contract --- Checkpoints

Read:

``` text
games/{gameId}/checkpoints
```

Order by:

``` text
order ASC
```

The exact query may be adjusted to match the final Firestore indexes.

------------------------------------------------------------------------

# 86. Firestore Query Contract --- Membership

Read:

``` text
gamePlayers/{gameId}_{uid}
```

This determines whether the current player has joined the selected game.

------------------------------------------------------------------------

# 87. Firestore Query Contract --- Player Progress

Read:

``` text
progress/{uid}/games/{gameId}/checkpoints
```

This gives only the selected player's progress for the selected game.

------------------------------------------------------------------------

# 88. Firestore Query Contract --- Leaderboard

Read:

``` text
leaderboards/{gameId}/entries
```

Order:

``` text
checkpointsDiscovered DESC
```

The query is always scoped to one game.

------------------------------------------------------------------------

# 89. Firestore Write Contract --- Game

Create:

``` text
games/{gameId}
```

Required:

``` text
creatorId
title
description
status
createdAt
```

------------------------------------------------------------------------

# 90. Firestore Write Contract --- Checkpoint

Create:

``` text
games/{gameId}/checkpoints/{checkpointId}
```

Required:

``` text
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
```

------------------------------------------------------------------------

# 91. Firestore Write Contract --- Membership

Create:

``` text
gamePlayers/{gameId}_{uid}
```

Required:

``` text
gameId
userId
joinedAt
```

------------------------------------------------------------------------

# 92. Firestore Write Contract --- Discovery

Create or idempotently confirm:

``` text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

Required:

``` text
userId
gameId
checkpointId
foundAt
```

------------------------------------------------------------------------

# 93. Firestore Write Contract --- Leaderboard

Update:

``` text
leaderboards/{gameId}/entries/{uid}
```

Required:

``` text
gameId
userId
displayName
checkpointsDiscovered
totalCheckpoints
isCompleted
lastUpdate
```

------------------------------------------------------------------------

# 94. Data Validation

The repository should validate:

### Game

``` text
title not blank
creatorId available
status valid
```

### Checkpoint

``` text
gameId valid
coordinates valid
radiusM > 0
minLux >= 0
maxLux >= minLux
```

### Membership

``` text
gameId valid
userId == authenticated UID
```

### Discovery

``` text
gameId valid
checkpoint belongs to game
userId == authenticated UID
```

------------------------------------------------------------------------

# 95. Server Trust Boundary

The client is not trusted for:

``` text
user identity
creator ownership
game ownership
cross-user writes
cross-game leaderboard scope
```

The client can supply data.

Firestore security rules determine whether the write is permitted.

------------------------------------------------------------------------

# 96. Security Rules --- Conceptual Structure

The production rules should follow a structure similar to:

``` text
match /users/{uid}
    read/write only when request.auth.uid == uid

match /games/{gameId}
    create when creatorId == request.auth.uid
    read when published or creator owns draft
    update only by creator while editable

match /games/{gameId}/checkpoints/{checkpointId}
    read when game is accessible
    write only by game creator while editable

match /gamePlayers/{membershipId}
    read/write only for authenticated participant
    validate membership userId

match /progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
    read/write only when request.auth.uid == uid

match /leaderboards/{gameId}/entries/{uid}
    read for authenticated accessible players
    writes constrained by the chosen leaderboard strategy
```

This is the security design.

The exact deployed syntax must be tested against the actual Firestore
rules engine.

------------------------------------------------------------------------

# 97. Example Firestore Rules Skeleton

A conceptual rules skeleton:

``` text
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() {
      return request.auth != null;
    }

    function isUser(uid) {
      return signedIn() &&
             request.auth.uid == uid;
    }

    function ownsGame(gameId) {
      return signedIn() &&
             get(/databases/$(database)/documents/games/$(gameId))
               .data.creatorId == request.auth.uid;
    }

    match /users/{uid} {
      allow read, create, update:
        if isUser(uid);
    }

    match /games/{gameId} {
      allow create:
        if signedIn() &&
           request.resource.data.creatorId == request.auth.uid;

      allow read:
        if signedIn() &&
           (
             resource.data.status == "PUBLISHED" ||
             resource.data.creatorId == request.auth.uid
           );

      allow update:
        if signedIn() &&
           resource.data.creatorId == request.auth.uid;
    }

    match /games/{gameId}/checkpoints/{checkpointId} {
      allow read:
        if signedIn();

      allow create, update:
        if ownsGame(gameId);
    }

    match /gamePlayers/{membershipId} {
      allow read, create, update:
        if signedIn() &&
           request.resource.data.userId == request.auth.uid;
    }

    match /progress/{uid}/games/{gameId}/checkpoints/{checkpointId} {
      allow read, create, update:
        if isUser(uid);
    }

    match /leaderboards/{gameId}/entries/{uid} {
      allow read:
        if signedIn();

      allow write:
        if isUser(uid);
    }
  }
}
```

This skeleton is intentionally a starting point, not permission to
deploy it without testing.

------------------------------------------------------------------------

# 98. Security Rule Review Before Deployment

M5 must test:

``` text
anonymous read
anonymous write
user A → user B profile
user A → user B progress
player → creator game edit
player → checkpoint edit
creator A → creator B game edit
cross-game leaderboard write
forged userId
forged creatorId
```

Every forbidden operation must be denied.

------------------------------------------------------------------------

# 99. Game Access Security

A player should not obtain unpublished creator content merely by
guessing a game ID.

Draft access should be controlled.

Creator-owned drafts:

``` text
creator → access
other player → denied
```

Published games:

``` text
authenticated player → access
```

------------------------------------------------------------------------

# 100. Cross-Game Security Test

Test:

``` text
User A
Game 1
```

attempts to write:

``` text
Game 2 progress
```

Expected:

``` text
DENIED
```

The client must never be able to move progress between games.

------------------------------------------------------------------------

# 101. Cross-User Security Test

Test:

``` text
User A
```

attempts:

``` text
progress/UserB/games/game1/checkpoints/checkpoint1
```

Expected:

``` text
DENIED
```

------------------------------------------------------------------------

# 102. Creator Ownership Test

Test:

``` text
Creator A owns Game A
Creator B owns Game B
```

Creator A attempts to edit Game B.

Expected:

``` text
DENIED
```

------------------------------------------------------------------------

# 103. Published Game Editing Test

Test whether a normal player can modify:

``` text
games/game_001
```

Expected:

``` text
DENIED
```

Test whether a non-owner creator can modify the game.

Expected:

``` text
DENIED
```

------------------------------------------------------------------------

# 104. Notification Security

FCM notification content should not expose:

-   passwords;
-   authentication tokens;
-   private user data;
-   unnecessary sensitive information.

The notification payload should contain only information needed for
navigation and presentation:

``` text
gameId
gameTitle
creatorName
checkpointCount
publishedAt
```

------------------------------------------------------------------------

# 105. FCM Token Handling

If device tokens are stored, they must be associated with the
authenticated user appropriately.

The MVP can avoid a custom token collection if topic subscription is
sufficient.

Recommended MVP:

``` text
authenticated user
 ↓
subscribe to /topics/new_games
```

------------------------------------------------------------------------

# 106. New Game Notification Implementation Options

The notification trigger can be implemented using a controlled backend
mechanism.

Possible implementation:

``` text
Creator client
 ↓
publishGame()
 ↓
Firestore status change
 ↓
trusted notification trigger
 ↓
FCM
```

If a trusted server-side trigger is not implemented, the team must
document exactly how the notification is sent and ensure that untrusted
clients cannot impersonate the notification authority.

------------------------------------------------------------------------

# 107. Notification Trigger Requirement

The important product behavior is:

``` text
successful game publication
        ↓
new-game notification
```

The exact Firebase implementation may be selected by M5 based on project
setup and time.

Do not block the entire app on sophisticated notification
infrastructure.

------------------------------------------------------------------------

# 108. Firestore Index Planning

Potential queries requiring indexes:

``` text
games
where status == PUBLISHED
order by publishedAt DESC
```

and:

``` text
leaderboards/{gameId}/entries
order by checkpointsDiscovered DESC
```

Firestore will indicate missing composite indexes where required.

M5 must add only the indexes actually required by the final queries.

------------------------------------------------------------------------

# 109. Timestamp Strategy

The project may use:

``` text
Long epoch milliseconds
```

or:

``` text
Firestore Timestamp
```

The final choice should be consistent across the application.

Do not mix representations arbitrarily.

The repository should convert cloud timestamps into application-level
models.

------------------------------------------------------------------------

# 110. Server Timestamp Preference

Where ordering or auditability matters, server-generated timestamps are
preferable to trusting client clocks.

Important examples:

``` text
publishedAt
```

and potentially:

``` text
createdAt
updatedAt
```

The exact implementation may use Firestore server timestamps.

------------------------------------------------------------------------

# 111. Client Clock Limitation

The device clock must not be treated as a trusted security mechanism.

A player should not be able to obtain authorization merely by changing:

``` text
system time
```

Gameplay sensor validation remains local.

Cloud security is based on:

``` text
authentication
ownership
document relationships
```

------------------------------------------------------------------------

# 112. Data Minimization

Do not store unnecessary:

``` text
sensor streams
GPS histories
continuous location traces
```

The MVP needs:

``` text
checkpoint configuration
discovery result
progress
leaderboard
```

not a surveillance log of the player.

------------------------------------------------------------------------

# 113. Location Privacy

Campus Quest requires location during gameplay.

Firebase should not receive a continuous historical location trail as
part of the core schema.

M3 provides location to the local gameplay system.

M5 stores only the shared state required for the game.

------------------------------------------------------------------------

# 114. Firebase Authentication Flow

``` text
App launch
 ↓
Firebase Auth state
 ↓
No user
    → Login
 ↓
Authenticated user
 ↓
Load/create profile
 ↓
Open application
```

The application must not assume that a locally cached UI state means the
Firebase session is valid.

------------------------------------------------------------------------

# 115. First Login Profile Flow

After successful authentication:

``` text
Firebase UID
 ↓
users/{uid}
 ↓
profile exists?
 ├── yes → load
 └── no → create
```

The profile document should use the Firebase UID as its document ID.

------------------------------------------------------------------------

# 116. Logout Flow

``` text
UI
 ↓
AuthRepository.signOut()
 ↓
FirebaseAuth.signOut()
 ↓
auth state changes
 ↓
return to logged-out UI
```

Cached data should not accidentally be displayed as another
authenticated user's data.

------------------------------------------------------------------------

# 117. User Data Isolation After Logout

The repository must use the current authenticated UID when loading:

``` text
progress
game membership
personal state
```

Do not retain stale user-specific cloud queries after logout.

------------------------------------------------------------------------

# 118. Creator Draft Persistence

Creator draft data is stored in Firestore as:

``` text
games/{gameId}
games/{gameId}/checkpoints/{checkpointId}
```

The MVP may additionally cache drafts locally with Room.

If Room caching is implemented:

``` text
Room = local draft convenience
Firestore = shared cloud state
```

------------------------------------------------------------------------

# 119. Draft Editing Sequence

``` text
Create Game
 ↓
Game DRAFT
 ↓
Add checkpoint
 ↓
Update checkpoint
 ↓
Review
 ↓
Publish
```

Each checkpoint must retain:

``` text
gameId
```

so the repository never loses its parent game relationship.

------------------------------------------------------------------------

# 120. Publish Atomicity

The team should avoid a situation where:

``` text
game status = PUBLISHED
```

but required checkpoint data has not been written.

Recommended conceptual order:

``` text
validate all draft data
 ↓
confirm checkpoints
 ↓
publish game
 ↓
trigger notification
```

If the final implementation uses a transaction/batch, it should be
tested carefully.

------------------------------------------------------------------------

# 121. Game Count Fields

`checkpointCount` is a convenience field.

The authoritative checkpoint collection is:

``` text
games/{gameId}/checkpoints
```

Do not allow inconsistent client-written counts to become a security
boundary.

The repository should keep:

``` text
checkpointCount
```

consistent with the actual configured checkpoint set.

------------------------------------------------------------------------

# 122. Leaderboard Count Fields

Similarly:

``` text
checkpointsDiscovered
```

is derived from the player's game-scoped progress.

It should not be treated as an arbitrary user-entered score.

------------------------------------------------------------------------

# 123. Progress vs Leaderboard

Progress is the detailed source:

``` text
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
```

Leaderboard is the summarized representation:

``` text
leaderboards/{gameId}/entries/{uid}
```

Therefore:

``` text
Progress = detailed discovery state
Leaderboard = summarized ranking state
```

------------------------------------------------------------------------

# 124. Game-Specific Leaderboard Example

Game A:

``` text
Player A → 5/6
Player B → 3/6
```

Game B:

``` text
Player A → 1/2
Player C → 2/2
```

There are two separate rankings.

Never combine them.

------------------------------------------------------------------------

# 125. Repository Responsibility Matrix

  Operation           Repository   M5 Firebase    M6 Room
  ------------------- ------------ -------------- ----------------------
  Login               Yes          Auth           \-
  Game list           Yes          Read           Cache
  Game details        Yes          Read           Cache
  Create game         Yes          Write          Optional draft cache
  Create checkpoint   Yes          Write          Optional cache
  Publish game        Yes          Write          Optional
  Join game           Yes          Write          Cache
  Get checkpoints     Yes          Read           Cache
  Discovery           Yes          Sync           Local save
  Leaderboard         Yes          Read/write     Optional cache
  Offline queue       Yes          Cloud target   Owns local queue

------------------------------------------------------------------------

# 126. M5 Responsibilities

M5 owns:

``` text
Firebase project
Authentication
Firestore
FCM
Firestore schema
security rules
cloud repository
cloud validation
leaderboard persistence
cloud progress
notification mechanism
Firebase error handling
```

------------------------------------------------------------------------

# 127. M6 Responsibilities

M6 owns:

``` text
Room
local entities
DAOs
offline persistence
pendingSync
local repository layer
sync acknowledgement
integration stability
```

M6 does not own:

``` text
Firestore security rules
FCM infrastructure
Firebase project configuration
```

------------------------------------------------------------------------

# 128. M3 Firebase Boundary

M3 receives checkpoint location data through the repository.

M3 should not directly depend on:

``` text
FirebaseFirestore
```

The application boundary is:

``` text
GameRepository
 ↓
Checkpoint
 ↓
M3 location system
```

------------------------------------------------------------------------

# 129. M4 Firebase Boundary

M4 receives:

``` text
LightSignature
```

through the repository.

M4 does not directly query Firestore.

Correct:

``` text
Repository
 ↓
Checkpoint.lightSignature
 ↓
SensorFusionEngine
```

------------------------------------------------------------------------

# 130. M1 Firebase Boundary

M1 consumes:

``` text
Game
GamePlayer
Auth state
```

through repository/ViewModel boundaries.

M1 does not construct Firestore paths.

------------------------------------------------------------------------

# 131. M2 Firebase Boundary

M2 consumes:

``` text
Checkpoint
Discovery state
Leaderboard entries
```

through application models.

M2 does not write leaderboard documents directly.

------------------------------------------------------------------------

# 132. Cloud Data Flow

``` text
Firebase Auth
      ↓
      UID
      ↓
users/{uid}
      ↓
Game Creator / Game Player
      ↓
games/{gameId}
      ↓
checkpoints
      ↓
gamePlayers
      ↓
progress
      ↓
leaderboards
```

------------------------------------------------------------------------

# 133. Complete Creator Cloud Flow

``` text
Authenticated creator
 ↓
createGame()
 ↓
games/{gameId}
status = DRAFT
 ↓
createCheckpoint()
 ↓
games/{gameId}/checkpoints/{checkpointId}
 ↓
publishGame()
 ↓
status = PUBLISHED
 ↓
FCM notification
```

------------------------------------------------------------------------

# 134. Complete Player Cloud Flow

``` text
Authenticated player
 ↓
getAvailableGames()
 ↓
getGameDetails(gameId)
 ↓
joinGame(gameId)
 ↓
gamePlayers/{gameId}_{uid}
 ↓
getGameCheckpoints(gameId)
 ↓
play
 ↓
recordDiscovery(gameId, checkpointId)
 ↓
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
 ↓
leaderboards/{gameId}/entries/{uid}
```

------------------------------------------------------------------------

# 135. Complete Offline Flow

``` text
Player
 ↓
Game cached
 ↓
Checkpoint cached
 ↓
Physical discovery
 ↓
Room
 ↓
pendingSync
 ↓
network returns
 ↓
Repository
 ↓
Firestore
 ↓
acknowledgement
 ↓
pendingSync = false
```

------------------------------------------------------------------------

# 136. Firebase Failure During Discovery

If Firestore fails:

``` text
Discovery successful locally
 ↓
Room save succeeds
 ↓
cloud write fails
 ↓
pendingSync remains true
```

The user should not be told that the discovery was lost.

The UI may indicate:

``` text
Saved locally — will sync when online
```

if the product design supports that status.

------------------------------------------------------------------------

# 137. Firebase Failure During Game Browse

If game list loading fails:

``` text
Firestore unavailable
 ↓
try Room cache
```

If no cache exists:

``` text
show appropriate error
```

Do not fabricate game data.

------------------------------------------------------------------------

# 138. Firebase Failure During Publish

Publishing is a cloud operation.

If it fails:

``` text
Game remains DRAFT
```

unless the application can prove that publication succeeded.

Do not show:

``` text
Published
```

merely because the button was pressed.

------------------------------------------------------------------------

# 139. Notification Failure

Notification delivery failure must not undo successful game publication.

Correct:

``` text
Firestore:
PUBLISHED

FCM:
delivery may fail
```

The game remains published.

Players can still discover it through the games list.

------------------------------------------------------------------------

# 140. Notification as Secondary Discovery Path

The notification is a convenience.

The authoritative game availability is:

``` text
games where status == PUBLISHED
```

Therefore the application must continue to function if a notification is
missed.

------------------------------------------------------------------------

# 141. Deep Link Validation

When opening a game from a notification:

``` text
receive gameId
 ↓
load game
 ↓
verify game exists
 ↓
verify game is accessible
 ↓
show Game Details
```

Do not assume that the notification payload is permanently valid.

------------------------------------------------------------------------

# 142. Deleted Game Handling

The MVP should avoid hard deletion of games once players have
participated.

If a game is no longer active:

``` text
status = CLOSED
```

is preferred.

This preserves:

``` text
progress
leaderboard
historical participation
```

------------------------------------------------------------------------

# 143. Checkpoint Deletion

Avoid deleting published checkpoints after players begin playing.

If a creator needs to modify a game substantially, the MVP can require:

``` text
edit while DRAFT
publish once stable
```

This reduces synchronization complexity.

------------------------------------------------------------------------

# 144. Data Consistency Rules

The following relationships must remain true:

``` text
checkpoint.gameId == parent gameId
gamePlayer.gameId == parent membership game
gamePlayer.userId == authenticated UID
progress.gameId == path gameId
progress.checkpointId == path checkpointId
progress.userId == path uid
leaderboard.gameId == path gameId
leaderboard.userId == path uid
```

------------------------------------------------------------------------

# 145. Firestore Path Checklist

Correct:

``` text
users/{uid}
games/{gameId}
games/{gameId}/checkpoints/{checkpointId}
gamePlayers/{gameId}_{uid}
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
leaderboards/{gameId}/entries/{uid}
```

Incorrect production paths:

``` text
relics/{relicId}
progress/{uid}/found/{relicId}
leaderboard/{uid}
```

------------------------------------------------------------------------

# 146. No Global Checkpoint Catalogue

Campus Quest must not rely on:

``` text
all checkpoints in one global collection
```

because checkpoints belong to games.

The parent game is part of the identity.

------------------------------------------------------------------------

# 147. No Global Leaderboard

Campus Quest must not maintain a single leaderboard for all games.

Correct:

``` text
leaderboards/game_001/entries
leaderboards/game_002/entries
```

Incorrect:

``` text
leaderboard/all-users
```

------------------------------------------------------------------------

# 148. Security Rules and UI Are Separate

Hiding a button is not security.

For example:

``` text
UI hides Edit button
```

does not prevent a malicious client from sending an update.

Firestore rules must enforce:

``` text
creator ownership
```

independently.

------------------------------------------------------------------------

# 149. Security Rules and Repository Are Separate

Repository checks improve user experience.

Firestore rules enforce the actual cloud boundary.

Both are required.

``` text
UI
 ↓
ViewModel
 ↓
Repository validation
 ↓
Firestore security
```

------------------------------------------------------------------------

# 150. No Secrets in Client Code

Never commit:

``` text
passwords
service-account private keys
private signing keys
server credentials
```

The Android application may contain Firebase configuration intended for
the client SDK, but privileged server credentials must never be embedded
in the APK.

------------------------------------------------------------------------

# 151. Firebase Console Configuration

M5 must configure:

``` text
Firebase project
Android app registration
Authentication provider
Firestore database
FCM
Security rules
Required indexes
```

The exact Firebase project ID is environment-specific.

Do not hard-code it into this document.

------------------------------------------------------------------------

# 152. Authentication Provider

The exact provider must match the team's Firebase setup.

For the MVP, email/password authentication is sufficient if it is the
selected implementation.

The application contract remains:

``` text
sign in
sign out
observe auth state
current user
```

------------------------------------------------------------------------

# 153. Firestore Environment

The team should maintain a clear distinction between:

``` text
development/test data
```

and:

``` text
final demonstration data
```

Seed data must be identifiable.

------------------------------------------------------------------------

# 154. Seed Data Rule

Development sample users:

``` text
U001
U002
U003
U004
U005
```

must not be assumed to represent real authenticated Firebase users.

The actual Firebase UID is authoritative during runtime.

------------------------------------------------------------------------

# 155. Seed Checkpoint Rule

The sample records:

``` text
R001–R006
```

may be used to populate:

``` text
game_demo_001
```

but application code must not depend on:

``` text
R001 always existing
```

A creator-created game must work with:

``` text
checkpoint_A
checkpoint_B
```

or any other valid IDs.

------------------------------------------------------------------------

# 156. Dynamic Checkpoint Requirement

This is a critical acceptance condition.

The creator must be able to create:

``` text
Game A
 ├── Checkpoint 1
 ├── Checkpoint 2
 └── Checkpoint 3
```

and another creator may create:

``` text
Game B
 ├── Checkpoint X
 ├── Checkpoint Y
 ├── Checkpoint Z
 └── Checkpoint W
```

The code must not assume the number six.

------------------------------------------------------------------------

# 157. Dynamic Game Requirement

The player-facing application must discover games from Firestore.

It must not use:

``` text
hard-coded game list
```

as the production source.

------------------------------------------------------------------------

# 158. Dynamic Leaderboard Requirement

The leaderboard screen receives:

``` text
gameId
```

and loads:

``` text
leaderboards/{gameId}/entries
```

It must not load a global ranking.

------------------------------------------------------------------------

# 159. Dynamic Progress Requirement

The progress screen receives:

``` text
gameId
```

and loads:

``` text
progress/{uid}/games/{gameId}/checkpoints
```

It must not load all user discoveries and then attempt to filter them
incorrectly in the UI.

------------------------------------------------------------------------

# 160. Game Isolation Acceptance Test

Create:

``` text
Game A
2 checkpoints
```

Create:

``` text
Game B
3 checkpoints
```

User joins both.

Discover:

``` text
Game A → 1
Game B → 2
```

Expected:

``` text
Game A leaderboard = 1/2
Game B leaderboard = 2/3
```

No cross-contamination.

------------------------------------------------------------------------

# 161. Creator Isolation Acceptance Test

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
permission denied
```

------------------------------------------------------------------------

# 162. Player Isolation Acceptance Test

Player A joins Game A.

Attempt to write:

``` text
Player B progress
```

Expected:

``` text
permission denied
```

------------------------------------------------------------------------

# 163. Draft Visibility Acceptance Test

Creator creates:

``` text
Game C
status = DRAFT
```

Player opens Games list.

Expected:

``` text
Game C is not visible.
```

Creator opens own games.

Expected:

``` text
Game C is visible.
```

------------------------------------------------------------------------

# 164. Publish Acceptance Test

Creator publishes Game C.

Expected:

``` text
status = PUBLISHED
```

Then:

``` text
Game C appears in player list.
```

If notification infrastructure is active:

``` text
new-game notification is generated.
```

------------------------------------------------------------------------

# 165. Notification Acceptance Test

Test:

``` text
Creator publishes Game C
```

Player receives:

``` text
Campus Quest — New Game
```

Tap:

``` text
notification
 ↓
Game Details(Game C)
```

Not:

``` text
notification
 ↓
fixed checkpoint
```

------------------------------------------------------------------------

# 166. Offline Discovery Acceptance Test

Procedure:

``` text
Join game while online
 ↓
cache game/checkpoints
 ↓
disconnect network
 ↓
perform successful discovery
 ↓
Room record exists
 ↓
pendingSync = true
 ↓
restore network
 ↓
sync
```

Expected:

``` text
cloud progress exists
leaderboard reflects discovery
pendingSync = false
```

------------------------------------------------------------------------

# 167. Duplicate Sync Acceptance Test

Perform:

``` text
same discovery
sync
sync again
```

Expected:

``` text
one logical progress record
```

not:

``` text
two discoveries
```

------------------------------------------------------------------------

# 168. Authentication Security Test

Test:

``` text
signed out
 ↓
attempt Firestore protected operation
```

Expected:

``` text
denied
```

------------------------------------------------------------------------

# 169. Invalid Game Test

Player attempts:

``` text
join nonexistent game
```

Expected:

``` text
controlled NOT_FOUND result
```

No crash.

------------------------------------------------------------------------

# 170. Invalid Checkpoint Test

Attempt:

``` text
recordDiscovery(
    validGame,
    nonexistentCheckpoint
)
```

Expected:

``` text
validation failure
```

The application must not create a phantom checkpoint discovery.

------------------------------------------------------------------------

# 171. Cross-Game Checkpoint Test

Attempt:

``` text
game_001
checkpoint belonging to game_002
```

Expected:

``` text
rejected
```

The parent-child relationship must be verified.

------------------------------------------------------------------------

# 172. Game Completion Test

Game:

``` text
3 checkpoints
```

Player discovers:

``` text
1
2
3
```

Expected:

``` text
checkpointsDiscovered = 3
totalCheckpoints = 3
isCompleted = true
```

------------------------------------------------------------------------

# 173. Partial Progress Test

Game:

``` text
6 checkpoints
```

Player discovers:

``` text
R001
R003
R006
```

Expected:

``` text
3/6
```

for that game only.

------------------------------------------------------------------------

# 174. Restart Test

Procedure:

``` text
discover checkpoint
 ↓
close application
 ↓
reopen
```

Expected:

``` text
local progress remains
```

After connectivity:

``` text
cloud progress remains
```

------------------------------------------------------------------------

# 175. Firebase Repository Test Matrix

  Operation        Online   Offline                 Expected
  ---------------- -------- ----------------------- -----------------
  Login            Yes      No/limited              Auth state
  Game list        Yes      Cache if available      Games
  Game details     Yes      Cache if available      Game
  Create game      Yes      No                      Draft
  Add checkpoint   Yes      Optional local draft    Checkpoint
  Publish          Yes      No                      Published
  Join             Yes      Optional cached state   Membership
  Discovery        Yes      Yes                     Room then cloud
  Leaderboard      Yes      Cache if available      Game ranking
  Sync             Yes      No                      Pending cleared

------------------------------------------------------------------------

# 176. Firebase Test Accounts

The team should maintain dedicated development/test accounts rather than
using real personal accounts for automated or repeated testing.

Recommended roles:

``` text
creator test account
player test account A
player test account B
```

The exact credentials must not be placed in this document or committed
to Git.

------------------------------------------------------------------------

# 177. Firestore Emulator

If the team uses the Firebase Emulator Suite, it may be used for:

``` text
security rule testing
repository testing
seed data testing
```

Emulator use is optional for the MVP.

Do not let emulator setup become a blocker to physical-device
integration.

------------------------------------------------------------------------

# 178. Security Rule Test Categories

At minimum:

``` text
authenticated allowed
authenticated forbidden
unauthenticated forbidden
owner allowed
non-owner forbidden
same-user allowed
cross-user forbidden
same-game allowed
cross-game forbidden
```

------------------------------------------------------------------------

# 179. Firestore Cost Awareness

Avoid:

``` text
continuous listener on every document
repeated full collection reads
unnecessary writes
```

Use listeners where real-time behavior materially matters.

Leaderboard observation may use a listener if required by the UI.

------------------------------------------------------------------------

# 180. Leaderboard Listener

Conceptual:

``` kotlin
fun observeGameLeaderboard(
    gameId: String
): Flow<List<GameLeaderboardEntry>>
```

The listener must target:

``` text
leaderboards/{gameId}/entries
```

not all leaderboard documents.

------------------------------------------------------------------------

# 181. Game List Listener

A one-time query may be sufficient for the game list.

A real-time listener is optional.

The MVP should favor simple, reliable behavior.

------------------------------------------------------------------------

# 182. Notification vs Firestore Listener

FCM and Firestore solve different problems.

``` text
FCM
 ↓
user notification

Firestore
 ↓
authoritative game data
```

The app must load the actual game document after notification
navigation.

------------------------------------------------------------------------

# 183. Notification Payload Trust

The notification's:

``` text
gameTitle
creatorName
checkpointCount
```

are presentation values.

The app should load authoritative details from Firestore using:

``` text
gameId
```

when the user opens the game.

------------------------------------------------------------------------

# 184. Cloud Data Migration Principle

If an older database contains:

``` text
relics/{relicId}
```

do not silently interpret it as the new schema.

Migration should explicitly map:

``` text
old relic
 ↓
sample checkpoint
 ↓
assigned game
```

for seed/demo purposes only.

------------------------------------------------------------------------

# 185. Legacy Compatibility

If legacy Kotlin aliases are temporarily retained:

``` kotlin
typealias Relic = Checkpoint
```

this is a code migration aid only.

Firebase production paths must still use:

``` text
games/{gameId}/checkpoints/{checkpointId}
```

------------------------------------------------------------------------

# 186. No Legacy Repository Contract

Do not use:

``` kotlin
interface QuestRepository
```

as the canonical new repository if it implies the old global relic
model.

The preferred shared name is:

``` kotlin
GameRepository
```

Specialized repositories may be introduced if useful:

``` text
AuthRepository
GameRepository
ProgressRepository
LeaderboardRepository
```

------------------------------------------------------------------------

# 187. Repository Specialization

If the team splits repositories:

``` text
AuthRepository
    → authentication

GameRepository
    → games/checkpoints/membership

ProgressRepository
    → discoveries/sync

LeaderboardRepository
    → game-specific rankings
```

The split must remain consistent across all documentation and code.

------------------------------------------------------------------------

# 188. Firestore Naming Conventions

Use:

``` text
camelCase
```

for fields.

Examples:

``` text
creatorId
checkpointCount
publishedAt
foundAt
lastUpdate
checkpointsDiscovered
```

Use plural collection names:

``` text
users
games
gamePlayers
progress
leaderboards
```

------------------------------------------------------------------------

# 189. Document ID Conventions

Document IDs must be stable.

Do not use display names as document IDs.

Do not use checkpoint names as IDs.

Do not use user email addresses as document IDs.

Use:

``` text
Firebase UID
game ID
checkpoint ID
```

------------------------------------------------------------------------

# 190. Email Security

Email addresses may exist in:

``` text
users/{uid}
```

but should not be duplicated unnecessarily into unrelated public
documents.

Leaderboards may show:

``` text
displayName
```

rather than email.

------------------------------------------------------------------------

# 191. Public Leaderboard Privacy

The leaderboard should expose only the minimum presentation information
required.

Recommended:

``` text
displayName
checkpointsDiscovered
totalCheckpoints
completion status
```

Avoid displaying:

``` text
email
```

on leaderboard screens.

------------------------------------------------------------------------

# 192. Game Creator Privacy

A game may show:

``` text
creatorName
```

but should not expose:

``` text
creator email
```

unless explicitly required by the product.

------------------------------------------------------------------------

# 193. Firebase Data Lifecycle

``` text
Create
 ↓
Draft
 ↓
Publish
 ↓
Play
 ↓
Discover
 ↓
Leaderboard
 ↓
Close
```

Data should remain sufficiently stable to support historical results.

------------------------------------------------------------------------

# 194. Closed Game Behavior

For a closed game:

``` text
progress remains
leaderboard remains
historical details remain
```

New participation behavior is controlled by the application.

The exact closed-game UI is not a core MVP blocker.

------------------------------------------------------------------------

# 195. Game Deletion

Hard deletion is not recommended for the MVP.

Prefer:

``` text
status = CLOSED
```

This avoids orphaning:

``` text
progress
leaderboards
gamePlayers
```

------------------------------------------------------------------------

# 196. Orphan Data Prevention

Before deleting or restructuring data, verify dependencies:

``` text
Game
 ├── Checkpoints
 ├── GamePlayers
 ├── Progress
 └── Leaderboard
```

The MVP should avoid destructive deletion operations.

------------------------------------------------------------------------

# 197. Backup and Recovery

Before final demonstration data is established, M5 should maintain a
reproducible seed dataset.

The seed dataset should be sufficient to recreate:

``` text
sample game
sample checkpoints
test players
```

Do not rely solely on manually entered Firebase Console data.

------------------------------------------------------------------------

# 198. Seed Data Reproducibility

Seed data should be documented as:

``` text
game_demo_001
```

with its checkpoint records.

The team may create a seed script or controlled Firebase Console
procedure.

------------------------------------------------------------------------

# 199. Development vs Production Rules

During development, temporary permissive rules may be used only for
short testing periods.

Before final demonstration:

``` text
authenticated
+
ownership
+
game scope
+
user scope
```

must be enforced.

Never submit an application with:

``` text
allow read, write: if true;
```

------------------------------------------------------------------------

# 200. Final Security Rule Checklist

Before final freeze:

``` text
[ ] Anonymous writes denied
[ ] Anonymous protected reads denied
[ ] Cross-user writes denied
[ ] Cross-game writes denied
[ ] Non-owner game edits denied
[ ] Non-owner checkpoint edits denied
[ ] UID cannot be reassigned
[ ] creatorId cannot be reassigned
[ ] Published games visible to players
[ ] Draft games hidden from normal players
[ ] Progress scoped by UID
[ ] Progress scoped by game
[ ] Leaderboard scoped by game
```

------------------------------------------------------------------------

# 201. M5 Implementation Checklist

``` text
[ ] Firebase project configured
[ ] Android app registered
[ ] Authentication enabled
[ ] Firestore enabled
[ ] FCM configured
[ ] users schema implemented
[ ] games schema implemented
[ ] checkpoints schema implemented
[ ] gamePlayers schema implemented
[ ] progress schema implemented
[ ] leaderboard schema implemented
[ ] security rules implemented
[ ] required indexes created
[ ] repository cloud methods implemented
[ ] notification flow implemented
[ ] Firebase errors mapped
[ ] seed data prepared
```

------------------------------------------------------------------------

# 202. M6 Integration Checklist

``` text
[ ] Room discovery entity matches Firebase identity
[ ] gameId preserved
[ ] checkpointId preserved
[ ] userId preserved
[ ] pendingSync implemented
[ ] duplicate sync prevented
[ ] cloud acknowledgement handled
[ ] offline discovery retained
[ ] leaderboard cache isolated by game
```

------------------------------------------------------------------------

# 203. Integration Checklist for M1--M4

### M1

``` text
[ ] game list uses repository
[ ] game details uses gameId
[ ] creator screens use Game
[ ] notification opens game details
```

### M2

``` text
[ ] scan uses selected game/checkpoint
[ ] reveal records gameId/checkpointId
[ ] leaderboard UI is game-specific
```

### M3

``` text
[ ] location receives selected checkpoint
[ ] geofence generated dynamically
[ ] no hard-coded R001 location
```

### M4

``` text
[ ] light signature comes from selected checkpoint
[ ] fusion uses GPS + light + motion
[ ] proximity remains final gate
```

------------------------------------------------------------------------

# 204. End-to-End Firebase Vertical Slice

The primary cloud-backed vertical slice is:

``` text
Creator Login
 ↓
Create Game
 ↓
Create Checkpoint
 ↓
Save Draft
 ↓
Publish
 ↓
FCM New Game Notification
 ↓
Player Login
 ↓
Browse Published Games
 ↓
Open Game Details
 ↓
Join
 ↓
Load Game Checkpoints
 ↓
Play
 ↓
Discover Checkpoint
 ↓
Room Save
 ↓
Firestore Progress
 ↓
Game Leaderboard Update
```

This is the main Firebase acceptance path.

------------------------------------------------------------------------

# 205. End-to-End Test Data

Use:

``` text
Game:
game_demo_001

Checkpoint:
checkpoint_001

Players:
test player A
test player B
```

Additional checkpoints can be added.

Do not make the test dependent on six checkpoints.

------------------------------------------------------------------------

# 206. Minimal Cloud Demo

If time becomes constrained, demonstrate:

``` text
Creator
 ↓
Create one game
 ↓
Add one checkpoint
 ↓
Publish
 ↓
Player sees game
 ↓
Player joins
 ↓
Player discovers checkpoint
 ↓
Progress syncs
 ↓
Leaderboard shows player
```

This proves the new architecture without requiring the entire
six-checkpoint dataset.

------------------------------------------------------------------------

# 207. Extended Cloud Demo

If the core flow is stable:

``` text
Create game with 3–6 checkpoints
 ↓
Publish
 ↓
Two players join
 ↓
Players discover different checkpoints
 ↓
Each game-specific leaderboard updates
 ↓
Demonstrate cross-game isolation
```

------------------------------------------------------------------------

# 208. Lecturer Evidence Mapping

Firebase requirements should produce visible evidence:

  Requirement           Evidence
  --------------------- ---------------------------------
  Authentication        Login/logout demo
  Firestore             Cloud documents
  Dynamic games         Creator-created game
  Dynamic checkpoints   Creator-added checkpoint
  Game membership       Join operation
  Progress              Cloud progress record
  Leaderboard           Game-specific ranking
  FCM                   New-game notification
  Security              Denied unauthorized operation
  Offline sync          Local discovery then cloud sync

------------------------------------------------------------------------

# 209. Questions M5 Should Be Able to Answer

## Why Firebase?

Because the application needs:

``` text
authentication
+
shared cloud state
+
multi-device synchronization
+
game-specific leaderboard data
```

## Why Firestore?

Because the data is structured around:

``` text
games
checkpoints
players
progress
leaderboards
```

and the team needs cloud-backed document/collection access.

------------------------------------------------------------------------

# 210. Why Is Room Still Needed?

Firebase is not a replacement for local persistence.

Room provides:

``` text
immediate local state
offline discovery
pending synchronization
```

Firebase provides:

``` text
shared cloud state
```

------------------------------------------------------------------------

# 211. Why Is Progress Game-Scoped?

Because one user may play multiple independent games.

Without:

``` text
gameId
```

progress would become ambiguous.

Therefore:

``` text
uid + gameId + checkpointId
```

is the canonical discovery identity.

------------------------------------------------------------------------

# 212. Why Is the Leaderboard Game-Scoped?

Because each game is an independent competition.

A player who has:

``` text
5 checkpoints in Game A
```

must not automatically receive:

``` text
5 points in Game B
```

------------------------------------------------------------------------

# 213. Why Are Checkpoints Nested Under Games?

Because creators configure their own games.

Therefore:

``` text
Game A → its checkpoints
Game B → its checkpoints
```

This makes the relationship explicit in the data model.

------------------------------------------------------------------------

# 214. Why Is FCM Separate from Firestore?

Firestore stores the authoritative game.

FCM delivers the notification.

The notification is not the database.

------------------------------------------------------------------------

# 215. Why Is Proximity Not Stored as Fusion Data?

Because proximity is a final confirmation gate.

The fusion score consists of:

``` text
GPS
+
Light
+
Motion
```

Then:

``` text
Fusion threshold
 ↓
Proximity
 ↓
Reveal
```

------------------------------------------------------------------------

# 216. Why Not Upload Sensor Data?

The core requirement is to verify a local interaction.

Uploading raw sensor streams would:

``` text
increase complexity
increase storage
increase privacy concerns
increase network usage
```

without being required for the MVP.

------------------------------------------------------------------------

# 217. Why Not Use a Global Relic Collection?

Because the creator/player model requires dynamic game configuration.

A global collection would not naturally express:

``` text
Game A owns checkpoints
Game B owns checkpoints
```

The canonical structure is therefore:

``` text
games/{gameId}/checkpoints/{checkpointId}
```

------------------------------------------------------------------------

# 218. Cloud Contract Freeze

Before major integration:

``` text
Firestore paths
field names
repository signatures
identity rules
leaderboard scope
notification payload
```

must be frozen.

Changes after this point require team agreement.

------------------------------------------------------------------------

# 219. Change Control

Any proposed Firebase schema change must answer:

``` text
What changes?
Why?
Which members are affected?
Does Room change?
Does repository change?
Does UI change?
Does security rules change?
Does seed data change?
Does testing change?
```

------------------------------------------------------------------------

# 220. Breaking Change Examples

These are breaking changes:

``` text
games → quests
checkpoint → relic
gameId removed from progress
global leaderboard introduced
creatorId renamed
checkpoint path changed
```

Such changes require coordinated updates.

------------------------------------------------------------------------

# 221. Non-Breaking Changes

Examples:

``` text
adding optional Game field
adding optional leaderboard presentation field
adding optional notification metadata
adding indexes
```

provided existing consumers continue to work.

------------------------------------------------------------------------

# 222. Firebase Documentation Rule

Any Firebase implementation change must update this document before the
team treats the new contract as final.

Do not allow:

``` text
code says one schema
documentation says another
```

------------------------------------------------------------------------

# 223. Code Review Requirements

M5 pull requests should be checked for:

``` text
correct Firestore path
correct authenticated UID
correct game scope
correct checkpoint parent
security-rule compatibility
error handling
offline compatibility
no hard-coded R001 dependency
```

------------------------------------------------------------------------

# 224. No Hard-Coded Production IDs

Avoid code such as:

``` kotlin
getCheckpoint("R001")
```

for normal gameplay.

Correct:

``` kotlin
getCheckpoint(
    gameId = selectedGameId,
    checkpointId = selectedCheckpointId
)
```

------------------------------------------------------------------------

# 225. No Hard-Coded Game Counts

Avoid:

``` kotlin
if (foundCount == 6)
```

Correct:

``` kotlin
if (foundCount == totalCheckpoints)
```

------------------------------------------------------------------------

# 226. No Global Found Count

Avoid:

``` text
allFoundRelics.size
```

for game leaderboard logic.

Correct:

``` text
selectedGameProgress.size
```

or its repository-provided game-scoped equivalent.

------------------------------------------------------------------------

# 227. No Global Leaderboard Query

Avoid:

``` text
all leaderboard entries
```

and filtering in the UI.

Correct:

``` text
leaderboards/{selectedGameId}/entries
```

------------------------------------------------------------------------

# 228. Repository Must Carry Context

When operating on a checkpoint, the repository should know:

``` text
gameId
checkpointId
```

When operating on progress, it should know:

``` text
userId
gameId
checkpointId
```

When operating on leaderboard:

``` text
gameId
userId
```

------------------------------------------------------------------------

# 229. Canonical Firebase Object Relationships

``` text
User
 ├── creates → Game
 └── joins → GamePlayer

Game
 └── contains → Checkpoint

GamePlayer
 └── identifies participation

User + Game + Checkpoint
 └── identifies Discovery

Game + User
 └── identifies Leaderboard Entry
```

------------------------------------------------------------------------

# 230. Final Canonical Schema

``` text
users
└── {uid}

games
└── {gameId}
    └── checkpoints
        └── {checkpointId}

gamePlayers
└── {gameId}_{uid}

progress
└── {uid}
    └── games
        └── {gameId}
            └── checkpoints
                └── {checkpointId}

leaderboards
└── {gameId}
    └── entries
        └── {uid}
```

------------------------------------------------------------------------

# 231. Final FCM Model

``` text
/topics/new_games
```

Payload:

``` text
gameId
gameTitle
creatorName
checkpointCount
publishedAt
```

Routing:

``` text
notification
 ↓
gameId
 ↓
Game Details
```

------------------------------------------------------------------------

# 232. Final Ownership Model

``` text
M5
 ↓
Firebase
 ├── Auth
 ├── Firestore
 ├── Security
 ├── FCM
 ├── Cloud repository
 └── Cloud progress/leaderboard

M6
 ↓
Room
 ├── Cache
 ├── Offline discovery
 ├── pendingSync
 └── Sync integration
```

------------------------------------------------------------------------

# 233. Final Non-Negotiable Rules

1.  Firebase UID is the authoritative user identity.
2.  `gameId` is required to scope checkpoint gameplay.
3.  Checkpoints belong to games.
4.  Progress belongs to a user and a game.
5.  Leaderboards belong to a game.
6.  A player may participate in multiple games.
7.  Cross-game progress must never be merged.
8.  Draft games are not public player games.
9.  Only the creator may configure their own draft.
10. Published games trigger the new-game notification flow.
11. FCM notification data is not the source of truth.
12. Room handles local/offline persistence.
13. Firestore handles shared cloud state.
14. Raw sensor streams are not stored as core Firebase data.
15. Proximity is not part of the fusion score.
16. Firestore rules must enforce authorization independently of UI.
17. No production dependency on R001--R006.
18. No global relic collection.
19. No global leaderboard.
20. No cross-user progress writes.
21. No cross-game leaderboard writes.
22. Repository is the Firebase boundary for the application.
23. Firebase credentials and secrets must never be committed.
24. Published gameplay configuration should remain stable.
25. Security rules must be tested before final submission.

------------------------------------------------------------------------

# 234. Final Firebase Vertical Slice

The complete Firebase-backed target is:

``` text
AUTH
 ↓
Firebase UID
 ↓
USER PROFILE
 ↓
GAME CREATOR
 ↓
CREATE GAME
 ↓
CREATE CHECKPOINTS
 ↓
DRAFT
 ↓
PUBLISH
 ↓
FCM NEW GAME
 ↓
GAME PLAYER
 ↓
BROWSE PUBLISHED GAMES
 ↓
GAME DETAILS
 ↓
JOIN
 ↓
GAME CHECKPOINTS
 ↓
GAMEPLAY
 ↓
DISCOVERY
 ↓
ROOM
 ↓
FIRESTORE PROGRESS
 ↓
GAME-SCOPED LEADERBOARD
```

------------------------------------------------------------------------

# 235. Final Implementation Priority

If time becomes constrained, implement in this order:

``` text
1. Firebase Authentication
2. users
3. games
4. checkpoints
5. published-game query
6. game joining
7. progress
8. leaderboard
9. security rules
10. offline sync
11. FCM notification
12. additional robustness
```

Do not spend the final development days on optional Firebase features
while the core game-specific data flow is incomplete.

------------------------------------------------------------------------

# 236. Final Acceptance Criteria

The Firebase implementation is acceptable when all of the following are
true:

``` text
[ ] A user can authenticate.
[ ] A creator can create a draft game.
[ ] A creator can add checkpoints dynamically.
[ ] A creator can publish the game.
[ ] Published games appear to players.
[ ] A player can join a published game.
[ ] The selected game's checkpoints load dynamically.
[ ] A successful discovery can be persisted.
[ ] Discovery is scoped by user + game + checkpoint.
[ ] Offline discovery is retained locally.
[ ] Pending discovery synchronizes to Firestore.
[ ] A game-specific leaderboard updates.
[ ] Two games maintain independent leaderboards.
[ ] FCM can announce a newly published game.
[ ] Notification tap opens the correct game.
[ ] Unauthorized game edits are denied.
[ ] Cross-user progress writes are denied.
[ ] Cross-game writes are denied.
[ ] No production logic depends on R001–R006.
```

------------------------------------------------------------------------

# 237. Source-of-Truth Statement

For Firebase implementation, this document must remain consistent with:

``` text
00_MASTER_DEVELOPMENT_PLAN.md
SHARED_CONTRACTS_AND_AGREEMENTS.md
MOCK_DATA_CATALOG.md
INTEGRATION_AND_HANDOFF_PLAN.md
M5_FIREBASE_CLOUD_SYNC_WORKPLAN.md
M6_ROOM_DATA_INTEGRATION_WORKPLAN.md
```

If another document contradicts the canonical Firebase paths or
game-scoped data model defined here, the contradiction must be resolved
before implementation proceeds.

------------------------------------------------------------------------

# 238. Final Schema Summary

``` text
AUTH
  Firebase Authentication
        │
        ▼
users/{uid}
        │
        ├──────────────┐
        │              │
        ▼              ▼
   GAME CREATOR    GAME PLAYER
        │              │
        ▼              ▼
games/{gameId}    gamePlayers/{gameId}_{uid}
        │
        ▼
games/{gameId}/checkpoints/{checkpointId}
        │
        ▼
LOCAL GAMEPLAY
        │
        ▼
progress/{uid}/games/{gameId}/checkpoints/{checkpointId}
        │
        ▼
leaderboards/{gameId}/entries/{uid}

PUBLISH EVENT
        │
        ▼
/topics/new_games
        │
        ▼
PLAYER NOTIFICATION
        │
        ▼
Game Details(gameId)
```

------------------------------------------------------------------------

# 239. Document Freeze

Once the team begins full integration, the following should be treated
as frozen contracts unless a coordinated change is approved:

``` text
users/{uid}

games/{gameId}

games/{gameId}/checkpoints/{checkpointId}

gamePlayers/{gameId}_{uid}

progress/{uid}/games/{gameId}/checkpoints/{checkpointId}

leaderboards/{gameId}/entries/{uid}

/topics/new_games
```

The repository signatures, identity rules, and game-specific leaderboard
model should remain aligned with these paths.

------------------------------------------------------------------------

# 240. Final M5 Statement

M5's Firebase implementation must support the product model rather than
the original fixed relic prototype.

The backend target is:

``` text
Creators create games.
Games contain dynamic checkpoints.
Players join games.
Players discover checkpoints.
Discoveries are scoped to user + game + checkpoint.
Each game has its own leaderboard.
Published games generate notifications.
Room provides local/offline persistence.
Firestore provides shared cloud state.
Security rules protect ownership and identity.
```

This is the canonical Firebase architecture for Campus Quest.
