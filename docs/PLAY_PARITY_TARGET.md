# Play Parity Target (Android)

This document defines the feature parity target for the Google Play build of
`st-client-android`. It is the single source of truth for Play parity with
`st-client-React` and explicitly excludes features that are non-compliant or
high-risk for Play.

## Parity Principles
- Match React for core chat, creation, and UGC flows where policy allows.
- Default to safe content handling (no NSFW render without consent + age gate).
- Exclude any client-side code execution or chance-based/cash-like mechanics.
- Enforce exclusions in code (feature flags + release build guards).

## Included (Must Align)
- Chat: sessions, streaming, regenerate/continue/swipes, message tools.
- Explore: feed, detail, follow/block, share code.
- Creators: list, characters, assistant, create/edit role (no extensions).
- Social: followers/following/blocked, notifications, profile basics.
- Settings: privacy/terms, age gate, themes/language, personas, backgrounds,
  decorations, model presets.
- Shop/Wallet: Play Billing only, catalog + purchase + restore, balance +
  transactions (read-only acceptable for v1).

## Excluded on Play
- Extensions (dynamic scripts/styles): permanently disabled on Android.
- Red packet: blocked on Play until a deterministic, fixed-credit design passes
  policy review (no randomness, no cash-out, Play Billing-funded only).

## Conditional / Flagged
- Fan badges / creator badges: enabled only via Play Billing; no cash-out or
  transfer.
- Any feature that depends on server-provided content rating metadata must
  hard-block rendering until the server returns rating/tag/visibility fields.

## Backend Dependencies (Blocking for Full Parity)
- Content rating metadata for all UGC lists (notifications, social, creators,
  comments, assistant history) with hard block before render.
- Server-side policy enforcement for tags/visibility where client lacks context.

## Ownership
- Android: client gating, UI affordances, Play Billing enforcement.
- Server: rating taxonomy, content metadata, filtering guarantees.
