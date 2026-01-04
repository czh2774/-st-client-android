package com.stproject.client.android.core.network

import com.google.gson.annotations.SerializedName

data class PersonaDto(
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("is_default")
    val isDefault: Boolean? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

data class PersonaListResponseDto(
    val items: List<PersonaDto>? = null,
)

data class CreatePersonaRequestDto(
    val name: String,
    val description: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("is_default")
    val isDefault: Boolean? = null,
)

data class UpdatePersonaRequestDto(
    val name: String,
    val description: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("is_default")
    val isDefault: Boolean? = null,
)
