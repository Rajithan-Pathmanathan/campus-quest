# Campus Quest — Shared Contracts & Agreements

**Module:** INTE 22283 — Mobile Application Development  
**Project:** Campus Quest  
**Team:** 6 members  
**Working period covered:** 13 September 2026 – 28 September 2026  
**Feature-complete internal deadline:** 23 September 2026  
**Submission:** 1 October 2026  
**Document type:** Shared technical contract and team agreement  
**Status:** Working baseline — changes require team agreement

---

## 1. Purpose of This Document

This document is the **single shared technical contract** for the six Campus Quest developers.

Its purpose is to make parallel development possible without requiring one member to wait for another member's implementation.

The project follows the layered structure:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
 ┌───────────────┐
 ↓               ↓
Room          Firestore
(local)       (cloud)
```

The Repository is the boundary that hides where application data comes from. UI code must not directly decide whether data comes from Room or Firestore.

The project also contains a device/service layer:

```text
Google Maps
Fused Location Provider
Geofencing
        │
        ▼
   Location data
        │
        ▼
   Sensor Fusion
   ┌────┼────┐
   │    │    │
 GPS  Light  Accelerometer
   └────┼────┘
        ▼
   Fusion Score
        ▼
 Proximity Gate
        ▼
      Reveal
        ▼
       Room
        ▼
   Synchronization
        ▼
    Firestore
        ▼
   Leaderboard
```

The shared contracts exist so that each member can build against **interfaces, data models, and mock data** before another member finishes the real implementation.

The master plan specifically requires agreement on the Firestore schema, Room entities, Repository interface, and working process before parallel development. It also establishes mock-first development and a feature-complete checkpoint on 23 September. 

---

# 2. Non-Negotiable Architecture Rules

## 2.1 Layer responsibilities

### UI layer

Responsible for:

- displaying application state;
- accepting user interaction;
- navigation;
- Material UI;
- lists/cards/components;
- displaying errors/loading states;
- displaying the fusion score;
- displaying relic/lore information.

The UI must **not**:

- directly call Firestore;
- directly query Room;
- implement the fusion formula;
- calculate authoritative synchronization rules;
- contain business logic that belongs in ViewModels or repositories.

---

### ViewModel layer

Responsible for:

- holding screen state;
- receiving user actions;
- calling repository interfaces;
- coordinating UI state;
- exposing observable state to the UI.

The ViewModel must not:

- directly access Firestore;
- directly access Room;
- contain Android View references;
- duplicate the sensor implementation.

The module material recommends MVVM because ViewModels hold UI-related state without directly referencing the View and can be tested independently. 

---

### Repository layer

Responsible for:

- exposing the application's data API;
- coordinating local and remote data;
- deciding how data is retrieved;
- recording discoveries;
- synchronization;
- hiding Room/Firestore implementation details.

The Repository is the **only shared data boundary** between ViewModels and storage.

---

### Room layer

Responsible for:

- local structured persistence;
- cached relic data;
- discovered relic records;
- pending synchronization state;
- offline reads;
- local source used when network connectivity is unavailable.

Room uses the Entity → DAO → Database structure taught in the module.

---

### Firestore layer

Responsible for:

- shared cloud relic definitions;
- shared user data;
- leaderboard;
- cloud progress;
- stored relic signatures;
- future stretch team/relay state;
- authentication integration.

Firestore is the shared cloud data source.

---

### Sensor/device layer

Responsible for:

- Fused Location Provider;
- geofencing;
- Maps;
- light sensor;
- accelerometer;
- proximity sensor;
- fusion calculation;
- sensor availability;
- graceful degradation.

This layer must expose clean results to the rest of the application rather than forcing UI code to understand Android sensor APIs.

---

# 3. Six-Member Ownership

| Member | Primary ownership | Main shared contracts |
|---|---|---|
| M1 | App shell, navigation, game browsing UI, game details, join flow, creator wizard (create game, add/configure checkpoints, publish), notification deep-linking, game-specific leaderboard UI | Screen state, navigation, game & creator models, leaderboard UI state |
| M2 | Game checkpoint details, Scan HUD, fusion meter, reveal dialog, scan progress UI | Scan state, fusion HUD state, checkpoint details presentation |
| M3 | Fused Location, Google Maps, permissions, dynamic checkpoint geofencing & distance calculation for active game | Location result, dynamic geofence events, distance result |
| M4 | Accelerometer gesture detection, light signature comparison (`minLux`..`maxLux`), proximity final gate, fusion engine, sensor availability & degradation | Fusion result, sensor availability, motion state, light match |
| M5 | Firebase Auth, Firestore (`games`, `checkpoints`, `gamePlayers`, `progress`, `leaderboards`), FCM push notifications on game publish, cloud security rules | Remote data API, Auth contracts, FCM push payload, Firestore schema |
| M6 | Room database (`GameEntity`, `CheckpointEntity`, `GamePlayerEntity`, `DiscoveryEntity`, `PendingSyncEntity`), repository implementation, offline sync queue, cross-platform POC | Repository interface, Room DAOs/entities, sync coordination |

Ownership does not mean isolation. A member owns an implementation but must use the shared contracts when communicating with another module.

---

# 4. Shared Domain Models

These models are the conceptual data contracts.

The exact Kotlin implementation may be placed in the shared domain/model package.

## 4.1 User

```kotlin
data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val createdAt: Long
)
```

### Fields

| Field | Type | Required | Meaning |
|---|---|---:|---|
| uid | String | Yes | Firebase Authentication user ID |
| displayName | String | Yes | Name displayed in the application |
| email | String | Yes | Authenticated email |
| createdAt | Long | Yes | Creation timestamp |

### Owner

M5 owns the cloud implementation.

M1 consumes it for profile/UI.

---

# 4.2 Game Model

```kotlin
enum class GameStatus {
    DRAFT,
    PUBLISHED,
    CLOSED
}

