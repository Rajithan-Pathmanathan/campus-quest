# Campus Quest — M2 Workplan: Quest & Scan Experience

**Document ID:** CQ-M2  
**Role:** M2 — Frontend Developer: Quest + Scan Experience  
**Project:** Campus Quest  
**Development window:** 13 September 2026 – 28 September 2026  
**Primary responsibility:** Quest discovery UI, quest details, Scan Mode UI, fusion-meter presentation, scan instructions, relic reveal, lore/progress presentation, and leaderboard presentation where assigned  
**Primary dependencies:** M1 navigation/theme, M3 location state, M4 sensor-fusion result, M5/M6 relic/progress data

---

# 1. Role Objective

M2 owns the part of Campus Quest that turns the technical location/sensor system into the actual game experience.

The target user experience is:

```text
Quest List
 ↓
Select Relic
 ↓
Quest Details
 ↓
Approach Location
 ↓
Geofence ENTER
 ↓
Scan Mode
 ↓
Fusion Meter
 ↓
Proximity Confirmation
 ↓
Relic Reveal
 ↓
Lore
 ↓
Progress
```

M2 is responsible for presenting this experience.

M2 does **not** own the underlying GPS, geofence, sensor, Room, or Firebase implementation.

---

# 2. Primary Deliverables

M2 must deliver:

- Quest list
- Quest cards
- Quest details screen
- Relic information presentation
- Scan Mode screen
- Scan instructions
- Fusion progress/meter
- Scan state presentation
- Sensor limitation messaging
- Proximity confirmation state
- Successful reveal screen
- Relic/lore presentation
- Discovery success state
- Progress presentation
- Leaderboard presentation where assigned
- Mock-based versions of all screens
- Integration with real M3/M4/M5/M6 outputs

---

# 3. Ownership Boundary

M2 owns:

```text
Quest UI
Quest details
Scan UI
Fusion meter display
Scan instructions
Reveal UI
Lore
Discovery presentation
Progress presentation
Leaderboard presentation
```

M2 consumes:

```text
LocationResult / DistanceResult → M3
GeofenceEvent → M3
FusionResult → M4
ScanState → M4
Relic/light signature → M5/M6
Saved progress → M6/M5
```

---

# 4. Non-Ownership Boundary

M2 must not independently implement:

```text
GPS
Fused Location Provider
geofence registration
SensorManager
accelerometer processing
light sensor processing
proximity sensor detection
fusion mathematics
Room DAO
Firestore writes
Firebase authentication
```

If the UI needs one of these values, consume the agreed interface/state.

---

# 5. Architecture

Use:

```text
UI
 ↓
ViewModel
 ↓
Repository
```

for application data.

M2 should not place direct:

```text
Firestore
Room
sensor
location
```

operations inside the UI layer.

---

# 6. M2 Screen Map

```text
Quests
 ↓
Quest Details
 ↓
Scan
 ↓
Reveal
 ↓
Lore / Discovery
```

Additional:

```text
Quests → already discovered state
Quests → unavailable/locked state
Quests → completed state
```

---

# 7. Quest List

The Quest List should display the available relic quests.

For each quest, consider showing:

```text
Relic name
Rarity
Short description
Discovery state
Distance/status where available
```

Do not expose unnecessary technical sensor details on the main quest card.

---

# 8. Quest Card

A quest card should have:

```text
Title
Rarity
Short description
Status
Action
```

Example:

```text
Founder’s Bell
COMMON

A relic hidden within the historic campus grounds.

[Explore]
```

The exact wording can follow the final UI design.

---

# 9. Quest States

At minimum support:

```text
Available
Approaching
Ready to scan
Scanning
Discovered
```

Potential additional states:

```text
Locked
Unavailable
Limited
Error
```

Only display states that correspond to actual application state.

---

# 10. Quest Details

Quest Details should provide enough information for the user to understand the objective without revealing the entire solution.

Possible content:

```text
Relic name
Rarity
Lore preview
Discovery objective
Location status
Start/Explore action
```

