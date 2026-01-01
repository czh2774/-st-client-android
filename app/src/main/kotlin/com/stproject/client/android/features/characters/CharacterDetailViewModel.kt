package com.stproject.client.android.features.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterDetailViewModel
    @Inject
    constructor(
        private val characterRepository: CharacterRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CharacterDetailUiState())
        val uiState: StateFlow<CharacterDetailUiState> = _uiState

        fun load(characterId: String) {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null, shareUrl = null) }
            viewModelScope.launch {
                try {
                    val detail = characterRepository.getCharacterDetail(characterId)
                    val access =
                        resolveContentAccess.execute(
                            memberId = characterId,
                            isNsfwHint = detail.isNsfw,
                        )
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = accessErrorMessage(access),
                                detail = null,
                            )
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isLoading = false, detail = detail) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun generateShareCode(characterId: String) {
            viewModelScope.launch {
                try {
                    val access =
                        resolveContentAccess.execute(
                            memberId = characterId,
                            isNsfwHint = _uiState.value.detail?.isNsfw,
                        )
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(error = accessErrorMessage(access)) }
                        return@launch
                    }
                    val info = characterRepository.generateShareCode(characterId)
                    _uiState.update { it.copy(shareUrl = info?.shareUrl ?: info?.shareCode) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
                }
            }
        }

        fun followCharacter(
            characterId: String,
            value: Boolean,
        ) {
            val cleanId = characterId.trim()
            if (cleanId.isEmpty()) return
            viewModelScope.launch {
                try {
                    val result = characterRepository.followCharacter(cleanId, value)
                    _uiState.update { state ->
                        val detail = state.detail
                        if (detail == null) {
                            state
                        } else {
                            state.copy(
                                detail =
                                    detail.copy(
                                        totalFollowers = result.totalFollowers,
                                        isFollowed = result.isFollowed,
                                    ),
                            )
                        }
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
                }
            }
        }

        private fun accessErrorMessage(access: ContentAccessDecision.Blocked): String {
            return when (access.reason) {
                ContentBlockReason.NSFW_DISABLED -> "mature content disabled"
                ContentBlockReason.AGE_REQUIRED -> "age verification required"
                ContentBlockReason.CONSENT_REQUIRED -> "terms acceptance required"
                ContentBlockReason.CONSENT_PENDING -> "compliance not loaded"
            }
        }
    }
