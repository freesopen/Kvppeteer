package top.netapps.kvppeteer.download

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.slf4j.LoggerFactory
import top.netapps.kvppeteer.options.FetcherOptions
import top.netapps.kvppeteer.util.*
import top.netapps.kvppeteer.util.common.Constant
import java.io.*
import java.math.BigDecimal
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.function.BiConsumer
import java.util.regex.Pattern
import java.util.stream.Stream
import java.util.zip.ZipFile

/**
 * @ClassName: BrowserFetcher
 * @author: freesopen
 * @date: 2020/11/16  22:09
 */
class BrowserFetcher {

    companion object {

        private val LOGGER = LoggerFactory.getLogger(BrowserFetcher::class.java)

        val downloadURLs = object : java.util.HashMap<String, Map<String, String>>() {
            init {
                put("chrome", object : HashMap<String, String>() {

                    init {
                        put("host", "https://npm.taobao.org/mirrors")
                        put("linux", "%s/chromium-browser-snapshots/Linux_x64/%s/%s.zip")
                        put("mac", "%s/chromium-browser-snapshots/Mac/%s/%s.zip")
                        put("win32", "%s/chromium-browser-snapshots/Win/%s/%s.zip")
                        put("win64", "%s/chromium-browser-snapshots/Win_x64/%s/%s.zip")
                    }
                })
                put("firefox", object : HashMap<String, String>() {
                    private val serialVersionUID = 2053771138227029401L

                    init {
                        put("host", "https://github.com/puppeteer/juggler/releases")
                        put("linux", "%s/download/%s/%s.zip")
                        put("mac", "%s/download/%s/%s.zip")
                        put("win32", "%s/download/%s/%s.zip")
                        put("win64", "%s/download/%s/%s.zip")
                    }
                })
            }
        }
    }

    /**
     * 平台 win linux mac
     */
    private var platform: String? = null

    /**
     * 下载的域名
     */
    private var downloadHost: String? = null

    /**
     * 下载的文件夹
     */
    private var downloadsFolder: String? = null

    /**
     * 目前支持两种产品：chrome or firefix
     */
    private var product: String = "chrome"

    constructor () {
        this.downloadsFolder = Helper.join(System.getProperty("user.dir"), ".local-browser")
        this.downloadHost = downloadURLs[this.product]?.get("host")
        if (platform == null) {
            when {
                Helper.isMac -> platform = "mac"
                Helper.isLinux -> platform = "linux"
                Helper.isWindows -> platform = if (Helper.isWin64) "win64" else "win32"
            }
            ValidateUtil.notNull(platform, "Unsupported platform: " + Helper.paltform())
        }
        ValidateUtil.notNull(downloadURLs[product]!![platform!!], "Unsupported platform: $platform")
    }

    /**
     * 创建 BrowserFetcher 对象
     *
     * @param projectRoot 根目录，储存浏览器得根目录
     * @param options     下载浏览器得一些配置
     */
    constructor(projectRoot: String, options: FetcherOptions) {
        product = (options.product ?: "chrome").toLowerCase()
    }

    /**
     *
     * 下载浏览器，如果项目目录下不存在对应版本时
     *
     * 如果不指定版本，则使用默认配置版本
     *
     * @param version 浏览器版本
     * @throws InterruptedException 异常
     * @throws ExecutionException   异常
     * @throws IOException          异常
     */
    @Throws(InterruptedException::class, ExecutionException::class, IOException::class)
    fun downloadIfNotExist(version: String?): RevisionInfo? {
        val fetcher = BrowserFetcher()
        val downLoadVersion = if (StringUtil.isEmpty(version)) Constant.VERSION else version!!
        val revisionInfo: RevisionInfo = fetcher.revisionInfo(downLoadVersion)
        return if (!revisionInfo.local) {
            fetcher.download(downLoadVersion)
        } else revisionInfo
    }


    /**
     * 指定版本下载chromuim
     *
     * @param revision 版本
     * @return 下载后的chromuim包有关信息
     * @throws IOException          异常
     * @throws InterruptedException 异常
     * @throws ExecutionException   异常
     */
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun download(revision: String): RevisionInfo? {
        return this.download(fetchRevision(), BiConsumer<Int, Int> { t, u -> })

    }

