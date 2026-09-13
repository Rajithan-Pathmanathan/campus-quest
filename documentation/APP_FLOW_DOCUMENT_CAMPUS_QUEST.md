# APP FLOW DOCUMENT
## Campus Quest — Final Dynamic Creator & Player Application Flow

**Status:** Final reassignment-compatible version  
**Platform:** Android  
**Primary architecture:** MVVM + Repository + Room + Firebase/Firestore  
**Core interaction:** Dynamic game + dynamic checkpoints + physical sensor-fusion discovery

---

# 1. PURPOSE

This document defines the complete application flow for Campus Quest.

There are two primary user roles:

```text
GAME CREATOR
GAME PLAYER
```

The application flow is dynamic.

The production flow must not depend on:

```text
six fixed checkpoints
R001–R006
one fixed game
global leaderboard
```

---

# 2. HIGH-LEVEL APPLICATION FLOW

```text
App Launch
   ↓
Authentication
   ↓
Role-aware Home
   ├───────────────┐
   ↓               ↓
Player            Creator
   ↓               ↓
Games             Creator Games
   ↓               ↓
Game Details      Create/Edit Game
   ↓               ↓
Join              Checkpoints
   ↓               ↓
Game Map          Draft
   ↓               ↓
Checkpoint        Publish
   ↓               ↓
Scan              Notification
   ↓
Fusion
   ↓
Proximity Gate
   ↓
Reveal
   ↓
Discovery
   ↓
Game Leaderboard
```

---

# 3. AUTHENTICATION FLOW

```text
Launch
 ↓
Check authentication state
 ↓
Authenticated?
 ├── No → Login
 └── Yes → Role-aware home
```

Authentication is handled through Firebase Authentication.

---

# 4. LOGIN

Player enters:

```text
email/credentials
```

or the configured authentication method.

States:

```text
Loading
Success
Invalid credentials
Network error
Unknown error
```

On success:

```text
Firebase Auth
 ↓
authenticated UID
 ↓
application home
```

---

# 5. LOGOUT

```text
Home
 ↓
Logout
 ↓
Firebase session cleared
 ↓
Login
```

Local user-scoped transient state must not leak into another authenticated user session.

---

# 6. ROLE-AWARE HOME

The authenticated user may access:

```text
Player functionality
Creator functionality
```

according to the application's role model.

The UI must not use display name as the user's identity.

The authoritative identity is:

```text
uid
```

---

# 7. PLAYER FLOW

```text
Player Home
 ↓
Available Games
 ↓
Game Details
 ↓
Join
 ↓
Game Map
 ↓
Checkpoint
 ↓
Scan
 ↓
Reveal
 ↓
Progress
 ↓
Game Leaderboard
```

---

# 8. PLAYER — AVAILABLE GAMES

The player sees published games.

Conceptual data:

```text
Game ID
Title
Description
Creator
Checkpoint count
Publication information
```

Only discoverable/published games should appear as active games.

---

# 9. GAME LIST STATES

The screen should support:

```text
Loading
Content
Empty
Error
Retry
```

Empty state:

```text
No published games are currently available.
```

The exact UI wording follows the approved UI/UX specification.

---

# 10. GAME DETAILS

Navigation:

```text
GameDetails(gameId)
```

The screen loads the game using:

```text
gameId
```

It must not rely on a hard-coded game.

Display may include:

```text
title
description
creator
checkpoint count
status
join action
```

---

# 11. JOIN GAME

```text
Game Details
 ↓
Join
 ↓
membership request
 ↓
success
 ↓
Game Map
```

Membership is scoped by:

```text
gameId + userId
```

---

# 12. JOIN STATES

```text
Joining
Joined
Already Joined
Network Error
Permission/Authorization Error
```

After successful joining, the player can enter the game's gameplay flow.

---

# 13. GAME MAP

Navigation:

```text
GameMap(gameId)
```

The map loads:

```text
getGameCheckpoints(gameId)
```

It must not use a universal checkpoint list.

---

# 14. MAP CONTENT

The map may show:

```text
player location
checkpoint markers
progress
game information
navigation/approach information
```

Only checkpoints belonging to the current game should be displayed.

---

# 15. DYNAMIC CHECKPOINTS

A game may contain:

```text
2
4
6
8
...
```

checkpoints.

