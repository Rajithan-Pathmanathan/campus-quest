# GIT CHANGE CONTROL & TEAM DEVELOPMENT WORKFLOW
## Campus Quest — Final Six-Member Development Workflow

**Status:** Final reassignment version  
**Repository model:** Shared Git repository with feature branches and protected stable branches  
**Integration coordinator:** M6  
**Primary development platform:** Android Studio / Kotlin

---

# 1. PURPOSE

This document defines how the six members develop Campus Quest concurrently without creating incompatible implementations.

The project has six ownership areas:

```text
M1 — Player UI & Navigation
M2 — Creator & Game Management
M3 — Location, Sensors & Fusion
M4 — Quest & Scan Gameplay
M5 — Firebase & Backend
M6 — Room, Repository, Sync & Integration
```

The objective is:

```text
parallel development
        ↓
controlled changes
        ↓
review
        ↓
integration
        ↓
stable application
```

---

# 2. CORE RULE

A member owns an implementation area, but no member owns the shared architecture independently.

Shared contracts include:

```text
domain models
repository interfaces
navigation arguments
Firestore paths
Room keys
authentication identity
game/checkpoint scope
sensor/fusion contracts
FCM payloads
```

Changes to these interfaces must be communicated before implementation.

---

# 3. BRANCH STRUCTURE

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

# 4. BRANCH PURPOSES

## `main`

Contains:

```text
stable builds
demonstration-ready versions
submission-ready versions
```

Do not use `main` for unfinished feature development.

## `develop`

Contains:

```text
current integrated development state
```

Feature branches merge into `develop` after review.

## Feature branches

Each member develops primarily on their assigned branch.

---

# 5. OWNERSHIP

| Member | Branch | Primary area |
|---|---|---|
| M1 | feature/m1-player-ui-navigation | Player UI/navigation |
| M2 | feature/m2-creator-game-management | Creator/game management |
| M3 | feature/m3-location-sensors-fusion | Location/sensors/fusion |
| M4 | feature/m4-quest-scan-gameplay | Quest/scan gameplay |
| M5 | feature/m5-firebase-backend | Firebase/backend |
| M6 | feature/m6-room-sync-integration | Room/repository/sync/integration |

---

# 6. BEFORE STARTING WORK

Every member should:

```text
1. Pull latest develop.
2. Confirm their feature branch is current.
3. Read shared contracts.
4. Check dependencies.
5. Confirm no conflicting PR is already changing the same contract.
```

---

# 7. DAILY WORKFLOW

Recommended cycle:

```text
Pull
 ↓
Implement
 ↓
Build
 ↓
Run tests
 ↓
Commit
 ↓
Push
 ↓
Update PR
```

Do not accumulate several days of unmerged work if a smaller integration point is available.

---

# 8. COMMIT PRINCIPLE

Each commit should represent one logical change.

Good:

```text
Add checkpoint validation
Add creator checkpoint reorder
Add fusion score calculator tests
Add game leaderboard repository method
```

Avoid:

```text
final changes
fix everything
updates
stuff
```

---

# 9. COMMIT FORMAT

Recommended:

```text
type(scope): description
```

Examples:

```text
feat(m2): add checkpoint creation form
feat(m3): add light signature matcher
feat(m4): add scan state machine
feat(m5): add game publish repository
feat(m6): add pending sync entity
fix(m1): preserve gameId in navigation
test(m3): add fusion threshold tests
refactor(m6): isolate repository sync logic
```

---

# 10. COMMIT TYPES

Use:

```text
feat
fix
test
refactor
docs
chore
build
```

---

# 11. COMMIT SIZE

Prefer:

```text
small
focused
buildable
reviewable
```

commits.

Avoid committing generated files, IDE-local configuration, secrets, or unrelated formatting changes with a feature.

---

# 12. PUSH POLICY

Before pushing:

```text
[ ] Code compiles
[ ] Relevant tests pass
[ ] No accidental files
[ ] No API keys/secrets
[ ] No unrelated changes
```

