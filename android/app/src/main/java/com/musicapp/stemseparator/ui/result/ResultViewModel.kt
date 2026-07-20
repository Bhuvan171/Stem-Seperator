package com.musicapp.stemseparator.ui.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.data.model.Engine
import com.musicapp.stemseparator.data.model.JobStatus
import com.musicapp.stemseparator.data.model.Quality
import com.musicapp.stemseparator.data.repository.StemSeparatorRepository
import com.musicapp.stemseparator.data.repository.toAppError
import com.musicapp.stemseparator.player.MixerUiState
import com.musicapp.stemseparator.player.StemMixerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Call

data class ResultUiState(
    val isLoadingJobDetail: Boolean = true,
    val loadError: String? = null,
    val notDoneYet: Boolean = false,
    val mixer: MixerUiState = MixerUiState(),
    val engine: Engine? = null,
    val quality: Quality? = null,
)

/**
 * Owns the mixer engine directly: playback is foreground-only, scoped to this
 * ViewModel's lifecycle (the 6 players are released in [onCleared]) -- stopping when
 * the user leaves the Result screen is the intended behavior, not a limitation.
 */
class ResultViewModel(
    application: Application,
    private val repository: StemSeparatorRepository,
    mediaCallFactory: Call.Factory,
    private val jobId: String,
) : AndroidViewModel(application) {

    private val controller = StemMixerController(callFactory = mediaCallFactory, coroutineScope = viewModelScope)

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        controller.ensurePlayers(application)
        viewModelScope.launch {
            controller.uiState.collect { mixerState ->
                _uiState.update { it.copy(mixer = mixerState) }
            }
        }
        fetchJobDetail()
    }

    private fun fetchJobDetail() {
        viewModelScope.launch {
            try {
                val detail = repository.getJob(jobId)
                if (detail.status != JobStatus.DONE || detail.stems.isNullOrEmpty()) {
                    _uiState.update { it.copy(isLoadingJobDetail = false, notDoneYet = true) }
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoadingJobDetail = false, engine = detail.engine, quality = detail.quality)
                }
                controller.loadJob(jobId, detail.inputFilename, detail.stems)
            } catch (e: Exception) {
                val message = when (val err = e.toAppError()) {
                    is com.musicapp.stemseparator.data.repository.AppError.Network -> "Can't reach the server."
                    is com.musicapp.stemseparator.data.repository.AppError.Http -> err.detail
                    is com.musicapp.stemseparator.data.repository.AppError.Unknown -> err.cause.message ?: "Unknown error"
                }
                _uiState.update { it.copy(isLoadingJobDetail = false, loadError = message) }
            }
        }
    }

    fun togglePlayPause() = controller.togglePlayPause()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun setStemVolume(name: String, volume: Float) = controller.setStemVolume(name, volume)
    fun toggleMute(name: String) = controller.toggleMute(name)
    fun toggleSolo(name: String) = controller.toggleSolo(name)

    override fun onCleared() {
        super.onCleared()
        controller.release()
    }

    companion object {
        fun factory(container: AppContainer, application: Application, jobId: String) = viewModelFactory {
            initializer {
                ResultViewModel(application, container.repository, container.mediaClient, jobId)
            }
        }
    }
}
