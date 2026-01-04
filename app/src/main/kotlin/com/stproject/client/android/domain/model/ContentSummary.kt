package com.stproject.client.android.domain.model

data class ContentSummary(
    val characterId: String?,
    val isNsfw: Boolean?,
    val moderationAgeRating: AgeRating?,
    val tags: List<String> = emptyList(),
    val visibility: String? = null,
)
