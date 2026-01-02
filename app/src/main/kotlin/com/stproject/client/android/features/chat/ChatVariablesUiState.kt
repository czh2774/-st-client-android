package com.stproject.client.android.features.chat

data class ChatVariablesUiState(
    val text: String = "{}",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val error: String? = null,
)
