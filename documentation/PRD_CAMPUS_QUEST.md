# Campus Quest — Product Requirements Document (PRD)

**Product:** Campus Quest  
**Document:** Product Requirements Document  
**Version:** 1.0  
**Status:** Draft for Team Review  
**Platform:** Android  
**Primary Roles:** Game Creator, Game Player  
**Project Context:** Mobile Application Development  
**Document Purpose:** Product-level source of truth for what Campus Quest must provide and how it should behave.

---

# 1. Executive Summary

Campus Quest is an Android mobile application for creating and participating in interactive, location-based treasure-hunt games.

The product has two primary user roles:

1. **Game Creator** — creates games, defines checkpoints and their challenges, saves drafts, and publishes games.
2. **Game Player** — discovers published games, views their details, joins games, explores the physical environment, discovers checkpoints, tracks progress, and competes on a game-specific leaderboard.

The central product mechanic combines mobile-device capabilities with physical exploration:

```text
Player approaches checkpoint
        ↓
Geofence ENTER
        ↓
GPS + Ambient Light + Motion
        ↓
Weighted Fusion
        ↓
Fusion Threshold
        ↓
Proximity Final Gate
        ↓
Checkpoint Reveal
        ↓
Progress Recorded
```

Campus Quest is intentionally designed around **dynamic games and dynamic checkpoints**. A game is not restricted to a predefined set of six checkpoints. The existing identifiers R001–R006 may be used for demonstration/seed data, but they are not the permanent production checkpoint set.

---

# 2. Product Vision

To provide a flexible mobile platform that allows people to create and participate in interactive location-based treasure hunts, turning physical exploration into a structured and measurable mobile-game experience.

The product should make it possible for a creator to design a complete hunt without requiring developers to hard-code new checkpoints into the application.

---

# 3. Product Problem

Traditional campus treasure hunts commonly require organizers to manually prepare clues, physical locations, participant lists, progress records, and results.

This creates several problems:

- creating a new hunt can require repeated manual preparation;
- changing checkpoints can require reorganizing physical materials;
- participant progress can be difficult to track;
- competition results can require manual calculation;
- different treasure hunts are difficult to manage as independent experiences;
- a simple GPS-only interaction may not provide enough engagement.

Campus Quest addresses these problems by turning the treasure hunt into a configurable mobile product where creators define games and players participate through the application.

---

# 4. Product Goals

## 4.1 Dynamic Game Creation

Allow a creator to create a game and configure its checkpoints without modifying application source code.

## 4.2 Interactive Exploration

Require players to physically explore the environment rather than simply reading a list of locations.

## 4.3 Sensor-Based Discovery

Use GPS, ambient light and accelerometer-based motion as interactive discovery signals.

## 4.4 Progress Tracking

Track checkpoint discoveries within the specific game being played.

## 4.5 Competitive Gameplay

Provide a leaderboard for each individual game.

## 4.6 Reusable Platform

Allow the same application to host multiple independent games.

## 4.7 Offline Resilience

Preserve important local gameplay state when temporary network connectivity is unavailable.

---

# 5. Product Success Criteria

The MVP should demonstrate that:

- a creator can create a game;
- the creator can add multiple checkpoints;
- the creator can save a draft;
- the creator can publish a valid game;
- a player can discover the published game;
- the player can join it;
- the player can view its checkpoints on a map;
- location can trigger the appropriate checkpoint interaction;
- GPS, light and motion can contribute to discovery;
- the fusion threshold and final proximity gate are enforced;
- a successful discovery is recorded;
- local state can survive temporary network loss;
- cloud synchronization works;
- the correct game's leaderboard is displayed;
- a new published game can generate a notification.

---

# 6. Target Users

## 6.1 Game Creator

A registered user who designs and publishes treasure-hunt games.

### Creator goals

- create a new game;
- define the game's content;
- add checkpoints;
- configure checkpoint challenges;
- save incomplete work;
- edit drafts;
- publish completed games;
- manage games they own.

## 6.2 Game Player

A registered user who participates in published games.

### Player goals

- discover available games;
- understand a game's content before joining;
- join a game;
- explore its map;
- discover checkpoints;
- understand their current progress;
- complete the game's checkpoints;
- compare results with other players in that game.

---

# 7. User Role Boundaries

The application must clearly distinguish creator and player responsibilities.

