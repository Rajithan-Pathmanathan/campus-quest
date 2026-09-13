# MEMBER 4 — QUEST & SCAN GAMEPLAY WORKPLAN
## Campus Quest — Final Reassigned Team Plan

**Owner:** Member 4 (M4)  
**Primary responsibility:** Player checkpoint gameplay, scan experience, fusion-meter presentation, scan state machine, and discovery/reveal UI  
**Branch:** `feature/m4-quest-scan-gameplay`  
**Architecture:** Android + Kotlin, MVVM + Repository  
**Status:** Final reassignment version

---

# 1. PURPOSE

M4 owns the **player-side checkpoint gameplay experience**.

The player has already:

```text
Logged in
   ↓
Browsed games
   ↓
Viewed game details
   ↓
Joined a game
   ↓
Opened the game map
   ↓
Approached a checkpoint
```

M4 takes over when the checkpoint becomes eligible for scanning and provides the gameplay experience that turns M3's physical-discovery signals into a clear player interaction.

M4 owns:

- Quest/checkpoint gameplay screen.
- Scan HUD.
- Fusion meter presentation.
- GPS/light/motion status presentation.
- Scan state machine.
- Scan progress presentation.
- Success/failure states.
- Proximity-gate presentation.
- Reveal dialog/screen.
- Clue/lore presentation after discovery.
- Transition back to gameplay.
- Scan-related ViewModels.
- Scan UI tests.

M4 does NOT own:

- Location APIs.
- Geofence registration.
- SensorManager.
- Light sensor implementation.
- Accelerometer implementation.
- Proximity sensor implementation.
- Fusion mathematics.
- Room.
- Firestore.
- FCM.
- Creator checkpoint configuration.
- Leaderboard backend.

---

# 2. IMPORTANT REASSIGNMENT

The previous M2 responsibility for Quest/Scan UI is now fully assigned to M4.

M3 owns the physical signal system.

M4 owns the player-facing interpretation and interaction.

The boundary is:

```text
M3
Location + Sensors + Fusion
        ↓
DiscoverySignalState
        ↓
M4
Quest + Scan UI
        ↓
Reveal
        ↓
M6/M5
Progress persistence + cloud sync
```

M4 must not reimplement M3's sensor logic inside the UI.

---

# 3. CANONICAL GAMEPLAY FLOW

The complete player gameplay path is:

```text
Game Details
     ↓
Join Game
     ↓
Game Map
     ↓
Approach Checkpoint
     ↓
Geofence ENTER / proximity-area eligibility
     ↓
Scan Mode
     ↓
GPS + Light + Motion
     ↓
Weighted Fusion
     ↓
Fusion Threshold
     ↓
Proximity Final Gate
     ↓
Reveal
     ↓
Record Discovery
     ↓
Next Checkpoint
```

The exact transition into Scan Mode depends on the shared M3 geofence/location contract.

---

# 4. CRITICAL SENSOR-FUSION RULE

M4 must preserve this mechanic exactly:

```text
GPS
 +
Light
 +
Motion
 ↓
Weighted Fusion
 ↓
Fusion Threshold
 ↓
Proximity Final Gate
 ↓
Reveal
```

Do NOT present or implement:

```text
GPS + Light + Motion + Proximity
```

as four weighted signals.

Proximity is a **separate final physical confirmation gate**.

---

# 5. PLAYER CHECKPOINT SCREEN

The checkpoint gameplay screen should communicate:

- Checkpoint name.
- Current clue/instruction where appropriate.
- Distance/status.
- Scan state.
- Fusion progress.
- GPS status.
- Light status.
- Motion status.
- Proximity status when the final gate is active.
- Clear next action.
- Success/failure feedback.

The exact visual design should follow the project's established Campus Quest UI/UX specification.

---

# 6. SCAN HUD

The scan HUD is the primary gameplay interface.

Conceptually:

```text
┌─────────────────────────────┐
│ Checkpoint Name             │
│                             │
│       FUSION 72%            │
│       ███████░░░            │
│                             │
│ GPS      ✓                  │
│ LIGHT    ✓                  │
│ MOTION   ~                  │
│                             │
│ Keep sweeping...            │
└─────────────────────────────┘
```

This is conceptual only; the final visual layout must follow the project's UI design.

