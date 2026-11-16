# Campus Quest Privacy Policy

**Last updated:** 16 November 2026

> The app shows this policy in-app: on the login screen ("By continuing, you agree to the Privacy Policy") and under Dashboard → Account → Privacy policy. The source text is `app/src/main/res/values/privacy_policy.xml`; keep this file in sync with it.

## About Campus Quest
Campus Quest is a location-based campus exploration game built by a student team for academic coursework. It is not a commercial product, shows no advertising and is not sold. This policy explains what the app collects, why, and the choices you have.

## 1. Your account
- **Email sign-in:** your email address and the display name you choose.
- **Guest sign-in:** a random guest ID only; no email or name is needed.

These are handled by Firebase Authentication so you can sign in and keep your progress. Your display name appears on quest leaderboards.

## 2. Location
- **While you play or create quests**, the app uses your precise location to show you on the map, measure your distance to the next checkpoint, decide when a relic appears and help creators place checkpoints.
- Your location is processed **on your phone**. It is not uploaded and no location history is kept.
- **Background location (optional):** if you choose "Allow all the time", Android checks whether you are near the next checkpoint so the app can alert you while it is closed. This also runs on your phone.
- **Exception:** the coordinates of checkpoints that *you place* in a quest you publish are uploaded, because other players need them to play.

## 3. Phone sensors and camera
- The light, motion, proximity and compass sensors are read only during a quest and never leave your phone.
- The camera is used only in Light the Way to scan the repair card. Images are processed on the phone and are not stored or uploaded.

## 4. Game data stored in the cloud
- Quests you publish: title, description, checkpoints and your display name as creator.
- Your progress in each quest (checkpoints found, score, start and finish time).
- Leaderboard entries: your display name, score and completion time, visible to other players of that quest.
- A notification token, so the app can tell you about new quests.

## 5. Crash reports and app quality
Firebase Crashlytics and Analytics collect technical information such as your phone model, Android version and crash details, so we can fix bugs. Firebase App Check confirms requests come from the genuine app. These do not include your email or your location history.

## 6. Where data is stored and who sees it
Cloud data is stored with Google Firebase (Cloud Firestore, Mumbai region, asia-south1), which processes it on our behalf. We do not sell or share your data with anyone else. Other players can see only your display name, scores and the quests you publish. Security rules (`firestore.rules`) allow each player to read and change only their own private data, and data is encrypted in transit.

## 7. How long we keep it
Your data is kept while your account exists. Guest accounts belong to that guest session on the phone: after a guest signs out, that guest account and its quests can no longer be accessed from the app.

## 8. Your choices
- **Permissions:** you can decline location, background location, camera or notifications. The app explains what will not work without them, and you can change them later in your phone's Settings.
- **Delete a quest:** creators can delete their own quests at any time (Quest details or Creator Studio → Delete quest).
- **Delete your account:** Dashboard → Account → *Delete my account* permanently removes your account, your progress and your leaderboard entries. Delete any quests you published first if you want them removed too.
- **Sign out** at any time from the dashboard.

## 9. Children
Campus Quest is intended for university students and is not directed at children under 13.

## 10. Changes and contact
If the app's use of data changes, this policy will be updated and the date above will change. For questions or data requests, contact the Campus Quest project team.