| Capability | Game Creator | Game Player |
|---|---:|---:|
| Create game | Yes | No |
| Save draft | Yes | No |
| Add checkpoint | Yes | No |
| Edit owned checkpoint | Yes | No |
| Publish owned game | Yes | No |
| Browse published games | Yes | Yes |
| View game details | Yes | Yes |
| Join game | Optional/Yes | Yes |
| Play game | Yes if participating | Yes |
| Discover checkpoint | Yes if participating | Yes |
| View game leaderboard | Yes | Yes |
| Modify another user's game | No | No |

Authorization must be enforced by the application and backend rules where applicable.

---

# 8. Core Product Concepts

## 8.1 Game

A game is an independently playable treasure-hunt experience.

A game contains:

- title;
- description;
- creator;
- status;
- checkpoints;
- publication information;
- participating players;
- progress;
- leaderboard.

A game can have a variable number of checkpoints.

## 8.2 Checkpoint

A checkpoint is a location-based challenge within a specific game.

A checkpoint can contain:

- name;
- location;
- discovery radius;
- clue;
- lore;
- order;
- rarity;
- expected ambient-light range;
- motion type.

A checkpoint always belongs to one game.

## 8.3 Game Status

The product uses:

```text
DRAFT
PUBLISHED
CLOSED
```

### DRAFT

The creator is still preparing the game.

It should not be presented as a normal playable published game.

### PUBLISHED

The game is available to players.

### CLOSED

The game is no longer an active published experience.

---

# 9. Product Scope

## 9.1 In Scope for MVP

```text
Authentication
Game creation
Game drafts
Dynamic checkpoint creation
Checkpoint editing
Game publishing
Published-game browsing
Game details
Game joining
Map-based gameplay
Location
Geofencing
GPS signal
Ambient-light signal
Accelerometer motion
Weighted sensor fusion
Proximity final gate
Checkpoint reveal
Game-specific progress
Game-specific leaderboard
Room local persistence
Firestore synchronization
FCM new-game notification
Offline-aware gameplay
```

## 9.2 Out of Scope for MVP

The following should not be required for the first working version:

- iOS application;
- advanced social networking;
- real-money rewards;
- complex team-based competition;
- sophisticated commercial anti-cheat;
- full computer-vision object recognition;
- large-scale game analytics;
- complex administrative web portal;
- marketplace for public games;
- advanced AR object rendering;
- payment functionality.

These can be considered future enhancements.

---

# 10. MVP Prioritization

## Must Have

- authentication;
- game creation;
- checkpoint creation;
- draft saving;
- publishing;
- game browsing;
- joining;
- map;
- geofencing;
- sensor-based scanning;
- checkpoint reveal;
- progress;
- Room persistence;
- Firestore synchronization;
- game-specific leaderboard.

## Should Have

- new-game notifications;
- polished offline synchronization;
- detailed scan feedback;
- creator game management.

## Could Have

- additional game discovery features;
- richer creator analytics;
- achievements;
- enhanced social features.

---

# 11. Creator User Journey

The primary creator journey is:

```text
Login
  ↓
Creator Area
  ↓
Create Game
  ↓
Enter Game Details
  ↓
Save Draft
  ↓
Add Checkpoint
  ↓
Configure Checkpoint
  ↓
Repeat for Additional Checkpoints
  ↓
Review
  ↓
Publish
  ↓
Game Becomes Available
```

A creator should be able to stop during the process and return to a draft later.

---

# 12. Player User Journey

The primary player journey is:

```text
Login
  ↓
Browse Games
  ↓
Select Game
  ↓
View Game Details
  ↓
Join Game
  ↓
Open Game Map
  ↓
Explore
  ↓
Approach Checkpoint
  ↓
Geofence ENTER
  ↓
Scan
  ↓
GPS + Light + Motion
  ↓
Fusion Threshold
  ↓
Proximity Final Gate
  ↓
Reveal
  ↓
Progress Updated
  ↓
Continue
  ↓
Game-Specific Leaderboard
```

---

# 13. Functional Requirements

## 13.1 Authentication Requirements

### FR-AUTH-001

The system shall allow registered users to authenticate.

### FR-AUTH-002

The system shall maintain the authenticated user's identity while the user is using protected application features.

### FR-AUTH-003

The system shall prevent unauthenticated users from performing protected creator or gameplay operations where authentication is required.

### FR-AUTH-004

The system shall allow the user to sign out.

---

# 14. Game Creation Requirements

### FR-GAME-001

The system shall allow an authorized user to create a new game.

### FR-GAME-002

A newly created game shall initially have `DRAFT` status.

### FR-GAME-003

A game shall have a unique identifier.

### FR-GAME-004

A game shall record its creator.

### FR-GAME-005

A creator shall be able to provide a game title.

### FR-GAME-006

