package com.musicapp.stemseparator.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * 1:1 port of backend/app/main.py's route table. The base URL is a placeholder
 * ("http://placeholder/") rewritten per-request by [ServerUrlInterceptor], since the
 * user configures the real server address at runtime (see SettingsRepository) and
 * Retrofit's own baseUrl is otherwise fixed at construction time.
 */
interface StemSeparatorApi {

    @Multipart
    @POST("jobs")
    suspend fun createJob(
        @Part file: MultipartBody.Part,
        @Part("quality") quality: RequestBody,
    ): JobCreatedDto

    @GET("local-tracks")
    suspend fun listLocalTracks(): List<LocalTrackDto>

    @POST("jobs/local")
    suspend fun createLocalJob(@Body body: CreateLocalJobRequest): JobCreatedDto

    @GET("jobs")
    suspend fun listJobs(): List<JobSummaryDto>

    @GET("jobs/{jobId}")
    suspend fun getJob(@Path("jobId") jobId: String): JobDetailDto

    @GET("health")
    suspend fun health(): HealthDto
}
