# Google Play 18+ Compliance Plan (Android)

This doc defines the minimum policy-aligned requirements to ship an 18+ Android
client that remains in parity with the React web client while staying inside
Google Play policy boundaries. It is intentionally practical and implementation
oriented; it is not a substitute for the official Play policy docs.

## Scope
- Android native client (`st-client-android`) as a Play Store app.
- Feature parity with `st-client-React` where adult content exists.
- Server (`st-server-go`) is the source of truth for content rating, user config,
  and enforcement flags.
- Feature scope decisions for Play are tracked in `PLAY_SCOPE.md`.

## Policy-Aligned Product Decisions (Required)
1) Content rating taxonomy (server-driven)
   - Use the current server schema as canonical and extend only when needed.
   - The server must be able to return rating metadata for characters, sessions,
     and UGC entries.
2) Default-safe experience
   - New users default to `allowNsfw = false`.
   - NSFW content never renders unless consent + age verification are complete.
3) Consistent disclosure and reporting
   - All UGC entry points must offer report/block and clear restricted-content
     disclosure where applicable.
4) Deep link safety
   - Any link entry (share code, direct routes) must be gated by the same
     access-control checks as in-app navigation.

## Client Enforcement Requirements
- Single access-control source: `ContentGate` and `ResolveContentAccessUseCase`
  are the only decision points.
- Enforce gating in all entry paths:
  - Explore feed, character detail, chat open, chat share, creators/assistants,
    notifications, social, shop, wallet, profile.
  - Deep links (share codes or other routes).
- Blocked state UX is consistent:
  - Consent required -> show privacy/terms dialog.
  - Age required -> show age verification dialog.
  - NSFW disabled -> show explicit "mature content disabled" dialog or inline
    notice.
- Server-driven enforcement:
  - Client must honor blocked tags and server content rating metadata before any
    content is displayed.

## Server Content Rating Schema (Current)
Source of truth: `st-server-go` HTTP responses.
- Character detail (`GET /characters/:id`)
  - `isNsfw` (boolean): primary adult-content flag.
  - `moderationAgeRating` (string): server moderation age bucket (`all`, `13+`, `16+`, `18+`, `unknown`).
  - `tags` (string[]): used by server-side filtering and client UI.
  - `visibility` (string): `public` or `private`.
- Character list/query (`POST /characters/query`)
  - `isNsfw` (boolean): summary flag for explore lists.
  - `moderationAgeRating` (string): summary age rating bucket.
- User config (`GET /users/config`)
  - `blockedTags` (string[]): user-level content blocks; `nsfw` blocks NSFW.
  - `ageVerified` (boolean) and `birthDate` (string): required for 18+ access.
- Enforcement notes
  - Server hides NSFW characters when `ageVerified` is false.
- Client must still gate before rendering, even if server also filters.

## Backend Work Tracking
- Server-side Play compliance tasks are tracked in:
  - `st-server-go/PLAY_COMPLIANCE_BACKEND.md`

## UGC Safety Requirements
- Reporting is available on:
  - Character detail, chat, explore cards, creator/assistant surfaces, comments,
    and any UGC list or detail view.
- Blocking is available wherever user-to-user or user-to-character interaction
  occurs.
- Restricted content notices appear wherever NSFW content is allowed.

## Billing and Commerce Requirements
- Play Billing client flow must:
  - Submit purchase to server for verification.
  - Acknowledge or consume purchases based on product type.
  - Surface clear error states for verification/ack failures.
- Server must verify Android signatures and accept `platform=android`.

## Release Checklist (Play Readiness)
- `BuildConfig.API_BASE_URL`, `PRIVACY_URL`, `TERMS_URL` set to HTTPS in release.
- Consent + age verification are required before any NSFW content is displayed.
- All deep links are gated by the access-control layer.
- Report/block flows available on all UGC screens.
- Data Safety form aligns with actual data collection and deletion behavior.
- Play parity target list validated and excluded features disabled (see `PLAY_PARITY_TARGET.md`).

## Acceptance Criteria
- No UI or deep link path can display or start NSFW content when blocked.
- All UGC surfaces include report/block and restricted-content disclosure.
- Billing flow is verified and acknowledged end-to-end.
- Settings and privacy/terms are accessible and enforced across all entry paths.
