package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.ModelPresetDto
import com.stproject.client.android.core.network.StPresetApi
import com.stproject.client.android.domain.model.ModelPreset
import com.stproject.client.android.domain.repository.PresetRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpPresetRepository
    @Inject
    constructor(
        private val api: StPresetApi,
        private val apiClient: ApiClient,
    ) : PresetRepository {
        override suspend fun listPresets(seriesId: String?): List<ModelPreset> {
            val response = apiClient.call { api.listPresets(seriesId?.trim()?.takeIf { it.isNotEmpty() }) }
            return response.items
                ?.mapNotNull { it.toDomain() }
                ?.sortedBy { it.sortOrder }
                ?: emptyList()
        }

        private fun ModelPresetDto.toDomain(): ModelPreset? {
            val id = id?.trim().orEmpty()
            val displayName = displayName?.trim().orEmpty()
            if (id.isEmpty() || displayName.isEmpty()) return null
            return ModelPreset(
                id = id,
                displayName = displayName,
                subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                provider = provider?.trim()?.takeIf { it.isNotEmpty() },
                modelName = modelName?.trim()?.takeIf { it.isNotEmpty() },
                isDefault = isDefault == true,
                isEnabled = isEnabled != false,
                sortOrder = sortOrder ?: 0,
                tags = tags?.filter { it.isNotBlank() } ?: emptyList(),
            )
        }
    }
