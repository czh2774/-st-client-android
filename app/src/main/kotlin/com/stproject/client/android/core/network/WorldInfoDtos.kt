package com.stproject.client.android.core.network

data class WorldInfoEntryDto(
    val id: String,
    val userId: String? = null,
    val characterId: String? = null,
    val keys: List<String>? = null,
    val secondaryKeys: List<String>? = null,
    val content: String? = null,
    val comment: String? = null,
    val enabled: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class WorldInfoListResponseDto(
    val items: List<WorldInfoEntryDto>? = null,
)

data class WorldInfoUpsertRequestDto(
    val id: String? = null,
    val characterId: String? = null,
    val keys: List<String>,
    val secondaryKeys: List<String>? = null,
    val content: String,
    val comment: String? = null,
    val enabled: Boolean? = null,
)

data class WorldInfoDeleteResponseDto(
    val deleted: Boolean? = null,
)