data class Game(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val status: GameStatus = GameStatus.DRAFT,
    val checkpointCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null
)
```

# 5. Checkpoint / Relic Model

```kotlin
data class Checkpoint(
    val id: String,
    val gameId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float = 20.0f,
    val lightSignature: LightSignature,
    val clue: String,
    val lore: String,
    val order: Int = 1,
    val motionType: String = "SWEEP"
)

// Legacy alias for compatibility with internal references
typealias Relic = Checkpoint
```

## Fields

| Field | Type | Required | Meaning |
|---|---|---:|---|
| id | String | Yes | Unique relic identifier |
| name | String | Yes | Display name |
| lat | Double | Yes | Latitude |
| lng | Double | Yes | Longitude |
| radiusM | Float | Yes | Geofence radius |
| lightSignature | LightSignature | Yes | Expected environmental light range |
| rarity | String | Yes | Rarity label |
| lore | String | Yes | Narrative text |

The master plan defines these relic fields in the Firestore schema.

---

# 6. Light Signature

The initial shared representation is:

```kotlin
data class LightSignature(
    val minLux: Float,
    val maxLux: Float
) {
    // Compatibility accessors
    val min: Float get() = minLux
    val max: Float get() = maxLux
}
```

## Meaning

A relic has an expected ambient-light range.

The live light sensor value is compared against this range.

Example:

```text
Relic:
lightSignature.min = 30
lightSignature.max = 80

Live reading:
55 lux

Result:
strong match
```

The exact scoring function is defined in the separate Sensor Fusion Specification. This document only defines the **data shape**.

### Owner

M4 owns scoring.

M5 owns storage.

M6 owns local representation.

---

# 7. Found Relic

```kotlin
data class FoundRelic(
    val relicId: String,
    val foundAt: Long,
    val synced: Boolean
)
```

Room may represent the same state using:

```kotlin
@Entity
data class FoundRelicEntity(
    @PrimaryKey
    val relicId: String,
    val foundAt: Long,
    val pendingSync: Boolean
)
```

The master plan specifies `relicId`, `foundAt`, and a pending synchronization flag for local progress.

---

# 8. Game-Specific Leaderboard Entry

```kotlin
data class GameLeaderboardEntry(
    val gameId: String,
    val userId: String,
    val displayName: String,
    val checkpointsDiscovered: Int,
    val totalCheckpoints: Int,
    val isCompleted: Boolean,
    val completionDurationSeconds: Long? = null,
    val lastUpdate: Long = System.currentTimeMillis()
)

typealias LeaderboardEntry = GameLeaderboardEntry
```

# 8.1 New Game Push Notification Contract (FCM)

```kotlin
data class NewGameNotification(
    val gameId: String,
    val gameTitle: String,
    val creatorName: String,
    val checkpointCount: Int,
    val publishedAt: Long = System.currentTimeMillis()
)
```

### Owner

M5 owns Firestore storage.

M2 owns leaderboard presentation.

M6 supports local/integration behaviour where necessary.

---

# 9. Location Contract

M3 owns the actual Android location implementation.

Other members must consume a clean result.

## 9.1 LocationResult

Recommended shared representation:

```kotlin
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val timestamp: Long
)
```

### Meaning

- `latitude`: current latitude;
- `longitude`: current longitude;
- `accuracyM`: Android-reported estimated horizontal accuracy;
- `timestamp`: reading time.

GPS accuracy must be treated as an estimate, not exact truth.

The project explicitly identifies GPS/sensor readings as estimates and expects the fusion mechanism to tolerate normal GPS drift.

---

# 10. Distance Contract

M3 calculates distance from the current location to a relic.

The shared output is:

```kotlin
data class DistanceResult(
    val relicId: String,
    val distanceM: Float,
    val accuracyM: Float,
    val timestamp: Long
)
```

M4 consumes `distanceM`.

M2 does not need to know how the distance was calculated.

M5 does not calculate live device distance.

---

# 11. Geofence Contract

M3 owns geofence registration and transition handling.

A geofence event should conceptually expose:

```kotlin
enum class GeofenceTransition {
    ENTER,
    EXIT
}

data class GeofenceEvent(
    val relicId: String,
    val transition: GeofenceTransition,
    val timestamp: Long
)
```

## ENTER

Meaning:

```text
Player entered relic area
        ↓
Start Scan Mode
```

## EXIT

Meaning:

```text
Player left relic area
        ↓
