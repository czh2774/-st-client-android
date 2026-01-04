package com.stproject.client.android.features.creators

import com.stproject.client.android.domain.model.CreatorSummary

data class CreatorsUiState(
    val isLoading: Boolean = false,
    val items: List<CreatorSummary> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val searchKeyword: String = "",
    val sortBy: String = "recommend",
)
