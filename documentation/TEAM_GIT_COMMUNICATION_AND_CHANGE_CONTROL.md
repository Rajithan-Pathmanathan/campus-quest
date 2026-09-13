# Campus Quest — Team Git, Communication & Change Control

**Document ID:** CQ-SHARED-07  
**Document:** Team Git, Communication and Change Control  
**Project:** Campus Quest  
**Purpose:** Define the shared rules for source control, branching, pull requests, communication, ownership, integration, approvals, conflict resolution, and technical change management  
**Development window:** 13 September 2026 – 28 September 2026  
**Status:** Team execution specification

---

# 1. Purpose

Campus Quest is being developed by six members working in parallel.

Parallel development is useful only if the team controls:

```text
code ownership
branching
interfaces
shared files
integration
communication
technical changes
```

Without these rules, parallel development can create:

```text
merge conflicts
duplicate work
broken contracts
lost changes
inconsistent data models
integration delays
```

This document defines the team's operating rules.

---

# 2. Development Approach

The team is using an:

> **Agile-inspired iterative development approach**

This means the team will:

- develop independent features in parallel
- integrate incrementally
- test continuously
- use working software as the priority
- use mock data before dependent features are complete
- freeze core functionality before final testing
- avoid building isolated modules until the final day

This is not being presented as strict Scrum unless the team has separately adopted formal Scrum roles/events.

---

# 3. Core Rule

The most important Git rule is:

```text
main must always remain buildable.
```

Do not knowingly push broken code directly into `main`.

---

# 4. Branch Structure

Recommended branch structure:

```text
main
│
├── feature/ui-navigation
├── feature/quest-scan-ui
├── feature/location-geofence
├── feature/sensor-fusion
├── feature/firebase-sync
└── feature/room-data-integration
```

Additional branches may be created when required.

---

# 5. Branch Ownership

| Branch | Primary Owner |
|---|---|
| `feature/ui-navigation` | M1 |
| `feature/quest-scan-ui` | M2 |
| `feature/location-geofence` | M3 |
| `feature/sensor-fusion` | M4 |
| `feature/firebase-sync` | M5 |
| `feature/room-data-integration` | M6 |

The branch owner is responsible for keeping their branch understandable and reviewable.

---

# 6. No Direct Push to Main

Do not use:

```text
git push origin main
```

for normal development.

Instead:

```text
feature branch
 ↓
commit
 ↓
push
 ↓
Pull Request
 ↓
review
 ↓
tests
 ↓
merge
```

---

# 7. Pull Request Rule

Every meaningful feature should be merged through a Pull Request.

A PR should explain:

```text
What changed?
Why?
Which files/modules?
What was tested?
Are there contract changes?
Are there known limitations?
```

---

# 8. Pull Request Template

Use:

```text
## What changed
-

## Why
-

## Testing
- [ ] Build passes
- [ ] Relevant tests pass
- [ ] Manual test completed

## Contract changes
- [ ] No
- [ ] Yes — documented below

## Known issues
-

## Screenshots/video
-
```

---

# 9. Commit Message Format

Use short, meaningful commits.

Recommended:

```text
feat: add quest list
feat: implement geofence registration
feat: add sensor fusion engine
fix: handle missing light sensor
fix: prevent duplicate discovery
test: add fusion threshold tests
refactor: extract quest repository
docs: update Firebase schema
```

---

# 10. Avoid Bad Commit Messages

Avoid:

```text
update
changes
final
test
new
fixed stuff
asdf
```

These messages make project history difficult to understand.

---

# 11. One Logical Change Per Commit

Prefer:

```text
feat: add relic marker model
```

then:

```text
feat: render relic markers
```

rather than one enormous commit containing:

```text
UI
Firebase
Room
GPS
sensors
```

---

# 12. Commit Frequently

Do not wait several days before committing.

Recommended:

```text
small working change
 ↓
commit
 ↓
continue
```

A commit should represent a recoverable point.

---

# 13. Before Starting Work

Each member should begin with:

```text
git checkout main
git pull
git checkout <feature-branch>
git merge main
```

The exact Git commands can vary according to the team's chosen workflow, but the principle is:

```text
start from current shared code
```

---

# 14. Before Creating a PR

The developer should:

```text
pull latest main
resolve conflicts locally
build
run relevant tests
review changed files
push branch
create/update PR
```

---

# 15. Never Commit Secrets

Never commit:

```text
passwords
private API keys
service-account private keys
tokens
Firebase admin credentials
local machine secrets
```

---

# 16. Firebase Configuration

The team should distinguish between:

```text
client configuration required by the Android application
```

and:

```text
server/admin credentials
```

Do not put privileged Firebase credentials inside the Android repository.

M5 is responsible for confirming the final Firebase configuration strategy.

---

# 17. Shared Files

Some files affect several members.

Examples:

```text
data models
repository interfaces
Gradle configuration
navigation
AndroidManifest
Firestore rules
Room database configuration
dependency versions
sensor constants
location constants
```

These require additional care.

---

# 18. Shared File Rule

Before changing a shared file:

```text
check whether another member is working on it
```

If yes:

```text
communicate first
```

Do not silently overwrite their changes.

---

# 19. Shared Data Models

The following are cross-team contracts:

```text
User
Relic
LightSignature
FoundRelic
LeaderboardEntry
LocationResult
DistanceResult
GeofenceEvent
SensorAvailability
MotionState
LightMatch
Proximity
FusionResult
ScanState
```

The exact final Kotlin representation can evolve, but changes must be communicated.

---

# 20. Repository Contract

The agreed conceptual repository boundary includes operations such as:

```kotlin
interface QuestRepository {
    suspend fun getRelics(): List<Relic>
    suspend fun getFusionSignature(relicId: String): LightSignature
    suspend fun recordReveal(relicId: String)
    fun observeLeaderboard(): Flow<List<LeaderboardEntry>>
    suspend fun syncPending()
}
```

This is a shared contract.

If the team changes it, update all affected members before merging.

---

# 21. Contract Change Example

Original:

```kotlin
suspend fun recordReveal(relicId: String)
```

Proposed:

```kotlin
suspend fun recordReveal(
    relicId: String,
    foundAt: Long
)
```

This affects:

```text
M2
M6
M5
```

Therefore the change must be communicated before implementation is merged.

---

# 22. Contract Change Procedure

When a contract must change:

```text
1. Identify reason
2. Identify affected members
3. Communicate proposed change
4. Update shared specification
5. Update mock implementation
6. Update dependent modules
7. Test
8. Merge
```

Do not change the contract silently.

---

# 23. Interface-First Development

Before dependent code is ready, implement against:

```text
interface
```

rather than:

```text
unfinished concrete implementation
```

Example:

```text
M2
 ↓
QuestRepository
 ↓
MockQuestRepository
```

Later:

```text
QuestRepository
 ↓
Firebase/Room implementation
```

This allows UI development to continue in parallel.

---

# 24. Mock-First Rule

If a dependency is not ready:

```text
do not wait
```

Use the canonical mock implementation.

Examples:

```text
M2 uses mock QuestRepository
M3 uses mock relic coordinates
M4 uses mock location values
M1 uses mock authentication
```

Then replace the implementation during integration.

---

# 25. Mock Data Rule

Use the canonical:

```text
MOCK_DATA_CATALOG.md
```

Do not create random IDs independently.

---

# 26. Canonical User IDs

Development mock IDs:

```text
U001
U002
U003
U004
U005
```

These are development/test identifiers.

They should not be confused with Firebase Auth-generated UIDs.

---

# 27. Canonical Relic IDs

Use:

```text
R001
R002
R003
R004
R005
R006
```

Do not create:

```text
relic1
relic_1
founderBell
bell001
```

for the same conceptual relic.

---

# 28. Physical Coordinates

The coordinates in the mock catalog are development coordinates.

They must be physically validated before the final campus demonstration.

Do not assume that a coordinate works physically simply because it works in a mock test.

---

# 29. Sensor Contract Ownership

M4 owns:

```text
sensor availability
light processing
accelerometer processing
proximity processing
fusion calculation
degradation logic
```

M2 owns:

```text
presentation of FusionResult
scan UI
```

Therefore:

