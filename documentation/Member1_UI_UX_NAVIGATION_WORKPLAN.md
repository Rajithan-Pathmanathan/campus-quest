# Campus Quest — M1 Workplan: UI, UX & Navigation

**Document ID:** CQ-M1  
**Role:** M1 — Frontend Lead: App Shell & Main Screens  
**Project:** Campus Quest  
**Development window:** 13 September 2026 – 28 September 2026  
**Primary responsibility:** Application shell, navigation, common UI structure, authentication screens/UI, profile, theme, and integration of shared presentation patterns  
**Primary dependencies:** M5 Auth, M2 Quest/Scan UI, shared contracts and mock data

---

# 1. Role Objective

M1 is responsible for making Campus Quest feel like one coherent Android application.

M1 owns the application shell:

```text
Splash
 ↓
Login
 ↓
Main Application Shell
 ├── Map
 ├── Quests
 ├── Leaderboard
 └── Profile
```

M1 is not responsible for implementing:

```text
GPS logic
sensor fusion
Firebase data access
Room database
```

Those responsibilities belong to M3, M4, M5, and M6.

---

# 2. Primary Deliverables

M1 must deliver:

- Splash/startup screen
- Login screen
- Main navigation shell
- Bottom navigation
- Top app bar/toolbar pattern
- Map destination container
- Quest destination container
- Leaderboard destination container
- Profile destination
- Authentication UI states
- Loading/error/empty UI patterns
- Shared theme
- Reusable UI components
- Navigation between core screens
- Mock-based navigation before backend integration
- Final integration with M5 authentication

---

# 3. Architecture

The application should follow:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firestore
```

M1 should not bypass the ViewModel/Repository boundary for application data.

The architecture lecture material describes MVVM as separating UI concerns from application logic and using ViewModels for UI-related state. fileciteturn11file0L92-L106

---

# 4. M1 Ownership Boundary

M1 owns:

```text
App shell
Navigation
Theme
Common components
Splash
Login UI
Profile UI
Navigation state
UI loading/error/empty states
```

M1 shares:

```text
Auth → M5
Quest UI → M2
Leaderboard data → M5/M6
Map content → M3
```

---

# 5. Non-Ownership Boundary

Do not implement independently:

```text
Firebase queries
Firestore writes
Room queries
GPS calculations
Geofence registration
SensorManager logic
Fusion algorithm
Proximity detection
```

Use the contracts supplied by the responsible member.

---

# 6. Screen Map

Target navigation:

```text
Splash
  ↓
Login
  ↓
Main Shell
  ├── Map
  ├── Quests
  ├── Leaderboard
  └── Profile

Quests
  ↓
Quest Details
  ↓
Scan
  ↓
Reveal
```

M2 owns the Quest → Scan → Reveal feature screens.

M1 owns the navigation structure that makes these destinations reachable.

---

# 7. Startup Flow

Expected:

```text
App launch
 ↓
Splash/startup
 ↓
check authentication state
 ↓
authenticated?
 ├── YES → Main Shell
 └── NO  → Login
```

The exact authentication-state mechanism is an M5 integration concern.

---

# 8. Splash Screen

Required behaviour:

```text
Show application identity
Provide short startup state
Determine destination
```

Avoid making Splash perform:

```text
Firestore data loading
sensor initialization
GPS startup
large database operations
```

unless there is a specific architectural reason.

---

# 9. Splash Acceptance Criteria

- [ ] app launches without crash
- [ ] branding is visible
- [ ] authenticated user reaches main shell
- [ ] unauthenticated user reaches login
- [ ] no unnecessary long delay
- [ ] startup errors have a sensible path

---

# 10. Login Screen

Required UI:

```text
Application name
Email field
Password field
Login button
Loading state
Error state
```

Optional:

```text
show/hide password
```

Only implement additional features if they do not threaten the core schedule.

---

# 11. Login UI States

At minimum:

```text
Idle
Loading
Success
Error
```

Conceptually:

```text
Idle
 ↓ tap Login
Loading
 ↓
