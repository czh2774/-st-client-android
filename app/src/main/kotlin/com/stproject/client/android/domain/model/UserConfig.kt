package com.stproject.client.android.domain.model

data class UserConfig(
    val ageVerified: Boolean,
    val birthDate: String?,
    val blockedTags: List<String>,
)
