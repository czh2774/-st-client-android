package com.stproject.client.android.domain.model

data class SocialUserSummary(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val level: Int?,
    val followerCount: Int?,
    val followStatus: Int?,
)