Do not reveal the final relic before the user completes the scan.

---

# 11. R001 Primary Test Quest

Use:

```text
R001 — Founder’s Bell
```

for the primary UI/integration scenario.

Canonical development data:

```text
Radius: 25 m
Light signature: 180–320 lux
Rarity: Common
```

Coordinates come from the shared mock catalog and must be physically validated before final demonstration.

---

# 12. Additional Test Quests

Use:

```text
R002 — Scholar’s Compass
R003 — Heritage Key
R004 — Old Library Seal
R005 — Garden Chronicle
R006 — Clock Tower Relic
```

Do not create alternate IDs for these canonical development relics.

---

# 13. Quest List Mock Data

Before M5/M6 are ready, M2 should use a mock repository.

Expected mock result:

```text
R001
R002
R003
R004
R005
R006
```

The UI must not depend on Firebase being available during initial development.

---

# 14. Quest ViewModel

A conceptual ViewModel may expose:

```text
quests
loading
error
selectedQuest
```

Example conceptual state:

```kotlin
data class QuestListUiState(
    val isLoading: Boolean = false,
    val quests: List<Relic> = emptyList(),
    val errorMessage: String? = null
)
```

The exact implementation may differ.

---

# 15. Quest Loading State

When loading:

```text
show loading indicator/skeleton
```

Do not show a blank screen without explanation.

---

# 16. Quest Error State

If data cannot be loaded:

```text
Unable to load quests.
Try again.
```

Provide retry where appropriate.

Do not expose raw exceptions.

---

# 17. Quest Empty State

If there are no quests:

```text
No quests available.
```

Optionally provide:

```text
Refresh
```

if supported by the repository.

---

# 18. Quest Detail Navigation

Expected:

```text
Quest List
 ↓
tap R001
 ↓
Quest Details
```

The selected relic ID must be passed reliably.

Do not use:

```text
array position
```

as the long-term identity of a relic.

Use:

```text
relicId
```

such as:

```text
R001
```

---

# 19. Scan Entry

Scan Mode should become available when the location/geofence conditions allow it according to the integrated design.

M3 owns the location/geofence event.

M2 owns how that state is presented.

---

# 20. Geofence Boundary

Important:

```text
Geofence ENTER
```

does not automatically mean:

```text
Relic revealed
```

It means the user can begin the scan experience.

---

# 21. Scan Screen Objective

The Scan screen should make the user understand:

```text
What is happening?
What should I do?
How close am I?
How strong is the match?
What is preventing completion?
```

---

# 22. Scan UI Structure

Recommended conceptual structure:

```text
Top bar
 ↓
Relic title
 ↓
Instruction
 ↓
Fusion meter
 ↓
Sensor/status indicators
 ↓
Dynamic guidance
 ↓
Proximity confirmation
```

The final visual arrangement follows the approved UI design.

---

# 23. Fusion Meter

The central interaction should be a live:

```text
0–100%
```

score.

Example:

```text
MATCH
87%
```

The meter is a visual representation of `FusionResult`.

M2 must not independently calculate the percentage.

---

# 24. Fusion Architecture

The actual signal flow is:

```text
GPS
 +
Light
 +
Accelerometer
 ↓
Fusion Engine
 ↓
FusionResult
 ↓
M2 Scan UI
```

Proximity is separate:

```text
Fusion threshold reached
 ↓
Proximity check
 ↓
Reveal
```

---

# 25. Important Fusion Rule

M2 should never implement:

```text
GPS weight
+
light weight
+
accelerometer weight
```

inside the UI.

M4 owns that calculation.

M2 only displays:

```text
score
state
available signals
guidance
```

---

# 26. FusionResult

The shared conceptual result may contain:

```text
score
location contribution
light contribution
motion contribution
available signals
threshold state
```

The final Kotlin model may differ if the team agrees.

---

# 27. Scan States

The shared specification proposes states such as:

```text
NOT_AVAILABLE
READY
SCANNING
READY_FOR_PROXIMITY
REVEALED
LIMITED
ERROR
```

