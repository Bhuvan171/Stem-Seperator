package com.musicapp.stemseparator

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class StemSeparatorApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Process-wide collector keeping ServerUrlInterceptor in sync with the user's
        // configured server address, so Retrofit/OkHttp construction in AppContainer
        // stays a one-time thing independent of when/whether a server is configured.
        container.settingsRepository.serverBaseUrl
            .onEach { url -> container.serverUrlInterceptor.updateBaseUrl(url) }
            .launchIn(appScope)
    }
}
