# Member 5 — Task List: Firebase, Firestore, Auth & Notifications

**Based on:** `main` @ `8ad34e5` (after PRs #6–#10), 2026-11-15
**Owner:** M5 (chandika-ganganath)
**Works closely with:** M6 (repository and sync call your Firestore code), M1 (login screen, notification permission)
**Reference spec:** `FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md` (section numbers below refer to it), and your workplan's "UPDATE (2026-11-14)" section.

> **Status today:** only `AuthRepositoryImpl` is merged (PR #1). There is no Firestore code, no security rules in the repo, the leaderboard is hardcoded, and the Firebase Console setup is incomplete. Everything online in the app depends on this list.

Each task has a **Done when** line. Do them roughly in order; Part A unblocks the whole team.

---

## Part A — Firebase Console setup (no code, ~1 hour) 🔴 do first

Project: **`campus-quest-17889`** (the team project in `app/google-services.json`).

- [ ] **A1. Fix debug builds.** Debug builds use package `com.campusquest.debug` (`app/build.gradle.kts:40`), but `google-services.json` only has `com.campusquest`, so `assembleDebug` fails for everyone.
  Console → Project settings → **Add app → Android** → package `com.campusquest.debug` → download the new `google-services.json` → replace `app/google-services.json` → commit.
  **Done when:** `./gradlew assembleDebug` works on a clean checkout of `main` without editing `build.gradle.kts`.
- [ ] **A2. Enable sign-in methods.** Authentication → Sign-in method → turn on **Email/Password** and **Anonymous**. (Anonymous is off today; guest login fails with *"This operation is restricted to administrators only."*)
  **Done when:** "Continue as Guest Explorer" signs in and the user appears under Authentication → Users as *Anonymous*.
- [ ] **A3. Create the Firestore database.** Firestore → Create database → **Standard edition**, ID `(default)`, location **`asia-south1` (Mumbai)** (can't be changed later), **production mode**.
- [ ] **A4. Give teammates access.** Project settings → Users and permissions → add M3 and M6 at least, so they can test.

## Part B — Security rules & indexes (in the repo) 🔴

- [ ] **B1. Add `firestore.rules` at the repo root** (spec §43–53) and publish it in the Console. Minimum:
  - users write only their own `/users/{uid}`;
  - anyone reads `PUBLISHED` games; only the creator writes a game and its checkpoints;
  - each player reads/writes only their own progress and their own leaderboard entry.
  ⚠️ **Lesson from M3's field test:** the app publishes a game and its checkpoints in **one batch**. A checkpoint rule that checks the game owner with `get(...)` sees the database *before* the batch, so the first publish is always rejected. Use **`getAfter(...)`** in the checkpoint rule.
  **Done when:** the rules file is committed, published, and a publish from the app is accepted.
- [ ] **B2. Leaderboard index.** Firestore → Indexes → Composite → collection ID `entries`, `score` **Descending**, `completionTimeSeconds` **Ascending**, scope **Collection**. Optionally commit it as `firestore.indexes.json`.

## Part C — Authentication fixes 🟠

File: `data/repository/AuthRepositoryImpl.kt`

- [ ] **C1. Save the display name on sign-up** (line 54). The login screen (M1, PR #6) now asks for a name, but `signUpWithEmail` never calls `updateProfile`, so the name is lost on the next sign-in. Call `firebaseUser.updateProfile(userProfileChangeRequest { displayName = … }).await()`.
- [ ] **C2. Create the `/users/{uid}` profile** (spec §6) after every successful sign-in / sign-up / guest sign-in.
- [ ] **C3. Remove the `!!`** on `firebaseUser` (lines 42, 64, 82). Return `Result.failure(...)` instead of crashing.
- [ ] **C4. Friendly error messages** (spec §60): map the common Firebase errors ("wrong password", "email already in use", "no network") to short, readable messages instead of the raw exception text.
- [ ] **C5. One shared `AuthRepository`.** Today `AppProviders` (M1) creates its own `AuthRepositoryImpl`. Agree with M1/M6 on a single app-wide instance (for example in `CampusQuestApplication`) so the repository and the screens see the same signed-in user. M6 needs this for task M6-B1.
  **Done when:** signing up with a name shows that name on the next sign-in, and the profile document exists in Firestore.

## Part D — Firestore data source 🔴 (the core of M5)

There is **no Firestore code on `main`** today. `data/remote/dto/FirestoreDtos.kt` exists but is unused.

- [ ] **D1. Create a remote data source** (e.g. `data/remote/FirestoreService.kt` interface + `FirestoreServiceImpl.kt`) with the spec's paths:

  | Data | Path (spec) | Operations |
  |---|---|---|
  | User profile | `/users/{uid}` (§6) | save |
  | Game | `/games/{gameId}` (§8) | fetch published, fetch one, publish |
  | Checkpoints | `/games/{gameId}/checkpoints/{checkpointId}` (§13) | fetch for a game, write on publish |
  | Membership | `/gamePlayers/{gameId}_{uid}` (§17) | join |
  | Progress | `/progress/…` (§21) | save, fetch |
  | Leaderboard | `/leaderboards/{gameId}/entries/{uid}` (§26) | submit, observe live |

- [ ] **D2. Complete the DTOs** (`FirestoreDtos.kt`):
  - add `fromDomain(...)` (app → Firestore) next to the existing `toDomain()`;
  - add the missing DTOs: user profile, membership, leaderboard entry;
  - ⚠️ **bug:** in `FirestoreProgressDto` (line 71) a Kotlin `Boolean` named `isCompleted` is stored by Firestore as **`completed`**. Annotate it with `@get:PropertyName("isCompleted") @set:PropertyName("isCompleted")`.
- [ ] **D3. Publish in one batch:** game + all its checkpoints in a single `WriteBatch` (spec §38, publish atomicity).
- [ ] **D4. Live leaderboard:** `observeLeaderboard(gameId)` with a snapshot listener, ordered by score then time.
  ⚠️ **Lesson from M3's field test:** on any listener error (index still building, permission denied, signed out) Firestore **closes the flow with an exception**. If the ViewModel collects it without `.catch { }`, **the app crashes**. Either catch inside the data source or tell M1 to add `.catch` in `LeaderboardViewModel`.
- [ ] **D5. Hand over to M6:** M6's repository calls these functions; you don't need to touch Room. Agree the function names with M6 before writing them.
  **Done when:** a game published on one phone appears in the quest list on another phone, and two players' scores appear on the leaderboard in the right order.

## Part E — Leaderboard data 🟠

- [ ] **E1. Replace the hardcoded leaderboard.** `GameRepositoryImpl.observeGameLeaderboard()` (line 156) returns fixed names (Elena_Rostova, Marcus_Vance, Dev_Priya). With M6, return your Firestore listener instead.
- [ ] **E2. Write an entry after each synced discovery** (spec §54): `userId`, `userName`, `score`, `completionTimeSeconds`, `completedAt`.
- [ ] **E3. Agree one leaderboard model.** The app's `GameLeaderboardEntry` uses `score` + `completionTimeSeconds`; the spec (§27) uses `checkpointsDiscovered` / `totalCheckpoints` / `completionDurationSeconds`. Pick one with the team and update the other.

## Part F — Notifications (FCM) 🟡

- [ ] **F1. Subscribe to `new_games`** after sign-in: `FirebaseMessaging.getInstance().subscribeToTopic("new_games")`.
- [ ] **F2. Store the FCM token** (`CampusQuestMessagingService.onNewToken`, line 16 — currently empty) on the user profile.
- [ ] **F3. Ask M1 for the Android 13+ `POST_NOTIFICATIONS` runtime prompt** (declared in the manifest but never requested). The tap-to-open deep link is already done (M1, PR #6).
- [ ] **F4. Send a notification when a quest is published** (spec §32, §36–37). An Android app can't send to a topic itself. Options:
  - **Free:** a small serverless endpoint (e.g. a **Vercel** function) that the app calls after a successful publish. It must (1) verify the caller's Firebase ID token, (2) check in Firestore that the game is `PUBLISHED` and owned by the caller, (3) send only once per game, and (4) keep the Firebase service-account key **only in Vercel environment variables**, never in the app or Git.
  - **Paid:** a Cloud Function on Firestore writes (needs the Blaze plan).
  - **Demo fallback:** send manually from Firebase Console → Messaging.
  **Done when:** publishing a quest shows "A new relic has been hidden on campus!" on another phone, and tapping it opens that quest.

## Part G — Story quest & housekeeping 🟡

- [ ] **G1. Publish *The Founder's Lantern*** (M3 guide §4.4: Tennis Court, Buddha Statue, Medical Centre) to Firestore under an **official "Campus Quest" creator account**, so the rules accept it. Agree with M6 whether it's seeded locally, in Firestore, or both.
- [ ] **G2. Restrict the API key** in `google-services.json`: Google Cloud Console → Credentials → restrict to Android apps `com.campusquest` (+ `.debug`) with the team's SHA-1 fingerprints.
- [ ] **G3. Tests:** DTO round-trip (domain → DTO → domain), the `isCompleted` field name, and error mapping (C4).

---

## Already proven in M3's sandbox (for reference, not to copy)
M3's private test branch ran this whole list against a separate Firebase project: email + guest sign-in, `/users` profiles, Firestore rules with `getAfter`, leaderboard index, leaderboard entries after each discovery, and the leaderboard crash fix. Ask M3 if you want to see how it behaved.
