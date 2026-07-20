package com.musicapp.stemseparator.ui.processing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.musicapp.stemseparator.data.model.JobStage
import com.musicapp.stemseparator.data.model.JobStatus
import com.musicapp.stemseparator.ui.theme.AppIcons
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Spacing

private data class StageStep(val key: String, val label: String)

private val STAGES = listOf(
    StageStep("queued", "Queued"),
    StageStep("uploading", "Uploading track"),
    StageStep("separating", "Separating stems"),
    StageStep("downloading", "Downloading results"),
    StageStep("finalizing", "Finalizing"),
    StageStep("done", "Done"),
)

/**
 * 1:1 port of app.js's renderStageStepper()/updateStageStepper(). Deliberately
 * reproduces a real, confirmed quirk: mock-engine jobs never call the progress
 * callback at all, so `stage` stays null for the whole ~3s mock job lifetime even
 * though `status` is already "processing" -- the fallback `stage ?: "queued"` means a
 * mock job's stepper pins on "Queued" as the active step the entire time. This is
 * intentional parity with the web app, not a bug.
 */
@Composable
fun StageStepper(status: JobStatus, stage: JobStage?, separatingProgress: Float?) {
    val currentKey = if (status == JobStatus.DONE) "done" else stageKey(stage) ?: "queued"
    val currentIndex = STAGES.indexOfFirst { it.key == currentKey }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space3)) {
        STAGES.forEachIndexed { index, step ->
            StageRow(
                label = step.label,
                isComplete = index < currentIndex,
                isActive = index == currentIndex,
            )
            if (step.key == "separating" && index == currentIndex && separatingProgress != null) {
                LinearProgressIndicator(
                    progress = { (separatingProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.space6, end = Spacing.space2, bottom = Spacing.space2),
                )
                Text(
                    "${separatingProgress.toInt()}%",
                    style = AppTypography.caption,
                    color = LocalAppColors.current.muted,
                    modifier = Modifier.padding(start = Spacing.space6, bottom = Spacing.space2),
                )
            }
        }
    }
}

private fun stageKey(stage: JobStage?): String? = when (stage) {
    null -> null
    JobStage.UPLOADING -> "uploading"
    JobStage.SEPARATING -> "separating"
    JobStage.DOWNLOADING -> "downloading"
    JobStage.FINALIZING -> "finalizing"
    JobStage.UNKNOWN -> null
}

@Composable
private fun StageRow(label: String, isComplete: Boolean, isActive: Boolean) {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "stage-pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "stage-pulse-alpha",
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            isComplete -> Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = colors.foreground,
                modifier = Modifier.size(20.dp),
            )
            isActive -> androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(pulseAlpha)
                    .background(colors.foreground, CircleShape),
            )
            else -> androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.border, CircleShape),
            )
        }
        Text(
            label,
            style = AppTypography.bodyMedium,
            color = if (isComplete || isActive) colors.foreground else colors.muted,
            modifier = Modifier.padding(start = Spacing.space3),
        )
    }
}
