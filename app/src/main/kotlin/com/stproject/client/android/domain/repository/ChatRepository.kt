package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun sendUserMessage(content: String)
}


