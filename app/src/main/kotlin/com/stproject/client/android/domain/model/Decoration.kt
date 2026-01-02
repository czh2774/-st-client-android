package com.stproject.client.android.domain.model

data class DecorationItem(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val imageUrl: String?,
    val priceCredits: Int?,
    val owned: Boolean,
    val equipped: Boolean,
)
