package com.stproject.client.android.core.network

data class ReportReasonMetaDto(
    val reasons: List<String>? = null,
    val highPriorityReasons: List<String>? = null,
    val requiresDetailReasons: List<String>? = null,
    val maxDetailLength: Int? = null,
    val targetTypes: List<String>? = null,
    val defaultTargetType: String? = null,
)

data class ReportRequestDto(
    val targetType: String,
    val targetId: String,
    val reasons: List<String>,
    val detail: String? = null,
    val sessionId: String? = null,
    val characterName: String? = null,
)

data class ReportResponseDto(
    val ticketId: String? = null,
)
