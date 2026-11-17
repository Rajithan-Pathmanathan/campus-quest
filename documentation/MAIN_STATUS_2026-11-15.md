# Campus Quest: `main` status for the MVP

**Checked:** `main` @ `63e8fc1` (after PR #16), 2026-11-15
**MVP goal used here:** a player signs in, opens *The Founder's Lantern*, walks to the 3 places, passes each trial, sees the relic and memory, finishes the quest and sees a real leaderboard. A creator can build and publish a quest that other phones can play.

---

## 1. What already works on `main` ✅

| Area | Status |
|---|---|
| Login (email + guest), stays signed in after relaunch | ✅ (PR #6, #14) |
| Quest list, quest details, join | ✅ Story quest seeded, no California data (PR #15/#16) |
| Scan screen: zones, distance words, light/trial/shield hints, heartbeat, relic reveal, "Complete the quest" → leaderboard | ✅ M4 (PR #9) using M3's `QuestCues` |
| Trials: 3 shakes / 10 s steady hold / 3 sweeps | ✅ M3 (PR #7) |
| Progress per player, no double points, quest completes | ✅ M6 (PR #15) |
| Creator: create game, add/edit/delete checkpoints, publish (locally) | ✅ M2 (PR #10) + M6 |
| Campus map (OpenStreetMap), checkpoint circles, "You" marker | ✅ M3 (PR #13), but see 2.5 |

The whole game loop works **offline on one phone**. Anything that needs other phones does not work yet.

---

## 2. MVP blockers 🔴

| # | Problem | Evidence | Owner |
|---|---|---|---|
| 2.1 | **Debug build fails on a clean checkout.** `google-services.json` has only `com.campusquest`, but the debug build adds `.debug`. Everyone has to edit `build.gradle.kts` locally to build. | `app/build.gradle.kts:38`, 1 package in `google-services.json` | **M5** (Console: add `com.campusquest.debug`) |
| 2.2 | **No Firestore at all.** Quests published on one phone never reach another, the leaderboard can't be real, and progress never leaves the phone. | No `FirebaseFirestore` use anywhere in `app/src/main` | **M5** (data source), then **M6** (wire it, 3.1) |
| 2.3 | **The leaderboard shows fake players** (Elena_Rostova, Marcus_Vance, Dev_Priya), and it is the screen shown right after finishing the quest. | `GameRepositoryImpl.observeGameLeaderboard()` ~line 249 | **M5 + M6** |
| 2.4 | **Discovery can never succeed on phones without a real proximity sensor.** Success needs `fusionThresholdReached && proximityNear`. Phones with a "virtual" sensor (e.g. Xiaomi `Virtual_Prox`, including the test phone) never report "near", so the player is stuck on the last step. | `ui/quest/ScanState.kt:70`; no fallback | **M4 + M3** (agree on a fallback, e.g. "hold the shield" button once fusion is reached) |
| 2.5 | **The map's top card is fake placeholder text**: "Target: Old Clock Tower", "Distance: 85m remaining", "Clue: Where shadows stretch longest…". Nothing in the code updates it. The map also never starts location tracking itself, so the "You" marker only appears if the scan screen already started GPS. | `fragment_campus_map.xml:33-51`; only `QuestScanViewModel` calls `startTracking()` | **M3** |
| 2.6 | **The player dashboard is static**: "Welcome, Explorer" and "Score: 0 pts \| Relics Found: 0" are fixed text. | `fragment_player_dashboard.xml:20,29`; the fragment sets no text | **M1** (`getAllPlayerProgress()` now exists, from M6) |
| 2.7 | **The memory texts players see are placeholders**: "[Placeholder — replace with verified … facts]" appears on the relic reveal. | `MockDataCatalog.kt:32,46,60` | **Team / M3** (provide real facts for the 3 places) |

---

## 3. Code that exists but is not wired up 🟠

| # | What | Problem | Owner |
|---|---|---|---|
| 3.1 | `SyncWorker` + `remoteSyncHandler` | The worker is never scheduled and no upload handler is passed, so `syncPending()` returns immediately and `sync_queue` only grows. | **M6** (after M5's Firestore service) |
| 3.2 | `data/remote/dto/FirestoreDtos.kt` | Never used. Also has the `isCompleted` → `completed` field-name bug. | **M5** |
| 3.3 | Push notifications | `CampusQuestMessagingService.onNewToken` is empty; nothing calls `subscribeToTopic("new_games")`; nothing sends a notification when a quest is published. Receiving and opening one does work. | **M5** |
| 3.4 | `POST_NOTIFICATIONS` permission | Declared in the manifest, never requested, so notifications are silent on Android 13+. | **M1** |
| 3.5 | Geofencing | `GeofenceManagerHelper` is never called and `GeofenceBroadcastReceiver` is registered but nothing creates geofences. `ACCESS_BACKGROUND_LOCATION` is declared but never requested. The scan screen's zones already do this job. | **M3**: recommend **removing** it for the MVP rather than wiring it |
| 3.6 | Auth fixes | `AuthRepositoryImpl` still has `firebaseUser!!` (crash risk), never saves the sign-up name (`updateProfile`), and never creates `/users/{uid}`. | **M5** |
| 3.7 | Sign out | There is no sign-out button anywhere in the app. | **M1** |
| 3.8 | Story intro | The intro story cards planned for first launch are not built. | **M1** |
| 3.9 | Creator limits | `MIN_CHECKPOINTS_TO_PUBLISH = 1` and the other limits are still marked PLACEHOLDER. | **Team decision → M2** |

---

## 4. Dead code (never used by the app) 🟡

These are referenced by nothing in `app/src/main`. Delete them, or move test-only helpers into `src/test`.

| File | Used by | Owner |
|---|---|---|
| `ui/creator/CreatorGameViewModel.kt` | nothing | M2 |
| `device/location/MapGeofenceDataProvider.kt` | 1 test only | M3 |
| `device/location/geofence/GeofenceManagerHelper.kt`, `ScopedGeofenceFallback.kt`, `domain/model/GeofenceEvents.kt` | nothing / tests only | M3 (see 3.5) |
| `device/sensor/SensorDiagnosticsHelper.kt`, `SensorSimulationFixture.kt` | tests only | M3 |
| `domain/fusion/DiscoveryGateEvaluator.kt` | tests only. The scan screen uses its own gate in `ScanState.kt`, so **the same rule exists twice**. | M3 + M4 (keep one) |
| `domain/model/FusionState.kt`, `SensorReading.kt` | nothing | M3 |

Small cleanups:
- Remove the stale `// TODO` in `CreatorDashboardFragment.kt:29` (it is already done). (M2)
- `ACCESS_BACKGROUND_LOCATION` should be removed from the manifest if geofencing is dropped. (M3)

---

## 5. Suggested order to reach the MVP

1. **M5:** register the `.debug` app (2.1), set up Firestore, rules and index, and build the Firestore data source (2.2, 3.2). Everything online waits on this.
2. **M4 + M3:** proximity fallback (2.4). Without it the game can't be finished on some phones.
3. **M3:** live map top card and start GPS on the map (2.5); remove geofence and dead code (3.5, section 4).
4. **M1:** real dashboard (2.6), sign out (3.7), notification permission (3.4).
5. **M6:** wire Firestore into `syncPending()`, schedule `SyncWorker`, and use the real leaderboard listener (3.1, 2.3).
6. **Team:** real memory texts (2.7) and creator limits (3.9).
7. **Nice to have:** story intro (3.8), "new quest" push notifications (3.3).
