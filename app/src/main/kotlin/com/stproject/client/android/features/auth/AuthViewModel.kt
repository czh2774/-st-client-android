package com.stproject.client.android.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.auth.AuthService
import com.stproject.client.android.core.auth.AuthTokenStore
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val tokenStore: AuthTokenStore,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val email = MutableStateFlow("")
    private val password = MutableStateFlow("")
    private val isSubmitting = MutableStateFlow(false)
    private val isRestoring = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val isAuthenticated = MutableStateFlow(false)

    private val formState = combine(email, password, isSubmitting, isRestoring, error) { email, password, submitting, restoring, errorText ->
        AuthUiState(
            email = email,
            password = password,
            isSubmitting = submitting,
            isRestoring = restoring,
            error = errorText
        )
    }

    val uiState: StateFlow<AuthUiState> =
        combine(formState, isAuthenticated) { state, authed ->
            state.copy(isAuthenticated = authed)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AuthUiState(isAuthenticated = hasAccessToken())
        )

    init {
        restoreSession()
    }

    fun onEmailChanged(value: String) {
        email.value = value
    }

    fun onPasswordChanged(value: String) {
        password.value = value
    }

    fun onLoginClicked() {
        val emailValue = email.value.trim()
        val passwordValue = password.value
        if (emailValue.isEmpty() || passwordValue.isBlank()) {
            error.value = "email and password required"
            return
        }
        if (isSubmitting.value) return

        isSubmitting.value = true
        error.value = null

        viewModelScope.launch {
            try {
                authService.login(emailValue, passwordValue)
                password.value = ""
                isAuthenticated.value = true
            } catch (e: ApiException) {
                error.value = e.userMessage ?: e.message
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                error.value = "unexpected error"
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            try {
                authService.logout()
            } finally {
                withContext(NonCancellable) {
                    chatRepository.clearLocalSession()
                    isAuthenticated.value = false
                }
            }
        }
    }

    private fun restoreSession() {
        val accessToken = tokenStore.getAccessToken()
        val refreshToken = tokenStore.getRefreshToken()
        if (!accessToken.isNullOrBlank() && !isAccessTokenExpired()) {
            isAuthenticated.value = true
            return
        }
        if (refreshToken.isNullOrBlank()) {
            isAuthenticated.value = false
            return
        }
        viewModelScope.launch {
            isRestoring.value = true
            error.value = null
            try {
                val refreshed = authService.refreshTokens()
                isAuthenticated.value = !refreshed?.accessToken.isNullOrBlank()
            } catch (e: ApiException) {
                error.value = e.userMessage ?: e.message
                isAuthenticated.value = false
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                error.value = "unexpected error"
                isAuthenticated.value = false
            } finally {
                isRestoring.value = false
            }
        }
    }

    private fun hasAccessToken(): Boolean {
        val accessToken = tokenStore.getAccessToken()
        return !accessToken.isNullOrBlank() && !isAccessTokenExpired()
    }

    private fun isAccessTokenExpired(): Boolean {
        val expiresAt = tokenStore.getExpiresAtEpochSeconds() ?: return false
        val now = System.currentTimeMillis() / 1000
        return expiresAt <= (now + EXPIRY_LEEWAY_SECONDS)
    }

    private companion object {
        private const val EXPIRY_LEEWAY_SECONDS = 30
    }
}
