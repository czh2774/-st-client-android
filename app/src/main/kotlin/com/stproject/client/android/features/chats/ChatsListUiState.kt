package com.stproject.client.android.features.chats

import com.stproject.client.android.domain.model.ChatSessionSummary

data class ChatsListUiState(
    val isLoading: Boolean = false,
    val items: List<ChatSessionSummary> = emptyList(),
    val error: String? = null,
)
