package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.CommentCharacterDto
import com.stproject.client.android.core.network.CommentItemDto
import com.stproject.client.android.core.network.CreateCommentRequestDto
import com.stproject.client.android.core.network.LikeCommentRequestDto
import com.stproject.client.android.core.network.StCommentApi
import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.Comment
import com.stproject.client.android.domain.model.CommentLikeResult
import com.stproject.client.android.domain.model.CommentListResult
import com.stproject.client.android.domain.model.CommentSort
import com.stproject.client.android.domain.model.CommentUser
import com.stproject.client.android.domain.model.ContentSummary
import com.stproject.client.android.domain.repository.CommentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpCommentRepository
    @Inject
    constructor(
        private val api: StCommentApi,
        private val apiClient: ApiClient,
    ) : CommentRepository {
        override suspend fun listComments(
            characterId: String,
            sort: CommentSort,
            pageNum: Int,
            pageSize: Int,
        ): CommentListResult {
            val resp =
                apiClient.call {
                    api.listComments(
                        characterId = characterId,
                        sortBy = sort.apiValue,
                        pageNum = pageNum,
                        pageSize = pageSize,
                    )
                }
            val items = resp.items?.mapNotNull { it.toDomain(characterId) } ?: emptyList()
            return CommentListResult(
                items = items,
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
                character = resp.character?.toDomain(),
            )
        }

        override suspend fun createComment(
            characterId: String,
            content: String,
            parentId: String?,
        ): Comment {
            val resp =
                apiClient.call {
                    api.createComment(
                        CreateCommentRequestDto(
                            characterId = characterId,
                            content = content,
                            parentId = parentId,
                        ),
                    )
                }
            return resp.toDomain(characterId)
                ?: Comment(
                    id = "",
                    characterId = characterId,
                    userId = "",
                    content = content,
                    likesCount = 0,
                    isLiked = false,
                    createdAt = "",
                    parentId = parentId,
                    user = null,
                    replies = emptyList(),
                )
        }

        override suspend fun deleteComment(commentId: String) {
            apiClient.call { api.deleteComment(commentId) }
        }

        override suspend fun likeComment(
            commentId: String,
            value: Boolean,
        ): CommentLikeResult {
            val resp =
                apiClient.call {
                    api.likeComment(
                        commentId,
                        LikeCommentRequestDto(value = value),
                    )
                }
            return CommentLikeResult(
                likesCount = resp.likesCount ?: 0,
                isLiked = resp.isLiked ?: value,
            )
        }

        private fun CommentItemDto.toDomain(fallbackCharacterId: String): Comment? {
            val idValue = id?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            val characterIdValue = characterId?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackCharacterId
            val userIdValue = userId?.trim().orEmpty()
            val usernameValue = username?.trim().orEmpty()
            val user =
                if (usernameValue.isNotEmpty()) {
                    CommentUser(
                        id = userIdValue,
                        username = usernameValue,
                        avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
                    )
                } else {
                    null
                }
            val replies = replies?.mapNotNull { it.toDomain(characterIdValue) } ?: emptyList()
            return Comment(
                id = idValue,
                characterId = characterIdValue,
                userId = userIdValue,
                content = content?.trim().orEmpty(),
                likesCount = likesCount ?: 0,
                isLiked = isLiked ?: false,
                createdAt = createdAt?.trim().orEmpty(),
                parentId = parentId?.trim()?.takeIf { it.isNotEmpty() },
                user = user,
                replies = replies,
            )
        }

        private fun CommentCharacterDto.toDomain(): ContentSummary? {
            val idValue = id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val cleanedTags =
                tags
                    ?.mapNotNull { tag -> tag.trim().takeIf { it.isNotEmpty() } }
                    ?: emptyList()
            return ContentSummary(
                characterId = idValue,
                isNsfw = isNsfw,
                moderationAgeRating = AgeRating.from(moderationAgeRating),
                tags = cleanedTags,
                visibility = visibility?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
