# The Golden Tree

This replaces the playable Light the Way lamp/cover flow and the older ember/Resonator flow. Existing database records, accounts, completed checkpoints and QR tokens are preserved. The old internal LightTheWay name and card payload are compatibility identifiers, not player-facing story language.

## Before starting

The golden tree has lost its light. Find the key, read the keeper's card and bring golden water to the tree. Explain that players walk between three real locations, stop to play virtual puzzles, and scan one real printed card at stop 2. Check tilt hardware before starting. Ask for location and camera only when needed.

## Stop 1: Find the key

Show arrow, distance, stop number and arrival guidance. After stable GPS arrival, open the virtual key box. Hold the phone comfortably, calibrate, then tilt the key around eight barriers to the exit. Touch never moves the key. Award once and show the note directing the player to stop 2.

## Stop 2: Find the instruction card

Scan the checkpoint-specific printed QR card. The in-app flashlight controls the real camera torch. Ambient light only suggests using the flashlight; it never blocks completion. The printed code remains a fallback for camera problems. The key unlocks the keeper's supplies in the story; collect the virtual jug. Explain the next destination before navigation resumes.

## Stop 3: Bring the golden water

Stand still somewhere safe. The journey is virtual: the jug sways while the player tilts to keep its contents away from the rim. Balance for 60 active seconds. Sway increases gradually. Small spills reduce the visible water amount; an empty jug requires an explicit refill/restart. GPS loss, stale tilt readings and backgrounding pause progress. Return to the area and resume/calibrate. No proximity, lamp bearing, cover, latch or slide task is used.

Completing the challenge with water remaining awards the final checkpoint once. The tree shines and the results explain all three completed tasks. The UI phase should illustrate pouring water onto the tree; the current story implementation presents the completion text.

## Compatibility and limits

- Existing configured three-stop stories use this flow and retain their QR token and saved checkpoint awards.
- Games without story configuration show an explanation asking the creator to create a Golden Tree adventure; they cannot enter the retired sensor-fusion flow.
- Existing stored game titles/descriptions are not rewritten remotely. Newly created games use The Golden Tree.
- Existing alignment/cover database fields remain for non-destructive schema compatibility; gameplay no longer reads them.
- Leaving/recreating the story screen can restart an unfinished puzzle; awarded checkpoints remain saved. Full process-death puzzle resume is a follow-up.
- No game, account, branch or remote database record is deleted by this update.

## UI phase

Start from the verified story commit on local branch `ui-fix`. Follow the supplied treasure-hunt reference: forest backgrounds, parchment panels, gold controls, illustrated quest art, clear progress and one primary action. Generate artwork only during this phase. Do not introduce mock quests, fake scores, decorative nonworking controls or unrelated sensors to fill the reference screens.
