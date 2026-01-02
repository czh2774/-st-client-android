package com.stproject.client.android.core.network

data class A2UIUserActionDto(
    val name: String,
    val surfaceId: String? = null,
    val sourceComponentId: String? = null,
    val timestamp: String? = null,
    val context: Map<String, Any?>? = null,
)

data class A2UIEventRequestDto(
    val userAction: A2UIUserActionDto,
)

data class A2UIEventResponseDto(
    val accepted: Boolean,
    val reason: String? = null,
)