A creator shall be able to provide a game description.

### FR-GAME-007

The system shall allow a creator to save an incomplete game as a draft.

### FR-GAME-008

A creator shall be able to reopen and continue editing an owned draft.

---

# 15. Checkpoint Requirements

### FR-CP-001

A creator shall be able to add multiple checkpoints to a game.

### FR-CP-002

Each checkpoint shall belong to exactly one game.

### FR-CP-003

A checkpoint shall contain a location.

### FR-CP-004

A checkpoint shall contain a configurable discovery radius.

### FR-CP-005

A checkpoint shall contain a clue.

### FR-CP-006

A checkpoint may contain lore or descriptive content.

### FR-CP-007

A checkpoint shall have an order within its game.

### FR-CP-008

A checkpoint shall have an expected ambient-light configuration.

### FR-CP-009

A checkpoint shall have a supported motion type.

### FR-CP-010

A checkpoint may contain a rarity classification.

### FR-CP-011

A creator shall be able to edit checkpoints belonging to their game.

### FR-CP-012

The system shall validate checkpoint information before publication.

---

# 16. Dynamic Checkpoint Requirement

### FR-DYN-001

The system shall support games with different numbers of checkpoints.

Examples:

```text
Game A → 3 checkpoints
Game B → 6 checkpoints
Game C → 10 checkpoints
```

### FR-DYN-002

The player interface shall load the checkpoints belonging to the selected game dynamically.

### FR-DYN-003

The system shall not require exactly six checkpoints.

### FR-DYN-004

The identifiers R001–R006 shall not be treated as a permanent production checkpoint list.

---

# 17. Game Publishing Requirements

### FR-PUB-001

Only an authorized creator shall be able to publish their game.

### FR-PUB-002

The system shall validate a game before publication.

### FR-PUB-003

A successfully published game shall change from `DRAFT` to `PUBLISHED`.

### FR-PUB-004

An unpublished draft shall not be presented to players as a normal playable published game.

### FR-PUB-005

A published game shall become available through the player game-discovery experience.

### FR-PUB-006

Publishing should make sufficient information available for the player to understand the game before joining.

---

# 18. Game Discovery Requirements

### FR-DISC-001

Players shall be able to browse published games.

### FR-DISC-002

The game list shall display relevant information such as title, description, creator and checkpoint count.

### FR-DISC-003

The player shall be able to open a game's details.

### FR-DISC-004

Unrelated or unpublished drafts shall not appear as playable published games.

---

# 19. Game Details Requirements

The Game Details experience should communicate:

- game title;
- description;
- creator;
- checkpoint count;
- publication status;
- relevant participation information;
- join action.

### FR-DETAIL-001

The selected game's identity shall remain available when the player moves from details to joining and gameplay.

---

# 20. Game Joining Requirements

### FR-JOIN-001

A player shall be able to join a published game.

### FR-JOIN-002

Joining shall be associated with the specific game.

### FR-JOIN-003

Joining one game shall not automatically join the player to unrelated games.

### FR-JOIN-004

Repeated join actions should be handled safely without creating duplicate memberships.

---

# 21. Map Requirements

### FR-MAP-001

A joined player shall be able to open the game's map.

### FR-MAP-002

The map shall display checkpoints belonging to the active game.

### FR-MAP-003

The map shall display the player's current location where location permission and services permit it.

### FR-MAP-004

The application shall be able to provide distance information for a relevant checkpoint.

### FR-MAP-005

The map shall not display checkpoints from another game.

---

# 22. Location Requirements

### FR-LOC-001

The application shall request required location permission.

### FR-LOC-002

The application shall handle denied location permission without crashing.

### FR-LOC-003

The application shall obtain the player's location during location-dependent gameplay.

### FR-LOC-004

The application shall calculate distance between the player and the active checkpoint.

### FR-LOC-005

The system shall consider location accuracy when interpreting location information.

---

# 23. Geofencing Requirements

### FR-GEO-001

The system shall create geofences dynamically from the active game's checkpoint configuration.

### FR-GEO-002

Geofence identity shall preserve both game and checkpoint context.

### FR-GEO-003

Entering a checkpoint geofence shall enable or route the player to the appropriate scan experience.

### FR-GEO-004

A geofence ENTER event shall not by itself mark a checkpoint as discovered.

### FR-GEO-005

Stale or cross-game geofence events shall be ignored safely.

### FR-GEO-006

Unnecessary geofences shall be removed when their gameplay context is no longer active.

---

# 24. Sensor-Based Discovery Requirements

The discovery mechanic uses:

```text
GPS
+
Ambient Light
+
Accelerometer Motion
```

These signals are combined into a weighted fusion score.

### FR-SENSOR-001

The system shall obtain a GPS-derived signal score.

### FR-SENSOR-002

The system shall obtain an ambient-light match score where the required sensor is available.

### FR-SENSOR-003

The system shall obtain a motion score from accelerometer interaction where the required sensor is available.

### FR-SENSOR-004

The three signal scores shall be normalized before weighted combination.

---

# 25. Weighted Fusion Requirements

### FR-FUSION-001

The system shall calculate a weighted fusion score from GPS, light and motion.

Conceptually:

```text
fusionScore =
    GPS × GPSWeight
  + Light × LightWeight
  + Motion × MotionWeight
```

### FR-FUSION-002

The weights shall be configurable.

### FR-FUSION-003

The weights shall sum to 1.

### FR-FUSION-004

The fusion score shall be normalized to a defined range.

### FR-FUSION-005

The system shall compare the fusion score against a configurable fusion threshold.

### FR-FUSION-006

A fusion score below the threshold shall not proceed to successful reveal.

---

# 26. Proximity Final Gate Requirements

Proximity is a separate final gate.

### FR-PROX-001

After the fusion threshold has been passed, the system shall verify final physical proximity.

### FR-PROX-002

The player shall not receive the final checkpoint reveal while outside the required final proximity.

### FR-PROX-003

Proximity shall not be treated as a fourth weighted sensor-fusion input.

The canonical product rule is:

```text
GPS + Light + Motion
        ↓
Weighted Fusion
        ↓
Fusion Threshold
        ↓
Proximity Final Gate
        ↓
Reveal
```

---

# 27. Checkpoint Reveal Requirements

### FR-REVEAL-001

The checkpoint shall be revealed only after the required discovery conditions have been satisfied.

### FR-REVEAL-002

The reveal may display checkpoint lore, rarity, completion information or other configured content.

### FR-REVEAL-003

The player shall receive clear feedback that the checkpoint has been successfully discovered.

### FR-REVEAL-004

A failed scan shall not falsely mark the checkpoint as discovered.

---

# 28. Progress Requirements

### FR-PROG-001

The system shall record successful checkpoint discoveries.

### FR-PROG-002

Progress shall be associated with the player and game.

### FR-PROG-003

The system shall allow the player to understand how many checkpoints have been discovered within the active game.

Example:

```text
3 / 8 checkpoints discovered
```

### FR-PROG-004

Progress from one game shall not be counted as progress in another game.

---

# 29. Leaderboard Requirements

### FR-LEAD-001

Each game shall have its own leaderboard.

### FR-LEAD-002

A player's result in one game shall not appear as a result for another game.

### FR-LEAD-003

The leaderboard shall be accessible within the context of its game.

### FR-LEAD-004

The leaderboard shall identify participating players and their relevant results according to the final scoring rules.

There shall be no mandatory global leaderboard combining unrelated games.

---

# 30. Notification Requirements

### FR-NOTIF-001

When a valid new game is published, the system should notify registered users through the agreed notification mechanism.

### FR-NOTIF-002

A new-game notification shall contain enough information to identify the correct game.

### FR-NOTIF-003

Selecting a notification shall open the corresponding Game Details experience.

### FR-NOTIF-004

The navigation must preserve the `gameId`.

---

# 31. Offline Requirements

### FR-OFF-001

Previously available local game information should remain accessible when appropriate during temporary network loss.

### FR-OFF-002

A successful eligible discovery should be stored locally when cloud connectivity is unavailable.

### FR-OFF-003

The system should synchronize pending local changes when connectivity returns.

### FR-OFF-004

Synchronization should not create duplicate discovery records.

### FR-OFF-005

Temporary network loss should not cause the application to crash.

---

# 32. Data Isolation Requirements

This is a critical product requirement.

### FR-ISO-001

Every checkpoint shall belong to a specific game.

### FR-ISO-002

Every discovery shall belong to a specific game and player.

### FR-ISO-003

Game A shall never use Game B's checkpoint configuration.

### FR-ISO-004

Game A progress shall never be displayed as Game B progress.

### FR-ISO-005

Game A leaderboard data shall never be mixed with Game B leaderboard data.

### FR-ISO-006

Game A geofence events shall not activate Game B checkpoint scanning.

---

# 33. Creator Validation Requirements

Before publication, the system should verify:

```text
Game title exists
Game description is valid
Game has required checkpoint configuration
Checkpoint coordinates are valid
Checkpoint radius is valid
Light range is valid
Motion type is supported
Checkpoint data is internally consistent
```