M2 should map these states into user-facing UI.

---

# 28. NOT_AVAILABLE

Display:

```text
Scanning is currently unavailable.
```

Possible reasons:

```text
location unavailable
required capability unavailable
relic not ready
```

Use the actual reason where available.

---

# 29. READY

Display:

```text
Ready to scan
```

and explain the first action.

---

# 30. SCANNING

Display:

```text
live fusion score
instruction
sensor status
```

Example:

```text
Scanning...
72%

Move slowly around the area.
```

---

# 31. READY_FOR_PROXIMITY

Display:

```text
Signal match found.
Move closer to reveal.
```

This state is important because it explains why a high fusion score has not yet produced the reveal.

---

# 32. REVEALED

Display:

```text
Relic discovered
```

then transition to the relic reveal/lore experience.

---

# 33. LIMITED

Display:

```text
Limited sensor mode
```

Explain the experience without making the user believe all signals are available.

Example:

```text
Some sensors are unavailable.
The scan is using the available signals.
```

Exact wording may change.

---

# 34. ERROR

Display:

```text
Something went wrong.
Try again.
```

where retry is meaningful.

---

# 35. Sensor Status

M4 may provide sensor availability.

M2 may display:

```text
Location ✓
Light ✓
Motion ✓
```

or an equivalent visual representation.

Avoid showing unnecessary technical jargon such as:

```text
TYPE_LIGHT
TYPE_ACCELEROMETER
```

to normal users.

---

# 36. Missing Light Sensor

If light is unavailable:

```text
do not display fake light data
```

Instead:

```text
show limited mode
```

if the fusion engine supports degraded operation.

---

# 37. Missing Accelerometer

If accelerometer is unavailable:

```text
do not display fake motion data
```

Show the appropriate limited state.

---

# 38. Missing Proximity Sensor

Proximity is a final gate.

If unavailable, M2 should display the agreed unsupported/limited state.

Do not pretend:

```text
near
```

when no proximity measurement exists.

---

# 39. Distance Presentation

M3 may provide:

```text
DistanceResult
```

M2 can display user-friendly status such as:

```text
25 m away
Approaching
Within scan area
```

Do not display false precision.

If accuracy is poor, show an appropriate low-confidence state.

---

# 40. Scan Instructions

Instructions should change with state.

Example:

### Far

```text
Move toward the relic area.
```

### Geofence entered

```text
You're close. Begin scanning.
```

### Low fusion

```text
Move slowly around the area.
```

### High fusion

```text
Strong signal. Keep scanning.
```

### Threshold reached

```text
Signal matched. Move closer.
```

### Proximity near

```text
Relic confirmed.
```

---

# 41. Avoid Overly Complex Instructions

The user should not need to understand:

```text
sensor fusion
weighted averages
lux values
accelerometer vectors
```

The technical implementation can remain invisible.

---

# 42. Scan Animation

Animation can be used to make the scan feel active.

Examples:

```text
meter animation
subtle pulse
scan indicator
```

Avoid animation that:

```text
drains battery
blocks interaction
causes frame drops
```

---

# 43. Scan Meter Updates

The meter should respond smoothly to changing `FusionResult`.

Avoid:

```text
87 → 22 → 94
```

visually jumping wildly if the underlying signal changes rapidly.

M4 may smooth the result; M2 should not secretly implement a second fusion algorithm.

If UI smoothing is required, it should be purely presentational and agreed with M4.

---

# 44. Threshold Display

The UI does not necessarily need to display the exact threshold.

A user-facing state is usually clearer:

```text
Keep scanning
```

rather than:

```text
87/85 threshold
```

The exact threshold should remain a technical configuration.

---

# 45. Proximity Gate

When:

```text
FusionResult >= threshold
```

M2 should show:

```text
Ready for final confirmation
```

Then wait for:

```text
Proximity = NEAR
```

---

# 46. Proximity Must Not Be Part of Fusion Meter

Do not display:

```text
GPS 30%
Light 30%
Motion 30%
Proximity 10%
```

