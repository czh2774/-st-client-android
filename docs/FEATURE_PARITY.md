# Android vs React Feature Parity (Route + API Map)

Legend: DONE / PARTIAL / MISSING.
React routes based on `st-client-React/src/routes`. API list is from
`st-client-android/app/src/main/kotlin/com/stproject/client/android/core/network/*Api.kt`
plus direct chat endpoints in `HttpChatRepository`.
Play shipping scope (authoritative for Play builds) is tracked in `PLAY_PARITY_TARGET.md`.

## Chat & Sessions
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Sessions list | `/app-shell/chats` | DONE (basic list) | `GET chats` |
| Session messages | `/app-shell/chat` | DONE | `GET chats/{sessionId}/messages` |
| Stream completion | `/app-shell/chat` | DONE | `POST chats/{sessionId}/completion` (SSE), `POST chats` |
| Regenerate / continue | `/app-shell/chat` | DONE | `POST dialogs/regenerate`, `POST dialogs/continue` |
| Swipe actions | `/app-shell/chat` | DONE | `POST dialogs/swipe`, `POST dialogs/swipe/delete` |
| Delete message | `/app-shell/chat` | DONE | `POST dialogs/delete` |
| Chat share page | `/chat-share` | DONE | `GET characters/{id}/share-code`, `GET characters/share-code` |
| World info | `/app-shell/worldinfo` | DONE (basic CRUD) | `GET/POST/PUT/DELETE worldinfo` |

Deep link / share-code entry:
`stproject://share/c/{code}` or `...?shareCode={code}`.

## Explore & Characters
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Explore feed | `/app-shell/explore` | PARTIAL (homepage sort only) | `POST characters/query` |
| Character detail | `/app-shell/explore` | DONE | `GET characters/{id}` |
| Follow / block | `/app-shell/explore` | DONE | `POST characters/follow`, `POST characters/block` |
| Share code resolve | `/app-shell/explore` | DONE (basic UI) | `GET characters/share-code` |

## Creators & Assistant
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Creators list | `/app-shell/creators` | PARTIAL (basic list) | `GET creators/list` |
| Creator characters | `/app-shell/creators` | DONE | `GET creators/{id}/characters` |
| Creator assistant list/chat | `/creator-assistant` | DONE (basic) | `POST creator-assistant/start`, `GET creator-assistant/sessions`, `POST creator-assistant/chat` |
| Assistant draft/publish | `/creator-assistant` | DONE (basic) | `POST creator-assistant/generate-draft`, `POST creator-assistant/update-draft`, `POST creator-assistant/publish` |
| Creator badges | `/app-shell/creator/$creatorId/badges` | DONE (feature-flagged via BADGES_ENABLED) | `GET users/fan-badges?creatorId=...`, `POST users/fan-badges/purchase` |
| My badges | `/app-shell/fan_badges` | DONE (feature-flagged via BADGES_ENABLED) | `GET users/fan-badges/purchased`, `POST users/fan-badges/equip` |
| Create / role form | `/create/form` | PARTIAL (create/edit + import/export + parse-text/file + PNG export) | `POST/PUT cards`, `GET characters/{id}/export`, `GET characters/{id}/export-png`, `POST cards/parse-text`, `POST cards/parse-file`, `GET cards/template` |

## Social / Notifications / Profile
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Notifications | `/app-shell/notifications` | DONE (basic) | `GET notifications`, `GET notifications/unread`, `POST notifications/read` |
| Social (followers/following/blocked) | `/app-shell/profile/friends` | DONE (basic) | `GET users/followers`, `GET users/following`, `GET users/blocked`, `POST users/follow`, `POST users/block` |
| Profile summary | `/app-shell/profile` | PARTIAL | `GET users/me` |
| Account deletion | `/settings` | DONE | `DELETE users/me` |

## Shop / Wallet
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Shop catalog | `/app-shell/shop` | PARTIAL | `GET iap/products` |
| Purchase submit / restore | `/app-shell/shop` | PARTIAL (server config required) | `POST iap/transactions`, `POST iap/restore` |
| Wallet balance | `/app-shell/wallet` | PARTIAL | `GET wallet/balance` |
| Wallet transactions | `/app-shell/transactions` | PARTIAL | `GET wallet/transactions` |
| Red packet | `/app-shell/red-packet` | MISSING (flag only) | no Android API |

## Settings / Compliance
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Terms acceptance | `/settings` | DONE | `POST users/accept-tos` |
| Age verification / user config | `/settings` | DONE | `GET users/config`, `PUT users/config` |
| Personas | `/app-shell/chat` | DONE (Settings > Personas) | `GET/POST/PUT/DELETE personas` |
| Theme / language | `/theme_select`, `/language_select` | PARTIAL (local only) | no API |
| Backgrounds / decorations / model presets | `/settings/*` | PARTIAL (backgrounds + decorations done; model presets selection only) | `GET backgrounds/all`, `POST backgrounds/upload`, `POST backgrounds/rename`, `POST backgrounds/delete`, `GET users/decorations`, `POST users/decorations/equip`, `GET presets` |

## Comments
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Comment list + create + like + delete + replies | `/app-shell/explore` (comments sheet) | DONE (basic) | `GET comments`, `POST comments`, `POST comments/{id}/like`, `DELETE comments/{id}` |

## React-Only Features (Missing on Android)
- Extensions (permanently disabled on Android)
- Red packet (Play: excluded until fixed-credit design passes policy review)
