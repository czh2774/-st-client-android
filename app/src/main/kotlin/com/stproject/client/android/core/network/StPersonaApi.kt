package com.stproject.client.android.core.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface StPersonaApi {
    @GET("personas")
    suspend fun listPersonas(): ApiEnvelope<Any>

    @POST("personas")
    suspend fun createPersona(
        @Body request: CreatePersonaRequestDto,
    ): ApiEnvelope<PersonaDto>

    @PUT("personas/{id}")
    suspend fun updatePersona(
        @Path("id") personaId: String,
        @Body request: UpdatePersonaRequestDto,
    ): ApiEnvelope<PersonaDto>

    @DELETE("personas/{id}")
    suspend fun deletePersona(
        @Path("id") personaId: String,
    ): ApiEnvelope<OkResponseDto>
}
