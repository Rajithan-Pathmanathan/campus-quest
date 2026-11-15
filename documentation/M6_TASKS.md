# Member 6 — Task List: Room, Repository, Offline Sync & Integration

**Based on:** `main` @ `8ad34e5` (after PRs #6–#10), 2026-11-15
**Owner:** M6
**Works closely with:** M5 (Firestore functions you call), M2 (creator needs new repository functions), M4 (scan/map need the real user id)
**Reference:** your workplan's "UPDATE (2026-11-14)" section, and `M3_INTEGRATION_GUIDE.md` §4.4 (story quest data).

> **Status today:** Room works locally (games, checkpoints, progress, sync queue survive app restarts). But the repository has data-loss bugs, every player is the same `"current_user"`, there is no real sync, and the app still ships California sample quests. **Several merged screens are waiting on you** (see Part C).

All paths below are under `app/src/main/java/com/campusquest/`. Each task has a **Done when** line.

---

## Part A — Data-loss bugs 🔴 fix first

- [ ] **A1. Re-saving a game deletes all its checkpoints.**
  `data/local/dao/GameDao.kt` lines 29–33 use `@Insert(onConflict = REPLACE)`. REPLACE **deletes** the old row and inserts a new one, and `CheckpointEntity` has a foreign key with **`onDelete = CASCADE`**, so every re-save wipes the game's checkpoints. This is now reachable from the Creator screen (M2, PR #10): save a draft, add checkpoints, edit the title, save again → checkpoints gone.
  **Fix:** use **`@Upsert`** (Room 2.5+; the project has 2.6.1) for `insertGame` and `insertGames`.
  **Done when:** a test that saves a game, adds checkpoints and saves the game again still finds the checkpoints.
- [ ] **A2. Sync deletes queued data without uploading it.**
  `data/repository/GameRepositoryImpl.kt` `syncPending()` (line 171) calls `syncQueueDao.deleteById(...)` (line 175) for every item but never uploads anything.
  **Fix:** for each item call M5's Firestore function and delete the item **only after it succeeds**; on failure keep it (with a retry count).
  **Done when:** a test with a failing fake remote leaves the items in the queue.

## Part B — Real players and correct progress 🔴

- [ ] **B1. Use the signed-in user's id, not `"current_user"`.** It is hardcoded in:
  - `data/repository/GameRepositoryImpl.kt` lines 105, 130, 134 (`joinGame`, `recordDiscovery`);
  - `ui/map/CampusMapFragment.kt` line 43 and `ui/quest/QuestScanViewModel.kt` line 356 (they copied the repository's id; switch them once the repository does).
  Pass the `AuthRepository` into `GameRepositoryImpl` (agree a single app-wide instance with M5/M1) and use `getCurrentUserId()`. If nobody is signed in, use one **stable** local id so offline progress survives restarts.
  **Done when:** two accounts on the same phone have separate progress.
- [ ] **B2. A checkpoint can only be discovered once** (spec "discovery idempotency"). `recordDiscovery` (line 128) de-duplicates the id list but still adds **+100** and advances the order on a repeat claim. If the checkpoint is already in `completedCheckpointIds`, return success and change nothing.
- [ ] **B3. Quests must complete.** `isCompleted` is never set to `true`, and `completedAt` is overwritten on every discovery. When the found count reaches the game's checkpoint count, set `isCompleted = true` and `completedAt`. M4's finale and M5's leaderboard depend on this.
- [ ] **B4. Re-joining keeps progress.** `joinGame` (line 101) overwrites the progress row with an empty one. If a progress row already exists, keep it.
  **Done when:** tests show a repeat claim keeps the score, the last chapter completes the quest, and re-joining keeps found checkpoints.

## Part C — Functions other members are waiting for 🟠

Add these to `domain/repository/GameRepository.kt` and `GameRepositoryImpl`:

- [ ] **C1. `getCreatorGames(creatorId)`** — drafts **and** published games of one creator. Needed by M2: `ui/creator/CreatorGamesViewModel.kt` line 44 has a `TODO`; today the Creator dashboard only shows *published* games, so saved drafts disappear. Add a `suspend` query in `GameDao` (`WHERE creatorId = :creatorId`).
- [ ] **C2. `deleteCheckpoint(gameId, checkpointId)`** — needed by M2: `ui/creator/CheckpointListViewModel.kt` line 79 has a `TODO`. `CheckpointDao.deleteCheckpoint` already exists.
- [ ] **C3. `updateGame(game)`** — edit a draft's title/description without re-inserting (avoids A1 entirely). `GameDao.updateGame` already exists.
- [ ] **C4. `getAllPlayerProgress(userId)`** — so M1's dashboard can show the real total score and relics found (today it shows fixed text).
  Tip: give new interface functions a **default body** in `GameRepository` so the existing test fakes keep compiling.
  **Done when:** M2's two `TODO`s are replaced with real calls and a saved draft appears on the Creator dashboard.

## Part D — Seed data: the story quest 🟠

- [ ] **D1. Replace the California sample quests** in `data/mock/MockDataCatalog.kt` (and `data/mock/DebugSeeder.kt`, used from `ui/player/GamesViewModel.kt` line 40) with ***The Founder's Lantern*** from `M3_INTEGRATION_GUIDE.md` §4.4:

  | # | Chapter | Location | Coordinates | Light (lux) | `motionType` |
  |---|---|---|---|---|---|
  | 1 | The Ember in the Light | Tennis Court | 6.9722948878782995, 79.91638354596967 | 10,000 – 120,000 | `SHAKE` |
  | 2 | The Ember in the Shade | Buddha Statue | 6.974319507376335, 79.91600245459793 | 500 – 10,000 | `TILT` |
  | 3 | The Ember in the Dark | Medical Centre | 6.976196957961348, 79.91471980379754 | 0 – 60 | `SWEEP` |

  Use `radiusM = 15`, the quest story as the game `description`, each chapter's briefing as `clue`, and a "memory restored" text as `lore` (placeholders until M3 gets real facts about each place).
- [ ] **D2. Seed in release builds too.** Today seeding only runs in debug builds and only when the Games screen opens, so a release build shows an empty quest list. Seed on database creation (Room `onCreate` callback) or on first launch.
- [ ] **D3. Agree with M5** whether the story quest also lives in Firestore (M5 task G1).
  **Done when:** a fresh install shows only *The Founder's Lantern*, with 3 chapters at the real locations.

## Part E — Sync engine 🟠 (after M5's Part D)

- [ ] **E1. Make queue items useful.** Every enqueue today saves `payloadJson = "{}"` (lines 45, 61, 89, 147). Store at least the entity id + `gameId` (+ uid where needed) so the worker can reload the latest row and upload it.
- [ ] **E2. Correct operation types.** `createCheckpoint` enqueues `UPDATE_CHECKPOINT`; `updateCheckpoint` and `joinGame` enqueue **nothing**, so those changes never reach the cloud.
- [ ] **E3. Background sync job.** WorkManager is a dependency but no `Worker` exists and nothing calls `syncPending()`. Add a `SyncWorker` with a **network-connected** constraint and exponential back-off; run it at app start and after each queued write.
  ⚠️ **Lesson from M3's field test:** **don't call Firestore inline** from `createGame` / `recordDiscovery`. An offline Firestore `set().await()` never completes, so the save (and the screen waiting on it) hangs. Queue the write and let the worker sync it.
- [ ] **E4. Skip what the rules will reject.** Don't try to upload games the current user doesn't own (e.g. seed data) or anything from a not-signed-in player; drop or park those items instead of retrying forever.
- [ ] **E5. Refresh from the cloud.** When M5's "fetch published games" succeeds, **merge** the remote games into Room (with `@Upsert`, see A1) and return the local list. Don't replace the list with only the remote games, or seed and own quests vanish.
  **Done when:** a discovery made offline appears in Firestore after reconnecting, and nothing is lost if the phone is offline for a while.

## Part F — Housekeeping 🟡

- [ ] **F1. Lint errors.** `res/xml/backup_rules.xml` line 4 and `data_extraction_rules.xml` line 5 exclude `campus_quest_db`, which isn't in any included path (the only 2 lint errors on `main`). Remove the two `exclude` lines (no behaviour change) or include the database domain on purpose.
- [ ] **F2. Tests** for every fix above (in-memory fakes or mocked DAOs): A1 cascade, A2 failed upload kept, B2 repeat claim, B3 completion, B4 re-join, C1 creator games, E5 merge.

---

## Already proven in M3's sandbox (for reference, not to copy)
M3's private test branch implemented and tested A1 (`@Upsert`), A2, B1–B4, C1/C2/C4, E3–E5 and the story-quest seed. If something behaves unexpectedly, ask M3 how it went there.
