# MEMBER 2 — CREATOR & GAME MANAGEMENT WORKPLAN
## Campus Quest — Final Reassigned Team Plan

**Owner:** Member 2 (M2)  
**Primary responsibility:** Creator-facing game and checkpoint management  
**Branch:** `feature/m2-creator-game-management`  
**Architecture:** Android + Kotlin, MVVM + Repository, Room + Firebase/Firestore  
**Status:** Final reassignment version

---

# 1. PURPOSE

M2 owns the complete **Game Creator** workflow for Campus Quest.

The creator must be able to:

1. Enter creator/game-management functionality.
2. Create a new game.
3. Enter and edit game metadata.
4. Add checkpoints dynamically.
5. Configure checkpoint location and geofence radius.
6. Configure the ambient-light signature.
7. Add clues and lore.
8. Configure checkpoint order, rarity, and motion type.
9. Edit and delete checkpoints.
10. Save a game as a draft.
11. Validate the game before publication.
12. Publish a valid game.
13. Hand the published game to the backend so it becomes available to players.
14. Return to edit drafts where permitted.

The creator workflow must be **dynamic**.

The app must not require a permanent hard-coded set of checkpoints such as `R001–R006`. Those identifiers may exist only as seed/demo data.

---

# 2. IMPORTANT REASSIGNMENT

The previous M2 responsibility for **Quest / Scan UI** is no longer part of M2's ownership.

## M2 now owns

- Creator entry and creator navigation integration.
- Create-game screens.
- Game metadata editing.
- Checkpoint creation.
- Checkpoint editing.
- Checkpoint deletion.
- Checkpoint ordering.
- Location configuration fields.
- Geofence radius configuration.
- Ambient-light signature configuration fields.
- Clue/lore configuration.
- Rarity configuration.
- Motion-type configuration.
- Draft management.
- Creator-side validation.
- Publish UI and publish workflow.
- Creator ViewModels and creator UI state.

## M2 does NOT own

- Player scan HUD.
- GPS sensor implementation.
- Fused Location Provider implementation.
- Geofence registration internals.
- Ambient-light sensor implementation.
- Accelerometer implementation.
- Proximity sensor implementation.
- Sensor-fusion mathematics.
- Room database implementation.
- Firestore implementation.
- FCM implementation.
- Repository implementation internals.

Those responsibilities belong primarily to M3, M5 and M6.

---

# 3. CANONICAL DOMAIN MODEL

M2 must build against these concepts.

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

M2 should consume the shared model rather than creating an independent creator-only model that conflicts with the rest of the application.

---

# 4. CREATOR USER JOURNEY

The intended creator flow is:

```text
Creator Entry
    ↓
Creator Games
    ↓
Create New Game
    ↓
Game Details Editor
    ↓
Checkpoint List
    ↓
Add Checkpoint
    ↓
Checkpoint Editor
    ↓
Save Checkpoint
    ↓
Checkpoint List
    ↓
Add/Edit/Delete/Reorder
    ↓
Save Draft
    ↓
Validate Game
    ↓
Publish Confirmation
    ↓
Publish
    ↓
Published Game
```

A creator may leave an incomplete game as a draft.

Publication is a separate state transition.

---

# 5. CREATOR ENTRY

## Responsibility

M2 supplies the creator-side destination and navigation contract.

M1 owns the shared application navigation shell.

M2 must therefore provide:

- Creator destination/screen contract.
- Creator navigation routes.
- Required navigation arguments.
- Creator game-list state requirements.
- Navigation to create/edit game.
- Navigation to checkpoint editor.

M2 should not duplicate the application's global navigation implementation.

---

# 6. CREATOR GAMES SCREEN

This screen shows games created by the current user.

Possible sections:

```text
My Games

[ + Create Game ]

Draft Games
    Game A
    4 checkpoints
    Draft
    [Edit]

Published Games
    Game B
    8 checkpoints
    Published
    [View]
```

The exact visual layout should follow the project's established UI/UX specification and Material Design guidance.

The screen must distinguish:

- Draft.
- Published.
- Closed, if supported by the final implementation.

---

# 7. CREATE GAME SCREEN

The initial game editor should collect:

- Game title.
- Game description.

The creator identity must come from the authenticated user context.

The creator should not manually enter an arbitrary `creatorId`.

Example state:

```kotlin
data class GameEditorUiState(
    val gameId: String? = null,
    val title: String = "",
    val description: String = "",
    val status: GameStatus = GameStatus.DRAFT,
    val checkpointCount: Int = 0,
    val isSaving: Boolean = false,
    val isPublishing: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    val saveError: String? = null
)
```

