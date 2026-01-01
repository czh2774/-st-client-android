package com.stproject.client.android.core.network

/**
 * Error envelope shape in many v1 endpoints (non-200).
 * Keep it flexible; server may include any subset of these fields.
 */
data class ApiError(
    val code: Int? = null,
    val msg: String? = null,
    val message: String? = null,
    val error: String? = null,
    val errorDetail: ApiErrorDetail? = null,
)

data class ApiErrorDetail(
    val code: String? = null,
    val message: String? = null,
)
