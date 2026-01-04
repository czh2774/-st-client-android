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
    val previewCharacters: List<CreatorPreview> = emptyList(),
)

data class CreatorPreview(
    val id: String,
    val name: String,
    val backgroundUrl: String?,
    val tags: List<String>,
    val creatorName: String?,
    val isNsfw: Boolean?,
    val moderationAgeRating: AgeRating?,
    val visibility: String?,
)
