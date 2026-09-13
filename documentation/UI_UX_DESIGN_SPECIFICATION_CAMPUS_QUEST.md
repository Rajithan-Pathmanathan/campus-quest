# CAMPUS QUEST — UI/UX DESIGN SPECIFICATION
## Treasure-Hunt Themed Mobile Interface & Screen-by-Screen Visual Direction

**Document:** UI/UX Design Specification  
**Product:** Campus Quest  
**Platform:** Native Android  
**Primary users:** Game Player, Game Creator  
**Design direction:** Cinematic campus treasure hunt / exploration  
**Status:** Updated UI design baseline for implementation and Figma/mockup generation

**Revision:** September 2026 — aligned with the final M1–M6 team ownership and dynamic Game Creator / Game Player architecture.

---

# 1. Purpose

This document translates the approved Campus Quest application flow into a concrete UI/UX design direction.

It is intentionally focused on **what each screen should look like, what information it should contain, what the user can do, which visual assets are required, and where the user goes next**.

The visual identity must remain consistent with the current Campus Quest treasure-hunt concept:

> **Explore • Discover • Belong**

The application should feel like a modern digital treasure-hunting journal rather than a generic campus utility app.

The interface should combine:

- real campus photography;
- dark cinematic backgrounds;
- aged parchment/card surfaces;
- warm gold accents;
- deep forest/emerald tones;
- compass, map, lantern and discovery motifs;
- subtle treasure-map details;
- modern Android controls and touch targets;
- strong visual feedback during the discovery mechanic.

The treasure-hunt theme is a **visual layer**. It must not replace the functional clarity of the Android application.

---

# 2. Product UI Principles

## 2.1 Theme

The visual language should communicate:

- exploration;
- mystery;
- discovery;
- real-world adventure;
- campus identity;
- achievement;
- community-created quests.

Avoid making the interface look like:

- a generic banking application;
- a plain Material template;
- a children's cartoon treasure game;
- a fantasy RPG unrelated to the campus;
- an overly dark interface where text becomes difficult to read.

The correct balance is:

**cinematic + premium + adventurous + readable + practical.**

---

## 2.2 Core visual vocabulary

Use these motifs repeatedly but subtly:

| Motif | Use |
|---|---|
| Compass | Brand identity, navigation, discovery |
| Parchment | Cards, clues, creator forms, reveal content |
| Lantern | Scan/discovery state |
| Map | Game navigation and checkpoint discovery |
| Wax/seal style | Publish, achievement and completion moments |
| Gold linework | Borders, dividers, selected states |
| Pin markers | Checkpoints |
| Trail/dotted path | Game route and progress |
| Key/lock | Discovery and locked checkpoints |
| Chest | Major reveal / reward moment |
| Campus photography | Game covers, checkpoint imagery, atmosphere |

Do not place every motif on every screen. The interface should feel designed rather than decorated.

---

# 3. Visual Design System

## 3.1 Primary palette

Recommended baseline:

- **Obsidian / Charcoal:** `#171512`
- **Deep Brown:** `#2A2119`
- **Warm Brown:** `#4A3422`
- **Parchment:** `#F1E2C3`
- **Light Parchment:** `#FFF4DA`
- **Antique Gold:** `#D7A84B`
- **Bright Gold:** `#F0C866`
- **Forest Green:** `#164C3B`
- **Emerald:** `#1F8A68`
- **Discovery Green:** `#36B37E`
- **Alert Red:** `#B84A42`
- **Muted Text:** `#B9AD96`
- **Primary Text:** `#FFF7E8`

These values are a design starting point. Android theme resources should become the implementation source of truth.

---

## 3.2 Typography

Recommended hierarchy:

### Display
Use a distinctive serif or display serif for:

- CAMPUS QUEST;
- major adventure titles;
- game titles;
- checkpoint reveal titles.

### Interface
Use a highly readable sans-serif for:

- buttons;
- body copy;
- labels;
- metadata;
- form fields;
- status messages.

### Rule

Do not use the decorative font for long paragraphs.

---

# 4. Global Component Language

## 4.1 Buttons

Primary button:

- parchment/gold or strong blue/green depending on the screen;
- rounded but not excessively pill-shaped;
- high contrast;
- concise action label.

Examples:

- `JOIN GAME`
- `START SCAN`
- `CHECK PROXIMITY`
- `VIEW STORY`
- `CONTINUE`
- `PUBLISH GAME`
- `CREATE GAME`

Secondary button:

- transparent/dark surface;
- gold or parchment outline.

Destructive action:

- restrained red;
- confirmation required.

---

## 4.2 Cards

Cards should look like **modern parchment field notes**.

Recommended structure:

```text
┌─────────────────────────────┐
│  IMAGE                      │
│                             │
├─────────────────────────────┤
│ GAME / CHECKPOINT TITLE     │
│ Creator / metadata          │
│ Description                 │
│                             │
│ [ PRIMARY ACTION ]          │
└─────────────────────────────┘
```

Use a subtle worn-paper texture rather than a heavy image texture.

---

## 4.3 Navigation

Player navigation should prioritize:

- Games
- Map
- My Games
- Profile

Creator navigation can expose:

- Games
- Create
- My Games
- Profile

The bottom navigation must remain modern and readable.

---

# 5. Application Flow Overview

## Player

```text
Splash
  ↓
Login / Register
  ↓
Games
  ↓
Game Details
  ↓
Join Game
  ↓
Game Map
  ↓
Checkpoint Details
  ↓
Enter Geofence
  ↓
Scan Ready
  ↓
Scan Active
  ↓
GPS + Ambient Light + Motion
  ↓
Fusion Threshold
  ↓
Proximity Final Gate
  ↓
Reveal
  ↓
Discovery Saved
  ↓
Progress / Map
  ↓
Game-specific Leaderboard
```

This follows the approved application flow: geofence entry only makes the checkpoint eligible for scanning; GPS + ambient light + accelerometer motion form the weighted fusion; proximity is a separate final gate. fileciteturn25file0L79-L117

## Creator

```text
Creator Home
  ↓
Create Game
  ↓
Game Draft
  ↓
Add Checkpoints
  ↓
Checkpoint Details
  ↓
Save
  ↓
Preview Game
  ↓
Validate
  ↓
Publish
  ↓
Published
  ↓
New Game Notification
  ↓
Players Discover Game
```

A creator can add any number of checkpoints; the UI must never imply that production games contain exactly six. fileciteturn25file0L34-L48

---

# 6. Screen Inventory

