package com.stproject.client.android.features.worldinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.WorldInfoEntry
import com.stproject.client.android.domain.model.WorldInfoEntryInput
import com.stproject.client.android.domain.repository.WorldInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorldInfoEditorState(
    val id: String? = null,
    val characterId: String = "",
    val keys: String = "",
    val content: String = "",
    val comment: String = "",
    val enabled: Boolean = true,
) {
    val isEditing: Boolean = id != null
}

data class WorldInfoUiState(
    val items: List<WorldInfoEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val scopeCharacterId: String = "",
    val includeGlobal: Boolean = false,
    val editor: WorldInfoEditorState = WorldInfoEditorState(),
)

@HiltViewModel
class WorldInfoViewModel
    @Inject
    constructor(
        private val repository: WorldInfoRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(WorldInfoUiState())
        val uiState: StateFlow<WorldInfoUiState> = _uiState

        fun load(force: Boolean = false) {
            if (_uiState.value.isLoading && !force) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val state = _uiState.value
                    val items =
                        repository.listEntries(
                            characterId = state.scopeCharacterId.trim().takeIf { it.isNotEmpty() },
                            includeGlobal = state.includeGlobal,
                        )
                    _uiState.update { it.copy(isLoading = false, items = items) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun setScopeCharacterId(value: String) {
            _uiState.update { it.copy(scopeCharacterId = value) }
        }

        fun setIncludeGlobal(value: Boolean) {
            _uiState.update { it.copy(includeGlobal = value) }
        }

        fun applyScope() {
            load(force = true)
        }

        fun selectEntry(entry: WorldInfoEntry) {
            _uiState.update {
                it.copy(
                    editor =
                        WorldInfoEditorState(
                            id = entry.id,
                            characterId = entry.characterId.orEmpty(),
                            keys = entry.keys.joinToString(", "),
                            content = entry.content,
                            comment = entry.comment.orEmpty(),
                            enabled = entry.enabled,
                        ),
                )
            }
        }

        fun clearEditor() {
            _uiState.update { state ->
                val nextCharacterId =
                    state.scopeCharacterId.trim().takeIf { it.isNotEmpty() }
                        ?: state.editor.characterId.trim().takeIf { it.isNotEmpty() }
                state.copy(
                    editor =
                        WorldInfoEditorState(
                            characterId = nextCharacterId.orEmpty(),
                            enabled = true,
                        ),
                )
            }
        }

        fun updateEditorCharacterId(value: String) {
            _uiState.update { it.copy(editor = it.editor.copy(characterId = value)) }
        }

        fun updateEditorKeys(value: String) {
            _uiState.update { it.copy(editor = it.editor.copy(keys = value)) }
        }

        fun updateEditorContent(value: String) {
            _uiState.update { it.copy(editor = it.editor.copy(content = value)) }
        }

        fun updateEditorComment(value: String) {
            _uiState.update { it.copy(editor = it.editor.copy(comment = value)) }
        }

        fun updateEditorEnabled(value: Boolean) {
            _uiState.update { it.copy(editor = it.editor.copy(enabled = value)) }
        }

        fun submitEntry() {
            val state = _uiState.value
            val editor = state.editor
            val keys = parseKeys(editor.keys)
            val content = editor.content.trim()
            if (keys.isEmpty()) {
                _uiState.update { it.copy(error = "keys required") }
                return
            }
            if (content.isEmpty()) {
                _uiState.update { it.copy(error = "content required") }
                return
            }
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    val input =
                        WorldInfoEntryInput(
                            characterId = editor.characterId.trim().takeIf { it.isNotEmpty() },
                            keys = keys,
                            content = content,
                            comment = editor.comment.trim().takeIf { it.isNotEmpty() },
                            enabled = editor.enabled,
                        )
                    if (editor.isEditing) {
                        repository.updateEntry(editor.id.orEmpty(), input)
                    } else {
                        repository.createEntry(input)
                    }
                    clearEditor()
                    load(force = true)
                    _uiState.update { it.copy(isSubmitting = false) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        fun deleteEntry(entryId: String) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    repository.deleteEntry(entryId)
                    load(force = true)
                    _uiState.update { it.copy(isSubmitting = false) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        private fun parseKeys(raw: String): List<String> {
            return raw.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
