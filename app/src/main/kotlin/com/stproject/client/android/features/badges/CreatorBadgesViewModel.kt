package com.stproject.client.android.features.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.FanBadge
import com.stproject.client.android.domain.repository.FanBadgeRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatorBadgesUiState(
    val creatorId: String? = null,
    val items: List<FanBadge> = emptyList(),
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CreatorBadgesViewModel
    @Inject
    constructor(
        private val repository: FanBadgeRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreatorBadgesUiState())
        val uiState: StateFlow<CreatorBadgesUiState> = _uiState

        fun load(
            creatorId: String,
            force: Boolean = false,
        ) {
            val cleanId = creatorId.trim()
            if (cleanId.isEmpty()) return
            val state = _uiState.value
            if (state.isLoading && !force && state.creatorId == cleanId) return
            _uiState.update { it.copy(isLoading = true, error = null, creatorId = cleanId) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    val result = repository.listCreatorBadges(cleanId, pageNum = 1, pageSize = 50)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
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

        fun purchaseBadge(badgeId: String) {
            if (_uiState.value.isPurchasing) return
            _uiState.update { it.copy(isPurchasing = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.purchaseBadge(badgeId)
                    _uiState.update { it.copy(isPurchasing = false) }
                    val creatorId = _uiState.value.creatorId
                    if (!creatorId.isNullOrBlank()) {
                        load(creatorId, force = true)
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isPurchasing = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isPurchasing = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun ensureAccess(): Boolean {
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPurchasing = false,
                        items = emptyList(),
                        error = access.userMessage(),
                    )
                }
                return false
            }
            return true
        }
    }
