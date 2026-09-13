# CAMPUS QUEST
## Detailed Team Task Schedule, Deadlines & Blocker Plan

**Development window:** 13 September 2026 – 28 September 2026  
**Team:** 6 members  
**Architecture:** Dynamic Game Creator + Game Player  
**Platform:** Native Android / Android Studio  
**Core:** Room + Firestore + FCM + GPS/Geofence + Ambient Light + Accelerometer/Motion + Proximity + Sensor Fusion

---

## 1. Purpose

This document converts the final M1–M6 ownership model into a **day-by-day execution schedule**.

Every major task has:

- a responsible member;
- a completion deadline;
- dependencies;
- downstream blockers;
- integration milestones;
- testing and release responsibilities.

A task marked **BLOCKER** means the responsible member must provide the agreed contract/interface or working implementation by that deadline. A dependent member may continue with mocks where possible, but integration cannot be considered complete until the blocker is cleared.

---

# 2. Final Member Ownership

| Member | Primary responsibility | Must deliver | Main downstream dependents |
|---|---|---|---|
| **M1** | Player UI & Navigation Lead | App shell, player games list, details, join, player navigation, leaderboard UI, notification deep links, shared UI components | M2, M4, M5; whole-team integration |
| **M2** | Creator & Game Management | Creator entry, game creation/editing, checkpoint CRUD/reorder/configuration, drafts, validation, publish UI | M1, M5, M6, M3 |
| **M3** | Location + Sensors + Fusion | Maps, location, permissions, dynamic geofences, ambient light, accelerometer/motion, proximity, normalization, weighted fusion | M4, M6; final gameplay |
| **M4** | Quest & Scan Gameplay | Checkpoint gameplay, scan HUD, fusion meter/status, scan state machine, proximity gate UI, reveal/discovery UI | M1, M3, M5, M6 |
| **M5** | Firebase & Backend | Auth, Firestore, security rules, games/checkpoints/membership/progress/leaderboards, publishing, FCM, cloud repository | M1, M2, M4, M6 |
| **M6** | Room + Sync + Integration | Room entities/DAOs, local repository, cache, pending sync, retries/idempotency, migrations, integration builds and final E2E | All members |

---

# 3. Non-Negotiable Architecture Rules

| Rule | Requirement |
|---|---|
| **Dynamic data** | Do not build production logic around fixed R001–R006 checkpoints. Those IDs are seed/demo data only. |
| **Game scope** | Checkpoint, progress, membership and leaderboard operations must always be scoped by `gameId`. |
| **Sensor flow** | GPS + ambient light + accelerometer/motion feed the weighted fusion score. Proximity is a separate final close-range gate, not a fourth fusion weight. |
| **UI boundaries** | M1/M2/M4 UI code must not directly own Firebase, Room, SensorManager or location-provider internals. Use ViewModels/repository/contracts. |
| **Offline** | Successful discoveries must be retained locally and marked for synchronization when connectivity is unavailable. |
| **Publishing** | A creator publishes only after validation. Published games become player-discoverable and trigger the new-game notification flow. |
| **Leaderboard** | There is one leaderboard per game; no global leaderboard is required. |
| **Integration** | Every member works against shared contracts and reports blockers immediately rather than waiting for final integration. |

---

# 4. Master Calendar

