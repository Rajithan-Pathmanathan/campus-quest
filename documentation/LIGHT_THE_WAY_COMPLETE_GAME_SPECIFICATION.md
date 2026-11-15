# Light the Way — Complete Game Story and Implementation Specification

**Application:** Campus Quest  
**Date:** 15 November 2026  
**Local branch:** `member-3-ui-polish`  
**Audience:** M1–M6 developers, game creators, testers and assessment presenters  
**Status:** The user accepted the story direction and minimal physical setup. This is the detailed design for implementation, not a claim that every feature already exists. Numerical thresholds are initial proposals that require field testing.

---

> **Later requirement update:** The key puzzle is now tilt-only. The user explicitly removed its touch/drag alternative. An accelerometer with usable readings is required, and preflight must block unsupported devices. This update overrides the key-puzzle touch fallback described in the original design below. The proximity cover's separate slide fallback remains supported.

## Contents

1. Purpose and design rules
2. Full story and chapter sequence
3. Physical setup and route
4. Device checks and permissions
5. Starting the game
6. Walking, guidance and arrival
7. Chapter 1: The Key in the Box
8. Chapter 2: The Hidden Repair Card
9. Chapter 3: Bring Back the Light
10. Ending and scoring
11. Hardware and alternative controls
12. State, saving and recovery
13. Creator Studio
14. Suggested data contracts
15. Visuals, accessibility and feedback
16. Assessment evidence
17. Team responsibilities and delivery order
18. Acceptance tests
19. Demonstration procedure
20. Current prototype and outstanding decisions

## 1. Purpose and design rules

Light the Way is a three-checkpoint campus adventure. The player visits real places and completes story tasks using the phone. They find a virtual key, scan a real repair card and fix a virtual meeting lamp.

The goal is to make location services and hardware essential to the experience without presenting unrelated sensor exercises. Tilting moves a key. The camera reads a real card. The flashlight illuminates it when needed. Turning the phone aims a lamp. Covering a sensor closes the lamp cover.

### 1.1 Player language

Use simple words: key, box, lamp, light, cover, card and path. Instructions must say exactly what the player should do. For example, “Tilt your phone left and right” is clearer than “Wake the signal.” Avoid words such as resonator, relic and ember when plain alternatives communicate the task better.

Give one main action at a time. Keep the current instruction visible and allow the player to read the clue again. Engineering names belong in a separate diagnostics screen.

### 1.2 Interaction rules

- Every action must have a purpose in the story and visible feedback.
- Do not award success just because the player entered a radius.
- Do not silently pass a task because hardware is missing.
- Do not force flashlight use when daylight already makes the card readable.
- Do not invent a distance, compass direction, sensor reading or player score.
- Missing optional hardware must select a real alternative challenge.
- Save completed chapters immediately and award their points once.
- Ask players to stop in a safe place before motion puzzles.

### 1.3 Scope

The game requires three real locations and one printed card installation. The box, key, lamp, glass cover and inventory exist inside the app. A real lamp, real key, real box, microphone challenge or augmented-reality object recognition is not required.

## 2. Full story and chapter sequence

### 2.1 Game title

**Light the Way**

### 2.2 Short description for the quest list

> The campus welcome walk starts soon, but the meeting lamp is not working. Find its key, follow the repair note and bring its light back.

### 2.3 Full description for quest details

> An old lamp marks the meeting place for the campus welcome walk.
>
> After a windy night, the key has gone missing. The repair card is at a nearby board, and the lamp is facing away from the path.
>
> Someone has left clues to help you put everything back.
>
> Start near the garden. Find the key, collect the glass cover and return to the meeting place. With your help, the lamp can guide the next group of students.

The story is fictional. Real locations provide the setting; the app must not present invented campus history as fact.

### 2.4 Complete story progression

| Stage | Story event | Player action | Result |
|---|---|---|---|
| Introduction | The meeting lamp is broken | Read the problem and start the walk | First destination unlocked |
| Chapter 1 | The key is trapped inside a box | Tilt the phone to guide the key out | Key saved; repair-card clue revealed |
| Chapter 2 | The repair card is inside a shaded pocket | Scan the card, using the light if needed | Virtual cover saved; final destination revealed |
| Chapter 3 | The lamp needs its parts and direction restored | Fit parts, aim lamp and close cover | Lamp lights; quest completes |
| Ending | The welcome walk has its meeting light | View the completed story and results | Progress and rewards remain saved |