```text
M2 should not independently recreate the fusion algorithm.
```

---

# 30. Location Contract Ownership

M3 owns:

```text
GPS
Fused Location Provider
map
distance
geofencing
location permissions
```

M4 consumes location-related input for fusion.

Therefore:

```text
M4 should not create a second competing location system.
```

---

# 31. Proximity Ownership

M4 owns proximity detection.

Important:

```text
Proximity ≠ fusion score
```

The proximity signal is the final reveal gate after the fusion threshold is satisfied.

---

# 32. UI Ownership

M1 owns:

```text
app shell
navigation
theme
common UI structure
```

M2 owns:

```text
quest experience
scan experience
reveal experience
```

M2 should reuse the shared navigation/theme patterns instead of creating a second application shell.

---

# 33. Firebase Ownership

M5 owns:

```text
Firebase project
Auth
Firestore
security rules
cloud repository implementation
cloud seed data
```

M5 should coordinate with M6 for synchronization.

---

# 34. Room Ownership

M6 owns:

```text
Room database
entities
DAOs
local repository
pending synchronization
```

M6 should not redefine cloud schemas independently.

---

# 35. Integration Ownership

M6 coordinates integration because M6 owns the Room/local data boundary and has broad cross-module visibility.

However:

```text
M6 does not integrate everybody's code alone.
```

Every member remains responsible for integrating and fixing their own feature.

---

# 36. Integration Boundary Map

```text
M1 ↔ M5
Authentication

M2 ↔ M5
Quest/progress/leaderboard cloud data

M2 ↔ M6
Local discovery/progress

M3 ↔ M4
Distance/location input

M4 ↔ M2
FusionResult/ScanState

M4 ↔ M5
Relic light signature

M5 ↔ M6
Cloud synchronization

M3 ↔ M5/M6
Relic coordinate data
```

---

# 37. Communication Before Integration

Before connecting two modules:

```text
confirm input
confirm output
confirm IDs
confirm error states
confirm loading states
confirm ownership
```

Do not integrate based on assumptions.

---

# 38. Communication Channel

The team should use one primary communication channel for technical decisions.

Possible:

```text
WhatsApp group
Discord
Slack
GitHub Discussions
```

Choose one.

The team should not distribute critical decisions across private conversations where other members cannot see them.

---

# 39. Technical Decision Format

Use:

```text
[TECH DECISION]

Topic:
Decision:
Reason:
Affected members:
Effective date:
Specification updated:
```

Example:

```text
[TECH DECISION]

Topic: Fusion threshold
Decision: Use final agreed threshold from M4 calibration
Reason: Physical device calibration
Affected: M2, M4
Specification: Updated
```

---

# 40. Decision Log

Maintain a lightweight decision log.

Suggested columns:

| Date | Decision | Reason | Members | Document |
|---|---|---|---|---|
| | | | | |

---

# 41. No Silent Breaking Changes

A breaking change is anything that can cause another member's code to stop compiling or behave incorrectly.

Examples:

```text
rename Relic.id
change repository method
change Firestore path
change scan state
change fusion result fields
change Room entity
```

Communicate first.

---

# 42. Non-Breaking Changes

Examples:

```text
internal refactoring
private helper method
UI spacing
internal variable rename
```

These may not require a team-wide decision if no contract is affected.

---

# 43. Architecture Changes

The following should be discussed before implementation:

```text
changing MVVM boundaries
removing repository layer
changing Room/Firestore responsibility
moving sensor logic into UI
changing cloud/local source of truth
adding a new persistence system
```

