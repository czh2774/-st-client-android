package com.stproject.client.android.core.di

import com.stproject.client.android.core.network.CookieCleaner
import com.stproject.client.android.core.network.CookieStorage
import com.stproject.client.android.core.network.PersistentCookieJar
import com.stproject.client.android.core.network.SharedPreferencesCookieStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CookieModule {
    @Binds
    @Singleton
    abstract fun bindCookieStorage(impl: SharedPreferencesCookieStorage): CookieStorage

    @Binds
    @Singleton
    abstract fun bindCookieJar(impl: PersistentCookieJar): CookieJar

    @Binds
    @Singleton
    abstract fun bindCookieCleaner(impl: PersistentCookieJar): CookieCleaner
}