| ID | Screen | Role |
|---|---|---|
| UI-01 | Splash | Shared |
| UI-02 | Login | Shared |
| UI-03 | Register | Shared |
| UI-04 | Games Home / Discover | Player |
| UI-05 | Game Details | Player |
| UI-06 | Join Confirmation | Player |
| UI-07 | My Games | Player |
| UI-08 | Game Map | Player |
| UI-09 | Checkpoint Details | Player |
| UI-10 | Approach Checkpoint | Player |
| UI-11 | Scan Ready | Player |
| UI-12 | Active Sensor Scan | Player |
| UI-13 | Fusion Progress | Player |
| UI-14 | Proximity Final Gate | Player |
| UI-15 | Checkpoint Reveal | Player |
| UI-16 | Discovery Saved | Player |
| UI-17 | Game Progress | Player |
| UI-18 | Game Leaderboard | Player |
| UI-19 | Notification Deep Link | Shared |
| UI-20 | Creator Home | Creator |
| UI-21 | My Created Games | Creator |
| UI-22 | Create Game | Creator |
| UI-23 | Game Draft Editor | Creator |
| UI-24 | Checkpoint List | Creator |
| UI-25 | Add Checkpoint | Creator |
| UI-26 | Edit Checkpoint | Creator |
| UI-27 | Checkpoint Location Picker | Creator |
| UI-28 | Checkpoint Configuration | Creator |
| UI-29 | Game Preview | Creator |
| UI-30 | Validation Results | Creator |
| UI-31 | Publish Confirmation | Creator |
| UI-32 | Published Game | Creator |
| UI-33 | Game Closed / Unavailable | Shared |
| UI-34 | Notifications | Shared |
| UI-35 | Profile / Settings | Shared |
| UI-36 | Permission / Location Setup | Player |
| UI-37 | Sensor Unavailable | Player |
| UI-38 | Offline / Sync State | Shared |
| UI-39 | Empty Games | Player |
| UI-40 | Error / Retry | Shared |

---

# 7. IMAGE ASSET MASTER CATALOG

The following numbered assets should be prepared for the UI design. The image prompts are intentionally short enough to use in an image-generation workflow.

## Image 01 — Splash Screen Hero

**Purpose:** Main launch visual.

**Prompt:**

> Cinematic evening view of a historic university campus entrance, warm lantern glow, subtle mist, dramatic tree silhouettes, premium treasure-hunt atmosphere, dark green and brown tones, realistic photography, no people, vertical mobile composition.

**Usage:**

- Splash screen background.
- Optional onboarding hero.

---

## Image 02 — Login Background

**Prompt:**

> Atmospheric historic university courtyard at dusk, warm window lights, old stone architecture, subtle compass-inspired treasure hunt mood, dark cinematic photography, vertical mobile composition, no people.

**Usage:**

- Login/register background.
- Keep image dark enough for parchment login card.

---

## Image 03 — Discover Games — Hidden Histories

**Prompt:**

> Historic university building with dramatic evening light, mysterious exploration atmosphere, realistic campus photography, cinematic treasure hunt aesthetic, vertical card crop.

---

## Image 04 — Discover Games — Green Campus Hunt

**Prompt:**

> Beautiful green university garden with walking path and mature trees, morning light, inviting campus exploration atmosphere, realistic photography, vertical card crop.

---

## Image 05 — Discover Games — Tech Treasures

**Prompt:**

> Modern university technology building with subtle evening lighting, architectural photography, adventurous exploration atmosphere, cinematic realistic style, vertical card crop.

---

## Image 06 — Game Details Cover

**Prompt:**

> Hero photograph of a distinctive university landmark at golden hour, warm light, cinematic shadows, premium adventure poster feeling, realistic photography, landscape composition.

---

## Image 07 — Library Garden Checkpoint

**Prompt:**

> Large old tree beside a university library garden, sunlight passing through leaves, quiet mysterious atmosphere, realistic photography, treasure hunt checkpoint feeling.

---

## Image 08 — Science Block Checkpoint

**Prompt:**

> University science building exterior, subtle dramatic lighting, clean architectural photography, mysterious campus discovery atmosphere.

---

## Image 09 — Cafeteria Checkpoint

**Prompt:**

> University cafeteria exterior and surrounding student area, warm natural light, realistic campus photography, inviting exploration atmosphere.

---

## Image 10 — Main Gate Checkpoint

**Prompt:**

> Grand university main gate at dusk, glowing lamps, dramatic perspective, cinematic realistic photography, adventure destination feeling.

---

## Image 11 — Founder’s Plaza

**Prompt:**

> Historic campus plaza with statue or landmark feature, warm afternoon light, realistic photography, subtle mystery and exploration atmosphere.

---

## Image 12 — Clock Tower

**Prompt:**

> University clock tower at blue hour, glowing windows, dramatic sky, cinematic campus photography, mysterious treasure-hunt destination.

---

## Image 13 — Checkpoint Reveal Image

**Prompt:**

> Ancient campus landmark photographed like a discovered historical treasure, warm sunlight, cinematic depth, realistic photography, rich environmental detail.

---

## Image 14 — Creator Game Cover

**Prompt:**

> Premium campus adventure poster image showing a university landmark surrounded by subtle map lines and compass motifs, cinematic realistic photography, dark green and antique gold mood.

---

## Image 15 — Notification Artwork

**Prompt:**

> Antique lantern and compass on an old treasure map with a university silhouette in the background, cinematic lighting, premium adventure aesthetic, square composition.

---

## Image 16 — Achievement / Discovery Emblem

**Prompt:**

> Elegant antique gold compass emblem combined with a small treasure key, premium game achievement icon, dark background, clean centered composition.

---

## Image 17 — Empty Games Illustration

**Prompt:**

> Closed antique treasure map on a wooden explorer desk beside a compass and lantern, soft cinematic lighting, no text, premium realistic photography.

---

## Image 18 — Offline Illustration

**Prompt:**

> Antique compass and folded campus map with a small disconnected signal motif, warm parchment background, premium treasure hunt illustration, clean centered composition.

---

# 8. PLAYER UI — DETAILED SCREEN SPECIFICATIONS

# UI-01 — SPLASH SCREEN

### Purpose

Establish the Campus Quest identity immediately.

### Layout

```text
FULL-SCREEN CAMPUS IMAGE
        ↓
Dark gradient
        ↓
Compass logo
CAMPUS QUEST
EXPLORE • DISCOVER • BELONG
        ↓
Loading indicator
```

### Visual treatment

- Full-screen campus hero image.
- Dark overlay.
- Large compass emblem.
- Gold title.
- Small subtitle.
- Thin progress line or subtle animated compass rotation.

### Content

**CAMPUS QUEST**

**EXPLORE • DISCOVER • BELONG**

Optional:

`More Than a Campus. A World of Discoveries.`

### Flow

```text
Launch
 ↓
Restore session
 ↓
Authenticated?
 ├── Yes → Games
 └── No → Login
```

### Image

**Image 01**

### Small UI image prompt

> Historic campus entrance at dusk with warm lanterns, cinematic treasure hunt mood, dark green and antique gold palette, vertical mobile background.

### States

- Initial loading.
- Session restoration.
- Bootstrap error → Retry.

---

# UI-02 — LOGIN

### Purpose

Authenticate the user while preserving the adventure theme.

### Layout

Top:

- campus image;
- compass mark.

Middle:

- parchment authentication card.

Bottom:

- Google sign-in;
- email sign-in;
- Register link.

### Content

**Welcome, Explorer**

`Sign in to continue your campus adventure.`

