package com.stproject.client.android.core.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    @Named("auth_prefs")
    fun provideAuthPrefs(@ApplicationContext context: Context): SharedPreferences {
        return createEncryptedPrefs(context, "st_auth_prefs")
    }

    @Provides
    @Singleton
    @Named("cookie_prefs")
    fun provideCookiePrefs(@ApplicationContext context: Context): SharedPreferences {
        return createEncryptedPrefs(context, "st_cookie_prefs")
    }

    @Provides
    @Singleton
    @Named("chat_session_prefs")
    fun provideChatSessionPrefs(@ApplicationContext context: Context): SharedPreferences {
        return createEncryptedPrefs(context, "st_chat_session_prefs")
    }

    private fun createEncryptedPrefs(context: Context, name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
