package com.stproject.client.android.core.network

data class CardCreateResponseDto(
    val characterId: String? = null,
    val name: String? = null,
)

data class CardParseTextRequestDto(
    val content: String,
    val fileName: String? = null,
)

data class CardParseResponseDto(
    val success: Boolean? = null,
    val card: Map<String, Any>? = null,
    val error: String? = null,
)