---

# 8. GAME TITLE VALIDATION

At minimum, validate:

- Title is not blank.
- Title is not whitespace-only.
- Title length is within the application's agreed limit.
- Unsafe/invalid input is rejected according to the application's validation rules.

Do not silently invent different validation rules in M2 if shared project contracts already define them.

The UI should show validation close to the relevant field.

---

# 9. GAME DESCRIPTION VALIDATION

Validate:

- Description is not unintentionally empty if publication requires it.
- Length is within the agreed project limit.
- Excessive whitespace is handled consistently.

Drafts may be allowed to remain incomplete if the product specification permits it.

Publication validation must be stricter than draft saving.

---

# 10. CHECKPOINT LIST

The checkpoint list is the central creator-management screen.

Each checkpoint should expose enough information for the creator to understand its configuration.

Example:

```text
Checkpoint 1
Old Library
Location configured
Radius: 20 m
Light: configured
[Edit] [Delete]

Checkpoint 2
Main Courtyard
Location configured
Radius: 30 m
Light: configured
[Edit] [Delete]
```

The list must support dynamic counts.

Do not code:

```kotlin
val checkpoints = listOf(R001, R002, R003, R004, R005, R006)
```

as the production architecture.

---

# 11. ADD CHECKPOINT

The creator selects:

```text
+ Add Checkpoint
```

A new checkpoint editor opens.

The checkpoint belongs to the current game through:

```kotlin
gameId
```

The checkpoint ID must be unique within the game.

M2 should obtain IDs through the agreed repository/data contract rather than assuming a permanent global sequence.

---

# 12. CHECKPOINT EDITOR

The checkpoint editor should provide fields for:

### Identity/content

- Name.
- Clue.
- Lore.

### Location

- Latitude.
- Longitude.
- Geofence radius.

### Sensor-fusion configuration

- Minimum expected ambient light.
- Maximum expected ambient light.

### Gameplay metadata

- Order.
- Motion type.
- Rarity.

Example conceptual state:

```kotlin
data class CheckpointEditorUiState(
    val checkpointId: String? = null,
    val gameId: String,
    val name: String = "",
    val lat: String = "",
    val lng: String = "",
    val radiusM: String = "20",
    val minLux: String = "",
    val maxLux: String = "",
    val clue: String = "",
    val lore: String = "",
    val order: String = "1",
    val motionType: String = "SWEEP",
    val rarity: String = "COMMON",
    val isSaving: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)
```

This is a UI-state representation. It is not a replacement for the shared domain model.

---

# 13. LOCATION CONFIGURATION

M2 owns the creator-facing configuration of coordinates.

M2 does not implement the location provider.

The editor may receive coordinates through:

- Map-based selection.
- Location picker.
- Manual latitude/longitude entry.

The exact mechanism should follow the project's UI specification.

M3 owns the actual map/location/geospatial implementation.

## Integration contract

M2 requests or receives:

```text
SelectedLocation(
    latitude,
    longitude
)
```

and places those values into the checkpoint editor.

M2 should not directly depend on `FusedLocationProviderClient`.

---

# 14. GEOFENCE RADIUS

The creator configures:

```text
radiusM
```

Example:

```text
Geofence radius
[ 20 ] metres
```

Validation should ensure:

- Numeric value.
- Positive value.
- Within the agreed safe/project range.
- No impossible or nonsensical radius.

The radius is stored with the checkpoint.

M3 later consumes the radius when registering dynamic geofences.

---

# 15. AMBIENT LIGHT SIGNATURE

The creator configures an expected environmental light range:

```text
Minimum Lux: 100
Maximum Lux: 350
```

This represents the **expected ambient environmental light range at the checkpoint**.

It is not a light emitted by the relic.

M2 owns the input/configuration UI.

M3 owns:

- Light sensor access.
- Actual lux reading.
- Light matching.
- Normalization.
- Fusion.

M2 must therefore store the configuration as data and not calculate sensor scores.

---

# 16. LIGHT SIGNATURE VALIDATION

The UI should validate:

```text
minLux >= 0
maxLux >= 0
minLux <= maxLux
```

If the project contract defines an additional sensor range, M2 should use that shared range.

Example:

```kotlin
if (minLux < 0) error("Minimum lux cannot be negative")
if (maxLux < 0) error("Maximum lux cannot be negative")
if (minLux > maxLux) error("Minimum lux cannot exceed maximum lux")
```

---

# 17. CLUE

