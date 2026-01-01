package com.stproject.client.android.core.network

/**
 * Standard v1 API envelope used by st-server-go:
 *   { "code": number, "data": T }
 */
data class ApiEnvelope<T>(
    val code: Int,
    val data: T?,
)