The UI should generate the list/map markers from returned data.

Do not create six permanent UI slots.

---

# 16. CHECKPOINT ORDER

Checkpoint order is obtained from:

```text
Checkpoint.order
```

The application must not infer order from:

```text
R001
R002
R003
```

or other ID naming conventions.

---

# 17. CHECKPOINT SELECTION

Navigation:

```text
Checkpoint(gameId, checkpointId)
```

The current checkpoint must be resolved using both:

```text
gameId
checkpointId
```

This prevents identical checkpoint IDs in different games from colliding.

---

# 18. CHECKPOINT GAMEPLAY

The checkpoint screen may display:

```text
checkpoint name
clue
distance/proximity
scan availability
progress/status
```

The physical signal processing remains owned by M3.

---

# 19. GEOFENCE ENTER

Typical flow:

```text
Player approaches checkpoint
 ↓
Location updates
 ↓
Checkpoint geofence ENTER
 ↓
Scan becomes available
```

Geofence ENTER does not mean the checkpoint has been discovered.

---

# 20. GEOFENCE RULE

Geofences are dynamic and game-scoped.

When the active game changes:

```text
old game geofences
        ↓
removed
        ↓
new game geofences
        ↓
registered
```

An event from an old game must not activate the current game's scan.

---

# 21. SCAN FLOW

```text
Scan opened
 ↓
Check location
 ↓
Collect physical signals
 ↓
GPS score
Light score
Motion score
 ↓
Weighted fusion
 ↓
Threshold?
 ├── No → Continue collecting
 └── Yes
       ↓
   Proximity final gate
       ↓
   Near?
   ├── No → Waiting for proximity
   └── Yes → Success
```

---

# 22. SCAN PRINCIPLE

The weighted fusion uses exactly:

```text
GPS
+
Light
+
Motion
```

Proximity is separate.

It is not:

```text
GPS + Light + Motion + Proximity
```

as a four-input weighted model.

---

# 23. GPS SIGNAL

M3 provides:

```text
distance
accuracy
normalized location score
```

The UI may display:

```text
Location
```

as a signal status.

The scan screen must not calculate GPS itself.

---

# 24. LIGHT SIGNAL

M3 provides:

```text
current lux
light match/score
availability
```

The light signature represents expected ambient environmental light.

The UI should not interpret it as emitted relic light.

---

# 25. MOTION SIGNAL

M3 provides:

```text
motion score
sweep detected
availability
```

The scan UI presents the motion requirement but does not directly read the accelerometer.

---

# 26. FUSION METER

The scan screen may display:

```text
0–100%
```

or an equivalent progress representation.

The value represents:

```text
GPS + Light + Motion
```

weighted fusion.

The numerical weighting belongs to M3's fusion layer.

---

# 27. FUSION THRESHOLD

When:

```text
fusionScore >= configured threshold
```

the scan moves to:

```text
proximity final gate
```

The threshold must not be implemented separately in multiple screens.

---

# 28. PROXIMITY FINAL GATE

Condition:

```text
fusion threshold reached
AND
proximity near
```

Only then:

```text
successful scan
```

---

# 29. WAITING FOR PROXIMITY

If:

```text
fusionScore >= threshold
```

but:

```text
proximityNear = false
```

show an appropriate state such as:

```text
Move closer / complete the final confirmation
```

Do not reveal the checkpoint yet.

---

# 30. SCAN FAILURE

Possible reasons:

```text
location unavailable
low location accuracy
light sensor unavailable
motion unavailable
invalid checkpoint
scan cancelled
timeout if implemented
```

The UI should provide an actionable state.

---

# 31. SENSOR DEGRADATION

If a physical signal becomes unavailable:

```text
do not fabricate a successful signal
```

M3 reports the degraded/unavailable state.

M4 presents the appropriate scan state.

---

# 32. SCAN STATE MACHINE

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

# 33. SUCCESS

On successful physical confirmation:

```text
Scan Success
 ↓
Reveal
```

M4 controls the transition.

---

# 34. REVEAL

Reveal may display:

```text
checkpoint name
lore
clue/result
completion state
```

The exact visual design follows the UI/UX specification.

---

# 35. DISCOVERY PERSISTENCE

After successful reveal/discovery:

```text
M4
 ↓
GameRepository.recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
 ↓
M6 local persistence
 ↓
M5 cloud synchronization
```