The architecture should remain:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firestore
```

The architecture lecture materials emphasize separation of concerns and repository responsibility. fileciteturn11file0L178-L190

---

# 44. No Business Logic in UI

Avoid placing:

```text
fusion calculations
Firestore writes
Room queries
complex location calculations
```

directly inside Activities/Fragments/Composables.

The UI should primarily:

```text
display state
send user actions
```

---

# 45. ViewModel Responsibility

ViewModels should coordinate:

```text
UI state
user actions
repository calls
```

They should not become a dumping ground for unrelated hardware implementation.

---

# 46. Repository Responsibility

Repositories coordinate data sources:

```text
Room
Firestore
mock data
```

The repository should hide the implementation details from the ViewModel.

---

# 47. Code Review Checklist

Before merging:

- [ ] Builds successfully
- [ ] No obvious crashes
- [ ] Naming is understandable
- [ ] No secrets committed
- [ ] No duplicated major logic
- [ ] Correct repository boundary
- [ ] Lifecycle handling considered
- [ ] Error state considered
- [ ] Mock data uses canonical IDs
- [ ] Tests added/updated
- [ ] Documentation updated if contract changed

---

# 48. PR Review Responsibility

At least one other member should review meaningful cross-module changes.

For critical changes, preferably involve the affected owner.

Examples:

```text
Fusion interface → M4 + M2
Firebase schema → M5 + M6
Room schema → M6 + affected repository consumers
Navigation → M1 + M2
Location contract → M3 + M4
```

---

# 49. Merge Rule

A PR can be merged when:

```text
code is understandable
+
build passes
+
relevant tests pass
+
affected members are aware of contract changes
```

---

# 50. Conflict Resolution

When Git reports a conflict:

```text
do not blindly choose "ours"
do not blindly choose "theirs"
```

First determine:

```text
What changed?
Why?
Which version matches the current contract?
```

---

# 51. Conflict Resolution Procedure

```text
1. Stop
2. Identify conflicting files
3. Identify owners
4. Compare both changes
5. Decide intended final behaviour
6. Resolve manually
7. Build
8. Test affected feature
9. Continue
```

---

# 52. High-Risk Conflict Files

Extra care is required for:

```text
AndroidManifest.xml
build.gradle / build.gradle.kts
settings.gradle
navigation
shared data models
repository interfaces
Room database
Firestore configuration
```

---

# 53. Gradle Changes

Do not independently change:

```text
dependency versions
plugins
compile SDK
target SDK
Kotlin version
```

without communication.

One member changing the build configuration can break everyone's branch.

---

# 54. Dependency Addition

Before adding a dependency, communicate:

```text
Library:
Version:
Purpose:
Why existing libraries are insufficient:
Affected modules:
```

Avoid unnecessary dependencies because the project has a short implementation window.

---

# 55. Android Manifest Changes

M3 and M4 may require permissions.

Examples:

```text
location
foreground/background behaviour where applicable
```

M5/M1 may also have configuration requirements.

Coordinate changes to avoid overwriting another member's manifest work.

---

# 56. Sensor Permission/Capability Changes

If Android behaviour requires a permission or manifest configuration:

```text
M4 proposes
M1/M3/M5 review if affected
```

Do not assume hardware access automatically works on every Android version/device.

---

# 57. Firestore Schema Changes

M5 must communicate changes to:

```text
collection names
document paths
field names
field types
security rules
```

before dependent code is updated.

---

# 58. Room Schema Changes

M6 must communicate changes to:

```text
table/entity names
columns
primary keys
indices
relations
migration strategy
```

if already used by other code.

---

# 59. Mock Repository Changes

If the real repository changes:

```text
mock repository
```

must be updated as well.

Otherwise UI development may silently diverge from the real implementation.

---

# 60. Feature Flags / Temporary Mocks

Temporary implementations should be clearly identified.

Example:

```text
// DEVELOPMENT MOCK
```

Do not accidentally submit a fake implementation as production functionality.

---

# 61. Debug Logging

Debug logging may be used during development.

Avoid logging:

```text
passwords
authentication tokens
private user data
sensitive credentials
```

Remove unnecessary verbose logging before final release.

---

# 62. Commit Before Experimenting

Before a risky refactor:

```text
commit current working state
```

Then experiment.

This provides a recovery point.

---

# 63. Experimental Branches

For uncertain work:

```text
experiment/<name>
```

Example:

```text
experiment/camera-relic
```

Do not destabilize the main feature branch while evaluating a stretch feature.

---

# 64. Stretch Feature Rule

Stretch features must not destabilize the MVP.

Potential stretch features include:

```text
CameraX legendary relic
team relay
crowd-aware routing
badges/rarity expansion
cross-platform proof-of-concept
```

If the MVP is unstable:

```text
cut stretch features
```

---

# 65. Feature Freeze

**23 September 2026** is the target hard feature-freeze point.

After feature freeze:

```text
no new core features
```

Only:

```text
bug fixes
testing
performance/refinement
documentation
demo preparation
```

---

# 66. Why Feature Freeze Exists

Without a freeze:

```text
new feature
 ↓
