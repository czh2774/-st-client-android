# Android Alignment Tasks

Priority scale:
- P0: blocking for release/compliance
- P1: major parity
- P2: quality/UX

Feature parity map (route/feature list):
- `FEATURE_PARITY.md`

## P0 Compliance and Store Readiness
- [x] Gate NSFW access on share-code join, explore list, creator list, and character detail entry points (client UI).
- [x] Gate NSFW access on chat session open (best-effort when primaryMemberId is available).
- [x] Gate NSFW access on shop + wallet entry points (client UI).
- [x] Centralize compliance checks (ageVerified/allowNsfw) in a single access-control layer.
- [ ] Add restricted-content disclosures and in-app reporting prompts for UGC flows.
- [x] Complete Play Billing purchase verification + acknowledgement (client + server, signature mode requires env config).
- [x] Server support: `/iap/transactions` accepts `platform=android` (signature verification requires `IAP_ANDROID_LICENSE_KEY`).
- [ ] Add privacy/terms links to all entry points that can bypass login (if any).

## P0 Core Feature Parity
- [x] Chat: regenerate/continue/swipes.
- [x] Chat: message delete/delete-after (last assistant only).
- [ ] Chat: multi-branch handling + long-press tools.
- [x] Chat history: multi-session local cache + eviction.
- [ ] Chat history: restore on cold start.
- [ ] WorldInfo tooling and chat share flow.
- [ ] Creator tools: create/edit character + role form.

## P1 Product Parity
- [x] Wallet + transactions (Android: read-only balance + transactions list)
- [ ] Fan badges + creator badges
- [ ] Red packet
- [ ] Personas + masks + extensions
- [ ] Comments

## P2 UX and Platform
- [ ] Compose Navigation graph + deep links + state restoration (top-level tabs + chat/detail/creator/assistant routes moved to NavHost; remaining nested flows pending).
- [x] i18n resource extraction for all strings.
- [ ] Design system layer (typography, spacing, surface styles).
- [ ] Performance: list paging, image loading with caching (Coil).

## Testing
- [ ] Compliance flows: age verification, NSFW gating, share-code join.
- [ ] Chat lifecycle: create/open/restore/multi-session.
- [ ] Billing flow: success, cancel, error, ack.
- [ ] Moderation: report/block flows.
