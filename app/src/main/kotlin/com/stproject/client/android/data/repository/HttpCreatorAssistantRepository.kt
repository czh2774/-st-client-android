package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.network.CreatorAssistantChatResponseDto
import com.stproject.client.android.core.network.CreatorAssistantDraftDto
import com.stproject.client.android.core.network.CreatorAssistantDraftResponseDto
import com.stproject.client.android.core.network.CreatorAssistantGenerateDraftRequestDto
import com.stproject.client.android.core.network.CreatorAssistantMessageDto
import com.stproject.client.android.core.network.CreatorAssistantPublishRequestDto
import com.stproject.client.android.core.network.CreatorAssistantSessionHistoryDto
import com.stproject.client.android.core.network.CreatorAssistantSessionSummaryDto
import com.stproject.client.android.core.network.CreatorAssistantStartRequestDto
import com.stproject.client.android.core.network.CreatorAssistantSubCharacterDto
import com.stproject.client.android.core.network.CreatorAssistantUpdateDraftRequestDto
import com.stproject.client.android.core.network.StCreatorAssistantApi
import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.CreatorAssistantChatResult
import com.stproject.client.android.domain.model.CreatorAssistantDraft
import com.stproject.client.android.domain.model.CreatorAssistantDraftResult
import com.stproject.client.android.domain.model.CreatorAssistantMessage
import com.stproject.client.android.domain.model.CreatorAssistantPublishResult
import com.stproject.client.android.domain.model.CreatorAssistantSessionHistory
import com.stproject.client.android.domain.model.CreatorAssistantSessionSummary
import com.stproject.client.android.domain.model.CreatorAssistantSubCharacter
import com.stproject.client.android.domain.repository.CreatorAssistantRepository
import com.stproject.client.android.domain.repository.CreatorAssistantSessionsResult
import com.stproject.client.android.domain.repository.CreatorAssistantStartResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpCreatorAssistantRepository
    @Inject
    constructor(
        private val api: StCreatorAssistantApi,
        private val apiClient: ApiClient,
    ) : CreatorAssistantRepository {
        override suspend fun listSessions(
            pageNum: Int,
            pageSize: Int,
            status: String?,
        ): CreatorAssistantSessionsResult {
            val resp =
                apiClient.call {
                    api.listSessions(
                        pageNum = pageNum,
                        pageSize = pageSize,
                        status = status?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
            val items = resp.items ?: emptyList()
            return CreatorAssistantSessionsResult(
                items = items.mapNotNull { it.toDomain() },
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
            )
        }

        override suspend fun startSession(
            characterType: String?,
            parentCharacterId: String?,
            initialPrompt: String?,
        ): CreatorAssistantStartResult {
            val resp =
                apiClient.call {
                    api.startSession(
                        CreatorAssistantStartRequestDto(
                            characterType = characterType?.trim()?.takeIf { it.isNotEmpty() },
                            parentCharacterId = parentCharacterId?.trim()?.takeIf { it.isNotEmpty() },
                            initialPrompt = initialPrompt?.trim()?.takeIf { it.isNotEmpty() },
                        ),
                    )
                }
            val sessionId = resp.sessionId?.trim().orEmpty()
            if (sessionId.isEmpty()) {
                throw ApiException(message = "session id missing", userMessage = "session start failed")
            }
            return CreatorAssistantStartResult(
                sessionId = sessionId,
                characterType = resp.characterType?.trim()?.takeIf { it.isNotEmpty() },
                greeting = resp.greeting?.trim()?.takeIf { it.isNotEmpty() },
                suggestions = resp.suggestions ?: emptyList(),
            )
        }

        override suspend fun getSessionHistory(sessionId: String): CreatorAssistantSessionHistory {
            val cleanId = sessionId.trim()
            if (cleanId.isEmpty()) {
                throw ApiException(message = "session id missing", userMessage = "missing session")
            }
            val resp = apiClient.call { api.getSessionHistory(cleanId) }
            return resp.toDomain()
        }

        override suspend fun chat(
            sessionId: String,
            content: String,
        ): CreatorAssistantChatResult {
            val cleanId = sessionId.trim()
            val cleanContent = content.trim()
            if (cleanId.isEmpty() || cleanContent.isEmpty()) {
                throw ApiException(message = "invalid chat request", userMessage = "message required")
            }
            val resp =
                apiClient.call {
                    api.chat(
                        com.stproject.client.android.core.network.CreatorAssistantChatRequestDto(
                            sessionId = cleanId,
                            content = cleanContent,
                        ),
                    )
                }
            return resp.toDomain()
        }

        override suspend fun generateDraft(sessionId: String): CreatorAssistantDraftResult {
            val cleanId = sessionId.trim()
            if (cleanId.isEmpty()) {
                throw ApiException(message = "session id missing", userMessage = "missing session")
            }
            val resp =
                apiClient.call {
                    api.generateDraft(CreatorAssistantGenerateDraftRequestDto(sessionId = cleanId))
                }
            return resp.toDomain()
        }

        override suspend fun updateDraft(
            sessionId: String,
            draftId: String,
            updates: Map<String, Any>,
        ): CreatorAssistantDraftResult {
            val cleanId = sessionId.trim()
            val cleanDraftId = draftId.trim()
            if (cleanId.isEmpty() || cleanDraftId.isEmpty()) {
                throw ApiException(message = "draft missing", userMessage = "missing draft")
            }
            val resp =
                apiClient.call {
                    api.updateDraft(
                        CreatorAssistantUpdateDraftRequestDto(
                            sessionId = cleanId,
                            draftId = cleanDraftId,
                            updates = updates,
                        ),
                    )
                }
            return resp.toDomain()
        }

        override suspend fun publish(
            sessionId: String,
            draftId: String,
            isPublic: Boolean,
        ): CreatorAssistantPublishResult {
            val cleanId = sessionId.trim()
            val cleanDraftId = draftId.trim()
            if (cleanId.isEmpty() || cleanDraftId.isEmpty()) {
                throw ApiException(message = "draft missing", userMessage = "missing draft")
            }
            val resp =
                apiClient.call {
                    api.publish(
                        CreatorAssistantPublishRequestDto(
                            sessionId = cleanId,
                            draftId = cleanDraftId,
                            isPublic = isPublic,
                        ),
                    )
                }
            val characterId = resp.characterId?.trim().orEmpty()
            if (characterId.isEmpty()) {
                throw ApiException(message = "publish missing id", userMessage = "publish failed")
            }
            return CreatorAssistantPublishResult(
                characterId = characterId,
                name = resp.name?.trim()?.takeIf { it.isNotEmpty() },
                avatarUrl = resp.avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
                backgroundUrl = resp.backgroundUrl?.trim()?.takeIf { it.isNotEmpty() },
                shareCode = resp.shareCode?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        override suspend fun abandon(sessionId: String): Boolean {
            val cleanId = sessionId.trim()
            if (cleanId.isEmpty()) return false
            apiClient.call { api.abandonSession(cleanId) }
            return true
        }

        private fun CreatorAssistantSessionSummaryDto.toDomain(): CreatorAssistantSessionSummary? {
            val idValue = sessionId?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            return CreatorAssistantSessionSummary(
                sessionId = idValue,
                characterType = characterType?.trim()?.takeIf { it.isNotEmpty() },
                status = status?.trim()?.takeIf { it.isNotEmpty() },
                draftName = draftName?.trim()?.takeIf { it.isNotEmpty() },
                draftIsNsfw = draftIsNsfw,
                draftTags = draftTags ?: emptyList(),
                draftModerationAgeRating = AgeRating.from(draftModerationAgeRating),
                messageCount = messageCount ?: 0,
                createdAt = createdAt?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = updatedAt?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        private fun CreatorAssistantSessionHistoryDto.toDomain(): CreatorAssistantSessionHistory {
            val idValue = sessionId?.trim().orEmpty()
            if (idValue.isEmpty()) {
                throw ApiException(message = "session id missing", userMessage = "session not found")
            }
            return CreatorAssistantSessionHistory(
                sessionId = idValue,
                characterType = characterType?.trim()?.takeIf { it.isNotEmpty() },
                status = status?.trim()?.takeIf { it.isNotEmpty() },
                messages = messages?.mapNotNull { it.toDomain() } ?: emptyList(),
                currentDraft = currentDraft?.toDomain(),
                createdAt = createdAt?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = updatedAt?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        private fun CreatorAssistantMessageDto.toDomain(): CreatorAssistantMessage? {
            val idValue = messageId?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            val roleValue = role?.trim()?.takeIf { it.isNotEmpty() } ?: "assistant"
            return CreatorAssistantMessage(
                messageId = idValue,
                role = roleValue,
                content = content?.trim().orEmpty(),
                createdAt = createdAt?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        private fun CreatorAssistantChatResponseDto.toDomain(): CreatorAssistantChatResult {
            val messageIdValue = messageId?.trim().orEmpty()
            if (messageIdValue.isEmpty()) {
                throw ApiException(message = "message id missing", userMessage = "message failed")
            }
            return CreatorAssistantChatResult(
                messageId = messageIdValue,
                content = content?.trim().orEmpty(),
                suggestions = suggestions ?: emptyList(),
                draftReady = draftReady ?: false,
            )
        }

        private fun CreatorAssistantDraftResponseDto.toDomain(): CreatorAssistantDraftResult {
            val draftIdValue = draftId?.trim().orEmpty()
            if (draftIdValue.isEmpty() || draft == null) {
                throw ApiException(message = "draft missing", userMessage = "draft not ready")
            }
            return CreatorAssistantDraftResult(
                draftId = draftIdValue,
                draft = draft.toDomain(),
                confidence = confidence ?: 0.0,
                missingFields = missingFields ?: emptyList(),
            )
        }

        private fun CreatorAssistantDraftDto.toDomain(): CreatorAssistantDraft {
            return CreatorAssistantDraft(
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                greeting = greeting?.trim()?.takeIf { it.isNotEmpty() },
                personality = personality?.trim()?.takeIf { it.isNotEmpty() },
                scenario = scenario?.trim()?.takeIf { it.isNotEmpty() },
                exampleDialogs = exampleDialogs?.trim()?.takeIf { it.isNotEmpty() },
                tags = tags ?: emptyList(),
                gender = gender ?: 0,
                isNsfw = isNsfw,
                characterType = characterType?.trim()?.takeIf { it.isNotEmpty() },
                parentCharacterId = parentCharacterId?.trim()?.takeIf { it.isNotEmpty() },
                subCharacters = subCharacters?.map { it.toDomain() } ?: emptyList(),
            )
        }

        private fun CreatorAssistantSubCharacterDto.toDomain(): CreatorAssistantSubCharacter {
            return CreatorAssistantSubCharacter(
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                personality = personality?.trim()?.takeIf { it.isNotEmpty() },
                avatarBase64 = avatarBase64?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