new code
 ↓
new bug
 ↓
integration
 ↓
new bug
 ↓
less testing time
```

The team has a limited schedule.

Testing time must be protected.

---

# 67. Change Requests After Freeze

After 23 September, classify every requested change:

```text
Critical bug
High-priority bug
Required academic requirement
Cosmetic improvement
New feature
```

Only the first three should normally be considered.

---

# 68. Post-Freeze Change Approval

For a significant post-freeze change:

```text
Change:
Reason:
Risk:
Affected modules:
Testing required:
Rollback plan:
Decision:
```

At least the affected members must agree.

---

# 69. Rollback Principle

If a new change creates more risk than value:

```text
rollback
```

Do not keep a broken feature merely because it is newer.

---

# 70. Definition of Ready

A feature is ready to start when:

- [ ] owner identified
- [ ] expected behaviour defined
- [ ] dependencies known
- [ ] interface known or agreed
- [ ] mock data available
- [ ] acceptance criteria defined
- [ ] no unresolved blocker

---

# 71. Definition of Done

A feature is done when:

- [ ] implementation complete
- [ ] relevant tests pass
- [ ] UI state handled
- [ ] error state handled
- [ ] lifecycle considered
- [ ] mock/real integration tested as appropriate
- [ ] code reviewed
- [ ] PR merged
- [ ] documentation updated if necessary

---

# 72. Module-Specific Definition of Done

## M1

```text
App launches
Navigation works
Core screens reachable
Theme consistent
Auth UI works
```

## M2

```text
Quest list works
Scan UI works
Fusion result displayed
Reveal flow works
```

## M3

```text
Map works
Location works
Geofence works
Permission states handled
```

## M4

```text
Sensors detected
Fusion works
Degradation works
Proximity gate works
```

## M5

```text
Firebase works
Auth works
Firestore works
Security rules tested
```

## M6

```text
Room works
Offline persistence works
Sync works
Integration stable
```

---

# 73. Daily Stand-Up Format

Each member reports:

```text
Yesterday:
Today:
Blocked by:
```

Keep it short.

Example:

```text
M4
Yesterday:
Completed light matching.

Today:
Integrate location input.

Blocked by:
Waiting for M3's DistanceResult contract.
```

---

# 74. Blocker Rule

A blocker is something that prevents meaningful progress.

Examples:

```text
cannot build project
missing Firebase configuration
unknown repository contract
required device unavailable
broken shared model
```

When blocked:

```text
report immediately
```

Do not wait several days hoping it resolves itself.

---

# 75. Blocker Message Format

```text
[BLOCKER]

Member:
Feature:
Blocked by:
What I need:
Impact:
```

---

# 76. Dependency Waiting Rule

If waiting for another member:

```text
use mock data
```

where possible.

Example:

M2 is waiting for M4's real fusion engine.

M2 should use:

```text
MockFusionResult
```

and continue UI development.

---

# 77. Handoff Format

When a module is ready for another member:

```text
[HANDOFF]

From:
To:
Feature:
Branch/PR:
Interface:
Input:
Output:
How to test:
Known limitations:
Required follow-up:
```

---

# 78. Example Handoff

```text
[HANDOFF]