The player performs the repair. There is no need for additional named characters or a complicated background story.

## 3. Physical setup and route

### 3.1 Minimum physical kit

1. One small printed repair card containing a QR code and a short backup code.
2. One folded piece of cardboard forming a shallow shaded pocket around the card.
3. Removable tape or a clip to attach the pocket to an approved board or stand.
4. Optional weather protection that avoids strong camera glare.

The card is the only physical clue. Players do not remove any real object from it.

### 3.2 Placement by chapter

| Chapter | Suitable setting | Physical item required |
|---|---|---|
| 1 | Garden, courtyard or safe open space | None |
| 2 | Covered noticeboard or approved stand | Printed card in shaded pocket |
| 3 | Open meeting place beside a safe path | None |

Place the card where it is easy to reach and photograph. Do not require climbing, entry into restricted areas or walking into darkness. The cardboard pocket should provide useful shade without making camera focus difficult. Test the QR print size, viewing distance and paper finish on actual phones.

The flashlight has a practical purpose in a dim pocket. If the same card is already clear in daylight, scanning should succeed without turning the light on.

### 3.3 Coordinates and existing Test game

The existing local two-checkpoint Test game uses these positions:

| Existing test checkpoint | Latitude | Longitude |
|---|---:|---:|
| 1 | 7.727295 | 81.696698 |
| 2 | 7.727241 | 81.696882 |

These are test positions, not an approved three-stop route for this story. The third coordinate has not been supplied. Do not invent it or automatically overwrite the Test game. The first two positions may be reused after confirming the route, safe stopping space and card placement.

### 3.4 Proposed location settings

| Setting | Initial proposal | Reason |
|---|---|---|
| Arrival radius | 15 m, configurable | Practical starting point for outdoor GPS |
| Maximum reported accuracy for task entry | 20 m | Reject clearly poor fixes |
| Stable arrival period | About 3 seconds of usable recent fixes | Reduce single-fix jumps |
| Exit boundary | Arrival radius plus 10 m | Avoid repeated entry/exit flicker |
| Maximum fix age for new entry | About 15 seconds | Avoid unlocking from stale position |

These values require testing and do not guarantee the player's physical position. Closely spaced or overlapping circles must still unlock only the next chapter. Reported GPS accuracy is an estimate, not proof of location.

## 4. Device checks and permissions

### 4.1 Preflight purpose

Before walking starts, determine the controls the phone can use. Check four distinct conditions: hardware exists, permission is granted, the feature is currently available and its readings are usable.

A phone may report a proximity sensor that is virtual or unreliable in this usage. A camera or torch may exist but be busy. Recheck during gameplay as well as before starting.

### 4.2 Capability checks

| Feature | What to verify | Outcome if unavailable |
|---|---|---|
| Location | Permission, enabled services and usable fix | Explain recovery; no false arrival |
| Accelerometer/gravity | Presence and useful readings | Drag controls for the key |
| Gyroscope | Presence and useful readings | Use another appropriate orientation source or touch controls |
| Compass/orientation | Heading source and reliability | Map/distance guidance and relative lamp alignment |
| Camera | Camera exists, permission and opening succeeds | Printed-code entry |
| Flashlight | Flash unit and availability | Available-light scanning or printed code |
| Ambient light | Sensor presence and plausible readings | Camera brightness or manual light control |
| Proximity | Usable far-to-near-to-far readings | Drag-and-hold cover |
| Vibration | Capability and preference | Visual and optional sound feedback |

### 4.3 Player-facing preflight

> **Getting your tools ready…**
>
> Your phone is ready for the walk.

If a proximity check is needed:

> Cover the top front of your phone, then move your hand away.

Show a hand illustration. A proposed timeout is 8 seconds. If the expected change does not happen, explain the alternative rather than making the player retry forever:

> Your phone will use a slide control to close the lamp cover.

The summary may show “Tilt controls ready,” “Camera available” and “Lamp cover: touch control.” It must reflect actual capability.

### 4.4 Permission flow

Explain the feature before requesting permission. Request location when starting navigation and camera when opening the scanner. No microphone permission is needed for this story.

