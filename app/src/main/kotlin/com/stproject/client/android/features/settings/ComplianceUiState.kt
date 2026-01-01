package com.stproject.client.android.features.settings

import com.stproject.client.android.core.theme.ThemeMode

data class ComplianceUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val consentLoaded: Boolean = false,
    val consentRequired: Boolean = false,
    val tosAcceptedAt: String? = null,
    val ageVerified: Boolean = false,
    val birthDate: String? = null,
    val blockedTags: List<String> = emptyList(),
    val allowNsfw: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val languageTag: String? = null,
    val error: String? = null,
    val accountDeleted: Boolean = false,
)