Success → Main
or
Error → Login
```

---

# 12. Login Validation

Before calling the backend where appropriate:

```text
email not empty
password not empty
```

Show readable messages.

Do not expose raw exceptions directly to users.

---

# 13. Login Integration Contract

M1 should consume an authentication abstraction rather than directly constructing Firebase calls.

Conceptually:

```kotlin
interface AuthRepository {
    fun observeAuthState(): Flow<User?>
    suspend fun signIn(
        email: String,
        password: String
    ): Result<User>
    suspend fun signOut()
}
```

This is a proposed shared contract. If M5 implements a different final interface, M1 must use the agreed shared version rather than maintaining a competing implementation.

---

# 14. Mock Authentication

Before M5 is ready, M1 should use a mock implementation.

Example:

```text
Email:
nimal@example.com

Password:
development-password
```

Successful result:

```text
U001
Nimal Perera
```

The exact mock credentials should remain development-only.

---

# 15. Invalid Login Mock

Test:

```text
unknown@example.com
```

Expected:

```text
Login error
```

No crash.

---

# 16. Main Shell

After authentication:

```text
Main Shell
```

should provide the primary destinations.

Recommended:

```text
Map
Quests
Leaderboard
Profile
```

---

# 17. Bottom Navigation

Bottom navigation should:

- [ ] show core destinations
- [ ] indicate selected destination
- [ ] preserve intuitive back behaviour
- [ ] avoid excessive nesting
- [ ] use consistent icons
- [ ] use meaningful labels

---

# 18. Navigation Labels

Use stable labels such as:

```text
Map
Quests
Leaderboard
Profile
```

Do not change labels independently during development without notifying affected UI/documentation owners.

---

# 19. Map Destination

M1 owns:

```text
destination
toolbar
loading/error container
```

M3 owns:

```text
Google Maps
location
markers
geofencing
```

M1 should provide the screen structure that hosts M3's implementation.

---

# 20. Quest Destination

M2 owns the actual quest content.

M1 should provide:

```text
navigation destination
toolbar/back-stack behaviour
```

M2 should not create a second root navigation system.

---

# 21. Leaderboard Destination

M1 owns:

```text
navigation destination
common shell
```

M2 owns presentation details if agreed.

M5/M6 own the data source.

---

# 22. Profile Destination

M1 owns:

```text
profile screen
user display information
logout action
```

Profile may show:

```text
display name
email
relic count
```

The actual progress value should come through the agreed data boundary.

---

# 23. Logout Flow

Expected:

```text
Profile
 ↓
Logout
 ↓
authentication state cleared
 ↓
Login
```

M1 owns the UI action.

M5 owns the authentication implementation.

---

# 24. Theme

M1 owns the shared visual theme.

The theme should be applied consistently to:

```text
buttons
text
cards
navigation
backgrounds
inputs
dialogs
```

Do not let individual members invent unrelated themes for their screens.

---

# 25. Material UI

Use the project's agreed Material design approach consistently.

The uploaded layout/design materials cover standard Android layout choices and modern UI approaches; the team should keep component structure consistent rather than mixing unrelated UI patterns. fileciteturn11file1L341-L378

---

# 26. Layout Choice

For XML-based screens, use appropriate layouts rather than deeply nested containers.

Common options:

```text
ConstraintLayout
FrameLayout
LinearLayout
RecyclerView
```

The layout guide describes ConstraintLayout as useful for positioning views through constraints and FrameLayout as useful for stacking/overlay-style content. fileciteturn11file1L341-L378

---

# 27. Reusable Components

Create reusable components where repetition exists.

Examples:

```text
PrimaryButton
QuestCard
StatusMessage
LoadingIndicator
EmptyState
ErrorState
TopBar
```

Do not over-engineer every small TextView into a separate abstraction.

---

# 28. Loading State

Every data-dependent screen should have a predictable loading state.

Example:

```text
Loading...
```

then:

```text
content
```

or:

```text
error
```

---

# 29. Error State

Errors should be:

```text
understandable
recoverable where possible
consistent
```

Example:

```text
Unable to load quests.
Try again.
```

rather than exposing:

```text
FirebaseFirestoreException: PERMISSION_DENIED...
```

---

# 30. Empty State

Examples:

```text
No quests available
No discoveries yet
No leaderboard data
```

Provide meaningful UI instead of blank space.

---

# 31. Offline UI

If cloud data is unavailable but local data exists, communicate the state appropriately.

Example:

```text
Offline — showing saved data
```

M6 owns the underlying offline behaviour.

M1 owns how the state is communicated visually where the UI contract requires it.

---

# 32. Accessibility

At minimum:

- [ ] readable text
- [ ] meaningful button labels
- [ ] sufficient touch target size
- [ ] icons not used as the only source of meaning
- [ ] content is understandable without colour alone

---

# 33. Screen Consistency

All screens should share:

```text
typography
spacing
button style
card style
icon style
navigation behaviour
```

---

# 34. Navigation Back Stack

Test:

```text
Map
 ↓
