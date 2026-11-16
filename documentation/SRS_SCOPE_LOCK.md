# Campus Quest — SRS and scope lock

Version: 1.0  
Date: 16 November 2026  
Baseline: story commit `6430cf3`, based on main `8b2ac34`  
Status: scope baseline; functional acceptance is still pending  
Target: complete every functional item below, then leave only visual UI implementation and its regression testing.

## 1. Purpose and change rule

This document defines the final application scope and the remaining work needed to finish it. It is not a claim that every requirement already works. No new gameplay, unrelated feature, sensor task or external service is to be added after this baseline without an explicit approved scope change.

Fixing defects, completing the listed requirements, protecting data and making the existing flows accessible are within scope. A feature visible in the UI reference is not automatically a functional requirement.

This turn creates documentation only. The work below is a backlog, not authorization to delete shared games, deploy security rules, delete accounts, publish releases or merge branches without the appropriate task authorization.

## 2. Source of truth

- This SRS controls final functional scope.
- `GOLDEN_TREE_FLOW.md` describes the accepted story; this SRS adds completion requirements.
- `LECTURE_COVERAGE_REVIEW.md` maps the supplied slides to the app.
- The treasure-hunt reference controls the later visual direction.
- `APP_STATUS_AND_PRODUCTION_READINESS.md` describes an older baseline. Its claims that Firestore, sync scheduling, sign-out, account deletion, privacy, CI, schema export and release shrinking are absent are outdated. These now exist in source and need verification, not duplicate implementations.
- Old Light the Way and ember/Resonator specifications are historical. Their lamp, cover, shake, sweep and proximity tasks must not return to gameplay.

## 3. Locked product

Campus Quest is a native Android location-based adventure with player and creator flows. The first supported story is **The Golden Tree**, containing exactly three ordered checkpoints. Multiple published instances may use different safe locations; the application must not create fake games, fake players, fake scores or placeholder discoveries.

Player goal: find the key, read the keeper's instruction card, and deliver golden water to restore the golden tree's light.

Only one real game prop is required: the checkpoint-2 printed QR card with its fallback code. The key box, jug, supplies and tree are virtual. Players walk between checkpoints and stand still in a safe place while solving puzzles.

### 3.1 Locked story sequence

| Step | Required behaviour | Completion condition |
|---|---|---|
| Introduction | Explain the goal, three stops, real versus virtual objects, safe stationary play and controls in simple English. Check required hardware. | Player starts or resumes with a clear next destination. |
| Navigation | Show next stop, live distance, direction, map, arrival state and useful permission/GPS recovery. | Fresh, sufficiently accurate GPS remains inside the arrival area for the required hold. |
| Stop 1: key | Calibrate real tilt. Guide the key around the existing eight barriers to the opening. Explain how lowering a side moves the key. | Key reaches the exit through valid collision geometry; award once. |
| Transition 1 | Explain that the note points to the keeper's card. | Explicit action: Go to stop 2. |
| Stop 2: card | Scan this game's real QR card using CameraX. Provide real torch control and printed-code fallback. Explain that the key opens the keeper's supplies and the jug is collected. | Correct checkpoint card/code validated at the location; award once. |
| Transition 2 | Explain why the tree needs golden water and where to go next. | Explicit action: Go to stop 3. |
| Stop 3: jug | Balance a virtual jug for 60 active seconds using real tilt. Show moving liquid, time remaining, water remaining, spills, pause, resume and refill. Player remains stationary. | Sixty valid active seconds finish with water remaining; award once. |
| Ending | Explain that the water restores the tree; show saved results and the three completed stops. | Completion persists and results can be reopened without duplicate awards. |

### 3.2 Fixed gameplay rules