    @Throws(IOException::class)
    private fun fetchRevision(): String {
        val downloadUrl = downloadURLs[product]!![platform!!]
        val urlSend = URL(String.format(downloadUrl!!.substring(0, downloadUrl.length - 9), downloadHost))
        val conn = urlSend.openConnection()
        conn.connectTimeout = DownloadUtil.CONNECT_TIME_OUT
        conn.readTimeout = DownloadUtil.READ_TIME_OUT
        val pageContent = StreamUtil.toString(conn.getInputStream())
        return parseRevision(pageContent)
    }

    /**
     * 解析得到最新的浏览器版本
     *
     * @param pageContent 页面内容
     * @return 浏览器版本
     */
    private fun parseRevision(pageContent: String): String {
        var result: String? = null
        val pattern = Pattern.compile("<a href=\"/mirrors/chromium-browser-snapshots/(.*)?/\">")
        val matcher = pattern.matcher(pageContent)
        while (matcher.find()) {
            result = matcher.group(1)
        }
        val split = Objects.requireNonNull(result)?.split("/")?.toTypedArray()!!
        result = if (split.size == 2) {
            split[1]
        } else {
            throw RuntimeException("cant't find latest revision from pageConten:$pageContent")
        }
        return result
    }

    /**
     * 根据给定得浏览器版本下载浏览器，可以利用下载回调显示下载进度
     *
     * @param revision         浏览器版本
     * @param progressCallback 下载回调
     * @return RevisionInfo
     * @throws IOException          异常
     * @throws InterruptedException 异常
     * @throws ExecutionException   异常
     */
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun download(revision: String, progressCallback: BiConsumer<Int, Int>): RevisionInfo {
        var progressCallback = progressCallback
        val url: String = downloadURL(product, platform, downloadHost, revision)
        val lastIndexOf = url.lastIndexOf("/")
        val archivePath = Helper.join(downloadsFolder, url.substring(lastIndexOf))
        val folderPath: String = this.getFolderPath(revision)
        if (existsAsync(folderPath)) return this.revisionInfo(revision)
        if (!existsAsync(downloadsFolder)) mkdirAsync(downloadsFolder)
        try {
            if (progressCallback == null) {
                progressCallback = defaultDownloadCallback()
            }
            downloadFile(url, archivePath, progressCallback)
            install(archivePath, folderPath)
        } finally {
            unlinkAsync(archivePath)
        }
        val revisionInfo: RevisionInfo = this.revisionInfo(revision)
        if (revisionInfo != null) {
            try {
                val executableFile: File = File(revisionInfo.executablePath)
                executableFile.setExecutable(true, false)
            } catch (e: Exception) {
                LOGGER.error("Set executablePath:{} file executation permission fail.", revisionInfo.executablePath)
            }
        }
        //睡眠5s，让解压程序释放chrome.exe
        Thread.sleep(5000L)
        return revisionInfo
    }

