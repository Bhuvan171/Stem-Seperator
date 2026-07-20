package com.musicapp.stemseparator.ui.navigation

object Routes {
    const val SERVER_SETUP = "server_setup"
    const val LIBRARY = "library"
    const val UPLOAD = "upload"
    const val PROCESSING_NEW = "processing/new"
    const val PROCESSING_RESUME = "processing/resume/{jobId}"
    const val RESULT = "result/{jobId}"

    fun processingResume(jobId: String) = "processing/resume/$jobId"
    fun result(jobId: String) = "result/$jobId"
}