The scan UI does not directly write Room or Firestore.

---

# 36. OFFLINE DISCOVERY

If the network is unavailable:

```text
physical discovery
 ↓
Room
 ↓
pendingSync = true
```

The player should not lose a locally successful discovery solely because the network is unavailable.

---

# 37. SYNC

When connectivity returns:

```text
pending discovery
 ↓
repository sync
 ↓
Firestore
 ↓
acknowledgement
 ↓
pendingSync = false
```

Repeated synchronization must be idempotent.

---

# 38. PROGRESS

Player progress is scoped to:

```text
userId
gameId
checkpointId
```

The same checkpoint ID in another game is a different progress record.

---

# 39. DUPLICATE DISCOVERY

If the player repeats a completed checkpoint:

```text
do not create another logical completion
```

The repository/data layer must protect against:

```text
duplicate taps
duplicate geofence events
retries
offline sync duplication
```

---

# 40. GAME COMPLETION

When all required checkpoints in the current game are completed:

```text
game completion
```

The application should use the actual checkpoint collection/count.

It must not assume:

```text
completion = R006
```

---

# 41. GAME-SPECIFIC LEADERBOARD

After progress changes:

```text
Game Leaderboard(gameId)
```

The leaderboard is scoped to the current game.

There is no global leaderboard in the production architecture.

---

# 42. LEADERBOARD NAVIGATION

```text
Leaderboard(gameId)
```

The screen observes:

```text
observeGameLeaderboard(gameId)
```

---

# 43. LEADERBOARD STATES

```text
Loading
Content
Empty
Error
```

The current player's progress/rank may be highlighted.

---

# 44. CREATOR FLOW

```text
Creator Home
 ↓
Create Game
 ↓
Game Details/Edit
 ↓
Checkpoint List
 ↓
Add/Edit Checkpoint
 ↓
Save Draft
 ↓
Validate
 ↓
Publish
```

---

# 45. CREATOR HOME

Display:

```text
creator's games
drafts
published games
closed games
create action
```

Creator games must be filtered by:

```text
creatorId == authenticated uid
```

Backend security rules remain authoritative.

---

# 46. CREATE GAME

Required fields:

```text
title
description
```

System fields:

```text
gameId
creatorId
creatorName
status
createdAt
```

Initial status:

```text
DRAFT
```

---

# 47. EDIT GAME

Navigation:

```text
EditGame(gameId)
```

Only the creator should be able to edit their game.

Editable fields may include:

```text
title
description
```

according to publication state.

---

# 48. CHECKPOINT LIST

Navigation:

```text
CheckpointList(gameId)
```

The list is loaded dynamically.

Actions:

```text
Add
Edit
Delete
Reorder
```

---

# 49. ADD CHECKPOINT

Navigation:

```text
EditCheckpoint(gameId, checkpointId)
```

For a new checkpoint, the implementation may use a temporary local ID until persistence assigns the final identifier.

Required configuration:

```text
name
coordinates
radius
light signature
clue
lore
order
motion type
rarity
```

---

# 50. COORDINATE CONFIGURATION

Creator supplies:

```text
latitude
longitude
```

The application should validate coordinate ranges.

The actual final physical demo coordinates must be verified on location.

---

# 51. RADIUS CONFIGURATION

Creator configures:

```text
radiusM
```

The value must be positive.

M3 consumes this configuration for physical proximity/geofencing.

---

# 52. LIGHT CONFIGURATION

Creator configures:

```text
minLux
maxLux
```

Rules:

```text
minLux >= 0
maxLux >= 0
minLux <= maxLux
```

---

# 53. CLUE AND LORE

Creator can define:

```text
clue
lore
```

These are checkpoint content and should be stored with the checkpoint.

---

# 54. RARITY

Supported content values may include:

```text
COMMON
RARE
EPIC
LEGENDARY
```

Rarity is content metadata unless another approved game rule explicitly uses it.

---

# 55. MOTION TYPE

Initial MVP value:

```text
SWEEP
```

The creator may configure it if the UI supports multiple motion types.

M3 owns interpretation of the physical motion mechanic.

---

# 56. DELETE CHECKPOINT

Deleting a checkpoint should:

```text
remove it from the creator's current game configuration
```

The implementation must account for:

