package com.stproject.client.android.features.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val isRestoring: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
)
