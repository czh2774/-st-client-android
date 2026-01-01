package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.ReportReasonMeta

interface ReportRepository {
    suspend fun getReasonMeta(): ReportReasonMeta

    suspend fun submitReport(
        targetId: String,
        reasons: List<String>,
        detail: String?,
        sessionId: String?,
    )
}
