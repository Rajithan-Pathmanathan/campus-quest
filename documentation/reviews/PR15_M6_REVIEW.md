# Review of PR #15 — M6 Room, progress and sync queue

Reviewed: 15 November 2026.  
PR: https://github.com/Rajithan-Pathmanathan/campus-quest/pull/15  
Author: KPDulsara.  
Status observed: already merged.  
Reviewed main: `0d3b0da2449c0e000e451261268bfbb4ff1012b2`.  
Previous main: `4629c2d78c7f0bfb92b210a5dedb8480c1d878e2`.

## Result

The PR makes useful progress: game upserts, shared auth, per-user progress, duplicate-claim protection, completion flags, creator queries and queue retention. Two correctness problems remain and have executable reproductions. Automatic synchronization is also not connected end to end.

## Findings

### [P1] Commit the discovery and its sync operation atomically

File: `app/src/main/java/com/campusquest/data/repository/GameRepositoryImpl.kt`, lines 234–240; repeat guard at 205–208.

The progress row is written before its queue entry in separate DAO operations. If queue insertion fails after progress was saved, recordDiscovery returns failure. Retrying sees the checkpoint in completedCheckpointIds and immediately returns success, never creating the missing upload operation. A locally completed discovery can therefore permanently lack a cloud upload, even after M5 connects the remote handler.

Fix: write progress and outbox operation in one Room transaction. Keep repeat handling idempotent, and ensure a failed transaction leaves neither half saved. Apply the same persistence rule to other local-write-plus-outbox operations.

Reproduction: save succeeds; enqueue throws; retry succeeds but enqueue is never called. The regression test expecting an upload operation fails.

### [P2] Do not count deleted checkpoint IDs toward current quest completion

File: `app/src/main/java/com/campusquest/data/repository/GameRepositoryImpl.kt`, lines 210–217. The newly enabled delete operation is at 115–129.

Completion compares the number of historical completed IDs with the current number of checkpoints. After a creator deletes a checkpoint already found by a player, that historical ID remains in progress. Example: A/B/C existed, player found A, creator deletes A, player finds B. The progress list has A and B, the game now has B and C, and the code marks the game complete although C was never found.

Fix: compare membership of current valid checkpoint IDs, not raw historical counts. Define how edits to published games affect existing sessions, scores and versions. Validate that a newly claimed ID belongs to the game.

Reproduction: stored progress contains a removed checkpoint, current game has B/C, then B is discovered. The regression test expecting isCompleted=false fails.

### [P2] Connect worker scheduling and propagate upload failure into retry

Files: `app/src/main/java/com/campusquest/SyncWorker.kt`, lines 24–28 and 42–54; `GameRepositoryImpl.kt`, lines 269–282.

SyncWorker.enqueue is defined but has no production caller. The application does not provide a remoteSyncHandler, so syncPending currently exits without upload; that missing remote handler is acknowledged in the PR as an M5 dependency. Independently, when a handler eventually returns false or throws, syncPending increments retryCount and returns normally. SyncWorker then returns Result.success, so its exponential retry path never handles these upload failures. There is no periodic scheduler to compensate.

Fix: schedule work at agreed write/start/reconnect points, and return a meaningful batch outcome to the worker so failed pending uploads request retry. Test the enqueue/running-write race created by KEEP unique work. Keep no-uploader behavior explicitly deferred instead of claiming live sync.

Evidence: repository-wide reference search finds no caller of SyncWorker.enqueue. Failure handling is visible in the worker and repository; this finding was not reproduced with an instrumented WorkManager test.

## Validation

Command: `gradlew.bat testReleaseUnitTest --console=plain` using Android Studio JBR and an isolated checkout.

- Existing suite: 188 passing tests.
- Added review regressions: 2 failing tests, confirming the first two findings.
- Total run: 190 tests, 2 failures.
- Regression source is preserved as `PR15_REGRESSION_REPRODUCTIONS.kt.txt`, outside the normal test source set. Copy it into the test source directory to reproduce; it intentionally fails on the reviewed commit.
- No clean debug build was attempted; main retains the previously identified Firebase debug package mismatch.
- No device, Firestore or actual WorkManager upload verification was performed for this review.

The PR's upsert test uses fake DAOs, so it cannot by itself prove actual SQLite foreign-key behavior. The production annotation change is appropriate, but add a real Room integration regression for cascade preservation.

## Compatibility with the local UI/story work

- M6 repository method signatures and auth wiring overlap with local real-data/UI branch changes. Do not overwrite either version wholesale.
- Main now seeds The Founder's Lantern at database startup and from GamesViewModel. This would reintroduce an unwanted quest if merged unchanged into the user's single-Test local setup.
- Main still contains mock leaderboard data and mock-auth fallback; local changes removed them.
- Preserve M6 upserts, creator contracts, queue operation types and tests while selectively porting live compass and visual polish.
- The new Light the Way story needs task-specific state/inventory and migrations; the current generic discovery API is not a complete implementation of its puzzles.

## Actions taken by this review

No remote review/comment, merge, push, branch deletion or phone installation was performed. Prepared local branch `codex/light-the-way-game` from updated main, with the accepted story specification and an integration plan. Findings should be resolved in follow-up changes before relying on these paths for the new story.