    /**
     * intall archive file: *.zip,*.tar.bz2,*.dmg
     *
     * @param archivePath zip路径
     * @param folderPath  存放的路径
     * @throws IOException          异常
     * @throws InterruptedException 异常
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun install(archivePath: String, folderPath: String) {
        LOGGER.info("Installing $archivePath to $folderPath")
        if (archivePath.endsWith(".zip")) {
            extractZip(archivePath, folderPath)
        } else if (archivePath.endsWith(".tar.bz2")) {
            extractTar(archivePath, folderPath)
        } else if (archivePath.endsWith(".dmg")) {
            mkdirAsync(folderPath)
            installDMG(archivePath, folderPath)
        } else {
            throw java.lang.IllegalArgumentException("Unsupported archive format: $archivePath")
        }
    }

    /**
     * mount and copy
     *
     * @param archivePath zip路径
     * @param folderPath  存放路径
     * @return string
     * @throws IOException          异常
     * @throws InterruptedException 异常
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun mountAndCopy(archivePath: String, folderPath: String): String? {
        var mountPath: String? = null
        var reader: BufferedReader? = null
        var line: String?
        var stringWriter: StringWriter? = null
        try {
            val arguments: MutableList<String> = ArrayList()
            arguments.add("/bin/sh")
            arguments.add("-c")
            arguments.add("hdiutil")
            arguments.add("attach")
            arguments.add("-nobrowse")
            arguments.add("-noautoopen")
            arguments.add(archivePath)
            val processBuilder = ProcessBuilder().command(arguments).redirectErrorStream(true)
            val process = processBuilder.start()
            reader = BufferedReader(InputStreamReader(process.inputStream))
            val pattern = Pattern.compile("/Volumes/(.*)", Pattern.MULTILINE)
            stringWriter = StringWriter()
            while (reader.readLine().also { line = it } != null) {
                stringWriter.write(line)
            }
            process.waitFor()
            process.destroyForcibly()
            val matcher = pattern.matcher(stringWriter.toString())
            while (matcher.find()) {
                mountPath = matcher.group()
            }
        } finally {
            StreamUtil.closeQuietly(reader)
            StreamUtil.closeQuietly(stringWriter)
        }
        if (StringUtil.isEmpty(mountPath)) {
            throw RuntimeException("Could not find volume path in [" + stringWriter.toString() + "]")
        }
//        val optionl: Optional<Path> =  this.readdirAsync(Paths.get(mountPath))?.filter(
//            it -> it.toString().endsWith(".app")
//            ).findFirst()
        var optionl = this.readdirAsync(Paths.get(mountPath)).also {
            it.toString().endsWith(".app")
        }.findFirst()

        if (optionl.isPresent) {
            try {
                val path = optionl.get()
                val copyPath = path.toString()
                LOGGER.info("Copying $copyPath to $folderPath")
                val arguments: MutableList<String> = ArrayList()
                arguments.add("/bin/sh")
                arguments.add("-c")
                arguments.add("cp")
                arguments.add("-R")
                arguments.add(copyPath)
                arguments.add(folderPath)
                val processBuilder2 = ProcessBuilder().command(arguments)
                val process2 = processBuilder2.start()
                reader = BufferedReader(InputStreamReader(process2.inputStream))
                while (reader.readLine().also { line = it } != null) {
                    LOGGER.trace(line)
                }
                reader.close()
                reader = BufferedReader(InputStreamReader(process2.errorStream))
                while (reader.readLine().also { line = it } != null) {
                    LOGGER.error(line)
                }
                process2.waitFor()
                process2.destroyForcibly()
            } finally {
                StreamUtil.closeQuietly(reader)
            }
        }
        return mountPath
    }

    /**
     * Install *.app directory from dmg file
     *
     * @param archivePath zip路径
     * @param folderPath  存放路径
     * @throws IOException          异常
     * @throws InterruptedException 异常
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun installDMG(archivePath: String, folderPath: String) {
        var mountPath: String? = null
        try {
            mountPath = mountAndCopy(archivePath, folderPath)
        } finally {
            if (mountPath != null) {
                unmount(mountPath)
            } else {
                throw  RuntimeException("Cannot find app in $mountPath")
            }
        }
//
    }

    /**
     * unmount finally
     *
     * @param mountPath mount Path
     * @throws IOException          异常
     * @throws InterruptedException 异常
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun unmount(mountPath: String) {
        var reader: BufferedReader? = null
        if (StringUtil.isEmpty(mountPath)) {
            return
        }
        val arguments: MutableList<String> = ArrayList()
        arguments.add("/bin/sh")
        arguments.add("-c")
        arguments.add("hdiutil")
        arguments.add("detach")
        arguments.add(mountPath)
        arguments.add("-quiet")
        try {
            val processBuilder3 = ProcessBuilder().command(arguments)
            val process3 = processBuilder3.start()
            LOGGER.info("Unmounting $mountPath")
            var line: String?
            reader = BufferedReader(InputStreamReader(process3.inputStream))
            while (reader.readLine().also { line = it } != null) {
                LOGGER.trace(line)
            }
            reader.close()
            reader = BufferedReader(InputStreamReader(process3.errorStream))
            while (reader.readLine().also { line = it } != null) {
                LOGGER.error(line)
            }
            process3.waitFor()
            process3.destroyForcibly()
        } finally {
            StreamUtil.closeQuietly(reader)
        }
    }

    /**
     * 解压tar文件
     *
     * @param archivePath zip路径
     * @param folderPath  存放路径
     * @throws IOException 异常
     */
    @Throws(IOException::class)
    private fun extractTar(archivePath: String, folderPath: String) {
        var wirter: BufferedOutputStream? = null
        var reader: BufferedInputStream? = null
        var tarArchiveInputStream: TarArchiveInputStream? = null
        try {
            tarArchiveInputStream = TarArchiveInputStream(FileInputStream(archivePath))
            var nextEntry: ArchiveEntry
            while (tarArchiveInputStream.getNextEntry().also { nextEntry = it } != null) {
                val name: String = nextEntry.name
                val path = Paths.get(folderPath, name)
                val file = path.toFile()
                if (nextEntry.isDirectory) {
                    file.mkdirs()
                } else {
                    reader = BufferedInputStream(tarArchiveInputStream)
                    val bufferSize = 8192
                    var perReadcount: Int
                    FileUtil.createNewFile(file)
                    val buffer = ByteArray(bufferSize)
                    wirter = BufferedOutputStream(FileOutputStream(file))
                    while (reader.read(buffer, 0, bufferSize).also { perReadcount = it } != -1) {
                        wirter.write(buffer, 0, perReadcount)
                    }
                    wirter.flush()
                }
            }
        } finally {
            StreamUtil.closeQuietly(wirter)
            StreamUtil.closeQuietly(reader)
            StreamUtil.closeQuietly(tarArchiveInputStream)
        }
    }

