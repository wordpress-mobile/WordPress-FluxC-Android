package org.wordpress.android.fluxc.network.rest.wpcom.media

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.Util
import okio.BufferedSink
import okio.Okio
import okio.Source
import java.io.IOException
import java.io.InputStream

object MediaRequestUtils {
    @JvmStatic
    fun create(mediaType: MediaType?, inputStream: InputStream): RequestBody? {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return mediaType
            }

            override fun contentLength(): Long {
                return try {
                    inputStream.available().toLong()
                } catch (e: IOException) {
                    0L
                }
            }

            @Throws(IOException::class) override fun writeTo(sink: BufferedSink) {
                var source: Source? = null
                try {
                    source = Okio.source(inputStream)
                    source?.let { sink.writeAll(source) }
                } finally {
                    Util.closeQuietly(source)
                }
            }
        }
    }
}
