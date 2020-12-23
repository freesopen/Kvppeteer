package top.netapps.kvppeteer.config.download

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RequestCallback
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.annotation.Resource


@Component
class WebFileUtils {
    private var log = LoggerFactory.getLogger(WebFileUtils::class.java)

    /**
     * 使用自定义的httpclient的restTemplate
     */
    @Resource(name = "httpClientTemplate")
    private lateinit var httpClientTemplate: RestTemplate

    /**
     * 下载小文件,采用字节数组的方式,直接将所有返回都放入内存中,容易引发内存溢出
     *
     * @param url
     * @param targetDir
     */
    fun downloadLittleFileToPath(url: String, targetDir: String) {
        downloadLittleFileToPath(url, targetDir, null)
    }
    /**
     * 下载小文件,直接将所有返回都放入内存中,容易引发内存溢出
     *
     * @param url
     * @param targetDir
     */
    fun downloadLittleFileToPath(url: String, targetDir: String, params: Map<String, String>?) {
        val now = Instant.now()
        val completeUrl: String = addGetQueryParam(url, params)
        val rsp = httpClientTemplate.getForEntity(completeUrl, ByteArray::class.java)
        log.info("[下载文件] [状态码] code:{}", rsp.statusCode)
        try {
            val path: String = getAndCreateDownloadDir(url, targetDir)
            //定义请求头的接收类型
            //定义请求头的接收类型
            val requestCallback = RequestCallback { request: ClientHttpRequest ->
                request.headers.accept = listOf(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
            }
            // getForObject会将所有返回直接放到内存中,使用流来替代这个操作
            val responseExtractor = ResponseExtractor<Void> { response: ClientHttpResponse ->
                // Here I write the response to a file but do what you like
                Files.copy(response.body, Paths.get(path))
                null
            }
            httpClientTemplate.execute(completeUrl, HttpMethod.GET, requestCallback, responseExtractor)
        } catch (e: IOException) {
            log.error("[下载文件] 写入失败:", e)
        }
        log.info("[下载文件] 完成,耗时:{}", ChronoUnit.MILLIS.between(now, Instant.now()))
    }

    /**
     * 拼接get请求参数
     *
     * @param url
     * @param params
     * @return
     */
    private fun addGetQueryParam(url: String, params: Map<String, String>?): String {
        val uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url)
        if (params != null) {
            for ((key, value) in params) {
                uriComponentsBuilder.queryParam(key, value)
            }
        }
        return uriComponentsBuilder.build().encode().toString()
    }

    /**
     * 创建或获取下载文件夹的路径
     *
     * @param url
     * @param targetDir
     * @return
     */
    @Throws(IOException::class)
    fun getAndCreateDownloadDir(url: String, targetDir: String): String {
        var filename = url.substring(url.lastIndexOf("/") + 1)
        var i = 0
        if (url.indexOf("?").also { i = it } != -1) {
            filename = filename.substring(0, i)
        }
        if (!Files.exists(Paths.get(targetDir))) {
            Files.createDirectories(Paths.get(targetDir))
        }
        return if (targetDir.endsWith("/")) targetDir + filename else "$targetDir/$filename"
    }
}