package com.stproject.client.android.core.theme

enum class ThemeMode(val storageValue: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun fromStorage(raw: String?): ThemeMode {
            return when (raw?.trim()?.lowercase()) {
                Light.storageValue -> Light
                Dark.storageValue -> Dark
                else -> System
            }
        }
    }
}