---

# 13. PULL REQUEST POLICY

Every PR should contain:

```text
Summary
Changes
Tests
Dependencies
Shared contracts affected
Screenshots where UI changed
Known limitations
```

---

# 14. PR TEMPLATE

Recommended:

```text
## Summary
What does this change implement?

## Changes
- ...
- ...

## Tests
- ...
- ...

## Shared Contract Impact
None / Describe changes

## Dependencies
None / List dependent branch or PR

## UI Evidence
Attach screenshots if applicable.

## Known Limitations
- ...
```

---

# 15. REVIEW REQUIREMENT

At least one other team member should review important PRs.

For shared architecture changes, review should involve the affected owners.

Examples:

```text
Repository contract → M5 + M6
Sensor contract → M3 + M4
Navigation contract → M1 + M4
Game model → M2 + M5 + M6
```

---

# 16. SHARED-CONTRACT CHANGE RULE

Do not silently change:

```text
Game
Checkpoint
GameStatus
LightSignature
Repository methods
Firestore paths
Room keys
FCM payload
navigation arguments
```

without notifying the affected members.

---

# 17. CONTRACT CHANGE PROCESS

```text
Identify required change
        ↓
Explain reason
        ↓
Identify affected members
        ↓
Agree on new contract
        ↓
Update shared contract document
        ↓
Update implementations
        ↓
Run integration tests
        ↓
Merge
```

---

# 18. DOMAIN MODEL CHANGE

Example:

```text
Checkpoint.radiusM
```

is changed.

Affected:

```text
M2 — creator form
M3 — geofencing
M4 — scan eligibility
M5 — Firestore
M6 — Room/repository
```

Therefore this is not an isolated M2 change.

---

# 19. NAVIGATION CHANGE

Example:

Old:

```text
ScanScreen
```

requires:

```text
checkpointId
```

New:

```text
ScanScreen
```

requires:

```text
gameId + checkpointId
```

Affected:

```text
M1 navigation
M4 scan
M3 checkpoint context
M6 repository
```

This change must be coordinated.

---

# 20. FIRESTORE PATH CHANGE

Example:

```text
games/{gameId}/checkpoints/{checkpointId}
```

is changed.

Affected:

```text
M5
M6
M2
M3
M4
```

No member should continue using the old path after the contract is updated.

---

# 21. ROOM KEY CHANGE

Example:

Old:

```text
checkpointId
```

New:

```text
gameId + checkpointId
```

This is a critical data-isolation change.

Required:

```text
M6 schema update
migration
DAO update
repository update
tests
```

and affected consumers must be checked.

---

# 22. MERGE ORDER

When several PRs are ready, prefer:

```text
1. Shared contract/foundation
2. M6 repository/Room foundation
3. M5 Firebase foundation
4. M1 navigation shell
5. M2 creator
6. M1 player discovery
7. M3 physical signal pipeline
8. M4 scan
9. M6 sync
10. FCM/deep links
11. Full integration
```

This follows dependency direction rather than member number.

---

# 23. CONFLICT RESOLUTION

When a conflict occurs:

```text
Do not immediately choose "ours" or "theirs".
```

First determine:

```text
Which behavior matches the current shared contract?
```

Then:

```text
resolve
 ↓
build
 ↓
test
 ↓
review
```

---

# 24. CONFLICT PRIORITY

When resolving conflicts, preserve:

```text
1. Current shared contract
2. Data correctness
3. Security
4. Game/user isolation
5. Working behavior
6. UI polish
```

---

# 25. NO DUPLICATE IMPLEMENTATIONS

Avoid situations such as:

```text
M3 has FusionManager
M4 also has FusionManager
```

or:

```text
M5 has one leaderboard calculation
M6 has another leaderboard calculation
```

There must be one agreed source of truth for each core behavior.

---

# 26. SOURCE-OF-TRUTH RULE

