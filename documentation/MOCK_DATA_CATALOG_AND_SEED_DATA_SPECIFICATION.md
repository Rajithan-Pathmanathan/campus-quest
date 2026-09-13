# MOCK DATA CATALOG & SEED DATA SPECIFICATION
## Campus Quest — Final Dynamic Game/Checkpoint Architecture

**Status:** Final reassignment-compatible version  
**Purpose:** Define development, UI-preview, testing, and controlled demo data without allowing mock data to become the production architecture.

---

# 1. PURPOSE

This document defines the mock and seed data required for Campus Quest.

The project uses a dynamic model:

```text
Game
 └── Checkpoints
```

Games and checkpoints are created dynamically by creators.

The historical `R001–R006` identifiers are retained only as sample/demo seed data. They must not represent a permanent six-checkpoint production limitation.

---

# 2. DATA CATEGORIES

Campus Quest should distinguish:

```text
1. UI mock data
2. Unit-test fixtures
3. Repository fake data
4. Seed/demo data
5. Production Firebase data
```

These must not be mixed.

---

# 3. DATA LIFECYCLE

```text
UI mock
   ↓
development fixture
   ↓
controlled seed/demo
   ↓
real Firebase/Room data
```

Mock data should be progressively removed from production paths.

---

# 4. PRIMARY DEMO GAME

Use the following controlled seed game:

```text
gameId: demo-campus-quest
title: Campus Quest — Main Campus Trail
description:
Explore the campus, follow the clues, and discover each hidden checkpoint.
creatorId: creator-demo
creatorName: Campus Quest Team
status: PUBLISHED
checkpointCount: 6
```

The six checkpoints are demonstration data only.

---

# 5. DEMO GAME CHECKPOINTS

The sample identifiers are:

```text
R001
R002
R003
R004
R005
R006
```

They should be stored under:

```text
games/demo-campus-quest/checkpoints/{checkpointId}
```

They must not be hard-coded into production game logic.

---

# 6. CHECKPOINT DATA SHAPE

Each seed checkpoint should contain:

```kotlin
Checkpoint(
    id = "...",
    gameId = "demo-campus-quest",
    name = "...",
    lat = ...,
    lng = ...,
    radiusM = 20f,
    lightSignature = LightSignature(...),
    clue = "...",
    lore = "...",
    order = ...,
    motionType = "SWEEP",
    rarity = "COMMON"
)
```

---

# 7. SAMPLE CHECKPOINT CATALOG

The exact coordinates should be replaced with the verified coordinates selected by the team for the actual demonstration location.

| ID | Order | Name | Radius | Rarity | Motion |
|---|---:|---|---:|---|---|
| R001 | 1 | The Starting Relic | 20 m | COMMON | SWEEP |
| R002 | 2 | The Hidden Marker | 20 m | COMMON | SWEEP |
| R003 | 3 | The Silent Archive | 20 m | RARE | SWEEP |
| R004 | 4 | The Old Passage | 20 m | COMMON | SWEEP |
| R005 | 5 | The Final Signal | 20 m | EPIC | SWEEP |
| R006 | 6 | The Campus Crown | 20 m | LEGENDARY | SWEEP |

These names are seed content and may be replaced by creator-generated content.

---

# 8. COORDINATE RULE

Do not copy unverified coordinates into the final seed.

Before the physical demo:

```text
1. Visit checkpoint location.
2. Obtain current device GPS coordinates.
3. Confirm location accuracy.
4. Verify geofence radius.
5. Test entry/exit.
6. Store verified values.
```

The application must accept arbitrary valid coordinates created by M2 rather than assuming the six seed locations.

---

# 9. LIGHT SIGNATURE SEED DATA

Light signature represents expected ambient environmental light.

It is not:

```text
light emitted by the relic
```

Each checkpoint should have a configurable expected range.

Example:

```kotlin
LightSignature(
    minLux = 100f,
    maxLux = 500f
)
```

The numerical values below are calibration examples only.

---

# 10. SAMPLE LIGHT SIGNATURES

| ID | Min Lux | Max Lux | Purpose |
|---|---:|---:|---|
| R001 | 100 | 500 | Example calibration |
| R002 | 150 | 600 | Example calibration |
| R003 | 80 | 350 | Example calibration |
| R004 | 200 | 800 | Example calibration |
| R005 | 100 | 450 | Example calibration |
| R006 | 250 | 1000 | Example calibration |

These values must be measured/calibrated at the actual checkpoint environment before the final physical demo.