Camera denial leads to printed-code entry. Location denial prevents location-verified play and leads to a clear settings/retry path. Never label a denied feature as successfully checked.

### 4.5 In-game failure

If hardware fails after the game begins, pause the current task, explain the available alternative and preserve completed steps. A selected alternative must be recorded so diagnostics accurately describe the playthrough.

## 5. Starting the game

### 5.1 Quest details screen

Show the title, description, three chapter names, physical-card requirement and control alternatives. Show an estimated duration only after the real route has been selected and tested. Until then, use “Three short chapters.”

### 5.2 Opening story card

> **Can you help light the way?**
>
> The welcome walk starts soon, but the old meeting lamp is not working.
>
> Find its key, follow the repair note and bring its light back.
>
> Your first clue is near the garden.

Button: **Find the key**

### 5.3 Inventory

Start with three simple illustrated states: key not found, glass cover not found and lamp needs repair. Inventory changes follow saved task completion, not an animation alone.

### 5.4 Starting or resuming

New players begin at chapter 1. Returning players resume the next unfinished chapter with their saved items. Existing Firebase sessions should continue without another login. An explicit sign-out still returns the player to authentication.

## 6. Walking, guidance and arrival

### 6.1 Navigation screen

Show the next chapter name, clue, approximate distance, map, direction arrow and cold-to-hot meter. Use real checkpoint data and location updates.

The arrow indicates direct bearing, not a routed path around buildings. Display:

> Follow safe paths. The arrow points toward the place.

### 6.2 Proposed warmth labels

| Distance | Label |
|---|---|
| More than 200 m | Cold |
| More than 100 m, up to 200 m | Cool |
| More than 30 m, up to 100 m | Warm |
| More than 10 m, up to 30 m | Hot |
| 10 m or less | Very hot |

The proximity meter measures closeness only. It is separate from puzzle progress and arrival validation. At a configured 15 m radius, a task may unlock while the meter still says Hot. The arrival card must clearly explain that the player has reached the marked area.

### 6.3 Missing or unreliable readings

Without a usable fix, show “Waiting for GPS” and no invented distance. Without reliable orientation, show the map and distance rather than a confident incorrect arrow. Explain compass calibration in simple language when needed.

A gyroscope alone cannot provide absolute north. Use a suitable orientation source and account for magnetic declination when aligning a true bearing.

### 6.4 Arrival behavior

Once the arrival condition is met, give a short vibration if enabled and show the chapter entry card. An explicit button starts the puzzle so an automatic location update does not unexpectedly interrupt the player.

Before a motion task, show:

> Stop in a safe place before you begin.

## 7. Chapter 1 — The Key in the Box

### 7.1 Walking clue

> Someone left the lamp key near the garden. Follow the arrow to find it.

### 7.2 Arrival card

> **You found the box!**
>
> The key is inside, but it has slipped behind the wooden bars.
>
> Tilt your phone to slide it toward the opening.

Button: **Open the box**

### 7.3 Puzzle screen

Show a shallow wooden box viewed from above, a key, two simple wooden bars and a clearly marked exit. The puzzle should be short and understandable, not a difficult maze.

> Tilt left or right to move the key sideways.
>
> Tilt forward or back to move it up or down.

Button: **Ready**

### 7.4 Mechanics

1. Capture a comfortable neutral phone pose when Ready is tapped.
2. Convert tilt relative to that pose into key movement.
3. Use a small dead zone to ignore tiny hand movements.
4. Smooth movement and cap speed.
5. Apply collisions against bars and walls.
6. Stop or gently bounce the key on collision.
7. Complete when the key enters the exit region.

Use frame-time-based movement so display refresh rate does not change difficulty. Screen orientation must be handled correctly or the puzzle must use a clearly supported orientation. Provide Pause, Restart puzzle and Recalibrate controls.

### 7.5 Initial tuning proposals

- Neutral dead zone: approximately 3 degrees.
- Maximum comfortable control tilt: approximately 20 degrees.
- Optional exit confirmation hold: approximately 0.5 seconds.
- No countdown timer in normal play.

Tune these values on physical devices. A collision does not reset the entire quest.

### 7.6 Hardware role

Accelerometer/gravity data supplies tilt. Gyroscope or a suitable fused orientation source may improve motion response when used. Diagnostics must name the actual active input; accelerometer-only tilt must not be presented as proof of gyroscope use.

