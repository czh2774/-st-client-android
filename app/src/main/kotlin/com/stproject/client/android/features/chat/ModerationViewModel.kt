package com.stproject.client.android.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentBlockReason
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.domain.repository.ReportRepository
import com.stproject.client.android.domain.usecase.BlockCharacterUseCase
import com.stproject.client.android.domain.usecase.GuardedActionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModerationViewModel
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
        private val chatSessionStore: ChatSessionStore,
        private val blockCharacterUseCase: BlockCharacterUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ModerationUiState())
        val uiState: StateFlow<ModerationUiState> = _uiState

        fun loadReasonsIfNeeded() {
            val state = _uiState.value
            if (state.isLoadingReasons) return
            if (state.reasons.isNotEmpty()) {
                _uiState.update { it.copy(error = null, lastReportSubmitted = false) }
                return
            }
            _uiState.update { it.copy(isLoadingReasons = true, error = null, lastReportSubmitted = false) }
            viewModelScope.launch {
                try {
                    val meta = reportRepository.getReasonMeta()
                    _uiState.update {
                        it.copy(
                            isLoadingReasons = false,
                            reasons = meta.reasons,
                            requiresDetailReasons = meta.requiresDetailReasons,
                            maxDetailLength = meta.maxDetailLength,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoadingReasons = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoadingReasons = false, error = "unexpected error") }
                }
            }
        }

        fun submitReport(
            reasons: List<String>,
            detail: String?,
        ) {
            submitReportInternal(
                reasons = reasons,
                detail = detail,
                targetId = null,
                sessionId = chatSessionStore.getSessionId(),
            )
        }

        fun submitReportForCharacter(
            targetId: String,
            reasons: List<String>,
            detail: String?,
        ) {
            submitReportInternal(reasons = reasons, detail = detail, targetId = targetId, sessionId = null)
        }

        fun blockDefaultCharacter() {
            blockCharacterInternal(targetId = null)
        }

        fun blockCharacter(targetId: String) {
            blockCharacterInternal(targetId = targetId)
        }

        private fun submitReportInternal(
            reasons: List<String>,
            detail: String?,
            targetId: String?,
            sessionId: String?,
        ) {
            val state = _uiState.value
            if (state.isSubmitting) return
            val resolvedTargetId = resolveTargetId(targetId)
            if (resolvedTargetId.isEmpty()) {
                _uiState.update { it.copy(error = "character not selected") }
                return
            }
            _uiState.update { it.copy(isSubmitting = true, error = null, lastReportSubmitted = false) }
            viewModelScope.launch {
                try {
                    reportRepository.submitReport(
                        targetId = resolvedTargetId,
                        reasons = reasons,
                        detail = detail,
                        sessionId = sessionId,
                    )
                    _uiState.update { it.copy(isSubmitting = false, lastReportSubmitted = true) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        private fun blockCharacterInternal(targetId: String?) {
            val state = _uiState.value
            if (state.isSubmitting) return
            val resolvedTargetId = resolveTargetId(targetId)
            if (resolvedTargetId.isEmpty()) {
                _uiState.update { it.copy(error = "character not selected") }
                return
            }
            _uiState.update { it.copy(isSubmitting = true, error = null, lastBlockSuccess = false) }
            viewModelScope.launch {
                try {
                    val result =
                        blockCharacterUseCase.execute(
                            characterId = resolvedTargetId,
                            isNsfwHint = false,
                            value = true,
                        )
                    if (result is GuardedActionResult.Blocked) {
                        _uiState.update {
                            it.copy(isSubmitting = false, error = accessErrorMessage(result.decision))
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(isSubmitting = false, lastBlockSuccess = true)
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
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

        private fun resolveTargetId(targetId: String?): String {
            val trimmed = targetId?.trim()?.takeIf { it.isNotEmpty() }
            if (trimmed != null) return trimmed
            return chatSessionStore.getPrimaryMemberId()?.trim().orEmpty()
        }
    }
