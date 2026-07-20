package com.musicapp.stemseparator.data.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every outgoing request's scheme/host/port to the user's currently configured
 * server address, leaving path/query untouched. This exists because Retrofit's own
 * baseUrl is fixed at client-construction time, but the server address here is chosen
 * by the user at runtime (there's no service discovery / stable hostname to build
 * around -- see SettingsRepository) and can change later from Settings.
 *
 * [currentBaseUrl] is updated from a single process-wide collector of
 * SettingsRepository.serverBaseUrl (started once in the Application class), so
 * Retrofit/OkHttp/StemSeparatorApi construction stays a one-time thing at app start.
 */
class ServerUrlInterceptor : Interceptor {

    @Volatile
    var currentBaseUrl: HttpUrl? = null

    fun updateBaseUrl(rawUrl: String?) {
        currentBaseUrl = rawUrl?.toHttpUrlOrNull()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = currentBaseUrl ?: return chain.proceed(request)

        val newUrl = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }

    companion object {
        /** Retrofit requires a syntactically valid baseUrl even though every request is rewritten. */
        const val PLACEHOLDER_BASE_URL = "http://localhost.invalid/"
    }
}