| Concern | Source of truth |
|---|---|
| UI state | ViewModel |
| Game model | Domain model |
| Cloud persistence | Firebase/Firestore |
| Local persistence | Room |
| Data orchestration | Repository |
| Location/sensors/fusion | M3 technical layer |
| Scan state | M4 |
| Game leaderboard data | Firestore |
| Player leaderboard presentation | M1 |

---

# 27. M1 CHANGE RULES

M1 should not directly introduce:

```text
Firebase calls inside screens
Room queries inside screens
SensorManager logic inside screens
FusedLocationProviderClient logic inside screens
```

UI should communicate through ViewModels and shared repository/state contracts.

The course architecture material identifies the View as presentation/input, the ViewModel as UI-related state/logic, and the repository/data layer as the abstraction over storage/network. fileciteturn34file1L163-L177

---

# 28. M2 CHANGE RULES

M2 owns:

```text
creator UI
game forms
checkpoint forms
draft UI
publish UI
```

M2 should not duplicate:

```text
Firebase implementation
Room implementation
sensor implementation
geofence implementation
scan state machine
```

---

# 29. M3 CHANGE RULES

M3 owns:

```text
location
geofence
ambient light
accelerometer
proximity
normalization
fusion
```

M3 should expose stable results rather than requiring M4 to access hardware directly.

---

# 30. M4 CHANGE RULES

M4 owns:

```text
scan UI
scan state machine
fusion meter presentation
proximity gate presentation
reveal
```

M4 should not duplicate:

```text
SensorManager
Firebase
Firestore
Room
```

---

# 31. M5 CHANGE RULES

M5 owns:

```text
Firebase Auth
Firestore
security rules
FCM
cloud validation
cloud leaderboard
```

M5 should not make UI screens directly depend on Firestore implementation details.

---

# 32. M6 CHANGE RULES

M6 owns:

```text
Room
DAO
repository orchestration
cache
pending sync
retry
transactions
migrations
integration
```

M6 should not silently change product behavior merely to simplify implementation.

---

# 33. FIREBASE SECURITY

Never commit:

```text
private credentials
service-account keys
API secrets
```

Use appropriate Android/Firebase configuration and repository/environment controls.

---

# 34. SECRETS CHECK

Before every push:

```text
Search for:
API_KEY
SECRET
PASSWORD
TOKEN
PRIVATE_KEY
SERVICE_ACCOUNT
```

No actual secrets should be committed to Git.

---

# 35. FIREBASE RULE CHANGES

Security rules must be reviewed as code.

Any change affecting:

```text
creator ownership
player membership
progress
leaderboards
```

requires security tests.

---

# 36. FIREBASE INDEX CHANGES

If Firestore requires a new index:

```text
document query
 ↓
identify missing index
 ↓
add configuration
 ↓
test query
 ↓
commit configuration
```

Do not rely on one developer's local console configuration only.

---

# 37. DATABASE MIGRATION POLICY

Room schema changes require:

```text
entity update
DAO review
migration
migration test
repository compatibility check
```

Do not simply delete the database during development to hide a migration problem.

---

# 38. DYNAMIC DATA RULE

No production feature may depend on:

```text
R001–R006
```

as fixed application logic.

They are seed/demo identifiers only.

---

# 39. LEGACY CLEANUP

Search before final merge for:

```text
RelicEntity
FoundRelicEntity
hard-coded relic lists
global leaderboard
fixed checkpoint count
```

Any remaining use must be justified as:

```text
seed
test
migration
legacy compatibility
```

not core production behavior.

---

# 40. FEATURE FLAGGING

If a feature is incomplete, prefer:

```text
clear disabled state
```

over silently merging half-working behavior into the main gameplay path.

Do not leave hidden debug bypasses enabled in the final build.

---

# 41. DEBUG CODE RULE

Before final build remove/disable:

```text
auto-success scan
fake sensor values
hard-coded GPS
bypass authentication
test leaderboard injection
debug-only navigation shortcuts
```

