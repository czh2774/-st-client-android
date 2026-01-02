package com.stproject.client.android.domain.model

data class ModelPreset(
    val id: String,
    val displayName: String,
    val subtitle: String?,
    val description: String?,
    val provider: String?,
    val modelName: String?,
    val isDefault: Boolean,
    val isEnabled: Boolean,
    val sortOrder: Int,
    val tags: List<String>,
)