if the agreed architecture defines proximity as a separate final gate.

The meter represents:

```text
GPS + Light + Accelerometer
```

only.

---

# 47. Successful Scan Example

Primary test:

```text
R001
distance = 18 m
light = 250 lux
motion = SCANNING
fusion = 87
proximity = NEAR
```

Expected:

```text
Scan success
 ↓
Reveal
```

---

# 48. Failed Scan Example

```text
R001
distance = 18 m
light = 500 lux
motion = STATIONARY
```

Expected:

```text
Low/insufficient fusion
No reveal
```

---

# 49. Proximity Failure Example

```text
fusion = 87
proximity = FAR
```

Expected:

```text
No reveal
```

UI should explain:

```text
Move closer.
```

---

# 50. Fusion Failure Example

```text
fusion = 55
proximity = NEAR
```

Expected:

```text
No reveal
```

UI should explain:

```text
Continue scanning.
```

---

# 51. Reveal Screen

The reveal should feel like the reward for completing the interaction.

Suggested structure:

```text
Discovery success
 ↓
Relic image/icon
 ↓
Relic name
 ↓
Rarity
 ↓
Lore
 ↓
Discovery time/progress
 ↓
Continue
```

Exact design follows the approved UI.

---

# 52. Lore

Each relic should have narrative/lore content.

Example:

```text
Founder’s Bell
```

with a short campus-history-style description.

Do not make lore dependent on the scan algorithm.

Lore comes from relic data/content.

---

# 53. Rarity

Canonical rarity values:

```text
Common
Uncommon
Rare
```

Use the relic dataset.

Do not independently redefine rarity rules in the UI.

---

# 54. Discovery Confirmation

The user should receive a clear confirmation:

```text
Relic discovered!
```

The confirmation must not appear merely because the user entered a geofence.

---

# 55. Duplicate Discovery

If R001 is already discovered:

```text
show discovered state
```

Do not create another reward event.

---

# 56. Already Discovered Quest

Quest card may display:

```text
Discovered
```

and optionally:

```text
View Lore
```

Do not require the user to repeat the scan unnecessarily unless the final game design explicitly calls for replay.

---

# 57. Progress

M2 presents progress such as:

```text
3 / 6 relics discovered
```

The source should be the agreed repository/local-cloud state.

M2 should not calculate progress from UI state alone.

---

# 58. Leaderboard

Where M2 owns leaderboard presentation, display:

```text
Rank
Player
Relics found
```

Example:

```text
1  Sahan Fernando   5
2  Kavindi Silva   4
3  Nimal Perera    3
```

Use shared cloud/local data.

---

# 59. Leaderboard Mock Data

Canonical development example:

```text
U003 = 5
U002 = 4
U001 = 3
U004 = 2
U005 = 0
```

Expected order:

```text
U003
U002
U001
U004
U005
```

---

# 60. Leaderboard Loading

Display loading state while data is being obtained.

---

# 61. Leaderboard Error

If unavailable:

```text
Unable to load leaderboard.
Try again.
```

If cached data is available, the final design may show it with an offline indicator.

---

# 62. M2 ViewModels

Potential ViewModels:

```text
QuestListViewModel
QuestDetailsViewModel
ScanViewModel
RevealViewModel
LeaderboardViewModel
```

Only create the ones required by the final architecture.

---

# 63. ScanViewModel Responsibilities

The ScanViewModel may:

```text
observe FusionResult
observe ScanState
observe location status
observe proximity state
trigger scan lifecycle
request reveal through repository
```

It should not implement the sensor mathematics.

---

# 64. ScanViewModel State

Conceptual:

```kotlin
data class ScanUiState(
    val relic: Relic? = null,
    val fusionScore: Int = 0,
    val scanState: ScanState = ScanState.NOT_AVAILABLE,
    val proximity: Proximity = Proximity.UNKNOWN,
    val errorMessage: String? = null
)
```

This is illustrative; use the final shared model agreed by M2/M4.

---

# 65. Scan Lifecycle

When entering Scan:

