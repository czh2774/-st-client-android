package com.stproject.client.android.core.network

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequestDto(
    val message: String,
    val stream: Boolean? = null,
    val worldInfoMinActivations: Int? = null,
    val worldInfoMinActivationsDepthMax: Int? = null,
    val clientMessageId: String? = null,
    val clientAssistantMessageId: String? = null,
    @SerializedName("latest_variables")
    val latestVariables: Map<String, Any?>? = null,
    @SerializedName("global_variables")
    val globalVariables: Map<String, Any?>? = null,
)

data class ChatCompletionResponseDto(
    val content: String,
    val finishReason: String,
)
