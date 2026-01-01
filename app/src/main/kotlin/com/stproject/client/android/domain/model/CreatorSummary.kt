package com.stproject.client.android.domain.model

data class CreatorSummary(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val followerCount: Int,
    val interactionCount: Int,
    val bio: String?,
    val followStatus: Int,
    val isBlocked: Boolean,
)
