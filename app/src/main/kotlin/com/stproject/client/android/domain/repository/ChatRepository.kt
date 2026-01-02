package com.stproject.client.android.domain.repository

import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.A2UIActionResult
import com.stproject.client.android.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val messages: Flow<List<ChatMessage>>
    val a2uiState: Flow<A2UIRuntimeState?>

    suspend fun sendUserMessage(content: String)

    suspend fun sendA2UIAction(action: A2UIAction): A2UIActionResult

    suspend fun startNewSession(
        memberId: String,
        shareCode: String? = null,
    )

    suspend fun openSession(
        sessionId: String,
        primaryMemberId: String?,
    )

    suspend fun listSessions(
        limit: Int,
        offset: Int,
    ): List<com.stproject.client.android.domain.model.ChatSessionSummary>

    suspend fun getLastSessionSummary(): com.stproject.client.android.domain.model.ChatSessionSummary?

    suspend fun regenerateMessage(messageId: String)

    suspend fun continueMessage(messageId: String)

    suspend fun deleteMessage(
        messageId: String,
        deleteAfter: Boolean,
    )

    suspend fun setActiveSwipe(
        messageId: String,
        swipeId: Int,
    )

    suspend fun deleteSwipe(
        messageId: String,
        swipeId: Int?,
    )

    suspend fun loadSessionVariables(): Map<String, Any>

    suspend fun updateSessionVariables(variables: Map<String, Any>)

    suspend fun clearLocalSession()
}
