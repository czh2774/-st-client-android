package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.FollowRequestDto
import com.stproject.client.android.core.network.SocialUserDto
import com.stproject.client.android.core.network.StSocialApi
import com.stproject.client.android.domain.model.SocialUserSummary
import com.stproject.client.android.domain.repository.SocialListResult
import com.stproject.client.android.domain.repository.SocialRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpSocialRepository
    @Inject
    constructor(
        private val api: StSocialApi,
        private val apiClient: ApiClient,
    ) : SocialRepository {
        override suspend fun followUser(
            userId: String,
            value: Boolean,
        ) {
            val cleanId = userId.trim()
            if (cleanId.isEmpty()) return
            apiClient.call { api.followUser(FollowRequestDto(userId = cleanId, value = value)) }
        }

        override suspend fun blockUser(
            userId: String,
            value: Boolean,
        ) {
            val cleanId = userId.trim()
            if (cleanId.isEmpty()) return
            apiClient.call { api.blockUser(FollowRequestDto(userId = cleanId, value = value)) }
        }

        override suspend fun listFollowers(
            pageNum: Int,
            pageSize: Int,
            userId: String?,
        ): SocialListResult {
            val resp =
                apiClient.call {
                    api.listFollowers(
                        pageNum = pageNum,
                        pageSize = pageSize,
                        userId = userId?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
            val items = resp.items ?: emptyList()
            return SocialListResult(
                items = items.mapNotNull { it.toDomain() },
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
            )
        }

        override suspend fun listFollowing(
            pageNum: Int,
            pageSize: Int,
            userId: String?,
        ): SocialListResult {
            val resp =
                apiClient.call {
                    api.listFollowing(
                        pageNum = pageNum,
                        pageSize = pageSize,
                        userId = userId?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
            val items = resp.items ?: emptyList()
            return SocialListResult(
                items = items.mapNotNull { it.toDomain() },
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
            )
        }

        override suspend fun listBlocked(
            pageNum: Int,
            pageSize: Int,
        ): SocialListResult {
            val resp =
                apiClient.call {
                    api.listBlocked(
                        pageNum = pageNum,
                        pageSize = pageSize,
                    )
                }
            val items = resp.items ?: emptyList()
            return SocialListResult(
                items = items.mapNotNull { it.toDomain() },
                total = resp.total ?: items.size,
                hasMore = resp.hasMore ?: false,
            )
        }

        private fun SocialUserDto.toDomain(): SocialUserSummary? {
            val idValue = (id ?: userId)?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            val name =
                displayName?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: username?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: idValue
            return SocialUserSummary(
                id = idValue,
                displayName = name,
                avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
                bio = bio?.trim()?.takeIf { it.isNotEmpty() },
                level = level,
                followerCount = followerCount,
                followStatus = followStatus,
            )
        }
    }
