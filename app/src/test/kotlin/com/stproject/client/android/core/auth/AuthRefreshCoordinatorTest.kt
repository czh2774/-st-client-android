package com.stproject.client.android.core.auth

import com.stproject.client.android.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRefreshCoordinatorTest : BaseUnitTest() {
    private class FakeTokenStore(
        private var accessToken: String? = null,
        private var refreshToken: String? = null,
    ) : AuthTokenStore {
        override fun getAccessToken(): String? = accessToken

        override fun getRefreshToken(): String? = refreshToken

        override fun getExpiresAtEpochSeconds(): Long? = null

        override fun getTokens(): AuthTokens? {
            val access = accessToken ?: return null
            return AuthTokens(access, refreshToken, null)
        }

        override fun updateTokens(
            accessToken: String,
            refreshToken: String?,
            expiresInSeconds: Long?,
        ) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }

        override fun clear() {
            accessToken = null
            refreshToken = null
        }
    }

    private class FakeAuthService(
        private val tokenStore: FakeTokenStore,
        private val result: AuthTokens?,
    ) : AuthService {
        var calls = 0
            private set

        override suspend fun login(
            email: String,
            password: String,
        ): AuthTokens {
            throw UnsupportedOperationException("login not used in this test")
        }

        override suspend fun refreshTokens(): AuthTokens? {
            calls += 1
            result?.let { tokenStore.updateTokens(it.accessToken, it.refreshToken, it.expiresAtEpochSeconds) }
            return result
        }

        override suspend fun logout() {
            tokenStore.clear()
        }
    }

    @Test
    fun `refresh uses latest token when store already updated`() =
        runTest(mainDispatcherRule.dispatcher) {
            val store = FakeTokenStore(accessToken = "new-access", refreshToken = "rt-1")
            val service = FakeAuthService(store, result = null)
            val coordinator = AuthRefreshCoordinator(store, service)

            val tokens = coordinator.refreshIfNeeded(currentAccessToken = "old-access")

            assertNotNull(tokens)
            assertEquals("new-access", tokens?.accessToken)
            assertEquals(0, service.calls)
        }

    @Test
    fun `refresh calls service when token unchanged`() =
        runTest(mainDispatcherRule.dispatcher) {
            val store = FakeTokenStore(accessToken = "old-access", refreshToken = "rt-1")
            val expected = AuthTokens(accessToken = "new-access", refreshToken = "rt-2", expiresAtEpochSeconds = null)
            val service = FakeAuthService(store, result = expected)
            val coordinator = AuthRefreshCoordinator(store, service)

            val tokens = coordinator.refreshIfNeeded(currentAccessToken = "old-access")

            assertEquals(1, service.calls)
            assertEquals(expected.accessToken, tokens?.accessToken)
            assertEquals(expected.refreshToken, tokens?.refreshToken)
        }

    @Test
    fun `refresh returns null when service fails`() =
        runTest(mainDispatcherRule.dispatcher) {
            val store = FakeTokenStore(accessToken = "old-access", refreshToken = "rt-1")
            val service = FakeAuthService(store, result = null)
            val coordinator = AuthRefreshCoordinator(store, service)

            val tokens = coordinator.refreshIfNeeded(currentAccessToken = "old-access")

            assertEquals(1, service.calls)
            assertNull(tokens)
        }
}
