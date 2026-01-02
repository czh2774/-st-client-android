package com.stproject.client.android.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ApiError
import com.stproject.client.android.core.network.ApiErrorMessageMapper
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.core.network.CardCreateResponseDto
import com.stproject.client.android.core.network.CardParseResponseDto
import com.stproject.client.android.core.network.CardParseTextRequestDto
import com.stproject.client.android.core.network.StCardApi
import com.stproject.client.android.core.network.StCharacterApi
import com.stproject.client.android.domain.model.CardCreateInput
import com.stproject.client.android.domain.model.CardCreateResult
import com.stproject.client.android.domain.repository.CardRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpCardRepository
    @Inject
    constructor(
        private val api: StCardApi,
        private val characterApi: StCharacterApi,
        private val apiClient: ApiClient,
    ) : CardRepository {
        private val gson = Gson()

        override suspend fun createCard(input: CardCreateInput): CardCreateResult {
            val wrapper = buildWrapper(input)
            val resp = apiClient.call { api.createCard(wrapper) }
            return resp.toDomain()
        }

        override suspend fun createCardFromWrapper(wrapper: Map<String, Any>): CardCreateResult {
            val resp = apiClient.call { api.createCard(wrapper) }
            return resp.toDomain()
        }

        override suspend fun updateCardFromWrapper(
            id: String,
            wrapper: Map<String, Any>,
        ): CardCreateResult {
            val resp = apiClient.call { api.updateCard(id, wrapper) }
            return resp.toDomain()
        }

        override suspend fun fetchCardWrapper(id: String): Map<String, Any> {
            try {
                val body = characterApi.exportCharacter(id)
                val json = body.string()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val parsed = gson.fromJson<Map<String, Any>>(json, type)
                return parsed ?: emptyMap()
            } catch (e: HttpException) {
                throw mapHttpException(e)
            }
        }

        override suspend fun parseCardFile(
            fileName: String,
            bytes: ByteArray,
        ): Map<String, Any> {
            val mediaType = "application/octet-stream".toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
            val resp = apiClient.call { api.parseFile(part) }
            return parseCardResponse(resp)
        }

        override suspend fun parseCardText(
            content: String,
            fileName: String?,
        ): Map<String, Any> {
            val resp = apiClient.call { api.parseText(CardParseTextRequestDto(content = content, fileName = fileName)) }
            return parseCardResponse(resp)
        }

        override suspend fun fetchTemplate(): Map<String, Any> {
            try {
                val body = api.getTemplate()
                val json = body.string()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val parsed = gson.fromJson<Map<String, Any>>(json, type)
                return parsed ?: emptyMap()
            } catch (e: HttpException) {
                throw mapHttpException(e)
            }
        }

        override suspend fun fetchExportPng(id: String): ByteArray {
            try {
                val body = characterApi.exportCharacterPng(id)
                return body.bytes()
            } catch (e: HttpException) {
                throw mapHttpException(e)
            }
        }

        private fun CardCreateResponseDto.toDomain(): CardCreateResult {
            val id = characterId?.trim().orEmpty()
            return CardCreateResult(
                characterId = id,
                name = name?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        private fun parseCardResponse(resp: CardParseResponseDto): Map<String, Any> {
            if (resp.success != true || resp.card == null) {
                throw ApiException(
                    httpStatus = 200,
                    apiCode = 200,
                    errorDetailCode = null,
                    message = resp.error ?: "parse failed",
                    userMessage = resp.error ?: "parse failed",
                )
            }
            return resp.card
        }

        private fun mapHttpException(e: HttpException): ApiException {
            val httpStatus = e.code()
            val raw = e.response()?.errorBody()?.string()
            val parsed = raw?.let { runCatching { gson.fromJson(it, ApiError::class.java) }.getOrNull() }
            val msg = parsed?.message ?: parsed?.msg ?: parsed?.error ?: e.message()
            val userMessage =
                ApiErrorMessageMapper.toUserMessage(
                    httpStatus = httpStatus,
                    apiCode = parsed?.code,
                    errorDetailCode = parsed?.errorDetail?.code,
                    fallback = msg,
                )
            return ApiException(
                httpStatus = httpStatus,
                apiCode = parsed?.code,
                errorDetailCode = parsed?.errorDetail?.code,
                message = msg ?: "http error ($httpStatus)",
                userMessage = userMessage,
            )
        }

        private fun buildWrapper(input: CardCreateInput): Map<String, Any> {
            val data = mutableMapOf<String, Any>()
            data["name"] = input.name
            input.description?.takeIf { it.isNotBlank() }?.let { data["description"] = it }
            input.personality?.takeIf { it.isNotBlank() }?.let { data["personality"] = it }
            input.scenario?.takeIf { it.isNotBlank() }?.let { data["scenario"] = it }
            input.firstMessage?.takeIf { it.isNotBlank() }?.let { data["first_mes"] = it }
            input.messageExample?.takeIf { it.isNotBlank() }?.let { data["mes_example"] = it }
            input.systemPrompt?.takeIf { it.isNotBlank() }?.let { data["system_prompt"] = it }
            input.postHistoryInstructions?.takeIf { it.isNotBlank() }?.let { data["post_history_instructions"] = it }
            if (input.tags.isNotEmpty()) data["tags"] = input.tags
            data["visibility"] = input.visibility.apiValue
            data["isNsfw"] = input.isNsfw
            data["characterType"] = input.characterType.apiValue
            input.avatarUrl?.takeIf { it.isNotBlank() }?.let { data["avatar"] = it }

            val wrapper = mutableMapOf<String, Any>()
            wrapper["spec"] = "chara_card_v3"
            wrapper["spec_version"] = "3.0"
            wrapper["data"] = data
            return wrapper
        }
    }
