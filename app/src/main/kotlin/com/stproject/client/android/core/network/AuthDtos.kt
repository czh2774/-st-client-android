package com.stproject.client.android.core.network

data class AuthRefreshRequestDto(
    val refreshToken: String
)

data class AuthLoginRequestDto(
    val email: String,
    val password: String
)

data class AuthLogoutRequestDto(
    val refreshToken: String? = null
)

data class AuthRefreshResponseDto(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long
)

data class AuthLoginResponseDto(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long
)
