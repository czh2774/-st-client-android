package com.stproject.client.android.features.explore

import android.net.Uri
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
class ExploreViewModel
    @Inject
    constructor(
        private val characterRepository: CharacterRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
        private val followCharacterUseCase: FollowCharacterUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExploreUiState())
        val uiState: StateFlow<ExploreUiState> = _uiState
        private var allowNsfw = false

        fun load(force: Boolean = false) {
            if (_uiState.value.isLoading && !force) return
            _uiState.update { it.copy(isLoading = true, error = null, accessError = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                accessError = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val state = _uiState.value
                    val items =
                        characterRepository.queryCharactersFiltered(
                            cursor = null,
                            limit = 20,
                            sortBy = state.sortBy,
                            isNsfw = if (allowNsfw) null else false,
                            tags = parseTags(state.tagsInput),
                            searchKeyword = state.searchKeyword,
                            gender = null,
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

        fun setNsfwAllowed(allow: Boolean) {
            allowNsfw = allow
        }

        fun onSearchChanged(value: String) {
            _uiState.update { it.copy(searchKeyword = value) }
        }

        fun onTagsChanged(value: String) {
            _uiState.update { it.copy(tagsInput = value) }
        }

        fun setSortBy(sortBy: String) {
            val normalized = sortBy.trim()
            if (normalized.isEmpty()) return
            if (normalized == _uiState.value.sortBy) return
            _uiState.update { it.copy(sortBy = normalized) }
            load(force = true)
        }

        fun applyFilters() {
            load(force = true)
        }

        fun clearFilters() {
            _uiState.update {
                it.copy(
                    sortBy = "homepage",
                    searchKeyword = "",
                    tagsInput = "",
                )
            }
            load(force = true)
        }

        fun followCharacter(
            characterId: String,
            value: Boolean,
        ) {
            val cleanId = characterId.trim()
            if (cleanId.isEmpty()) return
            viewModelScope.launch {
                try {
                    val item = _uiState.value.items.firstOrNull { it.id == cleanId }
                    val result =
                        followCharacterUseCase.execute(
                            cleanId,
                            item?.isNsfw,
                            value,
                            ageRatingHint = item?.moderationAgeRating,
                        )
                    if (result is GuardedActionResult.Blocked) {
                        _uiState.update { it.copy(error = result.decision.userMessage()) }
                        return@launch
                    }
                    _uiState.update { state ->
                        val followResult = (result as GuardedActionResult.Allowed).value
                        state.copy(
                            items =
                                state.items.map { item ->
                                    if (item.id == cleanId) {
                                        item.copy(
                                            totalFollowers = followResult.totalFollowers,
                                            isFollowed = followResult.isFollowed,
                                        )
                                    } else {
                                        item
                                    }
                                },
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
                }
            }
        }

        fun onShareCodeChanged(value: String) {
            _uiState.update { it.copy(shareCodeInput = value, shareCodeError = null) }
        }

        fun resolveShareCode() {
            val state = _uiState.value
            if (state.isResolvingShareCode) return
            val normalized = normalizeShareCode(state.shareCodeInput)
            if (normalized.isNullOrEmpty()) {
                _uiState.update { it.copy(shareCodeError = "Share code is required.") }
                return
            }
            _uiState.update { it.copy(isResolvingShareCode = true, shareCodeError = null, resolvedTags = null) }
            viewModelScope.launch {
                try {
                    val memberId = characterRepository.resolveShareCode(normalized)
                    if (memberId.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                isResolvingShareCode = false,
                                shareCodeError = "Share code not found.",
                                resolvedTags = null,
                            )
                        }
                        return@launch
                    }
                    val detail = runCatching { characterRepository.getCharacterDetail(memberId) }.getOrNull()
                    val resolvedIsNsfw = detail?.isNsfw
                    val resolvedAgeRating = detail?.moderationAgeRating
                    val resolvedTags = detail?.tags
                    val access =
                        resolveContentAccess.execute(
                            memberId = memberId,
                            isNsfwHint = resolvedIsNsfw,
                            ageRatingHint = resolvedAgeRating,
                            tags = resolvedTags,
                            requireMetadata = true,
                        )
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isResolvingShareCode = false,
                                resolvedMemberId = null,
                                resolvedShareCode = null,
                                resolvedIsNsfw = null,
                                resolvedAgeRating = null,
                                resolvedTags = null,
                                accessError = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isResolvingShareCode = false,
                            resolvedMemberId = memberId,
                            resolvedShareCode = normalized,
                            resolvedIsNsfw = resolvedIsNsfw,
                            resolvedAgeRating = resolvedAgeRating,
                            resolvedTags = resolvedTags,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update {
                        it.copy(
                            isResolvingShareCode = false,
                            shareCodeError = e.userMessage ?: e.message,
                            resolvedTags = null,
                        )
                    }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update {
                        it.copy(
                            isResolvingShareCode = false,
                            shareCodeError = "unexpected error",
                            resolvedTags = null,
                        )
                    }
                }
            }
        }

        fun consumeResolvedShareCode() {
            _uiState.update {
                it.copy(
                    resolvedMemberId = null,
                    resolvedShareCode = null,
                    resolvedIsNsfw = null,
                    resolvedAgeRating = null,
                    resolvedTags = null,
                )
            }
        }

        private fun normalizeShareCode(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
            val queryCode = parsed?.getQueryParameter("shareCode")?.trim()
            return if (!queryCode.isNullOrEmpty()) queryCode else trimmed
        }

        private fun parseTags(raw: String): List<String>? {
            val cleaned =
                raw.split(",", "\n", "\t", " ")
                    .mapNotNull { it.trim().takeIf { part -> part.isNotEmpty() } }
                    .distinct()
            return cleaned.takeIf { it.isNotEmpty() }
        }
    }
