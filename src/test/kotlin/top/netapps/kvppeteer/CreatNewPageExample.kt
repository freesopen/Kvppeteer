package top.netapps.kvppeteer


import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import top.netapps.kvppeteer.config.download.DownloadTool
import javax.annotation.Resource


@SpringBootTest
class CreatNewPageExample {
    @Resource
    private lateinit var downloadTool: DownloadTool


    @Test
    fun testDownloadQiniu2() {
        val path = "D:/down/file/"
//        var url: String = "https://download.jetbrains.com/idea/ideaIU-2020.2.4.win.zip"
//        var url="https://ime.sogoucdn.com/a32265033d23bf3bada1e3b78f3c747f/5fc4d23f/dl/index/1603177583/sogou_pinyin_98a.exe"
        var url="https://npm.taobao.org/mirrors/chromium-browser-snapshots/Win_x64/722234/chrome-win.zip"
        downloadTool.downloadByMultiThread(url, path,Runtime.getRuntime().availableProcessors())
    }
}