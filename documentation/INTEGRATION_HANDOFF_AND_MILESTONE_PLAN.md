# INTEGRATION HANDOFF & MILESTONE PLAN
## Campus Quest — Final Six-Member Development Plan

**Status:** Final reassignment version  
**Project:** Campus Quest  
**Platform:** Native Android / Kotlin / Android Studio  
**Architecture:** MVVM + Repository + Room + Firebase/Firestore  
**Team:** 6 members  
**Primary objective:** Integrate six independently developed feature areas into one demonstrable Android application without breaking shared contracts.

---

# 1. PURPOSE

This document defines how M1–M6 move from individual implementation to one integrated Campus Quest application.

The plan is designed around the final ownership model:

1. M1 — Player UI & Navigation
2. M2 — Creator & Game Management
3. M3 — Location, Sensors & Fusion
4. M4 — Quest & Scan Gameplay
5. M5 — Firebase & Backend
6. M6 — Room, Repository, Sync & Integration

The project uses MVVM and separation of concerns. The course architecture material describes the UI as responsible for presentation/input, the ViewModel as the coordinator of UI-related state and logic, and the repository/data layer as hiding the underlying storage/network mechanism. fileciteturn32file1L581-L586

---

# 2. INTEGRATION PRINCIPLE

Integration must happen incrementally.

Do not wait until the final day to merge all six members' work.

The required progression is:

```text
Shared contracts
      ↓
Application shell
      ↓
Local/domain foundation
      ↓
Cloud foundation
      ↓
Creator flow
      ↓
Player discovery flow
      ↓
Location/sensor flow
      ↓
Scan/reveal flow
      ↓
Offline/sync
      ↓
End-to-end testing
      ↓
Demo build
```

---

# 3. FINAL OWNERSHIP

| Member | Area | Integration responsibility |
|---|---|---|
| M1 | Player UI & Navigation | App shell and player-flow integration |
| M2 | Creator & Game Management | Dynamic game/checkpoint creation flow |
| M3 | Location, Sensors & Fusion | Physical discovery signal pipeline |
| M4 | Quest & Scan Gameplay | Scan/reveal gameplay integration |
| M5 | Firebase & Backend | Cloud/auth/security/FCM integration |
| M6 | Room + Repository + Sync | Local/cloud boundary and final integration |

M6 is the final integration coordinator, but **integration is a team responsibility**.

---

# 4. DEFINITION OF INTEGRATED

A feature is not considered integrated merely because its branch compiles.

It is integrated when:

```text
UI
 ↓
ViewModel
 ↓
Shared interface
 ↓
Correct implementation
 ↓
Data persisted correctly
 ↓
Next feature receives correct state
 ↓
Relevant test passes
```

---

# 5. SHARED CONTRACT FREEZE

Before major feature development:

```text
Game model
Checkpoint model
GameStatus
LightSignature
Repository methods
Firestore paths
Room keys
Navigation IDs
FCM payload
Fusion boundary
Progress scope
Leaderboard scope
```

must be agreed.

Any later change requires team communication.

---

# 6. MILESTONE OVERVIEW

| Milestone | Main objective | Primary owners |
|---|---|---|
| M0 | Shared foundation | All |
| M1 | App shell + local/cloud foundation | M1/M5/M6 |
| M2 | Dynamic creator flow | M2/M5/M6 |
| M3 | Player discovery + joining | M1/M5/M6 |
| M4 | Location + sensor pipeline | M3 |
| M5 | Scan/reveal gameplay | M4/M3 |
| M6 | Offline/sync | M6/M5 |
| M7 | Full end-to-end integration | All, led by M6 |
| M8 | Final stabilization/demo | All |

---

# 7. M0 — SHARED FOUNDATION

## Objective

Ensure every member is building against the same project structure.

## Required outputs

```text
Android project builds
Shared package structure exists
Navigation convention agreed
Domain models agreed
Repository interface agreed
Git branches created
Firebase project identified
Room database skeleton identified
```

## M1

- App entry point.
- Navigation shell.
- Shared theme/components.

## M2

- Creator screen skeletons.
- Game/checkpoint form models.

## M3

- Location/sensor abstraction skeleton.
- No dependency on final UI.

