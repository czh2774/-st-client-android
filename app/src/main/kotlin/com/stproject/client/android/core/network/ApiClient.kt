package com.stproject.client.android.core.network

import com.google.gson.Gson
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor() {
    private val gson = Gson()

    suspend fun <T> call(block: suspend () -> ApiEnvelope<T>): T {
        try {
            val env = block()
            val code = env.code
            val data = env.data
            if (code != 200) {
                throw ApiException(
                    httpStatus = 200,
                    apiCode = code,
                    message = "api error (code=$code)"
                )
            }
            return data ?: throw ApiException(
                httpStatus = 200,
                apiCode = code,
                message = "api error: missing data"
            )
        } catch (e: HttpException) {
            val httpStatus = e.code()
            val raw = e.response()?.errorBody()?.string()
            val parsed = raw?.let { runCatching { gson.fromJson(it, ApiError::class.java) }.getOrNull() }

            val errorDetailCode = parsed?.errorDetail?.code
            val msg = parsed?.message ?: parsed?.msg ?: parsed?.error ?: e.message()

            throw ApiException(
                httpStatus = httpStatus,
                apiCode = parsed?.code,
                errorDetailCode = errorDetailCode,
                message = msg ?: "http error ($httpStatus)"
            )
        }
    }
}


