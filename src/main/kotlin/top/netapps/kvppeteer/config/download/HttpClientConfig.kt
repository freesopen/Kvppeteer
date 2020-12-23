package top.netapps.kvppeteer.config.download

import org.apache.http.*
import org.apache.http.client.HttpClient
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicHeaderElementIterator
import org.apache.http.protocol.HTTP
import org.apache.http.protocol.HttpContext
import org.apache.http.ssl.SSLContextBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.*
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@Configuration
@ConditionalOnClass(value = [RestTemplate::class, CloseableHttpClient::class])
class HttpClientConfig {
    private var log = LoggerFactory.getLogger(HttpClientConfig::class.java)

    @Autowired
    private lateinit var httpClientPoolConfig: HttpClientPoolConfig

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    fun clientHttpRequestInterceptor(): ClientHttpRequestInterceptor {
        return object : ClientHttpRequestInterceptor {
            override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
                tranceRequest(request, body)
                //                traceResponse(response)
                return execution.execute(request, body)
            }

            @Throws(UnsupportedEncodingException::class)
            private fun tranceRequest(request: HttpRequest, body: ByteArray) {
                log.info("======= request begin ========")
                log.info("uri : {}", request.uri)
                log.info("method : {}", request.method)
                log.info("headers : {}", request.headers)
//                log.info("request body : {}", String(body, Charset.defaultCharset()))
                log.info("======= request end ========")
            }

            @Throws(IOException::class)
            private fun traceResponse(response: ClientHttpResponse) {
                val inputStringBuilder = StringBuilder()
                BufferedReader(InputStreamReader(response.body, "UTF-8")).use { bufferedReader ->
                    var line = bufferedReader.readLine()
                    while (line != null) {
                        inputStringBuilder.append(line)
                        inputStringBuilder.append('\n')
                        line = bufferedReader.readLine()
                    }
                }
                log.info("============================response begin==========================================")
                log.info("Status code  : {}", response.statusCode)
                log.info("Status text  : {}", response.statusText)
                log.info("Headers      : {}", response.headers)
                log.info("Response body: {}", inputStringBuilder.toString())
                log.info("=======================response end=================================================")
            }
        }
    }

    /**
     * 创建HTTP客户端工厂
     */
    @Bean(name = ["clientHttpRequestFactory"])
    fun clientHttpRequestFactory(): ClientHttpRequestFactory {
        /**
         * maxTotalConnection 和 maxConnectionPerRoute 必须要配
         */
        require(httpClientPoolConfig.maxTotalConnect > 0) {
            "invalid maxTotalConnection: " + httpClientPoolConfig.maxTotalConnect
        }
        require(httpClientPoolConfig.maxConnectPerRoute > 0)
        { "invalid maxConnectionPerRoute: " + httpClientPoolConfig.maxConnectPerRoute }

        val clientHttpRequestFactory = HttpComponentsClientHttpRequestFactory(httpClient()!!)
        // 连接超时
        clientHttpRequestFactory.setConnectTimeout(httpClientPoolConfig.connectTimeout)
        // 数据读取超时时间，即SocketTimeout
        clientHttpRequestFactory.setReadTimeout(httpClientPoolConfig.readtimeout)

        // 从连接池获取请求连接的超时时间，不宜过长，必须设置，比如连接不够用时，时间过长将是灾难性的
        clientHttpRequestFactory.setConnectionRequestTimeout(httpClientPoolConfig.connectionRequestTimout)
        return clientHttpRequestFactory
    }

    /**
     * 初始化RestTemplate,并加入spring的Bean工厂，由spring统一管理
     */
    @Bean(name = ["httpClientTemplate"])
    fun restTemplate(factory: ClientHttpRequestFactory): RestTemplate? {
        return createRestTemplate(factory)
    }

    /**
     * 初始化支持异步的RestTemplate,并加入spring的Bean工厂，由spring统一管理,如果你用不到异步，则无须创建该对象
     * 这个类过时了
     * @return
     */
