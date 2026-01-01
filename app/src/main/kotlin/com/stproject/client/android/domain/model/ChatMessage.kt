package com.stproject.client.android.domain.model

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val serverId: String? = null,
    val createdAt: Long? = null,
    val isStreaming: Boolean = false,
    val swipes: List<String> = emptyList(),
    val swipeId: Int? = null,
)