```text
start observing scan state
```

When leaving Scan:

```text
stop observation/cleanup
```

M4 owns sensor listener lifecycle.

M2 should not leave UI observers running unnecessarily.

---

# 66. Screen Lifecycle

Test:

```text
Scan
 ↓
background
 ↓
foreground
```

Expected:

```text
state remains coherent
no duplicate observers
no duplicate reveal
```

---

# 67. Navigation During Scan

If the user leaves Scan:

```text
stop or pause according to final design
```

Do not leave a hidden Scan process consuming resources indefinitely.

Coordinate with M4.

---

# 68. Reveal Idempotency

If the reveal action is triggered twice:

```text
record one discovery
```

The UI should prevent accidental repeated submissions.

Use:

```text
isRevealing
isRevealed
```

or equivalent state.

---

# 69. Reveal Loading

After a successful scan condition but before persistence completes, the UI may show:

```text
Saving discovery...
```

The exact behaviour depends on the local-first repository contract.

---

# 70. Local-First Interaction

The intended flow is:

```text
scan succeeds
 ↓
record locally
 ↓
mark pending if cloud sync unavailable
 ↓
sync
```

The user should not lose the discovery merely because Firebase is temporarily unavailable.

---

# 71. Offline Reveal

If:

```text
Internet OFF
```

and the scan succeeds:

```text
show successful discovery
```

provided local persistence succeeds.

The cloud synchronization can occur later.

---

# 72. Offline UI

After an offline discovery, optionally communicate:

```text
Saved on this device.
Will sync when you're online.
```

This should reflect the actual M6 state.

---

# 73. Firebase Failure

If cloud write fails after local save:

```text
do not show "discovery failed"
```

if the local-first contract considers the discovery successful.

Instead show an appropriate sync state.

---

# 74. Data Boundary

M2 should consume:

```text
QuestRepository
```

rather than directly using:

```text
Firestore
Room
```

---

# 75. Repository Operations

Relevant conceptual operations include:

```kotlin
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
```

The final shared contract may contain additional fields or differ slightly after team agreement.

---

# 76. Mock Repository

M2 should have a mock repository that can return:

```text
six relics
success
empty
error
offline
already discovered
```

---

# 77. Mock Fusion Provider

During early UI development, use fixed results:

```text
20%
45%
72%
87%
95%
```

and states:

```text
READY
SCANNING
READY_FOR_PROXIMITY
REVEALED
LIMITED
ERROR
```

This allows the complete UI to be built before M4 is integrated.

---

# 78. Mock Scan Scenario

Primary:

```text
R001
fusion = 87
proximity = NEAR
```

Expected:

```text
Reveal
```

---

# 79. Mock Blocked Scenario

```text
R001
fusion = 87
proximity = FAR
```

Expected:

```text
Ready for proximity
```

---

# 80. Mock Low-Fusion Scenario

```text
R001
fusion = 55
proximity = NEAR
```

Expected:

```text
Continue scanning
```

---

# 81. Mock Limited Scenario

```text
light unavailable
accelerometer available
GPS available
```

Expected:

```text
Limited sensor mode
```

and the UI should render the degraded result supplied by M4.

---

# 82. Quest UI Test Cases

## M2-001 — Quest List Loads

Expected:

```text
R001–R006 displayed
```

---

# 83. M2-002 — Quest List Loading

Expected:

```text
loading indicator
```

---

# 84. M2-003 — Quest List Error

Expected:

```text
error state
retry
```

where supported.

---

# 85. M2-004 — Quest List Empty

Expected:

```text
empty state
```

---

# 86. M2-005 — Select R001

Expected:

```text
Quest Details for Founder’s Bell
```

---

# 87. M2-006 — Already Discovered

Expected:

```text
Discovered state
```

---

# 88. Scan UI Test Cases

## M2-010 — Scan Ready

Expected:

```text
Ready to scan
```

---

# 89. M2-011 — Scan Progress

Input:

```text
20 → 45 → 72 → 87
```

Expected:

```text
meter updates
```

