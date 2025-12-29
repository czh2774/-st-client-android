package com.stproject.client.android.data.di

import com.stproject.client.android.data.repository.HttpChatRepository
import com.stproject.client.android.domain.repository.ChatRepository
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
}


