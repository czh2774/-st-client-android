package com.stproject.client.android.core.network

object ApiErrorMessageMapper {
    fun toUserMessage(
        httpStatus: Int?,
        apiCode: Int?,
        errorDetailCode: String?,
        fallback: String?
    ): String {
        when (errorDetailCode) {
            "INSUFFICIENT_BALANCE" -> return "insufficient balance"
            "INVALID_PRODUCT" -> return "invalid product"
            "rate_limit_exceeded" -> return "rate limited"
        }
        return when {
            httpStatus == 401 -> "unauthorized"
            httpStatus == 402 -> "insufficient balance"
            httpStatus == 403 -> "forbidden"
            httpStatus == 423 -> "account locked"
            httpStatus == 429 -> "rate limited"
            httpStatus != null && httpStatus >= 500 -> "server error"
            apiCode != null && apiCode != 200 -> "request failed"
            !fallback.isNullOrBlank() -> fallback
            else -> "request failed"
        }
    }
}