---

# 90. M2-012 — Ready for Proximity

Input:

```text
fusion >= threshold
proximity = FAR
```

Expected:

```text
Move closer
```

---

# 91. M2-013 — Successful Reveal

Input:

```text
fusion >= threshold
proximity = NEAR
```

Expected:

```text
Reveal
```

---

# 92. M2-014 — Low Fusion

Input:

```text
fusion below threshold
proximity = NEAR
```

Expected:

```text
Continue scanning
```

---

# 93. M2-015 — Limited Sensors

Input:

```text
one sensor unavailable
```

Expected:

```text
limited state rendered
```

---

# 94. M2-016 — Sensor Error

Expected:

```text
error/limited UI
```

No crash.

---

# 95. M2-017 — Leave Scan

Expected:

```text
no duplicated observer
no unexpected reveal
```

---

# 96. M2-018 — Re-enter Scan

Expected:

```text
correct current state
```

---

# 97. Reveal Test Cases

## M2-020 — Reveal Content

Expected:

```text
name
rarity
lore
```

---

# 98. M2-021 — Duplicate Reveal

Expected:

```text
one discovery
```

---

# 99. M2-022 — Offline Reveal

Expected:

```text
local success
pending sync
```

where repository state supports it.

---

# 100. M2-023 — Reveal Loading

Expected:

```text
saving state
```

if required.

---

# 101. Leaderboard Test Cases

## M2-030

Load leaderboard.

Expected:

```text
correct ordering
```

---

# 102. M2-031

Use:

```text
5,4,3,2,0
```

Expected descending order.

---

# 103. M2-032

Cloud unavailable.

Expected:

```text
error or cached state
```

according to repository state.

---

# 104. M2-033

After discovery count increases.

Expected:

```text
new count displayed after data refresh
```

---

# 105. Full M2 UI Flow

Run:

```text
Quests
 ↓
R001
 ↓
Details
 ↓
Scan
 ↓
Fusion 20%
 ↓
Fusion 45%
 ↓
Fusion 72%
 ↓
Fusion 87%
 ↓
Proximity FAR
 ↓
"Move closer"
 ↓
Proximity NEAR
 ↓
Reveal
 ↓
Lore
 ↓
Progress
```

---

# 106. Integration with M3

M3 provides location/geofence state.

M2 should consume:

```text
distance
accuracy/status
geofence event
```

Expected UI:

```text
Far
Approaching
Within area
Ready to scan
```

---

# 107. Integration with M4

M4 provides:

```text
FusionResult
ScanState
SensorAvailability
Proximity
```

M2 maps these to:

```text
meter
instructions
status
reveal transition
```

---

# 108. Integration with M5

M5 provides:

```text
relic data
authentication state
cloud data
```

M2 consumes through the repository boundary.

---

# 109. Integration with M6

M6 provides:

```text
local discovery state
offline state
pending sync state
```

M2 displays appropriate progress/sync status.

---

# 110. M2 Handoff to M1

M2 should provide:

```text
Quest destination
Quest Details destination
Scan destination
Reveal destination
```

and tell M1:

```text
required navigation arguments
```

Example:

```text
relicId = R001
```

---

# 111. M2 Handoff to M4

M2 should communicate:

```text
required FusionResult fields
required ScanState values
required proximity state
```

M4 should not need to know the visual implementation.

---

# 112. M2 Handoff to M6

M2 communicates:

```text
when recordReveal() is called
what relic ID is required
what UI state follows local success
```

M6 handles persistence.

---

# 113. M2 Handoff to M5

M2 may require:

```text
relic list
light signature
user information
leaderboard
```

M5 supplies these through the agreed repository/cloud boundary.

---

# 114. M2 Git Branch

Recommended:

```text
feature/quest-scan-ui
```

---

# 115. M2 Commit Examples

```text
feat: add quest list
feat: add quest details
feat: add scan screen
feat: render fusion meter
feat: add relic reveal
fix: prevent duplicate reveal
fix: handle limited scan state
test: add scan ui state tests
```