| Date | M1 — Player UI | M2 — Creator | M3 — Location/Sensors/Fusion | M4 — Quest/Scan | M5 — Firebase | M6 — Room/Sync/Integration |
|---|---|---|---|---|---|---|
| **13 Sep** | Confirm navigation graph, screen list and shared UI contracts. | Confirm creator flow and checkpoint form fields. | Confirm physical-signal interfaces and map/geofence requirements. | Confirm scan state machine and reveal states. | Confirm Firestore schema, auth and security boundaries. | Confirm Room entities, repository boundary and integration branch. **TEAM CHECKPOINT:** freeze contracts. |
| **14 Sep** | App shell + theme + navigation skeleton. | Creator entry + create-game screen skeleton. | Location permission + FLP abstraction skeleton. | Checkpoint gameplay + scan screen skeleton. | Firebase project/config + Auth skeleton. | Room database + entity/DAO skeleton. **BLOCKER:** shared IDs, Game/Checkpoint models and navigation args agreed. |
| **15 Sep** | Games list + game card + loading/empty/error states. | Game title/description + draft model + validation. | Map screen + current location + distance calculation. | Scan HUD layout + state rendering from mock signals. | `games/checkpoints` Firestore read/write. | Game/Checkpoint local cache + DAOs. **BLOCKER:** M5 schema and M6 model match M2 dynamic model. |
| **16 Sep** | Game details + checkpoint count + creator info. | Checkpoint add/edit: coordinates, radius, clue, lore. | Dynamic checkpoint markers and game-scoped geofence registration. | GPS/light/motion status cards and fusion meter. | `gamePlayers` join flow + ownership/security rules. | Repository interface + local read/write orchestration. **BLOCKER:** M3 exposes dynamic checkpoint/geofence contract; M5 exposes join contract. |
| **17 Sep** | Join game flow + navigation to player map. | Light signature + rarity + motion configuration; reorder/delete. | Ambient light + accelerometer/motion + proximity abstractions. | Scan state machine: entering → ready → scanning → threshold → proximity → reveal. | Progress + leaderboard write/read contracts. | Offline discovery + pending-sync model. **BLOCKER:** M3 signal outputs and M4 scan-state contract become integration interfaces. |
| **18 Sep** | Leaderboard UI + per-game ranking states. | Draft list/edit/resume + publish validation UI. | Normalization + weighted fusion + missing-sensor reweighting. | Fusion meter driven by M3 interface; proximity final-gate presentation. | FCM `new_games` topic + notification payload/deep-link support. | Room transaction for discovery + pending sync. **BLOCKER:** M3 deterministic fusion fixtures; M5 notification data shape. |
| **19 Sep** | Notification deep link → correct game details. | Publish confirmation + success/error states. | Physical-device GPS/geofence/light/motion/proximity test. | Reveal dialog + clue/lore + discovery completion. | Firestore security + multi-user/game isolation tests. | Retry/idempotency + Room/Firestore sync. **BLOCKER:** first complete golden-path integration runnable. |
| **20 Sep** | UI polish: responsive layouts, accessibility, list performance. | Creator validation and edge cases. | Lifecycle/background/battery/degraded-sensor handling. | Duplicate discovery, lifecycle and offline scan behavior. | Cloud repository stabilization. | **First full integration build**; resolve compile/API conflicts. **INTEGRATION GATE 1:** login → games → details → join → map → scan. |
| **21 Sep** | Player E2E + leaderboard states. | Create a new game with a non-fixed checkpoint count. | Dynamic geofences generated from that game's checkpoints. | Scan dynamically created checkpoint. | Publish created game → player discovery. | Cache created game/checkpoints + discovery locally. **BLOCKER TEST:** no dependency on R001–R006. |
| **22 Sep** | Notification tap → correct game; polish errors/loading. | Draft → edit → publish complete. | Calibration and false-positive tests. | Complete scan/reveal acceptance tests. | FCM + publish → notification → deep-link E2E. | Offline → online sync E2E. **INTEGRATION GATE 2:** creator-to-player pipeline works. |
| **23 Sep** | Fix integration/UI bugs; freeze navigation APIs. | Fix validation/publish bugs; freeze creator APIs. | Fix physical-signal bugs; freeze sensor APIs. | Fix scan/reveal bugs; freeze gameplay APIs. | Fix backend/security/sync bugs; freeze cloud APIs. | Full integration + regression. **FEATURE FREEZE:** bug fixes only after this date. |
| **24 Sep** | Regression + accessibility + orientation/device checks. | Dynamic checkpoint count tests. | Physical-device regression + lifecycle tests. | Scan-state and reveal regression. | Security/isolation/FCM regression. | Room/repository/sync regression + clean build. **BLOCKER:** all P0/P1 defects assigned. |
| **25 Sep** | Final UI/UX polish. | Final creator UI polish. | Final sensor tuning + battery review. | Final scan HUD/reveal polish. | Final Firestore/FCM cleanup. | **RC-1 release candidate**. |
| **26 Sep** | Demo script + player evidence. | Creator evidence. | Physical sensor/geofence demo + fallback fixtures. | Scan/reveal demo. | Publish/notification/security demo. | Offline/sync + final integration evidence. **DEMO REHEARSAL.** |
| **27 Sep** | Final bug fixes only. | Final bug fixes only. | Final physical-device verification. | Final gameplay verification. | Final backend/security verification. | Final regression + release build. **RELEASE FREEZE.** |
| **28 Sep** | Final UI walkthrough. | Creator workflow walkthrough. | Explain location/sensor/fusion. | Explain scan/reveal. | Explain Firebase/FCM/security. | Present integration, Room/offline/sync and final evidence. **FINAL.** |