unless explicitly required for a controlled demo and clearly isolated.

---

# 42. MOCK DATA RULE

Mock data is acceptable for:

```text
unit tests
UI previews
early feature development
controlled seed/demo
```

It must not silently replace the production repository.

---

# 43. INTEGRATION CHECKPOINTS

Recommended checkpoints:

```text
IC-1 — App launches
IC-2 — Auth + player navigation
IC-3 — Creator → Firestore
IC-4 — Player → dynamic game
IC-5 — Map + dynamic checkpoints
IC-6 — Sensors + fusion
IC-7 — Scan + reveal
IC-8 — Discovery + leaderboard
IC-9 — Offline + sync
IC-10 — Final end-to-end
```

---

# 44. IC-1 — APP LAUNCH

Required:

```text
clean build
app launches
navigation graph loads
no fatal startup exception
```

---

# 45. IC-2 — AUTH + PLAYER

Required:

```text
login
authenticated state
player games
logout
```

---

# 46. IC-3 — CREATOR CLOUD

Required:

```text
create game
save draft
create checkpoint
publish
```

Cloud data must match the domain model.

---

# 47. IC-4 — PLAYER CLOUD

Required:

```text
published game
game details
join
membership
```

---

# 48. IC-5 — MAP

Required:

```text
gameId
checkpoint list
markers
location
game-scoped geofences
```

---

# 49. IC-6 — SENSOR FUSION

Required:

```text
GPS score
light score
motion score
weighted fusion
threshold
proximity state
```

---

# 50. IC-7 — SCAN

Required:

```text
scan state
fusion display
proximity gate
success
reveal
```

---

# 51. IC-8 — DISCOVERY

Required:

```text
Room
repository
Firestore
leaderboard
duplicate protection
```

---

# 52. IC-9 — OFFLINE

Required:

```text
cache
offline discovery where supported
pending sync
retry
reconnection
```

---

# 53. IC-10 — FINAL

Required:

```text
creator
publish
notification
player
join
map
checkpoint
scan
fusion
proximity
reveal
discovery
sync
leaderboard
```

---

# 54. DAILY INTEGRATION BUILD

At least once during active integration work:

```text
pull develop
build
run smoke tests
```

If the build is broken, stop adding unrelated changes until the cause is understood.

---

# 55. BROKEN BUILD POLICY

If `develop` is broken:

```text
1. Identify last known good commit.
2. Identify breaking PR.
3. Notify affected member.
4. Fix or revert.
5. Re-run smoke tests.
```

Do not stack additional features on top of a known broken integration state.

---

# 56. REVERT POLICY

A PR should be reverted when:

```text
it breaks core functionality
cannot be repaired quickly
introduces unacceptable security/data risk
```

Reverting is preferable to leaving `develop` permanently broken.

---

# 57. RELEASE BRANCH

When the application reaches feature completeness:

```text
develop
   ↓
release/final-demo
   ↓
stabilization
   ↓
main
```

Use the release branch only for:

```text
bug fixes
configuration
final UI polish
documentation
release preparation
```

---

# 58. RELEASE FREEZE

After final stabilization:

```text
No new major features.
```

Only:

```text
P0/P1 fixes
required configuration
submission-critical fixes
```

should be merged.

---

# 59. TAGGING

Recommended tags:

```text
v0.1-foundation
v0.2-player
v0.3-creator
v0.4-location
v0.5-scan
v0.6-cloud-sync
v1.0-demo
```

Final tag:

```text
v1.0-demo
```

or another agreed submission version.

---

# 60. FINAL BUILD REPRODUCIBILITY

A new team member should be able to:

```text
clone repository
 ↓
open Android Studio
 ↓
configure required local settings
 ↓
build
 ↓
run
```

without relying on undocumented local changes.

---

# 61. README REQUIREMENTS

Repository README should contain:

```text
Project description
Technology stack
Architecture
Setup instructions
Firebase configuration
Build instructions
Test instructions
Branch structure
Team ownership
Known limitations
Demo instructions
```

