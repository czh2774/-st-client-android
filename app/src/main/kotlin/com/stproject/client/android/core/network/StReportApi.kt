package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StReportApi {
    @GET("reports/reasons")
    suspend fun getReasons(): ApiEnvelope<ReportReasonMetaDto>

    @POST("reports")
    suspend fun createReport(
        @Body request: ReportRequestDto,
    ): ApiEnvelope<ReportResponseDto>
}
