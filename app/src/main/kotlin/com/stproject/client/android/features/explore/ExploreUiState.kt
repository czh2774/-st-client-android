package com.stproject.client.android.features.explore

import com.stproject.client.android.domain.model.CharacterSummary

data class ExploreUiState(
    val isLoading: Boolean = false,
    val items: List<CharacterSummary> = emptyList(),
    val error: String? = null,
    val accessError: String? = null,
    val shareCodeInput: String = "",
    val shareCodeError: String? = null,
    val isResolvingShareCode: Boolean = false,
    val resolvedShareCode: String? = null,
    val resolvedMemberId: String? = null,
    val resolvedIsNsfw: Boolean? = null,
)