Pause scan
```

The user story acceptance criteria specify that ENTER starts Scan Mode automatically and EXIT before completion pauses the scan rather than losing progress.

---

# 12. Sensor Availability Contract

The application must not assume every Android device contains every sensor.

Recommended representation:

```kotlin
data class SensorAvailability(
    val accelerometerAvailable: Boolean,
    val lightAvailable: Boolean,
    val proximityAvailable: Boolean
)
```

M4 owns detection.

The application must remain usable if a sensor is missing.

The master plan defines graceful sensor degradation as a core feature.

---

# 13. Accelerometer Contract

M4 owns the Android `TYPE_ACCELEROMETER` implementation.

The important application-level result is whether the required scanning motion is currently detected.

Recommended:

```kotlin
data class MotionState(
    val isScanningMotionDetected: Boolean,
    val confidence: Float,
    val timestamp: Long
)
```

The exact gesture-detection algorithm belongs to M4.

The UI must not receive raw accelerometer streams unless there is a specific debugging screen.

The user-facing behaviour is:

```text
Correct slow side-to-side sweep
        ↓
Motion condition active
        ↓
Fusion continues normally
```

If the player stops the scanning motion, the motion contribution can decay/stall according to the fusion specification.

---

# 14. Light Sensor Contract

M4 reads Android `TYPE_LIGHT`.

The raw reading is:

```text
lux
```

The shared application-level output may be:

```kotlin
data class LightMatch(
    val currentLux: Float,
    val score: Float,
    val timestamp: Long
)
```

Where:

```text
score = normalized 0–1 light-signature match
```

The exact scoring algorithm belongs in the Sensor Fusion Specification.

M5 supplies the stored `LightSignature`.

---

# 15. Proximity Contract

M4 owns Android `TYPE_PROXIMITY`.

Proximity is **not a fourth fusion input**.

It is a final gate.

Correct flow:

```text
GPS
  +
Light
  +
Accelerometer
  ↓
Fusion Score
  ↓
Threshold crossed?
  ↓ YES
Enable proximity confirmation
  ↓
Near detected?
  ↓ YES
Reveal
```

Incorrect flow:

```text
GPS + Light + Accelerometer + Proximity
              ↓
        one weighted score
```

The second design must not be implemented.

The project specification explicitly defines proximity as a gate after the fused score crosses its threshold.

---

# 16. Fusion Result Contract

M4 owns the fusion engine.

The rest of the application should receive a result similar to:

```kotlin
data class FusionResult(
    val relicId: String,
    val scorePercent: Int,
    val thresholdPercent: Int,
    val thresholdReached: Boolean,
    val proximityRequired: Boolean,
    val sensorAvailability: SensorAvailability,
    val timestamp: Long
)
```

## Meaning

### `scorePercent`

0–100.

### `thresholdPercent`

The configured reveal threshold.

### `thresholdReached`

```text
score >= threshold
```

### `proximityRequired`

Normally false until threshold is reached, then true.

### `sensorAvailability`

Allows the UI or diagnostics to explain degraded operation when necessary.

---

# 17. Scan State Contract

M2 owns the presentation of Scan Mode.

M4 provides sensor/fusion state.

A shared state model should be used:

```kotlin
sealed interface ScanState {

    data object Idle : ScanState

    data object WaitingForLocation : ScanState

    data object Scanning : ScanState

    data object ThresholdReached : ScanState

    data object WaitingForProximity : ScanState

    data object Revealed : ScanState

    data object PausedOutsideGeofence : ScanState

    data class Error(val message: String) : ScanState
}
```

This exact implementation may be adjusted during coding, but the meanings must remain stable.

---

# 18. Scan State Behaviour

## Idle

No scan is active.

## WaitingForLocation

The scan has started but a usable location reading is not yet available.

## Scanning

The fusion score is actively updating.

## ThresholdReached

The fusion score has crossed the required threshold.

## WaitingForProximity

The player is instructed to bring the phone close.

## Revealed

Proximity has confirmed the reveal.

## PausedOutsideGeofence

The player left the geofence before completing the scan.

## Error

An unrecoverable scan-specific error occurred.

Missing sensors must not automatically produce `Error`. Missing sensors should normally trigger graceful reweighting.

---

# 19. Repository Contract

This is the most important application-level contract.

Every ViewModel that needs quest/progress data uses the Repository.

Initial contract:

```kotlin
interface QuestRepository {
    // --- Player Game Discovery & Details ---
    suspend fun getAvailableGames(): List<Game>
    suspend fun getGameDetails(gameId: String): Game?
    suspend fun joinGame(gameId: String): Result<Unit>
    suspend fun getGameCheckpoints(gameId: String): List<Checkpoint>

    // --- Creator Game Authoring ---
    suspend fun createGame(game: Game): Result<String>
    suspend fun createCheckpoint(checkpoint: Checkpoint): Result<String>
    suspend fun publishGame(gameId: String): Result<Unit>

    // --- Gameplay & Scanning ---
    suspend fun getFusionSignature(checkpointId: String): LightSignature
    suspend fun recordDiscovery(
        gameId: String,
        checkpointId: String,
        fusionScore: Int
    ): Result<Unit>

