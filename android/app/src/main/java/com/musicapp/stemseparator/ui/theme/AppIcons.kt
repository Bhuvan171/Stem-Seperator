package com.musicapp.stemseparator.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ported 1:1 from the inline SVG sprite in backend/frontend/index.html (lines 12-58):
 * hand-authored Phosphor-style outline icons, 24x24 viewBox, stroke-width 1.75
 * (2 for check), round caps/joins. Baked-in color is irrelevant at call sites --
 * always render via the `Icon()` composable, which overrides colors with `tint`
 * (the Compose analog of the web's `currentColor`).
 */
private fun strokeIcon(name: String, pathData: String, strokeWidth: Float = 1.75f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

private fun fillIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
    ).build()

object AppIcons {
    val Waveform: ImageVector by lazy {
        strokeIcon("icon-waveform", "M2 12h2l2-7 3 14 3-10 2 5 2-3 2 4h4")
    }
    val Plus: ImageVector by lazy {
        strokeIcon("icon-plus", "M12 5v14M5 12h14")
    }
    val ArrowLeft: ImageVector by lazy {
        strokeIcon("icon-arrow-left", "M19 12H5M11 18l-6-6 6-6")
    }
    val Upload: ImageVector by lazy {
        strokeIcon("icon-upload", "M12 16V4M7 9l5-5 5 5M4 20h16")
    }
    val Music: ImageVector by lazy {
        strokeIcon(
            "icon-music",
            "M9 18V5l11-2v13" +
                "M6 18m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" +
                "M17 16m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        )
    }
    val Cpu: ImageVector by lazy {
        strokeIcon(
            "icon-cpu",
            "M6 6h12v12h-12zM9 2v3M15 2v3M9 19v3M15 19v3M2 9h3M2 15h3M19 9h3M19 15h3M9 9h6v6h-6z",
        )
    }
    val Play: ImageVector by lazy {
        fillIcon("icon-play", "M8 5.5v13l11-6.5z")
    }
    val Pause: ImageVector by lazy {
        fillIcon("icon-pause", "M6 5h4v14h-4zM14 5h4v14h-4z")
    }
    val Mute: ImageVector by lazy {
        strokeIcon("icon-mute", "M11 5 6 9H3v6h3l5 4V5zM17 9l4 6M21 9l-4 6")
    }
    val Solo: ImageVector by lazy {
        strokeIcon(
            "icon-solo",
            "M12 8m-4 0a4 4 0 1 0 8 0a4 4 0 1 0 -8 0" +
                "M4 20c0-3.5 3.5-6 8-6s8 2.5 8 6",
        )
    }
    val Check: ImageVector by lazy {
        strokeIcon("icon-check", "M20 6 9 17l-5-5", strokeWidth = 2f)
    }
    val Alert: ImageVector by lazy {
        strokeIcon("icon-alert", "M12 9v4M12 17h.01M10.3 3.86 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.86a2 2 0 0 0-3.4 0Z")
    }
    val Clock: ImageVector by lazy {
        strokeIcon("icon-clock", "M12 12m-9 0a9 9 0 1 0 18 0 9 9 0 1 0 -18 0M12 7v5l3 3")
    }
    val Download: ImageVector by lazy {
        strokeIcon("icon-download", "M12 4v12M7 11l5 5 5-5M4 20h16")
    }
    val Bolt: ImageVector by lazy {
        fillIcon("icon-bolt", "M13 2 3 14h7l-1 8 10-12h-7l1-8z")
    }
}