### 7.7 Alternative controls

> Drag the key around the bars to the opening.

Touch input moves the same key through the same maze with the same exit and collision rules. This preserves the story and challenge. Offer it for accessibility as well as missing hardware.

### 7.8 Success and reveal

Play a short key-release animation and optional vibration. Save the chapter award and inventory together before revealing the next clue.

> **You found the key!**
>
> A note is tied to it:
>
> “The lamp needs one more part. Look for my repair card at the covered board.”

Inventory: key found.  
Button: **Find the repair card**

## 8. Chapter 2 — The Hidden Repair Card

### 8.1 Walking clue

> The key’s note points to the covered board. Find the repair card there.

### 8.2 Arrival card

> **This is the repair board.**
>
> The card is inside the small covered pocket.
>
> Open the camera to read it.

Button: **Look at the card**

### 8.3 Card contents

The physical card contains “Light the Way — Repair Card,” a lamp symbol matching the app, a high-contrast QR code, empty space around the QR code and a short backup code below it. Add “Scan this card in Campus Quest.”

Generate the QR identity and backup code from the game configuration. Do not use one generic code that completes unrelated games.

### 8.4 Scanner screen

Show an unobstructed live camera preview and a scan frame. Include Back, Light on/Light off and Enter card code controls.

> Point the camera at the repair card.

When the scene is dim:

> It is dark inside the pocket. Turn on your light to read the card.

### 8.5 Actual flashlight control

The in-app button must control the real supported rear torch and reflect its confirmed state. A lit button or screen animation alone does not count as hardware use.

The scanner owns one camera session. With CameraX, use the active camera's torch control; do not open a second competing camera session. Handle permission, camera-in-use, unavailable flash and camera closure states.

Turn the torch off when leaving the scanner, backgrounding the app or ending the camera session. Never blink the flashlight rapidly as a decorative effect.

### 8.6 Light sensor purpose

An available ambient-light sensor can suggest that lighting may be dim before scanning. During scanning, camera-image brightness is more directly related to whether the card is visible.

The front ambient-light sensor does not necessarily see the rear flashlight beam. Do not demand a lux increase when the torch switches on. Successful reading of the correct card is the goal. In bright daylight, flashlight use is optional.

### 8.7 Validation

Accept the card only if chapter 1 is complete, the current location meets chapter 2's policy and the payload matches this game, checkpoint and supported card version. Process one completion even if many camera frames detect the same QR.

Treat QR text as untrusted input. Validate its length and format; do not automatically open arbitrary URLs or execute content. Offline verification must be supported by cached game configuration if offline play is promised.

Wrong card:

> This card belongs to another place. Look for the lamp repair card.

Unreadable card:

> Move a little farther back, keep the card inside the frame, or enter the code below it.

### 8.8 Alternatives

If the camera is unavailable or permission is declined:

> Read the short code printed under the card and enter it here.

Validate the printed code against the same chapter and keep the location requirement. It is a gameplay fallback, not a security-grade proof of presence, because players can share printed codes.

Without flash, allow available-light scanning or code entry. Without ambient-light sensing, use camera brightness or manual light control. Do not claim screen brightness is equivalent to rear torch operation.

### 8.9 Success and reveal

> **You found the missing part!**
>
> The lamp’s glass cover is here.
>
> Take it to the meeting place. Turn the lamp toward the path, then close its cover.

Show a simple repair drawing. Add the virtual glass cover to inventory; the player does not remove anything from the physical board. Save chapter 2 and unlock the final destination.

Button: **Take the cover**

## 9. Chapter 3 — Bring Back the Light

### 9.1 Walking clue

> Bring the key and cover to the meeting place. The lamp is waiting there.

### 9.2 Arrival card

> **You reached the old lamp.**
>
> You have the key and the glass cover.
>
> Let’s put them back.

Button: **Fix the lamp**

### 9.3 Fit the parts

Show the virtual lamp with its keyhole and missing cover. The player taps the saved key to unlock the lamp and taps the saved cover to fit it. These simple actions connect earlier rewards to the ending. They do not need extra sensor tasks.

### 9.4 Aim the lamp

> **The lamp is facing the wrong way.**
>
> Hold your phone level and turn slowly until the arrow lines up with the path.

