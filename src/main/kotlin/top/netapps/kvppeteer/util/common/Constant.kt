package top.netapps.kvppeteer.util.common

import java.util.*

/**
 * 存放所用到的常量
 */
interface Constant {
    companion object {

        /**
         * 指定版本
         */
        const val VERSION = "722234"

        /**
         * 临时文件夹前缀
         */
        const val PROFILE_PREFIX = "puppeteer_dev_chrome_profile-"

        /**
         * 把产品存放到环境变量的所有可用字段
         */
        val PRODUCT_ENV = arrayOf("PUPPETEER_PRODUCT", "java_config_puppeteer_product", "java_package_config_puppeteer_product")

        /**
         * 把浏览器执行路径存放到环境变量的所有可用字段
         */
        val EXECUTABLE_ENV = arrayOf("PUPPETEER_EXECUTABLE_PATH", "java_config_puppeteer_executable_path", "java_package_config_puppeteer_executable_path")

        /**
         * 把浏览器版本存放到环境变量的字段
         */
        const val PUPPETEER_CHROMIUM_REVISION_ENV = "PUPPETEER_CHROMIUM_REVISION"

        /**
         * 读取流中的数据的buffer size
         */
        const val DEFAULT_BUFFER_SIZE = 8 * 1024

        /**
         * 启动浏览器时，如果没有指定路径，那么会从以下路径搜索可执行的路径
         */
        val PROBABLE_CHROME_EXECUTABLE_PATH = arrayOf(
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser",
                "/usr/bin/google-chrome-stable",
                "/usr/bin/google-chrome",
                "/Applications/Chromium.app/Contents/MacOS/Chromium",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
                "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe"
        )

        /**
         * 谷歌浏览器默认启动参数
         */
        val DEFAULT_ARGS = Collections.unmodifiableList(object : ArrayList<String?>() {
            val serialVersionUID = 1L

            init {
                addAll(Arrays.asList(
                        "--disable-background-networking",
                        "--disable-background-timer-throttling",
                        "--disable-breakpad",
                        "--disable-browser-side-navigation",
                        "--disable-client-side-phishing-detection",
                        "--disable-default-apps",
                        "--disable-dev-shm-usage",
                        "--disable-extensions",
                        "--disable-features=site-per-process",
                        "--disable-hang-monitor",
                        "--disable-popup-blocking",
                        "--disable-prompt-on-repost",
                        "--disable-sync",
                        "--disable-translate",
                        "--metrics-recording-only",
                        "--no-first-run",
                        "--safebrowsing-disable-auto-update",
                        "--enable-automation",
                        "--password-store=basic",
                        "--use-mock-keychain"))
            }
        })
        val supportedMetrics: HashSet<String?> = object : HashSet<String?>() {


            init {
                add("Timestamp")
                add("Documents")
                add("Frames")
                add("JSEventListeners")
                add("Nodes")
                add("LayoutCount")
                add("RecalcStyleCount")
                add("LayoutDuration")
                add("RecalcStyleDuration")
                add("ScriptDuration")
                add("TaskDuration")
                add("JSHeapUsedSize")
                add("JSHeapTotalSize")
            }
        }

        /**
         * fastjson的一个实例
         */

        /**
         * 从浏览器的websocket接受到消息中有以下这些字段，在处理消息用到这些字段
         */
        const val RECV_MESSAGE_METHOD_PROPERTY = "method"
        const val RECV_MESSAGE_PARAMS_PROPERTY = "params"
        const val RECV_MESSAGE_ID_PROPERTY = "id"
        const val RECV_MESSAGE_RESULT_PROPERTY = "result"
        const val RECV_MESSAGE_SESSION_ID_PROPERTY = "sessionId"
        const val RECV_MESSAGE_TARGETINFO_PROPERTY = "targetInfo"
        const val RECV_MESSAGE_TYPE_PROPERTY = "type"
        const val RECV_MESSAGE_ERROR_PROPERTY = "error"
        const val RECV_MESSAGE_ERROR_MESSAGE_PROPERTY = "message"
        const val RECV_MESSAGE_ERROR_DATA_PROPERTY = "data"
        const val RECV_MESSAGE_TARFETINFO_TARGETID_PROPERTY = "targetId"
        const val RECV_MESSAGE_STREAM_PROPERTY = "stream"
        const val RECV_MESSAGE_STREAM_EOF_PROPERTY = "eof"
        const val RECV_MESSAGE_STREAM_DATA_PROPERTY = "data"
        const val RECV_MESSAGE_BASE64ENCODED_PROPERTY = "base64Encoded"

        /**
         * 默认的超时时间：启动浏览器实例超时，websocket接受消息超时等
         */
        const val DEFAULT_TIMEOUT = 30000

        /**
         * 追踪信息的默认分类
         */
        val DEFAULTCATEGORIES = object : LinkedHashSet<String?>() {
            init {
                add("-*")
                add("devtools.timeline")
                add("v8.execute")
                add("disabled-by-default-devtools.timeline")
                add("disabled-by-default-devtools.timeline.frame")
                add("toplevel")
                add("blink.console")
                add("blink.user_timing")
                add("latencyInfo")
                add("disabled-by-default-devtools.timeline.stack")
                add("disabled-by-default-v8.cpu_profiler")
                add("disabled-by-default-v8.cpu_profiler.hires")
            }
        }

        /**
         * 内置线程池的数量
         */
        const val COMMONT_THREAD_POOL_NUM = "kvppeteer_common_thread_number"
    }
}