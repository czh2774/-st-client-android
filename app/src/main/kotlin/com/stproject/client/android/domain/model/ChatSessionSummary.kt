package com.stproject.client.android.domain.model

data class ChatSessionSummary(
    val sessionId: String,
    val primaryMemberId: String?,
    val displayName: String,
    val updatedAt: String?,
    val primaryMemberIsNsfw: Boolean? = null,
    val primaryMemberAgeRating: AgeRating? = null,
    val primaryMemberTags: List<String> = emptyList(),
)