Show a lamp beam, target direction and visible alignment band. As the phone turns, the beam turns. The creator must configure the path direction; it cannot be inferred from the bearing to a checkpoint the player has already reached.

Use shortest-angle differences so crossing from 359 degrees to 1 degree does not spin the beam around.

When aligned:

> **That’s the right way. Hold still.**

Proposed condition: remain within approximately 15 degrees of the configured direction for 2 continuous seconds. A filling indicator shows the hold. Leaving the band resets only that hold, not earlier chapters.

### 9.5 Alignment alternatives

If reliable absolute heading is unavailable:

> Turn the lamp until its two marks line up.

This uses relative motion and visible marks. If usable motion input is also unavailable, offer a touch dial. Record the selected variant. Do not describe relative rotation as a true north measurement.

### 9.6 Close the cover

After alignment has been saved:

> **Now close the cover.**
>
> Place your hand over the top front of your phone. Hold it there until the latch clicks.

Show where to place the hand. A usable transition from far to near begins lowering the virtual cover. A proposed 1-second continuous near reading completes it. If the hand moves away early, pause or reopen the cover and show:

> Hold your hand there a little longer.

A permanent near reading without a usable baseline must not cause automatic success. Proximity availability must have been checked before the walk and must remain usable now.

### 9.7 Cover alternative

> Slide the glass cover down, then hold the latch closed.

The player drags the cover into position and holds the latch for the same short duration. Show matching cover movement and progress. If proximity fails during this step, preserve completed alignment and explain the alternative.

### 9.8 Completion

Play a lamp-lighting animation, gentle latch sound and optional short vibration. Save chapter 3 and final quest completion before showing the ending.

## 10. Ending and scoring

### 10.1 Ending text

> **The light is back!**
>
> You found the key, followed the repair note and fixed the meeting lamp.
>
> The path is ready for the welcome walk.
>
> **Well done—you finished the walk.**

### 10.2 Results screen

Show the three completed chapter names, actual earned points, actual elapsed time and saved completion status. If cloud sync exists, show its real status. Buttons: View my results and Back to quests.

### 10.3 Proposed reward policy

Award 100 points per completed chapter, 300 total. No penalty for alternative controls, sensor calibration, camera-code entry or GPS recovery. Do not reward unsafe walking speed.

This is a proposed story score policy; prototype variants may differ. Store the final policy in the game configuration and verify it in tests. Never award again for repeated callbacks, retries, rejoining or app restarts.

If ranking by time, disclose whether pauses count. Task alternatives may affect speed; do not claim competitive fairness before evaluating that policy.

## 11. Hardware and alternative controls

| Feature | Story role | Alternative | Evidence |
|---|---|---|---|
| Location/GPS | Reach all three real places | Recovery instructions; no simulated arrival | Fix age, accuracy and arrival decisions |
| Accelerometer/gravity | Move the key by tilting | Drag the key | Actual active input and maze outcome |
| Gyroscope | Relative rotation and smoother motion where used | Suitable orientation source or touch dial | Real sensor registration/readings |
| Compass/orientation | Direction arrow and lamp alignment | Map/distance and relative alignment | Heading source and reliability |
| Camera | Read the printed repair card | Printed backup code | Scanner opened and correct card validated |
| Flashlight | Illuminate the shaded card | Available light or backup code | Confirmed torch state |
| Ambient-light sensor | Suggest light assistance | Camera brightness/manual control | Readings actually used for hint |
| Proximity | Hand closes the cover | Drag and hold | Near/far evidence and hold duration |
| Vibration | Arrival, key and latch feedback | Visual and optional sound | Capability and preference |
| Speaker | Repair and completion sound | Visual feedback | User-controlled sound preference |

Microphone input is outside this story. A compulsory clap or phrase would add a task without a clear purpose. Reconsider only if course requirements demand it or the story gains a natural reason for sound input.

Fallbacks are not automatic skips. They preserve the puzzle goal while changing how the player controls it. All selected variants must be honest in diagnostics.

## 12. State, saving and recovery

### 12.1 Overall sequence

```text
Quest details -> Device checks -> Introduction
  -> Walk to chapter 1 -> Arrival -> Key puzzle
  -> Save key/chapter 1 -> Reveal next clue
  -> Walk to chapter 2 -> Arrival -> Scan card or enter code
  -> Save cover/chapter 2 -> Reveal final clue
  -> Walk to chapter 3 -> Arrival -> Fit parts
  -> Aim lamp -> Save alignment -> Close cover
  -> Save chapter 3 and quest completion -> Ending/results
```

