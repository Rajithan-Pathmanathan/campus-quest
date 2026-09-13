# Campus Quest — Member 1 Implementation Workplan
## Player UI & Navigation Lead

**Project:** Campus Quest  
**Module:** INTE 22283 – Mobile Application Development  
**Platform:** Native Android / Kotlin / Android Studio  
**Architecture:** MVVM + Repository + Room + Firebase/Firestore  
**Member:** M1  
**Primary ownership:** Player-facing UI, navigation, shared UI shell and player flow  
**Approximate workload:** 17%

---

# 1. Role Summary

M1 owns the **player-facing application experience**.

The goal is to make the application easy for a player to navigate from authentication through discovering games, viewing game information, joining a game, entering gameplay, and viewing the game-specific leaderboard.

M1 owns the presentation and navigation layer.

M1 does **not** own:
- Firebase implementation;
- Room implementation;
- GPS implementation;
- geofencing;
- accelerometer/light/proximity sensor logic;
- sensor fusion algorithm;
- creator data persistence.

The architecture follows the separation-of-concerns principle: the UI displays data and forwards user actions, ViewModels coordinate UI state and actions, and the repository/data layer hides the data source. This is consistent with the module architecture material. fileciteturn28file0L178-L191

---

# 2. Where M1 Works in the Application

M1 primarily owns this section:

```text
Application
│
├── Authentication entry/navigation
│
├── Player
│   ├── Games
│   ├── Game Details
│   ├── Join Game
│   ├── Game navigation
│   └── Leaderboard UI
│
└── Common UI / Navigation
```

The core player journey is:

```text
Login
  ↓
Games
  ↓
Game Details
  ↓
Join Game
  ↓
Game Map / Gameplay
  ↓
Checkpoint
  ↓
Leaderboard
```

M1 owns the navigation between these destinations but does not implement the physical checkpoint-detection logic.

---

# 3. Main Responsibilities

## 3.1 Application Shell

M1 establishes:

- application entry point;
- navigation host;
- navigation graph;
- common app bar/toolbar;
- common navigation behavior;
- common loading state;
- common error state;
- common empty state;
- theme integration;
- reusable UI components.

The course UI material covers app-wide navigation, toolbar/app bar, bottom navigation, dialogs, responsive layouts and Material components. These principles should guide the implementation. fileciteturn30file0L344-L360

---

# 4. Authentication Navigation

M1 implements the presentation side of authentication.

## Screens

- Login
- Registration, if included in the agreed product scope
- authentication loading state
- authentication error state

## Responsibilities

M1 handles:

```text
User enters credentials
        ↓
Login action
        ↓
ViewModel
        ↓
Auth/repository contract
        ↓
Authentication result
        ↓
Navigate to Games
```

M5 owns Firebase Authentication.

M1 should never place Firebase authentication implementation directly inside an Activity/Fragment.

---

# 5. Games List

M1 owns the main player game-discovery screen.

## Screen purpose

Display games that the current player can discover/join.

Each game card should provide enough information to select a game.

Potential displayed information:

- game title;
- description/summary;
- creator name;
- checkpoint count;
- status where appropriate;
- published information where required.

## States

The screen must support:

```text
Loading
   ↓
Success → Game List
   ↓
Empty → No Games Available
   ↓
Error → Retry
```

## Data flow

```text
GamesScreen
    ↓
GamesViewModel
    ↓
GameRepository
    ↓
Room / Firebase
```

M1 owns the screen and ViewModel.

M6/M5 own the underlying data implementation.

---

# 6. Game Details

M1 owns the player-facing game details screen.

## Display

At minimum:

- game title;
- description;
- creator;
- checkpoint count;
- relevant game status;
- Join button;
- appropriate loading/error state.

## Flow

```text
Games List
    ↓
Select Game
    ↓
Game Details(gameId)
    ↓
Load Game
    ↓
Display Details
```

The `gameId` must be passed through navigation.

Do not pass the complete `Game` object unnecessarily when the screen can retrieve the authoritative/current data using `gameId`.

