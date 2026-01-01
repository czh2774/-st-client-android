package com.stproject.client.android.core.di

import com.stproject.client.android.core.preferences.SharedPreferencesUserPreferencesStore
import com.stproject.client.android.core.preferences.UserPreferencesStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    @Singleton
    abstract fun bindUserPreferencesStore(impl: SharedPreferencesUserPreferencesStore): UserPreferencesStore
}
