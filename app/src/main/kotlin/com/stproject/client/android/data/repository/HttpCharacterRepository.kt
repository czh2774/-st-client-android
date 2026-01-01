package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.CharacterBlockRequestDto
import com.stproject.client.android.core.network.CharacterDetailDto
import com.stproject.client.android.core.network.CharacterFollowRequestDto
import com.stproject.client.android.core.network.CharacterFollowResponseDto
import com.stproject.client.android.core.network.QueryCharacterItemDto
import com.stproject.client.android.core.network.QueryCharactersRequestDto
import com.stproject.client.android.core.network.StCharacterApi
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpCharacterRepository
    @Inject
    constructor(
        private val api: StCharacterApi,
        private val apiClient: ApiClient,
    ) : CharacterRepository {
        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ): List<CharacterSummary> {
            val resp =
                apiClient.call {
                    api.queryCharacters(
                        QueryCharactersRequestDto(
                            cursor = cursor?.trim()?.takeIf { it.isNotEmpty() },
                            limit = limit,
                            sortBy = sortBy?.trim()?.takeIf { it.isNotEmpty() },
                            isNsfw = isNsfw,
                        ),
                    )
                }
            val items = resp.items ?: emptyList()
            return items.map { it.toDomain() }
        }

        override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
            val dto = apiClient.call { api.getCharacter(characterId) }
            return dto.toDomain()
        }

        override suspend fun resolveShareCode(shareCode: String): String? {
            val dto = apiClient.call { api.resolveShareCode(shareCode) }
            return dto.characterId?.trim()?.takeIf { it.isNotEmpty() }
        }

        override suspend fun generateShareCode(characterId: String): ShareCodeInfo? {
            val dto = apiClient.call { api.generateShareCode(characterId) }
            val code = dto.shareCode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val url = dto.shareUrl?.trim()?.takeIf { it.isNotEmpty() }
            return ShareCodeInfo(shareCode = code, shareUrl = url)
        }

        override suspend fun blockCharacter(
            characterId: String,
            value: Boolean,
        ) {
            apiClient.call {
                api.blockCharacter(CharacterBlockRequestDto(characterId = characterId, value = value))
            }
        }

        override suspend fun followCharacter(
            characterId: String,
            value: Boolean,
        ): CharacterFollowResult {
            val resp =
                apiClient.call {
                    api.followCharacter(CharacterFollowRequestDto(characterId = characterId, value = value))
                }
            return resp.toDomain()
        }

        private fun QueryCharacterItemDto.toDomain(): CharacterSummary =
            CharacterSummary(
                id = id,
                name = name.trim(),
                description = description?.trim().orEmpty(),
                avatarUrl = avatar?.trim()?.takeIf { it.isNotEmpty() },
                isNsfw = isNsfw ?: false,
                totalFollowers = totalFollowers ?: 0,
                isFollowed = isFollowed ?: false,
            )

        private fun CharacterDetailDto.toDomain(): CharacterDetail =
            CharacterDetail(
                id = id,
                name = name.trim(),
                description = description?.trim().orEmpty(),
                tags = tags ?: emptyList(),
                creatorName = creatorName?.trim()?.takeIf { it.isNotEmpty() },
                isNsfw = isNsfw ?: false,
                totalFollowers = totalFollowers ?: 0,
                isFollowed = isFollowed ?: false,
            )

        private fun CharacterFollowResponseDto.toDomain(): CharacterFollowResult {
            return CharacterFollowResult(
                totalFollowers = totalFollowers ?: 0,
                isFollowed = isFollowed ?: false,
            )
        }
    }
