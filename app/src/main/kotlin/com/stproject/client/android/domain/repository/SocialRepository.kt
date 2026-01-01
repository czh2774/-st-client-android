package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.SocialUserSummary

data class SocialListResult(
    val items: List<SocialUserSummary>,
    val total: Int,
    val hasMore: Boolean,
)

interface SocialRepository {
    suspend fun followUser(
        userId: String,
        value: Boolean,
    )

    suspend fun blockUser(
        userId: String,
        value: Boolean,
    )

    suspend fun listFollowers(
        pageNum: Int,
        pageSize: Int,
        userId: String? = null,
    ): SocialListResult

    suspend fun listFollowing(
        pageNum: Int,
        pageSize: Int,
        userId: String? = null,
    ): SocialListResult

    suspend fun listBlocked(
        pageNum: Int,
        pageSize: Int,
    ): SocialListResult
}
