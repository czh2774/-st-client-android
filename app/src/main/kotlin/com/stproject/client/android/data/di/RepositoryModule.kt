package com.stproject.client.android.data.di

import com.stproject.client.android.data.repository.HttpCharacterRepository
import com.stproject.client.android.data.repository.HttpChatRepository
import com.stproject.client.android.data.repository.HttpCreatorAssistantRepository
import com.stproject.client.android.data.repository.HttpCreatorRepository
import com.stproject.client.android.data.repository.HttpIapRepository
import com.stproject.client.android.data.repository.HttpNotificationRepository
import com.stproject.client.android.data.repository.HttpReportRepository
import com.stproject.client.android.data.repository.HttpSocialRepository
import com.stproject.client.android.data.repository.HttpUserRepository
import com.stproject.client.android.data.repository.HttpWalletRepository
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.repository.CreatorAssistantRepository
import com.stproject.client.android.domain.repository.CreatorRepository
import com.stproject.client.android.domain.repository.IapRepository
import com.stproject.client.android.domain.repository.NotificationRepository
import com.stproject.client.android.domain.repository.ReportRepository
import com.stproject.client.android.domain.repository.SocialRepository
import com.stproject.client.android.domain.repository.UserRepository
import com.stproject.client.android.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: HttpChatRepository): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: HttpUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: HttpReportRepository): ReportRepository

    @Binds
    @Singleton
    abstract fun bindCharacterRepository(impl: HttpCharacterRepository): CharacterRepository

    @Binds
    @Singleton
    abstract fun bindCreatorRepository(impl: HttpCreatorRepository): CreatorRepository

    @Binds
    @Singleton
    abstract fun bindCreatorAssistantRepository(impl: HttpCreatorAssistantRepository): CreatorAssistantRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: HttpNotificationRepository): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(impl: HttpSocialRepository): SocialRepository

    @Binds
    @Singleton
    abstract fun bindIapRepository(impl: HttpIapRepository): IapRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(impl: HttpWalletRepository): WalletRepository
}
