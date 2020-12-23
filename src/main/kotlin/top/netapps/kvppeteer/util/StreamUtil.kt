package top.netapps.kvppeteer.util

import java.io.*

/**
 * 流的工具类
 */
object StreamUtil {
    fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: IOException) {
                // Ignore this exception.
            }
        }
    }

    fun close(readLineThread: Thread?) {
        var readLineThread = readLineThread
        if (readLineThread != null) {
            readLineThread = null
        }
    }

    @Throws(IOException::class)
    fun toString(`in`: InputStream?): String {
        var wirter: StringWriter? = null
        var reader: InputStreamReader? = null
        return try {
            reader = InputStreamReader(`in`)
            val bufferSize = 4096
            var perReadcount: Int
            val buffer = CharArray(bufferSize)
            wirter = StringWriter()
            while (reader.read(buffer, 0, bufferSize).also { perReadcount = it } != -1) {
                wirter.write(buffer, 0, perReadcount)
            }
            wirter.toString()
        } finally {
            closeQuietly(wirter)
            closeQuietly(reader)
        }
    }
}