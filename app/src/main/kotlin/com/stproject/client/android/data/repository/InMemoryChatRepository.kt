package com.stproject.client.android.data.repository

import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.A2UIActionResult
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole
import com.stproject.client.android.domain.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal repository to unblock UI/architecture wiring.
 *
 * Replace with a real implementation (Retrofit + SSE + Room) once endpoints are ready.
 */
@Singleton
class InMemoryChatRepository
    @Inject
    constructor() : ChatRepository {
        private val _messages =
            MutableStateFlow<List<ChatMessage>>(
                listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.System,
                        content = "Chat repository is currently in-memory (stub).",
                    ),
                ),
            )
        private var sessionVariables: Map<String, Any?> = emptyMap()
        private val _a2uiState = MutableStateFlow<A2UIRuntimeState?>(null)

        override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()
        override val a2uiState: Flow<A2UIRuntimeState?> = _a2uiState.asStateFlow()

        override suspend fun sendUserMessage(content: String) {
            val user =
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.User,
                    content = content,
                )

            _messages.value = _messages.value + user

            // Simulate assistant response.
            delay(250)
            val assistant =
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.Assistant,
                    content = "Echo: $content",
                )
            _messages.value = _messages.value + assistant
        }

        override suspend fun sendA2UIAction(action: A2UIAction): A2UIActionResult {
            return A2UIActionResult(accepted = false, reason = "not_supported")
        }

        override suspend fun startNewSession(
            memberId: String,
            shareCode: String?,
        ) {
            _messages.value =
                listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.System,
                        content = "New session started for $memberId",
                    ),
                )
        }

        override suspend fun openSession(
            sessionId: String,
            primaryMemberId: String?,
        ) {
            _messages.value =
                listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.System,
                        content = "Opened session $sessionId",
                    ),
                )
        }

        override suspend fun listSessions(
            limit: Int,
            offset: Int,
        ): List<com.stproject.client.android.domain.model.ChatSessionSummary> {
            return emptyList()
        }

        override suspend fun getLastSessionSummary(): com.stproject.client.android.domain.model.ChatSessionSummary? {
            return null
        }

        override suspend fun regenerateMessage(messageId: String) {
            // No-op for in-memory stub.
        }

        override suspend fun continueMessage(messageId: String) {
            // No-op for in-memory stub.
        }

        override suspend fun deleteMessage(
            messageId: String,
            deleteAfter: Boolean,
        ) {
            _messages.value = _messages.value.filterNot { it.id == messageId }
        }

        override suspend fun setActiveSwipe(
            messageId: String,
            swipeId: Int,
        ) {
            // No-op for in-memory stub.
        }

        override suspend fun deleteSwipe(
            messageId: String,
            swipeId: Int?,
        ) {
            // No-op for in-memory stub.
        }

        override suspend fun loadSessionVariables(): Map<String, Any?> {
            return sessionVariables
        }

        override suspend fun updateSessionVariables(variables: Map<String, Any?>) {
            sessionVariables = variables
        }

        override suspend fun updateMessageVariables(
            messageId: String,
            swipesData: List<Map<String, Any?>>,
        ) {
            _messages.value =
                _messages.value.map { message ->
                    if (message.id != messageId) return@map message
                    val nextMetadata =
                        (message.metadata ?: emptyMap()) + mapOf("swipes_data" to swipesData)
                    message.copy(metadata = nextMetadata)
                }
        }

        override suspend fun clearLocalSession() {
            _messages.value = emptyList()
        }
    }
