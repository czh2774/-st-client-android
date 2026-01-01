package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val messages: Flow<List<ChatMessage>>

    suspend fun sendUserMessage(content: String)

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

    suspend fun clearLocalSession()
}
