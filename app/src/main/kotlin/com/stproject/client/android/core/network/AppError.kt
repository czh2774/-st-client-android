package com.stproject.client.android.core.network

sealed class AppError(
    open val message: String,
    open val retryable: Boolean,
    open val requestId: String? = null,
) {
    data class Http(
        val statusCode: Int?,
        val apiCode: Int?,
        val errorDetailCode: String?,
        override val message: String,
        override val retryable: Boolean,
        override val requestId: String? = null,
    ) : AppError(message, retryable, requestId)

    data class Network(
        override val message: String,
        override val retryable: Boolean = true,
        override val requestId: String? = null,
    ) : AppError(message, retryable, requestId)

    data class Billing(
        val code: Int?,
        override val message: String,
        override val retryable: Boolean = true,
        override val requestId: String? = null,
    ) : AppError(message, retryable, requestId)

    data class Validation(
        override val message: String,
    ) : AppError(message, retryable = false)

    data class Unknown(
        override val message: String,
    ) : AppError(message, retryable = false)
}

fun AppError.userMessage(): String = message

fun ApiException.toAppError(): AppError {
    val status = httpStatus
    val api = apiCode
    val detail = errorDetailCode
    val msg = userMessage?.takeIf { it.isNotBlank() } ?: message
    val retryable =
        when {
            detail == "rate_limit_exceeded" -> true
            status == 408 || status == 429 -> true
            status != null && status >= 500 -> true
            else -> false
        }
    return when {
        status != null || api != null || detail != null ->
            AppError.Http(
                statusCode = status,
                apiCode = api,
                errorDetailCode = detail,
                message = msg,
                retryable = retryable,
            )
        isNetworkError(msg) -> AppError.Network(message = msg)
        userMessage != null -> AppError.Validation(message = msg)
        else -> AppError.Unknown(message = msg)
    }
}

private fun isNetworkError(message: String): Boolean {
    val normalized = message.trim().lowercase()
    return normalized.startsWith("network error") || normalized.contains("timeout")
}
