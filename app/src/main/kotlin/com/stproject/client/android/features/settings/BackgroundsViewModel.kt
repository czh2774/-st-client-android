package com.stproject.client.android.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.BackgroundConfig
import com.stproject.client.android.domain.model.BackgroundItem
import com.stproject.client.android.domain.repository.BackgroundRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackgroundsUiState(
    val items: List<BackgroundItem> = emptyList(),
    val config: BackgroundConfig? = null,
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BackgroundsViewModel
    @Inject
    constructor(
        private val repository: BackgroundRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BackgroundsUiState())
        val uiState: StateFlow<BackgroundsUiState> = _uiState

        fun load() {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    val result = repository.listBackgrounds()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
                            config = result.config,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun upload(
            fileName: String,
            bytes: ByteArray,
        ) {
            if (_uiState.value.isUploading) return
            _uiState.update { it.copy(isUploading = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.uploadBackground(fileName, bytes)
                    _uiState.update { it.copy(isUploading = false) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isUploading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isUploading = false, error = "unexpected error") }
                }
            }
        }

        fun rename(
            oldName: String,
            newName: String,
        ) {
            if (_uiState.value.isRenaming) return
            _uiState.update { it.copy(isRenaming = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.renameBackground(oldName, newName)
                    _uiState.update { it.copy(isRenaming = false) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isRenaming = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isRenaming = false, error = "unexpected error") }
                }
            }
        }

        fun delete(name: String) {
            if (_uiState.value.isDeleting) return
            _uiState.update { it.copy(isDeleting = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.deleteBackground(name)
                    _uiState.update { it.copy(isDeleting = false) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isDeleting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isDeleting = false, error = "unexpected error") }
                }
            }
        }

        fun setError(message: String?) {
            _uiState.update { it.copy(error = message) }
        }

        private suspend fun ensureAccess(): Boolean {
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUploading = false,
                        isRenaming = false,
                        isDeleting = false,
                        items = emptyList(),
                        config = null,
                        error = access.userMessage(),
                    )
                }
                return false
            }
            return true
        }
    }
