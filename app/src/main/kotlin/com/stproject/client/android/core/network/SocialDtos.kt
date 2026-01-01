package com.stproject.client.android.core.network

data class FollowRequestDto(
    val userId: String,
    val value: Boolean,
)

data class FollowResponseDto(
    val success: Boolean? = null,
    val followerCount: Int? = null,
    val followStatus: Int? = null,
    val created: Boolean? = null,
    val ok: Boolean? = null,
)

data class SocialUserDto(
    val id: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val level: Int? = null,
    val followerCount: Int? = null,
    val followStatus: Int? = null,
    val followedAt: String? = null,
    val blockedAt: String? = null,
)

data class FollowListResponseDto(
    val items: List<SocialUserDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
)

data class BlockResponseDto(
    val success: Boolean? = null,
)