Buttons:

`Continue with Google`

`Continue with Email`

Footer:

`New here? Create an account`

### Visual direction

Use a parchment card floating over a dark campus image.

### Flow

```text
Login
 ├── Success → Games
 ├── Success + notification deep link → Target Game Details
 └── Error → Inline error
```

### Image

**Image 02**

### Prompt

> Historic campus courtyard at dusk, warm lights, dark cinematic treasure hunt atmosphere, realistic photography, vertical mobile background.

---

# UI-03 — REGISTER

### Purpose

Create a player/creator account.

### Fields

- Display name
- Email
- Password
- Confirm password

### UI

Use parchment form surface.

### Primary action

`CREATE ACCOUNT`

### Secondary

`Already have an account? Sign In`

### Validation

- required fields;
- valid email;
- password requirements;
- password confirmation.

### Flow

```text
Register
 ↓
Account created
 ↓
Games
```

---

# UI-04 — GAMES HOME / DISCOVER

### Purpose

Primary player discovery screen.

### Header

Compass icon +:

`Discover Games`

Optional search icon.

### Filters

- All
- Near You
- Trending

### Game card

Each card includes:

- cover image;
- game title;
- creator;
- checkpoint count;
- estimated duration;
- approximate distance;
- status badge such as `NEW`.

### Example

**Hidden Histories**

`By History Club`

`N checkpoints`

`1–2 hours`

### Bottom navigation

- Games
- Map
- My Games
- Profile

### Flow

```text
Select Game
 ↓
Game Details
```

### Important rule

Do not hard-code six checkpoints. The displayed count comes from the game.

### Images

Images 03–05.

---

# UI-05 — GAME DETAILS

### Purpose

Help the player understand the game before joining.

### Hero

Large cover image.

### Information

- Game title
- Creator
- Checkpoint count
- Estimated duration
- Campus/area
- Description
- Short rules
- progress if already joined

### Primary CTA

If not joined:

`JOIN GAME`

If already joined:

`CONTINUE GAME`

### Secondary

`View Map`

### Visual

Hero image with dark overlay and parchment content card.

### Flow

```text
Game Details
 ├── Join → Join Confirmation → Game Map
 ├── Continue → Game Map
 └── Back → Games
```

### Image

Image 06.

---

# UI-06 — JOIN CONFIRMATION

### Purpose

Prevent accidental joining.

### Modal/card

**Ready to begin this adventure?**

Show:

- game title;
- checkpoint count;
- estimated duration;
- location requirement;
- sensor requirement.

CTA:

`JOIN & START`

Secondary:

`CANCEL`

### Flow

```text
Confirm
 ↓
Membership created
 ↓
Game Map
```

---

# UI-07 — MY GAMES

### Purpose

Show games the player has joined.

### Tabs

- In Progress
- Completed

### Card

- game image;
- game title;
- progress percentage;
- discovered/total checkpoints;
- last activity.

### CTA

`CONTINUE`

### Empty state

Use Image 17.

Text:

**Your adventure log is empty**

`Join a game to start discovering the campus.`

CTA:

`DISCOVER GAMES`

---

# UI-08 — GAME MAP

### Purpose

Main geographic gameplay screen.

### Layout

Full-screen campus map.

### Overlay

Top:

- game title;
- progress.

Map:

- current location;
- checkpoint markers;
- discovered markers;
- locked/unavailable markers;
- route/trail where appropriate.

Bottom sheet:

**2 / N checkpoints found**

`Continue exploring`

### Marker language

- undiscovered: treasure-pin;
- discovered: gold/green completed marker;
- current: pulsing location marker;
- unavailable: muted/locked.

### Important

Markers are generated from the selected game's checkpoints.

### Flow

```text
Tap checkpoint
 ↓
Checkpoint Details
```

### Map controls

- current location;
- zoom;
- recenter;
- list view.

---

# UI-09 — CHECKPOINT DETAILS

### Purpose

Give context before the player approaches the physical location.

### Content

- checkpoint image;
- checkpoint name;
- short clue;
- lore teaser;
- distance;
- locked/unlocked status.

### Example

**Library Garden**

`A quiet corner where generations of students have passed beneath the old trees.`

`120 m away`

CTA:

`GET DIRECTIONS`

or, when eligible:

`START SCAN`

### Flow

```text
Far away → Approach Checkpoint
Near enough → Scan Ready
```

### Image

Use the corresponding checkpoint image, such as Image 07.

---

# UI-10 — APPROACH CHECKPOINT

### Purpose

Guide the user physically toward the checkpoint.

### UI

Large distance indicator:

`120 m`

Compass/direction cue.

Circular location radius.

Text:

**Head towards the checkpoint area.**

### Visual

Use a parchment bottom sheet over the map.

### CTA

`GET DIRECTIONS`

### When inside geofence

Transition to:

**You're in the area!**

`You can now start scanning.`

CTA:

`START SCAN`

### Important

Entering the geofence does NOT reveal the checkpoint.

---

# UI-11 — SCAN READY

### Purpose

Prepare the player for the sensor interaction.

### Header

**Scan the Environment**

### Instructions

`Stay within the checkpoint area and perform the required motion.`

Show three requirements:

- Location signal
- Light level
- Motion

### CTA

`START SCAN`

### Visual

Large lantern icon inside a circular scan ring.

### Theme

Dark screen with glowing emerald/gold scan ring.

---

# UI-12 — ACTIVE SENSOR SCAN

### Purpose

Display live scanning without exposing technical complexity unnecessarily.

### Center

Large circular fusion meter.

Example:

`42%`

### Status indicators

```text
✓ Location signal
✓ Light level matching
○ Motion detected
```

### Instruction changes dynamically

Examples:

- `Move slowly through the area...`
- `Keep moving...`
- `Adjust your position...`
- `Hold steady...`

### Motion visualization

Use a subtle directional sweep animation.

### Visual

This is one of the most important screens in the entire app.

It should feel like the player is **unlocking something hidden in the environment**.

### No QR scanner

Do not use a QR-code scanning interface.

### Sensor model

GPS + ambient light + accelerometer motion.

---

# UI-13 — FUSION PROGRESS

### Purpose

Show that multiple environmental signals are converging.

### Main meter

`72%`

### Signal breakdown

```text
Location       ✓
Ambient Light  ✓
Motion         ✓
```

### Status

**Scan Successful**

`Move closer to the exact location.`

CTA:

`CHECK PROXIMITY`

### Important

The fusion threshold alone does not reveal the checkpoint.

### Visual

Large emerald check mark inside a compass ring.

---

# UI-14 — PROXIMITY FINAL GATE

### Purpose

Final physical confirmation.

### Center

Large distance display:

`1.8 m`

### Instruction

**Move closer**

`You're very close!`

### Circular proximity visualization

Use nested rings.

The user's position should visually converge toward a central checkpoint icon.

### Success

`REVEALING...`

### Failure / too far

`Move closer to continue.`

### Important

Proximity is a separate final gate, not a fourth weighted fusion signal.

---

