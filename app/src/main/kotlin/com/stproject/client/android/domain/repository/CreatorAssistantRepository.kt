package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.CreatorAssistantChatResult
import com.stproject.client.android.domain.model.CreatorAssistantDraftResult
import com.stproject.client.android.domain.model.CreatorAssistantPublishResult
import com.stproject.client.android.domain.model.CreatorAssistantSessionHistory
import com.stproject.client.android.domain.model.CreatorAssistantSessionSummary

data class CreatorAssistantSessionsResult(
    val items: List<CreatorAssistantSessionSummary>,
    val total: Int,
    val hasMore: Boolean,
)

data class CreatorAssistantStartResult(
    val sessionId: String,
    val characterType: String?,
    val greeting: String?,
    val suggestions: List<String>,
)

interface CreatorAssistantRepository {
    suspend fun listSessions(
        pageNum: Int,
        pageSize: Int,
        status: String? = null,
    ): CreatorAssistantSessionsResult

    suspend fun startSession(
        characterType: String? = null,
        parentCharacterId: String? = null,
        initialPrompt: String? = null,
    ): CreatorAssistantStartResult

    suspend fun getSessionHistory(sessionId: String): CreatorAssistantSessionHistory

    suspend fun chat(
        sessionId: String,
        content: String,
    ): CreatorAssistantChatResult

    suspend fun generateDraft(sessionId: String): CreatorAssistantDraftResult

    suspend fun updateDraft(
        sessionId: String,
        draftId: String,
        updates: Map<String, Any>,
    ): CreatorAssistantDraftResult

    suspend fun publish(
        sessionId: String,
        draftId: String,
        isPublic: Boolean,
    ): CreatorAssistantPublishResult

    suspend fun abandon(sessionId: String): Boolean
}
