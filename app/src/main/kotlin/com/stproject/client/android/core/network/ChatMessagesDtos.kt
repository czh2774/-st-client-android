package com.stproject.client.android.core.network

data class ChatMessageDto(
    val messageId: String? = null,
    val id: String? = null,
    val sessionId: String? = null,
    val role: String? = null,
    val content: String? = null,
    val createdAt: String? = null,
    val metadata: Map<String, Any>? = null,
    val swipes: List<String>? = null,
    val swipeId: Int? = null,
)

data class ChatMessagesResponseDto(
    val items: List<ChatMessageDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
)
