package com.stproject.client.android.features.creators

import com.stproject.client.android.domain.model.CreatorAssistantSessionSummary

data class CreatorAssistantListUiState(
    val items: List<CreatorAssistantSessionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    val newSessionId: String? = null,
    val openSessionId: String? = null,
)