---

# 7. FUSION METER

M4 displays M3's normalized fusion score.

Conceptually:

```text
fusionScore = 0.72
```

becomes:

```text
72%
```

The UI must not calculate the score itself.

M4 receives the already-computed score from M3.

---

# 8. FUSION METER RULE

The meter represents:

```text
GPS + Light + Motion
```

It does not represent:

```text
GPS + Light + Motion + Proximity
```

When fusion reaches the threshold, the UI changes state.

Example:

```text
72%
   ↓
85%
   ↓
92%
   ↓
THRESHOLD REACHED
```

Then the interface moves to the proximity confirmation phase.

---

# 9. SENSOR STATUS INDICATORS

The scan HUD can show separate indicators:

```text
GPS      READY
LIGHT    MATCHING
MOTION   SWEEP
```

M4 maps M3's technical state into understandable player-facing language.

M4 must not directly read the sensors to determine these states.

---

# 10. GPS PRESENTATION

Possible UI information:

```text
Distance: 12 m
GPS: Good
```

or:

```text
GPS signal weak
Move to a clearer location
```

The exact wording can be refined during UI implementation.

M3 provides:

- Distance.
- GPS score.
- Accuracy/availability.
- Error state.

---

# 11. LIGHT PRESENTATION

The player does not need to see raw lux values unless the product design intentionally exposes them.

Prefer a gameplay-oriented status:

```text
Light signature
Searching...
```

or:

```text
Light signature
Matched
```

M3 supplies the light score/status.

---

# 12. MOTION PRESENTATION

The player should receive a clear instruction based on the checkpoint's configured motion type.

For the canonical default:

```text
SWEEP
```

the UI might communicate:

```text
Sweep your phone slowly
```

M3 determines whether the movement pattern matches.

M4 presents the instruction and state.

---

# 13. PROXIMITY PHASE

When:

```text
fusionThresholdReached == true
```

M4 transitions the player into the final physical confirmation phase.

Example:

```text
SIGNALS ALIGNED

Bring the phone close to the checkpoint
to reveal the clue.
```

The actual interaction wording should match the product design.

---

# 14. PROXIMITY IS A GATE

M4 must treat:

```text
finalGatePassed
```

as the condition for revealing the checkpoint.

Before that:

```text
Reveal = blocked
```

After that:

```text
Reveal = eligible
```

M4 must not reveal simply because the fusion percentage reaches 100%.

---

# 15. REVEAL

Once M3 reports:

```text
finalGatePassed = true
```

M4 shows the reveal.

The reveal may include:

- Checkpoint name.
- Discovery confirmation.
- Clue.
- Lore.
- Rarity.
- Visual effect.
- Continue/Next Checkpoint action.

The exact content follows the established product/UI specification.

---

# 16. REVEAL MUST BE ONCE

A successful discovery should not repeatedly trigger the reveal when:

- Sensor values fluctuate.
- Location updates continue.
- The player remains nearby.
- The screen recomposes/re-renders.

M4 must guard the success transition.

Conceptually:

```text
DISCOVERY_ELIGIBLE
       ↓
REVEAL_SHOWN
       ↓
PROGRESS_RECORDED
```

Once successful, the active scan should be closed.

---

# 17. SCAN STATE MACHINE

Use an explicit state machine.

Suggested states:

```kotlin
enum class ScanState {
    IDLE,
    WAITING_FOR_LOCATION,
    OUTSIDE_CHECKPOINT,
    READY_TO_SCAN,
    SCANNING,
    FUSION_READY,
    WAITING_FOR_PROXIMITY,
    SUCCESS,
    REVEALING,
    COMPLETED,
    ERROR
}
```

The exact names can be adjusted to the project's shared contracts.

---

# 18. STATE TRANSITIONS

Conceptually:

```text
IDLE
 ↓
READY_TO_SCAN
 ↓
SCANNING
 ↓
FUSION_READY
 ↓
WAITING_FOR_PROXIMITY
 ↓
SUCCESS
 ↓
REVEALING
 ↓
COMPLETED
```

Possible failure path:

```text
SCANNING
 ↓
ERROR
 ↓
RETRY
 ↓
SCANNING
```

---

# 19. OUTSIDE CHECKPOINT

If the player leaves the required area before discovery:

