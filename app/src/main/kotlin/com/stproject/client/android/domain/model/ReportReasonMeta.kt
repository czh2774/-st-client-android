package com.stproject.client.android.domain.model

data class ReportReasonMeta(
    val reasons: List<String>,
    val requiresDetailReasons: List<String>,
    val maxDetailLength: Int,
)