- Three checkpoints in order; no skipping by opening the map, navigating back or scanning another game's card.
- Default arrival radius: 15 m; accuracy at most 20 m; fix age at most 15 seconds; stable entry for approximately 3 seconds; existing exit hysteresis retained. Verify these values in the field before acceptance.
- Existing initial locations: stop 1 `7.727295, 81.696698`; stop 2 `7.727241, 81.696882`; stop 3 approximately 50 m north of stop 2. Creators validate actual access and safety before publishing.
- Maze movement is tilt-only. No touch dragging, joystick or slide substitute.
- Jug duration is 60 active seconds, not wall-clock time. Small spills are allowed. Empty jug requires an explicit refill and restart.
- Jug sway should require understandable corrective tilts. Calibrate difficulty with real players; leaving a phone untouched must not be the intended winning strategy.
- Missing accelerometer blocks starting with a clear explanation. Stale sensor readings never count as success.
- Missing camera/denied camera permission uses the printed code. Missing torch or light sensor does not block the card step.
- Light level only suggests turning on the flashlight; it never creates an unrelated brightness puzzle.
- Compass failure leaves usable map/distance guidance. An arrow is direct bearing, not a safe walking-route promise.
- No proximity, lamp aiming, latch, cover slider, compulsory shaking, sweeping, microphone or Bluetooth task.
- A checkpoint may contain several related actions; there is no one-sensor-per-checkpoint rule.
- Default awards are 100 points per checkpoint, 300 for the completed story. Display totals from saved results rather than independent hard-coded success claims.

## 4. Current evidence and limits

The redesigned story builds; 210 JVM tests passed at the preceding implementation checkpoint. A phone test verified accelerometer readings, no touch movement of the key, and jug rendering. These are baseline results, not proof of full outdoor completion, current deployed Firebase rules, cross-device sync, accessibility or release readiness.

Known remaining issues include unfinished puzzle state loss on screen recreation, old stored titles/descriptions, legacy code and documentation, generic creator output that cannot enter the supported story, and jug difficulty that has not been field-calibrated. The ending currently uses completion text; its tree/pouring artwork belongs to the UI phase.

## 5. Required work before UI-only status

Every row starts OPEN. “Verify/fix” means implementation exists or may exist: inspect and test first, then change only what fails. Suggested owners describe responsibility; they do not assign or notify collaborators.

### A. Story and lifecycle

| ID | Work | Acceptance evidence | Suggested owner |
|---|---|---|---|
| A01 | Complete the single-flow cleanup. Remove obsolete gameplay entry points and unnecessary sensor registration; audit callers before removing unused code. | Every player entry point opens the same supported story. No old vocabulary/tasks appear. Retained compatibility fields have documented reasons. | M3 + M4 |
| A02 | Put story state in a lifecycle-safe owner and preserve unfinished puzzle state. | Rotation, background/foreground, navigation away/back and process recreation preserve maze position or jug time/water. Timers never advance in the background. Restore requires fresh location/tilt and explicit resume. | M3 + M4 |
| A03 | Finish and calibrate jug gameplay. | Tilt directions match the visual response; sway is readable; corrective tilts help; spills are visible; refill resets all challenge state; 60 seconds and empty-jar failure have automated tests and a real-phone demonstration. | M3 |
| A04 | Harden checkpoint completion and recovery. | Save failure is visible and retryable; retries, rotation and double callbacks cannot grant points twice; changing account cannot award another player's progress. | M4 + M6 |
| A05 | Finalize all story instructions and action states. | A first-time player can explain what to do at every screen without developer assistance. Include arrival, calibration, pause, failure, retry, next stop and finish wording. | M3 + UI team |
| A06 | Preserve completed play and separate new attempts from old progress. | Completed games stay completed across upgrades. No automatic reset. Any replay support already exposed has clear semantics and cannot inflate the leaderboard. Do not add replay if it is not exposed. | M4 + M6 |

### B. Creator tools and existing data

| ID | Work | Acceptance evidence | Suggested owner |
|---|---|---|---|
| B01 | Align creator creation/editing/publishing with the supported three-stop story. | Creator can save a draft, reopen it, configure three ordered locations and publish a playable Golden Tree instance. Generic creation cannot silently publish an unplayable quest. | M2 |
| B02 | Validate checkpoints and actual placement descriptions. | Coordinates, count, ordering and radius are validated. Card instructions describe its real placement; no invented garden/board/pocket. Creator confirms safe access. | M2 + M3 |
| B03 | Complete QR export and compatibility. | Creator can view/save the correct card; another phone reads it; backup code works; wrong-game card/code fails. Existing printed tokens remain usable. | M2 + M3 |
| B04 | Define and implement non-destructive handling of legacy records. | Supported old story records show consistent new copy without losing IDs, QR tokens or awards. Unsupported quests are clearly identified to creators, with an explicit conversion or recreation path. No silent shared-data rewrite or deletion. | M2 + M5 + M6 |
| B05 | Verify creator ownership and edit/delete semantics. | Another account cannot edit/delete/export protected creator data. Editing or deleting checkpoints cannot accidentally complete a game or erase unrelated progress. Confirmation explains any destructive action. | M2 + M5 + M6 |

