package com.stproject.client.android.core.auth

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ClientHeadersInterceptor
import com.stproject.client.android.core.network.CookieCleaner
import com.stproject.client.android.core.network.StApi
import com.stproject.client.android.core.network.StAuthApi
import com.stproject.client.android.core.network.StOkHttpClientFactory
import kotlinx.coroutines.runBlocking
import okhttp3.CookieJar
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

class AuthRefreshIntegrationTest {
    private lateinit var server: MockWebServer

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

    private class FakeCookieCleaner : CookieCleaner {
        var cleared = false
            private set

        override fun clearCookies() {
            cleared = true
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login stores tokens`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/api/v1/auth/login") == true -> {
                        val body = request.body.readUtf8()
                        if (!body.contains("\"email\":\"user@example.com\"")) {
                            return MockResponse().setResponseCode(400).setBody("""{"message":"missing email"}""")
                        }
                        if (!body.contains("\"password\":\"secret\"")) {
                            return MockResponse().setResponseCode(400).setBody("""{"message":"missing password"}""")
                        }
                        if (request.getHeader("X-Auth-Client") != "android") {
                            return MockResponse().setResponseCode(400).setBody("""{"message":"missing client header"}""")
                        }
                        MockResponse().setResponseCode(200).setBody(
                            """{"code":200,"data":{"accessToken":"access-1","refreshToken":"refresh-1","expiresIn":3600}}"""
                        )
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val tokenStore = FakeTokenStore()
        val cookieCleaner = FakeCookieCleaner()
        val baseUrl = server.url("/api/v1/")
        val factory = StOkHttpClientFactory()
        val headersInterceptor = ClientHeadersInterceptor()

        val authClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor)
        )
        val authRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val authApi = authRetrofit.create(StAuthApi::class.java)
        val authService = DefaultAuthService(authApi, ApiClient(), tokenStore, cookieCleaner)

        val tokens = authService.login("user@example.com", "secret")

        assertNotNull(tokens)
        assertEquals("access-1", tokenStore.getAccessToken())
        assertEquals("refresh-1", tokenStore.getRefreshToken())

        val req = server.takeRequest()
        assertEquals("/api/v1/auth/login", req.path)
    }

    @Test
    fun `refreshes on 401 and retries once`() = runBlocking {
        val healthCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/api/v1/health") == true -> {
                        if (healthCalls.getAndIncrement() == 0) {
                            MockResponse().setResponseCode(401).setBody("""{"message":"unauthorized"}""")
                        } else {
                            MockResponse().setResponseCode(200).setBody(
                                """{"code":200,"data":{"status":"ok","ok":true,"service":"st"}}"""
                            )
                        }
                    }
                    request.path?.startsWith("/api/v1/auth/refresh") == true -> {
                        val body = request.body.readUtf8()
                        if (!body.contains("\"refreshToken\":\"rt-1\"")) {
                            return MockResponse().setResponseCode(400).setBody("""{"message":"bad refresh"}""")
                        }
                        if (request.getHeader("X-Auth-Client") != "android") {
                            return MockResponse().setResponseCode(400).setBody("""{"message":"missing client header"}""")
                        }
                        MockResponse().setResponseCode(200).setBody(
                            """{"code":200,"data":{"accessToken":"new-access","refreshToken":"rt-2","expiresIn":3600}}"""
                        )
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val tokenStore = FakeTokenStore(accessToken = "old-access", refreshToken = "rt-1")
        val cookieCleaner = FakeCookieCleaner()
        val baseUrl = server.url("/api/v1/")
        val factory = StOkHttpClientFactory()
        val headersInterceptor = ClientHeadersInterceptor()

        val authClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor)
        )
        val authRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val authApi = authRetrofit.create(StAuthApi::class.java)
        val authService = DefaultAuthService(authApi, ApiClient(), tokenStore, cookieCleaner)
        val refreshCoordinator = AuthRefreshCoordinator(tokenStore, authService)
        val authenticator = AuthAuthenticator(tokenStore, refreshCoordinator)

        val mainClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor, AuthInterceptor(tokenStore)),
            authenticator = authenticator
        )
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(mainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(StApi::class.java)

        val resp = api.health()

        assertEquals(200, resp.code)
        assertEquals("new-access", tokenStore.getAccessToken())

        val first = server.takeRequest()
        val second = server.takeRequest()
        val third = server.takeRequest()
        assertEquals("/api/v1/health", first.path)
        assertEquals("/api/v1/auth/refresh", second.path)
        assertEquals("/api/v1/health", third.path)
        assertEquals("Bearer old-access", first.getHeader("Authorization"))
        assertEquals("Bearer new-access", third.getHeader("Authorization"))
    }

    @Test
    fun `clears tokens when refresh is unauthorized`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/api/v1/health") == true ->
                        MockResponse().setResponseCode(401).setBody("""{"message":"unauthorized"}""")
                    request.path?.startsWith("/api/v1/auth/refresh") == true ->
                        MockResponse().setResponseCode(401).setBody("""{"message":"refresh expired"}""")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val tokenStore = FakeTokenStore(accessToken = "old-access", refreshToken = "rt-1")
        val cookieCleaner = FakeCookieCleaner()
        val baseUrl = server.url("/api/v1/")
        val factory = StOkHttpClientFactory()
        val headersInterceptor = ClientHeadersInterceptor()

        val authClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor)
        )
        val authRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val authApi = authRetrofit.create(StAuthApi::class.java)
        val authService = DefaultAuthService(authApi, ApiClient(), tokenStore, cookieCleaner)
        val refreshCoordinator = AuthRefreshCoordinator(tokenStore, authService)
        val authenticator = AuthAuthenticator(tokenStore, refreshCoordinator)

        val mainClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor, AuthInterceptor(tokenStore)),
            authenticator = authenticator
        )
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(mainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(StApi::class.java)

        try {
            api.health()
        } catch (_: Exception) {
            // Expected: refresh fails, health stays 401.
        }

        assertNull(tokenStore.getAccessToken())
        assertNull(tokenStore.getRefreshToken())
        assertEquals(true, cookieCleaner.cleared)

        val first = server.takeRequest()
        val second = server.takeRequest()
        assertEquals("/api/v1/health", first.path)
        assertEquals("/api/v1/auth/refresh", second.path)
    }

    @Test
    fun `logout calls server and clears local state`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/api/v1/auth/logout") == true -> {
                        val body = request.body.readUtf8()
                        if (!body.contains("\"refreshToken\":\"rt-9\"")) {
                            return MockResponse().setResponseCode(400).setBody("""{"message":"missing refresh"}""")
                        }
                        MockResponse().setResponseCode(200).setBody("""{"code":200,"data":{"message":"ok"}}""")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val tokenStore = FakeTokenStore(accessToken = "access-9", refreshToken = "rt-9")
        val cookieCleaner = FakeCookieCleaner()
        val baseUrl = server.url("/api/v1/")
        val factory = StOkHttpClientFactory()
        val headersInterceptor = ClientHeadersInterceptor()

        val authClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor)
        )
        val authRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val authApi = authRetrofit.create(StAuthApi::class.java)
        val authService = DefaultAuthService(authApi, ApiClient(), tokenStore, cookieCleaner)

        authService.logout()

        assertNull(tokenStore.getAccessToken())
        assertNull(tokenStore.getRefreshToken())
        assertEquals(true, cookieCleaner.cleared)
        val req = server.takeRequest()
        assertEquals("/api/v1/auth/logout", req.path)
    }

    @Test
    fun `logout clears tokens and prevents refresh on 401`() = runBlocking {
        val refreshCalls = AtomicInteger(0)
        val healthCalls = AtomicInteger(0)
        val logoutCalls = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/api/v1/auth/logout") == true -> {
                        logoutCalls.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody("""{"code":200,"data":{"message":"ok"}}""")
                    }
                    request.path?.startsWith("/api/v1/auth/refresh") == true -> {
                        refreshCalls.incrementAndGet()
                        MockResponse().setResponseCode(500).setBody("""{"message":"unexpected refresh"}""")
                    }
                    request.path?.startsWith("/api/v1/health") == true -> {
                        healthCalls.incrementAndGet()
                        MockResponse().setResponseCode(401).setBody("""{"message":"unauthorized"}""")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val tokenStore = FakeTokenStore(accessToken = "access-9", refreshToken = "rt-9")
        val cookieCleaner = FakeCookieCleaner()
        val baseUrl = server.url("/api/v1/")
        val factory = StOkHttpClientFactory()
        val headersInterceptor = ClientHeadersInterceptor()

        val authClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor)
        )
        val authRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val authApi = authRetrofit.create(StAuthApi::class.java)
        val authService = DefaultAuthService(authApi, ApiClient(), tokenStore, cookieCleaner)
        val refreshCoordinator = AuthRefreshCoordinator(tokenStore, authService)
        val authenticator = AuthAuthenticator(tokenStore, refreshCoordinator)

        val mainClient = factory.create(
            cookieJar = CookieJar.NO_COOKIES,
            additionalInterceptors = listOf(headersInterceptor, AuthInterceptor(tokenStore)),
            authenticator = authenticator
        )
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(mainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(StApi::class.java)

        authService.logout()

        try {
            api.health()
        } catch (_: Exception) {
            // Expected: 401 with no refresh.
        }

        assertNull(tokenStore.getAccessToken())
        assertNull(tokenStore.getRefreshToken())
        assertEquals(true, cookieCleaner.cleared)
        assertEquals(1, logoutCalls.get())
        assertEquals(1, healthCalls.get())
        assertEquals(0, refreshCalls.get())
    }
}
