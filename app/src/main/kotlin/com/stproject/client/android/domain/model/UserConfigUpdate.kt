package com.stproject.client.android.domain.model

data class UserConfigUpdate(
    val blockedTags: List<String>? = null,
    val ageVerified: Boolean? = null,
    val birthDate: String? = null,
)