A game that does not meet the publication requirements shall remain unpublished.

---

# 34. Game and Checkpoint Identity

The product must preserve:

```text
gameId
checkpointId
```

throughout the relevant user journey.

The checkpoint should never be treated as globally independent of its game.

Conceptually:

```text
Game
 ├── Checkpoint A
 ├── Checkpoint B
 └── Checkpoint C
```

not:

```text
Application
 ├── Checkpoint A
 ├── Checkpoint B
 └── Checkpoint C
```

---

# 35. State Requirements

The application should clearly represent important states.

## Game States

```text
Loading
Available
Empty
Error
Joined
Not Joined
Draft
Published
Closed
```

## Scan States

```text
Idle
Starting
Collecting
Fusion Progress
Fusion Passed
Proximity Check
Revealed
Failed
Cancelled
```

## Connectivity States

```text
Online
Offline
Synchronizing
Sync Failed
```

The user should receive understandable feedback for these states.

---

# 36. Error and Edge Cases

The product shall handle:

### Location Permission Denied

Explain that location is required and provide an appropriate recovery action.

### Location Unavailable

Prevent false discovery and communicate the problem.

### Poor GPS Accuracy

Do not treat an unreliable location as precise confirmation.

### Light Sensor Unavailable

Clearly indicate the limitation and follow the agreed MVP sensor policy.

### Accelerometer Unavailable

Clearly indicate the limitation and follow the agreed MVP sensor policy.

### Network Lost

Use available local data and preserve eligible local progress.

### Invalid Game

Do not allow the player to start an invalid or unavailable game.

### Closed Game

Do not treat a closed game as a normal active published experience.

### Duplicate Discovery

Do not create multiple authoritative discoveries for the same:

```text
game + player + checkpoint
```

### Cross-Game Event

Ignore stale or incorrectly scoped events.

---

# 37. Non-Functional Requirements

## 37.1 Performance

### NFR-PERF-001

Normal navigation should remain responsive.

### NFR-PERF-002

Sensor processing should not unnecessarily block the main user interface.

### NFR-PERF-003

Database and network operations should be handled without freezing the UI.

## 37.2 Reliability

### NFR-REL-001

Temporary network failure should not unnecessarily destroy locally recorded gameplay state.

### NFR-REL-002

Repeated synchronization should be safe and idempotent.

### NFR-REL-003

Sensor and location failures should produce controlled application states rather than crashes.

## 37.3 Security

### NFR-SEC-001

Users shall only modify games they are authorized to manage.

### NFR-SEC-002

Players shall not be allowed to modify another user's game.

### NFR-SEC-003

Cloud access shall enforce appropriate authentication and authorization.

### NFR-SEC-004

Sensitive credentials and service-account secrets shall not be embedded in the client application.

## 37.4 Usability

### NFR-USE-001

The application shall provide understandable instructions during game creation.

### NFR-USE-002

The scan interface shall clearly communicate sensor and progress states.

### NFR-USE-003

Important state shall not depend only on color.

## 37.5 Compatibility

### NFR-COMP-001

The application shall support the Android version range defined by the final project configuration.

### NFR-COMP-002

The product shall gracefully handle differences in available device sensors.

## 37.6 Maintainability

### NFR-MAIN-001

The application should maintain clear separation between UI, ViewModel, repository, local persistence, remote persistence and platform services.

### NFR-MAIN-002

Dynamic game/checkpoint behavior should be data-driven rather than hard-coded.

---

# 38. UX Requirements

## 38.1 Game Discovery

The player should quickly understand:

```text
What is this game?
Who created it?
How many checkpoints does it have?
Can I join it?
```

## 38.2 Creator Experience

The creator should understand:

```text
What game am I editing?
Which checkpoints have I created?
Is the game a draft or published?
Is the game ready to publish?
```

## 38.3 Scan Experience

The player should understand:

```text
Am I close enough?
Is GPS responding?
Is the light condition matching?
Has my motion been detected?
How close am I to the fusion threshold?
Do I need to move closer?
Has the checkpoint been revealed?
```

A conceptual scan display may communicate:

```text
GPS        Good
Light      Matching
Motion     Detecting
Fusion     74%
Proximity  12 m
```

---

# 39. Accessibility Requirements

The MVP should:

- provide readable text;
- use sufficiently large touch targets;
- provide meaningful content descriptions where appropriate;
- avoid relying on color alone;
- provide understandable error messages;
- provide text-based status in addition to visual indicators.

---

# 40. Data Requirements