---

# 7. Join Game

M1 owns the Join Game UI and user interaction.

## Flow

```text
Game Details
      ↓
Join
      ↓
Loading
      ↓
Success
      ↓
Game Gameplay / Map
```

Possible states:

```text
Join available
Joining
Joined
Already joined
Join failed
Unauthorized
Network unavailable
```

M1 presents these states.

M5/M6 handle the data operation through the repository.

---

# 8. Player Game Navigation

Once a player joins a game, M1 handles the navigation structure around the game.

Conceptually:

```text
Game Details
      ↓
Join
      ↓
Game Map / Gameplay
      ↓
Checkpoint Discovery
      ↓
Leaderboard
```

M3 owns the map/location implementation.

M4 owns the checkpoint scanning experience.

M1 owns the navigation that connects the player flow.

---

# 9. Map/Gameplay Boundary

M1 should not implement location logic.

The map screen may be owned jointly at the integration boundary:

- M1: navigation and player-facing screen structure;
- M3: map/location/geofence implementation;
- M4: checkpoint scanning UI.

Example:

```text
M1
Game Navigation
      ↓
M3
Game Map + Location
      ↓
Geofence ENTER
      ↓
M4
Scan Experience
```

M1 must consume states/contracts rather than directly controlling sensors or geofences.

---

# 10. Leaderboard UI

M1 owns the player-facing leaderboard presentation.

## Requirement

Leaderboards are **game-specific**.

Correct:

```text
Game A
  ↓
Leaderboard A

Game B
  ↓
Leaderboard B
```

There is no global leaderboard.

## Display

Potential fields:

- player rank;
- display name;
- checkpoints discovered;
- relevant game score/progress;
- last update where appropriate.

## Data flow

```text
LeaderboardScreen
      ↓
LeaderboardViewModel
      ↓
Repository
      ↓
leaderboards/{gameId}/entries/{uid}
```

M5 owns the Firebase leaderboard implementation.

M6 may provide local caching.

---

# 11. Notification Deep Link

M1 owns what happens after a player taps a new-game notification.

Expected flow:

```text
FCM notification
      ↓
Player taps
      ↓
Application opens
      ↓
Read gameId
      ↓
Game Details(gameId)
```

M5 owns FCM delivery.

M1 owns navigation/deep-link handling.

The notification payload should contain the game identifier required to open the correct game.

---

# 12. Common UI Components

M1 should create reusable components rather than duplicating UI code.

Examples:

```text
GameCard
PrimaryButton
LoadingView
ErrorView
EmptyStateView
GameStatusChip
CheckpointCount
AppToolbar
ConfirmationDialog
```

The exact component list can be adapted to the existing UI implementation.

Material Design should be applied consistently through the project theme and components. The supplied Material Design guide describes themes as controlling colors, typography, shapes, buttons and icons consistently across screens. fileciteturn31file1L88-L101

---

# 13. Responsive UI

M1 should avoid hard-coded screen dimensions.

Use:

- `dp` for layout dimensions;
- `sp` for text;
- responsive constraints;
- appropriate scrolling;
- layouts that adapt to different screen sizes.

The course UI material specifically recommends avoiding fixed pixel dimensions and using `dp`/`sp`, while testing across multiple device configurations. fileciteturn30file0L322-L337

---

# 14. Navigation Design

M1 should keep navigation predictable.

The application can use the project's selected Navigation Component structure.

Potential top-level player destinations may include:

```text
Games
Profile
Leaderboard
```

Do not create excessive top-level destinations.

The course material notes that bottom navigation is suited to approximately 3–5 frequently used top-level destinations, while a drawer is more appropriate when there are many destinations. fileciteturn30file0L354-L360

The final navigation structure should follow the existing UI/UX specification rather than inventing a conflicting structure.

---

# 15. ViewModel Responsibility

M1's ViewModels should:

- expose UI state;
- handle user actions;
- request repository operations;
- transform domain data into display state;
- manage loading/error/success state;
- survive configuration changes appropriately;
- avoid direct View references.

