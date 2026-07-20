package com.musicapp.stemseparator.ui.serversetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.data.repository.StemSeparatorRepository
import com.musicapp.stemseparator.data.repository.toAppError
import com.musicapp.stemseparator.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ConnectionTestState {
    data object Idle : ConnectionTestState()
    data object Testing : ConnectionTestState()
    data class Success(val separatorMode: String) : ConnectionTestState()
    data class Failure(val message: String) : ConnectionTestState()
}

data class ServerSetupUiState(
    val urlInput: String = "",
    val testState: ConnectionTestState = ConnectionTestState.Idle,
    val savedAndReady: Boolean = false,
)

class ServerSetupViewModel(
    private val repository: StemSeparatorRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerSetupUiState())
    val uiState: StateFlow<ServerSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.serverBaseUrl.first()?.let { existing ->
                _uiState.value = _uiState.value.copy(urlInput = existing)
            }
        }
    }

    fun onUrlChanged(value: String) {
        _uiState.value = _uiState.value.copy(urlInput = value, testState = ConnectionTestState.Idle)
    }

    /** Tests, then persists on success -- the interceptor picks up the new base URL on
     * the next SettingsRepository.serverBaseUrl emission, so subsequent calls (including
     * a re-test) already go through the freshly saved address. */
    fun testConnectionAndSave() {
        val trimmed = _uiState.value.urlInput.trim().trimEnd('/')
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testState = ConnectionTestState.Testing)
            settingsRepository.setServerBaseUrl(trimmed)
            try {
                val health = repository.health()
                _uiState.value = _uiState.value.copy(
                    testState = ConnectionTestState.Success(health.separatorMode),
                    savedAndReady = true,
                )
            } catch (e: Exception) {
                val message = when (val err = e.toAppError()) {
                    is com.musicapp.stemseparator.data.repository.AppError.Network ->
                        "Couldn't reach that address. Check the IP/port and that your phone is on the same network."
                    is com.musicapp.stemseparator.data.repository.AppError.Http ->
                        "Server responded with an error: ${err.detail}"
                    is com.musicapp.stemseparator.data.repository.AppError.Unknown ->
                        "Unexpected error: ${err.cause.message}"
                }
                _uiState.value = _uiState.value.copy(testState = ConnectionTestState.Failure(message))
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                ServerSetupViewModel(container.repository, container.settingsRepository)
            }
        }
    }
}