---

# 116. M2 PR Checklist

- [ ] Quest list works
- [ ] Quest details works
- [ ] Scan UI works with mock results
- [ ] Reveal works with mock results
- [ ] Fusion score is not recalculated in UI
- [ ] No direct Firebase calls
- [ ] No direct Room calls
- [ ] Navigation contract documented
- [ ] Error/loading states included
- [ ] Relevant tests pass
- [ ] Screenshots/video attached where useful

---

# 117. M2 Daily Schedule — 13 September

Start:

```text
Quest UI structure
Scan UI skeleton
```

Use mock data.

---

# 118. 14 September

Implement:

```text
Quest List
Quest Card
Quest Details
```

---

# 119. 15 September

Implement:

```text
Scan Screen
Fusion Meter
Scan Instructions
```

using mock FusionResult.

---

# 120. 16 September

Implement:

```text
Scan states
limited mode
error state
proximity state
```

---

# 121. 17 September

Implement:

```text
Reveal
Lore
Progress
```

---

# 122. 18 September

Integrate:

```text
M1 navigation
```

and test:

```text
Quest → Details → Scan → Reveal
```

with mocks.

---

# 123. 19 September Checkpoint

M2 must have:

```text
complete UI flow
```

using mock data.

The flow must be demonstrable even if:

```text
GPS
sensors
Firebase
```

are not yet integrated.

---

# 124. 20 September

Integrate:

```text
M3 location state
```

Test:

```text
distance
geofence
scan availability
```

---

# 125. 21 September

Integrate:

```text
M4 FusionResult
ScanState
Proximity
```

Test all major scan states.

---

# 126. 22 September

Run physical sensor integration.

Focus on:

```text
meter stability
instructions
threshold transition
proximity transition
reveal
```

---

# 127. 23 September

Integrate:

```text
M5/M6 persistence
progress
offline state
```

Then freeze core UI architecture.

---

# 128. 24 September

Run functional tests:

```text
quest
scan
reveal
duplicate
error
empty
```

---

# 129. 25 September

Run device compatibility tests.

At least:

```text
Device A
Device B
```

---

# 130. 26 September

Run resilience tests:

```text
offline
Firebase failure
sensor unavailable
poor GPS
background/foreground
```

---

# 131. 27 September

Regression only.

No new major UI features.

---

# 132. 28 September

Final:

```text
demo flow
screenshots
video evidence
final build
```

---

# 133. M2 Acceptance Criteria — Quest

- [ ] quest list loads
- [ ] all canonical relics can be represented
- [ ] quest detail opens
- [ ] discovery state displayed
- [ ] loading state works
- [ ] empty state works
- [ ] error state works

---

# 134. M2 Acceptance Criteria — Scan

- [ ] scan screen opens
- [ ] scan instructions visible
- [ ] fusion meter displays 0–100
- [ ] score updates
- [ ] scan states display correctly
- [ ] limited state works
- [ ] error state works

---

# 135. M2 Acceptance Criteria — Reveal

- [ ] fusion threshold alone does not reveal
- [ ] proximity gate is respected
- [ ] successful reveal works
- [ ] relic name displayed
- [ ] rarity displayed
- [ ] lore displayed
- [ ] duplicate discovery prevented

---

# 136. M2 Acceptance Criteria — Persistence

- [ ] successful discovery produces local success
- [ ] offline discovery remains available
- [ ] progress can be displayed
- [ ] sync state can be displayed where required
- [ ] leaderboard refresh works

---

# 137. M2 Acceptance Criteria — Integration

- [ ] M1 navigation integrated
- [ ] M3 location integrated
- [ ] M4 fusion integrated
- [ ] M5 relic/cloud data integrated
- [ ] M6 local persistence integrated
- [ ] complete end-to-end flow tested

---

# 138. M2 Performance

Do not:

```text
perform repository calls repeatedly on every UI redraw
start sensors from UI rendering
perform heavy calculations on main thread
```

M4 handles sensor processing.

M5/M6 handle data access.

