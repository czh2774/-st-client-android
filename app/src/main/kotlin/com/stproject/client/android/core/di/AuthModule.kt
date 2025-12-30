package com.stproject.client.android.core.di

import com.stproject.client.android.core.auth.AuthService
import com.stproject.client.android.core.auth.AuthTokenStore
import com.stproject.client.android.core.auth.DefaultAuthService
import com.stproject.client.android.core.auth.EncryptedAuthTokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthTokenStore(impl: EncryptedAuthTokenStore): AuthTokenStore

    @Binds
    @Singleton
    abstract fun bindAuthService(impl: DefaultAuthService): AuthService
}
