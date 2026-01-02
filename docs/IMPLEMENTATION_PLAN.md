# Android Implementation Plan

Goal: close the highest-risk compliance gaps and move Android toward feature parity
with the React client while improving architecture and test coverage.

Related plans:
- `PLAY_COMPLIANCE_PLAN.md`
- `TEST_PLAN.md`

## Phase 0: Compliance Hardening (P0)

1) Access control layer
   - Create a small compliance gate (ageVerified, allowNsfw, consentReady).
   - Enforce on every entry path (explore, share-code, creator list, character detail,
     chat open, deep links).
2) Server-driven content rating
   - Extend API calls to fetch and cache content rating metadata for characters/sessions.
   - Block or warn before rendering NSFW content when not allowed.
3) Play Billing completion
   - Implement purchase ack + server verification flow.
   - Add client-side purchase status and error surfaces in Shop UI.
   - Backend prerequisite: `/iap/transactions` must accept `platform=android`.
4) Disclosures
   - Add restricted-content warnings and reporting entry points on all UGC surfaces.

## Phase 1: Chat Parity (P0/P1)

1) Session model
   - Expand Room schema for sessions + message history across multiple sessions.
   - Add eviction and storage limits.
2) Chat actions
   - Regenerate/continue/alt swipes, message delete, and retry flows.
   - Align SSE event handling to include all metadata frames.
3) WorldInfo + share
   - Add WorldInfo UI and chat share flow, aligned with server contracts.

## Phase 2: Settings + Personalization (P1)

1) Settings features
   - Theme, language, backgrounds, decorations, model presets.
   - Add Settings subroutes to mirror React (`/settings`, `/theme_select`,
     `/language_select`, `/backgrounds`, `/decorations`, `/model-presets`).
   - Decide storage per setting:
     - Local-only: theme/language/backgrounds (via `UserPreferencesStore`).
     - Server-synced (if required): model presets / interaction settings.
2) UX baseline
   - Consistent typography, spacing, and component styles.
   - Extract strings to resources for i18n.

## Phase 3: Product Parity (P1)

1) Wallet + transactions
2) Fan badges / creator badges
3) Red packet
4) Personas, masks, extensions
5) Comments (basic list/create/like/delete/replies done; align moderation UX and pagination)

## Architecture Track (Parallel)

- Compose Navigation graph with a feature-based module boundary.
- Move to Contract-based MVI per feature.
- Usecases for all network operations; repositories stay data-only.
- Shared UI components for dialogs, lists, and surfaces.

## Testing Track (Parallel)

- Unit tests for compliance gate and share-code resolution.
- UI tests for compliance dialogs and chat lifecycle.
- Integration tests for billing + server verification.
