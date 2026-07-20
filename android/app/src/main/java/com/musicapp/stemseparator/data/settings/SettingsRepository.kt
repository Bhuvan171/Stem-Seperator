package com.musicapp.stemseparator.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Persists the user-chosen backend base URL (e.g. "http://192.168.1.20:8000").
 * There is no service discovery in this project (no mDNS/stable hostname) -- the
 * server is only reachable on the user's LAN, per the backend's own design, so the
 * app must always ask the user directly rather than guess.
 */
class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    private val serverBaseUrlKey = stringPreferencesKey("server_base_url")

    val serverBaseUrl: Flow<String?> = appContext.dataStore.data.map { prefs ->
        prefs[serverBaseUrlKey]
    }

    suspend fun setServerBaseUrl(url: String) {
        val normalized = url.trim().trimEnd('/')
        appContext.dataStore.edit { prefs ->
            prefs[serverBaseUrlKey] = normalized
        }
    }
}