    /**
     * 解压zip文件
     *
     * @param archivePath zip路径
     * @param folderPath  存放路径
     * @throws IOException 异常
     */
    @Throws(IOException::class)
    private fun extractZip(archivePath: String, folderPath: String) {
        var wirter: BufferedOutputStream? = null
        var reader: BufferedInputStream? = null
        val zipFile = ZipFile(archivePath)
        val entries = zipFile.entries()
        try {
            while (entries.hasMoreElements()) {
                val zipEntry = entries.nextElement()
                val name = zipEntry.name
                val path = Paths.get(folderPath, name)
                if (zipEntry.isDirectory) {
                    path.toFile().mkdirs()
                } else {
                    reader = BufferedInputStream(zipFile.getInputStream(zipEntry))
                    var perReadcount: Int
                    val buffer = ByteArray(Constant.DEFAULT_BUFFER_SIZE)
                    wirter = BufferedOutputStream(FileOutputStream(path.toString()))
                    while (reader.read(buffer, 0, Constant.DEFAULT_BUFFER_SIZE).also { perReadcount = it } != -1) {
                        wirter.write(buffer, 0, perReadcount)
                    }
                    wirter.flush()
                }
            }
        } finally {
            StreamUtil.closeQuietly(wirter)
            StreamUtil.closeQuietly(reader)
            StreamUtil.closeQuietly(zipFile)
        }
    }

    /**
     * 下载浏览器到具体的路径
     * ContentTypeapplication/x-zip-compressed
     *
     * @param url              url
     * @param archivePath      zip路径
     * @param progressCallback 回调函数
     */
    @Throws(IOException::class, ExecutionException::class, InterruptedException::class)
    private fun downloadFile(url: String, archivePath: String, progressCallback: BiConsumer<Int, Int>) {
        LOGGER.info("Downloading binary from $url")
        DownloadUtil.download(url, archivePath, progressCallback)
        LOGGER.info("Download successfully from $url")
    }

    /**
     * 获取文件夹下所有项目，深度：一级
     *
     * @param downloadsFolder 下载文件夹
     * @return Stream<Path> Stream<Path>
     * @throws IOException 异常
    </Path></Path> */
    @Throws(IOException::class)
    private fun readdirAsync(downloadsFolder: Path): Stream<Path?> {
        ValidateUtil.assertArg(Files.isDirectory(downloadsFolder), "downloadsFolder $downloadsFolder is not Directory")
        return Files.list(downloadsFolder)
    }

    /**
     * 删除压缩文件
     *
     * @param archivePath zip路径
     * @throws IOException 异常
     */
    @Throws(IOException::class)
    private fun unlinkAsync(archivePath: String) {
        Files.deleteIfExists(Paths.get(archivePath))
    }

