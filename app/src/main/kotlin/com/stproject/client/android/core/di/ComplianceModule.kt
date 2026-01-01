package com.stproject.client.android.core.di

import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.InMemoryContentAccessManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ComplianceModule {
    @Binds
    @Singleton
    abstract fun bindContentAccessManager(impl: InMemoryContentAccessManager): ContentAccessManager
}