The product requires the following logical data:

## User

```text
userId
name/display information
authentication identity
```

## Game

```text
gameId
title
description
creatorId
creatorName
status
checkpointCount
createdAt
publishedAt
```

## Checkpoint

```text
checkpointId
gameId
name
latitude
longitude
radius
light signature
clue
lore
order
motion type
rarity
```

## Game Membership

```text
gameId
userId
joinedAt
```

## Discovery

```text
gameId
userId
checkpointId
foundAt
sync state where required
```

## Leaderboard

```text
gameId
userId
game-specific result
```

---

# 41. Product-Level Data Ownership

The product distinguishes between:

### Authoritative shared data

Examples:

```text
published games
checkpoint definitions
memberships
cloud progress
leaderboard results
```

### Local operational data

Examples:

```text
cached games
cached checkpoints
pending discoveries
temporary scan state
```

The implementation documents determine exactly how these are stored.

---

# 42. Privacy Requirements

The MVP should avoid collecting unnecessary personal or sensor data.

The product should not require continuous storage of:

- raw accelerometer streams;
- raw ambient-light streams;
- every GPS location sample.

The product needs meaningful gameplay results rather than continuous surveillance-style telemetry.

---

# 43. Anti-Cheat Boundary

The MVP is not intended to provide enterprise-grade anti-cheat.

The product should nevertheless reduce obvious inconsistencies by:

- requiring location-dependent interaction;
- using sensor-based discovery;
- enforcing game/checkpoint context;
- requiring the final proximity gate;
- protecting authoritative cloud records.

Advanced anti-cheat is outside MVP unless the project scope is later expanded.

---

# 44. Game Lifecycle

The intended lifecycle is:

```text
CREATE
  ↓
DRAFT
  ↓
VALIDATE
  ↓
PUBLISH
  ↓
ACTIVE PLAY
  ↓
CLOSE
```

The creator should not need to recreate a game merely because they want to add another game later.

Multiple games can coexist independently.

---

# 45. Multi-Game Product Model

Campus Quest is a platform for games, not a single fixed treasure hunt.

Example:

```text
Campus Quest
│
├── History Quest
│     ├── H001
│     ├── H002
│     └── H003
│
├── Science Trail
│     ├── S001
│     ├── S002
│     ├── S003
│     └── S004
│
└── Orientation Hunt
      ├── O001
      └── O002
```

Each game is independently configurable and playable.

---

# 46. Core Acceptance Scenarios

## AC-001 — Create Draft

**Given** an authenticated creator  
**When** they create a game  
**Then** the game is created in DRAFT status.

## AC-002 — Add Checkpoints

**Given** a draft game  
**When** the creator adds several checkpoints  
**Then** each checkpoint belongs to that game and can be configured independently.

## AC-003 — Publish Game

**Given** a valid draft  
**When** the creator selects Publish  
**Then** the game becomes PUBLISHED and becomes available to players.

## AC-004 — Browse Game

**Given** a published game  
**When** a player opens the game list  
**Then** the published game is available for viewing.

## AC-005 — Join Game

**Given** a player viewing a valid published game  
**When** the player selects Join  
**Then** membership is associated with that specific game.

## AC-006 — Dynamic Map

**Given** a game containing five checkpoints  
**When** the player opens the map  
**Then** the five checkpoints belonging to that game are displayed.

## AC-007 — Geofence

**Given** a player approaches a checkpoint  
**When** the player enters its configured area  
**Then** the appropriate scan experience becomes available.

Entering the geofence alone must not complete the checkpoint.

## AC-008 — Fusion

**Given** an active scan  
**When** GPS, light and motion produce a sufficient weighted score  
**Then** the fusion threshold is passed.

## AC-009 — Proximity

**Given** the fusion threshold has passed  
**When** the player is outside the final proximity requirement  
**Then** the checkpoint is not revealed.

## AC-010 — Reveal

**Given** the fusion threshold and proximity gate have both passed  
**When** the scan completes  
**Then** the checkpoint is revealed and discovery is recorded.

## AC-011 — Offline Discovery

**Given** cached game data and no network connection  
**When** a player successfully discovers a checkpoint  
**Then** the eligible discovery is stored locally for later synchronization.

## AC-012 — Game-Specific Leaderboard

**Given** two independent games  
**When** a player views Game A's leaderboard  
**Then** only Game A results are displayed.

## AC-013 — Notification

**Given** a valid new game is published  
**When** the notification is selected  
**Then** the correct game's details screen opens.

## AC-014 — Cross-Game Isolation

