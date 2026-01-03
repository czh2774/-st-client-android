# Android Test Plan (Parity + Compliance)

This plan scopes the minimum test coverage needed for feature parity,
compliance gating, and Play billing stability.

## Scope
- Compliance: consent, age verification, NSFW gating.
- Navigation: entry paths, deep links, share codes.
- Chat: session lifecycle, streaming, actions, multi-session restore.
- Billing: purchase submit, restore, acknowledge/consume paths.
- Moderation: report/block flows.

## Unit Tests (JVM)
- `ContentGate` decision matrix (`CONSENT_REQUIRED`, `AGE_REQUIRED`,
  `NSFW_DISABLED`).
- `ResolveContentAccessUseCase` behavior with/without `memberId` and `isNsfw`.
- `PolicyUrlProvider` and `StBaseUrlProvider` release guards.
- Share code parsing utilities (URL and raw code variants).
- Chat repository core logic: swipes, delete, regenerate/continue request
  building, metadata handling.
- Comments: list/create/reply/like/delete logic in ViewModel/repository layers.
- Billing transformations: product -> query type, transaction payload mapping.

## Instrumentation / Integration Tests
- Auth + consent + age gating flow from cold start.
- Share-code deep link -> gated block -> allow after verification.
- Explore -> detail -> start chat flow with NSFW blocked and allowed.
- Chat session: create -> stream -> restore -> multi-session open.
- Billing: purchase update -> server submit -> ack/consume (happy + error).

## UI Tests (Compose)
- Consent dialog + age verification dialog rendering and lockout behavior.
- "Mature content disabled" dialog from explore, detail, chat entry.
- Restricted content notice visibility when NSFW is allowed.
- Shop purchase disabled state and error surfaces.
- Report dialog submit success/close state.

## Manual Smoke Checks (Settings)
- Settings -> Customization -> Backgrounds: upload image, rename, delete; verify list refresh and copy link.
- Settings -> Customization -> Backgrounds: thumbnail size hint visible when config is present.
- Settings -> Customization -> Decorations: switch types, equip/unequip owned items, empty state shows Shop entry.

## Coverage Targets
- Compliance and entry-path gating: 100% for all routes that can open content.
- Chat lifecycle: create/open/restore/switch sessions covered.
- Billing: success + cancel + error + ack failure paths covered.

## Run Notes
- JVM: `./gradlew :app:testDebugUnitTest`
- Instrumentation: `./gradlew :app:connectedAndroidTest`
