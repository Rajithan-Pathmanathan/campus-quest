# Member 5 (Chandika) — Pending Responsibilities & Work

**Role:** Firebase, Firestore, Authentication & Notifications
**GitHub:** chandika-ganganath
**Checked against:** `main` @ `4629c2d` (after PR #14), 2026-11-15
**Full step-by-step checklist:** `documentation/M5_TASKS.md` (on main). This file is the current status of that list.

---

## Summary

| | |
|---|---|
| M5 code merged so far | `AuthRepositoryImpl` only (PR #1) |
| M5 PRs since then | none |
| Firestore code on main | **none** |
| Security rules in repo | **none** |
| Leaderboard | **still hardcoded** (fake names) |
| Notifications | receive/display works (M1); **nothing sends, no token saved, no topic subscription** |
| Team blocker | **debug builds fail on a clean checkout** until A1 is done |

Everything "online" in the app (shared quests, leaderboard, multi-phone play, push notifications) is waiting on M5.

---

## What changed in the project recently (affects M5)

- **PR #13 – Map is OpenStreetMap now.** No Google Maps key or billing is needed. M5 does **not** need to set up a Maps API key. Task G2 now only covers the Firebase key.
- **PR #14 – Users stay signed in across app launches** (`LoginViewModel` resumes the Firebase session). It reuses Firebase's own persistence, so nothing to do for M5, but it makes C5 (one shared `AuthRepository`) more important: every screen must see the same signed-in user.
- **Leaderboard crash guard is already in place.** `LeaderboardViewModel` now has `.catch { }`, so a Firestore listener error shows an error message instead of crashing. M5 can still catch in the data source, but it is no longer a must.
- **Story quest (PR #7).** *The Founder's Lantern* is the official first quest (Tennis Court → Buddha Statue → Medical Centre). M5 publishes it to Firestore (G1).

---

## Pending work, in priority order

### 🔴 Priority 1 — Firebase Console (no code, unblocks the whole team)

Project: `campus-quest-17889`

| # | Task | Status | Evidence on main |
|---|---|---|---|
| A1 | Register Android app **`com.campusquest.debug`**, download the new `google-services.json`, commit it | ❌ Not done | `google-services.json` lists only `com.campusquest`, but `app/build.gradle.kts:38` adds `.debug` → `assembleDebug` fails |
| A2 | Enable **Email/Password** and **Anonymous** sign-in | ❓ Unknown / likely not | Guest login falls back to a local guest when Anonymous is disabled |
| A3 | Create Firestore database: Standard, `(default)`, **asia-south1**, production mode | ❓ Unknown | — |
| A4 | Add teammates (at least M3 and M6) to the Firebase project | ❓ Unknown | — |

### 🔴 Priority 2 — Security rules & index

| # | Task | Status |
|---|---|---|
| B1 | Add `firestore.rules` at the repo root and publish it. Own `/users/{uid}` only; anyone reads `PUBLISHED` games; only the creator writes a game and its checkpoints (**use `getAfter()`** for the checkpoint rule, because games are published in one batch); players write only their own progress and leaderboard entry | ❌ No rules file in repo |
| B2 | Composite index: collection `entries`, `score` DESC, `completionTimeSeconds` ASC (optionally `firestore.indexes.json`) | ❌ |

### 🔴 Priority 3 — Firestore data source (the core of M5)

| # | Task | Status |
|---|---|---|
| D1 | Create `data/remote/FirestoreService` (+ impl) for users, games, checkpoints, gamePlayers, progress, leaderboards (paths in `FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md`) | ❌ No Firestore code anywhere in `app/src/main` |
| D2 | Finish `FirestoreDtos.kt`: add `fromDomain()`, add user/membership/leaderboard DTOs, and **fix `isCompleted`** (line 71). Firestore saves it as `completed` unless it is annotated with `@get:PropertyName("isCompleted") @set:PropertyName("isCompleted")` | ❌ DTOs are unused and have no `fromDomain`; bug still present |
| D3 | Publish game + checkpoints in **one `WriteBatch`** | ❌ |
| D4 | `observeLeaderboard(gameId)` snapshot listener ordered by score, then time | ❌ (the ViewModel crash guard already exists) |
| D5 | Agree function names with **M6**, who calls them from the repository and sync worker | ❌ |

### 🟠 Priority 4 — Authentication fixes (`AuthRepositoryImpl.kt`)

| # | Task | Status |
|---|---|---|
| C1 | Save the display name on sign-up (`updateProfile`). The login screen already sends a name (`LoginViewModel.register`) | ❌ No `updateProfile` call |
| C2 | Create/update `/users/{uid}` after every sign-in, sign-up and guest sign-in | ❌ |
| C3 | Remove the `!!` on `firebaseUser` (lines 42, 64, 82) → return `Result.failure` | ❌ Still there |
| C4 | Friendly error messages (wrong password, email in use, no network) | ❌ Raw exception text |
| C5 | One app-wide `AuthRepository` (agree with M1/M6). `AppProviders.kt:24` creates its own | ❌ |

### 🟠 Priority 5 — Real leaderboard

| # | Task | Status |
|---|---|---|
| E1 | Replace the fake list in `GameRepositoryImpl.observeGameLeaderboard()` (lines 156–162: Elena_Rostova, Marcus_Vance, Dev_Priya) with the Firestore listener (together with M6) | ❌ Still hardcoded |
| E2 | Write a leaderboard entry after each synced discovery (`userId`, `userName`, `score`, `completionTimeSeconds`, `completedAt`) | ❌ |
| E3 | Agree one leaderboard model (app uses `score` + `completionTimeSeconds`; spec §27 uses `checkpointsDiscovered`/`completionDurationSeconds`) | ❌ |

### 🟡 Priority 6 — Notifications (FCM)

| # | Task | Status |
|---|---|---|
| F1 | `subscribeToTopic("new_games")` after sign-in | ❌ Not called anywhere |
| F2 | Save the FCM token in `CampusQuestMessagingService.onNewToken` (line 16, only a comment today) | ❌ |
| F3 | Ask M1 for the Android 13+ `POST_NOTIFICATIONS` runtime prompt (declared in the manifest, never requested) | ❌ |
| F4 | Send "A new relic has been hidden on campus!" when a quest is published: free Vercel function (verify ID token, check owner + `PUBLISHED`, send once, service-account key only in Vercel env vars), or Cloud Function (Blaze plan), or manual send from the Console for the demo | ❌ |

### 🟡 Priority 7 — Story quest & housekeeping

| # | Task | Status |
|---|---|---|
| G1 | Publish *The Founder's Lantern* to Firestore under an official "Campus Quest" creator account; agree with M6 on local seed vs Firestore | ❌ |
| G2 | Restrict the Firebase API key to `com.campusquest` (+ `.debug`) with the team's SHA-1s | ❌ |
| G3 | Tests: DTO round-trip, the `isCompleted` field name, error mapping | ❌ |

---

## Dependencies

- **M6 is blocked by D1/D5:** the repository and sync worker can't upload anything until the Firestore service exists.
- **Everyone is affected by A1:** until the debug app is registered, teammates have to edit `build.gradle.kts` locally to build.
- **M1 needs F3** (notification permission prompt) and C5 (shared auth instance).

## Definition of done for M5

1. `./gradlew assembleDebug` works on a clean checkout of `main`.
2. Signing up with a name shows that name after relaunch, and `/users/{uid}` exists.
3. A quest published on phone A appears on phone B.
4. Two players' scores appear on the leaderboard in the right order (no fake names).
5. Publishing a quest shows a notification on another phone, and tapping it opens that quest.