Example:

```kotlin
data class GamesUiState(
    val isLoading: Boolean = false,
    val games: List<Game> = emptyList(),
    val error: String? = null
)
```

Conceptual flow:

```text
UI event
   ↓
ViewModel
   ↓
Repository
   ↓
Result
   ↓
UiState
   ↓
UI
```

The module architecture material describes ViewModels as holding UI-related state and logic without directly referencing the View. fileciteturn28file0L92-L106

---

# 16. M1 Must Not Do These Things

Do not put the following into M1's UI classes:

```text
FirebaseFirestore.getInstance()
SensorManager
FusedLocationProviderClient
GeofencingClient
RoomDatabase
DAO queries
Firestore security logic
fusion calculations
```

Instead:

```text
UI
 ↓
ViewModel
 ↓
Repository / feature interface
```

This prevents Activities/Fragments from becoming large classes and keeps the system testable.

---

# 17. Shared Interfaces M1 Depends On

M1 needs the team to agree on:

### Game

```kotlin
Game(
    id,
    title,
    description,
    creatorId,
    creatorName,
    status,
    checkpointCount,
    createdAt,
    publishedAt
)
```

### Checkpoint

M1 may display basic checkpoint information but should not own checkpoint sensor logic.

### Repository

M1 consumes operations such as:

```kotlin
getAvailableGames()
getGameDetails(gameId)
joinGame(gameId)
observeGameLeaderboard(gameId)
```

### Navigation

At minimum:

```text
gameId
```

must be available to game-specific screens.

---

# 18. Mock Data Strategy

M1 should be able to develop without waiting for Firebase.

Use mock repository data such as:

```text
Game(
    id = "demo-campus-quest",
    title = "Campus Quest",
    description = "Explore the campus...",
    creatorId = "creator-001",
    creatorName = "Demo Creator",
    status = PUBLISHED,
    checkpointCount = 6
)
```

The exact seed data should come from the shared mock-data specification.

M1 should replace mock data with the real repository without redesigning the UI.

---

# 19. Implementation Order

## Step 1 — Project Shell

Implement:

- theme;
- navigation host;
- app entry;
- common UI state components.

## Step 2 — Authentication UI

Implement:

- login;
- loading;
- error;
- authenticated navigation.

## Step 3 — Games

Implement:

- games list;
- game card;
- loading;
- empty;
- error.

## Step 4 — Game Details

Implement:

- details;
- Join action;
- state handling.

## Step 5 — Game Navigation

Implement:

- navigation to gameplay;
- navigation to leaderboard;
- gameId arguments.

## Step 6 — Leaderboard

Implement:

- leaderboard screen;
- game-scoped data;
- loading/error/empty states.

## Step 7 — Notification Deep Link

Implement:

- gameId extraction;
- navigation to Game Details.

## Step 8 — Integration

Replace mock repository with the real repository implementation.

---

# 20. Testing

## Unit tests

Test ViewModels for:

- loading state;
- successful game loading;
- empty games;
- repository failure;
- join success;
- join failure;
- leaderboard loading;
- leaderboard error.

## Navigation tests

Verify:

```text
Login → Games
Games → Details
Details → Gameplay
Details → Leaderboard
Notification → Details
```

## UI tests

Verify:

- buttons are visible;
- game cards display correct data;
- loading states appear;
- error/retry works;
- empty states work;
- Join action changes state;
- leaderboard is game-specific.

---

# 21. Integration Tests

M1 must test against the real repository once M5/M6 provide the implementation.

### Test 1

```text
Firebase game
 ↓
Repository
 ↓
Games ViewModel
 ↓
Games UI
```

### Test 2

```text
Game Details
 ↓
Join
 ↓
Repository
 ↓
membership result
 ↓
Gameplay navigation
```

### Test 3

```text
FCM gameId
 ↓
Notification
 ↓
M1 navigation
 ↓
Game Details(gameId)
```

---

# 22. Definition of Done

M1 is complete when:

