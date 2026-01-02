package com.stproject.client.android.features.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.usecase.FollowCharacterUseCase
import com.stproject.client.android.domain.usecase.GuardedActionResult
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
        private val followCharacterUseCase: FollowCharacterUseCase,
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
                            ageRatingHint = detail.moderationAgeRating,
                        )
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = access.userMessage(),
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
                            ageRatingHint = _uiState.value.detail?.moderationAgeRating,
                        )
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update { it.copy(error = access.userMessage()) }
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
                    val result =
                        followCharacterUseCase.execute(
                            cleanId,
                            _uiState.value.detail?.isNsfw,
                            value,
                            ageRatingHint = _uiState.value.detail?.moderationAgeRating,
                        )
                    if (result is GuardedActionResult.Blocked) {
                        _uiState.update { it.copy(error = result.decision.userMessage()) }
                        return@launch
                    }
                    _uiState.update { state ->
                        val detail = state.detail
                        if (detail == null) {
                            state
                        } else {
                            val followResult = (result as GuardedActionResult.Allowed).value
                            state.copy(
                                detail =
                                    detail.copy(
                                        totalFollowers = followResult.totalFollowers,
                                        isFollowed = followResult.isFollowed,
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
    }