---

# 62. DOCUMENTATION CHANGE CONTROL

When architecture changes, update the affected documentation.

Minimum affected documents may include:

```text
Shared Contracts
Master Development Plan
Member Workplan
TRD
Testing Strategy
Integration Plan
Git Workflow
```

Do not allow code and documentation to describe different architectures.

---

# 63. CHANGE IMPACT MATRIX

| Change | Likely affected |
|---|---|
| UI-only | M1/M2/M4 |
| Navigation | M1/M2/M4 |
| Game model | M2/M5/M6 + consumers |
| Checkpoint model | M2/M3/M5/M6 |
| Sensor contract | M3/M4 |
| Repository interface | M1/M2/M4/M5/M6 |
| Firestore schema | M5/M6 + consumers |
| Room schema | M6 + repository |
| FCM payload | M1/M5 |
| Leaderboard logic | M1/M5/M6 |
| Offline behavior | M5/M6 + UI consumers |

---

# 64. DOCUMENT VERSIONING

Use a simple convention:

```text
v1.0
v1.1
v1.2
```

Major changes:

```text
v2.0
```

Examples:

```text
Shared Contracts v1.2
Testing Strategy v1.1
```

---

# 65. TEAM COMMUNICATION

For changes affecting another member:

```text
mention member
describe contract
describe reason
describe expected migration
```

Example:

```text
M3: FusionResult will now expose proximity separately.
M4: Please update scan state handling to consume the new field.
```

---

# 66. NO ASSUMPTION RULE

If an interface is unclear:

```text
do not guess
```

Instead:

```text
check shared contract
ask owner
document decision
implement
```

This is especially important for:

```text
IDs
weights
thresholds
Firestore paths
Room keys
navigation arguments
```

---

# 67. CODE REVIEW QUESTIONS

Reviewers should ask:

```text
Does this follow the shared architecture?
Does this introduce duplicated logic?
Does it preserve game/user isolation?
Does it introduce hard-coded checkpoints?
Does it bypass the repository?
Does it break offline behavior?
Does it introduce a security problem?
Are tests included?
```

---

# 68. UI REVIEW QUESTIONS

For UI PRs:

```text
Does the screen follow approved Figma flow?
Are loading/error/empty states handled?
Are navigation arguments correct?
Are touch targets usable?
Is content clipped?
Does rotation preserve important state?
```

The course UI/UX material emphasizes usability, consistency, responsive layouts, safe areas, touch targets, and prototyping before implementation. fileciteturn34file0L209-L235 fileciteturn34file0L243-L265

---

# 69. ARCHITECTURE REVIEW QUESTIONS

For architecture PRs:

```text
Is responsibility in the correct layer?
Does ViewModel avoid direct View references?
Does UI avoid direct data-source access?
Does repository hide data-source implementation?
Can the logic be unit-tested?
```

The course material describes MVVM specifically as separating UI from ViewModel state/logic and notes the resulting testability and lifecycle benefits. fileciteturn34file1L163-L177

---

# 70. FINAL CODE QUALITY CHECK

Before final release:

```text
[ ] No unused production classes
[ ] No duplicate managers
[ ] No dead navigation routes
[ ] No debug bypasses
[ ] No hard-coded production checkpoint list
[ ] No global leaderboard logic
[ ] No direct Firestore from UI
[ ] No direct Room from UI
[ ] No sensor logic in scan UI
[ ] No secrets
```

---

# 71. FINAL BRANCH CHECK

Before release:

```text
main = stable
develop = tested
release/final-demo = submission candidate
```

No unfinished feature branch should be required to make the final application run.

---

# 72. FINAL PR CHECKLIST

```text
[ ] Correct branch
[ ] Small logical change
[ ] Builds
[ ] Tests pass
[ ] No secrets
[ ] No unrelated files
[ ] Shared contract checked
[ ] Documentation updated if required
[ ] Reviewer assigned
[ ] Conflicts resolved
```