Quest
 ↓
Details
 ↓
Scan
```

Back should return to the appropriate previous destination.

Avoid accidental navigation to:

```text
Splash
Login
```

unless logout occurs.

---

# 35. Deep Navigation Rule

The Scan screen should be reachable through the quest flow.

M3 geofence activation can trigger or make Scan available according to the agreed integration design.

M1 should not duplicate M3's geofence logic.

---

# 36. State Restoration

Important UI state should be handled through the selected architecture.

Examples:

```text
selected quest
loading/error state
authentication state
```

ViewModel-based state helps separate UI state from Activities/Fragments and supports configuration-change handling. fileciteturn11file0L102-L106

---

# 37. ViewModel Responsibilities

For M1 screens:

```text
LoginViewModel
ProfileViewModel
MainViewModel
```

should be created only where meaningful.

Avoid creating ViewModels solely because every screen is required to have one.

---

# 38. LoginViewModel

Conceptual responsibilities:

```text
email state
password state
loading state
login action
error state
authentication result
```

---

# 39. ProfileViewModel

Potential responsibilities:

```text
user information
progress information
logout action
```

Do not directly query Firebase inside the ViewModel.

---

# 40. Main Navigation State

The navigation layer should determine:

```text
current destination
back behaviour
auth-gated destinations
```

It should not contain:

```text
fusion algorithm
Firestore business logic
Room DAO operations
```

---

# 41. Development Phase 1 — 13 September

Deliver:

```text
project builds
theme foundation
navigation skeleton
Splash
Login UI
Main shell
```

Do not wait for Firebase.

---

# 42. Development Phase 2 — 14 September

Deliver:

```text
Map destination
Quest destination
Leaderboard destination
Profile destination
```

Use placeholders/mock content.

---

# 43. Development Phase 3 — 15 September

Implement:

```text
navigation polish
loading states
error states
empty states
reusable components
```

---

# 44. Development Phase 4 — 16 September

Integrate:

```text
mock authentication
mock user
mock navigation state
```

Test the complete startup journey.

---

# 45. Development Phase 5 — 17 September

Integrate M2's quest screens.

Expected:

```text
Quests
 ↓
Quest Details
 ↓
Scan
 ↓
Reveal
```

---

# 46. Development Phase 6 — 18 September

Focus on:

```text
visual consistency
navigation bugs
state handling
device testing
```

---

# 47. 19 September Checkpoint

M1 must demonstrate:

```text
app launch
 ↓
login
 ↓
main shell
 ↓
Map
 ↓
Quests
 ↓
Leaderboard
 ↓
Profile
```

and:

```text
Quest
 ↓
Scan
 ↓
Reveal
```

using mock data if necessary.

---

# 48. 20–21 September

Integrate real:

```text
M5 Auth
M5/M6 progress data
```

Fix:

```text
loading
error
logout
authentication state
```

---

# 49. 22 September

Full integration support.

M1 participates in:

```text
map
quest
scan
reveal
progress
```

end-to-end validation.

---

# 50. 23 September

M1 feature freeze.

After this point:

```text
no major navigation redesign
no new visual architecture
no unnecessary component replacement
```

Focus on stability.

---

# 51. 24–27 September

Testing and bug fixing.

Priority:

```text
P0
P1
P2
```

Do not spend major time on cosmetic P3 issues if core functionality is unstable.

---

# 52. 28 September

Final:

```text
build
navigation test
login test
device test
demo rehearsal
```

---

# 53. M1 Mock Data

Use:

```text
U001 — Nimal Perera
```

for the primary logged-in mock user.

Use:

```text
U005 — Test Explorer
```

for clean-state testing.

---

# 54. Mock Quest Data

M1 should not independently redefine relics.

Use canonical:

```text
R001
R002
R003
R004
R005
R006
```

from the shared mock catalog.

---

# 55. M1 Primary User Journey

Test:

```text
Launch
 ↓
Splash
 ↓
Login
 ↓
Map
 ↓
Quests
 ↓
Quest Details
 ↓
Scan
 ↓
Reveal
 ↓
Progress
 ↓
Leaderboard
 ↓
Profile
 ↓