### 12.2 State model

Use explicit states: locked, navigating, waiting for location, arrived, task ready, task active, paused, saving, completed and recoverable error. Each task needs its own steps. A generic sensor-fusion percentage cannot represent maze collision, QR validation and lamp alignment.

Proximity progress and puzzle completion must remain separate. A full warmth meter does not award a chapter.

### 12.3 Save contract

Persist the authenticated UID, game ID, story version, completed chapter IDs, inventory, selected task variants, score, start/completion timestamps and durable final-lamp steps.

Save the chapter award, points and inventory together in a database transaction. Make awards idempotent and protect against concurrent scanner or sensor callbacks. Do not save all players under a fixed current_user identifier.

### 12.4 Restart and resume

- Restore a valid sign-in session.
- Resume the next unfinished chapter.
- Recheck permissions and current hardware availability.
- Preserve completed chapters and inventory.
- An unfinished maze may restart with a clear message; it must not erase the found items from prior chapters.
- Preserve completed final-lamp alignment if interruption occurs during cover closure.
- Never reopen a scanner with its torch secretly left on.

### 12.5 Leaving a checkpoint

Use the exit margin to reduce GPS flicker. If the player clearly leaves, pause the task and say:

> Come back to the marked area to continue.

Return to navigation without deleting previous chapter completion. Require a usable recent location again before task completion.

### 12.6 Lifecycle and hardware cleanup

Stop unnecessary listeners when the relevant screen stops. Pause the maze while backgrounded. Close the scanner camera and turn off its torch. Coordinate ownership so one screen does not stop location tracking already taken over by another.

The story needs foreground gameplay. Background location tracking is not required merely to demonstrate location services.

### 12.7 Offline play

Once configuration and assets are installed or cached, local story tasks should remain playable offline. QR and backup-code validation need cached configuration. GPS can still produce a fix without internet, although acquisition may take time; map tiles may be unavailable and need an honest fallback.

Pending cloud writes stay queued until acknowledged. Do not delete them without upload. If cloud integration is unfinished, describe saving as local and do not promise cross-device recovery.

## 13. Creator Studio

The quest must be owned by the actual creator account, visible in that account's Creator Studio and visible to players when published.

### 13.1 Game fields

Store a stable game ID, title, descriptions, creator UID/name, status, ordered three checkpoints, story/configuration version, score policy and physical-card installation confirmation.

### 13.2 Common checkpoint fields

Each checkpoint needs an ID, order, name, latitude/longitude, arrival radius, accuracy/exit policy, walking clue, arrival text, reveal text, task type, alternative controls, points and required inventory.

### 13.3 Task-specific authoring

| Task | Settings |
|---|---|
| Key box | Maze geometry, key start/exit, tilt sensitivity, touch variant |
| Repair card | QR identifier/version, backup-code validator, camera/light instructions |
| Meeting lamp | Target bearing or relative marks, angle tolerance, hold duration, proximity/touch cover settings |

### 13.4 Publishing checks

Reject publication with clear errors when checkpoints are missing, orders/IDs are duplicated, coordinates are invalid, task alternatives are incomplete, card validation is undefined or final lamp alignment has no target.

The author confirms the physical card is installed and readable before publishing. Saving or editing the game must not cascade-delete its checkpoints.

## 14. Suggested data contracts

The following configuration is illustrative. It is not a database migration or a record to publish without the missing route settings.

```json
{
  "gameId": "LIGHT_THE_WAY",
  "storyVersion": 1,
  "title": "Light the Way",
  "checkpointOrder": ["KEY_BOX", "REPAIR_CARD", "MEETING_LAMP"],
  "pointsPerChapter": 100,
  "tasks": {
    "KEY_BOX": {"type": "TILT_MAZE", "fallback": "DRAG_MAZE"},
    "REPAIR_CARD": {
      "type": "QR_SCAN",
      "fallback": "PRINTED_CODE",
      "torch": "PLAYER_CONTROLLED"
    },
    "MEETING_LAMP": {
      "type": "ALIGN_AND_CLOSE",
      "alignmentFallback": "RELATIVE_OR_TOUCH_DIAL",
      "coverFallback": "DRAG_AND_HOLD"
    }
  }
}
```

