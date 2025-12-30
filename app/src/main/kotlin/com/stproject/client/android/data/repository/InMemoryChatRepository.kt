package com.stproject.client.android.data.repository

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
class InMemoryChatRepository @Inject constructor() : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.System,
                content = "Chat repository is currently in-memory (stub)."
            )
        )
    )

    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendUserMessage(content: String) {
        val user = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.User,
            content = content
        )

        _messages.value = _messages.value + user

        // Simulate assistant response.
        delay(250)
        val assistant = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.Assistant,
            content = "Echo: $content"
        )
        _messages.value = _messages.value + assistant
    }

    override suspend fun clearLocalSession() {
        _messages.value = emptyList()
    }
}