Logout
```

---

# 56. Navigation Test Matrix

| Test | Expected |
|---|---|
| Splash → Login | Correct |
| Splash → Main | Correct when authenticated |
| Login → Main | Correct |
| Main → Map | Correct |
| Main → Quests | Correct |
| Main → Leaderboard | Correct |
| Main → Profile | Correct |
| Quest → Details | Correct |
| Details → Scan | Correct |
| Scan → Reveal | Correct |
| Profile → Logout | Login |
| Back navigation | Correct |

---

# 57. M1 Testing

Minimum tests:

```text
M1-001 Launch
M1-002 Unauthenticated startup
M1-003 Authenticated startup
M1-004 Valid login
M1-005 Invalid login
M1-006 Logout
M1-007 Bottom navigation
M1-008 Back navigation
M1-009 Loading state
M1-010 Error state
M1-011 Empty state
M1-012 Screen restoration
M1-013 Device compatibility
```

---

# 58. M1-001 — Launch

Expected:

```text
No crash
Splash/startup state appears
```

---

# 59. M1-002 — Unauthenticated Startup

Expected:

```text
Splash
 ↓
Login
```

---

# 60. M1-003 — Authenticated Startup

Expected:

```text
Splash
 ↓
Main Shell
```

---

# 61. M1-004 — Valid Login

Use mock:

```text
U001
```

Expected:

```text
Main Shell
```

---

# 62. M1-005 — Invalid Login

Expected:

```text
error
remain on login
```

---

# 63. M1-006 — Logout

Expected:

```text
Login
```

---

# 64. M1-007 — Bottom Navigation

Tap every destination.

Expected:

```text
correct screen
correct selected state
```

---

# 65. M1-008 — Back Navigation

Expected:

```text
previous logical screen
```

---

# 66. M1-009 — Loading State

Force a delayed/mock repository response.

Expected:

```text
loading indicator
```

---

# 67. M1-010 — Error State

Force repository error.

Expected:

```text
readable error
retry where appropriate
```

---

# 68. M1-011 — Empty State

Return empty list.

Expected:

```text
meaningful empty UI
```

---

# 69. M1-012 — State Restoration

Open a screen.

Trigger a supported configuration/lifecycle change.

Expected:

```text
important state remains coherent
```

---

# 70. M1-013 — Device Compatibility

Run core UI flow on:

```text
Device A
Device B
```

Expected:

```text
no blocking layout/navigation issue
```

---

# 71. M1 Handoff to M2

M1 provides:

```text
navigation destination
theme
shared components
navigation contract
```

M2 provides:

```text
quest/scan/reveal screens
```

---

# 72. M1 Handoff to M5

M1 requires:

```text
AuthRepository contract
User model
auth states
```

M5 provides:

```text
Firebase Auth implementation
```

---

# 73. M1 Handoff to M6

If profile/progress data is locally available:

```text
M6 → agreed progress state
```

M1 displays it.

M1 does not query Room directly.

---

# 74. M1 Handoff to M3

M1 provides:

```text
Map destination container
```

M3 provides:

```text
Map implementation
location state
```

---

# 75. M1 Handoff Format

When M1 finishes a shared navigation component:

```text
[HANDOFF]

From: M1
To: M2/M3/M5/M6
Feature:
Branch/PR:
How to use:
Dependencies:
Known limitations:
```

---

# 76. Common UI Contract

M1 should define:

```text
Primary button
Secondary button
Card
Loading
Error
Empty
Top bar
```

The exact visual values should come from the final design system.

---

# 77. Avoid Over-Engineering

Do not spend project time creating:

```text
large custom design system
complex animation framework
generic navigation abstraction
unused UI architecture
```

The app has a one-month implementation window.

---

# 78. UI Performance

Avoid:

```text
heavy work on main thread
unnecessary recomposition/re-rendering
large images without need
nested expensive layouts
```

The UI should remain responsive during sensor scanning and data operations.

---

# 79. Sensor Screen Boundary

M2 owns Scan UI.

M4 owns sensor processing.

M1 owns navigation into the Scan experience.

Therefore:

```text
M1 → navigation
M2 → presentation
M4 → sensor logic
```

---

# 80. Map Screen Boundary

```text
M1 → shell
M3 → map/location
```

---

# 81. Profile Boundary

```text
M1 → profile presentation
M5 → auth
M6/M5 → progress data
```

---

# 82. Leaderboard Boundary

```text
M1 → destination
M2 → presentation where assigned
M5/M6 → data
```

---

# 83. UI Error Mapping

M1 should map technical states to user-friendly states.

Examples:

```text
Network unavailable
→ "You're offline. Saved data is still available."

