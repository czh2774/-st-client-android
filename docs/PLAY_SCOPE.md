# Play Store Feature Scope (Android)

This document defines the Play Store feature scope for `st-client-android`,
with explicit decisions for React-only features and policy-sensitive areas.
It is an implementation guide, not a policy substitute.

Play parity target list (single source of truth for Play builds):
`PLAY_PARITY_TARGET.md`.

## Scope Decisions (Initial)

- Extensions (dynamic scripts/styles)
  - Status: **Permanently disabled on Android**.
  - Rationale: dynamic code loading is a high-risk area for Play review.
- Red packet
  - Status: **Blocked on Play pending policy review**.
  - Rationale: must avoid chance-based or cash-like behavior.
  - Alternative: deterministic, fixed-amount credit gift with clear disclosure,
    Play Billing-funded only, no cash-out or transfer.
- Fan badges / creator badges
  - Status: **Implemented**, gated by Play Billing.
  - Rationale: IAP must be Play Billing only; no cash-out or trading.
- Personas / masks
  - Status: **Implemented**.
  - Rationale: local profile metadata; no policy blockers.
- Settings parity (themes/backgrounds/decorations)
  - Status: **Planned**.
  - Rationale: UI customization only.

## Parity Target (Play)

- Align with `st-client-React` for core chat and creator flows.
- Exclude any feature that requires client-side code execution.
- Require server-provided content ratings before rendering UGC.

## Follow-up Tasks

- Confirm Play-safe red packet flow (fixed credit gift, no randomness, no cash-out).
- Ensure red packet funding is Play Billing or promo grants only (no web top-up).
- Expand content rating coverage for all UGC lists (notifications, social, creator).
