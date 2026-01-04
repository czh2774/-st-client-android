package com.stproject.client.android.features.explore

import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.CharacterSummary

data class ExploreUiState(
    val isLoading: Boolean = false,
    val items: List<CharacterSummary> = emptyList(),
    val error: String? = null,
    val accessError: String? = null,
    val sortBy: String = "homepage",
    val searchKeyword: String = "",
    val tagsInput: String = "",
    val shareCodeInput: String = "",
    val shareCodeError: String? = null,
    val isResolvingShareCode: Boolean = false,
    val resolvedShareCode: String? = null,
    val resolvedMemberId: String? = null,
    val resolvedIsNsfw: Boolean? = null,
    val resolvedAgeRating: AgeRating? = null,
    val resolvedTags: List<String>? = null,
)
