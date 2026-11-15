# Light the Way — UI team handoff

## Run and open the story

Build with Android Studio/JDK 17+ and the committed Gradle wrapper. Debug uses the Firebase-registered `com.campusquest` package. The game is local-first; Firestore story publishing is not connected yet.

Sign in, open Creator Studio and select **Create Light the Way story**. The form supplies the user's three-stop test route; confirm the coordinates and lamp bearing before creating. This creates a separate three-chapter game under the signed-in creator. Existing games and progress are preserved. Fresh installs do not automatically receive sample quests.

Open the new quest and save/print its checkpoint-2 repair card using **View / save repair card**. Put the card in a shaded cardboard pocket at checkpoint 2. The QR and printed backup code are unique to that local game; do not substitute the card from another installation.

## Gameplay contract to preserve

- Chapter 1 is tilt-only. No drag mode, automatic touch fallback or touch completion. Accelerometer readings and neutral calibration are required; a gyroscope is not required for this task.
- The key maze has eight barriers: five alternating long walls and three short obstacles. Rendering and collision use `KeyBox.walls`, and `KeyBox.RADIUS` determines the player's collision size. Change geometry in that model rather than drawing separate cosmetic obstacles.
- The gold exit is at the top right. Tests verify the complete switchback route and blocked shortcuts. Preserve a solvable path when polishing the board.
- Chapter 2 uses the real camera and camera-owned flashlight, with a printed-code alternative. The light is optional in daylight. Do not substitute an animated toggle for actual torch control.
- Chapter 3 aligns the lamp and closes its cover. The proximity slide/hold alternative is separate from the removed key-maze touch controls and is still needed for missing or unreliable proximity hardware.
- Location gates remain real. Entry requires recent accurate readings inside the circle; do not add a hidden bypass to make a preview look ready. The UI explains waiting, calibration and GPS pauses.
- Awards and sync-queue writes share a Room transaction. Completed chapter IDs, inventory-derived progress and final alignment survive restarts.

## Useful files

| Area | File under app/src/main/java/com/campusquest |
|---|---|
| Story/game screens | `story/StoryGameFragment.kt` |
| Creator setup/card access | `story/StorySetupFragment.kt` |
| Maze geometry and collision | `story/KeyBox.kt` |
| Themed maze rendering | `story/KeyBoxView.kt` |
| Tilt filtering/calibration | `story/TiltControl.kt` |
| Hardware registration | `story/StorySensors.kt` |
| Lamp/cover graphics | `story/LampView.kt` |
| Camera/torch/QR lifecycle | `story/CardScanner.kt` |
| Printable QR card | `story/RepairCardArtwork.kt` |
| Location recovery | `story/LocationAccess.kt` |
| Compass and warmth meter | `ui/quest/TrailCompassView.kt` |

Theme colors and shapes are in `app/src/main/res/values` and `drawable`. The story uses dynamic native views; preserve IDs/behavior and keep controls reachable when replacing layouts.

## Verification and limits

Run `testDebugUnitTest assembleDebug assembleDebugAndroidTest`. Room/device tests cover migration, atomic saving and real camera/torch behavior. The route-dependent UI smoke test requires usable GPS and should be assessed according to the device's actual location. TiltHardwareTest verifies real accelerometer readiness and that finger gestures do not move the key.

The full three-stop walk, printed-card scan on site, large-font accessibility and additional phone variants still need field testing. Cloud story sync and worker scheduling remain follow-up work. See `LIGHT_THE_WAY_IMPLEMENTATION_STATUS.md` and the full story specification for details.
