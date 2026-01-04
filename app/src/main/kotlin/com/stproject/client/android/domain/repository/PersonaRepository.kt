package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.Persona

interface PersonaRepository {
    suspend fun listPersonas(): List<Persona>

    suspend fun createPersona(
        name: String,
        description: String?,
        avatarUrl: String?,
        isDefault: Boolean,
    ): Persona

    suspend fun updatePersona(
        personaId: String,
        name: String,
        description: String?,
        avatarUrl: String?,
        isDefault: Boolean,
    ): Persona

    suspend fun deletePersona(personaId: String)
}
