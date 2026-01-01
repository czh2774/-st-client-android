package com.stproject.client.android.features.creators

import com.stproject.client.android.domain.model.CreatorAssistantDraft
import com.stproject.client.android.domain.model.CreatorAssistantDraftResult
import com.stproject.client.android.domain.model.CreatorAssistantMessage
import com.stproject.client.android.domain.model.CreatorAssistantPublishResult

data class CreatorAssistantChatUiState(
    val sessionId: String? = null,
    val messages: List<CreatorAssistantMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isDrafting: Boolean = false,
    val isPublishing: Boolean = false,
    val error: String? = null,
    val draftReady: Boolean = false,
    val currentDraft: CreatorAssistantDraft? = null,
    val draftResult: CreatorAssistantDraftResult? = null,
    val publishResult: CreatorAssistantPublishResult? = null,
)