# UI-15 — CHECKPOINT REVEAL

### Purpose

Deliver the reward moment.

This is the most dramatic UI screen.

### Visual

- full-width checkpoint photograph;
- parchment story card;
- treasure chest/key/compass motif;
- subtle particle effect;
- gold glow.

### Content

**Checkpoint Found!**

**The Old Oak Tree**

Lore:

`This tree has witnessed decades of student life on this campus...`

### CTA

`VIEW FULL STORY`

### Secondary

`CONTINUE`

### Animation

Recommended:

1. screen dims;
2. compass rotates;
3. treasure emblem appears;
4. reveal image expands;
5. title fades in;
6. gold particles appear;
7. discovery confirmation.

### Image

Image 13.

---

# UI-16 — DISCOVERY SAVED

### Purpose

Confirm local persistence and successful discovery.

### Visual

Large compass/checkmark emblem.

### Content

**Checkpoint Saved!**

`Your progress has been recorded locally and will sync to the cloud automatically.`

### CTA

`CONTINUE`

### Offline variation

If offline:

**Saved Offline**

`Your discovery is safe. It will sync when you're back online.`

---

# UI-17 — GAME PROGRESS

### Purpose

Show progress for the selected game.

### Header

Game title.

### Main progress

`2 / N checkpoints`

`33%`

### Checklist

```text
✓ The Old Oak Tree
✓ Science Block Mural
○ Library Garden
○ Sunset Viewpoint
○ Founder's Plaza
○ Clock Tower
```

### Visual

A vertical treasure trail can replace a normal progress bar.

### Important

Progress belongs to the selected game.

---

# UI-18 — GAME LEADERBOARD

### Purpose

Show competition within one game.

### Header

**Leaderboard**

Game title below.

### Tabs

- This Game
- Friends (optional if supported)

### Ranking

1. Player
2. Player
3. Player
4. Player
5. Player

Show:

- rank;
- avatar;
- name;
- score or discovered checkpoints;
- completion state.

### Visual

Top three can use subtle gold/silver/bronze medal treatment.

### Important

There is no global leaderboard combining unrelated games.

---

# UI-19 — NOTIFICATION DEEP LINK

### Purpose

Bring players directly to a newly published game.

### Notification

**Campus Quest**

`New game available!`

**Discover Our Campus**

`4 checkpoints • Created by ...`

CTA behavior:

`Tap → Game Details`

### Deep-link requirement

The notification must carry the exact `gameId`.

### If logged out

```text
Notification
 ↓
Splash
 ↓
Login
 ↓
Exact Game Details
```

### Visual

Use Image 15 for optional notification artwork.

---

# 9. CREATOR UI — DETAILED SCREEN SPECIFICATIONS

# UI-20 — CREATOR HOME

### Purpose

Creator dashboard.

### Hero

Compass logo.

### Greeting

**Hi, Creator!**

`Create exciting games for the campus community.`

### Primary CTA

`+ CREATE NEW GAME`

### Secondary

`MY GAMES`

### Optional dashboard metrics

- Published games
- Drafts
- Total players
- Discoveries

Do not overload the first version.

---

# UI-21 — MY CREATED GAMES

### Purpose

Manage creator-owned games.

### Sections

- Drafts
- Published
- Closed

### Card

- cover;
- title;
- status;
- checkpoint count;
- published date;
- player count if available.

### Actions

Draft:

`EDIT`

Published:

`VIEW`

Closed:

`VIEW`

### Ownership

Only the creator who owns the game can edit/publish it.

---

# UI-22 — CREATE GAME

### Purpose

Start a new game.

### Fields

**Game Title**

Placeholder:

`e.g. Discover Our Campus`

**Description**

`Tell players about your game...`

**Cover Image**

`+ Add Photo`

### CTA

`NEXT`

### Secondary

`SAVE DRAFT`

### Design

Use a parchment form card with a small compass progress indicator.

### Flow

```text
Create Game
 ↓
Game Draft
 ↓
Add Checkpoints
```

---

# UI-23 — GAME DRAFT EDITOR

### Purpose

Central editor for an unfinished game.

### Header

Game title + `DRAFT` badge.

### Sections

1. Game Information
2. Checkpoints
3. Preview
4. Validation

### Progress indicator

```text
Game Info → Checkpoints → Preview → Publish
```

### Actions

- edit game;
- manage checkpoints;
- preview;
- validate;
- publish when valid.

### Autosave indicator

`Saved just now`

---

# UI-24 — CHECKPOINT LIST

### Purpose

Manage all checkpoints in the game.

### Header

**Add Checkpoints**

### List item

```text
01  Library Garden
    Location configured
    Light configured
    Motion: Sweep
```

### CTA

`+ ADD CHECKPOINT`

### Each checkpoint

- reorder;
- edit;
- duplicate optional;
- delete with confirmation.

### Critical design rule

The list must support **N checkpoints**.

Do not design the screen around six permanent slots.

---

# UI-25 — ADD CHECKPOINT

### Purpose

Start checkpoint creation.

### Fields

**Checkpoint Name**

**Clue**

**Lore**

**Checkpoint Image**

**Location**

**Light Signature**

**Required Motion**

**Order**

**Rarity**

### CTA

`SAVE CHECKPOINT`

### Visual

A parchment form with sections rather than one enormous form.

---

# UI-26 — EDIT CHECKPOINT

### Purpose

Modify an existing checkpoint.

### Header

Checkpoint name.

### Sections

- Basic Information
- Location
- Environmental Signature
- Gameplay
- Preview

### Actions

`SAVE CHANGES`

`DELETE CHECKPOINT`

### Delete

Require confirmation:

**Remove this checkpoint?**

`Players will no longer be able to discover it after the game is updated/republished according to product rules.`

---

# UI-27 — CHECKPOINT LOCATION PICKER

### Purpose

Set the physical location.

### Layout

Full-screen map.

### Controls

- draggable marker;
- current location;
- zoom;
- search if supported.

### Bottom sheet

**Checkpoint Location**

Latitude/longitude may be displayed for creator precision.

**Detection Radius**

Example:

`20 m`

CTA:

`CONFIRM LOCATION`

### Visual

Use treasure-map style map framing without reducing actual map readability.

---

# UI-28 — CHECKPOINT CONFIGURATION

### Purpose

Configure the environmental discovery requirements.

### Light Signature

Use a range:

`Expected ambient light`

Minimum / maximum lux.

Example:

```text
Min: 80 lx
Max: 220 lx
```

### Motion

Dropdown:

`Sweep`

Other supported values can be added later.

### Radius

`20 m`

### Clue/lore

Display previews.

### Important

The light signature describes **ambient environmental light**, not light emitted by a physical relic.

### Visual

Use a lantern icon beside the light configuration.

---

# UI-29 — GAME PREVIEW

### Purpose

Show creators what players will see.

### Preview should reproduce:

- Game Details;
- Game Map;
- Checkpoint Details;
- progress structure.

### Header

`PREVIEW MODE`

### CTA

`PREVIEW ON MAP`

`EDIT`