---

# 11. LIGHT CALIBRATION PROCEDURE

For each checkpoint:

```text
1. Reach the intended physical location.
2. Hold the device in the intended scan orientation.
3. Record multiple ambient-light samples.
4. Identify normal variation.
5. Select an acceptable min/max range.
6. Store the calibrated range.
7. Test indoors/outdoors or changing conditions where relevant.
```

Do not assume one universal lux range works everywhere.

---

# 12. CLUE SEED DATA

Example:

```text
R001:
"Begin where the journey starts. Look carefully around you."

R002:
"Follow the path and search for the place where people gather."

R003:
"Knowledge leaves traces. Search near the quietest collection of stories."

R004:
"An old passage can hide a new discovery."

R005:
"You are close to the final signal. Trust the clues."

R006:
"The final discovery awaits those who complete the trail."
```

These are demo strings and can be replaced by creator-generated clues.

---

# 13. LORE SEED DATA

Example:

```text
R001:
"The first marker represents the beginning of the expedition."

R002:
"Every explorer learns that the obvious path is not always the correct one."

R003:
"Old knowledge can reveal new paths."

R004:
"Places crossed every day can hide details that are easy to overlook."

R005:
"The final approach requires patience and careful observation."

R006:
"You completed the campus trail."
```

---

# 14. RARITY

Supported example values:

```text
COMMON
RARE
EPIC
LEGENDARY
```

Rarity is primarily presentation/game-content metadata.

It must not automatically alter sensor fusion unless the shared technical specification explicitly defines such behavior.

---

# 15. MOTION TYPE

Initial supported value:

```text
SWEEP
```

The seed dataset should use:

```text
motionType = "SWEEP"
```

The actual motion implementation belongs to M3.

M4 only presents the resulting scan state.

---

# 16. DEMO CREATOR

Controlled seed creator:

```text
userId: creator-demo
displayName: Campus Quest Team
role: CREATOR
```

This account is for development/demo purposes.

Do not hard-code creator identity into the production UI.

---

# 17. DEMO PLAYERS

Example users:

```text
player-demo-01
player-demo-02
player-demo-03
player-demo-04
```

Display names:

```text
Explorer One
Explorer Two
Explorer Three
Explorer Four
```

These users exist to test:

```text
joining
progress
leaderboards
multi-user isolation
```

---

# 18. GAME MEMBERSHIP SEED

Example:

```text
gamePlayers/demo-campus-quest_player-demo-01
gamePlayers/demo-campus-quest_player-demo-02
gamePlayers/demo-campus-quest_player-demo-03
```

Each membership must include the correct:

```text
gameId
userId
joinedAt
```

No membership should accidentally grant access to another game.

---

# 19. PROGRESS SEED

Progress should be scoped by:

```text
userId
gameId
checkpointId
```

Example:

```text
progress/
  player-demo-01/
    games/
      demo-campus-quest/
        checkpoints/
          R001
          R002
```

---

# 20. PARTIAL PROGRESS SCENARIO

Player 1:

```text
R001 = FOUND
R002 = FOUND
R003 = NOT FOUND
R004 = NOT FOUND
R005 = NOT FOUND
R006 = NOT FOUND
```

Purpose:

```text
map progress
checkpoint state
resume behavior
leaderboard
```

---

# 21. COMPLETE PROGRESS SCENARIO

Player 2:

```text
R001 = FOUND
R002 = FOUND
R003 = FOUND
R004 = FOUND
R005 = FOUND
R006 = FOUND
```

Purpose:

```text
completion UI
leaderboard
final reveal
```

---

# 22. LEADERBOARD SEED

The leaderboard is **game-specific**.

Path:

```text
leaderboards/demo-campus-quest/entries/{uid}
```

Example:

| Player | Found | Completion |
|---|---:|---:|
| Explorer Two | 6/6 | Complete |
| Explorer One | 2/6 | In progress |
| Explorer Three | 1/6 | In progress |

Ranking should use the agreed scoring/time rules rather than hard-coded rank numbers.

---

# 23. NO GLOBAL LEADERBOARD

Do not create:

```text
leaderboards/global
```

as the production leaderboard model.

Each game has its own leaderboard:

```text
leaderboards/{gameId}/entries/{uid}
```

---

# 24. SECOND DEMO GAME

Use a second game to prove that the architecture is genuinely dynamic.