---

# 5. Member 1 — Player UI & Navigation

**Branch:** `feature/m1-player-ui-navigation`

**Goal:** Deliver the complete player-facing application shell and navigation surfaces while consuming ViewModel/repository contracts rather than implementing data or sensor internals.

| Date | Task | Definition of Done | Dependencies | If Late / Blocker Impact |
|---|---|---|---|---|
| 13 Sep | Navigation contract + screen inventory | Navigation graph, route names and `gameId/checkpointId` arguments agreed | M2/M4/M5 | Wrong route arguments can block player flows |
| 14 Sep | App shell + theme | Main Activity, navigation host, shared theme/components | All | UI shell is base for every feature |
| 15 Sep | Games list | Recycler/list, cards, loading/empty/error states | M5/M6 | Player cannot discover dynamic games |
| 16 Sep | Game details | Title, creator, description, checkpoint count, join CTA | M5 | Details/join contract |
| 17 Sep | Join + player map navigation | Join result handling and game-scoped map navigation | M5/M3 | Membership/map dependency |
| 18 Sep | Leaderboard UI | Game-specific ranking states | M5 | Leaderboard cannot be integrated |
| 19 Sep | Notification deep link | New-game notification opens correct game details | M5 | Notification E2E blocked |
| 20 Sep | Responsive/accessibility polish | Touch targets, readability, orientation, loading/error consistency | All | Design consistency risk |
| 21 Sep | Player E2E | Login → games → details → join → map | M5/M3 | Integration Gate 1 |
| 22 Sep | Notification E2E + polish | Publish → notification → game details | M5 | Integration Gate 2 |
| 23 Sep | API/UI freeze | Only bug fixes after contract freeze | All | Prevents late navigation changes |
| 24 Sep | Regression | Player flows, orientation, accessibility | All | P0/P1 defects |
| 25 Sep | Final UI polish | Approved design and no placeholder UI | M4/M2 | RC risk |
| 26 Sep | Demo evidence | Player screenshots/video/script | All | Demo rehearsal |
| 27 Sep | Final bug fixes | Critical fixes only | All | Release freeze |
| 28 Sep | Presentation support | Player walkthrough | All | Final demo |

---

# 6. Member 2 — Creator & Game Management

**Branch:** `feature/m2-creator-game-management`

**Goal:** Deliver the complete creator workflow for dynamic games/checkpoints, including drafts, configuration, validation and publishing.

**Important:** M2 no longer owns Quest/Scan UI.

| Date | Task | Definition of Done | Dependencies | If Late / Blocker Impact |
|---|---|---|---|---|
| 13 Sep | Creator contract | Game/Checkpoint fields and validation rules frozen | M1/M5/M6 | Model mismatch blocks persistence/UI |
| 14 Sep | Creator entry + create game | Creator dashboard + create-game form | M1 | Creator navigation |
| 15 Sep | Game metadata + draft | Title, description, draft state, save/edit | M5/M6 | Persistence |
| 16 Sep | Checkpoint CRUD | Add/edit checkpoint, coordinates, radius, clue/lore | M3/M5/M6 | Dynamic checkpoint source |
| 17 Sep | Advanced checkpoint config | Light range, rarity, motion type, reorder/delete | M3/M5/M6 | M3 fusion configuration |
| 18 Sep | Draft management + validation | Draft list, resume editing, validation before publish | M5/M6 | Publish gate |
| 19 Sep | Publish UI | Confirmation, validation errors, success/failure | M5 | Publishing backend |
| 20 Sep | Creator edge cases | Invalid coordinates, duplicate order, empty fields | M5/M6 | Data quality |
| 21 Sep | Dynamic game test | Create game with non-fixed checkpoint count | M3/M5/M6 | Proves dynamic architecture |
| 22 Sep | Creator E2E | Create → checkpoints → draft → publish | M5/M6 | Integration Gate 2 |
| 23 Sep | API/UI freeze | Bug fixes only | All | Feature freeze |
| 24 Sep | Regression | Dynamic count, edit, delete, reorder, publish | M5/M6 | P0/P1 defects |
| 25 Sep | Final polish | Approved creator UI/errors | M1 | RC |
| 26 Sep | Demo evidence | Creator workflow demonstration | M5/M6 | Demo rehearsal |
| 27 Sep | Final bug fixes | Critical fixes only | All | Release freeze |
| 28 Sep | Presentation support | Explain creator workflow/dynamic data | All | Final demo |

