package com.stproject.client.android.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.domain.model.UserConfigUpdate
import com.stproject.client.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplianceViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val userPreferencesStore: UserPreferencesStore,
        private val contentAccessManager: ContentAccessManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ComplianceUiState())
        val uiState: StateFlow<ComplianceUiState> = _uiState

        fun load() {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, error = null, accountDeleted = false) }
            viewModelScope.launch {
                try {
                    val me = userRepository.getMe()
                    val config = userRepository.getUserConfig()
                    val tosAccepted = normalizeTosAcceptedAt(me.tosAcceptedAt)
                    val prefAllowNsfw = userPreferencesStore.isNsfwAllowed()
                    val blockedTags = normalizeTags(config.blockedTags)
                    val blockedNsfw = hasBlockedNsfw(blockedTags)
                    val allowNsfw = prefAllowNsfw && config.ageVerified && !blockedNsfw
                    if (allowNsfw != prefAllowNsfw) {
                        userPreferencesStore.setNsfwAllowed(allowNsfw)
                    }
                    val themeMode = userPreferencesStore.getThemeMode()
                    val languageTag = userPreferencesStore.getLanguageTag()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            consentLoaded = true,
                            consentRequired = tosAccepted == null,
                            tosAcceptedAt = tosAccepted,
                            ageVerified = config.ageVerified,
                            birthDate = config.birthDate,
                            blockedTags = blockedTags,
                            allowNsfw = allowNsfw,
                            themeMode = themeMode,
                            languageTag = languageTag,
                        )
                    }
                    syncAccessGate()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun acceptTos() {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    val me = userRepository.acceptTos()
                    val tosAccepted = normalizeTosAcceptedAt(me.tosAcceptedAt)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            consentRequired = tosAccepted == null,
                            tosAcceptedAt = tosAccepted,
                        )
                    }
                    syncAccessGate()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        fun verifyAge(birthDate: String) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    val config =
                        userRepository.updateUserConfig(
                            UserConfigUpdate(ageVerified = true, birthDate = birthDate),
                        )
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            ageVerified = config.ageVerified,
                            birthDate = config.birthDate,
                        )
                    }
                    syncAccessGate()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        fun deleteAccount() {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    userRepository.deleteMe()
                    _uiState.update { it.copy(isSubmitting = false, accountDeleted = true) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSubmitting = false, error = "unexpected error") }
                }
            }
        }

        fun setAllowNsfw(allow: Boolean) {
            if (_uiState.value.isSubmitting) return
            if (allow && !_uiState.value.ageVerified) {
                _uiState.update { it.copy(error = "age verification required") }
                return
            }
            userPreferencesStore.setNsfwAllowed(allow)
            val nextBlocked = updateBlockedTags(_uiState.value.blockedTags, allow)
            _uiState.update { it.copy(allowNsfw = allow, blockedTags = nextBlocked, error = null) }
            syncAccessGate()
            viewModelScope.launch {
                try {
                    userRepository.updateUserConfig(UserConfigUpdate(blockedTags = nextBlocked))
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(error = "unexpected error") }
                }
            }
        }

        fun reset() {
            _uiState.value =
                ComplianceUiState(
                    allowNsfw = userPreferencesStore.isNsfwAllowed(),
                    themeMode = userPreferencesStore.getThemeMode(),
                    languageTag = userPreferencesStore.getLanguageTag(),
                )
            syncAccessGate()
        }

        fun setThemeMode(mode: com.stproject.client.android.core.theme.ThemeMode) {
            userPreferencesStore.setThemeMode(mode)
            _uiState.update { it.copy(themeMode = mode) }
        }

        fun setLanguageTag(tag: String?) {
            userPreferencesStore.setLanguageTag(tag)
            _uiState.update { it.copy(languageTag = tag) }
        }

        private fun normalizeTosAcceptedAt(raw: String?): String? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (trimmed.startsWith("0001-")) return null
            return trimmed
        }

        private fun syncAccessGate() {
            contentAccessManager.updateGate(ContentGate.from(_uiState.value))
        }

        private fun normalizeTags(tags: List<String>): List<String> =
            tags.mapNotNull { tag -> tag.trim().takeIf { it.isNotEmpty() } }

        private fun hasBlockedNsfw(tags: List<String>): Boolean = tags.any { it.equals("nsfw", ignoreCase = true) }

        private fun updateBlockedTags(
            tags: List<String>,
            allowNsfw: Boolean,
        ): List<String> {
            val normalized = normalizeTags(tags)
            return if (allowNsfw) {
                normalized.filterNot { it.equals("nsfw", ignoreCase = true) }
            } else {
                if (normalized.any { it.equals("nsfw", ignoreCase = true) }) {
                    normalized
                } else {
                    normalized + "nsfw"
                }
            }
        }
    }
