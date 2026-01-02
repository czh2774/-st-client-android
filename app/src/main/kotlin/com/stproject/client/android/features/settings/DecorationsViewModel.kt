package com.stproject.client.android.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.DecorationItem
import com.stproject.client.android.domain.repository.DecorationRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DecorationsUiState(
    val items: List<DecorationItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DecorationsViewModel
    @Inject
    constructor(
        private val repository: DecorationRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DecorationsUiState())
        val uiState: StateFlow<DecorationsUiState> = _uiState

        fun load() {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    val items = repository.listDecorations()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = items,
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

        fun setEquipped(
            decorationId: String,
            equip: Boolean,
        ) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.setDecorationEquipped(decorationId = decorationId, equip = equip)
                    _uiState.update { it.copy(isSubmitting = false) }
                    load()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun ensureAccess(): Boolean {
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSubmitting = false,
                        items = emptyList(),
                        error = access.userMessage(),
                    )
                }
                return false
            }
            return true
        }
    }
