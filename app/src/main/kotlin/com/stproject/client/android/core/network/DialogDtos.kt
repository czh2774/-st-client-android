package com.stproject.client.android.core.network

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
    val metadata: Map<String, Any>? = null,
)

data class DialogStreamRequestDto(
    val messageId: String,
    val sessionId: String,
    val stream: Boolean = true,
    val persistMode: String = "sync",
    val worldInfoMinActivations: Int? = null,
    val worldInfoMinActivationsDepthMax: Int? = null,
)