```text
SCANNING
   ↓
OUTSIDE_CHECKPOINT
```

The exact behavior should follow the gameplay contract.

The UI should clearly explain:

```text
Move closer to continue scanning.
```

Do not erase already-recorded discoveries.

---

# 20. LOCATION UNAVAILABLE

If M3 reports:

```text
LOCATION_UNAVAILABLE
```

M4 should show a recoverable state.

Example:

```text
Location unavailable

Check that location services are enabled and try again.
```

Do not crash the scan screen.

---

# 21. SENSOR UNAVAILABLE

If a required sensor is unavailable, M4 displays the project-defined behavior.

Example:

```text
This device cannot perform this scan.
```

or the documented degraded mode.

M4 must not invent a workaround that bypasses the fusion contract.

---

# 22. SCAN ERROR

Use an explicit error state.

Example:

```kotlin
data class ScanErrorUiState(
    val title: String,
    val message: String,
    val retryAvailable: Boolean
)
```

This keeps error presentation separate from the sensor implementation.

---

# 23. RETRY

A retry should:

1. Reset only transient scan state.
2. Preserve game/checkpoint identity.
3. Reinitialize required M3 scan collection.
4. Start a fresh fusion session.

Do not accidentally create a second checkpoint discovery record.

---

# 24. VIEWMODEL RESPONSIBILITY

M4 should use a dedicated ViewModel.

Suggested:

```kotlin
class QuestScanViewModel
```

It coordinates:

- Current checkpoint.
- Scan state.
- M3 discovery signal state.
- UI state.
- Reveal state.
- Discovery-record request.
- Retry.
- Lifecycle transitions.

It should not contain raw sensor SDK calls.

---

# 25. SUGGESTED UI STATE

Conceptually:

```kotlin
data class QuestScanUiState(
    val gameId: String,
    val checkpointId: String,
    val checkpointName: String,
    val clue: String? = null,
    val lore: String? = null,
    val rarity: String? = null,
    val scanState: ScanState = ScanState.IDLE,
    val distanceM: Float? = null,
    val gpsScore: Float = 0f,
    val lightScore: Float = 0f,
    val motionScore: Float = 0f,
    val fusionScore: Float = 0f,
    val fusionThresholdReached: Boolean = false,
    val proximityState: ProximityState = ProximityState.UNKNOWN,
    val finalGatePassed: Boolean = false,
    val isRecordingDiscovery: Boolean = false,
    val error: String? = null
)
```

The actual project model may use separate state objects.

---

# 26. M3 SIGNAL CONTRACT

M4 consumes a state similar to:

```text
gameId
checkpointId
distance
gpsScore
lightScore
motionScore
fusionScore
fusionThresholdReached
proximityState
finalGatePassed
location availability
sensor availability
error
```

M3 owns how these values are calculated.

M4 owns how they are displayed and used to drive UI state.

---

# 27. M4 MUST NOT CALCULATE FUSION

Avoid code such as:

```kotlin
val score =
    gpsScore * 0.4 +
    lightScore * 0.3 +
    motionScore * 0.3
```

inside M4.

The fusion engine belongs to M3.

This prevents the UI from becoming a second, potentially inconsistent fusion implementation.

---

# 28. M4 MUST NOT READ SENSOR VALUES

Avoid:

```kotlin
SensorManager
LocationServices
GeofencingClient
```

inside M4.

M4 receives the result through the M3 contract.

---

# 29. CHECKPOINT CONTENT

After successful discovery, M4 may receive:

```text
name
clue
lore
rarity
```

from the checkpoint model.

M4 presents the content.

M2 owns creation/editing of these values.

M5/M6 own persistence.

---

# 30. DISCOVERY RECORDING

After successful physical confirmation:

```text
M3 finalGatePassed
        ↓
M4 SUCCESS
        ↓
recordDiscovery(gameId, checkpointId, foundAt)
```

M4 requests the operation through the repository/use-case boundary.

M4 does not directly write:

```text
Firestore
Room
```

---

# 31. DISCOVERY IDENTITY

Every discovery must be scoped by:

```text
userId
gameId
checkpointId
```

M4 should pass the correct:

```text
gameId
checkpointId
```

The authenticated user is resolved through the application's identity/repository layer.