From: M4
To: M2
Feature: Sensor Fusion
Branch: feature/sensor-fusion
Output: FusionResult
Input: DistanceResult, Light reading, MotionState
How to test:
Use R001 mock scenario
Known limitation:
Weights require physical calibration
```

---

# 79. Handoff Is Not Completion

A handoff means:

```text
"Here is the agreed interface and implementation."
```

It does not mean:

```text
"The receiving member has no responsibility."
```

The receiver must integrate and test.

---

# 80. Integration Meeting

Use short integration sessions around:

```text
19 Sep
22 Sep
23 Sep
27 Sep
```

Agenda:

```text
1. Current build status
2. Integration blockers
3. Contract changes
4. Failed tests
5. Next integration target
```

---

# 81. 13–16 September

Focus:

```text
repository contracts
mock data
navigation
Firebase setup
Room setup
map foundation
sensor foundation
```

Git priority:

```text
small commits
stable branches
no unnecessary shared-file conflicts
```

---

# 82. 17–19 September

Focus:

```text
feature completion
```

Begin cross-module PRs.

Target:

```text
mock-based end-to-end navigation
```

---

# 83. 20–21 September

Focus:

```text
real implementations replacing mocks
```

Examples:

```text
M5 Firebase → M2/M6
M3 location → M4
M4 fusion → M2
M6 Room → M2
```

---

# 84. 22 September

Target:

```text
real sensor scan integration
```

All relevant members should test together.

---

# 85. 23 September

Target:

```text
MVP integration complete
```

Then:

```text
hard feature freeze
```

---

# 86. 24–27 September

Focus only on:

```text
testing
bug fixes
compatibility
offline behaviour
security
performance
documentation
```

---

# 87. 28 September

Focus:

```text
final build
final regression
demo rehearsal
submission package
```

---

# 88. Emergency Integration Rule

If the project is behind schedule:

```text
reduce scope
```

Do not respond by:

```text
adding more people to every file
```

or:

```text
merging everything without review
```

---

# 89. Behind-Schedule Priority

Priority order:

```text
1. Build stability
2. Login
3. Navigation
4. Map/location
5. Geofence
6. Sensor fusion
7. Proximity reveal
8. Room persistence
9. Firebase sync
10. Leaderboard
11. UI polish
12. Stretch features
```

---

# 90. One Reliable Relic Rule

If time becomes severely limited:

```text
one complete reliable relic
```

is better than:

```text
six incomplete relics
```

The complete core loop must remain demonstrable.

---

# 91. Emergency Branch Strategy

If `main` becomes broken:

```text
1. Stop merging unrelated PRs
2. Identify breaking PR
3. Revert if necessary
4. Restore build
5. Re-test smoke suite
6. Reapply changes carefully
```

---

# 92. Revert Rule

A revert is appropriate when:

```text
merged change breaks main
```

and immediate repair is not safe.

Do not treat reverting as failure.

A stable main branch is more important.

---

# 93. No Force Push on Shared Branches

Avoid:

```text
git push --force
```

on:

```text
main
```

or another branch actively used by multiple members.

If history rewriting is absolutely necessary, coordinate first.

---

# 94. Branch Cleanup

After successful merge:

```text
delete obsolete feature branch
```

if the team agrees.

Keep repository history understandable.

---

# 95. Repository Organization

Recommended conceptual structure:

```text
app/
  ui/
  navigation/
  viewmodel/
  domain/
  data/
    local/
    remote/
    repository/
  location/
  sensors/
