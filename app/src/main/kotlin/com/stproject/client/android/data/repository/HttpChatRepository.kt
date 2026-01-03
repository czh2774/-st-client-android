package com.stproject.client.android.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.stproject.client.android.BuildConfig
import com.stproject.client.android.core.a2ui.A2UIClientCapabilitiesProvider
import com.stproject.client.android.core.a2ui.A2UIMessage
import com.stproject.client.android.core.a2ui.A2UIRuntimeReducer
import com.stproject.client.android.core.a2ui.A2UIRuntimeState
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.network.A2UIEventRequestDto
import com.stproject.client.android.core.network.A2UIMessageMetadataDto
import com.stproject.client.android.core.network.A2UIUserActionDto
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.network.ChatCompletionRequestDto
import com.stproject.client.android.core.network.ChatMessageDto
import com.stproject.client.android.core.network.ChatSessionDetailDto
import com.stproject.client.android.core.network.ChatSessionMetadataPatchDto
import com.stproject.client.android.core.network.CreateChatSessionRequestDto
import com.stproject.client.android.core.network.DialogDeleteRequestDto
import com.stproject.client.android.core.network.DialogStreamRequestDto
import com.stproject.client.android.core.network.DialogSwipeDeleteRequestDto
import com.stproject.client.android.core.network.DialogSwipeRequestDto
import com.stproject.client.android.core.network.DialogVariablesRequestDto
import com.stproject.client.android.core.network.StApi
import com.stproject.client.android.core.network.StBaseUrlProvider
import com.stproject.client.android.core.network.UpdateChatSessionRequestDto
import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.core.preferences.UserPreferencesStore
import com.stproject.client.android.data.local.ChatMessageDao
import com.stproject.client.android.data.local.ChatMessageEntity
import com.stproject.client.android.data.local.ChatSessionDao
import com.stproject.client.android.data.local.ChatSessionEntity
import com.stproject.client.android.domain.model.A2UIAction
import com.stproject.client.android.domain.model.A2UIActionResult
import com.stproject.client.android.domain.model.ChatMessage
import com.stproject.client.android.domain.model.ChatRole
import com.stproject.client.android.domain.model.ChatSessionSummary
import com.stproject.client.android.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
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
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class HttpChatRepository
    @Inject
    constructor(
        private val api: StApi,
        private val apiClient: ApiClient,
        private val okHttpClient: OkHttpClient,
        private val baseUrlProvider: StBaseUrlProvider,
        private val messageDao: ChatMessageDao,
        private val sessionDao: ChatSessionDao,
        private val sessionStore: ChatSessionStore,
        private val userPreferencesStore: UserPreferencesStore,
    ) : ChatRepository {
        private companion object {
            private const val MAX_CACHED_SESSIONS = 100
        }

        private val gson = Gson()
        private val swipesType = object : TypeToken<List<String>>() {}.type
        private val metadataType = object : TypeToken<Map<String, Any>>() {}.type
        private var latestSessionVariables: Map<String, Any> = emptyMap()
        private val sseClient =
            okHttpClient.newBuilder()
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

        override val a2uiState: Flow<A2UIRuntimeState?> =
            sessionIdFlow
                .flatMapLatest { sessionId ->
                    if (sessionId.isNullOrBlank()) {
                        flowOf(null)
                    } else {
                        streamA2uiRuntime(sessionId)
                    }
                }.catch { emit(null) }

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
                        isStreaming = false,
                    ),
                )
                updateSessionRecency(sessionId, now)
                messageDao.upsert(
                    ChatMessageEntity(
                        id = assistantId,
                        sessionId = sessionId,
                        serverId = null,
                        role = ChatRole.Assistant.name,
                        content = "",
                        createdAt = now + 1,
                        isStreaming = true,
                    ),
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

        override suspend fun sendA2UIAction(action: A2UIAction): A2UIActionResult {
            val name = action.name.trim()
            if (name.isEmpty()) {
                return A2UIActionResult(accepted = false, reason = "missing_action")
            }
            val context =
                action.context.entries
                    .mapNotNull { (key, value) -> value?.let { key to it } }
                    .toMap()
                    .ifEmpty { emptyMap() }
            val request =
                A2UIEventRequestDto(
                    userAction =
                        A2UIUserActionDto(
                            name = name,
                            surfaceId = action.surfaceId?.trim()?.takeIf { it.isNotEmpty() },
                            sourceComponentId = action.sourceComponentId?.trim()?.takeIf { it.isNotEmpty() },
                            timestamp = Instant.now().toString(),
                            context = context,
                        ),
                    metadata =
                        A2UIMessageMetadataDto(
                            a2uiClientCapabilities = A2UIClientCapabilitiesProvider.capabilities,
                        ),
                )
            val data = apiClient.call { api.sendA2UIEvent(request) }
            return A2UIActionResult(
                accepted = data.accepted,
                reason = data.reason,
            )
        }

        override suspend fun clearLocalSession() {
            withContext(Dispatchers.IO) {
                messageDao.deleteAll()
                sessionDao.deleteAll()
            }
            sessionStore.clear()
            sessionIdFlow.value = null
        }

        override suspend fun startNewSession(
            memberId: String,
            shareCode: String?,
        ) {
            val cleanMemberId = memberId.trim()
            if (cleanMemberId.isEmpty()) {
                throw ApiException(message = "member id is required", userMessage = "missing character")
            }
            sessionStore.setSessionId(null)
            sessionStore.setClientSessionId(null)
            sessionStore.setSessionUpdatedAtMs(null)
            sessionStore.setPrimaryMemberId(cleanMemberId)
            sessionStore.setShareCode(shareCode)
            sessionIdFlow.value = null
        }

        override suspend fun openSession(
            sessionId: String,
            primaryMemberId: String?,
        ) {
            val cleanSessionId = sessionId.trim()
            if (cleanSessionId.isEmpty()) {
                throw ApiException(message = "session id is required", userMessage = "missing session")
            }
            sessionStore.setSessionId(cleanSessionId)
            sessionStore.setPrimaryMemberId(primaryMemberId)
            sessionIdFlow.value = cleanSessionId
            ensureSessionRecorded(cleanSessionId, primaryMemberId)
            syncMessages(cleanSessionId)
        }

        override suspend fun listSessions(
            limit: Int,
            offset: Int,
        ): List<ChatSessionSummary> {
            return try {
                val resp = apiClient.call { api.listChatSessions(limit = limit, offset = offset) }
                val items = resp.items ?: emptyList()
                val summaries =
                    items.map { dto ->
                        val name =
                            dto.primaryMemberName
                                ?: dto.characterName
                                ?: dto.title
                                ?: "Untitled"
                        ChatSessionSummary(
                            sessionId = dto.sessionId,
                            primaryMemberId = dto.primaryMemberId,
                            displayName = name,
                            updatedAt = dto.updatedAt ?: dto.lastMessageAt,
                            primaryMemberIsNsfw = null,
                        )
                    }
                withContext(Dispatchers.IO) {
                    val entities =
                        items.mapNotNull { dto ->
                            val id = dto.sessionId.trim()
                            if (id.isEmpty()) return@mapNotNull null
                            val name =
                                dto.primaryMemberName
                                    ?: dto.characterName
                                    ?: dto.title
                                    ?: "Untitled"
                            val updatedAt = dto.updatedAt ?: dto.lastMessageAt
                            val updatedAtMs = parseCreatedAt(updatedAt) ?: System.currentTimeMillis()
                            ChatSessionEntity(
                                sessionId = id,
                                primaryMemberId = dto.primaryMemberId,
                                displayName = name,
                                updatedAt = updatedAt,
                                updatedAtMs = updatedAtMs,
                            )
                        }
                    if (entities.isNotEmpty()) {
                        sessionDao.upsertAll(entities)
                        trimSessionsIfNeeded()
                    }
                }
                summaries
            } catch (e: ApiException) {
                val cached =
                    withContext(Dispatchers.IO) {
                        sessionDao.listSessions(limit = limit, offset = offset)
                    }
                cached.map { it.toSummary() }
            }
        }

        override suspend fun getLastSessionSummary(): ChatSessionSummary? {
            val storedId = sessionStore.getSessionId()?.trim().orEmpty()
            val cached =
                withContext(Dispatchers.IO) {
                    if (storedId.isNotEmpty()) {
                        sessionDao.getSession(storedId)
                    } else {
                        null
                    }
                }
            if (cached != null) return cached.toSummary()
            val fallback =
                withContext(Dispatchers.IO) {
                    sessionDao.listSessions(limit = 1, offset = 0).firstOrNull()
                }
            return fallback?.toSummary()
        }

        override suspend fun regenerateMessage(messageId: String) {
            val sessionId = requireSessionId()
            val serverId = resolveServerMessageId(messageId)
            val original =
                withContext(Dispatchers.IO) {
                    messageDao.getByServerId(serverId)
                }
            withContext(Dispatchers.IO) {
                messageDao.updateContentByServerId(serverId, "", true)
            }
            try {
                streamDialog(
                    endpoint = "dialogs/regenerate",
                    request =
                        DialogStreamRequestDto(
                            messageId = serverId,
                            sessionId = sessionId,
                            worldInfoMinActivations = 2,
                            worldInfoMinActivationsDepthMax = 10,
                            globalVariables = userPreferencesStore.getGlobalVariables(),
                        ),
                    serverMessageId = serverId,
                    initialContent = "",
                    fallbackContent = original?.content ?: "",
                )
            } finally {
                syncMessages(sessionId)
            }
        }

        override suspend fun continueMessage(messageId: String) {
            val sessionId = requireSessionId()
            val serverId = resolveServerMessageId(messageId)
            val original =
                withContext(Dispatchers.IO) {
                    messageDao.getByServerId(serverId)
                }
            try {
                streamDialog(
                    endpoint = "dialogs/continue",
                    request =
                        DialogStreamRequestDto(
                            messageId = serverId,
                            sessionId = sessionId,
                            worldInfoMinActivations = 2,
                            worldInfoMinActivationsDepthMax = 10,
                            globalVariables = userPreferencesStore.getGlobalVariables(),
                        ),
                    serverMessageId = serverId,
                    initialContent = original?.content.orEmpty(),
                    fallbackContent = original?.content.orEmpty(),
                )
            } finally {
                syncMessages(sessionId)
            }
        }

        override suspend fun deleteMessage(
            messageId: String,
            deleteAfter: Boolean,
        ) {
            val sessionId = requireSessionId()
            val serverId = resolveServerMessageId(messageId)
            apiClient.call {
                api.deleteDialog(
                    DialogDeleteRequestDto(
                        messageId = serverId,
                        sessionId = sessionId,
                        deleteAfter = deleteAfter,
                    ),
                )
            }
            syncMessages(sessionId)
        }

        override suspend fun setActiveSwipe(
            messageId: String,
            swipeId: Int,
        ) {
            val sessionId = requireSessionId()
            val serverId = resolveServerMessageId(messageId)
            val resp =
                apiClient.call {
                    api.setActiveSwipe(
                        DialogSwipeRequestDto(
                            messageId = serverId,
                            sessionId = sessionId,
                            swipeId = swipeId,
                        ),
                    )
                }
            updateSwipeState(serverId, resp.content, resp.swipes, resp.swipeId, resp.metadata)
        }

        override suspend fun deleteSwipe(
            messageId: String,
            swipeId: Int?,
        ) {
            val sessionId = requireSessionId()
            val serverId = resolveServerMessageId(messageId)
            val resp =
                apiClient.call {
                    api.deleteSwipe(
                        DialogSwipeDeleteRequestDto(
                            messageId = serverId,
                            sessionId = sessionId,
                            swipeId = swipeId,
                        ),
                    )
                }
            updateSwipeState(serverId, resp.content, resp.swipes, resp.swipeId, resp.metadata)
        }

        override suspend fun loadSessionVariables(): Map<String, Any> {
            val sessionId = sessionStore.getSessionId()?.trim().orEmpty()
            if (sessionId.isEmpty()) {
                throw ApiException(message = "missing session", userMessage = "missing session")
            }
            val resp = apiClient.call { api.getChatSession(sessionId) }
            trackSessionUpdatedAt(resp)
            return parseSessionVariables(resp).also { latestSessionVariables = it }
        }

        override suspend fun updateSessionVariables(variables: Map<String, Any>) {
            val sessionId = sessionStore.getSessionId()?.trim().orEmpty()
            if (sessionId.isEmpty()) {
                throw ApiException(message = "missing session", userMessage = "missing session")
            }
            val request =
                UpdateChatSessionRequestDto(
                    metadata = ChatSessionMetadataPatchDto(xbVars = variables),
                )
            val resp = apiClient.call { api.updateChatSession(sessionId, request) }
            trackSessionUpdatedAt(resp)
            latestSessionVariables = variables
        }

        override suspend fun updateMessageVariables(
            messageId: String,
            swipesData: List<Map<String, Any>>,
        ) {
            val serverId = resolveServerMessageId(messageId)
            apiClient.call {
                api.updateDialogVariables(
                    DialogVariablesRequestDto(
                        messageId = serverId,
                        swipesData = swipesData,
                    ),
                )
            }
            updateMessageMetadata(serverId, mapOf("swipes_data" to swipesData))
        }

        private suspend fun ensureSessionId(): String {
            val existing = sessionStore.getSessionId()?.trim()?.takeIf { it.isNotEmpty() }
            if (existing != null) {
                if (sessionStore.getPrimaryMemberId().isNullOrBlank()) {
                    val fallbackMemberId = BuildConfig.DEFAULT_CHARACTER_ID.trim()
                    if (fallbackMemberId.isNotEmpty()) {
                        sessionStore.setPrimaryMemberId(fallbackMemberId)
                    }
                }
                return existing
            }

            val memberId =
                sessionStore.getPrimaryMemberId()?.trim().orEmpty().ifEmpty {
                    BuildConfig.DEFAULT_CHARACTER_ID.trim()
                }
            if (memberId.isEmpty()) {
                Timber.w("primary member id missing; set ST_DEFAULT_CHARACTER_ID or start from explore")
                throw ApiException(
                    message = "missing member id",
                    userMessage = "character not selected",
                )
            }

            val clientSessionId =
                sessionStore.getClientSessionId()?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "android_${UUID.randomUUID()}"
            if (sessionStore.getClientSessionId().isNullOrBlank()) {
                sessionStore.setClientSessionId(clientSessionId)
            }
            sessionStore.setPrimaryMemberId(memberId)

            val shareCode = sessionStore.getShareCode()?.trim()?.takeIf { it.isNotEmpty() }
            val presetId = userPreferencesStore.getModelPresetId()
            val created =
                apiClient.call {
                    api.createChatSession(
                        CreateChatSessionRequestDto(
                            members = listOf(memberId),
                            greetingIndex = 0,
                            clientSessionId = clientSessionId,
                            presetId = presetId,
                            shareCode = shareCode,
                        ),
                    )
                }
            val newId = created.sessionId.trim()
            if (newId.isEmpty()) {
                Timber.w("session create returned empty id")
                throw ApiException(message = "empty session id", userMessage = "session creation failed")
            }
            sessionStore.setSessionId(newId)
            ensureSessionRecorded(newId, memberId)
            if (shareCode != null) {
                sessionStore.setShareCode(null)
            }
            sessionIdFlow.value = newId
            return newId
        }

        private fun requireSessionId(): String {
            val existing = sessionStore.getSessionId()?.trim()?.takeIf { it.isNotEmpty() }
            if (existing != null) return existing
            throw ApiException(message = "session id is required", userMessage = "missing session")
        }

        private suspend fun syncMessages(sessionId: String) {
            val response =
                apiClient.call {
                    api.listChatMessages(sessionId = sessionId, limit = 50, beforeMessageId = null)
                }
            val items = response.items ?: emptyList()
            val now = System.currentTimeMillis()
            val entities =
                items.mapIndexedNotNull { index, dto ->
                    dto.toEntity(sessionId, now + index)
                }
            withContext(Dispatchers.IO) {
                messageDao.deleteBySessionId(sessionId)
                if (entities.isNotEmpty()) {
                    messageDao.upsertAll(entities)
                }
            }
        }

        private suspend fun resolveServerMessageId(messageId: String): String {
            val clean = messageId.trim()
            if (clean.isEmpty()) {
                throw ApiException(message = "message id is required", userMessage = "missing message")
            }
            val byServer =
                withContext(Dispatchers.IO) {
                    messageDao.getByServerId(clean)
                }
            if (byServer != null) return clean
            val byId = withContext(Dispatchers.IO) { messageDao.getById(clean) }
            val serverId = byId?.serverId?.trim()?.takeIf { it.isNotEmpty() }
            return serverId ?: throw ApiException(
                message = "message not synced",
                userMessage = "message not ready",
            )
        }

        private suspend fun streamDialog(
            endpoint: String,
            request: DialogStreamRequestDto,
            serverMessageId: String,
            initialContent: String,
            fallbackContent: String,
        ) {
            val streamFlow = streamDialogCompletion(endpoint, request)
            val builder = StringBuilder(initialContent)
            var hasDelta = false
            var streamFailed = false
            try {
                streamFlow.collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> {
                            hasDelta = true
                            builder.append(event.content)
                            withContext(Dispatchers.IO) {
                                messageDao.updateContentByServerId(
                                    serverMessageId,
                                    builder.toString(),
                                    true,
                                )
                            }
                        }
                        StreamEvent.Done -> {
                            withContext(Dispatchers.IO) {
                                messageDao.updateContentByServerId(
                                    serverMessageId,
                                    builder.toString(),
                                    false,
                                )
                            }
                        }
                        is StreamEvent.MessageIds -> Unit
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
                streamFailed = true
                Timber.w(e, "dialog stream failed")
                withContext(Dispatchers.IO) {
                    messageDao.updateContentByServerId(serverMessageId, fallbackContent, false)
                }
                if (e is ApiException) throw e
                throw ApiException(message = "stream failed")
            } finally {
                if (!hasDelta && !streamFailed) {
                    withContext(Dispatchers.IO) {
                        messageDao.updateContentByServerId(serverMessageId, fallbackContent, false)
                    }
                }
            }
        }

        private suspend fun fallbackCompletion(
            sessionId: String,
            content: String,
            userId: String,
            assistantId: String,
        ) {
            val resp =
                apiClient.call {
                    api.createChatCompletion(
                        sessionId = sessionId,
                        request =
                            ChatCompletionRequestDto(
                                message = content,
                                stream = false,
                                worldInfoMinActivations = 2,
                                worldInfoMinActivationsDepthMax = 10,
                                clientMessageId = userId,
                                clientAssistantMessageId = assistantId,
                                latestVariables = latestSessionVariables,
                                globalVariables = userPreferencesStore.getGlobalVariables(),
                            ),
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
            assistantId: String,
        ): Flow<StreamEvent> =
            callbackFlow {
                val payload =
                    gson.toJson(
                        ChatCompletionRequestDto(
                            message = content,
                            stream = true,
                            worldInfoMinActivations = 2,
                            worldInfoMinActivationsDepthMax = 10,
                            clientMessageId = userId,
                            clientAssistantMessageId = assistantId,
                            latestVariables = latestSessionVariables,
                            globalVariables = userPreferencesStore.getGlobalVariables(),
                        ),
                    )
                val request =
                    Request.Builder()
                        .url("${baseUrlProvider.baseUrl()}chats/$sessionId/completion")
                        .header("Accept", "text/event-stream")
                        .header("X-ST-SSE-Protocol", "1")
                        .post(payload.toRequestBody("application/json".toMediaType()))
                        .build()

                val listener =
                    object : EventSourceListener() {
                        override fun onEvent(
                            eventSource: EventSource,
                            id: String?,
                            type: String?,
                            data: String,
                        ) {
                            val parsed = parseSseData(data) ?: return
                            trySend(parsed)
                            if (parsed == StreamEvent.Done) {
                                eventSource.cancel()
                            }
                        }

                        override fun onFailure(
                            eventSource: EventSource,
                            t: Throwable?,
                            response: Response?,
                        ) {
                            close(t ?: ApiException(message = "stream failed"))
                        }
                    }
                val factory = EventSources.createFactory(sseClient)
                val eventSource = factory.newEventSource(request, listener)
                awaitClose { eventSource.cancel() }
            }

        private fun streamDialogCompletion(
            endpoint: String,
            request: DialogStreamRequestDto,
        ): Flow<StreamEvent> =
            callbackFlow {
                val payload = gson.toJson(request)
                val requestBody =
                    Request.Builder()
                        .url("${baseUrlProvider.baseUrl()}$endpoint")
                        .header("Accept", "text/event-stream")
                        .header("X-ST-SSE-Protocol", "1")
                        .post(payload.toRequestBody("application/json".toMediaType()))
                        .build()

                val listener =
                    object : EventSourceListener() {
                        override fun onEvent(
                            eventSource: EventSource,
                            id: String?,
                            type: String?,
                            data: String,
                        ) {
                            val parsed = parseSseData(data) ?: return
                            trySend(parsed)
                            if (parsed == StreamEvent.Done) {
                                eventSource.cancel()
                            }
                        }

                        override fun onFailure(
                            eventSource: EventSource,
                            t: Throwable?,
                            response: Response?,
                        ) {
                            close(t ?: ApiException(message = "stream failed"))
                        }
                    }
                val factory = EventSources.createFactory(sseClient)
                val eventSource = factory.newEventSource(requestBody, listener)
                awaitClose { eventSource.cancel() }
            }

        private fun streamA2uiRuntime(sessionId: String): Flow<A2UIRuntimeState?> =
            callbackFlow {
                var currentState = A2UIRuntimeState()
                val request =
                    Request.Builder()
                        .url("${baseUrlProvider.baseUrl()}a2ui/stream?sessionId=$sessionId")
                        .header("Accept", "text/event-stream")
                        .header("X-ST-SSE-Protocol", "1")
                        .get()
                        .build()
                val listener =
                    object : EventSourceListener() {
                        override fun onEvent(
                            eventSource: EventSource,
                            id: String?,
                            type: String?,
                            data: String,
                        ) {
                            if (data == "[DONE]") {
                                close()
                                eventSource.cancel()
                                return
                            }
                            val msg = runCatching { gson.fromJson(data, A2UIMessage::class.java) }.getOrNull() ?: return
                            currentState = A2UIRuntimeReducer.reduce(currentState, msg)
                            trySend(currentState.takeUnless { it.isEmpty })
                        }

                        override fun onFailure(
                            eventSource: EventSource,
                            t: Throwable?,
                            response: Response?,
                        ) {
                            close(t ?: ApiException(message = "a2ui stream failed"))
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
                            sessionUpdatedAtMs = json.readLong("sessionUpdatedAtMs"),
                        )
                    }
                    "session_recency" -> {
                        StreamEvent.SessionRecency(
                            sessionId = json.readString("sessionId"),
                            sessionUpdatedAtMs = json.readLong("sessionUpdatedAtMs"),
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
            assistantId: String,
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
                    event.clientAssistantMessageId,
                )
            }
            if (!event.sessionId.isNullOrBlank() && event.sessionId != sessionStore.getSessionId()) {
                Timber.w(
                    "message_ids sessionId mismatch (local=%s, remote=%s)",
                    sessionStore.getSessionId(),
                    event.sessionId,
                )
            }
            if (event.sessionUpdatedAtMs != null && event.sessionUpdatedAtMs > 0) {
                sessionStore.setSessionUpdatedAtMs(event.sessionUpdatedAtMs)
                val sessionId = sessionStore.getSessionId()
                if (!sessionId.isNullOrBlank()) {
                    updateSessionRecency(sessionId, event.sessionUpdatedAtMs)
                }
            }
        }

        private suspend fun handleSessionRecency(event: StreamEvent.SessionRecency) {
            if (event.sessionUpdatedAtMs != null && event.sessionUpdatedAtMs > 0) {
                sessionStore.setSessionUpdatedAtMs(event.sessionUpdatedAtMs)
                val sessionId =
                    event.sessionId
                        ?: sessionStore.getSessionId()
                if (!sessionId.isNullOrBlank()) {
                    updateSessionRecency(sessionId, event.sessionUpdatedAtMs)
                }
            }
            if (!event.sessionId.isNullOrBlank() && event.sessionId != sessionStore.getSessionId()) {
                Timber.w(
                    "session_recency sessionId mismatch (local=%s, remote=%s)",
                    sessionStore.getSessionId(),
                    event.sessionId,
                )
            }
        }

        private fun handleMetaEvent(event: StreamEvent.Meta) {
            if (!BuildConfig.DEBUG) return
            when (event.name) {
                "usage" -> {
                    val prompt =
                        event.payload.readLong("promptTokens")
                            ?: event.payload.readLong("prompt_tokens")
                    val completion =
                        event.payload.readLong("completionTokens")
                            ?: event.payload.readLong("completion_tokens")
                    val total =
                        event.payload.readLong("totalTokens")
                            ?: event.payload.readLong("total_tokens")
                    Timber.d("sse usage prompt=%s completion=%s total=%s", prompt, completion, total)
                }
                "emotion",
                "reasoning",
                "worldinfo_automations",
                "worldinfo_outlets",
                "speaker",
                "group_rotation",
                "assistant_segments",
                "prompt_itemization",
                -> {
                    Timber.d("sse meta event=%s", event.name)
                }
                else -> {
                    Timber.d("sse meta event=%s", event.name)
                }
            }
        }

        private fun ChatMessageEntity.toDomain(): ChatMessage {
            val role = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.Assistant)
            return ChatMessage(
                id = id,
                serverId = serverId,
                role = role,
                content = content,
                createdAt = createdAt,
                isStreaming = isStreaming,
                swipes = decodeSwipes(swipesJson),
                swipeId = swipeId,
                metadata = decodeMetadata(metadataJson),
            )
        }

        private fun ChatMessageDto.toEntity(
            sessionId: String,
            fallbackCreatedAt: Long,
        ): ChatMessageEntity? {
            val rawId = (messageId ?: id)?.trim().orEmpty()
            if (rawId.isEmpty()) return null
            val roleValue =
                when (role?.trim()?.lowercase()) {
                    "system" -> ChatRole.System
                    "user" -> ChatRole.User
                    "assistant" -> ChatRole.Assistant
                    else -> ChatRole.Assistant
                }
            val createdAtMs = parseCreatedAt(createdAt) ?: fallbackCreatedAt
            return ChatMessageEntity(
                id = rawId,
                sessionId = sessionId,
                serverId = rawId,
                role = roleValue.name,
                content = content?.trim().orEmpty(),
                createdAt = createdAtMs,
                isStreaming = false,
                swipeId = swipeId,
                swipesJson = encodeSwipes(swipes),
                metadataJson = encodeMetadata(metadata),
            )
        }

        private fun parseCreatedAt(raw: String?): Long? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return runCatching { Instant.parse(trimmed).toEpochMilli() }.getOrNull()
        }

        private fun encodeSwipes(swipes: List<String>?): String? {
            if (swipes.isNullOrEmpty()) return null
            return gson.toJson(swipes)
        }

        private fun encodeMetadata(metadata: Map<String, Any>?): String? {
            if (metadata.isNullOrEmpty()) return null
            return gson.toJson(metadata)
        }

        private fun decodeSwipes(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                gson.fromJson<List<String>>(raw, swipesType)
            }.getOrElse { emptyList() }
        }

        private fun decodeMetadata(raw: String?): Map<String, Any>? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                gson.fromJson<Map<String, Any>>(raw, metadataType)
            }.getOrNull()
        }

        private fun trackSessionUpdatedAt(detail: ChatSessionDetailDto) {
            val updatedAt = detail.updatedAt?.trim().orEmpty()
            val updatedAtMs =
                runCatching { Instant.parse(updatedAt).toEpochMilli() }.getOrNull()
            if (updatedAtMs != null && updatedAtMs > 0) {
                sessionStore.setSessionUpdatedAtMs(updatedAtMs)
            }
        }

        private fun parseSessionVariables(detail: ChatSessionDetailDto): Map<String, Any> {
            val metadata = detail.metadata ?: return emptyMap()
            val raw = metadata["xb_vars"] ?: metadata["xbVars"] ?: metadata["variables"]
            return toStringMap(raw)
        }

        private fun toStringMap(raw: Any?): Map<String, Any> {
            if (raw !is Map<*, *>) return emptyMap()
            val out = mutableMapOf<String, Any>()
            for ((key, value) in raw) {
                if (key is String && value != null) {
                    out[key] = value
                }
            }
            return out
        }

        private suspend fun updateSwipeState(
            serverMessageId: String,
            content: String,
            swipes: List<String>?,
            swipeId: Int?,
            metadata: Map<String, Any>?,
        ) {
            withContext(Dispatchers.IO) {
                val existing = messageDao.getByServerId(serverMessageId)
                if (existing != null) {
                    val swipesJson = swipes?.let { encodeSwipes(it) } ?: existing.swipesJson
                    val metadataJson =
                        if (metadata != null) {
                            encodeMetadata(metadata)
                        } else {
                            existing.metadataJson
                        }
                    messageDao.upsert(
                        existing.copy(
                            content = content,
                            isStreaming = false,
                            swipeId = swipeId ?: existing.swipeId,
                            swipesJson = swipesJson,
                            metadataJson = metadataJson,
                        ),
                    )
                }
            }
        }

        private suspend fun updateMessageMetadata(
            serverMessageId: String,
            patch: Map<String, Any>,
        ) {
            withContext(Dispatchers.IO) {
                val existing = messageDao.getByServerId(serverMessageId) ?: return@withContext
                val current = decodeMetadata(existing.metadataJson) ?: emptyMap()
                val merged = current.toMutableMap().apply { putAll(patch) }
                val metadataJson = encodeMetadata(merged)
                messageDao.upsert(existing.copy(metadataJson = metadataJson))
            }
        }

        private suspend fun ensureSessionRecorded(
            sessionId: String,
            primaryMemberId: String?,
        ) {
            val created =
                withContext(Dispatchers.IO) {
                    val existing = sessionDao.getSession(sessionId)
                    if (existing != null) return@withContext false
                    val now = System.currentTimeMillis()
                    sessionDao.upsert(
                        ChatSessionEntity(
                            sessionId = sessionId,
                            primaryMemberId = primaryMemberId?.trim()?.takeIf { it.isNotEmpty() },
                            displayName = "Untitled",
                            updatedAt = Instant.ofEpochMilli(now).toString(),
                            updatedAtMs = now,
                        ),
                    )
                    true
                }
            if (created) {
                trimSessionsIfNeeded()
            }
        }

        private suspend fun updateSessionRecency(
            sessionId: String,
            updatedAtMs: Long?,
        ) {
            val cleanId = sessionId.trim()
            if (cleanId.isEmpty()) return
            val created =
                withContext(Dispatchers.IO) {
                    val existing = sessionDao.getSession(cleanId)
                    val now = updatedAtMs?.takeIf { it > 0 } ?: System.currentTimeMillis()
                    val displayName = existing?.displayName ?: "Untitled"
                    val memberId = existing?.primaryMemberId ?: sessionStore.getPrimaryMemberId()
                    val updatedAt = existing?.updatedAt ?: Instant.ofEpochMilli(now).toString()
                    sessionDao.upsert(
                        ChatSessionEntity(
                            sessionId = cleanId,
                            primaryMemberId = memberId?.trim()?.takeIf { it.isNotEmpty() },
                            displayName = displayName,
                            updatedAt = updatedAt,
                            updatedAtMs = now,
                        ),
                    )
                    existing == null
                }
            if (created) {
                trimSessionsIfNeeded()
            }
        }

        private suspend fun trimSessionsIfNeeded() {
            withContext(Dispatchers.IO) {
                val total = sessionDao.countSessions()
                if (total <= MAX_CACHED_SESSIONS) return@withContext
                val overflow = total - MAX_CACHED_SESSIONS
                val oldSessionIds = sessionDao.listSessionIds(limit = overflow, offset = MAX_CACHED_SESSIONS)
                if (oldSessionIds.isNotEmpty()) {
                    sessionDao.deleteByIds(oldSessionIds)
                    messageDao.deleteBySessionIds(oldSessionIds)
                }
            }
        }

        private fun ChatSessionEntity.toSummary(): ChatSessionSummary {
            return ChatSessionSummary(
                sessionId = sessionId,
                primaryMemberId = primaryMemberId,
                displayName = displayName,
                updatedAt = updatedAt,
                primaryMemberIsNsfw = null,
            )
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
                val sessionUpdatedAtMs: Long?,
            ) : StreamEvent()

            data class SessionRecency(
                val sessionId: String?,
                val sessionUpdatedAtMs: Long?,
            ) : StreamEvent()

            data class Meta(val name: String, val payload: JsonObject) : StreamEvent()

            data object Done : StreamEvent()
        }
    }
