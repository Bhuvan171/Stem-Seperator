package com.musicapp.stemseparator.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.data.model.JobSummary
import com.musicapp.stemseparator.data.model.LocalTrack
import com.musicapp.stemseparator.data.repository.StemSeparatorRepository
import com.musicapp.stemseparator.data.repository.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val localTracks: List<LocalTrack> = emptyList(),
    val jobs: List<JobSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class LibraryViewModel(private val repository: StemSeparatorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Reloads both lists -- mirrors the web app's refreshLibraryScreen(), called on
     * screen entry/ON_RESUME since there's no push mechanism and nothing is cached. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val tracks = repository.listLocalTracks()
                val jobs = repository.listJobs()
                _uiState.value = _uiState.value.copy(localTracks = tracks, jobs = jobs, isLoading = false)
            } catch (e: Exception) {
                val message = when (val err = e.toAppError()) {
                    is com.musicapp.stemseparator.data.repository.AppError.Network -> "Can't reach the server."
                    is com.musicapp.stemseparator.data.repository.AppError.Http -> err.detail
                    is com.musicapp.stemseparator.data.repository.AppError.Unknown -> err.cause.message ?: "Unknown error"
                }
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { LibraryViewModel(container.repository) }
        }
    }
}