```text
order updates
player-visible data
existing progress
```

according to the product's published-game rules.

---

# 57. REORDER CHECKPOINTS

Example:

```text
1 → C001
2 → C002
3 → C003
```

becomes:

```text
1 → C003
2 → C001
3 → C002
```

Player progression must use the updated `order`.

---

# 58. SAVE DRAFT

```text
Creator edits
 ↓
Save
 ↓
DRAFT
```

A draft may be saved without publishing.

Drafts should not become visible as active player games.

---

# 59. PUBLISH VALIDATION

Before publishing, validate:

```text
title
description
creator ownership
checkpoint configuration
coordinates
radius
light range
clue
required ordering
```

The exact minimum checkpoint rule is controlled by the final product requirements.

---

# 60. PUBLISH

```text
Draft
 ↓
Publish confirmation
 ↓
Backend validation
 ↓
PUBLISHED
 ↓
publishedAt
 ↓
new-game notification
```

---

# 61. PUBLISH FAILURE

Possible:

```text
validation failure
authorization failure
network failure
server failure
```

The draft should remain recoverable.

Do not silently mark a failed publication as successful.

---

# 62. NEW-GAME NOTIFICATION

After publication:

```text
FCM topic:
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

# 63. NOTIFICATION TAP

```text
FCM
 ↓
gameId
 ↓
GameDetails(gameId)
```

Never navigate to a hard-coded game.

---

# 64. NOTIFICATION FAILURE

A failed notification must not undo:

```text
successful publication
```

The game remains:

```text
PUBLISHED
```

---

# 65. GAME STATUS FLOW

```text
DRAFT
 ↓
PUBLISHED
 ↓
CLOSED
```

The exact permissions/visibility of closed games follow the final product policy.

---

# 66. CLOSED GAME

When:

```text
status = CLOSED
```

the application should consistently handle:

```text
new joins
existing player access
leaderboard visibility
creator editing
```

across M1, M2, M5, and M6.

---

# 67. PLAYER NOTIFICATION FLOW

```text
New game published
 ↓
FCM
 ↓
Player receives notification
 ↓
Tap
 ↓
Game Details(gameId)
 ↓
Join
```

---

# 68. NAVIGATION CONTRACT

Important routes:

```text
Games
GameDetails(gameId)
GameMap(gameId)
Checkpoint(gameId, checkpointId)
Scan(gameId, checkpointId)
Reveal(gameId, checkpointId)
Leaderboard(gameId)

CreatorHome
CreateGame
EditGame(gameId)
CheckpointList(gameId)
EditCheckpoint(gameId, checkpointId)
PublishGame(gameId)
```

---

# 69. BACK NAVIGATION

Back navigation should preserve the logical game context.

Examples:

```text
Scan → Checkpoint → Map
Reveal → Checkpoint/Map
Game Details → Games
Edit Checkpoint → Checkpoint List
```

The exact stack follows the approved navigation implementation.

---

# 70. ROTATION

During scan:

```text
device rotates
```

Expected:

```text
no duplicate sensor pipeline
no duplicate geofence registration
scan state preserved appropriately
```

ViewModel state should survive configuration changes where required.

---

# 71. APP BACKGROUNDING

If the player backgrounds the application:

```text
active resources are lifecycle-managed
```

M3 controls sensor/location lifecycle.

M4 controls the scan-state presentation.

---

# 72. PERMISSION FLOW

For location:

```text
Scan/map requires location
 ↓
permission?
 ├── Granted → continue
 └── Denied → permission guidance
```

The application must distinguish ordinary denial from permanently restricted access where the platform provides that distinction.

---

# 73. LOCATION FAILURE

If location cannot be obtained:

```text
WaitingForLocation
```

or an appropriate failure state.

Do not report a successful physical match.

---

# 74. SENSOR FAILURE

If a required sensor is unavailable:

```text
SensorUnavailable / degraded state
```

The UI should explain what is required when appropriate.

---

# 75. NETWORK FAILURE

For cloud operations:

```text
show recoverable error
retry
or use supported offline behavior
```

For discovery:

```text
successful local save
```

should remain possible where checkpoint data is already cached and the physical mechanic is available.

---

# 76. AUTH FAILURE

If the session becomes invalid:

```text
clear protected session state
return to authentication
```

Do not expose another user's local session state.

---

# 77. EMPTY STATES

Important empty states:

```text
No published games
No creator games
No checkpoints in draft
No leaderboard entries
No local cached games
```

Each should have a meaningful next action where applicable.

---

# 78. ERROR STATES

Important errors:

```text
Authentication
Network
Permission
Location
Sensor
Validation
Publication
Join
Sync
```

Errors should not leave the UI in an indefinite loading state.

---

# 79. OFFLINE APP START

If cached state exists:

```text
app start
 ↓
