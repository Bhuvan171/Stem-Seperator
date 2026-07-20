package com.musicapp.stemseparator

import android.content.Context
import com.musicapp.stemseparator.data.network.ServerUrlInterceptor
import com.musicapp.stemseparator.data.network.StemSeparatorApi
import com.musicapp.stemseparator.data.repository.StemSeparatorRepository
import com.musicapp.stemseparator.data.settings.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled DI container (no Hilt/Dagger -- proportionate to a personal-scale,
 * 6-screen app). One instance lives for the process lifetime, built in
 * [StemSeparatorApp]. OkHttpClient/Retrofit/StemSeparatorApi construction happens
 * once here regardless of whether/when the user has configured a server address --
 * see [ServerUrlInterceptor] for how the runtime-configurable base URL is threaded in.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository = SettingsRepository(appContext)

    val serverUrlInterceptor = ServerUrlInterceptor()

    private val json = Json { ignoreUnknownKeys = true }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    /** Fast calls: GET /jobs, /jobs/{id} (polling), /local-tracks, /health, POST /jobs/local. */
    val fastClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(serverUrlInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * POST /jobs only: FastAPI doesn't respond with 202 until the whole multipart body
     * is received and written to disk, so a large lossless FLAC over a congested LAN
     * could legitimately take minutes -- both the write phase and the wait-for-response
     * phase need real headroom (bounded loosely by the server's own 30-minute RQ job
     * timeout, though that timeout covers the background separation, not this request).
     */
    val uploadClient: OkHttpClient = fastClient.newBuilder()
        .writeTimeout(20, TimeUnit.MINUTES)
        .readTimeout(20, TimeUnit.MINUTES)
        .callTimeout(20, TimeUnit.MINUTES)
        .build()

    /** Also used by Media3's OkHttpDataSource for stem streaming, so it shares the
     * ServerUrlInterceptor and connection pool with the REST calls. */
    val mediaClient: OkHttpClient get() = fastClient

    private fun buildRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(ServerUrlInterceptor.PLACEHOLDER_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: StemSeparatorApi = buildRetrofit(fastClient).create(StemSeparatorApi::class.java)
    private val uploadApi: StemSeparatorApi = buildRetrofit(uploadClient).create(StemSeparatorApi::class.java)

    val repository: StemSeparatorRepository = StemSeparatorRepository(
        api = api,
        uploadApi = uploadApi,
        contentResolver = appContext.contentResolver,
    )
}
