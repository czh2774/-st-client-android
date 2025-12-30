package com.stproject.client.android.core.auth

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthAuthenticatorTest {
    private class FakeTokenStore(
        private var accessToken: String? = null,
        private var refreshToken: String? = null
    ) : AuthTokenStore {
        override fun getAccessToken(): String? = accessToken
        override fun getRefreshToken(): String? = refreshToken
        override fun getExpiresAtEpochSeconds(): Long? = null
        override fun getTokens(): AuthTokens? {
            val access = accessToken ?: return null
            return AuthTokens(access, refreshToken, null)
        }

        override fun updateTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long?) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }

        override fun clear() {
            accessToken = null
            refreshToken = null
        }
    }

    private class FakeAuthService : AuthService {
        var calls = 0
            private set

        override suspend fun login(email: String, password: String): AuthTokens {
            throw UnsupportedOperationException("login not used in this test")
        }

        override suspend fun refreshTokens(): AuthTokens? {
            calls += 1
            return null
        }

        override suspend fun logout() {}
    }

    @Test
    fun `auth endpoints are not retried`() {
        val store = FakeTokenStore(accessToken = "token", refreshToken = "rt")
        val coordinator = AuthRefreshCoordinator(store, FakeAuthService())
        val authenticator = AuthAuthenticator(store, coordinator)
        val request = Request.Builder()
            .url("https://example.com/api/v1/auth/refresh")
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        val result = authenticator.authenticate(null, response)

        assertNull(result)
    }

    @Test
    fun `uses newer token from store without refresh`() {
        val store = FakeTokenStore(accessToken = "new-token", refreshToken = "rt")
        val coordinator = AuthRefreshCoordinator(store, FakeAuthService())
        val authenticator = AuthAuthenticator(store, coordinator)
        val request = Request.Builder()
            .url("https://example.com/api/v1/health")
            .header("Authorization", "Bearer old-token")
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        val result = authenticator.authenticate(null, response)

        assertEquals("Bearer new-token", result?.header("Authorization"))
    }
}
