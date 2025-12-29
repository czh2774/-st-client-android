package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.network.ChatCompletionRequestDto
import com.stproject.client.android.core.network.StApi
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole
import com.stproject.client.android.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpChatRepository @Inject constructor(
    private val api: StApi,
    private val apiClient: ApiClient
) : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.System,
                content = "Connected API base: v1. Completion endpoint wired (non-stream stub)."
            )
        )
    )

    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    // TODO: replace with real session creation + persistence.
    private val sessionId = "s1"

    override suspend fun sendUserMessage(content: String) {
        val user = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.User,
            content = content
        )
        _messages.value = _messages.value + user

        try {
            val resp = apiClient.call {
                api.createChatCompletion(
                    sessionId = sessionId,
                    request = ChatCompletionRequestDto(
                        message = content,
                        stream = false,
                        worldInfoMinActivations = 2,
                        worldInfoMinActivationsDepthMax = 10
                    )
                )
            }
            val assistant = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.Assistant,
                content = resp.content
            )
            _messages.value = _messages.value + assistant
        } catch (e: ApiException) {
            val details = listOfNotNull(
                e.errorDetailCode?.let { "errorDetail.code=$it" },
                e.httpStatus?.let { "http=$it" },
                e.apiCode?.let { "apiCode=$it" }
            ).joinToString(", ")
            val sys = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.System,
                content = if (details.isBlank()) "Request failed: ${e.message}" else "Request failed: ${e.message} ($details)"
            )
            _messages.value = _messages.value + sys
        }
    }
}


