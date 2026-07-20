package com.musicapp.stemseparator.ui.navigation

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** A new job to start on an already-present test_tracks/ file -- no upload leg. */
sealed class PendingJobRequest {
    data class Local(val filename: String) : PendingJobRequest()
    data class Upload(val uri: Uri, val filename: String, val sizeBytes: Long) : PendingJobRequest()
}

/**
 * Activity-scoped shared state for the "pick a track/file -> choose quality -> start a
 * job" handoff between Library/Upload and Processing. A Uri/PendingJobRequest can't be
 * cleanly round-tripped through Navigation-Compose string route args at this app's
 * scale, so it's held here instead -- obtained via the same activity-scoped `viewModel()`
 * call from Library, Upload, and Processing.
 */
class JobLaunchViewModel : ViewModel() {
    var pendingRequest: PendingJobRequest? by mutableStateOf(null)
        private set

    var pendingQuality: String? by mutableStateOf(null)
        private set

    fun launch(request: PendingJobRequest, quality: String) {
        pendingRequest = request
        pendingQuality = quality
    }

    /** Called once by ProcessingViewModel after reading the pending request, so a
     * process/back-stack re-entry into "processing/new" doesn't replay a stale request. */
    fun consume() {
        pendingRequest = null
        pendingQuality = null
    }
}
