# Play Store Feature Scope (Android)

This document defines the Play Store feature scope for `st-client-android`,
with explicit decisions for React-only features and policy-sensitive areas.
It is an implementation guide, not a policy substitute.

## Scope Decisions (Initial)

- Extensions (dynamic scripts/styles)
  - Status: **Not shipped on Play**.
  - Rationale: dynamic code loading is a high-risk area for Play review.
  - Alternative: curated, server-approved templates or fixed presets.
- Red packet
  - Status: **Blocked until compliant design**.
  - Rationale: must avoid chance-based or cash-like behavior.
  - Alternative: deterministic, non-cash bonus with clear disclosure.
- Fan badges / creator badges
  - Status: **Planned**, gated by Play Billing.
  - Rationale: IAP must be Play Billing only; no cash-out or trading.
- Personas / masks
  - Status: **Planned**.
  - Rationale: local profile metadata; no policy blockers.
- Settings parity (themes/backgrounds/decorations)
  - Status: **Planned**.
  - Rationale: UI customization only.

## Parity Target (Play)

- Align with `st-client-React` for core chat and creator flows.
- Exclude any feature that requires client-side code execution.
- Require server-provided content ratings before rendering UGC.

## Follow-up Tasks

- Confirm policy-compliant product design for red packet and badges.
- Add feature flags to hard-disable non-compliant features in release builds.
- Expand content rating coverage for all UGC lists (notifications, social, creator).
