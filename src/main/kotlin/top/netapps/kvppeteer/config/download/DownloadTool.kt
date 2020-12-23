package top.netapps.kvppeteer.config.download

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.*
import java.util.function.Consumer
import javax.annotation.Resource


@Component
class DownloadTool {
    private var log = LoggerFactory.getLogger(DownloadTool::class.java)

    /**
     * 使用自定义的httpclient的restTemplate
     */
    @Resource(name = "httpClientTemplate")
     lateinit var httpClientTemplate: RestTemplate

    @Resource
    private lateinit var webFileUtils: WebFileUtils

    companion object {
        /**
         * 线程最小值
         */
        private var MIN_POOL_SIZE = 10

        /**
         * 线程最大值
         */
        private var MAX_POOL_SIZE = 100

        /**
         * 等待队列大小
         */
        private var WAIT_QUEUE_SIZE = 1000

        /**
         * 线程池
         */
        private lateinit var threadPool: ExecutorService

        private var ONE_KB_SIZE = 1024

        /**
         * 大于20M的文件视为大文件,采用流下载
         */
        private var BIG_FILE_SIZE = 20 * 1024 * 1024
    }


    fun downloadByMultiThread(url: String, targetPath: String,tnum: Int?) {
        var threadNum = tnum
        val startTimestamp = System.currentTimeMillis()
        //开启线程
        threadNum = threadNum ?: MIN_POOL_SIZE
        Assert.isTrue(threadNum > 0, "线程数不能为负数")
        threadPool = ThreadPoolExecutor(
            threadNum, MAX_POOL_SIZE, 0, TimeUnit.MINUTES,
            LinkedBlockingDeque(WAIT_QUEUE_SIZE), ThreadFactory { r ->
                val thread = Thread(r)
                thread.name = "http-down"
                thread
            })
        val isBigFile: Boolean

        //调用head方法,只获取头信息,拿到文件大小
        val contentLength = httpClientTemplate.headForHeaders(url).contentLength
        Assert.isTrue(contentLength > 0, "获取文件大小异常")
        isBigFile = contentLength >= BIG_FILE_SIZE

        when {
            contentLength > 1024 * ONE_KB_SIZE -> {
                log.info(
                    "[多线程下载] Content-Length\t{} ({})",
                    contentLength,
                    (contentLength / 1024 / 1024).toString() + "MB"
                )
            }
            contentLength > ONE_KB_SIZE -> {
                log.info("[多线程下载] Content-Length\t{} ({})", contentLength, (contentLength / 1024).toString() + "KB")
            }
            else -> {
                log.info("[多线程下载] Content-Length\t" + contentLength + "B")
            }
        }
        val futures: ArrayList<CompletableFuture<DownloadTemp>> = ArrayList(threadNum)
        val fileFullPath: String
        val resultFile: RandomAccessFile
        try {
            fileFullPath = webFileUtils.getAndCreateDownloadDir(url, targetPath)
            //创建目标文件
            resultFile = RandomAccessFile(fileFullPath, "rw")
            log.info("[多线程下载] Download started, url:{}\tfileFullPath:{}", url, fileFullPath)

            //每个线程下载的大小 0-9 10个字节
            //5个线程
            //(10-1)/5 ==1+1 全部有了
//            val tempLength = (contentLength - 1) / threadNum + 1
            //分几个文件 tempLength
            val tempLength = contentLength / threadNum + 1
            var start: Long
            var end: Long
            var totalSize = 0
            var i = 0
            while (i < threadNum && totalSize < contentLength) {
                //累加
                start = i * tempLength
                end = start + tempLength - 1
                totalSize += tempLength.toInt()
                log.info("[多线程下载] start:{}\tend:{}", start, end)
                val thread = DownloadThread(httpClientTemplate, i, start, end, url, fileFullPath, isBigFile)
                val future = CompletableFuture.supplyAsync({ thread.call() }, threadPool)
                futures.add(future)
                ++i
            }
        } catch (e: Exception) {
            log.error("[多线程下载] 下载出错", e)
            return
        } finally {
            threadPool.shutdown()
        }

        //合并文件
        futures.forEach(Consumer { f: CompletableFuture<DownloadTemp> ->
            try {
                f.thenAccept { o: DownloadTemp ->
                    try {
                        log.info("[多线程下载] {} 开始合并,文件:{}", o.threadName, o.filename)
                        val tempFile = RandomAccessFile(o.filename, "rw")
                        tempFile.channel.transferTo(0, tempFile.length(), resultFile.channel)
                        tempFile.close()
                        val file = File(o.filename)
                        val b: Boolean = file.delete()
                        log.info("[多线程下载] {} 删除临时文件:{}\t结果:{}", o.threadName, o.filename, b)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        log.error("[多线程下载] {} 合并出错", o.threadName, e)
                    }
                }.get()

            } catch (e: Exception) {
                log.error("[多线程下载] 合并出错", e)
            } finally {
                threadPool.shutdown()
            }
        })

        try {
            resultFile.close()

        } catch (e: IOException) {
            log.error("关闭文件流失败: ", e)
        }
        val completedTimestamp = System.currentTimeMillis()
        log.info(
            "=======下载完成======,耗时{}",
            if (isBigFile) (completedTimestamp - startTimestamp).div(1000)
                .toString() + "s" else (completedTimestamp - startTimestamp).toString() + "ms"
        )
    }

}