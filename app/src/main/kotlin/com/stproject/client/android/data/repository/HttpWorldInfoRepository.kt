package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.StWorldInfoApi
import com.stproject.client.android.core.network.WorldInfoEntryDto
import com.stproject.client.android.core.network.WorldInfoUpsertRequestDto
import com.stproject.client.android.domain.model.WorldInfoEntry
import com.stproject.client.android.domain.model.WorldInfoEntryInput
import com.stproject.client.android.domain.repository.WorldInfoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpWorldInfoRepository
    @Inject
    constructor(
        private val api: StWorldInfoApi,
        private val apiClient: ApiClient,
    ) : WorldInfoRepository {
        override suspend fun listEntries(
            characterId: String?,
            includeGlobal: Boolean,
        ): List<WorldInfoEntry> {
            val normalizedCharacterId = characterId?.trim()?.takeIf { it.isNotEmpty() }
            val global = if (normalizedCharacterId == null) true else null
            val includeGlobalQuery = if (normalizedCharacterId != null && includeGlobal) true else null
            val resp =
                apiClient.call {
                    api.listEntries(
                        characterId = normalizedCharacterId,
                        global = global,
                        includeGlobal = includeGlobalQuery,
                    )
                }
            return resp.items?.map { it.toDomain() } ?: emptyList()
        }

        override suspend fun createEntry(input: WorldInfoEntryInput): WorldInfoEntry {
            val dto = apiClient.call { api.createEntry(input.toRequest()) }
            return dto.toDomain()
        }

        override suspend fun updateEntry(
            id: String,
            input: WorldInfoEntryInput,
        ): WorldInfoEntry {
            val dto = apiClient.call { api.updateEntry(id, input.toRequest()) }
            return dto.toDomain()
        }

        override suspend fun deleteEntry(id: String): Boolean {
            val resp = apiClient.call { api.deleteEntry(id) }
            return resp.deleted == true
        }

        private fun WorldInfoEntryDto.toDomain(): WorldInfoEntry {
            return WorldInfoEntry(
                id = id,
                characterId = characterId?.trim()?.takeIf { it.isNotEmpty() },
                keys = keys ?: emptyList(),
                secondaryKeys = secondaryKeys ?: emptyList(),
                content = content?.trim().orEmpty(),
                comment = comment?.trim()?.takeIf { it.isNotEmpty() },
                enabled = enabled ?: true,
            )
        }

        private fun WorldInfoEntryInput.toRequest(): WorldInfoUpsertRequestDto {
            return WorldInfoUpsertRequestDto(
                characterId = characterId?.trim()?.takeIf { it.isNotEmpty() },
                keys = keys,
                content = content,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() },
                enabled = enabled,
            )
        }
    }
