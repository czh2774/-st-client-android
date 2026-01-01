package com.stproject.client.android.features.creators

import com.stproject.client.android.domain.model.CreatorCharacter

data class CreatorCharactersUiState(
    val isLoading: Boolean = false,
    val items: List<CreatorCharacter> = emptyList(),
    val error: String? = null,
    val creatorId: String? = null,
    val hasMore: Boolean = false,
    val pageNum: Int = 1,
)
