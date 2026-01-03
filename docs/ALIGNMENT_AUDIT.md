# Android vs React Alignment Audit

Scope: st-client-android vs st-client-React feature parity, 18+ compliance readiness,
architecture, and test coverage. Source of truth: local codebase only.

## Executive Summary

- Android is a partial implementation of the React client. Core chat + explore exist,
  but major product areas are still missing (red packet, fan badges,
  creator tooling completeness, personas/masks/extensions, and settings customization gaps).
- Compliance foundations exist (age verification, privacy/terms consent, reporting,
  blocking). NSFW gating now covers core entry points and syncs server blocked-tags,
  with centralized access policy and chat entry enforcement. UI-level gating
  still dominates, and deep-link entry is not covered, keeping 18+ compliance risk high.
- Architecture is still in a Phase 0 shape: Compose Navigation now covers tabs
  and key flows, but deep links and nested graphs are limited; the use-case
  layer is thin, local persistence is limited, and tests are shallow.

## Feature Parity Checklist (React -> Android)

Legend: DONE / PARTIAL / MISSING

### Core Navigation / Shell
- App shell + deep routes: React DONE, Android PARTIAL (NavHost for tabs + detail/chat/creator flows; deep links limited)
- Mobile-friendly nav: React DONE, Android PARTIAL (basic tab row)

### Explore / Characters
- Explore feed, filters, tags: React DONE, Android PARTIAL (homepage sort only)
- NSFW toggle + gating dialog: React DONE, Android PARTIAL (toggle exists, gating limited)
- Character detail: React DONE, Android DONE (basic info)
- Follow/unfollow: React DONE, Android DONE
- Share code generation/resolve: React DONE, Android DONE (minimal UI)

### Chat
- Sessions list: React DONE, Android DONE (basic list)
- Streaming generation: React DONE, Android DONE
- Regenerate/continue/alt swipes: React DONE, Android DONE
- Message delete/undo/long-press tools: React DONE, Android PARTIAL (delete/delete-after per message; long-press copy/delete added)
- Local multi-session persistence: React DONE, Android PARTIAL (multi-session cache with eviction; resume last session added)
- WorldInfo tooling: React DONE, Android PARTIAL (basic CRUD only)
- Chat share page: React DONE, Android DONE

### Creators / Assistants
- Creator list + characters: React DONE, Android DONE (basic)
- Creator assistant list/chat: React DONE, Android DONE (basic)
- Create/role form: React DONE, Android PARTIAL (create/edit + import/export + parse-text/file + PNG export)
- Creator badges: React DONE, Android MISSING

### Social / Profile / Notifications
- Notifications list: React DONE, Android DONE (basic)
- Social (followers/following/blocked): React DONE, Android DONE (basic)
- Profile (account, role, friends): React DONE, Android PARTIAL (basic profile only)

### Shop / Wallet / Payments
- Shop catalog: React DONE, Android PARTIAL (catalog only)
- Play Billing flow: React DONE, Android PARTIAL (client purchase + restore + ack; server config required)
- Wallet + transactions: React DONE, Android PARTIAL (read-only balance + transactions list)

### Settings / Customization
- Theme/language/backgrounds/decorations: React DONE, Android PARTIAL (theme/language local only; backgrounds/decorations done)
- Model presets: React DONE, Android PARTIAL (selection only)
- Privacy/terms links: React DONE, Android DONE
- Age verification + user config sync: React DONE, Android DONE (self-entry)

### Other React Features Missing on Android
- Personas, masks, extensions
- Fan badges, red packet (wallet/transactions partially implemented)

## 18+ Compliance Readiness (Google Play Risk Lens)

Current controls in Android:
- Age verification dialog with 18+ gate
- Privacy/terms consent on login
- Report and block character flows
- NSFW preference stored locally

High-risk gaps:
- Enforcement is partial (chat + explore + detail/creator/assistant/notifications/social/profile/shop/wallet entry guarded); new entry paths can still bypass gating without use-case checks.
- Server-driven content restrictions are limited to ageVerified + blockedTags; no richer content rating enforcement.
- Purchase verification/ack flow exists but needs end-to-end validation (server keys/config + failure handling).
- Limited in-app disclosure/controls for UGC moderation (restricted-content notice added in chat/detail; other UGC flows still missing).

Implications:
- If adult content is a core feature, Play Store approval remains high risk even
  with an 18+ gate. This needs strict content classification, gating, and safe
  defaults across all entry points.

Minimum compliance actions:
- Centralize compliance checks and enforce in all entry paths (including future deep links).
- Continue honoring server-side flags (ageVerified/blockedTags/content rating) before
  content display or chat start.
- Expand restricted-content warnings and in-app reporting affordances to all UGC flows.
- Validate purchase verification/ack end-to-end with server configuration.

## Architecture / Tech Debt

- Compose Navigation used for top-level tabs, chat session route, and detail/creator/assistant flows; deep-link handling limited (share code only).
- MVI/contract-based feature layering not implemented; ViewModel <-> Repository
  coupling remains tight.
- Room usage includes multi-session cache with eviction; resume-last-session UX added
  still missing, migrations are now non-destructive.
- i18n resource extraction completed; translations still needed.
- UI consistency is basic; lacks shared design system layer.

## Test Coverage Gaps

Existing tests: a handful of unit tests + minimal instrumentation coverage.

Missing:
- NSFW gating coverage is partial; age verification flows still untested.
- Share-code resolve and direct navigation guardrails.
- Chat session lifecycle (restore partially covered; create/open/multi-session still missing).
- Billing flow (purchase verification + error paths).
- Regression tests for report/block flows and moderation UX.

## Action Plan (Next Implementation Phase)

1) Feature parity alignment
   - Define a target feature list and map each to Android screens, ViewModels,
     APIs, and data stores.
   - Prioritize chat parity (regenerate/continue/swipes) + settings parity
     (language/theme/backgrounds) + wallet/transactions if required by product.

2) Compliance hardening
   - Centralize gating checks in a single access-control layer and enforce in
     every entry path (including share links and deep links).
   - Add content warnings and enforce server-driven content rating.
   - Complete purchase verification flow for Play compliance.

3) Architecture and tests
   - Introduce Compose Navigation with a feature graph.
   - Move toward Contract-based MVI per feature.
   - Expand Room to multi-session storage with eviction.
   - Add unit + UI tests for compliance flows and chat lifecycle.
