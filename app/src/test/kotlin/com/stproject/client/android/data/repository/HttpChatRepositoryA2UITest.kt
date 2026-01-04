package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.A2UIEventResponseDto
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ApiEnvelope
import com.stproject.client.android.core.network.StApi
import com.stproject.client.android.core.network.StBaseUrlProvider
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.core.theme.ThemeMode
import com.stproject.client.android.data.local.ChatMessageDao
import com.stproject.client.android.data.local.ChatMessageEntity
import com.stproject.client.android.data.local.ChatSessionDao
import com.stproject.client.android.data.local.ChatSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class HttpChatRepositoryA2UITest {
    private lateinit var server: MockWebServer

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
    fun `reports error event when a2ui message is invalid`() =
        runBlocking {
            val invalidMessage =
                """{"beginRendering":{"surfaceId":"s1","root":"root"},"deleteSurface":{"surfaceId":"s1"}}"""
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        return when {
                            request.path?.startsWith("/api/v1/a2ui/stream") == true ->
                                MockResponse()
                                    .setResponseCode(200)
                                    .setHeader("Content-Type", "text/event-stream")
                                    .setSocketPolicy(SocketPolicy.KEEP_OPEN)
                                    .setBody("data: $invalidMessage\n\n")
                            request.path?.startsWith("/api/v1/a2ui/event") == true ->
                                MockResponse()
                                    .setResponseCode(200)
                                    .setBody(
                                        """{"code":200,"data":${encodeEnvelopeData(true)}}""",
                                    )
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }

            val repo = buildRepository(server.url("/api/v1/"), sessionId = "s1")
            val job = launch(Dispatchers.IO) { repo.a2uiState.collect { } }

            val requests = mutableListOf<RecordedRequest>()
            withTimeout(5_000) {
                while (requests.size < 2) {
                    val req = server.takeRequest(1, TimeUnit.SECONDS)
                    if (req != null) {
                        requests.add(req)
                    }
                }
            }

            val streamRequest = requests.firstOrNull { it.path == "/api/v1/a2ui/stream?sessionId=s1" }
            assertNotNull(streamRequest)
            val eventRequest = requests.firstOrNull { it.path == "/api/v1/a2ui/event" }
            assertNotNull(eventRequest)
            val body = eventRequest?.body?.readUtf8().orEmpty()
            assertTrue(body.contains("\"error\""))
            assertTrue(body.contains("\"type\":\"invalid_message\""))
            assertTrue(body.contains("\"surfaceId\":\"s1\""))

            job.cancelAndJoin()
        }

    @Test
    fun `retries a2ui stream after server failure`() =
        runBlocking {
            val attempts = AtomicInteger(0)
            val beginRendering =
                """{"beginRendering":{"surfaceId":"s1","root":"root"}}"""
            val sseBody = "data: $beginRendering\n\n" + "data: [DONE]\n\n"

            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        if (request.path?.startsWith("/api/v1/a2ui/stream") == true) {
                            return if (attempts.getAndIncrement() == 0) {
                                MockResponse().setResponseCode(500).setBody("boom")
                            } else {
                                MockResponse()
                                    .setResponseCode(200)
                                    .setHeader("Content-Type", "text/event-stream")
                                    .setBody(sseBody)
                            }
                        }
                        return MockResponse().setResponseCode(404)
                    }
                }

            val repo = buildRepository(server.url("/api/v1/"), sessionId = "s1")
            val state =
                withTimeout(5_000) {
                    repo.a2uiState.filterNotNull().first()
                }

            assertTrue(state.surfaces.containsKey("s1"))
            val first = server.takeRequest(2, TimeUnit.SECONDS)
            val second = server.takeRequest(2, TimeUnit.SECONDS)
            assertEquals("/api/v1/a2ui/stream?sessionId=s1", first?.path)
            assertEquals("/api/v1/a2ui/stream?sessionId=s1", second?.path)
        }

    private fun buildRepository(
        baseUrl: HttpUrl,
        sessionId: String,
    ): HttpChatRepository {
        val client = OkHttpClient.Builder().build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val api = retrofit.create(StApi::class.java)
        val baseUrlProvider =
            object : StBaseUrlProvider() {
                override fun baseUrl(): String = baseUrl.toString()
            }
        return HttpChatRepository(
            api = api,
            apiClient = ApiClient(),
            okHttpClient = client,
            baseUrlProvider = baseUrlProvider,
            messageDao = FakeChatMessageDao(),
            sessionDao = FakeChatSessionDao(),
            sessionStore = FakeChatSessionStore(sessionId),
            userPreferencesStore = FakeUserPreferencesStore(),
        )
    }

    private class FakeChatMessageDao : ChatMessageDao {
        override fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>> = flowOf(emptyList())

        override suspend fun getById(id: String): ChatMessageEntity? = null

        override suspend fun getByServerId(serverId: String): ChatMessageEntity? = null

        override suspend fun upsert(message: ChatMessageEntity) {}

        override suspend fun upsertAll(messages: List<ChatMessageEntity>) {}

        override suspend fun updateContent(
            id: String,
            content: String,
            isStreaming: Boolean,
        ) {}

        override suspend fun updateContentByServerId(
            serverId: String,
            content: String,
            isStreaming: Boolean,
        ) {}

        override suspend fun updateServerId(
            id: String,
            serverId: String?,
        ) {}

        override suspend fun deleteById(id: String) {}

        override suspend fun deleteBySessionId(sessionId: String) {}

        override suspend fun deleteBySessionIds(sessionIds: List<String>) {}

        override suspend fun deleteAll() {}
    }

    private class FakeChatSessionDao : ChatSessionDao {
        override suspend fun listSessions(
            limit: Int,
            offset: Int,
        ): List<ChatSessionEntity> = emptyList()

        override suspend fun getSession(sessionId: String): ChatSessionEntity? = null

        override suspend fun countSessions(): Int = 0

        override suspend fun listSessionIds(
            limit: Int,
            offset: Int,
        ): List<String> = emptyList()

        override suspend fun upsert(session: ChatSessionEntity) {}

        override suspend fun upsertAll(sessions: List<ChatSessionEntity>) {}

        override suspend fun deleteByIds(sessionIds: List<String>) {}

        override suspend fun deleteAll() {}
    }

    private class FakeChatSessionStore(
        private var sessionId: String?,
    ) : ChatSessionStore {
        private var clientSessionId: String? = null
        private var primaryMemberId: String? = null
        private var shareCode: String? = null
        private var updatedAtMs: Long? = null

        override fun getSessionId(): String? = sessionId

        override fun setSessionId(sessionId: String?) {
            this.sessionId = sessionId
        }

        override fun getClientSessionId(): String? = clientSessionId

        override fun setClientSessionId(clientSessionId: String?) {
            this.clientSessionId = clientSessionId
        }

        override fun getPrimaryMemberId(): String? = primaryMemberId

        override fun setPrimaryMemberId(memberId: String?) {
            primaryMemberId = memberId
        }

        override fun getShareCode(): String? = shareCode

        override fun setShareCode(shareCode: String?) {
            this.shareCode = shareCode
        }

        override fun getSessionUpdatedAtMs(): Long? = updatedAtMs

        override fun setSessionUpdatedAtMs(updatedAtMs: Long?) {
            this.updatedAtMs = updatedAtMs
        }

        override fun clear() {
            sessionId = null
            clientSessionId = null
            primaryMemberId = null
            shareCode = null
            updatedAtMs = null
        }
    }

    private class FakeUserPreferencesStore : UserPreferencesStore {
        private var nsfwAllowed: Boolean = false
        private var themeMode: ThemeMode = ThemeMode.System
        private var languageTag: String? = null
        private var modelPresetId: String? = null
        private var globalVariables: Map<String, Any?> = emptyMap()
        private val presetVariables: MutableMap<String, Map<String, Any?>> = mutableMapOf()

        override fun isNsfwAllowed(): Boolean = nsfwAllowed

        override fun setNsfwAllowed(value: Boolean) {
            nsfwAllowed = value
        }

        override fun getThemeMode(): ThemeMode = themeMode

        override fun setThemeMode(mode: ThemeMode) {
            themeMode = mode
        }

        override fun getLanguageTag(): String? = languageTag

        override fun setLanguageTag(tag: String?) {
            languageTag = tag
        }

        override fun getModelPresetId(): String? = modelPresetId

        override fun setModelPresetId(presetId: String?) {
            modelPresetId = presetId
        }

        override fun getGlobalVariables(): Map<String, Any?> = globalVariables

        override fun setGlobalVariables(variables: Map<String, Any?>) {
            globalVariables = variables
        }

        override fun getPresetVariables(presetId: String): Map<String, Any?> =
            presetVariables[presetId].orEmpty()

        override fun setPresetVariables(
            presetId: String,
            variables: Map<String, Any?>,
        ) {
            presetVariables[presetId] = variables
        }
    }

    private companion object {
        private fun encodeEnvelopeData(accepted: Boolean): String {
            val data = A2UIEventResponseDto(accepted = accepted, reason = null)
            return ApiEnvelope(code = 200, data = data).let { """{"accepted":${it.data?.accepted}}""" }
        }
    }
}