### Visual

Use realistic player-facing cards.

---

# UI-30 — VALIDATION RESULTS

### Purpose

Prevent publishing incomplete or invalid games.

### Success state

**Ready to publish**

Checklist:

```text
✓ Game title
✓ Description
✓ Cover image
✓ Checkpoints configured
✓ Locations configured
✓ Light signatures configured
✓ Required motion configured
✓ Game data valid
```

### Error state

**Needs attention**

Each error should be actionable:

`Checkpoint 3 — Location missing`

CTA:

`FIX`

### Visual

Gold/green success treatment; restrained red for errors.

---

# UI-31 — PUBLISH CONFIRMATION

### Purpose

Confirm the transition from draft to published game.

### Modal

**Ready to publish?**

`Your game will become available to players.`

Show:

- game title;
- checkpoint count;
- creator name.

CTA:

`PUBLISH GAME`

Secondary:

`BACK TO EDIT`

### Optional notice

`Publishing will also trigger a new-game notification flow.`

---

# UI-32 — PUBLISHED GAME

### Purpose

Celebrate successful publication.

### Visual

This should feel like opening a treasure chest.

### Center

Lantern + compass + gold emblem.

### Text

**Game Published!**

`Your game is now live and available for players.`

### CTA

`VIEW GAME`

Secondary:

`CREATE ANOTHER`

### Animation

- gold particles;
- seal/stamp effect;
- subtle compass movement.

---

# UI-33 — GAME CLOSED / UNAVAILABLE

### Purpose

Handle games that can no longer be joined or played.

### Visual

Closed treasure map / locked chest.

### Content

**Adventure Unavailable**

`This game is no longer available.`

### Actions

`BACK TO GAMES`

If the user previously joined it, show an appropriate history/progress option if product rules allow.

---

# UI-34 — NOTIFICATIONS

### Purpose

Notification center.

### Groups

- New Games
- Game Updates
- Discoveries / achievements

### Notification card

- small game image;
- title;
- creator;
- time;
- unread indicator.

### Tap

Navigate to exact game context.

---

# UI-35 — PROFILE / SETTINGS

### Purpose

Basic account management.

### Content

- avatar;
- display name;
- email;
- joined games;
- created games;
- notification settings;
- location/sensor help;
- logout.

### Visual

Keep this screen simpler than gameplay screens.

---

# 10. PERMISSION AND SENSOR UI

# UI-36 — LOCATION SETUP

### Purpose

Explain why location is required.

### Visual

Compass over campus map.

### Message

**Location powers your adventure**

`Campus Quest uses your location to determine when you are near a checkpoint and to guide you around the campus.`

CTA:

`ENABLE LOCATION`

Secondary:

`NOT NOW`

If permission is denied:

`Location permission is required to discover location-based checkpoints.`

---

# UI-37 — SENSOR UNAVAILABLE

### Purpose

Handle missing/unavailable sensors gracefully.

### Cases

- light sensor unavailable;
- accelerometer unavailable;
- location unavailable;
- poor accuracy.

### UI

Do not expose technical error codes.

Example:

**We can't read the environment yet**

`Move to an area with better sensor availability or check your device settings.`

CTA:

`TRY AGAIN`

Secondary:

`HELP`

---

# 11. OFFLINE AND SYNC UI

# UI-38 — OFFLINE / SYNC STATE

### Persistent lightweight indicator

Use a small banner:

`Offline — progress saved on this device`

When syncing:

`Syncing discoveries...`

When successful:

`All progress synced`

### Discovery behavior

A successful discovery must remain visible even without cloud connectivity.

### Saved screen

**Saved Offline**

`Your discovery is safe. We'll sync it automatically when connection returns.`

### Image

Image 18.

---

# 12. EMPTY AND ERROR STATES

# UI-39 — EMPTY GAMES

### Illustration

Image 17.

### Text

**No games nearby**

`New adventures may appear soon.`

CTA:

`REFRESH`

Secondary:

`VIEW ALL GAMES`

---

# UI-40 — ERROR / RETRY

### General pattern

```text
Icon
Title
Short explanation
[ RETRY ]
[ GO BACK ]
```

Examples:

**Couldn't load games**

`Check your connection and try again.`

**Couldn't save your discovery**

`Your local progress is safe. We'll retry cloud synchronization later.`

Never display raw Firebase exception messages to the user.

---

# 13. CHECKPOINT DISCOVERY EXPERIENCE — COMPLETE VISUAL SEQUENCE

This is the signature Campus Quest experience.

## Stage 1 — Far Away

```text
Game Map
 ↓
Checkpoint marker
 ↓
Checkpoint Details
 ↓
Approach
```

Visual:

- ordinary map;
- distant checkpoint marker;
- distance indicator.

---

## Stage 2 — Enter Geofence

Visual changes from blue/map mode to green discovery mode.

Message:

**You're in the area!**

`You can now start scanning.`

CTA:

`START SCAN`

The geofence event only enables the scan stage. It does not reveal the checkpoint. fileciteturn25file0L79-L83

---

## Stage 3 — Scan Active

Visual:

- darkened environment;
- glowing lantern;
- circular sensor meter;
- three signal states.

```text
LOCATION       ✓
LIGHT          ✓
MOTION         ○
```

---

## Stage 4 — Fusion Threshold

Meter reaches the configured threshold.

Visual:

- ring completes;
- compass emblem activates;
- success check.

Message:

**Scan Successful**

`Move closer to the exact location.`

---

## Stage 5 — Proximity Gate

Visual:

- concentric circles;
- center checkpoint icon;
- live distance.

Example:

`1.8 m`

When threshold is met:

`REVEALING...`

---

## Stage 6 — Reveal

This is the strongest emotional moment.

Visual:

- screen transition;
- chest/key/compass emblem;
- checkpoint photograph;
- lore parchment;
- gold particles.

---

## Stage 7 — Save

Visual:

- green check;
- cloud icon;
- local/offline status.

Then:

`Continue`

→ Game Map / Progress.

---

# 14. PLAYER GAME MAP VISUAL LANGUAGE

## Checkpoint states

### Undiscovered

Use:

- muted gold;
- question/compass motif;
- subtle pulse.

### Nearby

Use:

- emerald highlight;
- larger marker;
- distance card.

### Scanning

Use:

- animated ring;
- lantern motif.

### Discovered

Use:

- gold/green checkmark;
- completed trail.

### Locked / unavailable

Use:

- subdued lock;
- no distracting red.

---

# 15. CREATOR VISUAL LANGUAGE

Creator screens should feel like the player is **authoring an adventure map**.

Use:

- parchment forms;
- compass step indicators;
- map pins;
- small lantern icons for environmental signatures;
- gold section headers;
- dark brown editor background;
- preview cards matching the player experience.

The creator interface must still feel like the same application.

---

# 16. GAME CARD DESIGN

Every game card should follow this structure.