Room cache
 ↓
display supported local data
```

If no cached data exists:

```text
empty/offline state
```

---

# 80. OFFLINE CREATOR DRAFT

If creator drafts are supported locally:

```text
creator edits
 ↓
Room/local draft
 ↓
pending synchronization
```

The final implementation must clearly distinguish local draft state from published cloud state.

---

# 81. OFFLINE PLAYER

Supported cached data may include:

```text
game
checkpoint
membership
progress
```

The player may continue supported local gameplay.

---

# 82. DISCOVERY SYNC

```text
Local discovery
 ↓
pendingSync
 ↓
network returns
 ↓
repository
 ↓
Firestore
 ↓
cloud confirmation
```

---

# 83. MULTI-GAME FLOW

```text
Player joins Game A
 ↓
Game A map/geofences
 ↓
Player leaves/changes context
 ↓
Game B
 ↓
Game B map/geofences
```

Old Game A state must not trigger Game B scan behavior.

---

# 84. SAME CHECKPOINT ID

If:

```text
Game A / R001
Game B / R001
```

both exist:

```text
Game A:R001 ≠ Game B:R001
```

Navigation, Room, Firestore, geofence, progress, and leaderboard behavior must preserve this distinction.

---

# 85. GAME COMPLETION FLOW

```text
Last required checkpoint discovered
 ↓
progress updated
 ↓
completion determined
 ↓
completion UI
 ↓
game leaderboard
```

The final checkpoint is determined from game data/order, not an ID such as `R006`.

---

# 86. CREATOR-PLAYER END-TO-END

```text
Creator
 ↓
Create game
 ↓
Add N checkpoints
 ↓
Save draft
 ↓
Publish
 ↓
FCM

Player
 ↓
Open notification
 ↓
Game Details
 ↓
Join
 ↓
Map
 ↓
Checkpoint
 ↓
Geofence
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
Discovery
 ↓
Leaderboard
```

---

# 87. APPLICATION STATE OWNERSHIP

| State | Owner |
|---|---|
| Login/auth state | Auth/ViewModel |
| Player game list | M1/ViewModel + Repository |
| Creator game form | M2/ViewModel |
| Location/sensor state | M3 |
| Scan state | M4/ViewModel |
| Cloud state | M5 |
| Local persistence/sync | M6 |
| Leaderboard presentation | M1 |
| Leaderboard cloud data | M5 |

---

# 88. DATA FLOW

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firebase
```

Physical pipeline:

```text
Android hardware
 ↓
M3
 ↓
FusionResult
 ↓
M4
 ↓
UI
```

---

# 89. NO DIRECT UI DATA ACCESS

Screens should not directly perform:

```text
Firestore query
Room query
sensor registration
location callback management
```

Those responsibilities belong to the appropriate layer.

---

# 90. DYNAMIC FLOW ACCEPTANCE

The flow is considered correct only if:

```text
creator can create arbitrary valid games
player can discover published games
player can join a selected game
map loads that game's checkpoints
physical signals use that game's checkpoint configuration
progress remains game/user scoped
leaderboard remains game scoped
```

---

# 91. TEST FLOW — NEW GAME

```text
Create Game A
 ↓
2 checkpoints
 ↓
Publish
 ↓
Player sees Game A
 ↓
Join
 ↓
Map shows 2
```

Then:

```text
Create Game B
 ↓
4 checkpoints
 ↓
Publish
 ↓
Player sees Game B
 ↓
Map shows 4
```

No code change should be required.

---

# 92. TEST FLOW — NOTIFICATION

```text
Creator publishes
 ↓
FCM
 ↓
Player notification
 ↓
Tap
 ↓
Correct gameId
 ↓
Correct Game Details
```

---

# 93. TEST FLOW — PHYSICAL DISCOVERY

