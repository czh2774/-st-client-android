package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ReportReasonMetaDto
import com.stproject.client.android.core.network.ReportRequestDto
import com.stproject.client.android.core.network.StReportApi
import com.stproject.client.android.domain.model.ReportReasonMeta
import com.stproject.client.android.domain.repository.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpReportRepository
    @Inject
    constructor(
        private val api: StReportApi,
        private val apiClient: ApiClient,
    ) : ReportRepository {
        override suspend fun getReasonMeta(): ReportReasonMeta {
            val dto = apiClient.call { api.getReasons() }
            return dto.toDomain()
        }

        override suspend fun submitReport(
            targetType: String,
            targetId: String,
            reasons: List<String>,
            detail: String?,
            sessionId: String?,
        ) {
            val resolvedType = targetType.trim().ifEmpty { "character" }
            apiClient.call {
                api.createReport(
                    ReportRequestDto(
                        targetType = resolvedType,
                        targetId = targetId,
                        reasons = reasons,
                        detail = detail?.trim()?.takeIf { it.isNotEmpty() },
                        sessionId = sessionId?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
            }
        }

        private fun ReportReasonMetaDto.toDomain(): ReportReasonMeta =
            ReportReasonMeta(
                reasons = reasons?.filter { it.isNotBlank() } ?: emptyList(),
                requiresDetailReasons = requiresDetailReasons?.filter { it.isNotBlank() } ?: listOf("other"),
                maxDetailLength = maxDetailLength ?: 1000,
            )
    }
