package com.stproject.client.android.features.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsListViewModel
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        private val characterRepository: CharacterRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatsListUiState())
        val uiState: StateFlow<ChatsListUiState> = _uiState
        private val memberMetadataCache = mutableMapOf<String, MemberMetadata>()

        fun load(
            allowNsfw: Boolean,
            blockedTags: List<String> = emptyList(),
        ) {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
                    if (access is ContentAccessDecision.Blocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                lastSession = null,
                                error = access.userMessage(),
                            )
                        }
                        return@launch
                    }
                    val items = chatRepository.listSessions(limit = 20, offset = 0)
                    val shouldResolveDetails = !allowNsfw || blockedTags.isNotEmpty()
                    val resolved =
                        if (shouldResolveDetails) {
                            resolveMemberMetadata(items)
                        } else {
                            items
                        }
                    val lastSession = chatRepository.getLastSessionSummary()
                    val resolvedLast =
                        if (lastSession == null) {
                            null
                        } else if (!shouldResolveDetails) {
                            lastSession
                        } else {
                            resolveMemberMetadata(listOf(lastSession)).firstOrNull()
                        }
                    val displayLast =
                        if (resolvedLast != null && resolved.none { it.sessionId == resolvedLast.sessionId }) {
                            resolvedLast
                        } else {
                            null
                        }
                    _uiState.update { it.copy(isLoading = false, items = resolved, lastSession = displayLast) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun resolveMemberMetadata(items: List<ChatSessionSummary>): List<ChatSessionSummary> {
            return coroutineScope {
                val memberIds =
                    items.mapNotNull { item ->
                        item.primaryMemberId?.trim()?.takeIf { it.isNotEmpty() }
                    }.distinct()
                val missingIds = memberIds.filterNot { memberMetadataCache.containsKey(it) }
                val deferredDetails =
                    missingIds.associateWith { memberId ->
                        async {
                            runCatching { characterRepository.getCharacterDetail(memberId) }.getOrNull()
                        }
                    }
                deferredDetails.forEach { (memberId, deferred) ->
                    val detail = deferred.await() ?: return@forEach
                    memberMetadataCache[memberId] = detail.toMetadata()
                }
                items.map { item ->
                    val memberId = item.primaryMemberId?.trim().orEmpty()
                    if (memberId.isEmpty()) return@map item
                    val metadata = memberMetadataCache[memberId] ?: return@map item
                    item.copy(
                        primaryMemberIsNsfw = metadata.isNsfw,
                        primaryMemberAgeRating = metadata.ageRating,
                        primaryMemberTags = metadata.tags,
                    )
                }
            }
        }

        private fun CharacterDetail.toMetadata(): MemberMetadata {
            return MemberMetadata(
                isNsfw = isNsfw,
                ageRating = moderationAgeRating,
                tags = tags,
            )
        }

        private data class MemberMetadata(
            val isNsfw: Boolean?,
            val ageRating: com.stproject.client.android.domain.model.AgeRating?,
            val tags: List<String>,
        )
    }
