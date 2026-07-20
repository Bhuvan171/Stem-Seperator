package com.musicapp.stemseparator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.musicapp.stemseparator.data.model.Engine
import com.musicapp.stemseparator.data.model.JobStatus
import com.musicapp.stemseparator.data.model.Quality
import com.musicapp.stemseparator.ui.theme.AppIcons
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Radius
import com.musicapp.stemseparator.ui.theme.Spacing

/** A filled-black (or filled-white in dark mode) pill, or an outlined pill. Ported from
 * style.css's .badge/.status-pill patterns -- engine/quality/status all use this shape. */
@Composable
fun Pill(
    text: String,
    icon: ImageVector? = null,
    filled: Boolean = false,
    contentColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val background = if (filled) colors.primary else androidx.compose.ui.graphics.Color.Transparent
    val foreground = contentColor ?: if (filled) colors.onPrimary else colors.foreground
    val shape = RoundedCornerShape(Radius.full)

    Row(
        modifier = modifier
            .background(background, shape)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, shape))
            .padding(horizontal = Spacing.space3, vertical = Spacing.space1),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = foreground, modifier = Modifier.size(14.dp))
        }
        Text(text = text, style = AppTypography.caption, color = foreground)
    }
}

@Composable
fun EngineBadge(engine: Engine, modifier: Modifier = Modifier) {
    when (engine) {
        Engine.REAL -> Pill(text = "GPU", icon = AppIcons.Cpu, filled = true, modifier = modifier)
        else -> Pill(text = "Mock", filled = false, modifier = modifier)
    }
}

@Composable
fun QualityBadge(quality: Quality, modifier: Modifier = Modifier) {
    when (quality) {
        Quality.LOSSLESS -> Pill(text = "Lossless", icon = AppIcons.Waveform, filled = true, modifier = modifier)
        else -> Pill(text = "Fast", icon = AppIcons.Bolt, filled = false, modifier = modifier)
    }
}

@Composable
fun StatusPill(status: JobStatus, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    when (status) {
        JobStatus.DONE -> Pill(text = "Done", filled = true, modifier = modifier)
        JobStatus.FAILED -> Pill(
            text = "Failed",
            filled = false,
            contentColor = colors.destructive,
            modifier = modifier,
        )
        JobStatus.PROCESSING -> Pill(text = "Processing", filled = false, modifier = modifier)
        else -> Pill(text = "Queued", filled = false, modifier = modifier)
    }
}
