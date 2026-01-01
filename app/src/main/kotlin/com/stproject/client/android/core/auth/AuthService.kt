package com.stproject.client.android.core.auth

import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.network.AuthLoginRequestDto
import com.stproject.client.android.core.network.AuthLogoutRequestDto
import com.stproject.client.android.core.network.AuthRefreshRequestDto
import com.stproject.client.android.core.network.CookieCleaner
import com.stproject.client.android.core.network.StAuthApi
import javax.inject.Inject
import javax.inject.Singleton

interface AuthService {
    suspend fun login(
        email: String,
        password: String,
    ): AuthTokens

    suspend fun refreshTokens(): AuthTokens?

    suspend fun logout()
}

@Singleton
class DefaultAuthService
    @Inject
    constructor(
        private val authApi: StAuthApi,
        private val apiClient: ApiClient,
        private val tokenStore: AuthTokenStore,
        private val cookieCleaner: CookieCleaner,
    ) : AuthService {
        override suspend fun login(
            email: String,
            password: String,
        ): AuthTokens {
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()
            val resp =
                apiClient.call {
                    authApi.login(AuthLoginRequestDto(email = trimmedEmail, password = trimmedPassword))
                }
            val accessToken = resp.accessToken.trim()
            if (accessToken.isEmpty()) {
                throw ApiException(message = "login failed: missing access token")
            }
            val refreshToken = resp.refreshToken?.trim()?.takeIf { it.isNotEmpty() }
            tokenStore.updateTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresInSeconds = resp.expiresIn,
            )
            return tokenStore.getTokens() ?: AuthTokens(accessToken, refreshToken, null)
        }

        override suspend fun refreshTokens(): AuthTokens? {
            val refreshToken = tokenStore.getRefreshToken()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return try {
                val resp =
                    apiClient.call {
                        authApi.refresh(AuthRefreshRequestDto(refreshToken = refreshToken))
                    }
                val accessToken = resp.accessToken.trim()
                if (accessToken.isEmpty()) {
                    tokenStore.clear()
                    cookieCleaner.clearCookies()
                    return null
                }
                val newRefresh = resp.refreshToken?.trim()?.takeIf { it.isNotEmpty() } ?: refreshToken
                tokenStore.updateTokens(
                    accessToken = accessToken,
                    refreshToken = newRefresh,
                    expiresInSeconds = resp.expiresIn,
                )
                tokenStore.getTokens()
            } catch (e: ApiException) {
                if (e.httpStatus == 400 || e.httpStatus == 401 || e.httpStatus == 403 || e.httpStatus == 423) {
                    tokenStore.clear()
                    cookieCleaner.clearCookies()
                }
                null
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                null
            }
        }

        override suspend fun logout() {
            val refreshToken = tokenStore.getRefreshToken()?.trim()?.takeIf { it.isNotEmpty() }
            try {
                apiClient.call {
                    authApi.logout(AuthLogoutRequestDto(refreshToken = refreshToken))
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                // Best-effort logout; clear local state regardless of server response.
            } finally {
                tokenStore.clear()
                cookieCleaner.clearCookies()
            }
        }
    }
