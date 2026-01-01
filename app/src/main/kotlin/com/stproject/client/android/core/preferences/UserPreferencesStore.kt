package com.stproject.client.android.core.preferences

import android.content.SharedPreferences
import com.stproject.client.android.core.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface UserPreferencesStore {
    fun isNsfwAllowed(): Boolean

    fun setNsfwAllowed(value: Boolean)

    fun getThemeMode(): ThemeMode

    fun setThemeMode(mode: ThemeMode)

    fun getLanguageTag(): String?

    fun setLanguageTag(tag: String?)
}

@Singleton
class SharedPreferencesUserPreferencesStore
    @Inject
    constructor(
        @Named("user_prefs") private val prefs: SharedPreferences,
    ) : UserPreferencesStore {
        override fun isNsfwAllowed(): Boolean = prefs.getBoolean(KEY_NSFW_ALLOWED, false)

        override fun setNsfwAllowed(value: Boolean) {
            prefs.edit().putBoolean(KEY_NSFW_ALLOWED, value).apply()
        }

        override fun getThemeMode(): ThemeMode = ThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, null))

        override fun setThemeMode(mode: ThemeMode) {
            prefs.edit().putString(KEY_THEME_MODE, mode.storageValue).apply()
        }

        override fun getLanguageTag(): String? =
            prefs.getString(
                KEY_LANGUAGE_TAG,
                null,
            )?.trim()?.takeIf { it.isNotEmpty() }

        override fun setLanguageTag(tag: String?) {
            val value = tag?.trim()?.takeIf { it.isNotEmpty() }
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_LANGUAGE_TAG)
                } else {
                    putString(KEY_LANGUAGE_TAG, value)
                }
            }.apply()
        }

        private companion object {
            private const val KEY_NSFW_ALLOWED = "pref.nsfw_allowed"
            private const val KEY_THEME_MODE = "pref.theme_mode"
            private const val KEY_LANGUAGE_TAG = "pref.language_tag"
        }
    }