    /**
     * 默认的下载回调
     *
     * @return 回调函数
     */
    private fun defaultDownloadCallback(): BiConsumer<Int, Int> {
        return BiConsumer { integer1: Int?, integer2: Int? ->
            val decimal1 = BigDecimal(integer1!!)
            val decimal2 = BigDecimal(integer2!!)
            val percent = decimal1.divide(decimal2, 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal(100)).toInt()
            LOGGER.info("Download progress: total[{}M],downloaded[{}M],{}", decimal2, decimal1, "$percent%")
        }
    }

    /**
     * 获取浏览器版本相关信息
     *
     * @param revision 版本
     * @return RevisionInfo
     */
    private fun revisionInfo(revision: String?): RevisionInfo {
        val folderPath = getFolderPath(revision!!)
        val executablePath: String
        executablePath = if ("chrome" == product) {
            if ("mac" == platform) {
                Helper.join(folderPath, archiveName(product, platform!!, revision), "Chromium.app", "Contents", "MacOS", "Chromium")
            } else if ("linux" == platform) {
                Helper.join(folderPath, archiveName(product, platform!!, revision), "chrome")
            } else if ("win32" == platform || "win64" == platform) {
                Helper.join(folderPath, archiveName(product, platform!!, revision), "chrome.exe")
            } else {
                throw IllegalArgumentException("Unsupported platform: $platform")
            }
        } else if ("firefox" == product) {
            if ("mac" == platform) Helper.join(folderPath, "Firefox Nightly.app", "Contents", "MacOS", "firefox")
            else if ("linux" == platform) Helper.join(folderPath, "firefox", "firefox")
            else if ("win32" == platform || "win64" == platform) Helper.join(folderPath, "firefox", "firefox.exe")
            else throw IllegalArgumentException("Unsupported platform: $platform")
        } else {
            throw IllegalArgumentException("Unsupported product: $product")
        }
        val url = downloadURL(product, platform, downloadHost, revision)
        val local = existsAsync(folderPath)
        LOGGER.info("revision:{}，executablePath:{}，folderPath:{}，local:{}，url:{}，product:{}", revision, executablePath, folderPath, local, url, product)
        return RevisionInfo(revision, executablePath, folderPath, local, url, product)
    }

    /**
     * 检测给定的路径是否存在
     *
     * @param filePath 文件路径
     * @return boolean
     */
    fun existsAsync(filePath: String?): Boolean {
        return Files.exists(Paths.get(filePath))
    }

    /**
     * 确定下载的路径
     *
     * @param product  产品：chrome or firefox
     * @param platform win linux mac
     * @param host     域名地址
     * @param revision 版本
     * @return 下载浏览器的url
     */
    private fun downloadURL(product: String, platform: String?, host: String?, revision: String): String {
        return String.format(downloadURLs[product]!![platform!!]
                ?: error(""), host, revision, archiveName(product, platform, revision))
    }

    /**
     * 创建文件夹
     *
     * @param folder 要创建的文件夹
     * @throws IOException 创建文件失败
     */
    @Throws(IOException::class)
    private fun mkdirAsync(folder: String?) {
        val file = File(folder)
        if (!file.exists()) {
            Files.createDirectory(file.toPath())
        }
    }

    /**
     * 根据浏览器版本获取对应浏览器路径
     *
     * @param revision 浏览器版本
     * @return string
     */
    fun getFolderPath(revision: String): String {
        return Paths.get(downloadsFolder, platform + "-" + revision).toString()
    }

    /**
     * 根据平台信息和版本信息确定要下载的浏览器压缩包
     *
     * @param product  产品
     * @param platform 平台
     * @param revision 版本
     * @return 压缩包名字
     */
    fun archiveName(product: String, platform: String, revision: String): String? {
        if ("chrome" == product) {
            if ("linux" == platform) return "chrome-linux"
            if ("mac" == platform) return "chrome-mac"
            if ("win32" == platform || "win64" == platform) {
                // Windows archive name changed at r591479.
                return if (revision.toInt() > 591479) "chrome-win" else "chrome-win32"
            }
        } else if ("firefox" == product) {
            if ("linux" == platform) return "firefox-linux"
            if ("mac" == platform) return "firefox-mac"
            if ("win32" == platform || "win64" == platform) return "firefox-$platform"
        }
        return null
    }
}