---

# 32. DUPLICATE DISCOVERY PROTECTION

The same checkpoint should not be recorded repeatedly because:

- The sensor state fluctuates.
- The user reopens the screen.
- The device rotates.
- The player remains near the checkpoint.

The repository/data layer should provide idempotent recording.

M4 should also guard repeated UI success events.

---

# 33. FOUND CHECKPOINT FLOW

Conceptually:

```text
Final gate passes
      ↓
SUCCESS
      ↓
Request discovery recording
      ↓
Local progress recorded
      ↓
Cloud sync according to repository
      ↓
Reveal / completed state
```

The exact ordering can be adjusted to the shared offline contract.

---

# 34. OFFLINE DISCOVERY

M4 should support the application's offline contract.

If the repository accepts local recording while offline:

```text
Discovery
   ↓
Room/local record
   ↓
pendingSync = true
   ↓
Reveal
   ↓
M6 syncs later
```

M4 does not implement this synchronization.

---

# 35. NETWORK FAILURE DURING DISCOVERY

If network is unavailable but local discovery is supported:

```text
Discovery saved locally
Waiting for sync
```

The user should not necessarily lose the discovery.

If the canonical project contract requires online validation, M4 must follow that instead.

---

# 36. NEXT CHECKPOINT

After reveal:

```text
[Next Checkpoint]
```

should navigate back to the game progression.

The next checkpoint is determined from the game-specific checkpoint ordering/progress.

M4 must not hard-code:

```text
R001 → R002
R002 → R003
```

The player progression must be dynamic.

---

# 37. LAST CHECKPOINT

If the discovered checkpoint is the final checkpoint:

```text
Quest Complete
```

may be shown instead of:

```text
Next Checkpoint
```

The game-specific completion state must be based on the actual checkpoint list/progress.

---

# 38. GAME-SPECIFIC PROGRESSION

Progress belongs to:

```text
gameId
```

Therefore:

```text
Game A progress
```

must not affect:

```text
Game B progress
```

M4 must pass the correct game ID throughout the gameplay flow.

---

# 39. PLAYER LEADERBOARD HANDOFF

After discovery is recorded, the backend may update the game-specific leaderboard.

M4 does not calculate leaderboard ranking.

M4 may navigate to or refresh leaderboard UI through M1's player-facing flow.

The leaderboard is always:

```text
leaderboard for this game
```

not a global leaderboard.

---

# 40. NAVIGATION CONTRACT

Suggested conceptual routes:

```text
player/game/{gameId}
player/game/{gameId}/checkpoint/{checkpointId}/scan
player/game/{gameId}/checkpoint/{checkpointId}/reveal
```

The exact route syntax is defined by the application's shared navigation implementation.

Required identity:

```text
gameId
checkpointId
```

---

# 41. DEEP-LINK COMPATIBILITY

M4 should not assume that the player always arrives through the map.

The application may enter gameplay from:

- Game map.
- Continue game.
- Notification/deep-link path where appropriate.
- Previously saved progress.

The screen must validate that:

```text
gameId
checkpointId
```

refer to a valid accessible game/checkpoint.

---

# 42. LOADING STATE

When checkpoint details or scan dependencies are loading:

```text
Loading checkpoint...
```

Do not display stale information as if it were current.

---

# 43. EMPTY / INVALID CHECKPOINT

If the checkpoint cannot be found:

```text
Checkpoint unavailable

Return to the game and try again.
```

Do not crash due to:

```text
null checkpoint
```

---

# 44. ROTATION / RECREATION

The scan screen should survive normal Android recreation where supported.

Transient sensor data may be reset safely.

The app must preserve:

- gameId.
- checkpointId.
- Appropriate scan state.
- Recorded discovery status.

Do not duplicate discovery when the UI is recreated.

---

# 45. LIFECYCLE

M4 should coordinate scan lifecycle with M3.

Conceptually:

```text
Screen enters foreground
        ↓
Start/Resume scan
        ↓
Screen leaves foreground
        ↓
Pause/stop active scan
```

The exact behavior is implemented through the M3 contract.

M4 should not manually register sensors.

---

# 46. ACCESSIBILITY

The scan UI should communicate important states without relying only on color.

Examples:

