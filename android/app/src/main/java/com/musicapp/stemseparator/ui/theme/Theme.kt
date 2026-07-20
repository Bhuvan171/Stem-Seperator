package com.musicapp.stemseparator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppColors = staticCompositionLocalOf { LightAppColorScheme }

/**
 * Root theme. Ported from style.css's `prefers-color-scheme: dark` auto-switch --
 * there is no manual in-app theme toggle, it always follows the system setting.
 */
@Composable
fun StemSeparatorTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val appColors = if (isDark) DarkAppColorScheme else LightAppColorScheme

    val materialColorScheme = if (isDark) {
        darkColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            background = appColors.bg,
            surface = appColors.surface,
            surfaceVariant = appColors.surfaceRaised,
            onBackground = appColors.foreground,
            onSurface = appColors.foreground,
            outline = appColors.border,
            error = appColors.destructive,
        )
    } else {
        lightColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            background = appColors.bg,
            surface = appColors.surface,
            surfaceVariant = appColors.surfaceRaised,
            onBackground = appColors.foreground,
            onSurface = appColors.foreground,
            outline = appColors.border,
            error = appColors.destructive,
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content,
        )
    }
}