---

# 7. Member 3 — Location + Sensors + Fusion

**Branch:** `feature/m3-location-sensors-fusion`

**Goal:** Own all physical-signal acquisition and processing: location, geofencing, ambient light, accelerometer/motion, proximity, normalization and weighted fusion.

| Date | Task | Definition of Done | Dependencies | If Late / Blocker Impact |
|---|---|---|---|---|
| 13 Sep | Signal contracts | `LocationState`, sensor states, `FusionResult`, lifecycle rules | M4/M5/M6 | M4 cannot implement real scan |
| 14 Sep | FLP + permissions | Location abstraction + runtime permissions | M1/M4 | Map/scan dependency |
| 15 Sep | Map + distance | Current location, target distance, accuracy handling | M1/M4 | Player map/scan |
| 16 Sep | Dynamic geofencing | Register/unregister game-scoped checkpoint geofences | M2/M4/M6 | Fixed checkpoint architecture risk |
| 17 Sep | Light + motion + proximity | Ambient light, accelerometer/sweep, proximity abstractions | M4 | Scan inputs |
| 18 Sep | Fusion engine | Normalization, weighted score, missing-sensor reweighting, threshold | M4 | Core mechanic blocked |
| 19 Sep | Physical-device validation | GPS/geofence/light/motion/proximity tests | M4/M6 | Physical golden path |
| 20 Sep | Lifecycle/battery/degradation | Pause/resume, background behavior, sensor availability | M4/M6 | Stability |
| 21 Sep | Dynamic checkpoint validation | Geofences from creator-created checkpoints | M2/M5/M6 | Dynamic proof |
| 22 Sep | Calibration + false positives | Fixtures + physical tuning | M4 | Acceptance tests |
| 23 Sep | Sensor API freeze | Bug fixes only | M4 | Feature freeze |
| 24 Sep | Regression | Physical-device + lifecycle + sensor tests | M4/M6 | P0/P1 defects |
| 25 Sep | Final tuning | Threshold, stability, battery review | M4 | RC |
| 26 Sep | Demo preparation | Physical scan + fallback fixtures | M4/M6 | Demo rehearsal |
| 27 Sep | Final verification | Critical fixes only | All | Release freeze |
| 28 Sep | Presentation support | Explain FLP, geofence, sensor fusion, proximity gate | All | Final demo |

---

# 8. Member 4 — Quest & Scan Gameplay

**Branch:** `feature/m4-quest-scan-gameplay`

**Goal:** Own the gameplay experience after a checkpoint becomes actionable: scan HUD, state machine, fusion meter/status, proximity gate and reveal/discovery presentation.

| Date | Task | Definition of Done | Dependencies | If Late / Blocker Impact |
|---|---|---|---|---|
| 13 Sep | Gameplay contract | Scan states and inputs/outputs frozen | M3/M5/M6 | Signal contract dependency |
| 14 Sep | Checkpoint gameplay screen | Game/checkpoint context + entry states | M1/M3 | Navigation/map |
| 15 Sep | Scan HUD | Signal cards, meter, instructions, errors | M1/M3 | Use mock signals initially |
| 16 Sep | Scan state machine | Entering → ready → scanning → threshold → proximity → reveal | M3 | Core behavior |
| 17 Sep | Proximity gate | Separate final close-range gate UI | M3 | Must not add proximity to fusion |
| 18 Sep | Real fusion integration | Consume M3 `FusionResult` | M3 | Core mechanic |
| 19 Sep | Reveal/discovery | Clue/lore/reveal/completion states | M5/M6 | Discovery recording |
| 20 Sep | Offline + duplicate handling | Local success, already-found, retry/error | M6/M5 | Offline contract |
| 21 Sep | Dynamic checkpoint test | Scan creator-created checkpoint | M2/M3/M5/M6 | Dynamic proof |
| 22 Sep | Gameplay acceptance | Threshold, proximity block, success/failure | M3/M6 | Integration Gate 2 |
| 23 Sep | Gameplay API freeze | Bug fixes only | M3/M5/M6 | Feature freeze |
| 24 Sep | Regression | State machine, lifecycle, offline, duplicate | M3/M6 | P0/P1 defects |
| 25 Sep | Final HUD polish | Approved visual design | M1 | RC |
| 26 Sep | Demo preparation | Complete scan/reveal demo | M3/M6 | Demo rehearsal |
| 27 Sep | Final verification | Critical fixes only | All | Release freeze |
| 28 Sep | Presentation support | Explain scan mechanic/reveal | All | Final demo |