//    @Bean(name = ["asyncRestTemplate"])
//    @ConditionalOnMissingBean(AsyncRestTemplate::class)
//    open fun asyncRestTemplate(restTemplate: RestTemplate?): AsyncRestTemplate? {
//        val factory = Netty4ClientHttpRequestFactory()
//        factory.setConnectTimeout(this.connectionTimeout)
//        factory.setReadTimeout(this.readTimeout)
//        return AsyncRestTemplate(factory, restTemplate!!)
//    }

    /**
     * 配置httpClient
     *
     * @return
     */
    @Bean
    fun httpClient(): HttpClient? {
        val httpClientBuilder = HttpClientBuilder.create()
        try {
            //设置信任ssl访问
            val sslContext: SSLContext = SSLContextBuilder()
                    .loadTrustMaterial(null) { arg0, arg1 -> true }.build()
            httpClientBuilder.setSSLContext(sslContext)
            val hostnameVerifier: HostnameVerifier = NoopHostnameVerifier.INSTANCE
            val sslConnectionSocketFactory = SSLConnectionSocketFactory(sslContext, hostnameVerifier)
            val socketFactoryRegistry: Registry<ConnectionSocketFactory> =
                    RegistryBuilder.create<ConnectionSocketFactory>()
                            // 注册http和https请求
                            .register("http", PlainConnectionSocketFactory.getSocketFactory())
                            .register("https", sslConnectionSocketFactory).build()

            //使用Httpclient连接池的方式配置(推荐)，同时支持netty，okHttp以及其他http框架
            val poolingHttpClientConnectionManager = PoolingHttpClientConnectionManager(socketFactoryRegistry)
            // 最大连接数
            poolingHttpClientConnectionManager.maxTotal = httpClientPoolConfig.maxTotalConnect
            // 同路由并发数
            poolingHttpClientConnectionManager.defaultMaxPerRoute = httpClientPoolConfig.maxConnectPerRoute
            //配置连接池
            httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager)
            // 重试次数
            httpClientBuilder.setRetryHandler(DefaultHttpRequestRetryHandler(httpClientPoolConfig.retryTimes, true))

            //设置默认请求头
            val headers: List<Header> = getDefaultHeaders()
            httpClientBuilder.setDefaultHeaders(headers)
            //设置长连接保持策略
            httpClientBuilder.setKeepAliveStrategy(connectionKeepAliveStrategy())
            return httpClientBuilder.build()
        } catch (e: KeyManagementException) {
            log.error("初始化HTTP连接池出错", e)
        } catch (e: NoSuchAlgorithmException) {
            log.error("初始化HTTP连接池出错", e)
        } catch (e: KeyStoreException) {
            log.error("初始化HTTP连接池出错", e)
        }
        return null
    }

    /**
     * 配置长连接保持策略
     * @return
     */
    fun connectionKeepAliveStrategy(): ConnectionKeepAliveStrategy? {
        return object : ConnectionKeepAliveStrategy {
            override fun getKeepAliveDuration(response: HttpResponse, context: HttpContext): Long {
                // Honor 'keep-alive' header
                val it: HeaderElementIterator = BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE))
                while (it.hasNext()) {
                    val he: HeaderElement = it.nextElement()
                    log.info("HeaderElement:{}", he.toString())
                    val param: String = he.name
                    val value: String = he.value
                    if (value != null && "timeout".equals(param, ignoreCase = true)) {
                        try {
                            return value.toLong() * 1000
                        } catch (ignore: NumberFormatException) {
                            log.error("解析长连接过期时间异常", ignore)
                        }
                    }
                }
                val target: HttpHost = context.getAttribute(
                        HttpClientContext.HTTP_TARGET_HOST) as HttpHost
                //如果请求目标地址,单独配置了长连接保持时间,使用该配置
                val any: Optional<Map.Entry<String, Int>> = Optional.ofNullable(httpClientPoolConfig.keepAliveTargetHost).orElseGet { HashMap() }
                        .entries.stream().filter { e -> e.key == target.hostName }.findAny()
                return any.map { en -> en.value * 1000L }.orElse(httpClientPoolConfig.keepAliveTime * 1000L)
            }
        }
    }

    /**
     * 设置请求头
     *
     * @return
     */
    private fun getDefaultHeaders(): List<Header> {
        val headers: ArrayList<Header> = ArrayList()
        headers.add(BasicHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"))
        headers.add(BasicHeader("Accept-Encoding", "gzip,deflate"))
        headers.add(BasicHeader("Accept-Language", "zh-CN"))
        headers.add(BasicHeader("Connection", "Keep-Alive"))
        return headers
    }

    private fun createRestTemplate(factory: ClientHttpRequestFactory): RestTemplate {
        val restTemplate = RestTemplate(factory)

        //我们采用RestTemplate内部的MessageConverter
        //重新设置StringHttpMessageConverter字符集，解决中文乱码问题
        modifyDefaultCharset(restTemplate)
        //设置错误处理器
        restTemplate.errorHandler = DefaultResponseErrorHandler()
        return restTemplate
    }

    /**
     * 修改默认的字符集类型为utf-8
     *
     * @param restTemplate
     */
    private fun modifyDefaultCharset(restTemplate: RestTemplate) {
        val converterList: ArrayList<HttpMessageConverter<*>> =
                restTemplate.messageConverters as ArrayList<HttpMessageConverter<*>>
        var converterTarget: HttpMessageConverter<*>? = null
        for (item in converterList) {
            if (StringHttpMessageConverter::class.java == item::class.java) {
                converterTarget = item
                break
            }
        }
        if (null != converterTarget) {
//            converterList.remove(converterTarget)
            converterList.remove(converterTarget)
        }
        val defaultCharset: Charset = Charset.forName(httpClientPoolConfig.charset)
        converterList.add(1, StringHttpMessageConverter(defaultCharset))

    }
}