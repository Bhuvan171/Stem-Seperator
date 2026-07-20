package com.musicapp.stemseparator.ui.serversetup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.ui.theme.AppTypography
import com.musicapp.stemseparator.ui.theme.LocalAppColors
import com.musicapp.stemseparator.ui.theme.Spacing

@Composable
fun ServerSetupScreen(
    container: AppContainer,
    onReady: () -> Unit,
    viewModel: ServerSetupViewModel = viewModel(factory = ServerSetupViewModel.factory(container)),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current

    LaunchedEffect(state.savedAndReady) {
        if (state.savedAndReady) onReady()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.space5)) {
        Text(text = "Connect to your server", style = AppTypography.h1, color = colors.foreground)
        Text(
            text = "Enter the address of the machine running the Stem Separator backend (e.g. http://192.168.1.20:8000). It must be reachable on your current Wi-Fi network.",
            style = AppTypography.bodySmall,
            color = colors.muted,
            modifier = Modifier.padding(top = Spacing.space2, bottom = Spacing.space5),
        )

        OutlinedTextField(
            value = state.urlInput,
            onValueChange = viewModel::onUrlChanged,
            label = { Text("Server address") },
            placeholder = { Text("http://192.168.1.20:8000") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(modifier = Modifier.padding(top = Spacing.space4)) {
            when (val testState = state.testState) {
                is ConnectionTestState.Testing -> CircularProgressIndicator(modifier = Modifier.padding(bottom = Spacing.space3))
                is ConnectionTestState.Success -> Text(
                    text = "Connected -- server running in \"${testState.separatorMode}\" mode.",
                    style = AppTypography.bodySmall,
                    color = colors.foreground,
                    modifier = Modifier.padding(bottom = Spacing.space3),
                )
                is ConnectionTestState.Failure -> Text(
                    text = testState.message,
                    style = AppTypography.bodySmall,
                    color = colors.destructive,
                    modifier = Modifier.padding(bottom = Spacing.space3),
                )
                ConnectionTestState.Idle -> {}
            }

            Button(
                onClick = viewModel::testConnectionAndSave,
                enabled = state.urlInput.isNotBlank() && state.testState !is ConnectionTestState.Testing,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text("Test connection & continue")
            }
        }
    }
}