```text
Fusion 78 percent
GPS signal strong
Light signature matched
Motion detected
Move closer
Proximity confirmation required
Discovery successful
```

Use accessible text/content descriptions for visual indicators.

---

# 47. ANIMATION

Animations may be used for:

- Fusion-meter growth.
- Scan activity.
- Successful discovery.
- Reveal.

However, animations must not become the source of truth.

The actual state comes from M3/repository state.

---

# 48. SCAN FEEDBACK

Feedback should make the mechanic understandable.

Examples:

```text
Searching for signals...
```

```text
GPS aligned
```

```text
Light signature matched
```

```text
Keep sweeping
```

```text
Signals aligned
Bring the phone close
```

The exact copy can follow the final UI/UX specification.

---

# 49. NO FAKE PROGRESS

Do not animate:

```text
0% → 100%
```

on a timer independently of actual sensor fusion.

The fusion meter must reflect M3's actual fusion state.

---

# 50. THRESHOLD FEEDBACK

When fusion reaches the threshold:

```text
Fusion threshold reached
```

The UI should clearly transition into:

```text
Proximity confirmation
```

This makes the two-stage mechanic understandable.

---

# 51. PROXIMITY FEEDBACK

If fusion is ready but proximity is not:

```text
Almost there

Bring the phone closer to complete the scan.
```

If proximity passes:

```text
Confirmed
```

then reveal.

---

# 52. SUCCESS FEEDBACK

A successful scan should be unmistakable.

Possible sequence:

```text
SUCCESS
   ↓
Discovery recorded
   ↓
Reveal
```

The reveal should feel like the completion of the physical interaction, not simply a button click.

---

# 53. FAILURE FEEDBACK

A failed scan should tell the player what to do.

Avoid:

```text
Scan failed.
```

Prefer a state-specific instruction such as:

```text
Signals are not aligned.
Move closer and continue the sweep.
```

when supported by the available state.

---

# 54. VIEWMODEL EVENTS

Avoid using one-time UI events in a way that causes duplicate navigation after recreation.

Prefer a state-driven approach where practical:

```text
scanState = REVEALING
```

and guard the transition.

The exact event architecture should follow the project's existing MVVM implementation.

---

# 55. UNIT TESTS — STATE MACHINE

Test:

```text
IDLE → READY_TO_SCAN
READY_TO_SCAN → SCANNING
SCANNING → FUSION_READY
FUSION_READY → WAITING_FOR_PROXIMITY
WAITING_FOR_PROXIMITY → SUCCESS
SUCCESS → REVEALING
REVEALING → COMPLETED
```

Also test invalid transitions.

---

# 56. UNIT TEST — FUSION THRESHOLD

Given:

```text
fusionThresholdReached = false
```

the UI must not enter the proximity confirmation state.

Given:

```text
fusionThresholdReached = true
```

the UI may enter the proximity state.

---

# 57. UNIT TEST — FINAL GATE

Given:

```text
fusionThresholdReached = true
proximity = FAR
```

expect:

```text
WAITING_FOR_PROXIMITY
```

Given:

```text
fusionThresholdReached = true
proximity = NEAR
```

expect:

```text
SUCCESS
```

---

# 58. UNIT TEST — NO PROXIMITY WEIGHT

Verify M4 does not independently modify the fusion score based on proximity.

The displayed fusion value must equal the M3-provided value.

---

# 59. UNIT TEST — DISCOVERY ONCE

Trigger:

```text
finalGatePassed = true
```

multiple times.

Verify:

```text
recordDiscovery()
```

is requested only once for the active checkpoint.

---

# 60. UNIT TEST — CHECKPOINT ISOLATION

Switch:

```text
Game A / CP1
```

to:

```text
Game B / CP1
```

and verify state is reinitialized for the new game/checkpoint.

---

# 61. UNIT TEST — RETRY

Verify:

```text
ERROR
 ↓
RETRY
 ↓
SCANNING
```

without creating duplicate discovery records.

---

# 62. UI TESTS

Test:

- Checkpoint name appears.
- Fusion meter displays current score.
- GPS state appears.
- Light state appears.
- Motion state appears.
- Proximity state appears at the correct phase.
- Reveal is hidden before final gate.
- Reveal appears after successful gate.
- Error states are visible.
- Retry works.
- Continue/next action works.

