package com.stproject.client.android.domain.model

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String
)