    // --- Game-Specific Leaderboard ---
    fun observeGameLeaderboard(gameId: String): Flow<List<GameLeaderboardEntry>>

    // --- Offline Synchronization ---
    suspend fun syncPending()
}
```

This contract is taken directly from the project master plan.

Do not independently rename these methods in feature branches.

---

# 20. Repository Responsibility Rules

The Repository may internally perform:

```text
Room read
Firestore read
Room write
Firestore write
Connectivity check
Synchronization
Conflict resolution
```

But the ViewModel should see:

```text
getRelics()
recordReveal()
observeLeaderboard()
syncPending()
```

The UI should see neither Room nor Firestore.

---

# 21. Repository Mock Implementation

Before M5 and M6 finish the real data layer, other members must be able to use a fake Repository.

Example:

```kotlin
class FakeQuestRepository : QuestRepository {

    override suspend fun getRelics(): List<Relic> {
        return MockData.relics
    }

    override suspend fun getFusionSignature(
        relicId: String
    ): LightSignature {
        return MockData.relics
            .first { it.id == relicId }
            .lightSignature
    }

    override suspend fun recordReveal(
        relicId: String
    ) {
        // Store in fake in-memory list
    }

    override fun observeLeaderboard():
        Flow<List<LeaderboardEntry>> {
        return flowOf(MockData.leaderboard)
    }

    override suspend fun syncPending() {
        // No-op in mock
    }
}
```

The actual mock values will be defined in:

`MOCK_DATA_CATALOG.md`

---

# 22. Authentication Contract

M5 owns Firebase Authentication.

M1 must not implement Firebase calls inside Activities/Fragments.

Recommended abstraction:

```kotlin
interface AuthRepository {

    fun currentUser(): User?

    suspend fun signIn(
        email: String,
        password: String
    ): Result<User>

    suspend fun signOut()

    fun observeAuthState(): Flow<User?>
}
```

The exact authentication provider and credential rules are defined by M5's Firebase implementation.

---

# 23. Authentication States

Recommended UI-level states:

```kotlin
sealed interface AuthState {

    data object CheckingSession : AuthState

    data object SignedOut : AuthState

    data class SignedIn(
        val user: User
    ) : AuthState

    data class Error(
        val message: String
    ) : AuthState
}
```

Expected flow:

```text
App starts
 ↓
Check current Firebase session
 ↓
Existing valid session?
 ├── YES → Map
 └── NO  → Login
```

The master plan requires persistent login behaviour for returning users.

---

# 24. Room Contract

M6 owns Room.

The master plan defines two core local entities.

## 24.1 RelicEntity

```kotlin
@Entity(tableName = "relics")
data class RelicEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val lightMin: Float,
    val lightMax: Float
)
```

This is the cached copy of the cloud relic document.

---

## 24.2 FoundRelicEntity

```kotlin
@Entity(tableName = "found_relics")
data class FoundRelicEntity(
    @PrimaryKey
    val relicId: String,
    val foundAt: Long,
    val pendingSync: Boolean
)
```

`pendingSync = true` means:

```text
Local discovery exists
BUT
cloud confirmation has not yet been completed
```

After successful cloud synchronization:

```text
pendingSync = false
```

---

# 25. Room DAO Contract

Recommended DAO:

```kotlin
@Dao
interface RelicDao {

    @Query("SELECT * FROM relics")
    suspend fun getAllRelics(): List<RelicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelics(
        relics: List<RelicEntity>
    )
}
```

Found relic DAO:

```kotlin
@Dao
interface FoundRelicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(
        relic: FoundRelicEntity
    )

    @Query(
        "SELECT * FROM found_relics"
    )
    suspend fun getFoundRelics(): List<FoundRelicEntity>

    @Query(
        "SELECT * FROM found_relics WHERE pendingSync = 1"
    )
    suspend fun getPendingSync(): List<FoundRelicEntity>

    @Query(
        "UPDATE found_relics SET pendingSync = 0 WHERE relicId = :relicId"
    )
    suspend fun markSynced(
        relicId: String
    )
}
```

M6 owns implementation.

Any change to the shared entity fields must be communicated before merging.

---

# 26. Local-First Reveal Rule

When a relic is successfully revealed:

```text
1. Confirm fused threshold.
2. Confirm proximity gate.
3. Create FoundRelic.
4. Write to Room immediately.
5. Mark pendingSync = true.
6. Update local UI.
7. Attempt synchronization when possible.
```

Do not make successful gameplay depend on an immediate network response.

The project specifically requires Room to receive the successful reveal before the network call so the application remains usable in unreliable connectivity.

---

# 27. Synchronization Contract

The intended flow is:

```text
Reveal
 ↓
Room insert
 ↓
pendingSync = true
 ↓
Network available
 ↓
Firestore write
 ↓
Success
 ↓
pendingSync = false
```

If the network is unavailable:

```text
Reveal
 ↓
Room insert
 ↓
pendingSync = true
 ↓
No network
 ↓
Continue playing
```

When connectivity returns:

```text
syncPending()
 ↓
Find pending records
 ↓
Upload
 ↓
Confirm
 ↓
