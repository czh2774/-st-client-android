package com.stproject.client.android.core.network

data class DecorationItemDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val imageUrl: String? = null,
    val priceCredits: Int? = null,
    val owned: Boolean? = null,
    val equipped: Boolean? = null,
)

data class DecorationListResponseDto(
    val items: List<DecorationItemDto>? = null,
)

data class EquipDecorationRequestDto(
    val decorationId: String,
    val equip: Boolean,
)