```text
┌──────────────────────────────┐
│                              │
│       COVER IMAGE            │
│                         NEW  │
│                              │
├──────────────────────────────┤
│ Hidden Histories             │
│ By History Club              │
│                              │
│ ◉ N checkpoints   ◷ 1–2 hr  │
│                              │
│ Explore the stories hidden   │
│ around our campus...         │
│                              │
│              [ VIEW GAME ]   │
└──────────────────────────────┘
```

The actual checkpoint count must come from the selected game.

---

# 17. CHECKPOINT CARD DESIGN

```text
┌──────────────────────────────┐
│ CHECKPOINT 03                │
│                              │
│       IMAGE                  │
│                              │
├──────────────────────────────┤
│ Library Garden               │
│                              │
│ "Where old branches hide    │
│  stories from another era." │
│                              │
│ 120 m away                   │
│                              │
│ [ VIEW CLUE ]                │
└──────────────────────────────┘
```

After discovery:

```text
✓ DISCOVERED
```

---

# 18. LEADERBOARD DESIGN

The leaderboard should feel like a **quest ranking board**.

Top three:

- 1st: prominent gold medal;
- 2nd: silver treatment;
- 3rd: bronze treatment.

Others:

simple ranking rows.

Include:

- rank;
- player;
- discoveries;
- score/time if the product's scoring rules define it.

Never combine unrelated games.

---

# 19. GAME PROGRESS DESIGN

Prefer a **treasure trail** rather than a generic progress bar.

Example:

```text
START
  |
  ✓ Old Oak Tree
  |
  ✓ Science Block
  |
  ◉ Library Garden
  |
  ○ Sunset Viewpoint
  |
  ○ Founder's Plaza
  |
  ○ Clock Tower
  |
FINISH
```

For games with a different number of checkpoints, the trail expands or contracts dynamically.

---

# 20. IMAGE USAGE MATRIX

| Image | Main usage |
|---|---|
| Image 01 | Splash |
| Image 02 | Login/Register |
| Image 03 | Hidden Histories game card |
| Image 04 | Green Campus Hunt card |
| Image 05 | Tech Treasures card |
| Image 06 | Game Details |
| Image 07 | Library Garden |
| Image 08 | Science Block |
| Image 09 | Cafeteria |
| Image 10 | Main Gate |
| Image 11 | Founder's Plaza |
| Image 12 | Clock Tower |
| Image 13 | Reveal |
| Image 14 | Creator cover |
| Image 15 | Notifications |
| Image 16 | Achievement |
| Image 17 | Empty Games |
| Image 18 | Offline |

---

# 21. ADDITIONAL ICON / GRAPHIC ASSET SET

These do not require large generated images.

Prepare as vector icons:

1. Campus Quest compass logo
2. Compass outline
3. Lantern
4. Treasure chest
5. Treasure key
6. Map pin
7. Completed checkpoint
8. Locked checkpoint
9. GPS/location
10. Light/lux
11. Accelerometer/motion
12. Proximity/radar
13. Cloud sync
14. Offline cloud
15. Leaderboard trophy
16. Gold medal
17. Silver medal
18. Bronze medal
19. Draft document
20. Published seal
21. Creator pencil
22. Search
23. Filter
24. Directions
25. Back
26. Recenter map
27. Notification bell
28. Profile
29. Games
30. Map
31. My Games
32. Create

---

# 22. IMAGE GENERATION STYLE GUIDE

All generated campus images should look like they belong to the same visual world.

## Required style

- realistic photography;
- cinematic lighting;
- campus-specific architecture;
- warm highlights;
- deep shadows;
- subtle mystery;
- premium adventure mood;
- natural environments;
- no visible modern UI;
- no random fantasy castles;
- no pirates;
- no cartoon treasure islands;
- no unrelated fantasy objects in campus photos.

## Treasure-hunt elements

Treasure-hunt elements should be introduced mainly through:

- UI borders;
- compass;
- parchment;
- lantern;
- map lines;
- gold accents;
- discovery animations.

Do not turn every campus photograph into a fantasy scene.

---

# 23. UI TEXT STYLE

Use concise, adventure-oriented microcopy.

Good:

- `Begin Adventure`
- `Discover Games`
- `You're in the area!`
- `Start Scan`
- `Keep moving...`
- `Move closer`
- `Checkpoint Found!`
- `Adventure Saved`
- `Continue Exploring`
- `Ready to Publish?`
- `Your Game is Live`

Avoid excessive fantasy language that makes instructions ambiguous.

For technical states, prioritize clarity:

Bad:

`The ancient environmental resonance has failed.`

Good:

`Light level is outside the expected range.`

---

# 24. LOADING STATES

Do not use generic Android spinners everywhere.

Use themed but restrained loading indicators.

Examples:

### Games loading

Small compass rotation.

### Map loading

Subtle map pulse.

### Scan loading

Circular sensor ring.

### Reveal loading

Lantern glow.

### Sync loading

Cloud + small movement indicator.

---

# 25. ACCESSIBILITY

The treasure-hunt theme must not reduce accessibility.

Required:

- sufficient contrast;
- readable text;
- scalable font sizes;
- touch targets at least appropriate Android accessible sizes;
- do not rely on color alone;
- discovered/locked states use icons and labels;
- sensor states have text equivalents;
- animations should be subtle and respect reduced-motion preferences where applicable.

Example:

Do not represent:

`Green = successful`

Only.

Instead:

`✓ Light level matching`

---

# 26. RESPONSIVE / DEVICE CONSIDERATIONS

Primary design target:

- modern Android phone;
- portrait orientation.

Consider:

- 360dp width;
- 411dp width;
- larger Android devices.

Avoid placing critical controls only at the extreme bottom edge.

Maps and scanning screens should accommodate:

- system navigation bars;
- gesture navigation;
- display cutouts;
- varying aspect ratios.

---

# 27. Figma PAGE ORGANIZATION

Recommended Figma structure:

```text
00 — Cover / Brand
01 — Design System
02 — Components
03 — Player — Authentication
04 — Player — Game Discovery
05 — Player — Gameplay
06 — Player — Discovery Reveal
07 — Player — Progress
08 — Player — Leaderboard
09 — Creator — Game Creation
10 — Creator — Checkpoints
11 — Creator — Preview & Publish
12 — Notifications
13 — Errors / Empty / Offline
14 — Prototype Flow
15 — Image Assets
16 — Icons / Graphics
```

---

# 28. FIGMA COMPONENTS TO CREATE FIRST

Create reusable components before designing every screen.

## Navigation

- Top app bar
- Bottom navigation
- Back button
- Screen title

## Buttons

- Primary
- Secondary
- Text
- Destructive
- Disabled
- Loading

## Cards

- Game card
- Checkpoint card
- Leaderboard row
- Notification card
- Creator game card

## Status

- New badge
- Draft badge
- Published badge
- Discovered badge
- Offline badge
- Error badge

## Gameplay

- Fusion meter
- Sensor indicator
- Distance indicator
- Proximity radar
- Checkpoint marker
- Progress trail

## Creator

- Step indicator
- Form field
- Map location selector
- Light range selector
- Motion selector
- Validation item

---