**Given** Game A and Game B have different checkpoints  
**When** a player plays Game A  
**Then** Game B's checkpoints, sensor signatures, progress and leaderboard are not used.

---

# 47. Example Product Scenario

## Creator

A creator creates:

```text
Game:
Campus Heritage Hunt

Checkpoints:
5
```

They configure each checkpoint with:

- location;
- clue;
- lore;
- ambient-light range;
- motion type;
- radius.

The game is saved as a draft and later published.

## Player

A player sees:

```text
Campus Heritage Hunt
Created by: Creator
5 checkpoints
```

The player joins and opens the map.

At a checkpoint:

```text
Geofence ENTER
      ↓
Scan
      ↓
GPS contribution
      +
Light contribution
      +
Motion contribution
      ↓
Fusion score
      ↓
Threshold passed
      ↓
Player moves sufficiently close
      ↓
Proximity passed
      ↓
Checkpoint revealed
```

The discovery is recorded for that player and that game.

---

# 48. Product Constraints

The project is constrained by:

- university project timeline;
- six-person development team;
- Android-focused delivery;
- limited physical testing time;
- differences between Android device sensors;
- need to demonstrate course concepts;
- need to avoid excessive product scope.

The MVP should therefore prioritize the core creator-to-player-to-discovery journey over advanced optional features.

---

# 49. Dependencies

At the product level, Campus Quest depends on:

```text
Android platform
Location services
Device sensors
Maps capability
Authentication service
Cloud data service
Push notification service
Local persistence
```

Detailed technology and implementation choices are defined in the corresponding technical documentation.

The course material supports native Android development as an appropriate option when an application requires deep hardware/sensor access, while platform selection should consider requirements, timeline, skills and maintenance. fileciteturn20file0L11-L21

The architecture documentation also emphasizes MVVM's separation of View and ViewModel responsibilities and its suitability for applications expected to grow. fileciteturn20file4L263-L271

---

# 50. Assumptions

The PRD assumes:

1. Users have supported Android devices.
2. Players grant required permissions.
3. Location services can be unavailable and therefore require error handling.
4. Sensor availability may differ between devices.
5. Internet connectivity can be temporarily unavailable.
6. Creators provide valid checkpoint information.
7. Published games are the primary player-discovery content.
8. R001–R006 may exist in seed/demo data but are not a production limitation.

---

# 51. Future Enhancements

Potential future features include:

- game sharing;
- invitations;
- teams;
- achievements;
- badges;
- game ratings;
- creator analytics;
- game templates;
- richer clue types;
- photo-based challenges;
- advanced AR;
- advanced anti-cheat;
- social features;
- game categories;
- public game marketplace;
- advanced player statistics.

These should not be allowed to expand the MVP beyond the available development period unless the team explicitly reprioritizes.

---

# 52. Requirement Traceability

Each product requirement should eventually map to design, implementation and testing.

Example:

```text
FR-SCAN / FR-FUSION
        ↓
Sensor Fusion Technical Specification
        ↓
SensorFusionEngine
        ↓
Unit Tests
        ↓
Physical Device Test
        ↓
Acceptance Test
```

Another example:

```text
FR-GAME-003
        ↓
Game model
        ↓
Room + Firestore
        ↓
Game Creation UI
        ↓
Creator Acceptance Test
```

Another:

```text
FR-NOTIF-003
        ↓
FCM notification contract
        ↓
Navigation deep link
        ↓
Notification test
        ↓
Game Details acceptance test
```

This allows the team to demonstrate that each major product requirement has a corresponding implementation and test.

---

# 53. Requirement ID Catalogue

| Area | Prefix |
|---|---|
| Authentication | FR-AUTH |
| Game Management | FR-GAME |
| Dynamic Checkpoints | FR-DYN |
| Checkpoints | FR-CP |
| Publishing | FR-PUB |
| Discovery | FR-DISC |
| Game Details | FR-DETAIL |
| Joining | FR-JOIN |
| Map | FR-MAP |
| Location | FR-LOC |
| Geofencing | FR-GEO |
| Sensors | FR-SENSOR |
| Fusion | FR-FUSION |
| Proximity | FR-PROX |
| Reveal | FR-REVEAL |
| Progress | FR-PROG |
| Leaderboard | FR-LEAD |
| Notifications | FR-NOTIF |
| Offline | FR-OFF |
| Isolation | FR-ISO |
| Performance | NFR-PERF |
| Reliability | NFR-REL |
| Security | NFR-SEC |
| Usability | NFR-USE |
| Compatibility | NFR-COMP |
| Maintainability | NFR-MAIN |

---