## M4

- Scan screen skeleton.
- Scan state model.

## M5

- Firebase initialization.
- Auth foundation.
- Firestore structure.

## M6

- Room database foundation.
- Repository interface/implementation structure.
- Sync model foundation.

---

# 8. M0 EXIT CRITERIA

```text
[ ] Project builds
[ ] App launches
[ ] All six branches exist
[ ] Shared models compile
[ ] Repository interface compiles
[ ] Navigation arguments are agreed
[ ] Firebase configuration is available
[ ] Room configuration is available
[ ] No obsolete Relic-only architecture remains as the primary model
```

---

# 9. M1 — APPLICATION SHELL

## Objective

Create the stable application shell into which all feature screens can be inserted.

## M1 responsibilities

```text
Login
Player navigation
Games list
Game details
Join flow
Leaderboard presentation
Notification deep links
Shared UI
```

## M5 dependency

Authentication.

## M6 dependency

Repository methods.

## M2 dependency

Creator destinations/navigation.

## M4 dependency

Scan destination.

---

# 10. M1 EXIT CRITERIA

```text
[ ] App starts
[ ] Authentication state determines entry
[ ] Player can reach games
[ ] Game details route accepts gameId
[ ] Join route accepts gameId
[ ] Checkpoint/scan route accepts gameId + checkpointId
[ ] Leaderboard route is game-scoped
[ ] FCM deep link can target gameId
```

---

# 11. M2 — CREATOR FLOW

## Objective

Make game creation genuinely dynamic.

Creator must be able to:

```text
Create game
 ↓
Enter title/description
 ↓
Add checkpoints
 ↓
Configure coordinates
 ↓
Configure radius
 ↓
Configure light range
 ↓
Configure clue/lore
 ↓
Configure motion/rarity
 ↓
Reorder
 ↓
Save draft
 ↓
Validate
 ↓
Publish
```

---

# 12. M2 INTEGRATION DEPENDENCIES

M2 depends on:

```text
GameRepository
Game model
Checkpoint model
Auth user
```

M5 provides cloud persistence/security.

M6 provides local draft persistence where supported.

M3 provides valid location/sensor configuration constraints.

---

# 13. M2 EXIT CRITERIA

```text
[ ] Game can contain arbitrary checkpoint count
[ ] Checkpoints are linked to gameId
[ ] Drafts save correctly
[ ] Validation works
[ ] Existing checkpoint can be edited
[ ] Delete is confirmed
[ ] Reordering updates order
[ ] Publish uses repository
[ ] No direct Firestore from UI
[ ] No hard-coded R001–R006 dependency
```

---

# 14. M3 — PHYSICAL SIGNAL PIPELINE

## Objective

Make dynamic checkpoint discovery technically possible.

Pipeline:

```text
Checkpoint configuration
        ↓
Location
        ↓
Geofence / proximity
        ↓
Ambient light
        ↓
Accelerometer
        ↓
Normalization
        ↓
Weighted fusion
        ↓
Fusion threshold
        ↓
Proximity final gate
```

---

# 15. M3 INTEGRATION DEPENDENCIES

M3 consumes:

```text
gameId
checkpointId
lat
lng
radiusM
lightSignature
motionType
```

M3 provides:

```text
distance
GPS score
light score
motion score
fusion score
fusion threshold
proximity state
final gate state
availability/error state
```

---

# 16. M3 EXIT CRITERIA

```text
[ ] Permissions handled
[ ] Dynamic checkpoints supported
[ ] No fixed six-checkpoint logic
[ ] Geofences are game-scoped
[ ] Light signature comes from checkpoint config
[ ] Accelerometer motion is evaluated
[ ] Fusion is GPS + Light + Motion
[ ] Proximity is separate from weighted fusion
[ ] Lifecycle cleanup works
[ ] Sensor unavailability is represented
```

---

# 17. M4 — QUEST/SCAN GAMEPLAY

## Objective

Turn M3's physical signals into a complete player interaction.

Flow:

```text
Approach checkpoint
 ↓
Geofence / scan availability
 ↓
Scan HUD
 ↓
GPS + Light + Motion indicators
 ↓
Fusion meter
 ↓
Fusion threshold
 ↓
Waiting for proximity
 ↓
Final gate
 ↓
Success
 ↓
Reveal
 ↓
Record discovery
```

