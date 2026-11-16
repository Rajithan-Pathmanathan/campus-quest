# Campus Quest Privacy Policy

**Last updated:** [DATE]   **Contact:** [TEAM EMAIL]

> Draft for the team to review. Fill in the bracketed parts before publishing, then host this page and set `PRIVACY_POLICY_URL` (see `PRODUCTION_SETUP.md`).

Campus Quest is a location-based campus exploration game made by [TEAM / UNIVERSITY NAME]. This policy explains what the app collects and why.

## What we collect

| Data | Why | Stored |
|---|---|---|
| **Precise location** (while you play) | Guide you to checkpoints, show you on the map, confirm discoveries | On your phone. It is **not** uploaded or kept as a location history. |
| **Location in the background** (only if you choose "Allow all the time") | Alert you when you are near the next checkpoint while the app is closed | Handled by Android's geofencing on your phone; not uploaded |
| **Email address and display name** | Your account and your name on leaderboards | Firebase Authentication and Firestore (Google Cloud) |
| **Game progress and scores** | Saving your progress and leaderboards | Firestore |
| **Notification token** | Sending "new quest" notifications | Firestore |
| **Crash reports and basic usage** | Fixing bugs | Firebase Crashlytics / Analytics |
| **Camera** (Light the Way only) | Scanning the repair card. Images are processed on the phone and **not** stored or uploaded. | Not stored |

Motion, light and proximity sensors are read only on your phone during a quest and are never uploaded.

## Sharing
We do not sell your data. Data is processed by Google Firebase as our service provider. Your display name and score are visible to other players on quest leaderboards.

## Your choices
- You can decline location, camera or notification permissions. The app explains what will not work without them.
- **Delete your account** at any time: Dashboard → Account → *Delete my account*. This deletes your account, progress and leaderboard entries. Quests you published stay available unless you ask us to remove them at [TEAM EMAIL].

## Children
Campus Quest is intended for university students and is not directed at children under 13.

## Changes
We will update this page if the app's data use changes.
