package com.stproject.client.android.core.network

data class UserMeDto(
    val id: String,
    val email: String? = null,
    val tosVersion: String? = null,
    val tosAcceptedAt: String? = null,
)

data class AcceptTosRequestDto(
    val tosVersion: String? = null,
    val version: String? = null,
)

data class AcceptTosResponseDto(
    val ok: Boolean? = null,
    val tosAcceptedAt: String? = null,
    val tosVersion: String? = null,
)

data class UserConfigDto(
    val blockedTags: List<String>? = null,
    val blockedTagsHistory: List<String>? = null,
    val createdTagsHistory: List<String>? = null,
    val isNew: Boolean? = null,
    val ageVerified: Boolean? = null,
    val birthDate: String? = null,
)

data class UpdateUserConfigRequestDto(
    val blockedTags: List<String>? = null,
    val isNew: Boolean? = null,
    val ageVerified: Boolean? = null,
    val birthDate: String? = null,
)