The clue is the player-facing hint.

Example:

```text
Clue:
"Look for the place where knowledge sleeps beneath old stone."
```

M2 owns the creator input.

M4 owns the player-side reveal/presentation.

M2 should not implement the scan/reveal UI.

---

# 18. LORE

Lore provides additional narrative information.

Example:

```text
Lore:
"This building has been part of campus life for decades..."
```

M2 stores the creator's text.

M4 presents the content after successful discovery.

---

# 19. RARITY

Checkpoint rarity is creator-configurable metadata.

Example values may include:

```text
COMMON
RARE
EPIC
LEGENDARY
```

The exact accepted values must match the shared project contract.

M2 should avoid hard-coding display text in multiple places.

Prefer a shared enum or controlled value set where the architecture supports it.

---

# 20. MOTION TYPE

Motion type defines the intended physical interaction used during scanning.

The current canonical default is:

```text
SWEEP
```

M2 stores the configuration.

M3 interprets the sensor/motion behavior.

M4 presents the gameplay state.

M2 must not implement accelerometer processing.

---

# 21. CHECKPOINT ORDER

Creators need to control checkpoint sequence.

Example:

```text
1. Old Library
2. Main Courtyard
3. Engineering Building
4. Science Block
```

The `order` field belongs to the checkpoint.

When a creator reorders checkpoints:

1. Update the local/editor state.
2. Validate order uniqueness/consistency.
3. Persist through the repository contract.
4. Ensure the player receives the intended ordering.

Do not assume checkpoint order is determined by document creation time.

---

# 22. EDIT CHECKPOINT

When the creator selects Edit:

```text
Checkpoint List
    ↓
Checkpoint Editor
    ↓
Load existing checkpoint
    ↓
Modify fields
    ↓
Validate
    ↓
Save
```

The editor must distinguish:

- New checkpoint.
- Existing checkpoint.

Avoid creating a new checkpoint accidentally when editing an existing one.

---

# 23. DELETE CHECKPOINT

Before deletion, the UI should request confirmation.

Example:

```text
Delete checkpoint?

This checkpoint will be removed from this game.

[Cancel] [Delete]
```

M2 owns the confirmation UX.

M5/M6 own the underlying cloud/local persistence implementation.

If the checkpoint is already part of an active/published game, deletion behavior must follow the product's lifecycle rules. M2 must not invent destructive production behavior independently.

---

# 24. DRAFT SUPPORT

Drafts are important because creators may not finish an entire game in one session.

The workflow should support:

```text
Create Game
    ↓
Save Draft
    ↓
Leave
    ↓
Return Later
    ↓
Edit
    ↓
Continue
    ↓
Publish
```

A draft may contain incomplete information.

Therefore:

```text
Draft validation != Publish validation
```

A draft can be saved when publication requirements are not yet satisfied, subject to the agreed project rules.

---

# 25. SAVE DRAFT

M2 triggers:

```kotlin
gameRepository.createGame(...)
```

or the appropriate update operation defined by the shared repository contract.

For checkpoints:

```kotlin
gameRepository.createCheckpoint(...)
```

or:

```kotlin
gameRepository.updateCheckpoint(...)
```

M2 does not implement Firestore writes or Room DAOs.

---

# 26. LOCAL DRAFT SUPPORT

If the application allows offline creator editing, M2 must use the repository boundary.

The intended architecture is:

```text
Creator UI
   ↓
Creator ViewModel
   ↓
GameRepository
   ↓
Room / Firebase
```

M6 owns:

- Local draft persistence.
- Room entities.
- DAO.
- Pending sync.
- Retry.
- Conflict handling.

M2 only needs to expose the appropriate UI states.

---

# 27. PUBLISH VALIDATION

Publishing is more restrictive than saving a draft.

Before publishing, validate the complete game.

Conceptually:

```text
Game
 ├─ valid title
 ├─ valid description
 ├─ at least required checkpoint count
 ├─ every checkpoint has valid coordinates
 ├─ every checkpoint has valid radius
 ├─ every checkpoint has valid light signature
 ├─ every checkpoint has required clue/lore
 ├─ every checkpoint has valid order
 └─ all required gameplay metadata configured
```

The exact minimum checkpoint count and field requirements must follow the canonical product specification.

M2 should not invent a different publication rule.

---

# 28. PUBLISH VALIDATION UI

If validation fails, present actionable errors.

Example:

```text
Cannot publish yet.

• Checkpoint 2 has no clue.
• Checkpoint 3 has an invalid light range.
• Checkpoint 4 has no location.
```