```text
gameId: demo-science-trail
title: Science Trail
description: A sample science-themed campus challenge.
creatorId: creator-demo
creatorName: Campus Quest Team
status: PUBLISHED
```

It may contain a different number of checkpoints.

Example:

```text
S001
S002
S003
S004
```

The purpose is to prove that the application does not depend on exactly six checkpoints.

---

# 25. DYNAMIC GAME TEST

The following must work:

```text
Create game with 2 checkpoints
Publish
Player sees game
Join
Map shows 2 checkpoints
Complete checkpoint 1
Complete checkpoint 2
Leaderboard shows game-specific progress
```

Repeat with:

```text
4 checkpoints
6 checkpoints
8 checkpoints
```

The production code must not require a fixed count.

---

# 26. EMPTY GAME

Test:

```text
Game created
0 checkpoints
```

Expected:

```text
Draft allowed
Publish blocked
```

unless the final product rules explicitly permit empty published games.

---

# 27. ONE-CHECKPOINT GAME

Test:

```text
Game created
1 checkpoint
Publish
Player joins
Player completes checkpoint
Game completion
```

This proves there is no hidden assumption that a game contains six checkpoints.

---

# 28. MANY-CHECKPOINT GAME

Test a game containing more than six checkpoints.

Example:

```text
C001
C002
C003
C004
C005
C006
C007
C008
```

Verify:

```text
map
ordering
geofencing
scan
progress
leaderboard
```

---

# 29. CHECKPOINT ORDER

Seed order:

```text
R001 → 1
R002 → 2
R003 → 3
R004 → 4
R005 → 5
R006 → 6
```

The application should obtain order from checkpoint data.

Do not implement:

```kotlin
if (checkpointId == "R001") ...
```

to determine progression.

---

# 30. CHECKPOINT DELETION TEST

Create:

```text
C001
C002
C003
```

Delete `C002`.

Expected:

```text
C001
C003
```

remain valid.

The UI and repository must not assume contiguous numeric IDs.

---

# 31. CHECKPOINT REORDER TEST

Start:

```text
C001 order 1
C002 order 2
C003 order 3
```

Reorder:

```text
C003 order 1
C001 order 2
C002 order 3
```

Player-facing progression must use the updated order.

---

# 32. DUPLICATE DISCOVERY TEST

Attempt:

```text
discover R001
discover R001 again
```

Expected:

```text
one progress record
one logical completion
no duplicate leaderboard completion
```

---

# 33. CROSS-GAME ISOLATION TEST

Player completes:

```text
demo-campus-quest / R001
```

Expected:

```text
demo-science-trail
```

remains unaffected.

---

# 34. CROSS-USER ISOLATION TEST

Player 1 completes:

```text
demo-campus-quest / R001
```

Player 2 should still see:

```text
R001 = not found
```

until Player 2 completes it.

---

# 35. LEADERBOARD ISOLATION TEST

If Player 1 scores in:

```text
demo-campus-quest
```

that score must not appear in:

```text
demo-science-trail
```

---

# 36. OFFLINE SEED SCENARIO

Prepare local Room state:

```text
Game cached
Checkpoints cached
Player membership cached
```

Then simulate:

```text
network unavailable
```

Player should still receive the locally supported gameplay experience.

---

# 37. OFFLINE DISCOVERY

Example:

```text
Player reaches R003
Network unavailable
Scan succeeds
```

Expected:

```text
Room records discovery
pendingSync = true
```

When network returns:

```text
pendingSync → Firebase
pendingSync = false
```

---

# 38. SYNC RETRY TEST

Simulate:

```text
first upload fails
second upload fails
third upload succeeds
```

Expected:

```text
no duplicate discovery
no lost progress
controlled retry
```

---

# 39. IDEMPOTENCY TEST

Send the same discovery request multiple times:

```text
gameId
userId
checkpointId
```

Expected final state:

```text
one logical discovery
```

---

# 40. FCM NEW-GAME SEED

Example payload:

```json
{
  "gameId": "demo-science-trail",
  "gameTitle": "Science Trail",
  "creatorName": "Campus Quest Team",
  "checkpointCount": 4,
  "publishedAt": 1760000000000
}
```

Topic:

```text
/topics/new_games
```

---

# 41. FCM TEST

Publish:

```text
demo-science-trail
```

Expected:

```text
FCM notification received
        ↓
tap
        ↓
Game Details
        ↓
correct gameId
```

The notification must not open a hard-coded game.

---

# 42. NOTIFICATION DUPLICATION TEST

Send the same notification twice.

