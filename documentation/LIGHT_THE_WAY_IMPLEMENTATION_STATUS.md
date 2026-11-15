# Light the Way — Local implementation and test status

Branch: `feature/light-the-way-game`. Local only. Based on main `0d3b0da`, including M6 PR #15, with selected UI-polish changes integrated.

## Implemented

- Separate Light the Way story entry points from quest details and Creator Studio; old Test play flow remains available.
- Creator setup for three coordinate pairs and lamp bearing. Default third coordinate is 50 metres north of checkpoint 2, calculated on a spherical Earth rather than an arbitrary coordinate offset.
- New game creation preserves existing games and authenticated ownership.
- Unique checkpoint-specific QR card and case-insensitive printed backup code; complete printable PNG export through Android's document picker.
- Device preflight, proximity functional check and the selected proximity slide alternative persisted per account/game.
- Allow location handles runtime permission separately from the device location switch. It offers Android's location-settings resolution dialog, falls back to system settings when needed, and explains app-settings recovery after blocked permission. Access is rechecked on return; the walking screen has the same recovery button.
- GPS-gated, sequential three-chapter flow with stable arrival, an exit margin, stale-fix rejection and explicit waiting/recovery text.
- Tilt-only key maze using all three accelerometer axes, warm-up calibration, screen-rotation handling and wall-tunnelling protection. Drag input and saved touch-mode overrides are removed; the accelerometer is required.
- CameraX QR scanning using bundled ML Kit, in-app camera-owned torch controls, actual torch-state feedback and printed-code fallback.
- Ambient-light-based lighting hint; lux changes never determine QR success.
- Final lamp parts, true-heading alignment or relative gyroscope/touch dial alternative, continuous alignment hold and durable alignment saving.
- Proximity cover hold or explicit slide-and-hold alternative, with matching lamp/cover graphics.
- Per-chapter reveal, inventory, 100-point awards, final results and diagnostics.
- Camera/image/sensor cleanup and paused holds on background/exit.
- Room schema version 2 with explicit non-destructive migration from version 1.
- Transactional discovery and outbox writes; duplicate claims are serialized. Quest completion compares current valid checkpoint IDs, addressing the first two PR #15 review findings.
- Local leaderboard/dashboard reads instead of invented players; no automatic sample quest seeding. Main's creator repository contracts and shared auth are preserved.

## Default route

| Chapter | Latitude | Longitude |
|---|---:|---:|
| Key box | 7.727295 | 81.696698 |
| Repair card | 7.727241 | 81.696882 |
| Meeting lamp | 7.72769066080296 | 81.696882 |

Arrival radius is 15 metres. Initial lamp direction is north (0 degrees). Values can be entered before creating a new story. Existing published story configuration is displayed read-only; arbitrary live editing/version migration is not implemented.

## Validation

### Tilt-only correction

The user removed the key puzzle's touch alternative. Previously, choosing drag controls persisted `motionFallback=true`, which disabled tilt updates with no switch back. The former tilt calculation also ignored gravity Z, reducing forward/back response from an upright pose.

The key puzzle now ignores that legacy flag, has no touch/drag handler, and requires the accelerometer. Start tilting/recalibrate waits for actual sensor samples, captures a neutral pose, and uses filtered three-axis gravity angles. It supports upright/flat holding, display rotation, dead-zone filtering and stale-reading detection. Pause/GPS/sensor readiness is visible to the player. The legacy Room field remains for schema compatibility only and cannot activate touch controls.

201 JVM tests pass, including axis direction, upright-pose response, calibration, freshness and screen-rotation regressions. The device check verifies that real accelerometer readings arrive/calibrate and that touch events cannot move the key. Physical steering through the complete maze still needs the user's manual playthrough.

- 193 JVM tests passed, including maze collisions, QR validation, continuous holds and GPS arrival policy.
- Real Room instrumented tests cover version-1 migration, retained games/progress, upsert checkpoint preservation, rollback when queue insertion fails, repeat awards and deleted-checkpoint completion.
- Device scanner test verified the actual rear camera/torch on and off.
- Generated QR was decoded with ML Kit on the phone and matched against the story configuration.
- Device UI checks created the new local story, confirmed GAME_TEST still exists, and captured authoring/preflight/navigation/task-entry screens.
- The phone denies system touch injection, so UI smoke checks invoke the app's own control listeners; they are not physical touch-gesture validation.
- A stricter live-entry probe could not open chapter 1; the user confirmed being outside the circle and will verify entry manually. GPS availability/accuracy also varied during captures. No GPS arrival or chapter completion was simulated to bypass the gate. The UI smoke check now distinguishes an eligible stable fix from the outside-circle environment rather than requiring the phone to be at a fixed place.

## Field work still required

- Print the generated repair card and place it in a shallow shaded pocket at checkpoint 2.
- Check the third point is safe and accessible and north is the desired final path direction.
- Walk all three stops and verify tilt, camera reading from the installed card, lamp alignment and cover closure on the real route.
- Test more hardware variants, large-font accessibility and performance. This phone reports no separate gyroscope and a virtual proximity sensor; alternative controls must remain available.

## Boundaries

This is a local working implementation, not a completed cloud rollout or a claim that every production acceptance criterion has passed. M5's remote handler and M6's worker scheduling/retry integration remain separate work. Story configuration/session records are local and are not yet serialized to Firestore. Publishing here means available on this phone; it does not promise another device will receive the story.

The fixed story builder supplies the three supported tasks. It is not a generic task-authoring platform, a server-side anti-cheat verifier, a routed walking service or an AR object-recognition system. The proximity control preference persists; unfinished maze state restarts after screen recreation, while chapter rewards and final alignment remain saved.

## Official implementation references

- [ML Kit Android barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android): bundled scanner dependency and InputImage processing.
- [CameraX image analysis](https://developer.android.com/media/camera/camerax/analyze): image closing, lifecycle ownership and keep-latest backpressure.
- [ZXing QRCodeWriter](https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/qrcode/QRCodeWriter.java): generated physical-card encoding.

The original complete story specification remains the design reference. This document records the actual implementation boundary and takes precedence for current status claims.
