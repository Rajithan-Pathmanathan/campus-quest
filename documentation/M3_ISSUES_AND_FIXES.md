# Member 3 — Issues & Fixes (Location, Geofencing, Sensors, Fusion)

**Audited:** `main` @ `ceb49cf` (2026-11-13)
**Scope:** only code Member 3 owns per `Member3_LOCATION_GEOFENCING_WORKPLAN.md`.
**Instructions for the AI assistant:** fix issues in the order listed. Each issue has the file, the problem, the required fix, and how to verify it. Do not change fusion weights (`FusionConfig`) or touch Member 1/2/4/5/6 code except where an issue explicitly says so. Add/adjust unit tests for every fix. All existing tests must stay green.

---

> **Update 2026-11-13:** H1–H4, M1–M4, L1 (documented) and L2 are fixed on branch `fix/m3-integration-issues`.
> Policies are written down in `M3_INTEGRATION_GUIDE.md` §3. 57/57 unit tests pass (`M3GameLogicTest` adds 11).
> Only **B1** (Member 5) remains open, plus the manual on-device check in *Definition of done* §4.

---

## Current status

| Check | Result |
|---|---|
| Unit tests on `feature/m3-location-sensors-fusion` | ✅ 46/46 pass |
| Unit tests on `main` (release variant) | ✅ 46/46 pass |
| `main` debug build (`assembleDebug`, `testDebugUnitTest`) | ❌ fails — see **Blocker B1** (not M3's code, but blocks M3 device testing) |

### How to run the tests (Windows)

The system Java is 24, which Gradle 8.5 can't run. Use Android Studio's bundled JDK:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew testReleaseUnitTest      # works today
./gradlew testDebugUnitTest        # works only after B1 is fixed
```

---

## Blocker (owned by Member 5, listed so M3 knows why debug fails)

### B1. Debug build fails: `No matching client found for package name 'com.campusquest.debug'`
- **Files:** `app/build.gradle.kts:40` (`applicationIdSuffix = ".debug"`), `app/google-services.json` (only registers `com.campusquest`).
- **Fix (pick one, coordinate with Member 5):**
  - Remove `applicationIdSuffix = ".debug"` from the debug build type, **or**
  - Register `com.campusquest.debug` as a second Android app in the Firebase console and replace `app/google-services.json`.
- **Verify:** `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.

---

## HIGH — gameplay is wrong or the feature can't work on a device

### H1. GPS score is hard-coded to 1.0 — players can "discover" a checkpoint from anywhere
- **Files:** `device/sensor/SensorFusionEngine.kt:46,89` (`currentGpsScore = 1.0f` default), `domain/repository/SensorRepository.kt:22` (`initialGpsScore = 1.0f`), `ui/quest/QuestScanFragment.kt:47` (passes `gpsScore = 1.0f`). There is **no function anywhere** that converts distance-to-checkpoint into a GPS score.
- **Spec:** Workplan §36 — "GPS score should represent how strongly the current physical position matches the checkpoint… closer → higher".
- **Fix:**
  1. Add a pure function in `domain/fusion/` (e.g. `GpsScoreCalculator`):
     - `score(distanceM: Float, radiusM: Float, accuracyM: Float?): Float?`
     - returns `null` when there is no valid fix (so the fusion calculator re-normalises without GPS, see §26/§56 policy), `1.0` at/inside `radiusM`, falling linearly to `0.0` at `2 × radiusM`, clamped to `[0,1]`. Optionally penalise poor accuracy (> 35 m, matching `LocationState.hasValidFix`).
  2. Have `SensorFusionEngine` (or a new orchestrator in `data/repository`) observe `LocationRepository.locationState`, compute distance with the existing `HaversineDistanceCalculator` to the active checkpoint, and feed the result through `updateGpsScore(...)`.
  3. Change the default GPS score from `1.0f` to `0.0f` (or `null`) so a missing update never grants full GPS credit.
- **Verify:** new unit tests — inside radius → 1.0; at 2× radius → 0.0; midway → ~0.5; no fix → null. Golden-path test still passes when the fake location is at the checkpoint, and **fails to reach threshold** when the fake location is 200 m away.

### H2. Runtime location permission is never requested
- **Files:** whole app — `grep requestPermissions|RegisterForActivityResult|checkSelfPermission` returns nothing. `LocationManagerHelper.startLocationUpdates` is annotated `@SuppressLint("MissingPermission")` and just catches the `SecurityException`.
- **Spec:** Workplan §7–§8 (location permissions, permission principle), acceptance test §76.
- **Fix:**
  1. Add a small permission helper (M3-owned, UI-agnostic) that reports `LocationPermissionState` using `ContextCompat.checkSelfPermission` for FINE/COARSE.
  2. Expose it so M1's map screen can launch `ActivityResultContracts.RequestMultiplePermissions` and push the result into `LocationRepository.updatePermissionState(...)`.
  3. `startTracking()` must refuse to start (and set `error`) when permission is not granted, instead of relying on the exception.
  4. `ACCESS_BACKGROUND_LOCATION` (declared in `AndroidManifest.xml`) must be requested **separately and after** foreground location on Android 10+ — and only if the team decides geofences must fire in the background. If not, remove it from the manifest (Play policy risk).
- **Verify:** fresh install on a device/emulator → permission dialog appears on the map/scan screen; deny → UI shows denied state, no crash; grant → location updates start.

### H3. M3 repositories are never provided to the rest of the app; the Scan screen talks to sensors directly
- **Files:** `CampusQuestApplication.kt` only provides `gameRepository`. `LocationRepositoryImpl`, `SensorRepositoryImpl`, `GeofenceManagerHelper`, `MapGeofenceDataProvider` are never constructed. `ui/quest/QuestScanFragment.kt:40` does `SensorFusionEngine(requireContext())`.
- **Spec:** Workplan §45–§46 ("M4 must not access sensors directly"), §48 repository boundary.
- **Fix:**
  1. In `CampusQuestApplication`, add `by lazy` singletons next to `gameRepository`: `locationRepository: LocationRepository`, `sensorRepository: SensorRepository`, and the geofence helper/provider.
  2. Make sure the engine is a **single** instance owned by the repository, not one per fragment.
  3. Tell Member 4 (scan screen owner) to switch `QuestScanFragment` to `sensorRepository` — or, if you are asked to, change only that one constructor line.
- **Verify:** `grep -rn "SensorFusionEngine(" app/src/main/java/com/campusquest/ui` returns nothing.

### H4. `SensorFusionEngine` leaks a coroutine every time it is created
- **File:** `device/sensor/SensorFusionEngine.kt:29,68-79` — the engine creates its own `CoroutineScope(Dispatchers.Default + Job())` and starts collecting in `init`. Nothing ever cancels it. `stopListening()` only stops sensors. Because the Scan fragment creates a new engine per view (H3), every visit leaks a collector.
- **Fix:** add `fun close()` (or `release()`) that cancels `observationJob` and the scope. Call it from the owner (repository/application), not from UI. Once H3 makes the engine a singleton the leak stops growing, but `close()` should still exist for tests.
- **Verify:** unit test that after `close()`, emitting new sensor state no longer changes `fusionResult`.

---

## MEDIUM — spec / architecture violations

### M1. Domain layer depends on Android (`android.location.Location`)
- **Files:** `domain/repository/LocationRepository.kt` imports `com.campusquest.device.location.LocationState`, which has a field `location: android.location.Location?`.
- **Spec:** Workplan §6 — "Gameplay logic must not be tightly coupled to the Android location SDK"; §59 testable abstractions.
- **Fix:** remove the `location: Location?` field from `LocationState` (lat/lng/accuracy/etc. are already copied out). Move `LocationState` and `LocationPermissionState` to `domain/model/`. Update `hasValidFix` to use `smoothedLatitude != null`. Update `LocationManagerHelper.calculateDistanceTo/BearingTo` to use the stored lat/lng.
- **Verify:** `grep -rn "import android" app/src/main/java/com/campusquest/domain` returns nothing; all tests pass.

### M2. `Checkpoint.motionType` is a `String`, but the sensor layer uses `enum MotionType`
- **Files:** `domain/model/Checkpoint.kt:40` (`val motionType: String = "SWEEP"`), `domain/model/MotionType.kt`.
- **Risk:** a typo or lowercase value from Firestore/creator crashes `MotionType.valueOf` or silently picks the wrong detector.
- **Fix:** add a safe mapper in the M3 layer, e.g. `MotionType.fromString(value: String?): MotionType` using `entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MotionType.SWEEP`, and use it wherever a checkpoint starts a scan. (Changing the `Checkpoint` field type itself is a shared-contract change — agree with Members 5/6 first.)
- **Verify:** unit test for `"sweep"`, `"SHAKE"`, `null`, `"garbage"`.

### M3. `DiscoveryGateEvaluator` is injected into the engine but never used
- **Files:** `device/sensor/SensorFusionEngine.kt:28`; the actual gate logic is duplicated in `FusionResult.canClaimDiscovery`.
- **Fix:** pick one source of truth. Simplest: delete the unused `discoveryEvaluator` constructor parameter and keep `FusionResult.canClaimDiscovery`, or make `canClaimDiscovery` delegate to the evaluator. Don't keep both.
- **Verify:** tests still pass; only one place implements "threshold AND near".

### M4. Light and motion only score 0 or 1
- **File:** `domain/fusion/FusionInputs.kt:20-30` (`fromBooleans`).
- **Spec:** Workplan §24–§25 — light match should be converted into the normalised domain using the shared scoring curve.
- **Impact:** with weights 0.4/0.3/0.3 and threshold 0.85, all three channels must be fully matched; the meter jumps in 30 % steps instead of rising smoothly.
- **Fix:** add a continuous light score — `1.0` inside `[minLux, maxLux]`, falling linearly to `0.0` at a tolerance (e.g. 50 % of the range width) outside the range. Use it in `SensorFusionEngine.recalculate` instead of `lightMatched`. Motion can stay binary for the MVP — document that decision in `M3_INTEGRATION_GUIDE.md` (§56 requires the policy be written down).
- **Verify:** unit tests for in-range, at tolerance edge, far outside, and sensor missing (`null`).

---

## LOW — robustness

### L1. Geofence events are lost when the app process isn't running
- **File:** `device/location/geofence/GeofenceBroadcastReceiver.kt:24` — events go into an in-memory `companion object` `SharedFlow`. Nothing persists them, and there is no collector unless the app is open.
- **Fix (MVP-acceptable):** document that geofence ENTER is a foreground-only feature, **or** persist the last event per checkpoint (Room/SharedPreferences) so the map can read it on next launch. Also re-register geofences after reboot only if background geofencing is kept (H2 step 4).

### L2. `PendingIntent.FLAG_MUTABLE` on the geofence intent
- **File:** `device/location/geofence/GeofenceManagerHelper.kt:38`. This is correct: geofencing requires a mutable intent, and it is already explicit (`Intent(context, GeofenceBroadcastReceiver::class.java)`). Only add a one-line comment explaining why it's mutable, so nobody "fixes" it to `FLAG_IMMUTABLE` and breaks geofence delivery.

---

## Definition of done

1. All HIGH issues fixed; MEDIUM fixed or explicitly deferred in `M3_INTEGRATION_GUIDE.md` with a reason.
2. New unit tests for H1, H4, M2, M4 (and M1 if refactored).
3. `./gradlew testReleaseUnitTest` passes (and `testDebugUnitTest` once B1 is fixed).
4. Manual check on a real phone: permission dialog → map shows location → walking into the checkpoint radius raises the GPS part of the meter → light + sweep + covering the proximity sensor enables Claim; standing 200 m away never enables Claim.
5. Work on a branch `fix/m3-integration-issues`, open a PR to `main`; don't push to `main` directly.