- player navigation works;
- authentication navigation works;
- games list works with real repository data;
- game details works;
- Join flow works;
- gameplay navigation works;
- leaderboard UI works;
- notification deep link works;
- loading/empty/error states exist;
- UI follows the approved design;
- UI is responsive;
- no Firebase/Room/sensor/location internals are embedded in UI classes;
- ViewModels are used for screen state;
- required tests pass;
- code is integrated into the shared branch.

---

# 23. Integration Dependencies

| Dependency | Owner | M1 needs |
|---|---|---|
| Authentication backend | M5 | Auth result/state |
| Game data | M5/M6 | `Game` data |
| Join operation | M5/M6 | Join result |
| Map/location | M3 | Navigation destination/state |
| Scan experience | M4 | Navigation destination |
| Leaderboard data | M5/M6 | Game-scoped leaderboard |
| Notification | M5 | `gameId` payload |
| Local cache | M6 | Repository interface |

M1 should not wait for all implementations before starting.

Mocks/stubs should be used.

---

# 24. Collaboration Rules

M1 must communicate before changing:

- `Game`;
- `Checkpoint`;
- repository methods;
- navigation arguments;
- leaderboard models;
- notification payload;
- shared UI state models.

If a change affects M2, M3, M4, M5 or M6, the affected member must be informed before the change is merged.

---

# 25. Git Branch

Recommended:

```text
feature/m1-player-ui-navigation
```

Example commits:

```text
feat(m1): add application navigation
feat(m1): add player games screen
feat(m1): add game details screen
feat(m1): add join game flow
feat(m1): add leaderboard UI
feat(m1): add notification deep link
test(m1): add player navigation tests
```

---

# 26. M1 Handoff to Other Members

M1 provides:

```text
Player UI
Navigation
Navigation arguments
ViewModel contracts
UI states
Leaderboard presentation
Notification navigation
```

M1 receives:

```text
M5 → authentication/cloud results
M6 → repository/local data
M3 → location/gameplay integration state
M4 → scan gameplay destination/state
```

---

# 27. End-to-End Responsibility

M1's responsibility in the final application can be summarized as:

```text
Player opens app
      ↓
M1
      ↓
Login
      ↓
Games
      ↓
Game Details
      ↓
Join
      ↓
Gameplay
      ↓
M3/M4 handle discovery
      ↓
M1
      ↓
Leaderboard
```

M1 therefore owns the **player's navigation and presentation journey**, not the underlying physical detection or data infrastructure.

---

# 28. Final M1 Checklist

### Application
- [ ] App shell
- [ ] Navigation
- [ ] Theme integration
- [ ] Common UI states

### Authentication
- [ ] Login UI
- [ ] Auth loading
- [ ] Auth error
- [ ] Authenticated navigation

### Player
- [ ] Games list
- [ ] Game cards
- [ ] Game details
- [ ] Join
- [ ] Gameplay navigation
- [ ] Leaderboard

### Notifications
- [ ] Notification deep link
- [ ] gameId routing

### Quality
- [ ] Responsive UI
- [ ] Accessibility basics
- [ ] Loading/error/empty states
- [ ] ViewModel tests
- [ ] Navigation tests
- [ ] Real repository integration

### Architecture
- [ ] No direct Firebase in UI
- [ ] No direct Room in UI
- [ ] No direct SensorManager in UI
- [ ] No direct location logic in UI
- [ ] No fusion algorithm in UI
- [ ] Shared contracts respected

---

# 29. Final M1 Ownership Statement

**M1 owns the player-facing application shell, player navigation, games discovery, game details, joining flow, leaderboard presentation and notification deep linking.**

M1 does not own creator functionality, physical checkpoint detection, sensor fusion, Firebase internals or Room internals.

The intended boundary is:

```text
M1
Player UI + Navigation
        ↓
ViewModels
        ↓
Repository Contract
        ↓
M5 / M6
Cloud + Local Data

M3
Location + Sensors + Fusion
        ↓
M4
Quest + Scan Gameplay
```

This allows M1 to develop the majority of the player-facing experience independently while integrating through stable contracts.
