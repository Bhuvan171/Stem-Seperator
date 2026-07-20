package com.musicapp.stemseparator.data.model

import com.musicapp.stemseparator.data.network.JobDetailDto
import com.musicapp.stemseparator.data.network.JobSummaryDto
import com.musicapp.stemseparator.data.network.LocalTrackDto
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Canonical, order-significant stem list -- mirrors separator/base.py's STEM_NAMES. */
val STEM_NAMES = listOf("vocals", "drums", "bass", "guitar", "piano", "other")

enum class JobStatus {
    QUEUED, PROCESSING, DONE, FAILED, UNKNOWN;

    companion object {
        fun fromRaw(raw: String): JobStatus = when (raw) {
            "queued" -> QUEUED
            "processing" -> PROCESSING
            "done" -> DONE
            "failed" -> FAILED
            else -> UNKNOWN
        }
    }
}

enum class JobStage {
    UPLOADING, SEPARATING, DOWNLOADING, FINALIZING, UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): JobStage? = when (raw) {
            null -> null
            "uploading" -> UPLOADING
            "separating" -> SEPARATING
            "downloading" -> DOWNLOADING
            "finalizing" -> FINALIZING
            else -> UNKNOWN
        }
    }
}

enum class Engine { MOCK, REAL, UNKNOWN }
enum class Quality { FAST, LOSSLESS, UNKNOWN }

fun engineFromRaw(raw: String): Engine = when (raw) {
    "mock" -> Engine.MOCK
    "real" -> Engine.REAL
    else -> Engine.UNKNOWN
}

fun qualityFromRaw(raw: String): Quality = when (raw) {
    "fast" -> Quality.FAST
    "lossless" -> Quality.LOSSLESS
    else -> Quality.UNKNOWN
}

/** Parses FastAPI's tz-aware ISO-8601 datetime string; falls back to epoch on failure. */
private fun parseInstant(raw: String): Instant = try {
    OffsetDateTime.parse(raw).toInstant()
} catch (e: DateTimeParseException) {
    Instant.EPOCH
}

data class LocalTrack(
    val filename: String,
    val sizeBytes: Long,
    val modifiedAt: Instant,
)

fun LocalTrackDto.toModel() = LocalTrack(
    filename = filename,
    sizeBytes = sizeBytes,
    modifiedAt = Instant.ofEpochMilli((modifiedAt * 1000).toLong()),
)

data class JobSummary(
    val jobId: String,
    val status: JobStatus,
    val createdAt: Instant,
    val inputFilename: String,
    val engine: Engine,
    val quality: Quality,
)

fun JobSummaryDto.toModel() = JobSummary(
    jobId = jobId,
    status = JobStatus.fromRaw(status),
    createdAt = parseInstant(createdAt),
    inputFilename = inputFilename,
    engine = engineFromRaw(engine),
    quality = qualityFromRaw(quality),
)

data class JobDetail(
    val jobId: String,
    val status: JobStatus,
    val createdAt: Instant,
    val engine: Engine,
    val quality: Quality,
    val inputFilename: String,
    val progress: Float?,
    val stage: JobStage?,
    val stems: Map<String, String>?,
    val error: String?,
)

fun JobDetailDto.toModel() = JobDetail(
    jobId = jobId,
    status = JobStatus.fromRaw(status),
    createdAt = parseInstant(createdAt),
    engine = engineFromRaw(engine),
    quality = qualityFromRaw(quality),
    inputFilename = inputFilename,
    progress = progress,
    stage = JobStage.fromRaw(stage),
    stems = stems,
    error = error,
)
