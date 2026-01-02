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
import com.stproject.client.android.core.network.StCardApi
import com.stproject.client.android.core.network.StCharacterApi
import com.stproject.client.android.core.network.StCommentApi
import com.stproject.client.android.core.network.StCreatorApi
import com.stproject.client.android.core.network.StCreatorAssistantApi
import com.stproject.client.android.core.network.StIapApi
import com.stproject.client.android.core.network.StNotificationApi
import com.stproject.client.android.core.network.StOkHttpClientFactory
import com.stproject.client.android.core.network.StPresetApi
import com.stproject.client.android.core.network.StReportApi
import com.stproject.client.android.core.network.StSocialApi
import com.stproject.client.android.core.network.StUserApi
import com.stproject.client.android.core.network.StWalletApi
import com.stproject.client.android.core.network.StWorldInfoApi
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
        refreshCoordinator: AuthRefreshCoordinator,
    ): AuthAuthenticator = AuthAuthenticator(tokenStore, refreshCoordinator)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        factory: StOkHttpClientFactory,
        headersInterceptor: ClientHeadersInterceptor,
        authInterceptor: AuthInterceptor,
        authenticator: AuthAuthenticator,
        cookieJar: CookieJar,
    ): OkHttpClient {
        return factory.create(
            cookieJar = cookieJar,
            additionalInterceptors =
                listOfNotNull(
                    headersInterceptor,
                    authInterceptor,
                    createHttpLoggingInterceptor(),
                ),
            authenticator = authenticator,
        )
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthOkHttpClient(
        factory: StOkHttpClientFactory,
        headersInterceptor: ClientHeadersInterceptor,
        cookieJar: CookieJar,
    ): OkHttpClient {
        return factory.create(
            cookieJar = cookieJar,
            additionalInterceptors =
                listOfNotNull(
                    headersInterceptor,
                    createHttpLoggingInterceptor(),
                ),
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        baseUrlProvider: StBaseUrlProvider,
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
    fun provideStUserApi(retrofit: Retrofit): StUserApi = retrofit.create(StUserApi::class.java)

    @Provides
    @Singleton
    fun provideStReportApi(retrofit: Retrofit): StReportApi = retrofit.create(StReportApi::class.java)

    @Provides
    @Singleton
    fun provideStPresetApi(retrofit: Retrofit): StPresetApi = retrofit.create(StPresetApi::class.java)

    @Provides
    @Singleton
    fun provideStCharacterApi(retrofit: Retrofit): StCharacterApi = retrofit.create(StCharacterApi::class.java)

    @Provides
    @Singleton
    fun provideStCommentApi(retrofit: Retrofit): StCommentApi = retrofit.create(StCommentApi::class.java)

    @Provides
    @Singleton
    fun provideStCreatorApi(retrofit: Retrofit): StCreatorApi = retrofit.create(StCreatorApi::class.java)

    @Provides
    @Singleton
    fun provideStCreatorAssistantApi(retrofit: Retrofit): StCreatorAssistantApi =
        retrofit.create(StCreatorAssistantApi::class.java)

    @Provides
    @Singleton
    fun provideStNotificationApi(retrofit: Retrofit): StNotificationApi = retrofit.create(StNotificationApi::class.java)

    @Provides
    @Singleton
    fun provideStSocialApi(retrofit: Retrofit): StSocialApi = retrofit.create(StSocialApi::class.java)

    @Provides
    @Singleton
    fun provideStIapApi(retrofit: Retrofit): StIapApi = retrofit.create(StIapApi::class.java)

    @Provides
    @Singleton
    fun provideStWalletApi(retrofit: Retrofit): StWalletApi = retrofit.create(StWalletApi::class.java)

    @Provides
    @Singleton
    fun provideStWorldInfoApi(retrofit: Retrofit): StWorldInfoApi = retrofit.create(StWorldInfoApi::class.java)

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthRetrofit(
        @Named("auth") okHttpClient: OkHttpClient,
        baseUrlProvider: StBaseUrlProvider,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrlProvider.baseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideStAuthApi(
        @Named("auth") retrofit: Retrofit,
    ): StAuthApi {
        return retrofit.create(StAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStCardApi(retrofit: Retrofit): StCardApi = retrofit.create(StCardApi::class.java)
}
