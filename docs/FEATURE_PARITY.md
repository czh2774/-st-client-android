# Android vs React Feature Parity (Route + API Map)

Legend: DONE / PARTIAL / MISSING.
React routes based on `st-client-React/src/routes`. API list is from
`st-client-android/app/src/main/kotlin/com/stproject/client/android/core/network/*Api.kt`
plus direct chat endpoints in `HttpChatRepository`.

## Chat & Sessions
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Sessions list | `/app-shell/chats` | DONE (basic list) | `GET chats` |
| Session messages | `/app-shell/chat` | DONE | `GET chats/{sessionId}/messages` |
| Stream completion | `/app-shell/chat` | DONE | `POST chats/{sessionId}/completion` (SSE), `POST chats` |
| Regenerate / continue | `/app-shell/chat` | DONE | `POST dialogs/regenerate`, `POST dialogs/continue` |
| Swipe actions | `/app-shell/chat` | DONE | `POST dialogs/swipe`, `POST dialogs/swipe/delete` |
| Delete message | `/app-shell/chat` | PARTIAL (last assistant only) | `POST dialogs/delete` |
| Chat share page | `/chat-share` | MISSING (Android only shows share code modal) | `GET characters/{id}/share-code`, `GET characters/share-code` |
| World info | `/app-shell/worldinfo` | MISSING | TBD |

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
| Creator badges | `/app-shell/creator/$creatorId/badges` | MISSING | TBD |
| Create / role form | `/create/form` | MISSING | TBD |

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
| Red packet | `/app-shell/red-packet` | MISSING | TBD |

## Settings / Compliance
| Feature | React route | Android status | Android API |
| --- | --- | --- | --- |
| Terms acceptance | `/settings` | DONE | `POST users/accept-tos` |
| Age verification / user config | `/settings` | DONE | `GET users/config`, `PUT users/config` |
| Theme / language | `/theme_select`, `/language_select` | PARTIAL (local only) | no API |
| Backgrounds / decorations / model presets | `/settings/*` | MISSING | TBD |

## React-Only Features (Missing on Android)
- Comments
- Personas
- Masks
- Extensions
- Fan badges / creator badges
