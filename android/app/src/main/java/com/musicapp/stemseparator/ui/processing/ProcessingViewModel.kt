package com.musicapp.stemseparator.ui.processing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.data.model.Engine
import com.musicapp.stemseparator.data.model.JobStage
import com.musicapp.stemseparator.data.model.JobStatus
import com.musicapp.stemseparator.data.model.Quality
import com.musicapp.stemseparator.data.repository.StemSeparatorRepository
import com.musicapp.stemseparator.data.repository.toAppError
import com.musicapp.stemseparator.ui.navigation.JobLaunchViewModel
import com.musicapp.stemseparator.ui.navigation.PendingJobRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProcessingUiState(
    val filename: String? = null,
    val engine: Engine? = null,
    val quality: Quality? = null,
    /** Client's own upload-to-backend progress (0-100), Upload requests only; null hides
     * the bar entirely (mirrors the web's `if (e.lengthComputable)` guard). Distinct from
     * [separatingProgress] below, which is the server's own SCP-to-GPU-box "uploading"
     * stage plus the model's live separation percentage. */
    val uploadProgressPct: Float? = null,
    val showUploadProgress: Boolean = false,
    val status: JobStatus = JobStatus.QUEUED,
    val stage: JobStage? = null,
    val separatingProgress: Float? = null,
    val errorMessage: String? = null,
    val jobId: String? = null,
    val isDone: Boolean = false,
)

class ProcessingViewModel(
    private val repository: StemSeparatorRepository,
    private val pendingRequest: PendingJobRequest?,
    private val pendingQuality: String?,
    private val resumeJobId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private var lastReportedPct = -1

    init {
        viewModelScope.launch {
            try {
                val jobId = resumeJobId ?: startNewJob()
                _uiState.update { it.copy(jobId = jobId, showUploadProgress = false) }
                pollStatus(jobId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = describeError(e)) }
            }
        }
    }

    private suspend fun startNewJob(): String {
        val quality = pendingQuality ?: "fast"
        return when (val request = pendingRequest) {
            is PendingJobRequest.Local -> {
                _uiState.update { it.copy(filename = request.filename) }
                repository.createLocalJob(request.filename, quality)
            }
            is PendingJobRequest.Upload -> {
                _uiState.update { it.copy(filename = request.filename, showUploadProgress = true) }
                repository.uploadAndCreateJob(
                    uri = request.uri,
                    filename = request.filename,
                    knownLength = request.sizeBytes,
                    quality = quality,
                    onProgress = { written, total ->
                        if (total > 0) {
                            val pct = (written * 100f / total).toInt()
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                _uiState.update { it.copy(uploadProgressPct = pct.toFloat()) }
                            }
                        }
                    },
                )
            }
            null -> error("ProcessingViewModel started with neither a resume jobId nor a pending request")
        }
    }

    private suspend fun pollStatus(jobId: String) {
        try {
            repository.pollJob(jobId).collect { detail ->
                _uiState.update {
                    it.copy(
                        filename = detail.inputFilename,
                        engine = detail.engine,
                        quality = detail.quality,
                        status = detail.status,
                        stage = detail.stage,
                        separatingProgress = detail.progress,
                        errorMessage = detail.error,
                        isDone = detail.status == JobStatus.DONE,
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = describeError(e)) }
        }
    }

    private fun describeError(e: Exception): String = when (val err = e.toAppError()) {
        is com.musicapp.stemseparator.data.repository.AppError.Network -> "Lost connection to the server."
        is com.musicapp.stemseparator.data.repository.AppError.Http -> err.detail
        is com.musicapp.stemseparator.data.repository.AppError.Unknown -> err.cause.message ?: "Unknown error"
    }

    companion object {
        fun factory(
            container: AppContainer,
            jobLaunchViewModel: JobLaunchViewModel,
            resumeJobId: String?,
        ) = viewModelFactory {
            initializer {
                // Read-and-consume: a re-entry into "processing/new" (e.g. process
                // recreation) shouldn't replay a stale pending request.
                val request = jobLaunchViewModel.pendingRequest
                val quality = jobLaunchViewModel.pendingQuality
                jobLaunchViewModel.consume()
                ProcessingViewModel(container.repository, request, quality, resumeJobId)
            }
        }
    }
}