### C. Accounts, cloud and data integrity

| ID | Work | Acceptance evidence | Suggested owner |
|---|---|---|---|
| C01 | Verify email registration/login, profile name, persistent session, logout and account deletion. | Correct name appears; restart preserves login; logout isolates data; deletion handles recent-login requirements and partial failures. Existing features are repaired rather than rebuilt. | M1 + M5 |
| C02 | Resolve guest access configuration and error handling. | A fresh guest can enter when enabled; failures use understandable wording and do not pretend the player needs administrator access. Document guest progress/recovery limits. | M5 |
| C03 | Verify publish/download across two accounts and phones. | A creator publishes a real game; a second phone finds/joins it with all three checkpoints and correct story/card configuration. Fresh install has a meaningful empty/loading/error state. | M5 + M6 |
| C04 | Verify offline-first sync and failure recovery. | Downloaded content works offline; local progress is retained; reconnect uploads once; retry/duplicate/out-of-order updates cannot lose or multiply awards. Permission errors are distinguishable from temporary network failures. | M5 + M6 |
| C05 | Verify leaderboard and completion results. | Real players, display names, points and completion status agree across devices. No Firebase UID presented as a friendly name, fake rows or locally fabricated rankings. | M1 + M5 + M6 |
| C06 | Verify deployed authorization and validation. | Unauthorized access to another user's progress and another creator's edits is denied in rules tests and the configured environment. Client-side buttons alone are not authorization. | M5 |
| C07 | Verify migration and account isolation. | Upgrade preserves games, cards and progress. Old queued work cannot be sent as a newly signed-in user. Sign-out/deletion does not expose a previous user's private data. | M5 + M6 |

### D. Device behaviour, permissions and notifications

| ID | Work | Acceptance evidence | Suggested owner |
|---|---|---|---|
| D01 | Verify location permission versus device location settings. | First grant, denial, permanent denial, approximate-only permission and device-location-off each have a working explanation/recovery path. No blank or endless waiting screen. | M3 |
| D02 | Field-test navigation and arrival. | Correct target and distance at all three stops; inaccurate/stale fixes do not unlock tasks; entry/exit and GPS loss behave consistently. | M3 |
| D03 | Verify camera, torch and sensor lifecycle. | Backgrounding closes camera/torch and stops unnecessary listeners; returning recovers; no missing sensor creates a fake reading or success. No proximity listener in active story. | M3 |
| D04 | Verify existing geofence and new-quest notification features. | Correct notification permissions; real alerts open the correct game; no duplicates or alerts after leaving a quest/account. Denied background access does not block foreground play. | M3 + M5 |
| D05 | Review permission/privacy text against actual collection. | Privacy text, manifest permissions, notifications and runtime requests agree with the new flow and current Firebase/crash reporting behaviour. | M1 + M5 |

### E. Build, assessment evidence and functional acceptance

| ID | Work | Acceptance evidence | Suggested owner |
|---|---|---|---|
| E01 | Verify clean checkout and CI. | A teammate can build with documented configuration; CI runs the relevant unit/build checks without local hacks or committed signing secrets. | Team |
| E02 | Verify release packaging. | Signed release build installs and launches; shrinking preserves Room, QR, Firebase and maps; versioning/signing instructions are documented. Keystore stays outside Git. | Team |
| E03 | Add targeted regression tests for this locked scope. | Full story state transitions, timer pause/resume, persistence, wrong QR, migration, authorization and duplicate sync have appropriate unit/instrumented/rules tests. Do not delete tests just to obtain a pass. | Relevant owners |
| E04 | Run the end-to-end acceptance matrix below. | Recorded results with device, build SHA, tester, date and remaining defects. All functional blockers resolved. | Team |
| E05 | Close lecture/rubric uncertainty before functional sign-off. | Record how the API/database/security requirements are demonstrated. Verify whether the actual assignment rubric specifically requires direct REST/JSON, Google Maps or IoT; the slides alone do not establish that. | Project lead |
| E06 | Update current documentation and evidence. | One current story/SRS, setup guide, card instructions, architecture/data-flow explanation, test report and known limitations. Historical documents clearly labelled superseded. | Team |

## 6. Acceptance matrix

