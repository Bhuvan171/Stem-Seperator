package com.musicapp.stemseparator.data.network

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import okio.source

/**
 * A RequestBody backed by a SAF content:// Uri, re-opened on every writeTo() call since
 * OkHttp may in principle retry/rewrite a request body more than once. [knownLength] comes
 * from an earlier ContentResolver.query(OpenableColumns.SIZE) lookup; pass -1 if unknown,
 * in which case callers should hide upload-percentage UI (mirrors the web app's own
 * `if (e.lengthComputable)` guard around its XHR upload-progress handler).
 */
class UriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType?,
    private val knownLength: Long,
) : RequestBody() {

    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long = knownLength

    override fun writeTo(sink: BufferedSink) {
        val stream = contentResolver.openInputStream(uri)
            ?: error("Unable to open input stream for $uri")
        stream.use { input ->
            input.source().use { source ->
                sink.writeAll(source)
            }
        }
    }
}

/**
 * Wraps a delegate RequestBody and reports (bytesWritten, totalBytes) as its bytes are
 * streamed to the network -- OkHttp has no built-in upload-progress callback (the same
 * gap the web app worked around with a raw XMLHttpRequest's upload.progress event).
 * [onProgress] is called on the calling (network) thread; throttle/dispatch to the main
 * thread at the call site.
 */
class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit,
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var written = 0L
        val countingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                onProgress(written, total)
            }
        }
        val bufferedCountingSink = countingSink.buffer()
        delegate.writeTo(bufferedCountingSink)
        bufferedCountingSink.flush()
    }
}
