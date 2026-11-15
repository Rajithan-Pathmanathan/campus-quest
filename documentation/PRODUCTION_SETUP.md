# Campus Quest: production setup (what you need to provide)

The code for every integration is in place. This guide lists the **external** steps (consoles, keys and accounts) that the code needs. Each step says what stops working if it is skipped.

| # | What | Who provides | Without it |
|---|---|---|---|
| 1 | Firebase project config (`app/google-services.json`) | M5 / project owner | App can't sign in or reach the cloud |
| 2 | Sign-in methods enabled | M5 | Email / guest sign-in fail |
| 3 | Firestore database + rules + index | M5 | No shared quests, progress or leaderboard (app works offline only) |
| 4 | Notification server (Vercel) + `NEW_QUEST_NOTIFY_URL` | M5 | No "new quest" push (everything else works) |
| 5 | Privacy policy URL (`PRIVACY_POLICY_URL`) | Team | Dashboard says "not configured"; Google Play rejects the app |
| 6 | Release keystore (`keystore.properties`) | Project owner | Release APK/AAB is unsigned (can't upload to Play) |
| 7 | App Check + Crashlytics in the console | M5 | Crash reports / abuse protection inactive |
| 8 | Google Play declarations | Project owner | Play review rejects background location |

---

## 1. Firebase project config
The repo's `app/google-services.json` points to **`campus-quest-17889`** with package `com.campusquest` (debug and release use the same package). If you use another project, add an Android app with package `com.campusquest` and replace the file.

## 2. Sign-in methods
Firebase Console → **Authentication → Sign-in method** → enable **Email/Password** and **Anonymous**.

## 3. Firestore
1. Firebase Console → **Firestore Database → Create database** → Standard, location **asia-south1** (can't be changed later), **production mode**.
2. **Rules:** copy `firestore.rules` (repo root) into Firestore → Rules → **Publish**. With the Firebase CLI you can run `firebase deploy --only firestore` instead; it uses `firebase.json`.
3. **Index:** Firestore → Indexes → Composite → collection `entries`, `score` Descending, `completionTimeSeconds` Ascending (also in `firestore.indexes.json`). Until the index is built, the leaderboard falls back to this phone's players.

**What the app stores:**

| Path | Contents |
|---|---|
| `/users/{uid}` | profile and FCM token |
| `/games/{id}` | published quests (+ Light the Way card config) |
| `/games/{id}/checkpoints/*` | the quest's checkpoints |
| `/progress/{uid}_{gameId}` | a player's progress |
| `/leaderboards/{gameId}/entries/{uid}` | leaderboard entries |

## 4. New-quest push notifications (optional)
An Android app cannot send to a topic itself, so `server/notify-new-quest` contains a small Vercel function.
1. Firebase Console → Project settings → **Service accounts → Generate new private key** (a JSON file; keep it secret, never commit it).
2. Create a Vercel project with root directory `server/notify-new-quest`. In Settings → Environment Variables, add **`FIREBASE_SERVICE_ACCOUNT`** = the whole JSON. Deploy.
3. In `local.properties` add `NEW_QUEST_NOTIFY_URL=https://<project>.vercel.app/api/notify-new-quest` and rebuild.

The function checks the caller's Firebase ID token, checks that the quest is published and owned by the caller, and sends once per quest. Phones subscribe to the topic `new_games` automatically after sign-in.

## 5. Privacy policy
Host `documentation/PRIVACY_POLICY.md` (for example on GitHub Pages or Google Sites), then add `PRIVACY_POLICY_URL=<url>` to `local.properties`. The dashboard's **Privacy policy** button opens it.

## 6. Release signing
```bash
keytool -genkeypair -v -keystore release.jks -alias campusquest -keyalg RSA -keysize 2048 -validity 10000
```
Create `keystore.properties` at the project root (git-ignored) with `storeFile`, `storePassword`, `keyAlias` and `keyPassword` (see `.env.example`), then run `./gradlew bundleRelease`. Bump `VERSION_CODE` in `local.properties` for every upload. **Back up the keystore**: Play updates are impossible without it.

## 7. App Check and Crashlytics
- **Crashlytics** works as soon as the app runs with `google-services.json`. Reports appear under Firebase → Crashlytics.
- **App Check:** Firebase → App Check → register the Android app with **Play Integrity** (needs the release SHA-256). Debug builds print a debug token in Logcat; add it under *Manage debug tokens*. Turn on **Enforce** for Firestore only after both are registered.
- **Restrict the API key:** Google Cloud Console → Credentials → the Android key → restrict to package `com.campusquest` + your SHA-1 fingerprints.

## 8. Google Play
- **Data safety form:** location (precise, background), email, user ID, crash logs.
- **Background location declaration** with a short video showing: Start quest → the explanation dialog → "Allow all the time" → the "you're close" alert.
- **Account deletion:** in-app at Dashboard → Account → *Delete my account*. Play also asks for a web link; point it to your privacy policy's deletion section.
