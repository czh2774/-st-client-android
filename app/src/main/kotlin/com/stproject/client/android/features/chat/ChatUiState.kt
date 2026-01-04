package com.stproject.client.android.features.chat

import com.stproject.client.android.domain.model.AgeRating
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ShareCodeInfo

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val isActionRunning: Boolean = false,
    val shareInfo: ShareCodeInfo? = null,
    val activeCharacterIsNsfw: Boolean? = null,
    val activeCharacterAgeRating: AgeRating? = null,
    val activeCharacterTags: List<String> = emptyList(),
    val accessError: String? = null,
    val error: String? = null,
)