Mark local records synced
```

The module material identifies offline-first synchronization as local save → pending state → network availability → push → pull → conflict resolution → local update.

---

# 28. Conflict Resolution Agreement

For the current project scope, progress conflicts are resolved using the project-defined rule:

1. greater number of relics found wins;
2. if tied, earliest relevant timestamp wins.

Do not introduce a different conflict rule inside one module.

If implementation reveals that the current rule cannot be represented safely, raise the issue to M5 + M6 before changing it.

---

# 29. Firestore Collection Contract

The agreed core collections are:

```text
users
relics
progress
leaderboard
```

Optional stretch:

```text
relayPairs
```

---

# 30. users Collection

Path:

```text
users/{uid}
```

Fields:

```text
displayName
email
createdAt
```

Example:

```json
{
  "displayName": "Nimal",
  "email": "nimal@example.com",
  "createdAt": 1750000000000
}
```

M5 owns.

M1 consumes.

---

# 31. relics Collection

Path:

```text
relics/{relicId}
```

Fields:

```text
name
lat
lng
radiusM
lightSignature
rarity
lore
```

Example:

```json
{
  "name": "The Old Bell",
  "lat": 6.900000,
  "lng": 79.900000,
  "radiusM": 20,
  "lightSignature": {
    "min": 30,
    "max": 80
  },
  "rarity": "RARE",
  "lore": "A fragment of the campus legend..."
}
```

The final mock coordinates and lore must come from the mock-data agreement.

---

# 32. progress Collection

Logical path:

```text
progress/{uid}/found/{relicId}
```

Fields:

```text
relicId
foundAt
synced
```

The cloud copy represents shared account progress.

The local Room copy controls immediate offline behaviour.

---

# 33. leaderboard Collection

Path:

```text
leaderboard/{uid}
```

Fields:

```text
uid
displayName
relicsFound
lastUpdate
```

M5 owns writes and real-time observation.

M2 owns presentation.

---

# 34. relayPairs

This is a **stretch feature**.

Path:

```text
relayPairs/{relicId}
```

Potential fields:

```text
relicId
memberA_state
memberB_state
```

Do not implement this before all core functionality is feature-complete.

If the team is behind schedule, this is one of the first features to remove.

---

# 35. Shared Error Contract

Every layer should represent failures consistently.

Minimum categories:

```kotlin
sealed interface AppError {

    data object NetworkUnavailable : AppError

    data object PermissionDenied : AppError

    data object SensorUnavailable : AppError

    data object LocationUnavailable : AppError

    data object AuthenticationFailed : AppError

    data object RemoteOperationFailed : AppError

    data object LocalStorageFailed : AppError

    data object Unknown : AppError
}
```

The exact implementation may differ, but the meanings must remain consistent.

---

# 36. Loading / Empty / Error / Offline States

Screens must account for:

```text
Loading
Success
Empty
Offline
Permission denied
Sensor unavailable
Failure
```

The module material specifically identifies these states as important for reliable cloud-connected Android applications.

---

# 37. Permission Agreement

Location permission should be requested only when required.

The UI should explain why the permission is needed before requesting it.

Expected flow:

```text
User opens Map
 ↓
Explain location requirement
 ↓
Request permission
 ↓
Granted?
 ├── YES → Map
 └── NO  → Explain limitation + safe fallback
```

A denied permission must not crash the app.

---

# 38. Lifecycle Agreement

Sensor listeners must follow the Android lifecycle.

Expected principle:

```text
onResume()
    ↓
register required listeners

onPause()
    ↓
unregister listeners
```

Do not leave sensor listeners running indefinitely when Scan Mode is not active.

The project master plan explicitly identifies lifecycle handling as a design consideration and recommends registering listeners in `onResume()` and unregistering in `onPause()`.

---

# 39. Battery Agreement

The location design must be event-driven where possible.

The intended pattern is:

```text
Normal exploration
 ↓
Geofencing
 ↓
ENTER
 ↓
Start intensive Scan Mode
 ↓
EXIT / Reveal
 ↓
Stop intensive scan
```

Do not implement continuous high-frequency location polling for the entire application if the geofencing mechanism can provide the required transition.

Battery-conscious behaviour is a core design consideration in the project.

---

# 40. Sensor Fusion Rules

These are shared conceptual rules.

## Rule 1

GPS, light, and accelerometer contribute to **one score**.

## Rule 2

Proximity does not contribute to that score.

## Rule 3

Proximity only confirms a reveal after the score crosses the threshold.

## Rule 4

A missing sensor must not make the puzzle impossible.

## Rule 5

Weights are re-normalized when a sensor is unavailable.

## Rule 6

Sensor readings are treated as noisy estimates.

## Rule 7

Fusion must be testable without physical sensors by using mock inputs.

The project master plan explicitly defines the fusion as GPS distance + light match + accelerometer scanning motion, with graceful reweighting when a sensor is unavailable.

---

# 41. Example Fusion Contract

M4 may expose:

```kotlin
interface SensorFusionEngine {

    fun start(
        relic: Relic
    )

    fun stop()

    fun updateLocation(
        distance: DistanceResult
    )

    fun updateLight(
        light: LightMatch
    )

