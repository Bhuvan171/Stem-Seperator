package com.musicapp.stemseparator.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.musicapp.stemseparator.data.model.JobDetail
import com.musicapp.stemseparator.data.model.JobStatus
import com.musicapp.stemseparator.data.model.JobSummary
import com.musicapp.stemseparator.data.model.LocalTrack
import com.musicapp.stemseparator.data.model.toModel
import com.musicapp.stemseparator.data.network.CountingRequestBody
import com.musicapp.stemseparator.data.network.CreateLocalJobRequest
import com.musicapp.stemseparator.data.network.HealthDto
import com.musicapp.stemseparator.data.network.StemSeparatorApi
import com.musicapp.stemseparator.data.network.UriRequestBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Single seam between ViewModels and the network layer. Also centralizes the
 * job-status polling loop (see [pollJob]) so no ViewModel hand-rolls its own timer.
 *
 * Takes two [StemSeparatorApi] instances backed by differently-tuned OkHttpClients:
 * [api] uses short timeouts for the fast list/status/health calls, while [uploadApi]
 * uses much longer timeouts for POST /jobs, whose response only arrives after the
 * entire multipart body has been received and written to disk server-side.
 */
class StemSeparatorRepository(
    private val api: StemSeparatorApi,
    private val uploadApi: StemSeparatorApi,
    private val contentResolver: ContentResolver,
) {
    suspend fun listLocalTracks(): List<LocalTrack> =
        api.listLocalTracks().map { it.toModel() }

    suspend fun listJobs(): List<JobSummary> =
        api.listJobs().map { it.toModel() }

    suspend fun getJob(jobId: String): JobDetail =
        api.getJob(jobId).toModel()

    suspend fun health(): HealthDto = api.health()

    /** POST /jobs/local -- server already has the file (test_tracks/), no upload leg. */
    suspend fun createLocalJob(filename: String, quality: String): String =
        api.createLocalJob(CreateLocalJobRequest(filename = filename, quality = quality)).jobId

    /**
     * POST /jobs -- streams a SAF content:// Uri's bytes as the multipart file part.
     * [knownLength] is the file size queried via OpenableColumns.SIZE beforehand, or -1 if
     * unknown (callers should hide upload-percentage UI in that case). [onProgress] fires
     * on the network thread with (bytesWritten, totalBytes); totalBytes is -1 if unknown.
     */
    suspend fun uploadAndCreateJob(
        uri: Uri,
        filename: String,
        knownLength: Long,
        quality: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit,
    ): String {
        val mediaType = "application/octet-stream".toMediaType()
        val uriBody = UriRequestBody(contentResolver, uri, mediaType, knownLength)
        val countingBody = CountingRequestBody(uriBody, onProgress)
        val filePart = MultipartBody.Part.createFormData("file", filename, countingBody)
        val qualityBody = quality.toRequestBody("text/plain".toMediaType())
        return uploadApi.createJob(filePart, qualityBody).jobId
    }

    /**
     * Cold flow polling GET /jobs/{jobId} on the same ~1.5s cadence the web app uses
     * (POLL_INTERVAL_MS), emitting immediately then repeating, and completing once status
     * is terminal (done/failed). Tolerates a few consecutive failures before propagating --
     * Wi-Fi blips are more common on mobile than on a wired desktop browser tab, so this is
     * a deliberate, low-risk robustness improvement over the web app's hard-abort-on-first-
     * failure behavior.
     */
    fun pollJob(jobId: String): Flow<JobDetail> = flow {
        var consecutiveFailures = 0
        while (true) {
            try {
                val detail = api.getJob(jobId).toModel()
                consecutiveFailures = 0
                emit(detail)
                if (detail.status == JobStatus.DONE || detail.status == JobStatus.FAILED) {
                    break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                consecutiveFailures++
                if (consecutiveFailures > MAX_CONSECUTIVE_POLL_FAILURES) {
                    throw e.toAppError()
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    companion object {
        const val POLL_INTERVAL_MS = 1500L
        private const val MAX_CONSECUTIVE_POLL_FAILURES = 3
    }
}
