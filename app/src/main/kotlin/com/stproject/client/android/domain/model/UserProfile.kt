package com.stproject.client.android.domain.model

data class UserProfile(
    val id: String,
    val email: String?,
    val tosVersion: String?,
    val tosAcceptedAt: String?,
)