---

# 18. M4 RESPONSIBILITIES

M4 owns:

```text
Scan UI
Scan state machine
Signal presentation
Fusion meter
Proximity gate presentation
Success/failure states
Reveal
Discovery trigger
```

M4 does not own:

```text
SensorManager
FusedLocationProviderClient
Firebase
Room
Firestore
```

---

# 19. M4 EXIT CRITERIA

```text
[ ] Scan screen receives gameId/checkpointId
[ ] Signal states render correctly
[ ] Fusion score is displayed
[ ] Threshold state is displayed
[ ] Proximity final gate is distinct
[ ] Success cannot occur from fusion alone
[ ] Reveal appears only after final gate
[ ] Discovery is recorded through repository
[ ] Duplicate discovery is handled
[ ] Offline result follows shared contract
```

---

# 20. M5 — CLOUD FOUNDATION

## Objective

Provide authoritative cloud persistence and identity.

M5 owns:

```text
Firebase Auth
Firestore
Security Rules
FCM
Cloud validation
Cloud leaderboard
```

---

# 21. M5 REQUIRED PATHS

```text
users/{uid}

games/{gameId}

games/{gameId}/checkpoints/{checkpointId}

gamePlayers/{gameId}_{uid}

progress/{uid}/games/{gameId}/checkpoints/{checkpointId}

leaderboards/{gameId}/entries/{uid}
```

FCM:

```text
/topics/new_games
```

---

# 22. M5 SECURITY

Minimum ownership rule:

```text
request.auth.uid == game.creatorId
```

for creator-only operations.

Player progress must be scoped to:

```text
request.auth.uid
```

Leaderboard entries must remain game-scoped.

---

# 23. M5 EXIT CRITERIA

```text
[ ] Auth works
[ ] Games persist
[ ] Checkpoints persist
[ ] Creator ownership is enforced
[ ] Player membership works
[ ] Progress works
[ ] Game-specific leaderboard works
[ ] Publish changes status
[ ] New-game FCM flow works
[ ] Security rules tested
```

---

# 24. M6 — ROOM + REPOSITORY + SYNC

## Objective

Hide local/cloud implementation details behind the repository and make offline behavior reliable.

M6 owns:

```text
Room
DAOs
Entities
Repository orchestration
Cache
Pending sync
Retries
Transactions
Migrations
Integration
```

---

# 25. M6 ROOM KEYS

Required logical identities:

```text
Game:
    id

Checkpoint:
    gameId + id

GamePlayer:
    gameId + userId

FoundCheckpoint:
    gameId + userId + checkpointId

PendingSync:
    operationId
```

---

# 26. M6 EXIT CRITERIA

```text
[ ] Room compiles
[ ] Composite keys preserve game/user isolation
[ ] Repository delegates correctly
[ ] Cached games can be read
[ ] Cached checkpoints can be read
[ ] Discovery can be saved locally
[ ] Pending operations persist
[ ] Retry works
[ ] Sync is idempotent
[ ] Transactions protect required multi-write operations
[ ] Migrations are tested
```

---

# 27. M6 — FIRST INTEGRATION CHECK

The first complete integration target should be:

```text
Login
 ↓
Games
 ↓
Game Details
 ↓
Join
 ↓
Load Checkpoints
```

This validates:

```text
M1 + M5 + M6
```

before physical gameplay is added.

---

# 28. M6 — SECOND INTEGRATION CHECK

Next:

```text
Game Details
 ↓
Join
 ↓
Game Map
 ↓
Dynamic Checkpoint
```

This validates:

```text
M1 + M3 + M5 + M6
```

---

# 29. M6 — THIRD INTEGRATION CHECK

Then:

```text
Map
 ↓
Approach checkpoint
 ↓
Geofence
 ↓
Scan
```

This validates:

```text
M1 + M3 + M4 + M5 + M6
```

---

# 30. FINAL INTEGRATION CHECK

Complete:

