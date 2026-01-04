package com.stproject.client.android.features.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.network.AppError
import com.stproject.client.android.core.network.toAppError
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatShareUiState(
    val shareCode: String? = null,
    val resolvedMemberId: String? = null,
    val resolvedTags: List<String>? = null,
    val isResolving: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class ChatShareViewModel
    @Inject
    constructor(
        private val characterRepository: CharacterRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatShareUiState())
        val uiState: StateFlow<ChatShareUiState> = _uiState

        fun resolveShareCode(rawCode: String?) {
            val normalized = normalizeShareCode(rawCode)
            if (normalized.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isResolving = false,
                        error = AppError.Validation("Share code is required."),
                        resolvedMemberId = null,
                        resolvedTags = null,
                    )
                }
                return
            }
            if (_uiState.value.isResolving && _uiState.value.shareCode == normalized) return
            _uiState.update {
                it.copy(
                    shareCode = normalized,
                    resolvedMemberId = null,
                    resolvedTags = null,
                    isResolving = true,
                    error = null,
                )
            }
            viewModelScope.launch {
                try {
                    val memberId = characterRepository.resolveShareCode(normalized)
                    if (memberId.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                isResolving = false,
                                error = AppError.Validation("Share code not found."),
                                resolvedTags = null,
                            )
                        }
                        return@launch
                    }
                    val detail = runCatching { characterRepository.getCharacterDetail(memberId) }.getOrNull()
                    val access =
                        resolveContentAccess.execute(
                            memberId,
                            detail?.isNsfw,
                            ageRatingHint = detail?.moderationAgeRating,
                            tags = detail?.tags,
                            requireMetadata = true,
                        )
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isResolving = false,
                                error = AppError.Validation(access.userMessage()),
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isResolving = false,
                            resolvedMemberId = memberId,
                            resolvedTags = detail?.tags,
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update {
                        it.copy(
                            isResolving = false,
                            error = e.toAppError(),
                            resolvedTags = null,
                        )
                    }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update {
                        it.copy(
                            isResolving = false,
                            error = AppError.Unknown("unexpected error"),
                            resolvedTags = null,
                        )
                    }
                }
            }
        }

        fun consumeResolvedMemberId() {
            _uiState.update { it.copy(resolvedMemberId = null, resolvedTags = null) }
        }

        private fun normalizeShareCode(raw: String?): String? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
            val queryCode = parsed?.getQueryParameter("shareCode")?.trim()
            return if (!queryCode.isNullOrEmpty()) queryCode else trimmed
        }
    }
