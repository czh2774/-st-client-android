package com.stproject.client.android.core.network

import com.google.gson.annotations.SerializedName

data class ModelPresetDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("subtitle")
    val subtitle: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("provider")
    val provider: String? = null,
    @SerializedName("model_name")
    val modelName: String? = null,
    @SerializedName("is_default")
    val isDefault: Boolean? = null,
    @SerializedName("is_enabled")
    val isEnabled: Boolean? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,
)

data class PresetListResponseDto(
    val items: List<ModelPresetDto>? = null,
)
