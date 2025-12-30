package com.stproject.client.android.features.chat

import com.stproject.client.android.domain.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)


