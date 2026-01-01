package com.stproject.client.android.features.chat

data class ModerationUiState(
    val isLoadingReasons: Boolean = false,
    val isSubmitting: Boolean = false,
    val reasons: List<String> = emptyList(),
    val requiresDetailReasons: List<String> = emptyList(),
    val maxDetailLength: Int = 1000,
    val error: String? = null,
    val lastReportSubmitted: Boolean = false,
    val lastBlockSuccess: Boolean = false,
)