# 29. PROTOTYPE FLOW — PLAYER

Prototype this exact sequence first:

```text
UI-01 Splash
 ↓
UI-02 Login
 ↓
UI-04 Games
 ↓
UI-05 Game Details
 ↓
UI-06 Join Confirmation
 ↓
UI-08 Game Map
 ↓
UI-09 Checkpoint Details
 ↓
UI-10 Approach Checkpoint
 ↓
UI-11 Scan Ready
 ↓
UI-12 Active Scan
 ↓
UI-13 Fusion Progress
 ↓
UI-14 Proximity Gate
 ↓
UI-15 Reveal
 ↓
UI-16 Saved
 ↓
UI-17 Progress
 ↓
UI-18 Leaderboard
```

This is the primary demonstration path.

---

# 30. PROTOTYPE FLOW — CREATOR

```text
UI-20 Creator Home
 ↓
UI-22 Create Game
 ↓
UI-23 Game Draft
 ↓
UI-24 Checkpoint List
 ↓
UI-25 Add Checkpoint
 ↓
UI-27 Location
 ↓
UI-28 Configuration
 ↓
UI-24 Checkpoint List
 ↓
UI-29 Preview
 ↓
UI-30 Validation
 ↓
UI-31 Publish
 ↓
UI-32 Published
```

---

# 31. PROTOTYPE FLOW — NEW GAME NOTIFICATION

```text
Creator
 ↓
Publish
 ↓
New game notification
 ↓
Player taps notification
 ↓
Splash
 ↓
Login if necessary
 ↓
Exact Game Details
 ↓
Join Game
```

The deep link must resolve the exact `gameId`; it must not open a generic games screen when the target game is known. fileciteturn25file3L502-L524

---

# 32. OFFLINE PROTOTYPE

Demonstrate:

```text
Player enters checkpoint
 ↓
Scan succeeds
 ↓
Proximity succeeds
 ↓
Reveal
 ↓
Network unavailable
 ↓
Discovery Saved Offline
 ↓
Game Progress updates locally
 ↓
Network returns
 ↓
Syncing
 ↓
Synced
```

The user must not lose confirmed progress because the cloud is temporarily unavailable. fileciteturn25file0L107-L117

---

# 33. STATE DESIGN MATRIX

Every major screen should define these states.

| State | Example |
|---|---|
| Initial | First screen load |
| Loading | Fetching games |
| Success | Games displayed |
| Empty | No games |
| Error | Firebase/network error |
| Offline | Local data available |
| Permission | Location not granted |
| Disabled | Sensor unavailable |
| Processing | Scanning / syncing |
| Completed | Discovery found |

This is important because the final application flow explicitly requires loading, empty, error, offline, permission and sensor states to be defined. fileciteturn25file3L520-L525

---

# 34. DESIGNING THE SCAN HUD

The scan HUD is the feature that should make Campus Quest visually distinctive.

## Recommended composition

```text
┌─────────────────────────────┐
│ ← Library Garden       ?    │
│                             │
│      SCAN THE ENVIRONMENT   │
│                             │
│          ╭───────╮          │
│        ╭─│  72%  │─╮        │
│        │ ╰───────╯ │        │
│        ╰───────────╯        │
│                             │
│ ✓ Location signal           │
│ ✓ Light level matching      │
│ ○ Motion detected           │
│                             │
│       Keep moving...        │
│                             │
│      [ CANCEL SCAN ]        │
└─────────────────────────────┘
```

The ring should animate based on the fusion score.

---

# 35. DESIGNING THE PROXIMITY SCREEN

Recommended composition:

```text
┌─────────────────────────────┐
│ Library Garden              │
│                             │
│        MOVE CLOSER          │
│                             │
│          1.8 m              │
│                             │
│       ╭─────────╮           │
│     ╭─│    ●    │─╮         │
│   ╭─╯ │          │ ╰─╮      │
│   │   ╰──────────╯   │      │
│   ╰──────────────────╯      │
│                             │
│ You're very close!          │
│                             │
│       REVEALING...          │
└─────────────────────────────┘
```

---

# 36. DESIGNING THE REVEAL SCREEN

The reveal should feel substantially different from normal application screens.

## Before reveal

Dark screen.

Compass outline.

Small text:

`SIGNAL CONFIRMED`

## Reveal

Gold compass/key animation.

Then image.

Then parchment card.

```text
CHECKPOINT FOUND!

THE OLD OAK TREE

This tree has witnessed decades
of student life on this campus...

[ VIEW FULL STORY ]

[ CONTINUE ]
```

This is the main "reward" moment of the application.

---

# 37. DESIGNING THE CREATOR CHECKPOINT EDITOR

The editor should make environmental configuration understandable.

Recommended sections:

### 1. Identity

- Name
- Clue
- Lore
- Image

### 2. Location

- Map
- Pin
- Radius

### 3. Environment

- Ambient light range
- Motion type

### 4. Metadata

- Order
- Rarity

### 5. Preview

- Player-facing card.

This prevents the creator from seeing one giant technical form.

---

# 38. CREATOR PUBLISHING EXPERIENCE

The publishing experience should communicate a clear state transition.

```text
DRAFT
  ↓
VALIDATING
  ↓
READY TO PUBLISH
  ↓
PUBLISHED
  ↓
AVAILABLE TO PLAYERS
```

Use visual badges:

- `DRAFT`
- `READY`
- `PUBLISHED`
- `CLOSED`

Do not allow the visual design to imply that a draft is already public.

---

# 39. BRANDING RULES

## Logo

Primary logo:

**Compass + Campus Quest wordmark**

The compass should become the recurring brand symbol.

## Tagline

Preferred:

**Explore • Discover • Belong**

Optional supporting phrase:

**More Than a Campus. A World of Discoveries.**

## Tone

The UI should feel:

- adventurous;
- intelligent;
- mysterious;
- campus-specific;
- premium;
- welcoming.

Not:

- childish;
- overly corporate;
- overly technical;
- horror-themed.

---

# 40. IMPLEMENTATION HANDOFF RULES

The UI should not directly implement:

- Firebase calls;
- Room database operations;
- sensor acquisition;
- geofence registration;
- location business rules.

The screen should consume state supplied by the ViewModel/repository architecture.

For example:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room / Firestore / Sensor / Location
```

The approved flow defines screen contracts around entry points, required arguments, data, primary/secondary actions, destinations, loading, empty, offline, permission and lifecycle behavior. fileciteturn25file1L267-L289

## Final UI ownership map

The UI/UX specification is implemented according to the final team allocation:

| UI responsibility | Owner | Support |
|---|---|---|
| Player app shell, Games, Game Details, Join, navigation, leaderboard presentation, notification deep links | M1 | M4 / M5 |
| Creator Home, game creation/editing, checkpoint configuration, preview, validation and publish UI | M2 | M3 / M5 / M6 |
| Map, location permission states, checkpoint proximity presentation and sensor-status data supplied to UI | M3 | M4 |
| Checkpoint gameplay, Scan HUD, fusion meter, scan states, proximity-gate presentation and reveal UI | M4 | M3 / M1 / M6 |
| Firebase-backed UI data contracts, authentication, publishing/notification backend support | M5 | M1 / M2 / M6 |
| Room/offline/sync behavior and final integration validation | M6 | All members |

UI ownership does not transfer backend, persistence, location, geofence, or sensor implementation into the UI layer.

---

# 41. NAVIGATION DATA RULES

The UI design must preserve:

```text
gameId
checkpointId
```

through the relevant gameplay screens.

Examples:

```text
Game Details
    gameId