    fun updateMotion(
        motion: MotionState
    )

    fun updateProximity(
        isNear: Boolean
    )

    fun observeResult():
        Flow<FusionResult>
}
```

This is a proposed implementation contract derived from the project architecture. The exact method signatures must be finalized by M4 with M2/M3 before implementation.

---

# 42. Communication Contracts Between Members

## M1 ↔ M2

Discuss:

- navigation;
- screen ownership;
- Scan Mode UI;
- Quest UI;
- leaderboard UI;
- UI state names;
- Material components;
- shared theme.

Output:

```text
Approved screen list
Approved navigation routes
Approved ScanState usage
```

---

## M1 ↔ M5

Discuss:

- authentication states;
- User model;
- login success/failure;
- profile data.

Output:

```text
AuthRepository contract
User model
AuthState
```

---

## M2 ↔ M4

Discuss:

- FusionResult;
- ScanState;
- score update frequency;
- threshold state;
- proximity prompt;
- missing-sensor presentation.

Output:

```text
FusionResult contract
ScanState contract
```

---

## M3 ↔ M4

This is one of the most important handoffs.

Discuss:

- GPS distance format;
- location update frequency;
- location accuracy;
- geofence ENTER event;
- geofence EXIT event;
- scan start;
- scan pause;
- GPS unavailable behaviour.

Output:

```text
DistanceResult
GeofenceEvent
LocationResult
```

---

## M4 ↔ M5

Discuss:

- LightSignature structure;
- relic IDs;
- threshold configuration;
- rarity/lore relation;
- stored environmental signatures.

Output:

```text
Relic
LightSignature
```

---

## M5 ↔ M6

This is the primary data-layer agreement.

Discuss:

- Firestore schema;
- Room schema;
- mapping;
- synchronization;
- pendingSync;
- conflict resolution;
- retry behaviour.

Output:

```text
Firestore ↔ Room mapping
Sync rules
Repository implementation plan
```

---

## M6 ↔ M1/M2

Discuss:

- Repository interface;
- mock Repository;
- observable leaderboard;
- local progress;
- loading/error/offline states.

Output:

```text
Stable Repository API
```

---

# 43. Mock-First Development Agreement

Until the real dependency is ready:

**use the contract, not the implementation.**

Example:

M2 needs leaderboard data.

M5 has not finished Firestore.

M2 must not wait.

Instead:

```text
M2
 ↓
QuestRepository
 ↓
FakeQuestRepository
 ↓
Mock leaderboard
```

Later:

```text
M2
 ↓
QuestRepository
 ↓
RealQuestRepository
 ↓
Room + Firestore
```

M2's UI code should not need to change.

This is the main mechanism that allows parallel development.

---

# 44. Shared Mock Data Rules

Mock data must be:

- deterministic;
- identifiable;
- reusable;
- documented;
- consistent across all members.

Do not create:

```text
M1:
relic1

M2:
relic_001

M5:
R001
```

for the same relic.

Use one agreed ID:

```text
relic_001
```

The full mock dataset belongs in:

`MOCK_DATA_CATALOG.md`

---

# 45. Shared Relic IDs

Use stable IDs:

```text
relic_001
relic_002
relic_003
relic_004
relic_005
```

Do not use display names as primary identifiers.

Correct:

```text
id = "relic_001"
name = "The Old Bell"
```

Incorrect:

```text
id = "The Old Bell"
```

---

# 46. Shared User IDs for Testing

Use deterministic test IDs:

```text
mock_user_001
mock_user_002
mock_user_003
```

When Firebase Authentication is used for real testing, the Firebase UID becomes authoritative.

Do not hard-code real student passwords into source code or documentation.

---

# 47. What Can Be Changed Without Team Approval?

A member may freely change:

- private implementation details;
- internal helper classes;
- private functions;
- local variable names;
- internal UI styling;
- internal test utilities.

---

# 48. What Requires Team Agreement?

Team agreement is required before changing:

- shared model fields;
- field names;
- Firestore collection names;
- Room entity fields;
- Repository methods;
- scan states;
- fusion result structure;
- location result structure;
- synchronization rules;
- shared package interfaces;
- Firebase security assumptions;
- mock IDs;
- acceptance criteria.

---

# 49. Contract Change Procedure

If a member discovers a problem:

```text
1. Identify the problem.
2. Do not silently change the shared contract.
3. Message affected members.
4. Propose the change.
5. Explain who is affected.
6. Agree on the replacement.
7. Update the shared contract document.
8. Update mock data if necessary.
9. Update affected member workplans.
10. Implement.
11. Review.
12. Merge.
```

---

# 50. Git Agreement

The branch structure is:

```text
main
│
├── feature/m1-ui-navigation
├── feature/m2-quest-scan-ui
├── feature/m3-location-geofence
├── feature/m4-sensor-fusion
├── feature/m5-firebase
└── feature/m6-room-integration
```

Rules:

1. No direct pushes to `main`.
2. Each member works on their feature branch.
3. `main` should remain buildable.
4. Every merge requires at least one teammate review.
5. Shared-contract changes must be clearly mentioned in the PR.
6. A broken build must be treated as a priority.
7. Do not commit generated secrets or Firebase private credentials.

The master plan explicitly requires feature branches, no direct pushes to main, and teammate review before merging.

---

# 51. Definition of Ready

A task is ready to start when:

```text
[ ] Owner identified
[ ] Required contract identified
[ ] Required mock data identified
[ ] Dependency identified
[ ] Expected output defined
[ ] Acceptance criteria defined
[ ] No unresolved contract ambiguity
```

If a task fails these conditions, resolve the missing information before implementation.

---

# 52. Definition of Done

A feature is not "done" merely because the code was written.

Minimum:

```text
[ ] Code compiles
[ ] Feature runs
[ ] Feature works with mock data
[ ] Shared contract is respected
[ ] Normal case tested
[ ] Error case tested
[ ] Permission/sensor failure handled where relevant
[ ] No obvious crash
[ ] Branch pushed
[ ] PR/review completed
[ ] Dependent members informed
```

The master plan defines the basic module-level completion rule as compiling, running against mock data, and not crashing when permissions are denied or sensors are missing.

---

# 53. Integration Freeze Agreement

## September 23 = Feature Freeze

By the end of September 23:

```text
Login
 ↓
