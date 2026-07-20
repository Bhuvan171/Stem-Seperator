package com.musicapp.stemseparator.ui.upload

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.musicapp.stemseparator.ui.navigation.JobLaunchViewModel
import com.musicapp.stemseparator.ui.navigation.PendingJobRequest
import com.musicapp.stemseparator.ui.qualitypicker.QualityPickerSheet
import com.musicapp.stemseparator.ui.theme.AppIcons
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Radius
import com.musicapp.stemseparator.ui.theme.Spacing

/**
 * Manual upload flow. The web app's "dropzone" (index.html:90-94) is actually just a
 * styled native file-input trigger with no real HTML5 drag-and-drop wiring, so
 * Android's SAF picker (ACTION_OPEN_DOCUMENT) is a faithful, arguably better, port.
 */
@Composable
fun UploadScreen(
    jobLaunchViewModel: JobLaunchViewModel,
    onBack: () -> Unit,
    onOpenProcessingNew: () -> Unit,
) {
    val colors = LocalAppColors.current
    val contentResolver = LocalContext.current.contentResolver
    var picked by remember { mutableStateOf<Triple<android.net.Uri, String, Long>?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        var name = "audio_file"
        var size = -1L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        picked = Triple(uri, name, size)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.ArrowLeft, contentDescription = "Back", tint = colors.foreground)
            }
            Text("Upload", style = AppTypography.h1, color = colors.foreground, modifier = Modifier.padding(start = Spacing.space2))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.space5)
                .aspectRatio(1.6f)
                .border(BorderStroke(1.dp, colors.borderStrong), RoundedCornerShape(Radius.lg))
                .clickable { pickerLauncher.launch(arrayOf("audio/*")) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(AppIcons.Upload, contentDescription = null, tint = colors.muted, modifier = Modifier.size(32.dp))
            Text(
                "Choose an audio file",
                style = AppTypography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(top = Spacing.space3),
            )
        }
    }

    picked?.let { (uri, filename, size) ->
        QualityPickerSheet(
            filename = filename,
            onQualityChosen = { quality ->
                jobLaunchViewModel.launch(PendingJobRequest.Upload(uri, filename, size), quality)
                picked = null
                onOpenProcessingNew()
            },
            onDismiss = { picked = null },
        )
    }
}