Coordinates, creator identity, QR values and target bearing are intentionally not invented here.

### 14.1 Example QR structure

```text
campusquest:v1:LIGHT_THE_WAY:REPAIR_CARD:<card-id>
```

Agree the final format with the scanner/backend owners. Validate version, field count, allowed values and length. The card is a clue, not an authentication token or secret credential.

### 14.2 Progress record

```text
userId, gameId, storyVersion
startedAt, completedAt
completedChapterIds
inventory: keyFound, coverFound
taskVariantsSelected
finalLamp: aligned, coverClosed
score
pendingSyncState
```

Keep diagnostics separate from player progress. Avoid retaining camera images and continuous sensor traces unless a specific authorised debug session needs them.

## 15. Visuals, accessibility and feedback

Use the local UI direction: dark forest green, warm parchment, bronze buttons, gold guidance and readable cream text on dark surfaces.

| Screen | Main visual |
|---|---|
| Quest list/details | Parchment cards, story summary and three chapters |
| Navigation | Map, gold arrow, distance and proximity meter |
| Key task | Wooden tray, clear key and exit |
| Card task | Unobstructed camera preview and obvious light button |
| Lamp task | Large beam, alignment band and moving cover |
| Reveals/results | Earned item, short story text and next action |

Keep long story text away from the compass and camera. Support scrolling and large text. The next action must remain reachable on small screens.

### 15.1 Accessibility

- Offer touch alternatives even when movement hardware exists.
- Use words and shapes alongside color.
- Make controls at least 48 dp in their touch dimensions.
- Allow sound and vibration to be disabled.
- Provide a useful compass description without announcing every sensor sample.
- Allow clue rereading and printed-code entry.
- Avoid flashing effects and rapid torch blinking.
- Keep decorative animation modest and respect reduced/disabled system animation settings where applicable.

## 16. Assessment evidence

A separate developer diagnostics screen should demonstrate actual sensor use without placing engineering terminology in the story.

### 16.1 Diagnostics content

- Available hardware, useful readings and chosen alternatives.
- Actual sensor types active for the task.
- Location permission/services, fix age, accuracy and arrival decisions.
- Tilt/rotation values and puzzle outcome.
- Camera session and confirmed torch state.
- QR or backup-code validation result.
- Proximity baseline, near hold and fallback choice.
- Saved chapter IDs, score and local/cloud save status.

Do not expose tokens or credentials. Do not retain unnecessary camera content.

### 16.2 Honest assessment claims

Only claim hardware was demonstrated when that device actually used it. A working fallback proves compatibility handling, not the presence of a missing sensor. Present the story first, then use diagnostics to explain the technical implementation.

## 17. Team responsibilities and delivery order

| Member | Work |
|---|---|
| M1: Player UI/navigation | Introduction, inventory, results, permission explanations and accessible navigation |
| M2: Creator/game management | Task authoring, checkpoint settings, card configuration and publication validation |
| M3: Location/sensors | Capability checks, GPS policy, tilt/orientation, proximity, sensor reliability and diagnostics |
| M4: Quest/gameplay | Key puzzle, scanner/lamp presentation, task states and story reveals |
| M5: Firebase/backend | Shared game data, ownership, rules, cloud progress/results and validation where used |
| M6: Room/repository/sync | Schema, atomic chapter/inventory saving, resume, idempotency and queued sync |

Agree camera/flashlight ownership between M3 and M4. Use one active camera owner rather than competing hardware controllers.

### 17.1 Delivery sequence

1. Agree task types, state contracts and persistence requirements.
2. Select the route, third coordinate, lamp direction and card site.
3. Implement device checks and alternative controls.
4. Implement/test the key puzzle.
5. Implement/test camera, card validation and actual torch control.
6. Implement/test lamp alignment and cover closure.
7. Connect chapter ordering, inventory and transactional awards.
8. Add Creator Studio settings and publish checks.
9. Integrate cloud behavior when access and backend functions are ready.
10. Verify lifecycle recovery, hardware variants and the physical route.

## 18. Acceptance tests

### 18.1 Story/navigation

