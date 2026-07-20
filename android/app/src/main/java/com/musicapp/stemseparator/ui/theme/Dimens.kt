package com.musicapp.stemseparator.ui.theme

import androidx.compose.ui.unit.dp

/** Ported 1:1 from style.css's --space-* / --radius-* scale. */
object Spacing {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 24.dp
    val space6 = 32.dp
    val space7 = 48.dp
}

object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val full = 999.dp
}

/** Ported from style.css's --duration (180ms, cubic-bezier(0.4,0,0.2,1) eased). */
const val MotionDurationMs = 180

/** Ported from app.js's max-width:440px single-column mobile shell. */
val MaxContentWidth = 440.dp
