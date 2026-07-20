package com.musicapp.stemseparator.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.data.model.STEM_NAMES
import com.musicapp.stemseparator.ui.components.EngineBadge
import com.musicapp.stemseparator.ui.components.QualityBadge
import com.musicapp.stemseparator.ui.components.formatDuration
import com.musicapp.stemseparator.ui.theme.AppIcons
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Spacing

@Composable
fun ResultScreen(
    container: AppContainer,
    jobId: String,
    onBackToLibrary: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: ResultViewModel = viewModel(
        factory = ResultViewModel.factory(container, application, jobId),
    )
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackToLibrary) {
                Icon(AppIcons.ArrowLeft, contentDescription = "Library", tint = colors.foreground)
            }
            Row {
                state.engine?.let { EngineBadge(it, modifier = Modifier.padding(end = Spacing.space1)) }
                state.quality?.let { QualityBadge(it) }
            }
        }

        state.mixer.filename?.let {
            Text(it, style = AppTypography.filename, color = colors.foreground, modifier = Modifier.padding(horizontal = Spacing.space4))
        }

        when {
            state.isLoadingJobDetail -> CircularProgressIndicator(modifier = Modifier.padding(Spacing.space5))
            state.loadError != null -> Text(
                state.loadError ?: "",
                style = AppTypography.bodySmall,
                color = colors.destructive,
                modifier = Modifier.padding(Spacing.space4),
            )
            state.notDoneYet -> Text(
                "This job hasn't finished separating yet.",
                style = AppTypography.bodySmall,
                color = colors.muted,
                modifier = Modifier.padding(Spacing.space4),
            )
            else -> {
                TransportBar(
                    isPlaying = state.mixer.isPlaying,
                    positionMs = state.mixer.positionMs,
                    durationMs = state.mixer.durationMs,
                    onPlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                )

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(STEM_NAMES) { name ->
                        val stemState = state.mixer.stems[name]
                        StemRow(
                            name = name,
                            volume = stemState?.volume ?: 1f,
                            muted = stemState?.muted ?: false,
                            soloed = state.mixer.soloedStem == name,
                            onToggleMute = { viewModel.toggleMute(name) },
                            onToggleSolo = { viewModel.toggleSolo(name) },
                            onVolumeChange = { viewModel.setStemVolume(name, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportBar(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPlayPause) {
            val icon: ImageVector = if (isPlaying) AppIcons.Pause else AppIcons.Play
            Icon(icon, contentDescription = if (isPlaying) "Pause" else "Play", tint = colors.foreground, modifier = Modifier.size(28.dp))
        }
        Slider(
            value = positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..(durationMs.toFloat().coerceAtLeast(1f)),
            modifier = Modifier.weight(1f).padding(horizontal = Spacing.space2),
        )
        Text(
            "${formatDuration(positionMs / 1000.0)} / ${formatDuration(durationMs / 1000.0)}",
            style = AppTypography.caption,
            color = colors.muted,
        )
    }
}

@Composable
private fun StemRow(
    name: String,
    volume: Float,
    muted: Boolean,
    soloed: Boolean,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = AppTypography.bodyMedium,
            color = colors.foreground,
            modifier = Modifier.padding(end = Spacing.space2),
        )
        IconButton(onClick = onToggleMute) {
            Icon(
                AppIcons.Mute,
                contentDescription = "Mute $name",
                tint = if (muted) colors.destructive else colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onToggleSolo) {
            Icon(
                AppIcons.Solo,
                contentDescription = "Solo $name",
                tint = if (soloed) colors.foreground else colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f).padding(start = Spacing.space2),
        )
    }
}
