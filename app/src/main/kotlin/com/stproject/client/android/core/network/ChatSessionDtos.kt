package com.stproject.client.android.core.network

data class CreateChatSessionRequestDto(
    val members: List<String>,
    val greetingIndex: Int? = null,
    val clientSessionId: String? = null,
    val title: String? = null,
    val presetId: String? = null,
    val shareCode: String? = null,
)

data class CreateChatSessionResponseDto(
    val sessionId: String,
)

data class ChatSessionItemDto(
    val sessionId: String,
    val primaryMemberId: String? = null,
    val primaryMemberName: String? = null,
    val primaryMemberAvatarUrl: String? = null,
    val characterName: String? = null,
    val characterAvatarUrl: String? = null,
    val title: String? = null,
    val lastMessageAt: String? = null,
    val updatedAt: String? = null,
)

data class ChatSessionDetailDto(
    val sessionId: String,
    val metadata: Map<String, Any?>? = null,
    val updatedAt: String? = null,
)

data class ChatSessionsResponseDto(
    val items: List<ChatSessionItemDto>? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
)

data class UpdateChatSessionRequestDto(
    val title: String? = null,
    val metadata: ChatSessionMetadataPatchDto? = null,
)

data class ChatSessionMetadataPatchDto(
    val xbVars: Map<String, Any?>? = null,
)