```text
Creator
 ↓
Create game
 ↓
Add 2+ checkpoints
 ↓
Publish
 ↓
FCM
 ↓
Player opens notification
 ↓
Game details
 ↓
Join
 ↓
Map
 ↓
Checkpoint
 ↓
Sensor fusion
 ↓
Proximity gate
 ↓
Reveal
 ↓
Discovery
 ↓
Leaderboard
```

This is the primary end-to-end acceptance flow.

---

# 31. INTEGRATION ORDER

The safest merge order is:

```text
1. Shared contracts
2. M6 repository/Room foundation
3. M5 Firebase foundation
4. M1 navigation shell
5. M2 creator
6. M1 player discovery
7. M3 location/sensors
8. M4 scan
9. M6 offline/sync
10. FCM/deep-link integration
11. Full testing
```

This prevents UI members from building against undefined data behavior.

---

# 32. BRANCH STRUCTURE

Recommended branches:

```text
main
develop

feature/m1-player-ui-navigation
feature/m2-creator-game-management
feature/m3-location-sensors-fusion
feature/m4-quest-scan-gameplay
feature/m5-firebase-backend
feature/m6-room-sync-integration
```

---

# 33. BRANCH POLICY

Each member:

```text
feature branch
    ↓
local tests
    ↓
commit
    ↓
push
    ↓
pull request
    ↓
review
    ↓
develop
```

Do not merge untested feature branches directly into `main`.

---

# 34. INTEGRATION BRANCH

M6 should coordinate the integration branch.

Recommended:

```text
develop
```

is the shared integration branch.

`main` should contain only stable milestone builds.

---

# 35. PULL REQUEST REQUIREMENTS

Every PR should state:

```text
Purpose
Files changed
Shared contracts affected?
Tests run
Dependencies on other branches
Known limitations
```

---

# 36. DAILY INTEGRATION

At the end of each development session:

```text
1. Pull latest develop.
2. Resolve conflicts locally.
3. Build.
4. Run relevant tests.
5. Push changes.
```

This reduces late merge conflicts.

---

# 37. MERGE CONFLICT PRIORITY

When resolving conflicts:

```text
1. Preserve current shared contract.
2. Preserve working behavior.
3. Remove obsolete architecture.
4. Do not duplicate conflicting implementations.
5. Run tests after resolution.
```

---

# 38. DEMO DATA STRATEGY

Use:

```text
demo-campus-quest
```

as the main demonstration game.

Use:

```text
R001–R006
```

only as seed/demo checkpoint IDs.

Also maintain a second test game:

```text
demo-science-trail
```

to prove that data is game-scoped.

---

# 39. DYNAMIC DATA ACCEPTANCE TEST

Create:

```text
Game A = 2 checkpoints
Game B = 8 checkpoints
```

The application must work without modifying Kotlin source code.

This proves that the architecture is genuinely dynamic.

---

# 40. GAME ISOLATION TEST

Verify:

```text
Player joins Game A
Player discovers CP-A1
```

Then confirm:

```text
Game B
does not show CP-A1
does not inherit Game A progress
does not inherit Game A leaderboard points
```

---

# 41. LEADERBOARD ISOLATION TEST

Given:

```text
Game A:
User 1 = 5 points

Game B:
User 1 = 2 points
```

The displayed leaderboards must remain separate.

---

# 42. CREATOR ISOLATION TEST

Creator A:

```text
owns Game A
```

Creator B:

```text
owns Game B
```

Creator A must not be able to modify Game B.

---

# 43. NOTIFICATION ACCEPTANCE TEST

```text
Creator publishes Game A
       ↓
Game status becomes PUBLISHED
       ↓
New-game notification is generated
       ↓
Player receives notification
       ↓
Player taps notification
       ↓
Game Details(Game A)
```

The notification must carry `gameId`.

---

# 44. OFFLINE ACCEPTANCE TEST

Scenario:

```text
Player has previously cached Game A
Network becomes unavailable
Player enters gameplay
```

Verify the agreed cached configuration remains available.

If discovery is supported offline:

```text
Discovery
 ↓
Room
 ↓
Pending Sync
```

When network returns:

```text
Pending Sync
 ↓
Firebase
 ↓
Synced
```

---

# 45. DUPLICATE DISCOVERY TEST