Map
 ↓
Relic markers
 ↓
Geofence
 ↓
Scan Mode
 ↓
GPS + Light + Accelerometer
 ↓
Fusion score
 ↓
Threshold
 ↓
Proximity
 ↓
Reveal
 ↓
Room
 ↓
Sync
 ↓
Leaderboard
```

must work end-to-end.

After this point:

**No new core features.**

Only:

- bugs;
- performance;
- reliability;
- UI corrections;
- device compatibility;
- documentation;
- testing.

The project plan explicitly protects September 24–27 for testing and bug fixing.

---

# 54. Stretch Feature Rule

Stretch features are only allowed after the core path works.

Current stretch candidates:

- team relay mechanic;
- crowd-aware quest routing;
- collectible badge/rarity system;
- CameraX legendary relic;
- Flutter/React Native proof-of-concept.

If the core system is behind:

```text
CUT STRETCH
     ↓
PROTECT CORE
     ↓
PROTECT TESTING BUFFER
```

Do not sacrifice the September 24–27 testing buffer to finish stretch features.

---

# 55. Shared Acceptance Flow

The team must eventually demonstrate:

### A. Authentication

```text
Valid user
 → Login
 → Map
```

### B. Location

```text
Permission
 → Current location
 → Relic markers
```

### C. Geofence

```text
Enter relic area
 → Scan Mode starts
```

### D. Fusion

```text
GPS
+
Light
+
Accelerometer
 → Score 0–100
```

### E. Proximity

```text
Score >= threshold
+
Proximity near
 → Reveal
```

### F. Local persistence

```text
Reveal
 → Room
```

### G. Offline

```text
No network
 → Existing local data remains available
 → New find remains saved
```

### H. Synchronization

```text
Network returns
 → Pending record uploads
 → Local pending flag clears
```

### I. Leaderboard

```text
Firestore update
 → Real-time listener
 → Leaderboard updates
```

---

# 56. Shared Timeline for Contract Work

## September 13

All members:

```text
Read this document.
Read the Master Plan.
Confirm tooling.
Confirm repository access.
Confirm branch.
```

Contract owners:

```text
M3 + M4:
Location/sensor boundary

M4 + M5:
LightSignature/relic boundary

M5 + M6:
Firestore/Room/sync boundary

M1 + M2:
Screen/navigation/state boundary
```

---

## September 14

Contracts frozen for normal development.

Mock data available.

Members begin parallel feature implementation.

---

## September 15–19

Development against:

```text
Shared contracts
+
Mock data
```

Do not wait for production Firebase.

Do not wait for final Room implementation.

Do not wait for the final physical sensor calibration.

---

## September 20–21

Begin real integration.

Priority:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room/Firestore
```

and:

```text
Map
 ↓
Geofence
 ↓
Scan
```

---

## September 22

Full sensor integration:

```text
GPS
+
Light
+
Accelerometer
 ↓
Fusion
 ↓
Proximity
```

Begin physical calibration.

The project plan specifically calls for calibration against real campus locations during integration rather than leaving it until the bug-fixing buffer.

---

## September 23

Feature freeze.

End-to-end acceptance test.

---

## September 24–27

No feature expansion.

Test:

- two or more Android devices;
- location permissions;
- denied permissions;
- missing sensors;
- GPS inaccuracies;
- offline mode;
- reconnect;
- app restart;
- lifecycle/background behaviour;
- synchronization;
- duplicate reveal attempts;
- Firebase failure;
- battery-conscious behaviour.

Fix blocking bugs.

---

## September 28

Begin final documentation/demo preparation.

Each member contributes evidence for their owned component.

---

# 57. Evidence That Each Member Should Produce

## M1

- screenshots of navigation;
- screenshots of Material UI;
- MVVM structure;
- screen flow;
- login/profile UI;
- lore UI.

## M2

- Quest list;
- Scan Mode;
- fusion meter;
- threshold state;
- proximity prompt;
- reveal screen;
- leaderboard.

## M3