Prefer errors that identify the checkpoint and field.

Do not show only:

```text
Invalid game.
```

when more useful information is available.

---

# 29. PUBLISH CONFIRMATION

After validation:

```text
Publish Game?

Once published, players will be able to discover and join this game.

[Cancel] [Publish]
```

M2 owns this confirmation UI.

M5 owns the backend publication operation.

---

# 30. PUBLISH OPERATION

M2 calls the repository boundary:

```kotlin
gameRepository.publishGame(gameId)
```

The backend is responsible for the actual state transition:

```text
DRAFT → PUBLISHED
```

and related backend operations.

The intended event flow is:

```text
Creator
   ↓
Publish
   ↓
Firestore game status = PUBLISHED
   ↓
New-game notification event
   ↓
FCM /topics/new_games
   ↓
Registered players
```

M5 owns Firebase/FCM.

---

# 31. PUBLISH SUCCESS STATE

After successful publication:

```text
Game Published

Your game is now available to players.

[View Game]
```

The UI should not claim that notification delivery succeeded merely because the publish call succeeded.

Backend notification delivery is M5's responsibility.

---

# 32. PUBLISH FAILURE

Possible states:

- Network unavailable.
- Permission denied.
- Backend validation failed.
- Game changed before publication.
- Unknown server error.

M2 should translate repository results into useful UI states.

Example:

```kotlin
sealed interface PublishResultUiState {
    data object Idle : PublishResultUiState
    data object Publishing : PublishResultUiState
    data object Success : PublishResultUiState
    data class Error(val message: String) : PublishResultUiState
}
```

---

# 33. VIEWMODEL RESPONSIBILITY

M2 should use ViewModels to coordinate creator UI state.

Conceptually:

```text
Creator Screen
      ↓
Creator ViewModel
      ↓
GameRepository
      ↓
Data layer
```

The ViewModel should handle:

- Form state.
- Validation.
- Save requests.
- Publish requests.
- Loading states.
- Error states.
- Navigation events/state.
- Checkpoint list state.

The ViewModel should not contain:

- Firestore SDK code.
- Room DAO code.
- SensorManager code.
- FusedLocationProviderClient code.
- Geofence registration code.

---

# 34. SUGGESTED VIEWMODELS

Possible separation:

```text
CreatorGamesViewModel
CreateGameViewModel
CheckpointListViewModel
CheckpointEditorViewModel
```

The exact number can be adjusted to the project's existing architecture.

Do not create ViewModels simply to satisfy a naming convention; each should have a meaningful state-management responsibility.

---

# 35. CREATOR GAMES VIEWMODEL

Responsibilities:

- Load creator's games.
- Expose draft/published lists.
- Start create-game flow.
- Open existing game.
- Refresh state.
- Handle repository errors.

Conceptual state:

```kotlin
data class CreatorGamesUiState(
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

# 36. CREATE GAME VIEWMODEL

Responsibilities:

- Hold title/description.
- Validate fields.
- Save game.
- Expose save state.
- Navigate to checkpoint management after successful creation.

The creator ID/name should be resolved through authenticated user context rather than typed into the form.

---

# 37. CHECKPOINT LIST VIEWMODEL

Responsibilities:

- Load checkpoints for a selected game.
- Expose ordered checkpoints.
- Add checkpoint navigation event.
- Edit checkpoint navigation event.
- Request deletion.
- Handle reorder.
- Refresh after changes.

---

# 38. CHECKPOINT EDITOR VIEWMODEL

Responsibilities:

- Load existing checkpoint.
- Maintain form state.
- Validate fields.
- Convert UI strings into typed values.
- Save new checkpoint.
- Update existing checkpoint.
- Report errors.

For example:

```text
"20"
```

becomes:

```kotlin
20f
```

only after validation.

Do not allow malformed UI strings to reach the domain/data layer.

---

# 39. NAVIGATION CONTRACT WITH M1

M2 should define stable routes/arguments for creator screens.

Example conceptual routes:

```text
creator/games
creator/game/new
creator/game/{gameId}
creator/game/{gameId}/checkpoint/new
creator/game/{gameId}/checkpoint/{checkpointId}/edit
```

These are contracts, not necessarily the final literal route syntax.

Required arguments:

- `gameId`
- `checkpointId` where applicable.

M1 integrates these routes into the application navigation shell.

---

# 40. MAP SELECTION CONTRACT WITH M3

M2 may request a map/location picker.

The interface should conceptually return:

```kotlin
data class SelectedLocation(
    val latitude: Double,
    val longitude: Double
)
```

M2 then updates:

```kotlin
lat
lng
```

M2 does not own the map engine or location provider.

---

# 41. REPOSITORY CONTRACT

M2 consumes:

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

M2 mainly uses:

- `createGame`
- `getGameDetails`
- `createCheckpoint`
- `updateCheckpoint`
- `publishGame`
- `getGameCheckpoints`

The interface may evolve through shared team agreement.

---

# 42. SECURITY BOUNDARY

M2 must assume that client-side validation is not sufficient.

A malicious client could bypass UI validation.

M5 owns Firestore security and backend validation.

The backend should enforce:

- Creator ownership.
- Valid game transitions.
- Valid checkpoint ownership through game ownership.
- Publication restrictions.
- Appropriate access to draft games.

M2 should surface backend rejection cleanly.

---

# 43. CREATOR OWNERSHIP

The creator must only manage games belonging to the authenticated creator.

Conceptually:

```text
authenticated user
        ↓
