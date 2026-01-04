package com.stproject.client.android.core.network

import com.google.gson.annotations.SerializedName

data class DialogDeleteRequestDto(
    val messageId: String,
    val sessionId: String,
    val deleteAfter: Boolean,
)

data class DialogDeleteResponseDto(
    val deletedIds: List<String>? = null,
    val deletedCount: Int? = null,
)

data class DialogSwipeRequestDto(
    val messageId: String,
    val sessionId: String,
    val swipeId: Int,
)

data class DialogSwipeDeleteRequestDto(
    val messageId: String,
    val sessionId: String,
    val swipeId: Int? = null,
)

data class DialogSwipeResponseDto(
    val messageId: String,
    val swipeId: Int,
    val content: String,
    val swipes: List<String>? = null,
    val metadata: Map<String, Any?>? = null,
)

data class DialogStreamRequestDto(
    val messageId: String,
    val sessionId: String,
    val stream: Boolean = true,
    val persistMode: String = "sync",
    val worldInfoMinActivations: Int? = null,
    val worldInfoMinActivationsDepthMax: Int? = null,
    @SerializedName("global_variables")
    val globalVariables: Map<String, Any?>? = null,
)

data class DialogVariablesRequestDto(
    val messageId: String,
    val swipesData: List<Map<String, Any?>>,
    val swipesInfo: List<Map<String, Any?>>? = null,
)

data class DialogVariablesResponseDto(
    val messageId: String? = null,
)
