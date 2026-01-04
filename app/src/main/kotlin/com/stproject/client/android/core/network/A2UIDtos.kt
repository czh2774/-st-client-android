package com.stproject.client.android.core.network

import com.stproject.client.android.core.a2ui.A2UIClientCapabilities

data class A2UIUserActionDto(
    val name: String,
    val surfaceId: String? = null,
    val sourceComponentId: String? = null,
    val timestamp: String? = null,
    val context: Map<String, Any?>? = null,
)

data class A2UIMessageMetadataDto(
    val a2uiClientCapabilities: A2UIClientCapabilities? = null,
)

data class A2UIErrorDto(
    val type: String? = null,
    val message: String? = null,
    val surfaceId: String? = null,
    val componentId: String? = null,
    val details: Map<String, Any?>? = null,
)

data class A2UIEventRequestDto(
    val userAction: A2UIUserActionDto? = null,
    val error: A2UIErrorDto? = null,
    val metadata: A2UIMessageMetadataDto? = null,
)

data class A2UIEventResponseDto(
    val accepted: Boolean,
    val reason: String? = null,
)
