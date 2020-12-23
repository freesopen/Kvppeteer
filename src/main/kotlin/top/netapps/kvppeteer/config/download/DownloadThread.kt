package top.netapps.kvppeteer.config.download

import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.util.Assert
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RequestCallback
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable

class DownloadThread constructor(
    httpClientTemplate: RestTemplate,
    index: Int,
    start: Long,
    end: Long,
    url: String,
    fileFullPath: String,
    isBigFile: Boolean
) : Callable<DownloadTemp> {
    private var log = LoggerFactory.getLogger(DownloadThread::class.java)
    private var index: Int= index
    private var filePath: String= String.format("%s-%s-%d", fileFullPath,
        System.currentTimeMillis().toString(), index)
    private var start: Long = start
    private var end: Long = end
    private var urlString: String=url
    private var httpClientTemplate: RestTemplate=httpClientTemplate
    private var isBigFile = isBigFile
    init {
        Assert.hasText(fileFullPath, "文件下载路径不能为空")
    }

    override fun call(): DownloadTemp {
        //定义请求头的接收类型
        try {
            if (isBigFile) {
                downloadBigFile()
            } else {
                downloadLittleFile()
            }
        } catch (e: Exception) {
            log.error("[线程下载] 下载失败:", e)
        }
        val downloadTemp = DownloadTemp()
        downloadTemp.index = index
        downloadTemp.filename = filePath
        downloadTemp.threadName = Thread.currentThread().name
        log.info("[线程下载] \tcompleted.")
        return downloadTemp
    }

    /**
     * 下载小文件
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun downloadLittleFile() {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.RANGE, "bytes=$start-$end")
        headers.accept = listOf(MediaType.ALL)
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        val httpEntity = HttpEntity(object : LinkedMultiValueMap<String, String>() {

        }, headers)

        val rsp = httpClientTemplate.exchange(urlString, HttpMethod.GET, httpEntity, ByteArray::class.java)
        log.info("[线程下载] 返回状态码:{}", rsp.statusCode)
        Files.write(Paths.get(filePath), Objects.requireNonNull(rsp.body, "未获取到下载文件"))
    }

    /**
     * 下载大文件
     *
     * @throws IOException
     */
    private fun downloadBigFile() {
        val requestCallback = RequestCallback { request: ClientHttpRequest ->
            val headers: HttpHeaders = request.headers
            headers.set(HttpHeaders.RANGE, "bytes=$start-$end")
            headers.accept = listOf(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        }
        // getForObject会将所有返回直接放到内存中,使用流来替代这个操作
        val responseExtractor = ResponseExtractor<Void> { response: ClientHttpResponse ->
            // Here I write the response to a file but do what you like
            Files.copy(response.body, Paths.get(filePath))
            log.info("[线程下载] 返回状态码:{}", response.statusCode)
            null
        }
        httpClientTemplate.execute(urlString, HttpMethod.GET, requestCallback, responseExtractor)
    }


}