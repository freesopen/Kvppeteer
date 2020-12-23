package top.netapps.kvppeteer.util

import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*
import java.util.function.BiConsumer


/**
 * @ClassName: DownloadUtil
 * @author: freesopen
 * @date: 2020/11/15  22:56
 */
class DownloadUtil {


    companion object {
        private val LOGGER = LoggerFactory.getLogger(DownloadUtil::class.java)

        /**
         * 读取流中的数据的buffer size
         */
        var DEFAULT_BUFFER_SIZE = 8 * 1024

        /**
         * 线程池数量
         */
        val THREAD_COUNT = 5

        /**
         * 每条线程下载的文件块大小 5M
         */
        val CHUNK_SIZE = 5 shl 20

        /**
         * 重试次数
         */
        val RETRY_TIMES = 5

        /**
         * 读取数据超时
         */
        val READ_TIME_OUT = 10000

        /**
         * 连接超时设置
         */
        val CONNECT_TIME_OUT = 10000

        val FAIL_RESULT = "-1"


        /**
         * 下载
         * @param url 载的资源定位路径
         * @param filePath 文件路径
         * @param progressCallback 下载回调
         * @throws IOException 异常
         * @throws ExecutionException 异常
         * @throws InterruptedException 异常
         */
        @Throws(IOException::class, ExecutionException::class, InterruptedException::class)
        fun download(url: String, filePath: String, progressCallback: BiConsumer<Int, Int>) {
            val contentLength: Long = this.getContentLength(url)
            val taskCount: Long = if (contentLength % CHUNK_SIZE == 0L) contentLength / CHUNK_SIZE else contentLength / CHUNK_SIZE + 1
            createFile(filePath, contentLength)
            val executor: ThreadPoolExecutor = getExecutor()
            val completionService: CompletionService<String> = ExecutorCompletionService(executor)
            val futureList: MutableList<Future<String>> = ArrayList()
            var downloadCount = 0
            if (contentLength <= CHUNK_SIZE) {
                val future = completionService.submit(DownloadCallable(0, contentLength, filePath, url))
                futureList.add(future)
            } else {
                for (i in 0 until taskCount) {
                    if (i == taskCount - 1) {
                        val future = completionService.submit(DownloadCallable(i * CHUNK_SIZE, contentLength, filePath, url))
                        futureList.add(future)
                    } else {
                        val future = completionService.submit(DownloadCallable(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE, filePath, url))
                        futureList.add(future)
                    }
                }
            }
            executor.shutdown()
            for (future in futureList) {
                val result = future.get()
                if (FAIL_RESULT == result) {
                    LOGGER.error("download fail,url:$url")
                    Files.delete(Paths.get(filePath))
                    executor.shutdownNow()
                } else {
                    try {
                        downloadCount += result.toInt()
                        progressCallback.accept(downloadCount, (contentLength shr 20).toInt())
                    } catch (e: Exception) {
                        LOGGER.error("ProgressCallback has some problem", e)
                    }
                }
            }
        }

        /**
         * 获取下载得文件长度
         * @param url 资源定位路径
         * @throws IOException 连接异常
         * @return 长度
         */
        @Throws(IOException::class)
        fun getContentLength(url: String): Long {
            val uuuRl = URL(url)
            var conn: HttpURLConnection? = null
            return try {
                conn = uuuRl.openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = READ_TIME_OUT
                conn.readTimeout = CONNECT_TIME_OUT
                conn.connect()
                val responseCode = conn.responseCode
                if (responseCode in 200..204) {
                    conn.contentLengthLong
                } else {
                    throw RuntimeException("$url responseCode: $responseCode")
                }
            } finally {
                conn?.disconnect()
            }
        }

        /**
         * 创建一个用于下载chrome的线程池
         * @return 线程池
         */
        fun getExecutor(): ThreadPoolExecutor {
            return ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT, 30000, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        }

        /**
         * 创建固定大小的文件
         *
         * @param path 文件路径
         * @param length 文件大小
         * @throws IOException 操作文件异常
         */
        @Throws(IOException::class)
        fun createFile(path: String?, length: Long) {
            val file = File(path)
            if (file.exists()) {
                FileUtil.createNewFile(file)
            }
            val randomAccessFile = RandomAccessFile(path, "rw")
            randomAccessFile.setLength(length)
            randomAccessFile.close()
        }

        internal class DownloadCallable(private val startPosition: Long,
                                        private val endPosition: Long,
                                        private val filePath: String,
                                        private val url: String) : Callable<String> {
            override fun call(): String {
                var file: RandomAccessFile? = null
                var conn: HttpURLConnection? = null
                return try {
                    file = RandomAccessFile(filePath, "rw")
                    file.seek(startPosition)
                    val uRL = URL(url)
                    conn = uRL.openConnection() as HttpURLConnection
                    conn.connectTimeout = CONNECT_TIME_OUT
                    conn.readTimeout = READ_TIME_OUT
                    conn.requestMethod = "GET"
                    val range = "bytes=$startPosition-$endPosition"
                    conn.addRequestProperty("Range", range)
                    conn.addRequestProperty("accept-encoding", "gzip, deflate, br")
                    val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
                    val channel = file.channel
                    for (j in 0 until RETRY_TIMES) {
                        try {
                            conn.connect()
                            val inputStream = conn.inputStream
                            val readableByteChannel = Channels.newChannel(inputStream)
                            while (readableByteChannel.read(buffer) != -1) {
                                buffer.flip()
                                while (buffer.hasRemaining()) {
                                    channel.write(buffer)
                                }
                                buffer.clear()
                            }
                            return (endPosition - startPosition shr 20).toString()
                        } catch (e: java.lang.Exception) {
                            if (j == RETRY_TIMES - 1) {
                                LOGGER.error("download url[{}] bytes[{}] fail.", url, range)
                            }
                        }
                    }
                    FAIL_RESULT
                } catch (e: java.lang.Exception) {
                    LOGGER.error("download url[{}] bytes[{}] fail.", url, "$startPosition-$endPosition")
                    FAIL_RESULT
                } finally {
                    StreamUtil.closeQuietly(file)
                    conn?.disconnect()
                }
            }
        }
    }
}