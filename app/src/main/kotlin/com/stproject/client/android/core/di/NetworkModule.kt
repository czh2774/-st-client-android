package com.stproject.client.android.core.di

import com.stproject.client.android.core.logging.createHttpLoggingInterceptor
import com.stproject.client.android.core.auth.AuthInterceptor
import com.stproject.client.android.core.auth.AuthTokenStore
import com.stproject.client.android.core.network.StApi
import com.stproject.client.android.core.network.StBaseUrlProvider
import com.stproject.client.android.core.network.InMemoryCookieJar
import com.stproject.client.android.core.network.StOkHttpClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    fun provideCookieJar(): CookieJar = InMemoryCookieJar()

    @Provides
    @Singleton
    fun provideAuthTokenStore(): AuthTokenStore = AuthTokenStore()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: AuthTokenStore): Interceptor = AuthInterceptor(tokenStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        factory: StOkHttpClientFactory,
        authInterceptor: Interceptor,
        cookieJar: CookieJar
    ): OkHttpClient {
        return factory.create(
            cookieJar = cookieJar,
            additionalInterceptors = listOfNotNull(
                authInterceptor,
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
}


