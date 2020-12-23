package top.netapps.kvppeteer.config.download

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "spring.http-client.pool")
class HttpClientPoolConfig {

    /**
     * java配置的优先级低于yml配置；如果yml配置不存在，会采用java配置
     */
    /**
     * 连接池的最大连接数
     */
    var maxTotalConnect: Int = 0

    /**
     * 同路由的并发数
     */
    var maxConnectPerRoute: Int = Runtime.getRuntime().availableProcessors()

    /**
     * 客户端和服务器建立连接超时，默认2s
     */
    var connectTimeout = 10 * 1000

    /**
     * 指客户端从服务器读取数据包的间隔超时时间,不是总读取时间，默认30s
     */

    var readtimeout = 3 * 1000
    var charset = "UTF-8"

    /**
     * 重试次数,默认2次
     */
    var retryTimes = 3

    /**
     * 从连接池获取连接的超时时间,不宜过长,单位ms
     */
    var connectionRequestTimout = 200

    /**
     * 针对不同的地址,特别设置不同的长连接保持时间
     */
    var keepAliveTargetHost: Map<String, Int>? = null

    /**
     * 针对不同的地址,特别设置不同的长连接保持时间,单位 s
     */
    var keepAliveTime = 30

}