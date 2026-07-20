package com.musicapp.stemseparator.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.musicapp.stemseparator.AppContainer
import com.musicapp.stemseparator.ui.components.AppScaffold
import com.musicapp.stemseparator.ui.library.LibraryScreen
import com.musicapp.stemseparator.ui.processing.ProcessingScreen
import com.musicapp.stemseparator.ui.result.ResultScreen
import com.musicapp.stemseparator.ui.serversetup.ServerSetupScreen
import com.musicapp.stemseparator.ui.upload.UploadScreen
import kotlinx.coroutines.flow.first

@Composable
fun StemSeparatorNavGraph(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    // Activity-scoped shared state for the "pick a track/file -> choose quality ->
    // start a job" handoff -- see JobLaunchViewModel's class doc. NavGraph is only
    // ever hosted once per Activity, so the default (activity-scoped) viewModel() call
    // here is exactly the shared instance Library/Upload/Processing all need.
    val jobLaunchViewModel: JobLaunchViewModel = viewModel()

    // A saved server URL means the user has already been through setup before; skip
    // straight to Library instead of forcing them through it on every launch. Read
    // once at startup -- ServerSetupScreen is still reachable any time afterward via
    // the Library toolbar gear icon if the address needs to change.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val existingUrl = container.settingsRepository.serverBaseUrl.first()
        startDestination = if (existingUrl.isNullOrBlank()) Routes.SERVER_SETUP else Routes.LIBRARY
    }

    AppScaffold {
        val destination = startDestination
        if (destination == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AppScaffold
        }

        NavHost(navController = navController, startDestination = destination) {
            composable(Routes.SERVER_SETUP) {
                ServerSetupScreen(
                    container = container,
                    onReady = {
                        navController.navigate(Routes.LIBRARY) {
                            popUpTo(Routes.SERVER_SETUP) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    container = container,
                    jobLaunchViewModel = jobLaunchViewModel,
                    onOpenUpload = { navController.navigate(Routes.UPLOAD) },
                    onOpenSettings = { navController.navigate(Routes.SERVER_SETUP) },
                    onOpenProcessingNew = { navController.navigate(Routes.PROCESSING_NEW) },
                    onOpenProcessingResume = { jobId -> navController.navigate(Routes.processingResume(jobId)) },
                    onOpenResult = { jobId -> navController.navigate(Routes.result(jobId)) },
                )
            }
            composable(Routes.UPLOAD) {
                UploadScreen(
                    jobLaunchViewModel = jobLaunchViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenProcessingNew = {
                        navController.navigate(Routes.PROCESSING_NEW) {
                            popUpTo(Routes.LIBRARY)
                        }
                    },
                )
            }
            composable(Routes.PROCESSING_NEW) {
                ProcessingScreen(
                    container = container,
                    jobLaunchViewModel = jobLaunchViewModel,
                    resumeJobId = null,
                    onBackToLibrary = {
                        navController.navigate(Routes.LIBRARY) { popUpTo(Routes.LIBRARY) { inclusive = true } }
                    },
                    onDone = { jobId ->
                        navController.navigate(Routes.result(jobId)) {
                            popUpTo(Routes.LIBRARY)
                        }
                    },
                )
            }
            composable(Routes.PROCESSING_RESUME) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
                ProcessingScreen(
                    container = container,
                    jobLaunchViewModel = jobLaunchViewModel,
                    resumeJobId = jobId,
                    onBackToLibrary = {
                        navController.navigate(Routes.LIBRARY) { popUpTo(Routes.LIBRARY) { inclusive = true } }
                    },
                    onDone = { doneJobId ->
                        navController.navigate(Routes.result(doneJobId)) {
                            popUpTo(Routes.LIBRARY)
                        }
                    },
                )
            }
            composable(Routes.RESULT) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
                ResultScreen(
                    container = container,
                    jobId = jobId,
                    onBackToLibrary = { navController.popBackStack(Routes.LIBRARY, inclusive = false) },
                )
            }
        }
    }
}