Expected:

```text
no corruption
no duplicate game
```

The app may display both notifications depending on notification policy, but navigation must remain correctly scoped.

---

# 43. UI PREVIEW DATA

M1 and M2 may use lightweight preview data.

Example:

```kotlin
Game(
    id = "preview-game",
    title = "Campus Mystery",
    description = "Preview game",
    creatorId = "preview",
    creatorName = "Preview Creator",
    status = GameStatus.PUBLISHED,
    checkpointCount = 4
)
```

Preview data should remain isolated from production repositories.

---

# 44. SENSOR MOCK DATA

M3 tests should support deterministic sensor inputs.

Example:

```text
GPS score = 0.90
Light score = 0.85
Motion score = 0.95
```

Expected weighted fusion:

```text
weighted result > threshold
```

Exact weights and threshold must come from the shared sensor-fusion technical specification, not from this mock-data document.

---

# 45. SENSOR FAILURE FIXTURES

Test:

```text
GPS unavailable
Light unavailable
Accelerometer unavailable
Proximity unavailable
```

Also test combinations.

Expected behavior must follow the approved degradation rules.

---

# 46. FALSE-POSITIVE FIXTURES

Create cases where:

```text
GPS = strong
Light = strong
Motion = weak
```

and:

```text
GPS = strong
Light = weak
Motion = strong
```

The fusion system should behave according to its configured weights and threshold.

Proximity must remain a separate final gate.

---

# 47. PROXIMITY FIXTURES

Test:

```text
fusion >= threshold
proximity = false
```

Expected:

```text
no reveal
```

Then:

```text
fusion >= threshold
proximity = true
```

Expected:

```text
reveal allowed
```

This preserves the distinction between:

```text
fusion score
```

and:

```text
physical final gate
```

---

# 48. LIGHT MATCH FIXTURES

Example:

```text
expected = 100–500 lux
observed = 300 lux
```

Expected:

```text
match
```

Example:

```text
expected = 100–500 lux
observed = 900 lux
```

Expected:

```text
outside expected range
```

---

# 49. MOTION FIXTURES

Use deterministic accelerometer samples for:

```text
valid sweep
invalid movement
insufficient movement
excessive movement
```

The test must not require physically shaking a device for every automated test.

---

# 50. LOCATION FIXTURES

Test distances such as:

```text
100 m
50 m
20 m
10 m
1 m
```

against the checkpoint radius.

Do not assume that entering the map marker means entering the geofence.

---

# 51. GEOFENCE FIXTURES

For checkpoint radius:

```text
20 m
```

test:

```text
outside → inside
inside → outside
outside → outside
inside → inside
```

Verify duplicate ENTER events do not trigger duplicate scan/reveal operations.

---

# 52. ROOM SEED DATABASE

Local development may include:

```text
games
checkpoints
gamePlayers
foundCheckpoints
pendingSync
```

The Room schema must preserve:

```text
gameId
userId
checkpointId
```

where required for isolation.

---

# 53. FIRESTORE SEED DATABASE

Controlled demo data should follow:

```text
users
games
games/{gameId}/checkpoints
gamePlayers
progress
leaderboards
```

No fixed six-relic top-level collection should be required.

---

# 54. DATA RESET PROCEDURE

Before a fresh demo:

```text
1. Reset test user progress.
2. Verify game membership.
3. Verify game status.
4. Verify checkpoint order.
5. Verify coordinates.
6. Verify light signatures.
7. Verify leaderboard.
8. Verify FCM publication state.
```

Do not manually modify production data without recording the change.

---

# 55. DEMO RESET OPTIONS

Preferred:

```text
controlled reset script/tool
```

Alternative:

```text
Firebase console/manual reset
```

Manual reset should be documented and verified before the demonstration.

---

# 56. DEMO SCENARIO A — CREATOR

```text
Login as creator
↓
Create game
↓
Enter title/description
↓
Add checkpoints
↓
Configure coordinates/radius
↓
Configure light signature
↓
Add clues/lore
↓
Save draft
↓
Publish
```

---

# 57. DEMO SCENARIO B — PLAYER

```text
Login as player
↓
Receive new-game notification
↓
Open game details
↓
Join
↓
View map
↓
Approach checkpoint
↓
Enter geofence
↓
Scan
↓
Fusion increases
↓
Proximity gate passes
↓
Reveal
↓
Discovery saved
```

---

# 58. DEMO SCENARIO C — LEADERBOARD