---

# 9. Member 5 — Firebase & Backend

**Branch:** `feature/m5-firebase-backend`

**Goal:** Own Firebase Auth, Firestore, security rules, cloud repository implementation, publishing, FCM and game-specific leaderboard backend.

| Date | Task | Definition of Done | Dependencies | If Late / Blocker Impact |
|---|---|---|---|---|
| 13 Sep | Backend contract | Collections, IDs and security boundaries frozen | M2/M6 | Persistence alignment |
| 14 Sep | Firebase/Auth | Project config, Auth, user identity | M1/M2 | Login |
| 15 Sep | Games/checkpoints | Firestore CRUD/read structure | M2/M3/M6 | Dynamic data |
| 16 Sep | Membership | `gamePlayers` join + ownership rules | M1/M2 | Join |
| 17 Sep | Progress/leaderboard | Game-scoped progress and entries | M4/M6/M1 | Discovery/ranking |
| 18 Sep | FCM | `new_games` topic + notification payload | M1/M2 | Notification/deep link |
| 19 Sep | Security | Ownership, membership, user/game isolation | M2/M6 | Security gate |
| 20 Sep | Cloud repository | Repository implementation + error mapping | M1/M2/M4/M6 | Integration |
| 21 Sep | Creator-to-player E2E | Created game → published → discoverable | M2/M1 | Dynamic pipeline |
| 22 Sep | FCM + publish E2E | Publish → notification → correct game | M1/M2 | Integration Gate 2 |
| 23 Sep | Cloud API freeze | Bug fixes only | All | Feature freeze |
| 24 Sep | Security regression | Rules + multi-user/game isolation | M6 | P0/P1 defects |
| 25 Sep | Final cleanup | Indexes, errors, payload cleanup | M1/M6 | RC |
| 26 Sep | Demo evidence | Publish/notification/security demo | M1/M2 | Demo rehearsal |
| 27 Sep | Final verification | Critical fixes only | All | Release freeze |
| 28 Sep | Presentation support | Explain Firebase/Firestore/security/FCM | All | Final demo |

---

# 10. Member 6 — Room + Sync + Integration

**Branch:** `feature/m6-room-sync-integration`

**Goal:** Own local persistence, repository orchestration, offline sync and final integration. M6 is the integration coordinator, not the owner of every feature's internal implementation.

| Date | Task | Definition of Done | Dependencies | If Late / Blocker Impact |
|---|---|---|---|---|
| 13 Sep | Integration contract | Room/repository/sync boundaries + branch strategy frozen | All | Integration foundation |
| 14 Sep | Room foundation | Database, entities, DAOs, composite keys | M2/M5 | Local data |
| 15 Sep | Local Game/Checkpoint cache | Cache dynamic games/checkpoints | M2/M5 | Offline browsing |
| 16 Sep | Repository orchestration | Room + cloud data-source boundary | M1/M2/M4/M5 | Feature integration |
| 17 Sep | Discovery persistence | `FoundCheckpoint` composite key + `pendingSync` | M4/M5 | Offline discovery |
| 18 Sep | Transactions/sync queue | Pending writes, retry/backoff, idempotency | M5/M4 | Sync |
| 19 Sep | Sync implementation | Room ↔ Firestore synchronization | M5 | Cloud/local consistency |
| 20 Sep | Integration Build 1 | Compile/integrate completed modules | All | Integration Gate 1 |
| 21 Sep | Dynamic architecture integration | Non-fixed game/checkpoint test | All | Architecture proof |
| 22 Sep | Offline/online E2E | Discovery offline → reconnect → sync | M4/M5 | Integration Gate 2 |
| 23 Sep | Integration freeze | Bug fixes + approved contract fixes only | All | Feature freeze |
| 24 Sep | Full regression | Room, migrations, sync, isolation, build | All | P0/P1 defects |
| 25 Sep | RC-1 | Clean build + release candidate | All | Release candidate |
| 26 Sep | Demo rehearsal coordination | Full golden path + fallback plan | All | Demo rehearsal |
| 27 Sep | Final regression/build | Release-freeze build | All | Final candidate |
| 28 Sep | Final integration evidence | Clean checkout/build, E2E proof | All | Final submission |