---

# 73. FINAL INTEGRATION CHECKLIST

```text
[ ] M1 merged
[ ] M2 merged
[ ] M3 merged
[ ] M4 merged
[ ] M5 merged
[ ] M6 merged
[ ] develop builds
[ ] smoke tests pass
[ ] E2E passes
[ ] physical-device test passes
[ ] security tests pass
[ ] offline/sync tests pass
```

---

# 74. FINAL SUBMISSION CHECKLIST

```text
[ ] main contains final stable build
[ ] final version tagged
[ ] README complete
[ ] architecture documentation current
[ ] testing evidence collected
[ ] demo data prepared
[ ] Firebase configuration verified
[ ] physical device verified
[ ] no development bypasses
[ ] no exposed secrets
```

---

# 75. EMERGENCY SHORT-TIME RULE

If the team has very little time remaining:

### Freeze architecture

Do not redesign:

```text
Game model
Repository
Firestore structure
Room keys
Fusion model
Navigation architecture
```

unless a blocking defect requires it.

### Focus on:

```text
core E2E flow
security
data correctness
physical sensor behavior
build stability
```

### Defer:

```text
non-essential animations
extra customization
minor visual refinements
optional features
```

---

# 76. FINAL OWNERSHIP DURING RELEASE

| Responsibility | Owner |
|---|---|
| Player UI release fixes | M1 |
| Creator release fixes | M2 |
| Sensor/location fixes | M3 |
| Scan/reveal fixes | M4 |
| Firebase/security/FCM fixes | M5 |
| Room/sync/integration/build coordination | M6 |

---

# 77. FINAL INTEGRATION COORDINATOR

M6 coordinates:

```text
integration schedule
branch health
release candidate
cross-feature test execution
build verification
final blocker tracking
```

M6 does not absorb ownership of defects belonging to M1–M5.

---

# 78. FINAL DECISION RULE

When a proposed change improves one feature but damages the shared architecture:

```text
shared architecture wins
```

When a proposed shortcut creates:

```text
hard-coded data
duplicate logic
security bypass
cross-game leakage
unrecoverable offline state
```

it should not be accepted merely because it makes the demo faster.

---

# 79. FINAL GOLDEN RULES

```text
1. One shared architecture.
2. One source of truth per responsibility.
3. No silent contract changes.
4. Small commits.
5. Frequent integration.
6. Test before merge.
7. Never commit secrets.
8. Preserve game/user isolation.
9. No hard-coded production checkpoints.
10. Fusion = GPS + Light + Motion.
11. Proximity = separate final gate.
12. No global leaderboard.
13. Repository hides storage details.
14. M6 coordinates integration; owners fix their own defects.
15. Stable main branch only.
```

---

# 80. FINAL WORKFLOW

```text
                 SHARED CONTRACTS
                        │
                        ▼
                     develop
                        │
       ┌────────────────┼────────────────┐
       ▼                ▼                ▼
      M1               M2               M3
 Player UI          Creator          Location/
 Navigation        Management        Sensors/
                                      Fusion
       │                │                │
       └────────────┬───┴───────┬────────┘
                    ▼            ▼
                   M4           M5
                 Quest/       Firebase/
                  Scan        Backend
                    │            │
                    └─────┬──────┘
                          ▼
                         M6
                  Room/Repository/
                  Sync/Integration
                          │
                          ▼
                    Release Build
                          │
                          ▼
                         main
```

---

# 81. END STATE

The team should be able to explain:

```text
Who owns this feature?
What interface does it use?
Where is the data stored?
How does offline behavior work?
What happens when the network fails?
What happens when a sensor is unavailable?
How is game isolation maintained?
How is user isolation maintained?
How is the feature tested?
```

If those answers are clear, the development workflow is controlled.

---

**END OF GIT CHANGE CONTROL & TEAM DEVELOPMENT WORKFLOW**
