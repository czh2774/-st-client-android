package com.stproject.client.android.features.characters

import com.stproject.client.android.domain.model.CharacterDetail

data class CharacterDetailUiState(
    val isLoading: Boolean = false,
    val detail: CharacterDetail? = null,
    val shareUrl: String? = null,
    val error: String? = null,
)
