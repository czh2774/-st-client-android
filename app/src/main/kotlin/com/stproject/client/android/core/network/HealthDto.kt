package com.stproject.client.android.core.network

data class HealthDto(
    val status: String,
    val ok: Boolean,
    val service: String,
)