Perform the same successful discovery multiple times.

Expected:

```text
one logical FoundCheckpoint
one logical progress entry
one logical leaderboard contribution
```

No duplicated score.

---

# 46. SENSOR DEGRADATION TEST

Test:

```text
Light unavailable
Accelerometer unavailable
Location unavailable
Proximity unavailable
```

The application must display an understandable state rather than crash.

---

# 47. LIFECYCLE TEST

During scan:

```text
rotate device
background app
return to app
```

Verify:

```text
gameId preserved
checkpointId preserved
scan state does not corrupt
sensor lifecycle is restored correctly
```

The course architecture material specifically emphasizes Android lifecycle handling and ViewModel state survival across configuration changes. fileciteturn32file1L505-L509 fileciteturn32file1L546-L561

---

# 48. UI INTEGRATION TEST

All implemented screens should remain consistent with the approved Figma flow.

The course UI/UX material describes prototyping as connecting interfaces to review navigation before implementation and then embedding the design in Android Studio. fileciteturn32file0L209-L235

Check:

```text
spacing
navigation
buttons
states
loading
errors
back behavior
```

---

# 49. SMALL-SCREEN CHECK

Test at least:

```text
small phone
normal phone
large phone
portrait
landscape
```

The course material specifically requires support for orientation and screen sizes and recommends respecting safe areas. fileciteturn32file0L243-L265

---

# 50. ACCESSIBILITY CHECK

Verify:

```text
touch targets
text scaling
contrast
content descriptions
keyboard/safe-area behavior
```

The course material gives a 48dp × 48dp touch-target minimum and references 12sp minimum body text and 4.5:1 contrast for the covered Android UI guidance. fileciteturn32file0L303-L311

---

# 51. PERFORMANCE CHECK

Before final submission:

```text
No unnecessary continuous sensor registration
No unnecessary location updates
No large blocking database operations on UI thread
No repeated Firestore reads caused by UI recreation
No duplicate listeners
No obvious memory leaks
```

---

# 52. FINAL DEMO BUILD

The final demonstration build must use:

```text
Release-like stable configuration
Realistic seed data
At least two games
Dynamic checkpoints
Firebase configured
Room configured
FCM configured if demonstrated
Location/sensors tested on physical device
```

---

# 53. FINAL DEMO SCRIPT

## Part 1 — Creator

```text
Login
 ↓
Creator area
 ↓
Create game
 ↓
Add checkpoints
 ↓
Configure clue/location/light/motion
 ↓
Save
 ↓
Publish
```

## Part 2 — Notification

```text
Published game
 ↓
New-game notification
 ↓
Tap
 ↓
Game Details
```

## Part 3 — Player

```text
Join
 ↓
Map
 ↓
Approach checkpoint
 ↓
Geofence
 ↓
Scan
```

## Part 4 — Sensor Fusion

```text
GPS
Light
Motion
 ↓
Fusion meter
 ↓
Threshold
 ↓
Proximity
```

## Part 5 — Discovery

```text
Success
 ↓
Reveal
 ↓
Discovery saved
 ↓
Leaderboard updated
```

---

# 54. FINAL ACCEPTANCE MATRIX

| Requirement | M1 | M2 | M3 | M4 | M5 | M6 |
|---|---:|---:|---:|---:|---:|---:|
| Login | ✓ | | | | ✓ | |
| Player games | ✓ | | | | ✓ | ✓ |
| Game details | ✓ | | | | ✓ | ✓ |
| Join | ✓ | | | | ✓ | ✓ |
| Creator games | | ✓ | | | ✓ | ✓ |
| Checkpoint creation | | ✓ | ✓ | | ✓ | ✓ |
| Dynamic map | | | ✓ | | | ✓ |
| Geofence | | | ✓ | | | |
| Light | | | ✓ | | | |
| Motion | | | ✓ | | | |
| Fusion | | | ✓ | | | |
| Scan | | | | ✓ | | |
| Reveal | | | | ✓ | | |
| Discovery | | | ✓ | ✓ | ✓ | ✓ |
| Leaderboard | ✓ | | | | ✓ | ✓ |
| FCM | ✓ | | | | ✓ | |
| Room | | | | | | ✓ |
| Sync | | | | | ✓ | ✓ |
| Integration | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

