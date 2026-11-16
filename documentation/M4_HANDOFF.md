# M4 — Quest & Scan Gameplay: Handoff

Owner: M4. Code: `app/src/main/java/com/campusquest/ui/quest/`. Tests: `QuestScan*Test.kt`, `FinalFixTest.kt`.

## What M4 provides

| Piece | File | Notes |
|---|---|---|
| Scan state machine | `ScanState.kt` | Pure Kotlin. `ScanStateMachine.resolve` (from signals) and `canTransition` (guards). |
| Story UI state + copy | `QuestScanUiState.kt` | `ScanNarrator.hud` turns state + M3 signals into story words. No percentages or raw scores reach the screen. |
| ViewModel | `QuestScanViewModel.kt` | Consumes `SensorRepository`, `LocationRepository`, `GameRepository` only. |
| Screen | `QuestScanFragment.kt`, `fragment_quest_scan.xml`, `dialog_clue_reveal.xml` | Draws state; reveal dialog is state-driven, so it survives rotation. |
| Progression | `NextCheckpointResolver.kt` | Next checkpoint = lowest `order` not yet found. Used by the scan (is this the last one?) and the map (which place next). |
| Haptics | `HeartbeatHaptics.kt` | Vibrates at the interval M3's `QuestCues.heartbeatIntervalMs` gives. |

## States

`LOADING → WAITING_FOR_LOCATION ⇄ OUTSIDE_CHECKPOINT ⇄ READY_TO_SCAN → SCANNING ⇄ WAITING_FOR_PROXIMITY → SUCCESS → REVEALING → COMPLETED`
Side exits: `UNAVAILABLE`, `ALREADY_FOUND`, `ERROR` (retryable).

- `READY_TO_SCAN` needs the player to tap "Wake the Resonator" (`beginScan`). Leaving the relic ring or the screen cancels it.
- **Fusion threshold ≠ discovery.** `WAITING_FOR_PROXIMITY` becomes `SUCCESS` only when M3's proximity gate passes, or the player holds the shield fallback (`DiscoveryGateEvaluator`, only after the threshold).
- `SUCCESS` is latched: the discovery is saved once (`discoveryRequested`). A failed save becomes `ERROR`; retry re-saves with the same timestamp and never starts a second scan.
- Evidence whose `gameId#checkpointId` differs from the open checkpoint is ignored.

## Error / unavailable mapping

| Situation | State | Message |
|---|---|---|
| Blank/unknown game or checkpoint, draft game | `UNAVAILABLE` | "Checkpoint unavailable — Return to the quest and try again." |
| Closed game | `UNAVAILABLE` | "Scan unavailable — This game is no longer active." |
| Checkpoint already found | `ALREADY_FOUND` | "You already restored this memory." |
| Load threw | `ERROR` (retry reloads) | "Couldn't load this chapter" |
| `recordDiscovery` failed | `ERROR` (retry re-saves) | "The memory slipped away" |
| No GPS fix / permission denied | `WAITING_FOR_LOCATION` | Searching / "needs your location" + Allow location button |

## Contracts M4 relies on

- M3: `SensorRepository.fusionResult` (`thresholdReached`, `proximityNear`, `progressPercent`), `sensorState` (`currentLux`, `trialStatus`), `LocationRepository.locationState`, `QuestCues`, `nextQuestZone`.
- M5/M6: `GameRepository.getGameDetails`, `getGameCheckpoints`, `observePlayerProgress`, `recordDiscovery` (must be idempotent). The user id comes from `AuthRepository.getCurrentUserId()`.
- M1: nav destinations `questScanFragment(gameId, checkpointId)` and `leaderboardFragment(gameId)`. After the last chapter the scan opens that game's leaderboard; otherwise it returns to the map, which opens the next unfound checkpoint.

## Mock signal scenarios (all covered by unit tests, using `game-alpha` / `game-beta`)

Location unavailable · outside checkpoint · relic appears · scanning · fusion rising · threshold reached · waiting for proximity · proximity near · shield fallback · reveal · discovery saved · save error + retry · final checkpoint · closed/draft game · game isolation. See `QuestScanViewModelTest`, `QuestScanStateMachineTest`, `QuestScanProgressionTest`.

## UI tests

`app/src/androidTest/.../QuestScanScreenTest.kt` (7 Espresso tests, passed on a Galaxy S23 Ultra). It runs the real `QuestScanFragment` with fake repositories injected through `QuestScanFragment.viewModelFactoryOverride`, and stops the compass (its sensor redraws keep the main thread busy, so Espresso would never go idle). Run with `gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.campusquest.QuestScanScreenTest`.

## Look and feel (2026-11-16)

The scan flow and the main player and creator screens were polished in the app's own parchment, forest-green and gold style (no new palette):

- **Scan screen:** dark forest page, serif title with a back arrow, the compass, a story headline, three signal chips (GPS / Light / Motion, each a word and a colour, never a number), a tip card for the current hint, and the leather button. `ScanNarrator.signals` builds the chips and is unit-tested.
- **Reveal dialog:** "Clue found!" with a treasure chest, the clue and lore, a points row (`RevealContent.points`, 100 per checkpoint) and the leather button.
- **Shared:** leather-and-gold buttons and cream, rounded text fields are theme-level (`Widget.CampusQuest.Button`, `Widget.CampusQuest.TextInput`), so every screen picks them up. Toolbars use a real back arrow and a serif title.
- **Quest list and leaderboard:** dark forest page with cream cards; quest cards have a compass badge and a pin icon; the leaderboard has gold, silver and bronze rank badges and a gold edge for the player's own row.
- **Quest details:** status chip, pin icon, a Rewards section and a leaderboard button with a trophy icon.
- **Home and Creator Studio:** score and relic tiles, icons instead of emoji, a quiet account card, a primary and a secondary create button, and an empty state for authored quests.

Debug builds also include a **Scan Preview** launcher icon (`app/src/debug`) that shows the real scan screen with fake data and chips for every state. It is not part of release builds.

## Not done / needs a device

- On-device sensor walk test (light thresholds and the 10 m relic radius are first guesses from M3).
- "Saved locally / waiting to sync" wording: `recordDiscovery` returns only success or failure, so the screen cannot tell local from synced.
- Live ember "flame" graphic: the glow is a 5-step bar with a text label (`EmberGlow`).