```text
Enter geofence
 ↓
Scan
 ↓
GPS signal
Light signal
Motion signal
 ↓
Fusion
 ↓
Threshold
 ↓
Proximity
 ↓
Reveal
```

---

# 94. TEST FLOW — FALSE POSITIVE

```text
Fusion threshold reached
 ↓
Proximity false
 ↓
No reveal
```

---

# 95. TEST FLOW — OFFLINE

```text
Cached checkpoint
 ↓
Network off
 ↓
Physical scan
 ↓
Discovery
 ↓
Room
 ↓
Pending sync
 ↓
Network on
 ↓
Firebase
```

---

# 96. TEST FLOW — ISOLATION

```text
Player A completes:
Game A / R001

Verify:
Player B / Game A / R001 = incomplete

Verify:
Player A / Game B / R001 = incomplete
```

---

# 97. FINAL APPLICATION FLOW

```text
                         ┌──────────────┐
                         │  APP LAUNCH  │
                         └──────┬───────┘
                                ↓
                         ┌──────────────┐
                         │ AUTH CHECK   │
                         └──────┬───────┘
                                ↓
                    ┌───────────┴───────────┐
                    ↓                       ↓
                 PLAYER                  CREATOR
                    ↓                       ↓
               GAME LIST              CREATOR GAMES
                    ↓                       ↓
              GAME DETAILS             CREATE/EDIT
                    ↓                       ↓
                  JOIN                 CHECKPOINTS
                    ↓                       ↓
                GAME MAP                  DRAFT
                    ↓                       ↓
               CHECKPOINT              VALIDATE
                    ↓                       ↓
                  SCAN                  PUBLISH
                    ↓                       ↓
          GPS + LIGHT + MOTION             FCM
                    ↓
               FUSION
                    ↓
             THRESHOLD
                    ↓
          PROXIMITY GATE
                    ↓
                 REVEAL
                    ↓
              DISCOVERY
                    ↓
              ROOM/SYNC
                    ↓
          GAME LEADERBOARD
```

---

# 98. FINAL NON-NEGOTIABLE FLOW RULES

```text
1. Games are dynamic.
2. Checkpoints are dynamic.
3. Creator creates and publishes games.
4. Player discovers and joins published games.
5. Every gameplay route carries gameId.
6. Checkpoint routes carry gameId + checkpointId.
7. Geofences are game-scoped.
8. GPS + Light + Motion form the weighted fusion.
9. Proximity is a separate final gate.
10. Reveal occurs only after fusion threshold + proximity.
11. Discovery is user/game/checkpoint scoped.
12. Room handles local persistence.
13. Firebase handles cloud persistence.
14. Repository separates UI from storage.
15. Leaderboards are game-specific.
16. FCM navigation uses the actual gameId.
17. R001–R006 are demo data only.
18. No global leaderboard.
19. No fixed six-checkpoint production flow.
20. Offline discoveries are synchronized later.
```

---

# 99. FINAL ACCEPTANCE CHECKLIST

```text
[ ] Login works
[ ] Player home works
[ ] Creator home works
[ ] Player sees published games
[ ] Creator creates a game
[ ] Creator saves draft
[ ] Creator adds checkpoints
[ ] Creator edits checkpoints
[ ] Creator deletes/reorders checkpoints
[ ] Creator publishes
[ ] Player receives notification
[ ] Notification opens correct game
[ ] Player joins
[ ] Game map loads dynamic checkpoints
[ ] Geofence uses selected game
[ ] Scan receives M3 physical signals
[ ] Fusion displays progress
[ ] Proximity final gate works
[ ] Reveal works
[ ] Discovery persists
[ ] Offline discovery persists locally
[ ] Sync works
[ ] Leaderboard is game-specific
[ ] Multi-game isolation works
[ ] Multi-user isolation works
[ ] No fixed R001–R006 production dependency
```

---

# 100. FINAL DOCUMENT STATUS

This flow is aligned with the final implementation ownership:

```text
M1 — Player UI & Navigation
M2 — Creator & Game Management
M3 — Location + Sensors + Fusion
M4 — Quest & Scan Gameplay
M5 — Firebase & Backend
M6 — Room + Repository + Sync + Integration
```

The application flow is intentionally dynamic so that the same application can support creator-generated games without changing the source code for each new game.

---

**END OF APP FLOW DOCUMENT**
