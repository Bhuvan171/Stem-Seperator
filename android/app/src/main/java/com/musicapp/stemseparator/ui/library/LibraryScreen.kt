package com.musicapp.stemseparator.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.data.model.JobStatus
import com.musicapp.stemseparator.data.model.JobSummary
import com.musicapp.stemseparator.data.model.LocalTrack
import com.musicapp.stemseparator.ui.components.QualityBadge
import com.musicapp.stemseparator.ui.components.StatusPill
import com.musicapp.stemseparator.ui.components.formatBytes
import com.musicapp.stemseparator.ui.components.formatDateTime
import com.musicapp.stemseparator.ui.navigation.JobLaunchViewModel
import com.musicapp.stemseparator.ui.navigation.PendingJobRequest
import com.musicapp.stemseparator.ui.qualitypicker.QualityPickerSheet
import com.musicapp.stemseparator.ui.theme.AppIcons
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Spacing

@Composable
fun LibraryScreen(
    container: AppContainer,
    jobLaunchViewModel: JobLaunchViewModel,
    onOpenUpload: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProcessingNew: () -> Unit,
    onOpenProcessingResume: (jobId: String) -> Unit,
    onOpenResult: (jobId: String) -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container)),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    var qualityPickerFilename by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Library", style = AppTypography.h1, color = colors.foreground)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Server settings", tint = colors.foreground)
                }
                Button(onClick = onOpenUpload, contentPadding = PaddingValues(horizontal = Spacing.space4)) {
                    Icon(AppIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Upload", modifier = Modifier.padding(start = Spacing.space1))
                }
            }
        }

        if (state.isLoading && state.localTracks.isEmpty() && state.jobs.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(Spacing.space5))
        }

        state.errorMessage?.let { message ->
            Text(message, style = AppTypography.bodySmall, color = colors.destructive, modifier = Modifier.padding(Spacing.space4))
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            if (state.localTracks.isNotEmpty()) {
                item { SectionLabel("Ready to Separate") }
                items(state.localTracks) { track ->
                    LocalTrackRow(track = track, onSeparate = { qualityPickerFilename = track.filename })
                }
            }

            if (state.jobs.isNotEmpty()) {
                item { SectionLabel("History") }
                items(state.jobs) { job ->
                    JobRow(
                        job = job,
                        onClick = {
                            if (job.status == JobStatus.DONE) onOpenResult(job.jobId) else onOpenProcessingResume(job.jobId)
                        },
                    )
                }
            }

            if (state.localTracks.isEmpty() && state.jobs.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        "Nothing here yet. Drop a file into test_tracks/, or tap Upload above.",
                        style = AppTypography.bodySmall,
                        color = colors.muted,
                        modifier = Modifier.padding(Spacing.space4),
                    )
                }
            }
        }
    }

    qualityPickerFilename?.let { filename ->
        QualityPickerSheet(
            filename = filename,
            onQualityChosen = { quality ->
                jobLaunchViewModel.launch(PendingJobRequest.Local(filename), quality)
                qualityPickerFilename = null
                onOpenProcessingNew()
            },
            onDismiss = { qualityPickerFilename = null },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text.uppercase(),
        style = AppTypography.sectionLabel,
        color = colors.muted,
        modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space2),
    )
}

@Composable
private fun LocalTrackRow(track: LocalTrack, onSeparate: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Music, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.space3, end = Spacing.space3),
        ) {
            Text(
                track.filename,
                style = AppTypography.bodyMedium,
                color = colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatBytes(track.sizeBytes), style = AppTypography.caption, color = colors.muted)
        }
        Button(onClick = onSeparate) { Text("Separate") }
    }
}

@Composable
private fun JobRow(job: JobSummary, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Music, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.space3, end = Spacing.space3),
        ) {
            Text(
                job.inputFilename,
                style = AppTypography.bodyMedium,
                color = colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDateTime(job.createdAt),
                    style = AppTypography.caption,
                    color = colors.muted,
                    modifier = Modifier.padding(end = Spacing.space2),
                )
                QualityBadge(job.quality)
            }
        }
        StatusPill(job.status)
    }
}
