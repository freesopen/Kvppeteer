package top.netapps.kvppeteer.demo

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
 import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse

@Controller
class DemoController {
    var log = LoggerFactory.getLogger(DemoController::class.java)

    @GetMapping("/sleep")
    fun sleep(@RequestParam(required = false, defaultValue = "3000") mils: Int): String? {
        log.info("sleep:{}", mils)
        try {
            TimeUnit.MILLISECONDS.sleep(mils.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return "hello!$mils"
    }


    @GetMapping("/sleep/on_and_off")
    fun sleepOnAndOff(response: HttpServletResponse) {
        log.info("sleepOnAndOff")
        for (i in 0..9) {
            try {
                response.writer.println("" + i)
                response.flushBuffer()
                Thread.sleep(300)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}