M2 presents state.

---

# 139. M2 Lifecycle Safety

When Scan is not visible:

```text
do not maintain unnecessary UI observers
```

Coordinate sensor start/stop with M4.

---

# 140. M2 Accessibility

Check:

- [ ] buttons have meaningful labels
- [ ] text is readable
- [ ] important status is not communicated only by colour
- [ ] touch targets are usable
- [ ] dynamic scan state is understandable

---

# 141. M2 Visual Consistency

Use M1's:

```text
theme
typography
buttons
cards
spacing
navigation
```

Do not create a second visual language.

---

# 142. M2 Common Risks

## Risk 1 — Waiting for M4

Mitigation:

```text
MockFusionResult
```

## Risk 2 — Waiting for M3

Mitigation:

```text
MockDistanceResult
MockGeofenceEvent
```

## Risk 3 — Waiting for M5

Mitigation:

```text
MockQuestRepository
```

## Risk 4 — Reveal logic duplicated

Mitigation:

```text
centralized ScanState
```

## Risk 5 — Late UI redesign

Mitigation:

```text
freeze by 23 Sep
```

---

# 143. What M2 Must Not Do

Do not:

```text
create a second repository
implement a second fusion engine
calculate GPS distance independently
write directly to Firestore
write directly to Room
hard-code Firebase documents
use random relic IDs
reveal on geofence alone
include proximity in the fusion percentage
```

---

# 144. What M2 Should Do

M2 should:

```text
consume shared contracts
build against mocks
make state visible
keep the scan understandable
handle errors
handle limited mode
coordinate with M1/M3/M4/M5/M6
test continuously
```

---

# 145. Final M2 Demonstration

The M2 portion should be demonstrable as:

```text
Open Quests
 ↓
Select Founder’s Bell
 ↓
View details
 ↓
Enter scan
 ↓
See live fusion score
 ↓
Reach threshold
 ↓
Move closer
 ↓
Proximity confirmed
 ↓
Relic revealed
 ↓
Read lore
 ↓
See progress
```

---

# 146. Final M2 Checklist

## Quest

- [ ] Quest list
- [ ] Quest cards
- [ ] Details
- [ ] Discovery state
- [ ] Loading
- [ ] Empty
- [ ] Error

## Scan

- [ ] Scan screen
- [ ] Instructions
- [ ] Fusion meter
- [ ] Scan states
- [ ] Limited mode
- [ ] Proximity state
- [ ] Error state

## Reveal

- [ ] Success
- [ ] Relic information
- [ ] Rarity
- [ ] Lore
- [ ] Duplicate protection

## Integration

- [ ] M1
- [ ] M3
- [ ] M4
- [ ] M5
- [ ] M6

## Testing

- [ ] Mock tests
- [ ] Integration tests
- [ ] Offline test
- [ ] Sensor-degradation test
- [ ] Two-device test
- [ ] Full E2E test

---

# 147. M2 Definition of Ready

A task is ready when:

- [ ] UI purpose is known
- [ ] required input is known
- [ ] output/state is known
- [ ] dependency owner is known
- [ ] mock data exists
- [ ] acceptance criteria exist

---

# 148. M2 Definition of Done

A feature is done when:

- [ ] UI implemented
- [ ] mock flow works
- [ ] relevant states handled
- [ ] integration contract respected
- [ ] relevant tests pass
- [ ] real dependency integrated when available
- [ ] device-tested
- [ ] PR reviewed
- [ ] documentation updated if contract changed

---

# 149. Final Principle

M2's responsibility is to make the sensor-fusion technology understandable and engaging to the user.

The technical pipeline is:

```text
GPS
+
Light
+
Accelerometer
 ↓
Fusion
 ↓
Threshold
 ↓
Proximity
 ↓
Reveal
```

M2's responsibility is to turn that pipeline into a clear interaction:

```text
Approach
 ↓
Scan
 ↓
Improve match
 ↓
Move closer
 ↓
Discover
```

The UI should communicate the system accurately without exposing unnecessary technical complexity.