---

# 55. FINAL DEFINITION OF DONE

Campus Quest is ready for final submission when:

```text
[ ] Application builds cleanly
[ ] Authentication works
[ ] Player can browse games
[ ] Player can view game details
[ ] Player can join a game
[ ] Creator can create a game
[ ] Creator can create arbitrary checkpoints
[ ] Creator can save draft
[ ] Creator can publish
[ ] Published game appears to players
[ ] New-game notification works where configured
[ ] Dynamic checkpoints appear on map
[ ] Geofencing works
[ ] GPS score works
[ ] Light score works
[ ] Motion score works
[ ] Weighted fusion works
[ ] Proximity final gate works
[ ] Scan UI works
[ ] Reveal works
[ ] Discovery persists
[ ] Room cache works
[ ] Firebase sync works
[ ] Duplicate discovery is prevented
[ ] Leaderboard is game-specific
[ ] Game isolation is verified
[ ] User isolation is verified
[ ] Security rules are tested
[ ] Lifecycle behavior is tested
[ ] Sensor failures are handled
[ ] UI matches approved design
[ ] Small-screen behavior is acceptable
[ ] Final end-to-end flow passes
```

---

# 56. INTEGRATION OWNERSHIP RULE

M6 coordinates the final integration build, but M6 is not responsible for silently fixing another member's feature.

If an integration failure belongs to:

```text
M1 → M1 fixes
M2 → M2 fixes
M3 → M3 fixes
M4 → M4 fixes
M5 → M5 fixes
M6 → M6 fixes
```

M6 identifies and coordinates the issue.

---

# 57. BLOCKER ESCALATION

When blocked:

```text
Member identifies blocker
        ↓
Document exact interface/problem
        ↓
Contact affected owner
        ↓
Agree on fix
        ↓
Update shared contract if required
        ↓
Implement
        ↓
Retest
```

Do not silently introduce a workaround that creates a second architecture.

---

# 58. FINAL INTEGRATION PRIORITY

If time becomes limited, prioritize:

### Priority 1 — Must work

```text
Login
Games
Game details
Join
Dynamic checkpoint
Map
Location
Geofence
Sensor fusion
Scan
Reveal
Discovery
Leaderboard
```

### Priority 2 — Strongly recommended

```text
Creator drafts
FCM notification
Offline cache
Offline discovery
Sync/retry
```

### Priority 3 — Polish

```text
Advanced animations
Additional visual effects
Extended creator customization
Non-essential refinements
```

Do not sacrifice the core end-to-end flow for cosmetic features.

---

# 59. FINAL PROJECT FLOW

```text
                    ┌──────────────┐
                    │    M2        │
                    │   Creator    │
                    └──────┬───────┘
                           │
                     Create/Publish
                           │
                           ▼
                    ┌──────────────┐
                    │     M5       │
                    │   Firebase   │
                    └──────┬───────┘
                           │
                      FCM / Cloud
                           │
                           ▼
                    ┌──────────────┐
                    │     M1       │
                    │    Player    │
                    └──────┬───────┘
                           │
                         Join
                           │
                           ▼
                    ┌──────────────┐
                    │     M6       │
                    │ Room/Repo    │
                    └──────┬───────┘
                           │
                      Checkpoints
                           │
                           ▼
                    ┌──────────────┐
                    │     M3       │
                    │ Location +   │
                    │ Sensors +    │
                    │ Fusion       │
                    └──────┬───────┘
                           │
                     Signal State
                           │
                           ▼
                    ┌──────────────┐
                    │     M4       │
                    │ Quest/Scan   │
                    └──────┬───────┘
                           │
                       Discovery
                           │
                           ▼
                    ┌──────────────┐
                    │     M6       │
                    │ Local Sync   │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │     M5       │
                    │ Leaderboard  │
                    └──────────────┘
```

---

# 60. FINAL RULE

The project should be integrated as **one system**, not six separate mini-projects.

Every member owns an implementation area, but the final product must behave as one coherent application.

The shared contract is the boundary between those responsibilities.

**END OF INTEGRATION HANDOFF & MILESTONE PLAN**
