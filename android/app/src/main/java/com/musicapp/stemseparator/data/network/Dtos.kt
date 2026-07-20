package com.musicapp.stemseparator.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire types for backend/app/main.py's JSON contract. Kept as raw strings for
 * status/stage/engine/quality (not sealed enums) so an unrecognized future backend
 * value never crashes deserialization -- mapped to app-level types in the repository.
 */

@Serializable
data class JobCreatedDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
)

@Serializable
data class LocalTrackDto(
    val filename: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("modified_at") val modifiedAt: Double,
)

@Serializable
data class CreateLocalJobRequest(
    val filename: String,
    val engine: String? = null,
    val quality: String? = null,
)

@Serializable
data class JobSummaryDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("input_filename") val inputFilename: String,
    val engine: String,
    val quality: String,
)

@Serializable
data class JobDetailDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    val engine: String,
    val quality: String,
    @SerialName("input_filename") val inputFilename: String,
    val progress: Float? = null,
    val stage: String? = null,
    val stems: Map<String, String>? = null,
    val error: String? = null,
)

@Serializable
data class HealthDto(
    val status: String,
    @SerialName("separator_mode") val separatorMode: String,
)
