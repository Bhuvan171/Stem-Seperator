package com.musicapp.stemseparator.ui.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.data.model.JobStatus
import com.musicapp.stemseparator.ui.components.EngineBadge
import com.musicapp.stemseparator.ui.components.QualityBadge
import com.musicapp.stemseparator.ui.navigation.JobLaunchViewModel
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Spacing

@Composable
fun ProcessingScreen(
    container: AppContainer,
    jobLaunchViewModel: JobLaunchViewModel,
    resumeJobId: String?,
    onBackToLibrary: () -> Unit,
    onDone: (jobId: String) -> Unit,
    viewModel: ProcessingViewModel = viewModel(
        factory = ProcessingViewModel.factory(container, jobLaunchViewModel, resumeJobId),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current

    LaunchedEffect(state.isDone, state.jobId) {
        if (state.isDone && state.jobId != null) onDone(state.jobId!!)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Processing", style = AppTypography.h1, color = colors.foreground)
            Row {
                state.engine?.let { EngineBadge(it, modifier = Modifier.padding(end = Spacing.space1)) }
                state.quality?.let { QualityBadge(it) }
            }
        }

        state.filename?.let {
            Text(it, style = AppTypography.filename, color = colors.foreground, modifier = Modifier.padding(horizontal = Spacing.space4))
        }

        if (state.showUploadProgress) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.space4)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Uploading to server", style = AppTypography.bodySmall, color = colors.muted)
                    Text("${state.uploadProgressPct?.toInt() ?: 0}%", style = AppTypography.bodySmall, color = colors.muted)
                }
                LinearProgressIndicator(
                    progress = { (state.uploadProgressPct ?: 0f) / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.space1),
                )
            }
        }

        if (state.errorMessage == null || state.status != JobStatus.FAILED) {
            StageStepper(status = state.status, stage = state.stage, separatingProgress = state.separatingProgress)
        }

        if (state.status == JobStatus.FAILED) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.space4)) {
                Text(
                    state.errorMessage ?: "Separation failed.",
                    style = AppTypography.bodySmall,
                    color = colors.destructive,
                )
                Button(onClick = onBackToLibrary, modifier = Modifier.padding(top = Spacing.space3)) {
                    Text("Back to Library")
                }
            }
        } else if (state.errorMessage != null) {
            // Transient error (e.g. a polling hiccup) rather than a failed job -- shown
            // without abandoning the stepper, since the job may still complete.
            Text(
                state.errorMessage ?: "",
                style = AppTypography.bodySmall,
                color = colors.destructive,
                modifier = Modifier.padding(horizontal = Spacing.space4),
            )
        }
    }
}
