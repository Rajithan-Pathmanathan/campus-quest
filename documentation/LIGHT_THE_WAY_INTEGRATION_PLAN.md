# Light the Way — Integration plan after PR #15

Date: 15 November 2026.

Local branch: `codex/light-the-way-game`.
Base: main at `0d3b0da`, including the merged M6 PR #15.
Story specification: [complete specification](LIGHT_THE_WAY_COMPLETE_GAME_SPECIFICATION.md).

## Current boundary

This branch is prepared for the story implementation. No gameplay replacement, phone install, remote push or merge is performed by this review/preparation step. The installed phone build and the `member-3-ui-polish` branch remain separate and preserved.

## Preserve from M6

- Room `@Upsert` for game rows.
- Shared app-wide authentication instance.
- Real player IDs and separate progress.
- Rejoining without progress reset and duplicate-discovery guard.
- Creator draft query, game update, checkpoint deletion and player-progress query contracts.
- Structured sync-operation types and queue retention when no uploader is configured.
- Existing M6 unit tests, adding actual Room integration coverage where fake DAOs cannot validate database semantics.

## Reconcile before new tasks

1. Address the review's atomic progress/outbox and checkpoint-deletion completion cases.
2. Keep M6 repository contracts; do not overwrite the repository with our older local version.
3. Port local compass, map labels, real leaderboard/dashboard data and expedition theme selectively.
4. Preserve no-mock-runtime behavior. Main currently seeds The Founder's Lantern from both database startup and GamesViewModel; do not silently bring that back into the single-Test phone setup.
5. Preserve one shared auth instance; keep fallback policy explicit rather than restoring mock login.
6. Keep the existing Test data and login intact until the new three-checkpoint route is configured and an explicit replacement is requested.

## Story delivery slices

1. Define versioned task types, durable chapter state, inventory, capability selection and explicit alternative controls. Provide a Room migration; do not rely on destructive migration to add story state.
2. Implement and verify the tilt-box puzzle plus touch alternative.
3. Implement camera QR recognition, actual scanner-owned torch control and printed-code alternative.
4. Implement real/relative lamp alignment plus proximity or drag-and-hold closure.
5. Connect location-gated chapter order, transactional awards, restart recovery and diagnostics.
6. Add Creator Studio authoring/publish validation and run hardware/lifecycle tests.

Each slice must preserve the working earlier game flow until its replacement is complete. Do not expose a task as supported merely because an enum or screen placeholder exists.

## Inputs still needed for field rollout

- Confirm the complete three-stop route, including the missing third coordinate.
- Confirm whether the first two Test positions will be reused.
- Set the final lamp's path bearing.
- Generate and install the physical QR/backup-code card.

These inputs do not prevent implementing task mechanics and tests. They do prevent publishing a finished real-world route with invented values.

## Cloud boundary

M5 remote integration is still separate. A queue/worker class is not evidence of functioning cross-device sync. Keep saving status honest and preserve all pending uploads until acknowledgement.
