package com.stproject.client.android.features.profile

import com.stproject.client.android.domain.model.UserProfile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
)
