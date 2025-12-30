package com.stproject.client.android.core.network

data class CreateChatSessionRequestDto(
    val members: List<String>,
    val greetingIndex: Int? = null,
    val clientSessionId: String? = null,
    val title: String? = null,
    val presetId: String? = null,
    val shareCode: String? = null
)

data class CreateChatSessionResponseDto(
    val sessionId: String
)