# 54. Definition of Done — Product Level

The Campus Quest MVP can be considered product-complete when:

### Creator

- [ ] Can create a game.
- [ ] Can save a draft.
- [ ] Can add multiple checkpoints.
- [ ] Can configure checkpoint information.
- [ ] Can edit checkpoints.
- [ ] Can publish a valid game.

### Player

- [ ] Can browse published games.
- [ ] Can view game details.
- [ ] Can join a game.
- [ ] Can view the game's map.
- [ ] Can approach dynamic checkpoints.
- [ ] Can trigger the scan through the intended location flow.
- [ ] Can perform the sensor-based challenge.
- [ ] Can receive the checkpoint reveal.
- [ ] Can view game-specific progress.
- [ ] Can view the correct game leaderboard.

### Platform

- [ ] Authentication works.
- [ ] Location works.
- [ ] Geofencing works.
- [ ] GPS/light/motion fusion works.
- [ ] Proximity final gate works.
- [ ] Room persistence works.
- [ ] Firestore synchronization works.
- [ ] Notification flow works.
- [ ] Offline behavior works.
- [ ] Cross-game isolation works.

---

# 55. Product-Level Regression Checklist

Before the final demonstration:

```text
[ ] Register / Login
[ ] Create Game
[ ] Save Draft
[ ] Add Checkpoint
[ ] Edit Checkpoint
[ ] Publish
[ ] Game Appears in Player List
[ ] Game Details
[ ] Join
[ ] Map
[ ] Dynamic Checkpoints
[ ] Geofence ENTER
[ ] Scan
[ ] GPS Signal
[ ] Light Signal
[ ] Motion Signal
[ ] Fusion Threshold
[ ] Proximity Gate
[ ] Reveal
[ ] Progress
[ ] Room Persistence
[ ] Firestore Sync
[ ] Game Leaderboard
[ ] New-Game Notification
[ ] Notification Deep Link
[ ] Offline Discovery
[ ] Reconnection Sync
[ ] Second Game
[ ] Cross-Game Isolation
[ ] Permission Error Handling
[ ] Sensor Error Handling
```

---

# 56. Critical Product Rules

The following rules must not be violated during implementation.

## Rule 1 — Games Are Dynamic

```text
There is no fixed number of checkpoints per game.
```

## Rule 2 — Checkpoints Are Game-Scoped

```text
gameId + checkpointId
```

must be preserved.

## Rule 3 — No Global Leaderboard

Each game has its own leaderboard.

## Rule 4 — Geofence Is Not Discovery

```text
ENTER ≠ DISCOVERED
```

Geofence ENTER enables or routes the scan.

## Rule 5 — Three Weighted Signals

```text
GPS + Light + Motion
```

are the weighted fusion signals.

## Rule 6 — Proximity Is Separate

```text
Fusion Passed
    ↓
Proximity Final Gate
    ↓
Reveal
```

Proximity is not a fourth weighted input.

## Rule 7 — R001–R006 Are Not Architecture

They may be seed/demo checkpoint IDs only.

## Rule 8 — Creator Data Drives Gameplay

Player gameplay must consume the checkpoint configuration belonging to the selected game.

## Rule 9 — Game Context Must Survive Navigation

Relevant screens and operations must preserve `gameId`, and checkpoint-specific operations must preserve `checkpointId`.

## Rule 10 — Product Behavior Comes Before Implementation Convenience

A shortcut that makes development easier but violates the dynamic Game Creator / Game Player model is an architectural regression.

---

# 57. Final Product Definition

Campus Quest is ultimately:

```text
A platform for creating and playing location-based treasure-hunt games.
```

The complete product model is:

```text
                    CAMPUS QUEST
                         │
             ┌───────────┴───────────┐
             │                       │
       GAME CREATOR             GAME PLAYER
             │                       │
       Create Game              Browse Games
             │                       │
       Add Checkpoints          View Details
             │                       │
       Save Draft                   Join
             │                       │
          Publish                    Map
             │                       │
             └──────────┬────────────┘
                        │
                   GAME INSTANCE
                        │
                Dynamic Checkpoints
                        │
                   Explore Area
                        │
                  Geofence ENTER
                        │
              GPS + Light + Motion
                        │
                 Weighted Fusion
                        │
                Fusion Threshold
                        │
              Proximity Final Gate
                        │
                     Reveal
                        │
                 Record Discovery
                        │
                 Game Progress
                        │
              Game-Specific Leaderboard
```

This PRD is the product-level source of truth. Technical documents should explain how these requirements are implemented without changing the product behavior defined here.