Game Map
    gameId

Checkpoint Details
    gameId
    checkpointId

Scan
    gameId
    checkpointId

Reveal
    gameId
    checkpointId

Leaderboard
    gameId
```

Every checkpoint is scoped to its game. Every leaderboard is scoped to its game. fileciteturn25file0L56-L77

---

# 42. WHAT NOT TO DESIGN

Do not create:

1. A permanent six-checkpoint UI.
2. R001–R006 as fixed production buttons.
3. A global leaderboard.
4. A QR-code scanner.
5. A fake "relic light" mechanic.
6. A scan screen where geofence entry automatically reveals the checkpoint.
7. A fourth weighted "proximity" sensor.
8. A UI that depends on a fixed sample game.
9. Separate visual themes for creator and player.
10. A generic Material template without the Campus Quest visual identity.

The sample identifiers R001–R006 can be used for demo content, but they are not the production navigation model. fileciteturn25file0L56-L62

---

# 43. MVP DESIGN PRIORITY

If design time becomes limited, complete these first.

## Priority 1 — Essential

1. Splash
2. Login
3. Games
4. Game Details
5. Game Map
6. Checkpoint Details
7. Scan Ready
8. Active Scan
9. Fusion
10. Proximity
11. Reveal
12. Saved
13. Progress
14. Leaderboard
15. Creator Home
16. Create Game
17. Checkpoint Editor
18. Preview
19. Publish
20. Notification

## Priority 2 — Required supporting states

21. Location permission
22. Sensor unavailable
23. Offline
24. Sync
25. Empty games
26. Error/retry
27. Closed game

## Priority 3 — Refinement

28. Profile
29. Notification center
30. Advanced creator management
31. Extra animations
32. Optional social/friend features

---

# 44. RECOMMENDED DESIGN ORDER

Do not design all screens randomly.

Follow this sequence.

## Phase 1 — Brand foundation

- Logo
- Colors
- Typography
- Card
- Button
- Icon language
- Map marker style

## Phase 2 — Player discovery

- Splash
- Login
- Games
- Game Details
- Join
- My Games

## Phase 3 — Gameplay

- Map
- Checkpoint
- Approach
- Scan Ready
- Scan
- Fusion
- Proximity
- Reveal
- Saved

## Phase 4 — Player meta

- Progress
- Leaderboard
- Notifications
- Profile

## Phase 5 — Creator

- Creator Home
- Create Game
- Draft
- Checkpoint List
- Add/Edit
- Location
- Configuration
- Preview
- Validation
- Publish
- Published

## Phase 6 — Edge cases

- Permission
- Offline
- Sync
- Empty
- Error
- Closed

---

# 45. FINAL MASTER UI FLOW

```text
                           CAMPUS QUEST
                                |
               +----------------+----------------+
               |                                 |
            PLAYER                            CREATOR
               |                                 |
            Splash                         Creator Home
               |                                 |
          Login/Register                    Create Game
               |                                 |
             Games                           Draft
               |                                 |
         Game Details                    Checkpoint List
               |                                 |
          Join Game                        Add Checkpoint
               |                                 |
           Game Map                         Location
               |                                 |
      Checkpoint Details                  Configuration
               |                                 |
      Approach Checkpoint                    Preview
               |                                 |
         Geofence ENTER                    Validation
               |                                 |
          Scan Ready                         Publish
               |                                 |
         Active Scan                       Published
               |                                 |
     GPS + Light + Motion                       |
               |                                 |
        Fusion Threshold                         |
               |                                 |
      Proximity Final Gate                       |
               |                                 |
            Reveal                              FCM
               |                                 |
        Discovery Saved                 New Game Notification
               |                                 |
          Progress                        Players Discover
               |
          Leaderboard
```

---

# 46. FINAL UI DESIGN CHECKLIST

Before approving a screen, verify:

### Visual

- [ ] Campus Quest treasure-hunt theme is visible.
- [ ] Dark cinematic + parchment + gold language is consistent.
- [ ] Campus photography is realistic.
- [ ] Typography is readable.
- [ ] Buttons are visually consistent.
- [ ] Icons belong to the same family.

### Functional

- [ ] Screen has a clear purpose.
- [ ] Primary action is obvious.
- [ ] Back behavior is defined.
- [ ] Loading state is defined.
- [ ] Empty state is defined where applicable.
- [ ] Error state is defined.
- [ ] Offline state is defined where applicable.
- [ ] Permission state is defined where applicable.

### Product rules

- [ ] No fixed six-checkpoint assumption.
- [ ] `gameId` is preserved.
- [ ] `checkpointId` is preserved where relevant.
- [ ] Leaderboard is game-specific.
- [ ] Geofence entry does not reveal.
- [ ] GPS + light + motion are the fusion signals.
- [ ] Proximity is a separate final gate.
- [ ] Ambient light means environmental light.
- [ ] Offline discoveries remain safe.

### Implementation

- [ ] UI does not directly access Firebase.
- [ ] UI does not directly access Room.
- [ ] UI does not directly own sensor logic.
- [ ] UI does not directly own geofence logic.
- [ ] Screen state can be represented by ViewModel state.
- [ ] Navigation arguments are explicit.

---

# 46A. FINAL TEAM ALIGNMENT

This document is a UI/UX specification, so most visual requirements are ownership-neutral. The implementation split is nevertheless fixed as follows:

- **M1:** player UI and navigation lead.
- **M2:** creator and game-management UI lead.
- **M3:** location, sensors and fusion implementation; supplies physical-signal state to UI.
- **M4:** quest and scan gameplay UI lead.
- **M5:** Firebase/backend implementation; UI consumes repository/ViewModel state.
- **M6:** Room, synchronization and final integration.

The player and creator experiences share the same visual system. They are different functional surfaces, not separate themes.

# 47. FINAL DESIGN DIRECTION

Campus Quest should look like a **premium digital field journal for exploring a real university campus**.

The experience should visually progress from:

**ordinary campus → mysterious map → environmental scan → signal convergence → physical discovery → historical reveal → achievement.**

The creator side should feel like:

**blank map → authored adventure → configured checkpoints → preview → published quest.**

The visual identity must remain consistent across both roles.

The central design idea is:

> **The campus is the treasure map.  
> The environment is the puzzle.  
> The player is the explorer.  
> The creator writes the adventure.**

This UI/UX specification is the visual-design companion to the approved application flow. The application flow remains the source for navigation behavior and state transitions, while this document defines the intended visual treatment, screen composition, image assets, and prototype sequence.