---

# 63. ACCEPTANCE TEST — SCAN START

### Given

Player has entered the eligible checkpoint area.

### When

Scan mode starts.

### Then

The scan HUD shows the current checkpoint and real M3 signal states.

---

# 64. ACCEPTANCE TEST — FUSION

### Given

GPS, light and motion signals are being collected.

### When

M3 reports an increasing fusion score.

### Then

M4's fusion meter reflects the reported score.

The UI must not generate artificial progress.

---

# 65. ACCEPTANCE TEST — THRESHOLD

### Given

Fusion score reaches the configured threshold.

### Then

M4 changes from signal collection to proximity confirmation.

No reveal should occur yet unless the final gate has passed.

---

# 66. ACCEPTANCE TEST — PROXIMITY

### Given

Fusion threshold has been reached.

### When

M3 reports:

```text
proximity = NEAR
```

### Then

M4 transitions to successful discovery/reveal.

---

# 67. ACCEPTANCE TEST — PROXIMITY BLOCK

### Given

Fusion threshold has been reached.

### When

M3 reports:

```text
proximity = FAR
```

### Then

Reveal remains blocked.

---

# 68. ACCEPTANCE TEST — DISCOVERY RECORD

### Given

Final gate passes.

### Then

M4 requests:

```kotlin
recordDiscovery(gameId, checkpointId, foundAt)
```

only once for that discovery.

---

# 69. ACCEPTANCE TEST — OFFLINE

### Given

The application is offline.

### When

A valid discovery occurs.

### Then

M4 follows the shared offline contract and presents the appropriate local-save/sync state.

M6 owns the actual synchronization.

---

# 70. ACCEPTANCE TEST — DYNAMIC GAME

### Given

A creator publishes a game containing a newly configured checkpoint.

### When

A player reaches that checkpoint.

### Then

M4 can display and run the checkpoint using its dynamic configuration.

No code change or hard-coded checkpoint ID should be required.

---

# 71. ACCEPTANCE TEST — GAME ISOLATION

### Given

A player participates in two games.

### When

The player scans a checkpoint in Game A.

### Then

Game B's scan/progress state remains unaffected.

---

# 72. ACCEPTANCE TEST — FINAL CHECKPOINT

### Given

The player discovers the final checkpoint.

### Then

The UI presents the game-completion state rather than attempting to navigate to a nonexistent checkpoint.

---

# 73. INTEGRATION WITH M3

M4 requires:

```text
DiscoverySignalState
```

from M3.

Integration test:

```text
Fake/real location
      ↓
M3 fusion
      ↓
M4 HUD
      ↓
threshold
      ↓
proximity
      ↓
reveal
```

---

# 74. INTEGRATION WITH M5

M4 requires repository support for:

```kotlin
recordDiscovery(
    gameId,
    checkpointId,
    foundAt
)
```

M5 owns Firestore implementation and cloud-side progress/leaderboard behavior.

---

# 75. INTEGRATION WITH M6

M4 requires local progress support where offline operation is part of the final contract.

M6 owns:

- Room.
- Local discovery persistence.
- Pending sync.
- Idempotency.
- Retry.

M4 only consumes the repository/use-case result.

---

# 76. INTEGRATION WITH M1

M1 owns the player navigation shell and leaderboard UI.

M4 hands off:

- Game/checkpoint routes.
- Completion state.
- Reveal navigation.
- Next-checkpoint navigation requirements.

---

# 77. REPOSITORY BOUNDARY

M4 should use the shared repository contract.

Relevant operations include:

```kotlin
getGameDetails(gameId)
getGameCheckpoints(gameId)
recordDiscovery(gameId, checkpointId, foundAt)
```

and the M3 signal provider/use-case.

M4 must not directly instantiate Firebase or Room.

---

# 78. SUGGESTED PACKAGE STRUCTURE

```text
quest/
    QuestScreen.kt
    QuestScanViewModel.kt
    ScanState.kt
    ScanUiState.kt
    ScanComponents.kt
    RevealDialog.kt
    ScanNavigation.kt
```

Adapt to the project's existing XML/Compose architecture rather than introducing a second UI technology unnecessarily.

---

# 79. MOCK DATA STRATEGY

M4 may use seed/demo data during development:

```text
demo-campus-quest
R001–R006
```

but must test dynamic behavior with arbitrary IDs.

Recommended test IDs:

```text
game-alpha
cp-alpha-001

game-beta
cp-beta-001
```

This prevents accidental dependence on sample relic IDs.

---

# 80. MOCK SIGNAL STRATEGY

For UI development, create fake M3 states:

```text
fusion 0%
fusion 25%
fusion 50%
fusion 75%
fusion threshold reached
proximity far
proximity near
success
error
```

This allows M4 to build the UI without waiting for physical sensor integration.

---

# 81. MOCK SCAN STATE SCENARIOS

At minimum:

```text
Scenario 1: Location unavailable
Scenario 2: Outside checkpoint
Scenario 3: Scanning
Scenario 4: Fusion increasing
Scenario 5: Fusion threshold reached
Scenario 6: Waiting for proximity
Scenario 7: Proximity confirmed
Scenario 8: Reveal
Scenario 9: Discovery saved
Scenario 10: Error + retry
Scenario 11: Final checkpoint
```

---

# 82. PERFORMANCE

The UI should not recompose/re-render excessively because sensor updates may arrive frequently.

Use appropriate state collection and derive only the values required by the current screen.

Avoid heavy computation in composables/UI callbacks.

---

# 83. BATTERY

M4 itself should not keep sensors running.

When the scan screen becomes inactive, it must tell the M3 layer that active scanning is no longer required through the agreed lifecycle contract.

---

# 84. SECURITY

M4 should not trust the UI's own success state as authoritative.

The discovery record must go through the repository/backend rules.

For example:

```text
UI says SUCCESS
        ↓
recordDiscovery
        ↓
M5/M6 persistence/security
```

The backend may reject an invalid or unauthorized record.

M4 must handle that rejection.

---

# 85. ERROR AFTER SUCCESS

If physical confirmation succeeds but persistence fails:

```text
Discovery confirmed
Saving progress...
```

then:

```text
Saved locally / pending sync
```

or the documented error state.

Do not falsely claim cloud persistence if it has not been confirmed.

---

# 86. REVEAL CONTENT SAFETY

The reveal should use the checkpoint data already authorized for the current player/game.

Do not load arbitrary checkpoint data based solely on a user-provided ID without repository/access validation.

---

# 87. ACCESS CONTROL

A player must only be able to run gameplay for a game they are authorized to play according to the repository/backend rules.

M4 handles unauthorized responses gracefully.

Example:

```text
You can no longer access this game.
```

---

# 88. CLOSED GAME

If the game becomes:

```text
CLOSED
```

while the player is attempting to continue:

```text
Scan unavailable
This game is no longer active.
```

The exact lifecycle behavior follows the shared product contract.

---

# 89. PUBLISHED GAME CHANGES

M4 should not assume checkpoint configuration is immutable unless the product lifecycle specifies it.

If creators can modify published games, M4 must consume the repository's current valid configuration.

If published games are locked, the UI should follow the locked-game contract.

---

# 90. NO HARDCODED QUEST SEQUENCE

Do not write:

```kotlin
when (checkpointId) {
    "R001" -> ...
    "R002" -> ...
}
```

for gameplay behavior.

Gameplay must use:

```text
gameId
checkpointId
order
checkpoint configuration
```

from the dynamic data model.

---

# 91. GIT WORKFLOW

Branch:

```text
feature/m4-quest-scan-gameplay
```

Commit examples:

```text
feat(quest): add checkpoint scan screen
feat(quest): add scan state machine
feat(quest): add fusion meter
feat(quest): add sensor status indicators
feat(quest): add proximity gate UI
feat(quest): add discovery reveal
feat(quest): add discovery recording flow
test(quest): add scan state tests
test(quest): add reveal transition tests
```

Avoid mixing M3 sensor implementation or M5/M6 persistence implementation into M4 commits.

---

# 92. CHANGE CONTROL

Before changing:

- Scan state machine.
- Fusion threshold presentation.
- Proximity gate behavior.
- Discovery transition.
- Repository discovery contract.
- Navigation contract.

inform the relevant member and update shared documentation.

The physical mechanic must remain consistent across M3 and M4.

---

# 93. DEFINITION OF DONE

M4 is complete when:

### Gameplay