- [ ] Introduction uses simple, clear language.
- [ ] All targets come from the authored game.
- [ ] Missing, stale or inaccurate GPS cannot unlock a task.
- [ ] Only the next chapter unlocks, including overlapping circles.
- [ ] Arrow/distance/warmth react to actual readings.
- [ ] Compass failure gives a clear alternative.
- [ ] Arrival boundary jitter does not repeatedly reset gameplay.

### 18.2 Key puzzle

- [ ] Tilt direction matches visible key movement.
- [ ] Screen rotation and neutral calibration work.
- [ ] Collision prevents the key crossing walls.
- [ ] Pause/recalibration and touch controls work.
- [ ] Refresh rate does not determine puzzle speed.
- [ ] Key, chapter and points save together, once.

### 18.3 Camera and light

- [ ] Camera permission explanation and denial alternative work.
- [ ] Correct installed card validates at its checkpoint.
- [ ] Wrong/malformed/unsupported cards do not complete the chapter.
- [ ] Light button controls the real torch and confirms its state.
- [ ] Torch switches off on exit/background/camera shutdown.
- [ ] Daylight scanning does not require pointless flashlight use.
- [ ] Missing camera, flash or light sensor has the intended alternative.
- [ ] Printed code validates the correct chapter and requires location.
- [ ] Duplicate QR callbacks cannot award twice.

### 18.4 Final lamp

- [ ] Required inventory is checked.
- [ ] Real-direction alignment uses the configured path bearing.
- [ ] Relative/touch alignment is correctly selected and identified.
- [ ] Crossing north avoids full-circle jumps.
- [ ] Leaving the band resets only the alignment hold.
- [ ] Proximity requires usable evidence and continuous hold.
- [ ] Missing/unreliable proximity does not pass automatically.
- [ ] Completed alignment survives a cover-step interruption.
- [ ] Ending follows successful local persistence.

### 18.5 Data/lifecycle

- [ ] Restart restores sign-in and next chapter.
- [ ] Users have separate progress.
- [ ] Rejoin preserves progress; retries do not add points.
- [ ] Sensors, camera and torch clean up when inactive.
- [ ] Offline progress remains saved and cloud writes are retained until acknowledged.
- [ ] Creator Studio and player list show the correct game.
- [ ] Small screens and large text remain usable.

## 19. Demonstration procedure

1. Present quest details and simple story premise.
2. Run device checks and explain selected alternatives.
3. Begin walking and show real navigation updates.
4. Reach chapter 1 and solve the tilt-box puzzle.
5. Show the saved key and repair-card clue.
6. Walk to the installed physical card.
7. Scan it with the in-app camera; use the real light if dim.
8. Show the virtual cover and final destination.
9. Reach chapter 3, fit parts, align the lamp and close its cover.
10. Show saved completion and actual results.
11. Reopen the app to demonstrate persistence.
12. Open diagnostics to show actual hardware inputs and fallback choices.

Prepare a deliberate development reset for repeat demonstrations. Do not wipe all users' progress whenever the normal app launches.

## 20. Current prototype and outstanding decisions

### 20.1 What exists locally

The local UI-polish branch includes the forest/parchment styling, real direction arrow, GPS distance, cold-to-hot meter, session restoration and locally saved progress. Its Test game still uses the earlier two-chapter sweep/shake discovery flow. That is not this story.

### 20.2 What must be built

The tilt maze, card scanner, scanner flashlight control, complete preflight variants, lamp alignment/cover task, inventory, three-task authoring and diagnostics need implementation/integration. Do not replace story text while leaving an unrelated generic sweep/shake threshold as the success condition.

### 20.3 Remaining authoring decisions

- Final three coordinates and approved walking route.
- Whether to reuse the two existing Test positions.
- Final path bearing for the lamp.
- Actual card identity, backup code and installation site.
- Sensor timing and tolerance values after device tests.
- Final score, timer/pause and ranking policy.
- Cloud rollout after Firebase permissions/backend integration are ready.

### 20.4 Technical references

- [Android sensor overview](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview): capabilities differ by device; inspect them at runtime.
- [Android CameraManager](https://developer.android.com/reference/android/hardware/camera2/CameraManager): torch availability and camera-resource constraints.

---

**Agreed outcome:** one three-chapter story, one physical clue-card installation, clear simple English, meaningful phone interactions and explicit alternatives for optional hardware.