```

The exact package structure may differ, but responsibilities should remain separated.

---

# 96. Naming Rule

Use descriptive names.

Examples:

```text
QuestRepository
SensorFusionEngine
LocationRepository
RelicEntity
FoundRelicEntity
FusionResult
```

Avoid:

```text
Repo
Helper
Manager2
DataStuff
SensorThing
```

---

# 97. Kotlin Style

Follow the project's selected Kotlin/Android style consistently.

Examples:

```text
PascalCase → classes
camelCase → functions/properties
UPPER_SNAKE_CASE → constants where appropriate
```

Do not mix naming conventions across modules.

---

# 98. Error Handling

Cross-module operations should have predictable error states.

Examples:

```text
Loading
Success
Empty
Error
Offline
Limited
```

Do not make each screen invent unrelated error behaviour.

---

# 99. Sensor Errors

Sensor failures should be represented as meaningful application states.

Examples:

```text
sensor unavailable
limited mode
permission unavailable
measurement unavailable
```

Do not crash because:

```text
SensorManager.getDefaultSensor(...)
```

returns `null`.

---

# 100. Location Errors

Handle:

```text
permission denied
GPS unavailable
poor accuracy
location unavailable
geofence registration failure
```

---

# 101. Firebase Errors

Handle:

```text
network unavailable
permission denied
document missing
write failure
timeout
authentication failure
```

---

# 102. Room Errors

Handle:

```text
database access failure
duplicate insert
migration problem
empty result
```

---

# 103. Communication During Bugs

Do not write:

```text
"your code is broken"
```

Use:

```text
"M4 fusion result is currently not reaching the ScanViewModel on Device B. Can we check the interface mapping?"
```

The goal is to solve the technical issue, not assign blame.

---

# 104. Technical Ownership vs Blame

Ownership means:

```text
who investigates first
```

It does not mean:

```text
who is personally responsible for failure
```

Integration bugs may involve several members.

---

# 105. Evidence of Technical Decisions

When a decision changes the implementation, update at least one shared location:

```text
shared specification
decision log
Git commit
PR description
```

Do not rely only on memory.

---

# 106. Documentation Sync

When code changes a documented contract:

```text
code
+
documentation
```

must be updated.

Documentation should not describe an interface that no longer exists.

---

# 107. Versioning of Shared Specifications

Use Git history to track specification changes.

Optional version format:

```text
v1.0
v1.1
v1.2
```

Do not manually maintain complicated version numbers unless useful.

The important requirement is traceability.

---

# 108. Change Log Format

Use:

| Date | Document | Change | Reason | Owner |
|---|---|---|---|---|
| | | | | |

---

# 109. Example Change Log Entry

```text
2026-09-22
Sensor specification
Adjusted light signature after physical calibration
Reason: real campus reading differs from initial mock
Owner: M4
```

---

# 110. Physical Calibration Changes

Sensor thresholds are particularly likely to change after real-device testing.

If a light range changes:

```text
old range
 ↓
physical measurements
 ↓
new range
 ↓
update mock data
 ↓
update Firebase
 ↓
rerun fusion tests
```

---

# 111. Fusion Threshold Changes

Changing the fusion threshold affects:

```text
M4
M2
testing
demo behaviour
```

Therefore communicate and retest.

---

# 112. Geofence Radius Changes

Changing:

```text
radiusM
```

affects:

```text
M3
M4
physical test
mock data
Firebase
```

Retest boundary cases.

---

# 113. Firestore Path Changes

Changing:

```text
progress/{uid}/found/{relicId}
```

requires:

```text
M5 update
M6 sync update
security-rule update
tests update
```

Do not change only the repository code.

---

# 114. Room Field Changes

Changing:

```text
pendingSync
```

or another local field requires checking:

```text
DAO
repository
sync worker/process
tests
```

---

# 115. Source of Truth

Use this distinction:

```text
Room
= local/offline persistence

Firestore
= shared/cloud persistence
```

The application should not create two competing truths for the same cloud progress.

---

# 116. Sync Principle

For discovery:

```text
local save first
 ↓
mark pending
 ↓
cloud sync
 ↓