```text
Player completes checkpoint
↓
Progress stored
↓
Leaderboard updates
↓
Player sees ranking
```

Ranking must belong to the current game.

---

# 59. DEMO SCENARIO D — OFFLINE

```text
Game cached
↓
Network disabled
↓
Reach checkpoint
↓
Scan succeeds
↓
Room saves discovery
↓
Network restored
↓
Sync
↓
Firestore updated
```

---

# 60. DATA VALIDATION

Every seed record should be checked for:

```text
non-empty ID
valid gameId
valid coordinates
positive radius
valid order
valid light range
non-empty clue
non-empty lore
valid rarity
valid motion type
```

---

# 61. INVALID CHECKPOINT FIXTURES

Test:

```text
latitude > 90
latitude < -90
longitude > 180
longitude < -180
negative radius
minLux > maxLux
empty clue
duplicate ID
invalid gameId
```

Expected:

```text
validation failure
```

---

# 62. INVALID GAME FIXTURES

Test:

```text
empty title
empty description
missing creator
invalid status
zero checkpoints at publish
```

Expected behavior should follow the creator/publishing validation contract.

---

# 63. SEED VERSIONING

If seed data changes, update:

```text
seed version
```

Example:

```text
SEED_VERSION = 2
```

This makes it clear which controlled dataset is being used during a demo.

---

# 64. PRODUCTION SAFETY

Before release, verify:

```text
[ ] Seed IDs are not used for logic
[ ] Preview repositories are not injected
[ ] Fake sensors are disabled
[ ] Fake locations are disabled
[ ] Demo users are not required
[ ] Demo leaderboard entries are not required
[ ] Test FCM payloads are not hard-coded
```

---

# 65. MOCK DATA OWNERSHIP

| Data | Owner |
|---|---|
| Player preview games | M1 |
| Creator form preview | M2 |
| Sensor fixtures | M3 |
| Scan-state fixtures | M4 |
| Firebase seed/cloud data | M5 |
| Room test/seed data | M6 |

Shared seed decisions should be coordinated rather than independently duplicated.

---

# 66. TEST FIXTURE OWNERSHIP

```text
M1 → UI/navigation fixtures
M2 → creator/game validation fixtures
M3 → location/sensor/fusion fixtures
M4 → scan/reveal fixtures
M5 → Firebase/security/FCM fixtures
M6 → Room/sync/repository fixtures
```

---

# 67. SEED DATA CHANGE PROCESS

```text
Propose change
↓
Check affected features
↓
Update shared data specification
↓
Update seed implementation
↓
Run affected tests
↓
Run dynamic-game tests
↓
Merge
```

---

# 68. FINAL SEED PACKAGE

The controlled demo package should contain:

```text
1 primary demo game
1 secondary dynamic game
demo creator
multiple demo players
checkpoint seed data
membership seed data
progress scenarios
leaderboard scenarios
FCM test payload
sensor fixtures
offline fixtures
```

---

# 69. FINAL GOLDEN DATA SET

Primary:

```text
demo-campus-quest
R001–R006
```

Secondary:

```text
demo-science-trail
S001–S004
```

These are examples, not production constraints.

---

# 70. ACCEPTANCE CHECKLIST

```text
[ ] Primary game loads
[ ] Secondary game loads
[ ] Different checkpoint counts work
[ ] Creator can create a new game
[ ] Creator can create checkpoints
[ ] Creator can reorder checkpoints
[ ] Creator can publish
[ ] Player can discover published games
[ ] Player can join
[ ] Map loads dynamic checkpoints
[ ] Geofences are dynamic
[ ] Fusion uses GPS + Light + Motion
[ ] Proximity remains a separate final gate
[ ] Discovery is game/user scoped
[ ] Leaderboard is game scoped
[ ] Room stores offline progress
[ ] Sync is idempotent
[ ] FCM deep link uses gameId
[ ] No production code depends on R001–R006
```

---

# 71. FINAL PRINCIPLE

The seed dataset exists to make development and demonstration repeatable.

It must never dictate the architecture.

The real product model is:

```text
Creator
   ↓
Game
   ↓
0..N Checkpoints
   ↓
Player joins
   ↓
Dynamic gameplay
   ↓
Game-scoped progress
   ↓
Game-scoped leaderboard
```

Therefore:

```text
R001–R006 = sample data
```

not:

```text
R001–R006 = application architecture
```

---

**END OF MOCK DATA CATALOG & SEED DATA SPECIFICATION**
