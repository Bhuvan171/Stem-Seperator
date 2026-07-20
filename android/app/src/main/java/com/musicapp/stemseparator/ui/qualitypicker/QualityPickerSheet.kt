package com.musicapp.stemseparator.ui.qualitypicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.musicapp.stemseparator.ui.theme.AppIcons
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Spacing

/**
 * 1:1 port of the web app's shared quality bottom-sheet (index.html:147-163), used
 * identically whether starting from a local test_tracks/ file or a manual upload.
 * "Fast" and "Lossless" only affect the server's own upload-to-GPU transport leg, not
 * the client's upload to this backend nor the model's output quality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityPickerSheet(
    filename: String,
    onQualityChosen: (quality: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = Spacing.space5, vertical = Spacing.space3)) {
            Text("Separate track", style = AppTypography.h1, color = colors.foreground)
            Text(
                filename,
                style = AppTypography.bodySmall,
                color = colors.muted,
                modifier = Modifier.padding(top = Spacing.space1, bottom = Spacing.space4),
            )

            QualityOption(
                icon = AppIcons.Bolt,
                title = "Fast",
                subtitle = "Compressed upload — quicker to start, near-identical separation quality",
                onClick = { onQualityChosen("fast") },
            )
            QualityOption(
                icon = AppIcons.Waveform,
                title = "Lossless",
                subtitle = "Original-quality upload — slower to start, maximum fidelity",
                onClick = { onQualityChosen("lossless") },
            )

            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = Spacing.space2)) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun QualityOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.space3),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.foreground,
            modifier = Modifier.size(24.dp).padding(top = 2.dp),
        )
        Column(modifier = Modifier.padding(start = Spacing.space3)) {
            Text(title, style = AppTypography.bodyLarge, color = colors.foreground)
            Text(subtitle, style = AppTypography.bodySmall, color = colors.muted)
        }
    }
}