- Player can enter checkpoint scan mode.
- Checkpoint identity is correct.
- Scan state is explicit.
- GPS/light/motion states are displayed.
- Fusion meter reflects M3's actual score.
- Fusion threshold is represented correctly.
- Proximity final gate is represented separately.
- Reveal occurs only after final gate.

### Discovery

- Discovery is recorded through the repository boundary.
- Duplicate recording is prevented.
- Game/checkpoint IDs are correct.
- Offline behavior follows the shared contract.
- Final checkpoint completes the game appropriately.

### Architecture

- ViewModel owns scan UI state.
- M3 owns hardware/fusion logic.
- M4 does not access sensor SDKs directly.
- M4 does not access Firebase/Room directly.
- Dynamic checkpoint IDs are supported.

### Testing

- State-machine tests pass.
- Fusion presentation tests pass.
- Proximity gate tests pass.
- Reveal tests pass.
- Duplicate-discovery tests pass.
- Dynamic/multi-game tests pass.
- Error/retry tests pass.

---

# 94. LEGACY CLEANUP

Remove or isolate code that assumes:

```text
Relic
FoundRelic
R001–R006
```

are the permanent gameplay entities.

The production gameplay screen must consume:

```text
Game
   ↓
Checkpoint
   ↓
Checkpoint configuration
```

R001–R006 remain valid only as sample/seed data.

---

# 95. FINAL M4 ARCHITECTURE

The complete M4 path is:

```text
Game / Checkpoint
      ↓
QuestScanViewModel
      ↓
M3 DiscoverySignalState
      ↓
┌─────────────────────────────┐
│ Scan UI                     │
│                             │
│ GPS status                  │
│ Light status                │
│ Motion status               │
│ Fusion meter                │
│ Fusion threshold            │
│ Proximity final gate        │
│ Success                     │
│ Reveal                      │
└─────────────────────────────┘
      ↓
recordDiscovery()
      ↓
M6 local persistence
      ↓
M5 cloud sync / leaderboard
      ↓
Next checkpoint / completion
```

---

# 96. CORE DESIGN PRINCIPLE

M4 is the **player's physical-discovery experience layer**.

It does not decide whether the raw sensors are trustworthy and it does not implement the sensors.

M3 supplies:

```text
GPS evidence
Light evidence
Motion evidence
Fusion result
Proximity state
Final gate
```

M4 turns those results into:

```text
Scan
   ↓
Feedback
   ↓
Threshold
   ↓
Proximity confirmation
   ↓
Reveal
   ↓
Discovery
```

The most important rule is:

```text
Fusion threshold ≠ discovery
```

Discovery requires:

```text
Fusion threshold
+
Proximity final gate
```

---

# 97. M4 QUICK CHECKLIST

```text
[ ] Checkpoint gameplay screen
[ ] Scan HUD
[ ] Scan state machine
[ ] GPS status
[ ] Light status
[ ] Motion status
[ ] Fusion meter
[ ] Fusion threshold state
[ ] Proximity phase
[ ] Proximity final-gate state
[ ] Success state
[ ] Reveal UI
[ ] Clue display
[ ] Lore display
[ ] Rarity display
[ ] Discovery recording
[ ] Duplicate-discovery protection
[ ] Retry
[ ] Error states
[ ] Offline behavior
[ ] Next checkpoint
[ ] Final checkpoint completion
[ ] Dynamic checkpoint support
[ ] Multi-game isolation
[ ] ViewModel tests
[ ] State-machine tests
[ ] UI tests
[ ] M3 integration
[ ] M5/M6 integration
[ ] M1 navigation integration
```

---

# 98. FINAL HANDOFF PACKAGE

M4 should provide:

1. Quest/checkpoint gameplay screen.
2. Scan HUD.
3. Scan state machine.
4. Scan ViewModel.
5. Fusion-meter presentation.
6. Sensor-status presentation.
7. Proximity-gate presentation.
8. Reveal UI.
9. Discovery-recording integration.
10. Navigation contracts.
11. Mock signal states.
12. Unit tests.
13. UI tests.
14. Integration test notes.
15. Error-state mapping.
16. Any shared-contract changes.

---

**END OF MEMBER 4 QUEST & SCAN GAMEPLAY WORKPLAN**