| Scenario | Required result |
|---|---|
| Fresh player install | Sign in/guest flow works; real published content loads or a clear empty/error state is shown. |
| Fresh creator install | Create, draft, reopen, configure, publish and export the card; second phone can play. |
| Complete outdoor run | All three locations and tasks work in order; final saved score is correct. |
| Wrong/old QR | Wrong game is rejected; compatible existing card remains valid. |
| No camera/torch/light sensor | Clear fallback; printed code works; no compulsory light task. |
| No accelerometer | Clear preflight limitation, no dead-end halfway through the adventure. |
| No/restricted compass | Map and distance remain useful; direction is not fabricated. |
| Denied permissions/location off | Explain the specific missing requirement and provide the correct recovery. |
| GPS lost during puzzle | Pause without awarding or silently advancing time; recover safely. |
| Rotate/background/kill process | Restore unfinished work and saved progress; require fresh readings before resuming. |
| Jug empty | Fail clearly; explicit refill restarts time and liquid state. |
| Save/network failure | Local work remains safe; retry and reconnect produce one award. |
| Account changes | Correct ownership and per-player isolation everywhere. |
| Upgrade existing database | Games/cards/completed checkpoints survive; unsupported legacy content is clearly identified. |
| Release build | Core journey works with release configuration and shrinking enabled. |

## 7. Functional freeze gate — when only UI may remain

Do not mark this gate complete until:

- [ ] A01–E06 are accepted, or an explicit scope decision explains why a verification item does not apply.
- [ ] No unresolved defect blocks joining, creating, publishing, navigating, playing, saving, syncing or viewing results.
- [ ] No old story route or unexplained technical vocabulary is reachable by a player.
- [ ] The creator can produce a complete playable story without manual database edits.
- [ ] A real two-device run and an offline/reconnect run are recorded.
- [ ] The 60-second jug task is understandable and playable on the test devices.
- [ ] Lifecycle, permissions, missing hardware and save failures have been exercised.
- [ ] The lecture/rubric mapping is accepted; any additional mandatory requirement is settled before freezing.
- [ ] The tested commit and build artifact are recorded.

Freeze record:

| Field | Value |
|---|---|
| Accepted commit/build | Pending |
| Acceptance report | Pending |
| Accepted by/date | Pending |
| Functional blockers | Pending closure |
| Remaining work | Must contain only Section 8 plus regression fixes caused by those changes |

## 8. Only work allowed after functional freeze: UI implementation

Branch: `ui-fix` (local until publication is requested). Functional fixes should land in the story/fix branch first and be integrated into this branch; the branch name does not mean the freeze gate has already passed.

Match the supplied reference's forest scenery, parchment surfaces, gold/brown controls, illustrated cards, typography, spacing, depth and clear navigation. Use generated assets where useful, after the functional story is stable.

UI deliverables:

- Splash/welcome presentation and concise onboarding using the locked story.
- Login/register/guest screens and existing account actions.
- Player dashboard, real quest list, details, map and navigation.
- Creator dashboard, drafts, checkpoint editor and instruction-card export.
- Key puzzle presentation, QR scanner framing, jug liquid feedback and final tree/pouring reveal.
- Results/leaderboard presentation using real stored values.
- Existing privacy, permissions, errors, empty/loading/offline states.
- Consistent icons, reusable visual styles, touch feedback and restrained transitions.
- Small-screen/landscape/large-font checks, readable contrast, approximately 48 dp touch targets and accessible labels. Sensor-only tasks must honestly communicate their accessibility limits.

The reference's avatar picker, friends tab, badges, level system, reward shop, wallpaper prizes and profile editing are not new functionality authorized by the image. Do not show working-looking controls without real supported actions. UI changes cannot modify puzzle rules, authorization, storage contracts or scoring without reopening functional review.

## 9. Explicit exclusions

No new stories, extra checkpoints in this template, new game modes, multiplayer, chat, friends, payments, ads, microphone tasks, proximity tasks, IoT hardware, separate iOS app, alternate database stack or artificial sensor requirements.

No weather API is added under this lock. If the actual assessment requires a direct REST endpoint beyond the existing Firebase integration, record that as a requirement change before the freeze gate; do not quietly append it during UI work.

Store submission and public rollout are outside the academic functional freeze. If requested later, evaluate current store requirements and deployment readiness separately. Signing and a demonstrable release build remain in scope above.

## 10. Execution order and completion log

