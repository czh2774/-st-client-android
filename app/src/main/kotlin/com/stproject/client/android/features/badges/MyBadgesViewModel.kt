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

data class MyBadgesUiState(
    val items: List<FanBadge> = emptyList(),
    val isLoading: Boolean = false,
    val isEquipping: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MyBadgesViewModel
    @Inject
    constructor(
        private val repository: FanBadgeRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MyBadgesUiState())
        val uiState: StateFlow<MyBadgesUiState> = _uiState

        fun load(force: Boolean = false) {
            val state = _uiState.value
            if (state.isLoading && !force) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    val result = repository.listPurchasedBadges()
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

        fun toggleEquip(
            badgeId: String,
            equip: Boolean,
        ) {
            if (_uiState.value.isEquipping) return
            _uiState.update { it.copy(isEquipping = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.equipBadge(badgeId, equip)
                    _uiState.update { it.copy(isEquipping = false) }
                    load(force = true)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isEquipping = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isEquipping = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun ensureAccess(): Boolean {
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEquipping = false,
                        items = emptyList(),
                        error = access.userMessage(),
                    )
                }
                return false
            }
            return true
        }
    }
