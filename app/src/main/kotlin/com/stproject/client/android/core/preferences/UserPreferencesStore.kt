package com.stproject.client.android.core.preferences

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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

    fun getModelPresetId(): String?

    fun setModelPresetId(presetId: String?)

    fun getGlobalVariables(): Map<String, Any?>

    fun setGlobalVariables(variables: Map<String, Any?>)

    fun getPresetVariables(presetId: String): Map<String, Any?>

    fun setPresetVariables(
        presetId: String,
        variables: Map<String, Any?>,
    )
}

@Singleton
class SharedPreferencesUserPreferencesStore
    @Inject
    constructor(
        @Named("user_prefs") private val prefs: SharedPreferences,
    ) : UserPreferencesStore {
        private val gson: Gson = GsonBuilder().serializeNulls().create()
        private val variablesType = object : TypeToken<Map<String, Any?>>() {}.type

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

        override fun getModelPresetId(): String? =
            prefs.getString(
                KEY_MODEL_PRESET_ID,
                null,
            )?.trim()?.takeIf { it.isNotEmpty() }

        override fun setModelPresetId(presetId: String?) {
            val value = presetId?.trim()?.takeIf { it.isNotEmpty() }
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_MODEL_PRESET_ID)
                } else {
                    putString(KEY_MODEL_PRESET_ID, value)
                }
            }.apply()
        }

        override fun getGlobalVariables(): Map<String, Any?> {
            return readVariables(KEY_GLOBAL_VARIABLES)
        }

        override fun setGlobalVariables(variables: Map<String, Any?>) {
            writeVariables(KEY_GLOBAL_VARIABLES, variables)
        }

        override fun getPresetVariables(presetId: String): Map<String, Any?> {
            val key = presetVariablesKey(presetId)
            return readVariables(key)
        }

        override fun setPresetVariables(
            presetId: String,
            variables: Map<String, Any?>,
        ) {
            val key = presetVariablesKey(presetId)
            writeVariables(key, variables)
        }

        private fun readVariables(key: String): Map<String, Any?> {
            val raw = prefs.getString(key, null) ?: return emptyMap()
            return runCatching { gson.fromJson<Map<String, Any?>>(raw, variablesType) }.getOrNull()
                ?: emptyMap()
        }

        private fun writeVariables(
            key: String,
            variables: Map<String, Any?>,
        ) {
            prefs.edit().apply {
                if (variables.isEmpty()) {
                    remove(key)
                } else {
                    putString(key, gson.toJson(variables))
                }
            }.apply()
        }

        private fun presetVariablesKey(presetId: String): String {
            return "$KEY_PRESET_VARIABLES_PREFIX${presetId.trim()}"
        }

        private companion object {
            private const val KEY_NSFW_ALLOWED = "pref.nsfw_allowed"
            private const val KEY_THEME_MODE = "pref.theme_mode"
            private const val KEY_LANGUAGE_TAG = "pref.language_tag"
            private const val KEY_MODEL_PRESET_ID = "pref.model_preset_id"
            private const val KEY_GLOBAL_VARIABLES = "pref.global_variables"
            private const val KEY_PRESET_VARIABLES_PREFIX = "pref.preset_variables."
        }
    }
