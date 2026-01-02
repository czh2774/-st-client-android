package com.stproject.client.android.core.network

import com.google.gson.Gson
import com.stproject.client.android.core.common.rethrowIfCancellation
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient
    @Inject
    constructor() {
        private val gson = Gson()

        suspend fun <T> call(block: suspend () -> ApiEnvelope<T>): T {
            try {
                val env = block()
                val code = env.code
                val data = env.data
                if (code !in 200..299) {
                    val userMessage =
                        ApiErrorMessageMapper.toUserMessage(
                            httpStatus = 200,
                            apiCode = code,
                            errorDetailCode = null,
                            fallback = "request failed",
                        )
                    throw ApiException(
                        httpStatus = 200,
                        apiCode = code,
                        message = "api error (code=$code)",
                        userMessage = userMessage,
                    )
                }
                return data ?: throw ApiException(
                    httpStatus = 200,
                    apiCode = code,
                    message = "api error: missing data",
                    userMessage = "request failed",
                )
            } catch (e: HttpException) {
                val httpStatus = e.code()
                val raw = e.response()?.errorBody()?.string()
                val parsed = raw?.let { runCatching { gson.fromJson(it, ApiError::class.java) }.getOrNull() }

                val errorDetailCode = parsed?.errorDetail?.code
                val msg = parsed?.message ?: parsed?.msg ?: parsed?.error ?: e.message()
                val userMessage =
                    ApiErrorMessageMapper.toUserMessage(
                        httpStatus = httpStatus,
                        apiCode = parsed?.code,
                        errorDetailCode = errorDetailCode,
                        fallback = msg,
                    )

                throw ApiException(
                    httpStatus = httpStatus,
                    apiCode = parsed?.code,
                    errorDetailCode = errorDetailCode,
                    message = msg ?: "http error ($httpStatus)",
                    userMessage = userMessage,
                )
            } catch (e: IOException) {
                // Network failures (timeouts, no network, DNS, etc.)
                throw ApiException(
                    httpStatus = null,
                    apiCode = null,
                    errorDetailCode = null,
                    message = "network error: ${e.message ?: "io failure"}",
                    userMessage = "network error",
                )
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                // Keep a stable app-level exception; do not leak details by default.
                throw ApiException(
                    httpStatus = null,
                    apiCode = null,
                    errorDetailCode = null,
                    message = "unexpected error",
                    userMessage = "unexpected error",
                )
            }
        }
    }