---

# 11. Blocker & Dependency Matrix

| Deadline | Blocker owner | Must be ready | Blocked / dependent work | Impact if missed |
|---|---|---|---|---|
| **16 Sep** | **M3** | Dynamic checkpoint/geofence interface | M4 scan activation; M1 map integration; M6 integration | M4 can use mocks, but physical gameplay integration slips |
| **16 Sep** | **M5** | Game details + join contracts | M1 details/join; M2 persistence | Player cannot reliably join creator-created games |
| **17 Sep** | **M3** | Light/motion/proximity signal contract | M4 scan state/HUD | M4 remains mock-only |
| **18 Sep** | **M3** | `FusionResult` + weighted scoring | M4 fusion meter/threshold | Core mechanic cannot be acceptance-tested |
| **18 Sep** | **M5** | FCM payload + deep-link data | M1 notification navigation | New-game notification E2E blocked |
| **19 Sep** | **M5** | Security rules + progress/leaderboard backend | M4 discovery; M1 leaderboard; M6 sync | Cloud E2E blocked |
| **20 Sep** | **M6** | First integrated build | All members | Interface conflicts discovered late |
| **21 Sep** | **M2** | Creator can produce arbitrary checkpoint set | M3 geofences; M4 scan; M5 cloud; M6 cache | Dynamic architecture cannot be proven |
| **22 Sep** | **M6** | Offline → online sync | M4 offline discovery; M5 consistency | Offline acceptance blocked |
| **23 Sep** | **All** | Feature/API freeze | All regression work | Late changes destabilize testing |
| **25 Sep** | **M6** | RC-1 clean build | All final testing/demo | No stable rehearsal candidate |
| **27 Sep** | **M6 + All** | Final release build | Final presentation/submission | Unresolved P0/P1 threatens final demo |

---

# 12. Daily Team Rule

At the end of every development day, each member must report:

1. **Completed task**
2. **Unfinished task**
3. **Blocker/dependency**
4. **Next-day target**

If a task is going to miss its deadline, report it **before the deadline**.

The affected member should:

- continue using an agreed mock/fixture where possible;
- record the dependency;
- notify M6;
- avoid silently waiting.

---

# 13. Definition of Done

| Check | Requirement |
|---|---|
| **Code** | Implementation is committed to the member's feature branch and compiles. |
| **Contract** | Shared interfaces/models/navigation arguments match the agreed specification. |
| **UI** | Relevant loading, empty, error and success states exist where applicable. |
| **Tests** | Appropriate unit/integration/manual test has been executed. |
| **Dynamic behavior** | No new hard-coded dependency on R001–R006 is introduced. |
| **Isolation** | Game/user data is scoped correctly. |
| **Offline** | Discovery/progress features follow the Room/sync contract. |
| **Documentation** | Changed public contracts are documented and communicated. |
| **Integration** | Member has tested against the latest shared branch/contract. |

---

# 14. Milestone Gates

| Gate | Date | Required outcome | Owners | Pass condition |
|---|---|---|---|---|
| **M0 — Contracts frozen** | 13 Sep | Domain, navigation, sensor, repository, Firestore and Room boundaries agreed | All | No unresolved ownership ambiguity |
| **M1 — Skeletons** | 14 Sep | Every member has runnable feature skeleton | All | Clean build |
| **M2 — Data + UI foundation** | 16 Sep | Player/creator screens + dynamic persistence foundations | M1/M2/M5/M6 | Games/checkpoints represented dynamically |
| **M3 — Physical signals** | 18 Sep | Real location/sensor/fusion interfaces work | M3/M4 | Fusion output drives scan UI |
| **M4 — Golden path** | 20 Sep | Player traverses core flow | All / M6 | Login → games → details → join → map → scan |
| **M5 — Dynamic creator pipeline** | 21 Sep | Creator-created arbitrary checkpoint game reaches player map | M2/M3/M5/M6 | No fixed six-checkpoint assumption |
| **M6 — Full creator-to-player** | 22 Sep | Create → publish → notify → discover → join → play | All | FCM + gameId deep link verified |
| **M7 — Feature freeze** | 23 Sep | Feature APIs frozen | All | Only bug fixes afterward |
| **M8 — Release candidate** | 25 Sep | Clean RC passes primary acceptance tests | M6 + All | Reproducible install/build/demo |
| **M9 — Demo rehearsal** | 26 Sep | Full demo rehearsed | All | No manual database edits |
| **M10 — Final freeze** | 27 Sep | Final release build | M6 + All | P0/P1 defects resolved |
| **M11 — Final** | 28 Sep | Presentation/submission candidate | All | Complete golden path + evidence |

