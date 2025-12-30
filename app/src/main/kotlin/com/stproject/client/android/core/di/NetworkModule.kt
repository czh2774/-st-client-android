package com.stproject.client.android.core.di

import com.stproject.client.android.core.auth.AuthAuthenticator
import com.stproject.client.android.core.auth.AuthInterceptor
import com.stproject.client.android.core.auth.AuthRefreshCoordinator
import com.stproject.client.android.core.auth.AuthTokenStore
import com.stproject.client.android.core.logging.createHttpLoggingInterceptor
import com.stproject.client.android.core.network.ClientHeadersInterceptor
import com.stproject.client.android.core.network.StApi
import com.stproject.client.android.core.network.StAuthApi
import com.stproject.client.android.core.network.StBaseUrlProvider
import com.stproject.client.android.core.network.StOkHttpClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideBaseUrlProvider(): StBaseUrlProvider = StBaseUrlProvider()

    @Provides
    @Singleton
    fun provideOkHttpClientFactory(): StOkHttpClientFactory = StOkHttpClientFactory()

    @Provides
    @Singleton
    fun provideClientHeadersInterceptor(): ClientHeadersInterceptor = ClientHeadersInterceptor()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: AuthTokenStore): AuthInterceptor = AuthInterceptor(tokenStore)

    @Provides
    @Singleton
    fun provideAuthAuthenticator(
        tokenStore: AuthTokenStore,
        refreshCoordinator: AuthRefreshCoordinator
    ): AuthAuthenticator = AuthAuthenticator(tokenStore, refreshCoordinator)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        factory: StOkHttpClientFactory,
        headersInterceptor: ClientHeadersInterceptor,
        authInterceptor: AuthInterceptor,
        authenticator: AuthAuthenticator,
        cookieJar: CookieJar
    ): OkHttpClient {
        return factory.create(
            cookieJar = cookieJar,
            additionalInterceptors = listOfNotNull(
                headersInterceptor,
                authInterceptor,
                createHttpLoggingInterceptor()
            ),
            authenticator = authenticator
        )
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthOkHttpClient(
        factory: StOkHttpClientFactory,
        headersInterceptor: ClientHeadersInterceptor,
        cookieJar: CookieJar
    ): OkHttpClient {
        return factory.create(
            cookieJar = cookieJar,
            additionalInterceptors = listOfNotNull(
                headersInterceptor,
                createHttpLoggingInterceptor()
            )
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        baseUrlProvider: StBaseUrlProvider
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrlProvider.baseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideStApi(retrofit: Retrofit): StApi = retrofit.create(StApi::class.java)

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthRetrofit(
        @Named("auth") okHttpClient: OkHttpClient,
        baseUrlProvider: StBaseUrlProvider
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrlProvider.baseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideStAuthApi(@Named("auth") retrofit: Retrofit): StAuthApi {
        return retrofit.create(StAuthApi::class.java)
    }
}

