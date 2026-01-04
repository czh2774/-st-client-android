package com.stproject.client.android.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.CreatePersonaRequestDto
import com.stproject.client.android.core.network.PersonaDto
import com.stproject.client.android.core.network.PersonaListResponseDto
import com.stproject.client.android.core.network.StPersonaApi
import com.stproject.client.android.core.network.UpdatePersonaRequestDto
import com.stproject.client.android.domain.model.Persona
import com.stproject.client.android.domain.repository.PersonaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpPersonaRepository
    @Inject
    constructor(
        private val api: StPersonaApi,
        private val apiClient: ApiClient,
    ) : PersonaRepository {
        private val gson = Gson()
        private val listType = object : TypeToken<List<PersonaDto>>() {}.type

        override suspend fun listPersonas(): List<Persona> {
            val data = apiClient.call { api.listPersonas() }
            val rawJson = gson.toJson(data)
            val list = runCatching { gson.fromJson<List<PersonaDto>>(rawJson, listType) }.getOrNull()
            val items =
                if (list != null) {
                    list
                } else {
                    runCatching {
                        gson.fromJson(rawJson, PersonaListResponseDto::class.java)?.items
                    }.getOrNull()
                }
            return items?.mapNotNull { it.toDomain() } ?: emptyList()
        }

        override suspend fun createPersona(
            name: String,
            description: String?,
            avatarUrl: String?,
            isDefault: Boolean,
        ): Persona {
            val resp =
                apiClient.call {
                    api.createPersona(
                        CreatePersonaRequestDto(
                            name = name,
                            description = description,
                            avatarUrl = avatarUrl,
                            isDefault = isDefault,
                        ),
                    )
                }
            return resp.toDomain()
                ?: Persona(
                    id = "",
                    userId = "",
                    name = name,
                    description = description.orEmpty(),
                    avatarUrl = avatarUrl,
                    isDefault = isDefault,
                    createdAt = "",
                    updatedAt = "",
                )
        }

        override suspend fun updatePersona(
            personaId: String,
            name: String,
            description: String?,
            avatarUrl: String?,
            isDefault: Boolean,
        ): Persona {
            val resp =
                apiClient.call {
                    api.updatePersona(
                        personaId,
                        UpdatePersonaRequestDto(
                            name = name,
                            description = description,
                            avatarUrl = avatarUrl,
                            isDefault = isDefault,
                        ),
                    )
                }
            return resp.toDomain()
                ?: Persona(
                    id = personaId,
                    userId = "",
                    name = name,
                    description = description.orEmpty(),
                    avatarUrl = avatarUrl,
                    isDefault = isDefault,
                    createdAt = "",
                    updatedAt = "",
                )
        }

        override suspend fun deletePersona(personaId: String) {
            apiClient.call { api.deletePersona(personaId) }
        }

        private fun PersonaDto.toDomain(): Persona? {
            val idValue = id?.trim().orEmpty()
            if (idValue.isEmpty()) return null
            val nameValue = name?.trim().orEmpty()
            if (nameValue.isEmpty()) return null
            return Persona(
                id = idValue,
                userId = userId?.trim().orEmpty(),
                name = nameValue,
                description = description?.trim().orEmpty(),
                avatarUrl = avatarUrl?.trim()?.takeIf { it.isNotEmpty() },
                isDefault = isDefault ?: false,
                createdAt = createdAt?.trim().orEmpty(),
                updatedAt = updatedAt?.trim().orEmpty(),
            )
        }
    }
