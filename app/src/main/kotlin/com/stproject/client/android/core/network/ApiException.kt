package com.stproject.client.android.core.network

/**
 * Stable exception type for API failures.
 *
 * - httpStatus: HTTP status code (if available)
 * - apiCode: top-level "code" in JSON envelope (if available)
 * - errorDetailCode: server-stable code (e.g. INSUFFICIENT_BALANCE)
 */
class ApiException(
    val httpStatus: Int? = null,
    val apiCode: Int? = null,
    val errorDetailCode: String? = null,
    override val message: String,
    val userMessage: String? = null,
) : RuntimeException(message)
