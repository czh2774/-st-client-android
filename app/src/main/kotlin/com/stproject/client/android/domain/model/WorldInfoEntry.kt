package com.stproject.client.android.domain.model

data class WorldInfoEntry(
    val id: String,
    val characterId: String?,
    val keys: List<String>,
    val secondaryKeys: List<String>,
    val content: String,
    val comment: String?,
    val enabled: Boolean,
)

data class WorldInfoEntryInput(
    val characterId: String?,
    val keys: List<String>,
    val content: String,
    val comment: String?,
    val enabled: Boolean,
)