1. Resolve creator/legacy compatibility and complete one supported story route (A01, B01–B05).
2. Finish state retention, jug calibration, clear instructions and safe completion (A02–A06).
3. Verify accounts, cloud, offline sync, security and result consistency (C01–C07).
4. Verify permissions, real hardware, navigation and notifications (D01–D05).
5. Complete clean/release builds, tests, field evidence and rubric mapping (E01–E06).
6. Sign the functional freeze gate.
7. Implement the reference UI on `ui-fix` and rerun functional regression checks.

For each requirement, record: ID, implementation commit, verification method, result, date/tester and unresolved limitation. Marking a task complete requires evidence, not just a file or class existing. Scope additions require a written reason, affected requirements and explicit approval before implementation.

### Completion log

| ID | Implementation | Verification | Result | Date / tester | Unresolved limitation |
|---|---|---|---|---|---|
| A01 | Branch `fix/creator-legacy`. Removed the old ember route and code: `ui/quest/QuestScan*`, `ScanState`, `HeartbeatHaptics`, `device/sensor/**` (fusion engine, proximity/shake/sweep/trial detectors), `domain/fusion/**`, `SensorRepository(Impl)`, `QuestCues`, `LampView`, their layouts and the `questScanFragment` nav destination. Player text no longer says relics/ember/proximity. Kept for compatibility and documented in code: `Checkpoint.lightSignature/motionType/rarity` (Room + Firestore schema) and `StorySession.motionFallback/coverFallback` (Room v2, no migration). | `assembleDebug`, `compileDebugAndroidTestKotlin`, 132/132 unit tests. Tests that only covered the removed code were deleted with it. | Accepted for code; the field run is pending | 2026-11-16 / Claude (emulator) | Internal object name `LightTheWay` kept (stored records refer to it). |
| B01 | Single creator route: Creator Studio → **Create a Golden Tree quest** (story title/description prefilled) → save draft → add exactly 3 ordered stops → publish. Every creator quest reopens in the editor. Re-saving a published quest keeps it published. Publishing creates the instruction card if missing. The Golden Tree setup screen is now card view/export only. | Emulator: created draft, added 3 stops (auto-numbered 1–3, 15 m radius, Add disabled at 3), published, card shown, quest joinable and startable. Unit tests: validator 3-stop/numbering rules; repository creates the card on publish. | Accepted (emulator) | 2026-11-16 / Claude | Outdoor play of a creator-made quest not yet run. |
| B02 | Publish requires exactly 3 stops numbered 1, 2, 3, plus name, coordinates, 5–500 m radius and a "where" description for every stop. Stop 2's description is where the printed card is placed. The publish dialog asks the creator to confirm the places are safe and accessible. Players see the creator's "Where: …" text at each stop. | Emulator: "Where: Bottom of the library steps" shown at stop 1. Unit tests for the validator. | Accepted (emulator) | 2026-11-16 / Claude | Wording to be finalised with the UI team (A05). |
| B03 | Card export: Creator Studio (quest checkpoint screen ⋮ → Instruction card), Quest details (creator only) and automatically after publish. The QR contains the game ID; existing tokens are never regenerated (re-publish keeps the same card). | Emulator: card with QR and backup code rendered after publish. Unit test: re-publish keeps the same card. | Partly accepted | 2026-11-16 / Claude | Scanning from a second phone and rejecting a wrong-game card still need a two-device check. |
| B04 | Legacy quests: old "Light the Way" records are **displayed** as The Golden Tree (stored data untouched). Quests without a card or without exactly 3 stops show players "This quest uses an older format…" and can't be joined or started. Creators convert them in the editor (give 3 stops, publish again → card created). A published quest with more than 3 stops may be trimmed to 3. | Emulator: "Test 1" (3 stops, no card) correctly blocked with the message. Unit test: legacy title display without rewrite. | Accepted (emulator) | 2026-11-16 / Claude | No bulk migration by design; each creator converts their own quests. |
| B05 | Only the creator can open a quest in the editor, publish (repository check), delete (existing) or view/export its card (existing). Published quests keep their 3 stops: deleting a stop is blocked, while editing a stop's place is allowed. Destructive actions have confirmation dialogs. Cloud rules enforce the same. | Unit tests: a non-creator can't publish or delete. | Accepted (unit) | 2026-11-16 / Claude | Two-account check on real devices pending. |