creatorId
        ↓
owned games
        ↓
owned checkpoints
```

M2 must never expose a free-form creator ID field that lets a user claim another creator's game.

---

# 44. GAME-SCOPED CHECKPOINTS

Every checkpoint must be scoped to its game:

```kotlin
checkpoint.gameId == game.id
```

This prevents a checkpoint from accidentally appearing in another game.

This is particularly important when the app contains multiple games.

---

# 45. MULTI-GAME ISOLATION

Test:

```text
Game A
  ├─ CP-A1
  └─ CP-A2

Game B
  ├─ CP-B1
  └─ CP-B2
```

Editing Game A must never modify Game B.

Deleting CP-A1 must never delete CP-B1.

Reordering Game A must never reorder Game B.

---

# 46. UI STATE REQUIREMENTS

Every major creator operation should represent:

```text
Idle
Loading
Success
Error
```

For save/publish operations also represent:

```text
Saving
Publishing
```

Avoid allowing repeated rapid taps to submit duplicate operations.

---

# 47. DOUBLE-SUBMISSION PROTECTION

For operations such as:

- Create.
- Save.
- Publish.
- Delete.

the UI should disable or guard the operation while it is already executing.

Example:

```text
Publishing...
```

instead of allowing:

```text
Publish
Publish
Publish
```

---

# 48. ERROR HANDLING

Errors should be:

- Human-readable.
- Actionable.
- Associated with the correct field when possible.
- Recoverable when possible.

Examples:

```text
Unable to save checkpoint.
Check your connection and try again.
```

or:

```text
Minimum lux must be less than or equal to maximum lux.
```

Do not expose raw Firebase exception messages to users unless intentionally mapped.

---

# 49. OFFLINE BEHAVIOR

If offline creator editing is supported:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room
 ↓
Pending Sync
 ↓
Firebase
```

M2 should not implement the synchronization engine.

M6 owns pending synchronization and conflict/idempotency behavior.

M2 should still provide UI states such as:

```text
Saved locally
Waiting for sync
Synced
Sync failed
```

if those states are exposed by the repository contract.

---

# 50. MOCK DATA FOR M2

During UI development, M2 may use:

```text
demo-campus-quest
```

and sample checkpoints such as:

```text
R001
R002
R003
R004
R005
R006
```

However:

**These are seed/demo records, not the production architecture.**

M2 must ensure the UI works with:

- 0 checkpoints.
- 1 checkpoint.
- 2 checkpoints.
- Many checkpoints.
- Long checkpoint names.
- Missing optional data.
- Validation errors.
- Draft games.
- Published games.

---

# 51. ZERO-CHECKPOINT STATE

Example:

```text
No checkpoints yet.

Add your first checkpoint to begin building the quest.

[ + Add Checkpoint ]
```

Do not crash or display an empty screen with no explanation.

---

# 52. MANY-CHECKPOINT STATE

The checkpoint list should remain usable when a creator adds many checkpoints.

Use the project's established list component.

Avoid hard-coding six cards.

The implementation must support a dynamic list.

---

# 53. CHECKPOINT FORM VALIDATION MATRIX

| Field | Draft | Publish |
|---|---|---|
| Name | Validate when entered | Required |
| Latitude | May be incomplete if drafts allow | Required |
| Longitude | May be incomplete if drafts allow | Required |
| Radius | Validate if present | Required |
| Min Lux | Validate if present | Required |
| Max Lux | Validate if present | Required |
| Clue | May be incomplete if drafts allow | Required if specified by product rules |
| Lore | May be incomplete if drafts allow | Required if specified by product rules |
| Order | Must remain consistent | Required |
| Motion Type | Default may be used | Valid value |
| Rarity | Default may be used | Valid value |

