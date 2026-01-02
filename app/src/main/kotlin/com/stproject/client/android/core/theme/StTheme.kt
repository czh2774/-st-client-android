package com.stproject.client.android.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color

private val RosePrimary = Color(0xFFEB1C6F)
private val RosePrimaryContainer = Color(0xFFFFD9E3)
private val RoseOnPrimaryContainer = Color(0xFF3E0018)

private val DarkColorScheme = darkColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = RoseOnPrimaryContainer,
    onPrimaryContainer = RosePrimaryContainer,
    // Add other overrides as needed to align with "Midnight Romance"
    // For now we focus on the primary accent identity
)

private val LightColorScheme = lightColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = RosePrimaryContainer,
    onPrimaryContainer = RoseOnPrimaryContainer,
)

@Composable
fun StTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
        }
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
