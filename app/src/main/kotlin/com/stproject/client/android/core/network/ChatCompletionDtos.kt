package com.stproject.client.android.core.network

data class ChatCompletionRequestDto(
    val message: String,
    val stream: Boolean? = null,
    val worldInfoMinActivations: Int? = null,
    val worldInfoMinActivationsDepthMax: Int? = null
)

data class ChatCompletionResponseDto(
    val content: String,
    val finishReason: String
)