---

# 15. Final Golden Path — Everyone Must Support This

| Step | User action | Primary owner | Supporting owners | Required result |
|---|---|---|---|---|
| 1 | Launch app | M1 | All | Stable app shell |
| 2 | Login | M1/M5 | M6 | Authenticated user |
| 3 | Browse published games | M1/M5 | M6 | Dynamic game list |
| 4 | Open game details | M1/M5 | M6 | Correct `gameId` |
| 5 | Join game | M1/M5 | M6 | Membership recorded |
| 6 | Open game map | M1/M3 | M6 | Dynamic checkpoint markers |
| 7 | Approach checkpoint | M3 | M1 | Distance/geofence state |
| 8 | Enter geofence → Scan Mode | M3/M4 | M1 | Scan becomes actionable |
| 9 | Perform scan | M4/M3 | M6 | GPS + light + motion produce fusion score |
| 10 | Reach fusion threshold | M3/M4 | M6 | Threshold state |
| 11 | Confirm close proximity | M3/M4 | M6 | Separate proximity gate passes |
| 12 | Reveal clue/lore | M4 | M1 | Discovery presentation |
| 13 | Save discovery | M6 | M4/M5 | Room record + pending sync if offline |
| 14 | Sync to cloud | M6/M5 | M4 | Firestore progress |
| 15 | Show game leaderboard | M1/M5 | M6 | Game-specific ranking |
| 16 | Publish another game and notify players | M2/M5 | M1 | FCM deep link opens correct game |

---

# 16. What Counts as a Blocker?

### Critical blocker

A missing contract or implementation that prevents another member from integrating or testing a core feature.

Examples:

- M3 has not exposed `FusionResult`.
- M5 has not defined gameId-based join.
- M6 cannot provide discovery persistence.
- M2 has not defined dynamic checkpoint configuration.
- M5 has not provided the FCM payload/deep-link data.
- M6 cannot produce a clean integrated build.

### Non-critical delay

Visual polish or a secondary state that does not prevent integration.

The dependent member should continue with the available contract/mock while the delay is logged.

### Team rule

**No member should wait silently for another member.**

Use mocks/fixtures where the contract is stable and escalate integration blockers to M6 immediately.

---

# 17. Final Release Rules

By **23 September**:

- feature APIs are frozen;
- navigation contracts are frozen;
- sensor contracts are frozen;
- backend contracts are frozen;
- Room/repository contracts are frozen.

From **24 September onward**:

- no unnecessary feature additions;
- focus on testing, integration and bug fixing;
- P0/P1 defects take priority over visual enhancements.

By **25 September**:

- RC-1 must build cleanly.

By **26 September**:

- the full demo must be rehearsed.

By **27 September**:

- final release build must be frozen.

By **28 September**:

- the team presents/submits the complete Campus Quest candidate.

---

## 18. One-Line Responsibility Summary

| Member | If you remember only one thing |
|---|---|
| **M1** | Make the player experience and navigation work. |
| **M2** | Make creators able to build, edit and publish any game/checkpoint set. |
| **M3** | Make location, geofencing, sensors and fusion work correctly. |
| **M4** | Make the actual quest/scan/reveal gameplay work. |
| **M5** | Make authentication, cloud data, security, leaderboard and notifications work. |
| **M6** | Make local storage, offline sync and the complete integrated application work. |

---

**Schedule principle:** Each member owns a clear vertical responsibility, but the team succeeds only when the interfaces between those responsibilities are completed on time. M3, M5 and M6 have particularly important early deadlines because their outputs become dependencies for multiple other members.
