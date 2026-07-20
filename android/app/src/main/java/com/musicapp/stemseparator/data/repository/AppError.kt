package com.musicapp.stemseparator.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.HttpException

/** Mirrors the web app's `body.detail || fallback` pattern for FastAPI's {"detail": "..."} error bodies. */
sealed class AppError : Exception() {
    data object Network : AppError()
    data class Http(val code: Int, val detail: String) : AppError()
    data class Unknown(override val cause: Throwable) : AppError()
}

@Serializable
private data class FastApiErrorBody(@SerialName("detail") val detail: String? = null)

private val errorJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

fun Throwable.toAppError(): AppError = when (this) {
    is java.io.IOException -> AppError.Network
    is HttpException -> AppError.Http(code(), extractDetail(response()?.errorBody()) ?: message())
    else -> AppError.Unknown(this)
}

private fun extractDetail(errorBody: ResponseBody?): String? {
    val raw = errorBody?.string() ?: return null
    return try {
        errorJson.decodeFromString(FastApiErrorBody.serializer(), raw).detail
    } catch (e: Exception) {
        null
    }
}
