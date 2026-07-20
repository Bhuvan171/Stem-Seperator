package com.musicapp.stemseparator.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Ported 1:1 from backend/frontend/style.css custom properties.
 * Strict black-and-white palette; the only accent color anywhere is
 * [LightDestructive]/[DarkDestructive], reserved for errors/failed-state/mute-active.
 */
object AppColors {
    // Light scheme
    val LightBg = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceRaised = Color(0xFFF5F5F5)
    val LightPrimary = Color(0xFF000000)
    val LightPrimaryHover = Color(0xFF262626)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightForeground = Color(0xFF0A0A0A)
    val LightMuted = Color(0xFF6B6B6B)
    val LightMuted2 = Color(0xFF9A9A9A)
    val LightBorder = Color(0xFFE5E5E5)
    val LightBorderStrong = Color(0xFFD4D4D4)
    val LightDestructive = Color(0xFFDC2626)
    val LightDestructiveBg = Color(0x14DC2626)

    // Dark scheme
    val DarkBg = Color(0xFF000000)
    val DarkSurface = Color(0xFF0A0A0A)
    val DarkSurfaceRaised = Color(0xFF171717)
    val DarkPrimary = Color(0xFFFFFFFF)
    val DarkPrimaryHover = Color(0xFFE5E5E5)
    val DarkOnPrimary = Color(0xFF000000)
    val DarkForeground = Color(0xFFFAFAFA)
    val DarkMuted = Color(0xFFA3A3A3)
    val DarkMuted2 = Color(0xFF737373)
    val DarkBorder = Color(0xFF262626)
    val DarkBorderStrong = Color(0xFF404040)
    val DarkDestructive = Color(0xFFF87171)
    val DarkDestructiveBg = Color(0x1FF87171)
}

/** The subset of [AppColors] a composable needs, resolved for the current light/dark mode. */
data class AppColorScheme(
    val bg: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val primary: Color,
    val primaryHover: Color,
    val onPrimary: Color,
    val foreground: Color,
    val muted: Color,
    val muted2: Color,
    val border: Color,
    val borderStrong: Color,
    val destructive: Color,
    val destructiveBg: Color,
)

val LightAppColorScheme = AppColorScheme(
    bg = AppColors.LightBg,
    surface = AppColors.LightSurface,
    surfaceRaised = AppColors.LightSurfaceRaised,
    primary = AppColors.LightPrimary,
    primaryHover = AppColors.LightPrimaryHover,
    onPrimary = AppColors.LightOnPrimary,
    foreground = AppColors.LightForeground,
    muted = AppColors.LightMuted,
    muted2 = AppColors.LightMuted2,
    border = AppColors.LightBorder,
    borderStrong = AppColors.LightBorderStrong,
    destructive = AppColors.LightDestructive,
    destructiveBg = AppColors.LightDestructiveBg,
)

val DarkAppColorScheme = AppColorScheme(
    bg = AppColors.DarkBg,
    surface = AppColors.DarkSurface,
    surfaceRaised = AppColors.DarkSurfaceRaised,
    primary = AppColors.DarkPrimary,
    primaryHover = AppColors.DarkPrimaryHover,
    onPrimary = AppColors.DarkOnPrimary,
    foreground = AppColors.DarkForeground,
    muted = AppColors.DarkMuted,
    muted2 = AppColors.DarkMuted2,
    border = AppColors.DarkBorder,
    borderStrong = AppColors.DarkBorderStrong,
    destructive = AppColors.DarkDestructive,
    destructiveBg = AppColors.DarkDestructiveBg,
)
