package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.CreatorCardDto
import com.stproject.client.android.core.network.CreatorCharacterDto
import com.stproject.client.android.core.network.StCreatorApi
import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.CreatorCharacter
import com.stproject.client.android.domain.model.CreatorSummary
import com.stproject.client.android.domain.repository.CreatorCharactersResult
import com.stproject.client.android.domain.repository.CreatorListResult
import com.stproject.client.android.domain.repository.CreatorRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpCreatorRepository
    @Inject
    constructor(
        private val api: StCreatorApi,
        private val apiClient: ApiClient,
    ) : CreatorRepository {
        override suspend fun listCreators(
            limit: Int,
            cursor: String?,
            sortBy: String?,
            searchKeyword: String?,
        ): CreatorListResult {
            val resp =
                apiClient.call {
                    api.listCreators(
                        limit = limit,
                        cursor = cursor?.trim()?.takeIf { it.isNotEmpty() },
                        sortBy = sortBy?.trim()?.takeIf { it.isNotEmpty() },
                        searchKeyword = searchKeyword?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
            val items = resp.items ?: emptyList()
            return CreatorListResult(
                items = items.mapNotNull { it.toDomain() },
                hasMore = resp.hasMore ?: false,
                nextCursor = resp.nextCursor?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        override suspend fun listCreatorCharacters(
            creatorId: String,
            pageNum: Int,
            pageSize: Int,
        ): CreatorCharactersResult {
            val resp =
                apiClient.call {
                    api.listCreatorCharacters(
                        creatorId = creatorId,
                        pageNum = pageNum,
                        pageSize = pageSize,
                    )
                }
            val items = resp.items ?: emptyList()
            return CreatorCharactersResult(
                items = items.mapNotNull { it.toDomain() },
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
            )
        }

        private fun CreatorCardDto.toDomain(): CreatorSummary? {
            val idValue = (id ?: userId)?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            val display =
                displayName?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: username?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: idValue
            return CreatorSummary(
                id = idValue,
                displayName = display,
                avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
                followerCount = followerCount ?: 0,
                interactionCount = interactionCount ?: 0,
                bio = bio?.trim()?.takeIf { it.isNotEmpty() },
                followStatus = followStatus ?: 0,
                isBlocked = isBlocked ?: false,
            )
        }

        private fun CreatorCharacterDto.toDomain(): CreatorCharacter? {
            val idValue = characterId?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            return CreatorCharacter(
                id = idValue,
                name = name?.trim().orEmpty(),
                description = description?.trim().orEmpty(),
                avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
                backgroundUrl = backgroundUrl?.trim()?.takeIf { it.isNotEmpty() },
                tags = tags ?: emptyList(),
                isNsfw = isNsfw ?: false,
                moderationAgeRating = AgeRating.from(moderationAgeRating),
                creatorId = creatorId?.trim()?.takeIf { it.isNotEmpty() },
                creatorName = creatorName?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = updatedAt?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
