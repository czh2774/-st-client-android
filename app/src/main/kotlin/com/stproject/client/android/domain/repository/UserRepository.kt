package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.UserConfig
import com.stproject.client.android.domain.model.UserConfigUpdate
import com.stproject.client.android.domain.model.UserProfile

interface UserRepository {
    suspend fun getMe(): UserProfile

    suspend fun acceptTos(version: String? = null): UserProfile

    suspend fun getUserConfig(): UserConfig

    suspend fun updateUserConfig(update: UserConfigUpdate): UserConfig

    suspend fun deleteMe()
}
