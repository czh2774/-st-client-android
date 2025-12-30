package com.stproject.client.android.core.di

import com.stproject.client.android.core.session.ChatSessionStore
import com.stproject.client.android.core.session.SharedPreferencesChatSessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    @Singleton
    abstract fun bindChatSessionStore(impl: SharedPreferencesChatSessionStore): ChatSessionStore
}
