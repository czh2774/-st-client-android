package com.stproject.client.android.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.stproject.client.android.BuildConfig
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.network.ChatCompletionRequestDto
import com.stproject.client.android.core.network.CreateChatSessionRequestDto
import com.stproject.client.android.core.network.StApi
import com.stproject.client.android.core.network.StBaseUrlProvider
import com.stproject.client.android.data.local.ChatMessageDao
import com.stproject.client.android.data.local.ChatMessageEntity
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.core.session.ChatSessionStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class HttpChatRepository @Inject constructor(
    private val api: StApi,
    private val apiClient: ApiClient,
    private val okHttpClient: OkHttpClient,
    private val baseUrlProvider: StBaseUrlProvider,
    private val messageDao: ChatMessageDao,
    private val sessionStore: ChatSessionStore
) : ChatRepository {
    private val gson = Gson()
    private val sseClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .build()
    private val sessionIdFlow = MutableStateFlow(sessionStore.getSessionId())

    override val messages: Flow<List<ChatMessage>> =
        sessionIdFlow.flatMapLatest { sessionId ->
            if (sessionId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                messageDao.observeMessages(sessionId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override suspend fun sendUserMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        val sessionId = ensureSessionId()
        val userId = UUID.randomUUID().toString()
        val assistantId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            messageDao.upsert(
                ChatMessageEntity(
                    id = userId,
                    sessionId = sessionId,
                    serverId = null,
                    role = ChatRole.User.name,
                    content = trimmed,
                    createdAt = now,
                    isStreaming = false
                )
            )
            messageDao.upsert(
                ChatMessageEntity(
                    id = assistantId,
                    sessionId = sessionId,
                    serverId = null,
                    role = ChatRole.Assistant.name,
                    content = "",
                    createdAt = now + 1,
                    isStreaming = true
                )
            )
        }

        val streamFlow = streamCompletion(sessionId, trimmed, userId, assistantId)
        val builder = StringBuilder()
        var hasDelta = false
        var streamFailed = false
        try {
            streamFlow.collect { event ->
                when (event) {
                    is StreamEvent.Delta -> {
                        hasDelta = true
                        builder.append(event.content)
                        withContext(Dispatchers.IO) {
                            messageDao.updateContent(assistantId, builder.toString(), true)
                        }
                    }
                    StreamEvent.Done -> {
                        withContext(Dispatchers.IO) {
                            messageDao.updateContent(assistantId, builder.toString(), false)
                        }
                    }
                    is StreamEvent.MessageIds -> {
                        handleMessageIds(event, userId, assistantId)
                    }
                    is StreamEvent.SessionRecency -> {
                        handleSessionRecency(event)
                    }
                    is StreamEvent.Meta -> {
                        handleMetaEvent(event)
                    }
                    is StreamEvent.Error -> {
                        streamFailed = true
                        throw ApiException(message = event.message)
                    }
                }
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Timber.w(e, "chat stream failed")
            streamFailed = true
            withContext(Dispatchers.IO) {
                messageDao.deleteById(assistantId)
            }
            if (e is ApiException) throw e
            throw ApiException(message = "stream failed")
        } finally {
            if (!hasDelta && !streamFailed) {
                fallbackCompletion(sessionId, trimmed, userId, assistantId)
            }
        }
    }

    override suspend fun clearLocalSession() {
        withContext(Dispatchers.IO) {
            messageDao.deleteAll()
        }
        sessionStore.clear()
        sessionIdFlow.value = null
    }

    private suspend fun ensureSessionId(): String {
        val existing = sessionStore.getSessionId()?.trim()?.takeIf { it.isNotEmpty() }
        if (existing != null) return existing

        val memberId = BuildConfig.DEFAULT_CHARACTER_ID.trim()
        if (memberId.isEmpty()) {
            Timber.w("default character id missing; set ST_DEFAULT_CHARACTER_ID")
            throw ApiException(
                message = "missing default character id",
                userMessage = "default character not configured"
            )
        }

        val clientSessionId = sessionStore.getClientSessionId()?.trim()?.takeIf { it.isNotEmpty() }
            ?: "android_${UUID.randomUUID()}"
        if (sessionStore.getClientSessionId().isNullOrBlank()) {
            sessionStore.setClientSessionId(clientSessionId)
        }

        val created = apiClient.call {
            api.createChatSession(
                CreateChatSessionRequestDto(
                    members = listOf(memberId),
                    greetingIndex = 0,
                    clientSessionId = clientSessionId
                )
            )
        }
        val newId = created.sessionId.trim()
        if (newId.isEmpty()) {
            Timber.w("session create returned empty id")
            throw ApiException(message = "empty session id", userMessage = "session creation failed")
        }
        sessionStore.setSessionId(newId)
        sessionIdFlow.value = newId
        return newId
    }

    private suspend fun fallbackCompletion(
        sessionId: String,
        content: String,
        userId: String,
        assistantId: String
    ) {
        val resp = apiClient.call {
            api.createChatCompletion(
                sessionId = sessionId,
                request = ChatCompletionRequestDto(
                    message = content,
                    stream = false,
                    worldInfoMinActivations = 2,
                    worldInfoMinActivationsDepthMax = 10,
                    clientMessageId = userId,
                    clientAssistantMessageId = assistantId
                )
            )
        }
        withContext(Dispatchers.IO) {
            messageDao.updateContent(assistantId, resp.content, false)
        }
    }

    private fun streamCompletion(
        sessionId: String,
        content: String,
        userId: String,
        assistantId: String
    ): Flow<StreamEvent> = callbackFlow {
        val payload = gson.toJson(
            ChatCompletionRequestDto(
                message = content,
                stream = true,
                worldInfoMinActivations = 2,
                worldInfoMinActivationsDepthMax = 10,
                clientMessageId = userId,
                clientAssistantMessageId = assistantId
            )
        )
        val request = Request.Builder()
            .url("${baseUrlProvider.baseUrl()}chats/$sessionId/completion")
            .header("Accept", "text/event-stream")
            .header("X-ST-SSE-Protocol", "1")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                val parsed = parseSseData(data) ?: return
                trySend(parsed)
                if (parsed == StreamEvent.Done) {
                    eventSource.cancel()
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: ApiException(message = "stream failed"))
            }
        }
        val factory = EventSources.createFactory(sseClient)
        val eventSource = factory.newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    private fun parseSseData(data: String): StreamEvent? {
        if (data == "[DONE]") return StreamEvent.Done
        val json = runCatching { gson.fromJson(data, JsonObject::class.java) }.getOrNull() ?: return null
        if (json.has("error")) {
            val error = json.getAsJsonObject("error")
            val message = error?.readString("message") ?: "stream error"
            return StreamEvent.Error(message)
        }
        if (json.has("st_event")) {
            val event = json.readString("st_event") ?: return null
            return when (event) {
                "message_ids" -> {
                    StreamEvent.MessageIds(
                        sessionId = json.readString("sessionId"),
                        userMessageId = json.readString("userMessageId"),
                        assistantMessageId = json.readString("assistantMessageId"),
                        clientMessageId = json.readString("clientMessageId"),
                        clientAssistantMessageId = json.readString("clientAssistantMessageId"),
                        sessionUpdatedAtMs = json.readLong("sessionUpdatedAtMs")
                    )
                }
                "session_recency" -> {
                    StreamEvent.SessionRecency(
                        sessionId = json.readString("sessionId"),
                        sessionUpdatedAtMs = json.readLong("sessionUpdatedAtMs")
                    )
                }
                else -> StreamEvent.Meta(event, json)
            }
        }
        val choices = json.getAsJsonArray("choices") ?: return null
        if (choices.size() == 0) return null
        val choice = choices[0].asJsonObject
        val delta = choice.getAsJsonObject("delta") ?: return null
        val content = delta.get("content")?.asString ?: return null
        return StreamEvent.Delta(content)
    }

    private fun JsonObject.readString(key: String): String? =
        runCatching { get(key)?.takeIf { !it.isJsonNull }?.asString }.getOrNull()

    private fun JsonObject.readLong(key: String): Long? =
        runCatching { get(key)?.takeIf { !it.isJsonNull }?.asLong }.getOrNull()

    private suspend fun handleMessageIds(
        event: StreamEvent.MessageIds,
        userId: String,
        assistantId: String
    ) {
        val clientUserId = event.clientMessageId ?: userId
        val clientAssistantId = event.clientAssistantMessageId ?: assistantId
        withContext(Dispatchers.IO) {
            if (!event.userMessageId.isNullOrBlank()) {
                messageDao.updateServerId(clientUserId, event.userMessageId)
            }
            if (!event.assistantMessageId.isNullOrBlank()) {
                messageDao.updateServerId(clientAssistantId, event.assistantMessageId)
            }
        }
        if (event.clientMessageId != null && event.clientMessageId != userId) {
            Timber.w("message_ids clientMessageId mismatch (local=%s, remote=%s)", userId, event.clientMessageId)
        }
        if (event.clientAssistantMessageId != null && event.clientAssistantMessageId != assistantId) {
            Timber.w(
                "message_ids clientAssistantMessageId mismatch (local=%s, remote=%s)",
                assistantId,
                event.clientAssistantMessageId
            )
        }
        if (!event.sessionId.isNullOrBlank() && event.sessionId != sessionStore.getSessionId()) {
            Timber.w("message_ids sessionId mismatch (local=%s, remote=%s)", sessionStore.getSessionId(), event.sessionId)
        }
        if (event.sessionUpdatedAtMs != null && event.sessionUpdatedAtMs > 0) {
            sessionStore.setSessionUpdatedAtMs(event.sessionUpdatedAtMs)
        }
    }

    private fun handleSessionRecency(event: StreamEvent.SessionRecency) {
        if (event.sessionUpdatedAtMs != null && event.sessionUpdatedAtMs > 0) {
            sessionStore.setSessionUpdatedAtMs(event.sessionUpdatedAtMs)
        }
        if (!event.sessionId.isNullOrBlank() && event.sessionId != sessionStore.getSessionId()) {
            Timber.w("session_recency sessionId mismatch (local=%s, remote=%s)", sessionStore.getSessionId(), event.sessionId)
        }
    }

    private fun handleMetaEvent(event: StreamEvent.Meta) {
        if (!BuildConfig.DEBUG) return
        when (event.name) {
            "usage" -> {
                val prompt = event.payload.readLong("promptTokens") ?: event.payload.readLong("prompt_tokens")
                val completion = event.payload.readLong("completionTokens") ?: event.payload.readLong("completion_tokens")
                val total = event.payload.readLong("totalTokens") ?: event.payload.readLong("total_tokens")
                Timber.d("sse usage prompt=%s completion=%s total=%s", prompt, completion, total)
            }
            "emotion",
            "reasoning",
            "worldinfo_automations",
            "worldinfo_outlets",
            "speaker",
            "group_rotation",
            "assistant_segments",
            "prompt_itemization" -> {
                Timber.d("sse meta event=%s", event.name)
            }
            else -> {
                Timber.d("sse meta event=%s", event.name)
            }
        }
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        val role = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.Assistant)
        return ChatMessage(id = id, role = role, content = content)
    }

    private sealed class StreamEvent {
        data class Delta(val content: String) : StreamEvent()

        data class Error(val message: String) : StreamEvent()

        data class MessageIds(
            val sessionId: String?,
            val userMessageId: String?,
            val assistantMessageId: String?,
            val clientMessageId: String?,
            val clientAssistantMessageId: String?,
            val sessionUpdatedAtMs: Long?
        ) : StreamEvent()

        data class SessionRecency(
            val sessionId: String?,
            val sessionUpdatedAtMs: Long?
        ) : StreamEvent()

        data class Meta(val name: String, val payload: JsonObject) : StreamEvent()

        data object Done : StreamEvent()
    }
}
