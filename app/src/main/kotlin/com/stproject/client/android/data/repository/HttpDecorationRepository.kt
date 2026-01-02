package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.DecorationItemDto
import com.stproject.client.android.core.network.EquipDecorationRequestDto
import com.stproject.client.android.core.network.StUserApi
import com.stproject.client.android.domain.model.DecorationItem
import com.stproject.client.android.domain.repository.DecorationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpDecorationRepository
    @Inject
    constructor(
        private val api: StUserApi,
        private val apiClient: ApiClient,
    ) : DecorationRepository {
        override suspend fun listDecorations(): List<DecorationItem> {
            val response = apiClient.call { api.listDecorations() }
            return response.items?.mapNotNull { it.toDomain() } ?: emptyList()
        }

        override suspend fun setDecorationEquipped(
            decorationId: String,
            equip: Boolean,
        ) {
            apiClient.call {
                api.equipDecoration(
                    EquipDecorationRequestDto(
                        decorationId = decorationId,
                        equip = equip,
                    ),
                )
            }
        }

        private fun DecorationItemDto.toDomain(): DecorationItem? {
            val id = id?.trim().orEmpty()
            val name = name?.trim().orEmpty()
            val type = type?.trim().orEmpty()
            if (id.isEmpty() || name.isEmpty()) return null
            return DecorationItem(
                id = id,
                name = name,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                type = if (type.isEmpty()) "unknown" else type,
                imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                priceCredits = priceCredits,
                owned = owned == true,
                equipped = equipped == true,
            )
        }
    }