The exact publication requirements must follow the canonical project specification.

---

# 54. TESTING — UNIT TESTS

M2 should write tests for creator-specific business/UI logic.

Examples:

```text
blank title rejected
valid title accepted
invalid radius rejected
negative radius rejected
minLux > maxLux rejected
valid light range accepted
missing checkpoint location rejected for publish
duplicate checkpoint order rejected if prohibited
draft can save incomplete data where permitted
publish blocked when required fields are missing
```

---

# 55. VIEWMODEL TESTS

Test:

```text
CreateGameViewModel
CheckpointEditorViewModel
CheckpointListViewModel
CreatorGamesViewModel
```

Verify:

- Correct initial state.
- Validation state.
- Loading state.
- Success state.
- Error state.
- Repository interaction.
- No duplicate publish call.
- Correct game/checkpoint IDs.

---

# 56. NAVIGATION TESTS

Verify:

```text
Creator Games
 → New Game
 → Checkpoint List
 → New Checkpoint
 → Save
 → Checkpoint List
```

and:

```text
Creator Games
 → Existing Game
 → Edit Checkpoint
 → Save
```

Also verify invalid/missing IDs do not cause uncontrolled crashes.

---

# 57. INTEGRATION TESTS

M2 should test with M5/M6 integration:

```text
Create game
   ↓
Persist
   ↓
Reload
   ↓
Create checkpoint
   ↓
Reload
   ↓
Edit checkpoint
   ↓
Reload
   ↓
Publish
   ↓
Verify published state
```

For offline support:

```text
Create/edit offline
   ↓
Local save
   ↓
Reconnect
   ↓
Sync
   ↓
Verify cloud state
```

---

# 58. ACCEPTANCE TEST — CREATE GAME

### Given

Authenticated creator.

### When

Creator enters valid title and description and saves.

### Then

A new draft game exists with:

```text
creatorId = authenticated user
status = DRAFT
```

---

# 59. ACCEPTANCE TEST — CREATE CHECKPOINT

### Given

Existing draft game.

### When

Creator adds a valid checkpoint.

### Then

The checkpoint is associated with:

```text
gameId = current game ID
```

and appears in the ordered checkpoint list.

---

# 60. ACCEPTANCE TEST — EDIT CHECKPOINT

### Given

Existing checkpoint.

### When

Creator changes its clue and radius.

### Then

The same checkpoint ID remains associated with the game and the new values are persisted.

---

# 61. ACCEPTANCE TEST — PUBLISH

### Given

A valid draft game.

### When

Creator selects Publish and confirms.

### Then

The repository requests:

```kotlin
publishGame(gameId)
```

and the game transitions to the published state if backend validation succeeds.

---

# 62. ACCEPTANCE TEST — INVALID PUBLISH

### Given

A game with a required field missing.

### When

Creator selects Publish.

### Then

Publication is blocked and the creator is told what must be fixed.

---

# 63. ACCEPTANCE TEST — GAME ISOLATION

### Given

Two games owned by the same creator.

### When

Creator edits a checkpoint in Game A.

### Then

Game B remains unchanged.

---

# 64. ACCEPTANCE TEST — CREATOR OWNERSHIP

### Given

Authenticated creator A.

### When

Creator A attempts to edit a game owned by creator B.

### Then

The backend rejects the operation and the client handles the rejection safely.

---

# 65. MATERIAL DESIGN / UI GUIDANCE

Use the project's Material Design direction consistently.

Relevant patterns include:

- App bars.
- Cards.
- Buttons.
- FAB for creation where appropriate.
- Clear form fields.
- Dialog confirmation.
- Lists for dynamic checkpoints.
- Error/helper text.
- Consistent spacing and typography.

Do not redesign the product independently of the established Campus Quest UI/UX specification.

---

# 66. ACCESSIBILITY

Creator screens should support:

- Meaningful content descriptions.
- Adequate touch targets.
- Readable text.
- Clear error messages.
- Keyboard-friendly text entry.
- Avoiding color-only error indicators.
- Logical focus order.

---

# 67. RESPONSIVE BEHAVIOR

The creator UI should remain usable across supported Android screen sizes.

Check:

- Small phone.
- Normal phone.
- Large phone.
- Rotation if the project supports it.

Do not assume a fixed screen width.

---

# 68. IMPLEMENTATION ORDER

Recommended order:

```text
1. Review shared contracts.
2. Set up creator navigation contract.
3. Build Creator Games screen.
4. Build Create Game editor.
5. Implement game form validation.
6. Build checkpoint list.
7. Build checkpoint editor.
8. Add location-selection integration contract.
9. Add radius/light configuration.
10. Add clue/lore/rarity/motion configuration.
11. Add edit/delete/reorder.
12. Add draft save.
13. Add publish validation.
14. Add publish confirmation.
15. Connect repository.
16. Test Room/Firebase integration.
17. Test multi-game isolation.
18. Final integration with M1/M3/M5/M6.
```

---

# 69. DEPENDENCIES

## M1

Provides:

- Shared navigation shell.
- Authenticated-user navigation.
- Shared UI components.
- Player/creator navigation integration.

## M3

Provides:

- Map/location selection.
- Location data contract.
- Geospatial configuration support where needed.

## M5

Provides:

- Firebase Auth context.
- Firestore repository/backend implementation.
- Creator ownership validation.
- Publish operation.
- FCM publication event.

## M6

Provides:

- Room.
- Local persistence.
- Repository implementation/boundary.
- Offline draft storage.
- Synchronization.

---

# 70. HANDOFF TO M1

M2 should deliver:

```text
Creator route definitions
Navigation arguments
Creator screen list
Expected navigation events
Shared UI components required
```

Example:

```text
GameCreated(gameId)
CheckpointSaved(gameId, checkpointId)
GamePublished(gameId)
```

The exact implementation may use the project's established navigation/event pattern.

---

# 71. HANDOFF TO M3

M2 should document:

```text
Checkpoint location selection input/output
latitude
longitude
radiusM
```

M3 should document any constraints on coordinate precision or radius.

---

# 72. HANDOFF TO M5

M2 should provide:

```text
Game object requirements
Checkpoint object requirements
Publish validation expectations
Creator ownership assumptions
Error cases
```

M5 must confirm server-side validation.

---

# 73. HANDOFF TO M6

M2 should provide:

```text
Required local game fields
Required local checkpoint fields
Draft behavior
Save/update/delete expectations
Offline UI states required
```

M6 maps these requirements into Room entities/DAOs.

---

# 74. GIT WORKFLOW

Branch:

```text
feature/m2-creator-game-management
```

Commit examples:

```text
feat(creator): add creator games screen
feat(creator): add game editor
feat(creator): add checkpoint editor
feat(creator): add checkpoint validation
feat(creator): add checkpoint reorder support
feat(creator): add publish validation
feat(creator): add publish confirmation flow
test(creator): add game editor validation tests
test(creator): add checkpoint editor tests
```

Avoid mixing unrelated M3 sensor or M5 Firebase implementation into M2 commits.

---

# 75. CHANGE CONTROL

Before changing shared models or interfaces:

1. Identify the affected contract.
2. Inform the team.
3. Agree on the change.
4. Update shared documentation.
5. Update dependent code.
6. Run affected tests.

Do not silently create incompatible versions of:

- `Game`.
- `Checkpoint`.
- `GameStatus`.
- Repository methods.
- Navigation arguments.

---

# 76. DEFINITION OF DONE

M2 is complete when:

### Creator entry

- Creator can access creator functionality.
- Navigation works through the shared app shell.

### Game management

- Creator can create a game.
- Creator can edit game metadata.
- Creator can save drafts.
- Creator can reopen drafts.

### Checkpoint management

- Creator can add checkpoints.
- Creator can edit checkpoints.
- Creator can delete checkpoints.
- Creator can reorder checkpoints.
- Checkpoints are game-scoped.

### Configuration

- Coordinates can be configured.
- Radius can be configured.
- Ambient-light range can be configured.
- Clue can be configured.
- Lore can be configured.
- Rarity can be configured.
- Motion type can be configured.

### Publication

- Publish validation works.
- Invalid games cannot be published.
- Publish confirmation works.
- Successful publication calls the repository contract.
- Publish failures are handled.

### Architecture

- UI uses ViewModels.
- ViewModels use repository contracts.
- M2 does not own sensor/location internals.
- M2 does not directly implement Room or Firebase.

### Testing

- Creator validation tests pass.
- ViewModel tests pass.
- Navigation flow works.
- Multi-game isolation is tested.
- Draft/publish behavior is tested.

---

# 77. WHAT M2 MUST NOT IMPLEMENT

Do not duplicate these responsibilities:

