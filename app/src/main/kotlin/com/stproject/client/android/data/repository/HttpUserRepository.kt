package com.stproject.client.android.data.repository

import com.stproject.client.android.core.network.AcceptTosRequestDto
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.StUserApi
import com.stproject.client.android.core.network.UpdateUserConfigRequestDto
import com.stproject.client.android.core.network.UserConfigDto
import com.stproject.client.android.core.network.UserMeDto
import com.stproject.client.android.domain.model.UserConfig
import com.stproject.client.android.domain.model.UserConfigUpdate
import com.stproject.client.android.domain.model.UserProfile
import com.stproject.client.android.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpUserRepository
    @Inject
    constructor(
        private val api: StUserApi,
        private val apiClient: ApiClient,
    ) : UserRepository {
        override suspend fun getMe(): UserProfile {
            val dto = apiClient.call { api.getMe() }
            return dto.toDomain()
        }

        override suspend fun acceptTos(version: String?): UserProfile {
            apiClient.call { api.acceptTos(AcceptTosRequestDto(tosVersion = version, version = version)) }
            return getMe()
        }

        override suspend fun getUserConfig(): UserConfig {
            val dto = apiClient.call { api.getUserConfig() }
            return dto.toDomain()
        }

        override suspend fun updateUserConfig(update: UserConfigUpdate): UserConfig {
            val dto =
                apiClient.call {
                    api.updateUserConfig(
                        UpdateUserConfigRequestDto(
                            blockedTags = update.blockedTags,
                            ageVerified = update.ageVerified,
                            birthDate = update.birthDate,
                        ),
                    )
                }
            return dto.toDomain()
        }

        override suspend fun deleteMe() {
            apiClient.call { api.deleteMe() }
        }

        private fun UserMeDto.toDomain(): UserProfile =
            UserProfile(
                id = id,
                email = email?.trim()?.takeIf { it.isNotEmpty() },
                tosVersion = tosVersion?.trim()?.takeIf { it.isNotEmpty() },
                tosAcceptedAt = tosAcceptedAt?.trim()?.takeIf { it.isNotEmpty() },
            )

        private fun UserConfigDto.toDomain(): UserConfig =
            UserConfig(
                ageVerified = ageVerified ?: false,
                birthDate = birthDate?.trim()?.takeIf { it.isNotEmpty() },
                blockedTags =
                    blockedTags
                        ?.mapNotNull { tag -> tag.trim().takeIf { it.isNotEmpty() } }
                        ?: emptyList(),
            )
    }
