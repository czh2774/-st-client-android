package com.stproject.client.android.features.chat

enum class VariablesScope {
    Session,
    Global,
    Preset,
    Character,
    Message,
}

data class VariablesEditorState(
    val text: String = "{}",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val error: String? = null,
)

data class ChatVariablesUiState(
    val activeScope: VariablesScope = VariablesScope.Session,
    val session: VariablesEditorState = VariablesEditorState(),
    val global: VariablesEditorState = VariablesEditorState(),
    val preset: VariablesEditorState = VariablesEditorState(),
    val character: VariablesEditorState = VariablesEditorState(),
    val message: VariablesEditorState = VariablesEditorState(),
    val selectedMessageId: String? = null,
    val presetId: String? = null,
    val characterId: String? = null,
)