- Maps screen;
- current location;
- markers;
- permission flow;
- geofence ENTER;
- geofence EXIT.

## M4

- sensor readings;
- fusion calculation;
- score behaviour;
- missing-sensor fallback;
- proximity gate;
- calibration evidence.

## M5

- Firebase Auth;
- Firestore collections;
- security rules;
- cloud data;
- leaderboard;
- synchronization evidence.

## M6

- Room entities;
- DAO;
- local persistence;
- offline behaviour;
- pending synchronization;
- Repository;
- integration test matrix.

---

# 58. Final Contract Checklist

Before the team starts serious parallel implementation, confirm:

```text
[ ] User model agreed
[ ] Relic model agreed
[ ] LightSignature agreed
[ ] FoundRelic agreed
[ ] LeaderboardEntry agreed

[ ] LocationResult agreed
[ ] DistanceResult agreed
[ ] GeofenceEvent agreed

[ ] SensorAvailability agreed
[ ] MotionState agreed
[ ] LightMatch agreed
[ ] FusionResult agreed
[ ] ScanState agreed

[ ] Repository interface agreed
[ ] Auth contract agreed
[ ] Room entities agreed
[ ] Firestore schema agreed

[ ] Synchronization rule agreed
[ ] Conflict rule agreed
[ ] Permission behaviour agreed
[ ] Lifecycle behaviour agreed
[ ] Battery behaviour agreed

[ ] Mock IDs agreed
[ ] Mock relic data planned
[ ] Git branches agreed
[ ] PR/review process agreed
[ ] Definition of Ready agreed
[ ] Definition of Done agreed
[ ] September 23 freeze understood
```

---

# 59. Contract Ownership Matrix

| Contract | Primary owner | Required reviewers |
|---|---|---|
| User | M5 | M1, M6 |
| Relic | M5 | M2, M4, M6 |
| LightSignature | M4/M5 | M2, M6 |
| FoundRelic | M6/M5 | M2 |
| LeaderboardEntry | M5 | M2, M6 |
| LocationResult | M3 | M4 |
| DistanceResult | M3 | M4 |
| GeofenceEvent | M3 | M2, M4 |
| SensorAvailability | M4 | M2 |
| MotionState | M4 | M2 |
| LightMatch | M4 | M2, M5 |
| FusionResult | M4 | M2, M3 |
| ScanState | M2 | M4, M1 |
| QuestRepository | M6 | M1, M2, M4, M5 |
| AuthRepository | M5 | M1 |
| Room entities | M6 | M5 |
| Firestore schema | M5 | M6 |
| Sync rules | M5/M6 | Entire team |
| Mock data | Shared | Entire team |

---

# 60. Important Separation of Responsibilities

The following distinctions must remain clear:

```text
M3 calculates LOCATION.
M4 calculates FUSION.
M2 displays FUSION.
M5 stores CLOUD DATA.
M6 stores LOCAL DATA and coordinates REPOSITORY/SYNC.
M1 builds NAVIGATION and APP SHELL.
```

Therefore:

### M2 does NOT calculate fusion.

### M3 does NOT implement sensor fusion.

### M4 does NOT directly write Firestore.

### M5 does NOT directly update UI.

### M1 does NOT directly call Firebase.

### M6 does NOT own every integration task.

Every member integrates their own module through the agreed contracts.

---

# 61. Final Principle

The team should follow this rule throughout the project:

> **Depend on the contract, not on another member's unfinished implementation.**

Example:

M2 needs a fusion score.

M4 has not finished the sensor engine.

M2 still develops:

```text
FusionResult
 ↓
ViewModel
 ↓
Scan UI
```

using:

```text
FakeSensorFusionEngine
```

Later M4 supplies:

```text
RealSensorFusionEngine
```

without forcing M2 to redesign the screen.

The same principle applies to:

```text
Firebase
Room
Location
Geofencing
Authentication
Leaderboard
```

This is what allows six developers to work simultaneously while preserving the project's MVVM, separation-of-concerns, Room/cloud, and sensor-fusion architecture.

---

## 62. Related Documents

This document is the parent contract for:

```text
01_M1_UI_UX_NAVIGATION_WORKPLAN.md
02_M2_QUEST_SCAN_UI_WORKPLAN.md
03_M3_LOCATION_GEOFENCING_WORKPLAN.md
04_M4_SENSOR_FUSION_WORKPLAN.md
05_M5_FIREBASE_CLOUD_SYNC_WORKPLAN.md
06_M6_ROOM_DATA_INTEGRATION_WORKPLAN.md

08_MOCK_DATA_CATALOG.md
09_FIREBASE_SCHEMA_ENDPOINTS_AND_SECURITY.md
10_SENSOR_FUSION_AND_LOCATION_SPECIFICATION.md
11_INTEGRATION_AND_HANDOFF_PLAN.md
12_TESTING_ACCEPTANCE_AND_DEVICE_MATRIX.md
13_TEAM_GIT_COMMUNICATION_AND_CHANGE_CONTROL.md
```

If another document contradicts this one, the team must resolve the conflict before implementation continues.

**This document defines the shared interface. The individual member workplans define how each person implements their assigned portion of that interface.**
