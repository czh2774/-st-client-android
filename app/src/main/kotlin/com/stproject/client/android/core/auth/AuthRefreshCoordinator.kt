package com.stproject.client.android.core.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRefreshCoordinator
    @Inject
    constructor(
        private val tokenStore: AuthTokenStore,
        private val authService: AuthService,
    ) {
        private val mutex = Mutex()

        suspend fun refreshIfNeeded(currentAccessToken: String?): AuthTokens? {
            return mutex.withLock {
                val latestAccess = tokenStore.getAccessToken()
                if (!latestAccess.isNullOrBlank() && latestAccess != currentAccessToken) {
                    return tokenStore.getTokens()
                }
                authService.refreshTokens()
            }
        }
    }
