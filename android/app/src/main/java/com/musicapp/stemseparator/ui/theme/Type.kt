package com.musicapp.stemseparator.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Ported from style.css's type scale. Uses the system sans-serif family rather than
 * bundling Inter (the web app's font) as a font resource -- swap [FontFamily.Default]
 * for a bundled Inter FontFamily later if pixel-exact font matching matters.
 */
object AppTypography {
    private val family = FontFamily.Default

    // h1: 1.375rem / weight 800 / -0.02em tracking
    val h1 = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        letterSpacing = (-0.02).em,
    )

    // .section-label: 0.75rem / weight 700 / uppercase / 0.06em tracking
    val sectionLabel = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.06.em,
    )

    val bodyLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val bodyMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val bodySmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    val caption = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    val buttonLabel = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    )

    val filename = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )
}