clear pending
```

This prevents a temporary network failure from losing a successful discovery.

---

# 117. Duplicate Prevention

Use stable IDs:

```text
user UID
+
relic ID
```

rather than generating a new random record every time a user discovers a relic.

---

# 118. Auth ID Clarification

The mock catalog uses:

```text
U001
```

as a development identifier.

Firebase Auth normally provides its own UID.

The implementation must define how the display/development identity maps to the real Firebase user.

---

# 119. No Cross-Module Duplication

Avoid multiple implementations of:

```text
distance calculation
fusion calculation
relic lookup
progress counting
```

There should be one authoritative implementation for each core responsibility.

---

# 120. Review Red Flags

Reject or request changes when a PR introduces:

```text
Firebase calls directly from UI
Room calls directly from UI
sensor processing inside Activity
duplicated fusion logic
hard-coded production credentials
random relic IDs
silent contract changes
large unrelated refactor
```

---

# 121. Large Refactor Rule

Avoid large refactors after:

```text
23 September
```

unless required to fix a critical issue.

---

# 122. New Feature Request Format

Before accepting a new feature:

```text
Feature:
User value:
Required modules:
Estimated effort:
Dependencies:
Testing effort:
Risk:
Core or stretch:
```

---

# 123. Feature Approval

Ask:

```text
Does this help the assessed core requirements?
```

If no:

```text
stretch
```

If the MVP is incomplete:

```text
do not implement
```

---

# 124. Change Decision Matrix

| Situation | Action |
|---|---|
| P0 bug | Immediate |
| P1 bug | High priority |
| Required assessment feature | Prioritize |
| Contract bug | Fix before integration |
| Cosmetic issue | Later |
| New stretch feature | Only if MVP stable |
| Post-freeze new feature | Normally reject |

---

# 125. Final Branch State

By 28 September:

```text
main
```

should contain:

```text
latest stable MVP
```

not experimental branches.

---

# 126. Final Build Tag

If practical, create a release tag such as:

```text
v1.0.0-demo
```

or:

```text
v1.0.0-submission
```

after the final accepted build.

---

# 127. Final Build Record

Record:

```text
Commit:
Tag:
APK version:
Build date:
Primary device:
Android version:
Test result:
```

---

# 128. Final APK Rule

Do not submit an APK built from:

```text
uncommitted local changes
```

The final APK should correspond to a known Git commit.

---

# 129. Final Repository Check

Before submission:

- [ ] `main` builds
- [ ] no accidental secrets
- [ ] no irrelevant large files
- [ ] no broken experimental branch merged
- [ ] README available
- [ ] specifications updated
- [ ] final commit identified
- [ ] final APK corresponds to commit

---

# 130. Final Communication Checklist

Before final submission, confirm:

```text
[ ] All members know final branch/commit
[ ] All members know final APK version
[ ] All known P0/P1 bugs reviewed
[ ] Demo device confirmed
[ ] Firebase project confirmed
[ ] Test evidence collected
[ ] Documentation collected
```

---

# 131. Team Working Rule

The team should optimize for:

```text
shared visibility
+
small changes
+
clear ownership
+
early integration
+
fast feedback
```

not:

```text
individual perfection
+
late integration
```

---

# 132. Final Operating Model

The complete development loop is:

```text
Plan
 ↓
Create branch
 ↓
Implement
 ↓
Commit
 ↓
Test
 ↓
Push
 ↓
Pull Request
 ↓
Review
 ↓
Merge
 ↓
Integration test
 ↓
Regression test
```

Repeat throughout development.

---

# 133. Final Principle

The project should behave as one application, not six separate projects.

Therefore:

```text
M1 owns UI structure
M2 owns quest interaction
M3 owns location
M4 owns sensor fusion
M5 owns Firebase
M6 owns local data/integration
```

but:

```text
all six members own the success of the integrated application.
```

---

# 134. Final Checklist

## Git

- [ ] Feature branches used
- [ ] No routine direct pushes to main
- [ ] Meaningful commits
- [ ] PRs reviewed
- [ ] Main remains buildable
- [ ] Conflicts resolved intentionally
- [ ] No secrets committed

## Communication

- [ ] One primary technical communication channel
- [ ] Blockers reported immediately
- [ ] Technical decisions recorded
- [ ] Handoffs documented
- [ ] Contract changes communicated

## Architecture

- [ ] UI → ViewModel → Repository → Room/Firestore
- [ ] Sensor logic separated
- [ ] Location logic separated
- [ ] Shared contracts respected
- [ ] No duplicated core business logic

## Integration

- [ ] Mock-first development used
- [ ] Cross-module boundaries tested
- [ ] Real implementations integrated before final week
- [ ] End-to-end MVP tested
- [ ] Feature freeze observed

## Final

- [ ] Stable main branch
- [ ] Final commit identified
- [ ] Final APK built from known commit
- [ ] Regression suite passed
- [ ] Demo device validated
- [ ] Documentation synchronized
- [ ] No unresolved critical issue

---

# 135. Team Agreement

By following this document, the six members agree that:

1. `main` represents the stable shared application.
2. Feature branches are used for normal development.
3. Meaningful changes go through review.
4. Shared contracts are changed deliberately.
5. Mock data enables parallel development.
6. Integration begins before the end of the project.
7. Testing is continuous rather than postponed to the final day.
8. The MVP has priority over stretch features.
9. Post-freeze changes are tightly controlled.
10. The final submission must correspond to a known, tested Git state.

