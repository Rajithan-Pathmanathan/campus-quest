# Campus Quest — Firebase & Cloud Sync Issues

**Checked on:** `main` @ `f6dcd35` (after PR #4 merge), 2026-11-14
**Checked by:** M3 (code review + running the app on an emulator)
**For:** M5 (Firebase, Auth, Firestore, FCM) and M6 (Room, repository, sync). Some items also need M1 or M2. The owner is listed on each issue.
**Spec used:** `FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md` (section numbers below refer to it)

---

## 1. Short summary

The Firebase SDKs (Auth, Firestore, Messaging) are in the build and `google-services.json` is in the repo, **but the app does not actually use Firebase yet**:

- Login uses a **fake** auth repository (real Firebase login is switched off).
- **No code reads or writes Firestore.** All quests, checkpoints and progress live only on the phone (Room).
- The "sync" function **deletes the upload queue without uploading anything**, so data that should go to the cloud is silently lost.
- The leaderboard shows **hardcoded names**.
- Notifications can be received, but the app never subscribes to the `new_games` topic and nothing sends a notification when a quest is published.
- There are **no Firestore security rules** in the repo.
- **Debug builds fail** because the debug package name isn't registered in Firebase.

### What already works
- Firebase SDKs are added through the Firebase BoM (`32.7.2`) and the google-services plugin.
- `app/google-services.json` is present for project `campus-quest-17889`, package `com.campusquest`.
- `AuthRepositoryImpl` exists and its basic sign-in/sign-up/guest/sign-out calls are written.
- Room tables (games, checkpoints, progress, sync queue) work locally.
- `CampusQuestMessagingService` can show a notification when a message arrives.
- Firestore data classes exist in `FirestoreDtos.kt` (but they are not used).

---

## 2. Issue list (by priority)

| # | Issue | Owner | Severity |
|---|---|---|---|
| F1 | Debug build fails (`.debug` package not in `google-services.json`) | M5 | 🔴 Blocker |
| F2 | `syncPending()` deletes queued data without uploading it | M6 (+M5) | 🔴 Critical (data loss) |
| F3 | Real Firebase login is switched off (mock login used) | M5 + M1 | 🔴 High |
| F4 | No Firestore read/write code at all | M5 | 🔴 High |
| F5 | Every player is saved as `"current_user"` | M6 + M5 | 🔴 High |
| F6 | Firebase Console setup not finished (Anonymous auth disabled, etc.) | M5 | 🔴 High |
| F7 | No Firestore security rules / indexes in the repo | M5 | 🟠 High |
| F8 | Leaderboard is hardcoded and its model doesn't match the spec | M5 (+M6) | 🟠 High |
| F9 | Sync queue items carry no data, wrong types, missing operations | M6 | 🟠 High |
| F10 | Claiming the same checkpoint twice gives points twice | M6 | 🟠 Medium |
| F11 | Game membership (`/gamePlayers`) not implemented | M5 | 🟠 Medium |
| F12 | Push notifications (FCM) not wired end to end | M5 (+M1, M2) | 🟠 Medium |
| F13 | `AuthRepositoryImpl` gaps (display name, user profile, crashes, errors) | M5 | 🟡 Medium |
| F14 | `FirestoreDtos.kt` unused, incomplete, and has a field-name bug | M5 | 🟡 Medium |
| F15 | No background sync job (WorkManager) or "back online" trigger | M6 | 🟡 Medium |
| F16 | No offline/error states for Firebase failures | M5 + M6 | 🟡 Low |
| F17 | API key restriction and SHA-1 not set | M5 | 🟢 Low |
| F18 | No tests for any Firebase or sync code | M5 + M6 | 🟡 Medium |

---

## 3. Issue details

### F1 — Debug build fails 🔴 Blocker · M5
- **Where:** `app/build.gradle.kts:40` has `applicationIdSuffix = ".debug"`
- **What's wrong:** Debug builds use the package `com.campusquest.debug`, but `google-services.json` only has a client for `com.campusquest`. The build stops with *"No matching client found for package name 'com.campusquest.debug'"*.
- **Why it matters:** Nobody can press Run in Android Studio on `main` without editing the build file first.
- **Fix (pick one):**
  1. In the Firebase Console, add a second Android app with package `com.campusquest.debug`, download the new `google-services.json`, and commit it. **(Recommended: keeps debug and release apps separate.)**
  2. Or remove the `applicationIdSuffix` line.
- **Done when:** `./gradlew assembleDebug` succeeds on a clean checkout of `main`.

### F2 — Sync deletes data without uploading 🔴 Critical · M6 (+M5 for the upload calls)
- **Where:** `GameRepositoryImpl.syncPending()` (`data/repository/GameRepositoryImpl.kt:171-177`)
  ```kotlin
  for (item in pending) {
      // Push to Firestore and remove on success
      syncQueueDao.deleteById(item.syncId)   // <-- deletes, never pushes
  }
  ```
- **What's wrong:** Every item in the upload queue is deleted, but nothing is sent to Firestore.
- **Why it matters:** As soon as anyone calls `syncPending()`, every queued quest, checkpoint and discovery is lost for good, and the app gives no sign that anything failed. This is the most dangerous bug on `main`.
- **Fix:** For each item, call the matching Firestore write (from M5's remote data source, see F4). **Delete the item only after the write succeeds.** On failure, keep it and increase a retry counter. Spec §40 (Cloud Synchronization Contract).
- **Done when:** A unit test with a fake remote shows that failed uploads stay in the queue and successful ones are removed.

### F3 — Real Firebase login is switched off 🔴 High · M5 + M1
- **Where:** `ui/common/AppProviders.kt` → `private const val USE_REAL_AUTH = false`
- **What's wrong:** Login uses `MockAuthRepository`. Any email with a password of 6+ characters signs in, and Firebase is never called.
- **Why it matters:** There are no real user IDs, so per-user progress, leaderboards and creator ownership can't work.
- **Fix:**
  1. Finish the Console setup (F6), then set `USE_REAL_AUTH = true`.
  2. Better: make `AuthRepositoryImpl` the default, and use the mock only when Firebase isn't available, so nobody has to remember to flip a switch.
  3. Create **one** app-wide `AuthRepository` (e.g., in `CampusQuestApplication`) instead of creating it inside `AppProviders`.
- **Done when:** Signing in with a wrong password shows Firebase's error, and a new account appears in the Firebase Console → Authentication → Users.

### F4 — No Firestore code at all 🔴 High · M5
- **Where:** The whole `data/` package. `firebase-firestore-ktx` is a dependency, but nothing calls `FirebaseFirestore`.
- **What's wrong:** There is no remote data source. Quests and checkpoints come only from local sample data in Room.
- **Why it matters:** A quest created on one phone can never appear on another phone. "Publish to campus" does nothing outside the device.
- **Fix:** Add a small Firestore data source (e.g., `data/remote/FirestoreDataSource.kt`) with the paths from the spec:

  | Data | Path (spec) |
  |---|---|
  | User profile | `/users/{uid}` (§6) |
  | Game | `/games/{gameId}` (§8) |
  | Checkpoint | `/games/{gameId}/checkpoints/{checkpointId}` (§13) |
  | Membership | `/gamePlayers/{gameId}_{uid}` (§17) |
  | Discovery | `/progress/{uid}/games/{gameId}/checkpoints/{checkpointId}` (§21) |
  | Leaderboard | `/leaderboards/{gameId}/entries/{uid}` (§26) |

  Then (with M6) make `getAvailableGames()` refresh published games from Firestore into Room. The UI keeps reading from Room.
- **Done when:** A game added by hand in the Firestore console shows up in the app's quest list.

### F5 — Every player is `"current_user"` 🔴 High · M6 + M5
- **Where:** `GameRepositoryImpl.kt:105` (`joinGame`), `:130` and `:134` (`recordDiscovery`)
- **What's wrong:** The user ID is a fixed string, so all players on a device share the same progress, and uploads would have no real owner.
- **Fix:** Pass the `AuthRepository` into `GameRepositoryImpl` and use `getCurrentUserId()`. If it's `null`, return `Result.failure("Not signed in")`.
- **Done when:** Two different accounts on the same phone have separate progress.

### F6 — Firebase Console setup not finished 🔴 High · M5
- **What we saw:** On the emulator, Firebase guest sign-in returned *"This operation is restricted to administrators only."* That means **Anonymous sign-in is disabled**.
- **Checklist for project `campus-quest-17889`:**
  - [ ] Authentication → Sign-in method → enable **Email/Password**
  - [ ] Authentication → Sign-in method → enable **Anonymous** (the login screen has "Continue as Guest")
  - [ ] Firestore Database → **Create database** (pick a nearby region, e.g. `asia-south1`)
  - [ ] Publish security rules (F7)
  - [ ] Create the leaderboard index (F8)
  - [ ] Add the debug app `com.campusquest.debug` (F1)
  - [ ] Give the other members access to the project (at least M3 and M6) so they can test
- **Done when:** All boxes are ticked and guest login works with real Firebase.

### F7 — No Firestore security rules in the repo 🟠 High · M5
- **Where:** There is no `firestore.rules`, `firestore.indexes.json` or `firebase.json` at the repo root on `main`.
- **Why it matters:** Without rules, the database is either fully locked (the app fails) or open to anyone with the project ID (anyone can edit or delete quests and leaderboards).
- **Fix:** Add `firestore.rules` following spec §43–53:
  - users can only write their own `/users/{uid}`
  - only the creator can edit a game, and only while it's `DRAFT` (§47: published games are immutable)
  - discoveries can be created once and never edited (§51–52)
  - leaderboard entries can only be written by their own user (§53)

  Commit the file so the team can review it, and publish it in the Console.
- **Note:** `full-app-test` has a draft `firestore.rules` that can be used as a starting point.

### F8 — Leaderboard is hardcoded and doesn't match the spec 🟠 High · M5 (+M6)
- **Where:** `GameRepositoryImpl.observeGameLeaderboard()` (`:156-165`) returns a fixed list (Elena_Rostova, Marcus_Vance, Dev_Priya).
- **What's wrong:**
  1. No real data. Nothing writes leaderboard entries after a discovery.
  2. **Model mismatch:** the app's `GameLeaderboardEntry` uses `score` + `completionTimeSeconds`, but the spec (§27) uses `checkpointsDiscovered`, `totalCheckpoints`, `isCompleted` and `completionDurationSeconds`. The team needs to pick one.
- **Fix:**
  1. Agree on one model and update either the spec or the data class.
  2. After each successful discovery, update `/leaderboards/{gameId}/entries/{uid}` (spec §54).
  3. Read it with a Firestore snapshot listener so it updates live.
  4. Create the composite index the query needs. The first failed query prints a one-click link in Logcat.
- **Done when:** Two test accounts claim checkpoints and both appear on the leaderboard in the right order.

### F9 — Sync queue items are empty or wrong 🟠 High · M6
- **Where:** `GameRepositoryImpl.kt`
  - `createGame`, `createCheckpoint`, `publishGame`, `recordDiscovery` all save `payloadJson = "{}"`, so the queue doesn't record **what** to upload.
  - `createCheckpoint` (`:59`) uses `SyncOperationType.UPDATE_CHECKPOINT` instead of a create type.
  - `updateCheckpoint` (`:70`) and `joinGame` (`:101`) **enqueue nothing**, so those changes would never reach the cloud.
- **Fix:** Store the entity's ID plus a `gameId` (and the `uid` where needed) so the worker can reload the latest row from Room and upload it. Add the missing operation types and enqueue calls.
- **Done when:** Every write method in `GameRepositoryImpl` adds a queue item that contains enough information to upload it.

### F10 — Same checkpoint can be claimed twice 🟠 Medium · M6
- **Where:** `GameRepositoryImpl.recordDiscovery()` (`:128-154`)
- **What's wrong:** The checkpoint list is de-duplicated, but `score` still goes **+100** and `currentCheckpointOrder` still goes **+1** on a repeat claim. `isCompleted` is never set to `true`, and `completedAt` is overwritten on every discovery.
- **Fix:** If `checkpointId` is already in `completedCheckpointIds`, return success without changing anything (spec §24 and §52: discovery is unique). Set `isCompleted = true` and `completedAt` only when every checkpoint of the game is found.
- **Done when:** A unit test that claims the same checkpoint twice keeps a score of 100.

### F11 — Game membership not implemented 🟠 Medium · M5
- **Where:** `joinGame()` only writes local progress.
- **What's wrong:** The spec (§17–20) requires a `/gamePlayers/{gameId}_{uid}` document when a player joins. Without it, the creator can't see who joined, and the security rules can't check membership before allowing discoveries.
- **Fix:** Write the membership document when joining (through the sync queue if offline).

### F12 — Push notifications not wired end to end 🟠 Medium · M5 (+M1, M2)
- **Where / what's wrong:**
  1. Nothing calls `FirebaseMessaging.getInstance().subscribeToTopic("new_games")`, so the device never receives topic messages.
  2. `POST_NOTIFICATIONS` is declared in the manifest but **never requested at runtime**. On Android 13+, notifications are blocked until the user allows them. (M1 owns the permission prompt.)
  3. `CampusQuestMessagingService.onNewToken()` is empty (`:16-19`).
  4. A tapped notification passes `EXTRA_GAME_ID`, but `MainActivity` never reads it, so it can't open the quest (spec §35 deep link). (M1)
  5. **Nothing sends a notification when a quest is published.** An Android app can't send to a topic by itself. This needs a Cloud Function triggered when `status` becomes `PUBLISHED` (spec §32, §36–37), or a manual send from the Console for the demo.
  6. Publishing itself isn't wired yet: Creator Studio's buttons only show a Toast. (M2)
- **Done when:** A message sent from the Console to topic `new_games` shows on an Android 13+ phone, and tapping it opens that quest.

### F13 — `AuthRepositoryImpl` gaps 🟡 Medium · M5
- **Where:** `data/repository/AuthRepositoryImpl.kt`
- **What's wrong:**
  1. `signUpWithEmail` returns the display name but **never saves it to Firebase** (no `updateProfile`), so the name is lost on the next sign-in.
  2. No `/users/{uid}` profile document is created on sign-up (spec §6).
  3. `firebaseUser!!` is used in three places. If the user is `null`, this throws a crash-style exception instead of a clean error.
  4. `signInWithEmail` doesn't trim the email.
  5. Raw Firebase error text reaches the UI (e.g., *"The supplied auth credential is incorrect, malformed or has expired"*). Map the common errors to simple messages (spec §60).
  6. `signInAnonymously` sets `displayName = "Guest"`, but the rest of the app uses "Guest Explorer".

### F14 — `FirestoreDtos.kt` unused, incomplete, field-name bug 🟡 Medium · M5
- **Where:** `data/remote/dto/FirestoreDtos.kt`
- **What's wrong:**
  1. Nothing uses these classes.
  2. They only convert **from** Firestore to the app (`toDomain()`). There's nothing to convert app objects into Firestore documents.
  3. DTOs for user profile, membership and leaderboard entry are missing.
  4. **Bug:** In `FirestoreProgressDto`, a Kotlin `Boolean` named `isCompleted` is saved by Firestore as **`completed`**, not `isCompleted`, so it never matches the spec. Add `@get:PropertyName("isCompleted") @set:PropertyName("isCompleted")`.
  5. `FirestoreCheckpointDto` stores `minLux`/`maxLux` as flat fields, while the spec (§14) describes a light-signature object. Pick one.

### F15 — No background sync job 🟡 Medium · M6
- **Where:** `androidx.work` is a dependency, but there is no `Worker` class and nothing schedules one. Nothing calls `syncPending()`.
- **Fix:** Add a `SyncWorker` that calls `syncPending()`. Schedule it with a "network connected" constraint and exponential back-off, run it once at app start, and run it after each write that adds to the queue.

### F16 — No offline/error states for Firebase 🟡 Low · M5 + M6
- No code checks whether Firebase is reachable (spec §61–64). When Firestore is wired in (F4), offline mode should keep reading from Room, and the UI should show a small "offline" note instead of an error screen.

### F17 — API key restriction and SHA-1 🟢 Low · M5
- `google-services.json` is committed. That's normal for Firebase, but the API key inside it should be **restricted** in Google Cloud Console → Credentials (Android apps, package `com.campusquest`, plus the team's SHA-1 fingerprints).
- The file has no OAuth client (no SHA-1 registered). That's fine for Email/Password and Anonymous login, but required if Google Sign-In is ever added.

### F18 — No tests for Firebase or sync code 🟡 Medium · M5 + M6
- `app/src/test` has only M3 tests. Nothing tests `AuthRepositoryImpl`, `GameRepositoryImpl`, the sync queue or the DTO mapping.
- **Minimum suggested tests:**
  - sync keeps failed items (F2)
  - a repeat discovery doesn't add score (F10)
  - DTO → domain → DTO round trip (F14)
  - `joinGame` uses the signed-in user's ID (F5)

---

## 4. Suggested order of work

1. **F1** (debug build) and **F6** (Console setup): unblocks everyone. *(about 1 hour)*
2. **F2** (stop deleting the queue): one-line safety fix now, full upload later.
3. **F3 + F5 + F13**: real login and real user IDs.
4. **F4 + F14 + F9 + F15**: Firestore data source, DTOs, sync queue and worker.
5. **F7 + F11**: rules and membership.
6. **F8 + F10**: real leaderboard and duplicate-claim fix.
7. **F12**: notifications (with M1 and M2).
8. **F16–F18**: polish and tests.

## 5. Dependencies on other members
- **M2 (Creator Studio):** Save and Publish buttons must call `createGame`, `createCheckpoint` and `publishGame`. Until they do, there is nothing to upload or announce.
- **M1 (Player UI):** Notification permission prompt, notification deep link, and showing the signed-in user's name and score on the dashboard.
- **M3 (Location & sensors):** Nothing needed. `recordDiscovery` is already called correctly from the scan screen once the claim passes the sensor checks.