Permission denied
→ "Location permission is required for the map."

Unknown error
→ "Something went wrong. Try again."
```

Exact wording may change with final UX design.

---

# 84. Navigation Security

Unauthenticated users should not accidentally access authenticated-only application screens.

Expected:

```text
no auth
 ↓
Login
```

---

# 85. Logout Security

After logout:

```text
back navigation
```

should not expose authenticated content unexpectedly.

---

# 86. Profile Security

Do not display sensitive information unnecessarily.

At minimum display only information required by the project.

---

# 87. M1 Risks

## Risk 1 — Waiting for M5

Mitigation:

```text
mock authentication
```

## Risk 2 — Waiting for M2

Mitigation:

```text
placeholder quest/scan destinations
```

## Risk 3 — Navigation conflicts

Mitigation:

```text
M1 owns navigation
```

## Risk 4 — Late redesign

Mitigation:

```text
freeze UI architecture by 19–23 Sep
```

---

# 88. M1 Daily Checklist

Each day:

- [ ] pull latest main
- [ ] check shared contract changes
- [ ] implement small feature
- [ ] test
- [ ] commit
- [ ] push
- [ ] communicate blockers
- [ ] update handoff if needed

---

# 89. M1 Git Branch

Recommended:

```text
feature/ui-navigation
```

---

# 90. M1 Commit Examples

```text
feat: add app navigation shell
feat: add login screen
feat: add profile screen
feat: add shared loading state
fix: preserve selected tab
fix: handle logout navigation
test: add login validation tests
```

---

# 91. M1 Pull Request Checklist

- [ ] App builds
- [ ] Navigation tested
- [ ] Login tested
- [ ] No direct Firebase calls
- [ ] No direct Room calls
- [ ] Shared theme used
- [ ] No unrelated files changed
- [ ] Screenshots included when useful

---

# 92. M1 Code Review Risks

Watch for:

```text
navigation logic duplicated
hard-coded user data
Firebase calls in UI
Room calls in UI
business logic in Activity
large composable/screen functions
```

---

# 93. M1 Definition of Ready

M1 starts a task only when:

- [ ] screen purpose known
- [ ] owner known
- [ ] data source known
- [ ] navigation destination known
- [ ] mock data available
- [ ] acceptance criteria known

---

# 94. M1 Definition of Done

A screen is done when:

- [ ] correct navigation
- [ ] UI implemented
- [ ] loading handled
- [ ] error handled
- [ ] empty state handled where relevant
- [ ] lifecycle considered
- [ ] device tested
- [ ] code committed
- [ ] PR reviewed
- [ ] integration tested

---

# 95. Final M1 Acceptance

M1 is complete when the application can demonstrate:

```text
Launch
 ↓
Authentication
 ↓
Main Shell
 ↓
Map
 ↓
Quests
 ↓
Scan
 ↓
Reveal
 ↓
Leaderboard
 ↓
Profile
 ↓
Logout
```

with consistent UI and navigation.

---

# 96. Final M1 Checklist

## App Shell

- [ ] Splash
- [ ] Login
- [ ] Main shell
- [ ] Bottom navigation
- [ ] Toolbar
- [ ] Theme

## Screens

- [ ] Map destination
- [ ] Quest destination
- [ ] Leaderboard destination
- [ ] Profile destination

## States

- [ ] Loading
- [ ] Error
- [ ] Empty
- [ ] Offline where relevant
- [ ] Authenticated
- [ ] Unauthenticated

## Integration

- [ ] M5 Auth integrated
- [ ] M2 Quest flow integrated
- [ ] M3 Map integrated
- [ ] M5/M6 progress integrated
- [ ] Final navigation tested

## Quality

- [ ] Device A tested
- [ ] Device B tested
- [ ] No P0 UI/navigation bug
- [ ] No broken core navigation
- [ ] Final build verified

---

# 97. M1 Final Principle

M1's objective is not to make every feature personally.

It is to make sure that:

```text
six technical modules
```

appear to the user as:

```text
one coherent application.
```

M1 should therefore protect:

```text
navigation
consistency
clarity
stability
```

while allowing M2–M6 to own their technical domains.