```text
FusedLocationProviderClient
Geofence registration
SensorManager
Ambient light sensor reading
Accelerometer processing
Proximity sensor processing
Fusion score calculation
RoomDatabase
Room DAO implementation
Firestore SDK implementation
FCM topic management
Player scan HUD
Player reveal screen
Player leaderboard backend
```

M2 integrates with these features through shared contracts.

---

# 78. LEGACY CLEANUP

The old fixed relic/checkpoint implementation must not remain the primary creator architecture.

If old code contains:

```text
Relic
FoundRelic
R001
R002
R003
R004
R005
R006
```

M2 should identify it for migration/removal where it conflicts with the dynamic model.

The six sample IDs may remain in seed data for demonstration.

They must not be required for creating a new game.

---

# 79. FINAL CREATOR ARCHITECTURE

The complete M2 path is:

```text
Creator
   ↓
Creator Games UI
   ↓
Game Editor
   ↓
Checkpoint List
   ↓
Checkpoint Editor
   ↓
Creator ViewModels
   ↓
GameRepository
   ↓
Room / Firebase
```

For publication:

```text
Creator
   ↓
Validate
   ↓
Publish Confirmation
   ↓
GameRepository.publishGame(gameId)
   ↓
Firebase / Firestore
   ↓
GameStatus.PUBLISHED
   ↓
M5 FCM publication event
   ↓
Players notified
```

---

# 80. CORE DESIGN PRINCIPLE

M2 is responsible for **defining what a creator creates**, not for implementing how the player physically discovers it.

The creator configures:

```text
Game
 ├─ title
 ├─ description
 └─ checkpoints
       ├─ location
       ├─ radius
       ├─ light range
       ├─ clue
       ├─ lore
       ├─ order
       ├─ motion type
       └─ rarity
```

The player-side system later consumes that configuration:

```text
Checkpoint configuration
        ↓
M3 location/sensor systems
        ↓
M4 scan gameplay
        ↓
Discovery
        ↓
M6 local persistence
        ↓
M5 cloud sync/leaderboard
```

This separation keeps the six-member implementation manageable and prevents M2 from becoming coupled to hardware, Firebase, Room, and gameplay internals.

---

# 81. DOCUMENTS M2 SHOULD KEEP IN SYNC

If M2 changes creator behavior, check:

- `00_MASTER_DEVELOPMENT_PLAN_UPDATED.md`
- `SHARED_CONTRACTS_AND_INTEGRATION_INTERFACES.md`
- `TRD_CAMPUS_QUEST.md`
- `PRD_CAMPUS_QUEST.md`
- `APP_FLOW_DOCUMENT_CAMPUS_QUEST.md`
- `UI_UX_DESIGN_SPECIFICATION_CAMPUS_QUEST.md`
- `MOCK_DATA_CATALOG_AND_SEED_DATA_SPECIFICATION.md`
- `INTEGRATION_HANDOFF_AND_MILESTONE_PLAN.md`
- `TESTING_AND_ACCEPTANCE_STRATEGY.md`

Ownership changes alone do not require changing product behavior, but implementation/interface changes must be reflected in the appropriate source-of-truth documents.

---

# 82. M2 QUICK CHECKLIST

```text
[ ] Creator entry
[ ] Creator games list
[ ] Create game
[ ] Edit game
[ ] Save draft
[ ] Checkpoint list
[ ] Add checkpoint
[ ] Edit checkpoint
[ ] Delete checkpoint
[ ] Reorder checkpoint
[ ] Configure coordinates
[ ] Configure geofence radius
[ ] Configure minLux/maxLux
[ ] Configure clue
[ ] Configure lore
[ ] Configure rarity
[ ] Configure motion type
[ ] Validate checkpoint
[ ] Validate game
[ ] Publish confirmation
[ ] Publish request
[ ] Publish success/error states
[ ] Multi-game isolation
[ ] Creator ownership handling
[ ] ViewModel tests
[ ] Validation tests
[ ] Navigation tests
[ ] Repository integration
[ ] Room/offline integration
[ ] Firebase integration
[ ] Final handoff
```

---

# 83. FINAL HANDOFF PACKAGE

At the end of M2 implementation, provide the team with:

1. Creator screens.
2. Creator ViewModels.
3. Navigation contracts.
4. Game/checkpoint form validation.
5. Repository calls used by creator functionality.
6. Mock/seed data used during UI development.
7. Tests.
8. Known integration assumptions.
9. List of unresolved issues.
10. Any shared-contract changes.

M2 should not hand over private implementation assumptions that other members cannot reproduce.

---

**END OF MEMBER 2 CREATOR & GAME MANAGEMENT WORKPLAN**
